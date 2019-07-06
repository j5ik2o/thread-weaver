#!/usr/bin/env bash

set -eu

cd $(dirname $0)

pushd ../../../
AWS_DEFAULT_PROFILE=thread-weaver sbt api-server/ecr:push
popd