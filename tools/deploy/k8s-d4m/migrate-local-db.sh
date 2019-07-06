#!/bin/sh

set -eu

cd $(dirname $0)
HOST=$(minikube ip)
pushd ../../../

sbt -Dmysql.host="$HOST" -Dmysql.port=30306 'migrate-mysql/run'
DYNAMODB_HOST="$HOST" DYNAMODB_PORT=32000 sbt 'migrate-dynamodb/run'

popd
