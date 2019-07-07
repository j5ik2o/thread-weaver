#!/bin/sh

set -eu

cd $(dirname $0)
HOST=localhost
pushd ../../../

DYNAMODB_HOST="$HOST" DYNAMODB_PORT=32000 sbt 'migrate-dynamodb/run'

popd
