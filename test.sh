#!/bin/bash

# Point to a specific instance of elasticsearch (defaults to Docker instance)
TEST_HOST="localhost"
TEST_PORT="9200"
TEST_USERNAME="elastic"
TEST_PASSWORD="changeme"

# Specify expansion / search parameters
SEARCH_INDEX="biocaddie"
SEARCH_TYPE="dataset"
TEST_QUERY="multiple+sclerosis"

# Override additional parameters here
ADDITIONAL_ARGS="&fbDocs=50"

curl -u "${TEST_USERNAME}:${TEST_PASSWORD}" ${TEST_HOST}:${TEST_PORT}/${SEARCH_INDEX}/${SEARCH_TYPE}/_priorsearch'?pretty&query='${TEST_QUERY}${ADDITIONAL_ARGS}
