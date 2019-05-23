#!/bin/sh

helm install ./k8s/mysql --namespace thread-weaver
helm install ./k8s/dynamodb --namespace thread-weaver
