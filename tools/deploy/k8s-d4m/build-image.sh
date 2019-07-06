#!/usr/bin/env bash

set -eu

cd $(dirname $0)

pushd ../../../
sbt api-server/docker:publishLocal
popd