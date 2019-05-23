#!/bin/sh

sbt api-server/docker:publishLocal
helm install ./k8s/thread-weaver-api-server --namespace thread-weaver
