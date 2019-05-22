#!/bin/sh

eval $(minikube docker-env)

sbt -Dmysql.host=$(minikube ip) -Dmysql.port=30306 'migrate/run'
sbt api-server/docker:publishLocal
helm install ./k8s/mysql --namespace thread-weaver
helm install ./k8s/dynamodb --namespace thread-weaver
helm install ./k8s/thread-weaver-api-server --namespace thread-weaver
