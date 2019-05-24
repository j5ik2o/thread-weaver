#!/usr/bin/env bash

cd $(dirname $0)
eval $(minikube docker-env)
pushd ../../../
sbt api-server/docker:publishLocal
popd