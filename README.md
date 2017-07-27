# Query-Independent Priors
The script `query-independent-priors.py` can be used to calculate query-independent priors on the basis of training data. To simply print the repository priors, use the `--no-update` option. Otherwise, this script will attempt to update an existing ElasticSearch index to add the priors it has computed.

## Usage
In general, query independent priors can be computed and updated with the following command:
```
python3 query-independent-priors.py -q <qrels> -d <update_json_folder>
```
There are several options. The defaults for ElasticSearch updating are based on a basic ElasticSearch installation with an index called "biocaddie." These parameters may be overridden with the `-u`, `-p`, and `-i` options. See all options with `-h`.

## Scoring with Pre-computed Priors
Once your ElasticSearch index is updated to include priors, they may be incorporated into document scoring using the following query format (with an example curl command):
```
curl -XGET '<host>:<port>/<index>/_search?pretty' -H 'Content-Type: application/json' -d'
{
  "query": {
    "function_score": {
      "query": {
        "query_string": {
          "query": "<query string>"
        }
      },
      "script_score": {
        "script": {
          "lang": "painless",
          "inline": "_score * params._source.prior"
        }
      }
    }
  }
}'
```
Be sure to replace bracketed sections with your required values. This will adjust the raw document score by multiplying it against the stored prior value previously computed.

# Query-Dependent Priors
For query-dependent priors, we provide a simple ElasticSearch plugin wrapping around the search endpoint. Once installed, query-dependent priors may be computed and incorporated into scoring on-the-fly by directing searches to the `/<index>/_priorsearch` REST endpoint.

## Prerequisites
* Docker
   
**OR**

* Git + Maven

## Usage
For now, cloning the source is required to run the plugin:
```bash
git clone gtsherman/biocaddie-priors && cd biocaddie-priors 
```
To use:

0. [Setup](README.md#setup)
1. [Build](README.md#build)
2. [Load](README.md#load)
3. [Test](README.md#test)

### Setup
Make sure that the biocaddie benchmark test dataset exists somewhere on disk:
```bash
cd $HOME
wget https://biocaddie.org/sites/default/files/update_json_folder.zip && unzip update_json_folder.zip
```

Run an ElasticSearch 5.3.2 container using the helper script:
```bash
./scripts/start.sh
```

Then, set up an index with the required parameters (store==true):
```bash
./scripts/create-index.sh
```

NOTE: You may need to modify *dataset_path* in `./scripts/add-docs.sh` if your benchmark data is not located within `$HOME`.

Finally, use the helper script to add the documents to the index:
```bash
./scripts/add-docs.sh
```

NOTE: Indexing the full benchmark set can take a long time. If you only need a small subset of the documents, you can always `Ctrl+C` once you get the desired number of records indexed.

### Build
A helper script has been included to ease building:
```bash
./scripts/build.sh
```

This will attempt to build the source using Maven (or Docker, if Maven is not available).

Either way, the build should produce a `target/releases/` directory with the necessary `.zip` file.

The `.zip` that ElasticSearch needs should be found at `./target/releases/queryexpansion-5.3.2-SNAPSHOT.zip`.

### Load
Once the artifacts are built, we just need to install them and restart ElasticSearch:
```bash
./scripts/install.sh
./scripts/restart.sh
```

### Test
You should now be able to test the new endpoint using the helper script or via raw `curl`:
```bash
$ ./test.sh
```

You can check the container logs to see what happened under the covers:
```bash
$ ./logs.sh
...
[2017-07-25T23:47:58,861][INFO ][o.n.e.p.QueryDocPriorSearchRestAction] [cb4q1Rk] Starting QueryDocPriorSearch (index=biocaddie, query=multiple sclerosis, type=dataset, field=_all, fbDocs=50, stoplist=null)
[2017-07-25T23:47:58,923][INFO ][o.n.e.p.QueryDocPriorSearchRestAction] [cb4q1Rk] Parameters: 
[2017-07-25T23:47:58,923][INFO ][o.n.e.p.QueryDocPriorSearchRestAction] [cb4q1Rk] bioproject_021116: 0.0821917808219178
[2017-07-25T23:47:58,924][INFO ][o.n.e.p.QueryDocPriorSearchRestAction] [cb4q1Rk] arrayexpress_020916: 0.1095890410958904
[2017-07-25T23:47:58,924][INFO ][o.n.e.p.QueryDocPriorSearchRestAction] [cb4q1Rk] __DEFAULT_PRIOR__: 0.0136986301369863
[2017-07-25T23:47:58,924][INFO ][o.n.e.p.QueryDocPriorSearchRestAction] [cb4q1Rk] Running query with prior against: biocaddie
...
```

## Helper Scripts
A few other helper scripts are included to ease testing of the plugin:
```bash
./scripts/start.sh          # Runs or starts your elasticsearch container
./scripts/stop.sh           # Stops your elasticsearch container
./scripts/restart.sh
./scripts/create-index.sh   # Creates a test index with the proper settings to enable storing term vectors
./scripts/add-docs.sh [-v]  # Adds documents from the biocaddie benchmark set to your index (assumes correct paths)
./scripts/delete-index.sh   # Deletes your container's test index and the records within
./scripts/build.sh          # Builds up elasticsearch plugin artifacts
./scripts/install.sh        # Installs the elasticsearch plugin into your running container
./scripts/remove.sh         # Removes your container's installed queryexpanion plugin
./rebuild.sh                # Removes the current plugin, builds the artifacts, installs the new plugin, and restarts elasticsearch to facilitate rapid development and testing
./logs.sh                   # View your elasticsearch container logs (tail=100)
./test.sh [search]          # Performs a test query against our REST API endpoint (only expands by default, but searches if first parameter is "search")
```

# Credit
The plugin code is pseudo-forked from https://github.com/bodom0015/elasticsearch-queryexpansion-plugin. Much of it remains unchanged, and strangeness may occur.
