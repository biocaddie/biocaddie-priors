import argparse
import collections
import json
import logging
import os
import urllib.error
import urllib.request


DEFAULT_PRIOR_KEY = '__DEFAULT_PRIOR__'


def compute_priors(qrels_file, data_dir, epsilon=1.0):
    rel_docs = load_rel_docs(qrels_file)
    logging.info('Found {} relevant documents.'.format(str(len(rel_docs))))

    # This will be slow. Before we do anything, we have to know how many times each repository is associated with a
    # relevant document.
    repositories = collections.Counter()
    logging.info('Collecting repositories from relevant documents...')
    for rel_doc in rel_docs:
        with open(os.path.join(data_dir, '{}.json'.format(rel_doc))) as f:
            doc = json.load(f)
            repo = doc['REPOSITORY']
            repositories[repo] += 1
        if sum(repositories.values()) % 100 == 0:
            logging.info('...loaded {} documents...'.format(str(sum(repositories.values()))))
    logging.info('Loaded all documents')

    # We know there are at least 23 repositories from the bioCADDIE challenge; if we see more, cool.
    num_repos = max(len(repositories), 23)
    logging.debug('Found {} repositories.'.format(len(repositories)))
    if len(repositories) < num_repos:
        logging.debug('Defaulting to {} repositories.'.format(str(num_repos)))

    # Add a default repository with zero count for smoothing purposes. This doesn't count towards the number of
    # repositories because we haven't actually seen it -- in fact, it's specifically for repos we haven't seen.
    repositories[DEFAULT_PRIOR_KEY] = 0

    # Compute the repository priors based on the bioCADDIE challenge paper. Uses Laplace-smoothed MLE.
    repo_priors = {}
    logging.info('Computing repository priors...')
    for repo in repositories:
        prior = (repositories[repo] + epsilon) / (len(rel_docs) + epsilon * num_repos)
        repo_priors[repo] = prior

    return repo_priors


def load_rel_docs(qrels_file):
    # Assumes the file is in standard qrels format
    rel_docs = set()
    with open(qrels_file) as f:
        for line in f:
            doc_info = line.split()
            if int(doc_info[-1]) > 0:
                rel_docs.add(doc_info[2])
    return rel_docs


def elasticsearch_update(repository_priors, host, port, index):
    headers = {'Content-Type': 'application/json'}
    data = {
        "script": {
            "inline": "ctx._source.prior = params.getOrDefault(ctx._source['REPOSITORY'], params['{}'])".format(
                DEFAULT_PRIOR_KEY),
            "lang": "painless",
            "params": repository_priors
        }
    }
    req = urllib.request.Request('{host}:{port}/{index}/_update_by_query'.format(host=host, port=str(port),
                                                                                 index=index), headers=headers)

    logging.info('Updating documents in index...')
    urllib.request.urlopen(req, json.dumps(data).encode('utf-8'))


if __name__ == '__main__':
    options = argparse.ArgumentParser(description='Compute query-independent document priors')
    options.add_argument('-v', '--log-level', choices=['DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL'],
                         default='WARNING', help='Level of logging to print')
    options.add_argument('-u', '--host', help='ElasticSearch host to update', default='localhost')
    options.add_argument('-p', '--port', help='ElasticSearch port', default=9200, type=int)
    options.add_argument('-i','--index', help='ElasticSearch index to update', default='biocaddie')
    options.add_argument('--no-update', help='Print the repository priors without updating ElasticSearch',
                         action='store_true')
    required = options.add_argument_group('required arguments')
    required.add_argument('-q', '--qrels', help='The qrels file for the training data')
    required.add_argument('-d', '--data-dir', help='The directory containing the training data in JSON format')
    args = options.parse_args()

    logging.basicConfig(level=getattr(logging, args.log_level))

    repo_priors = compute_priors(args.qrels, args.data_dir)

    # If we are just printing the priors without updating ElasticSearch, then print to stdout. Otherwise, log to INFO.
    out = print if args.no_update else logging.info

    for repo in repo_priors:
        out('{},{}'.format(repo, str(repo_priors[repo])))

    if not args.no_update:
        host = 'http://{}'.format(args.host) if not 'http://' in args.host else args.host
        elasticsearch_update(repo_priors, host, args.port, args.index)