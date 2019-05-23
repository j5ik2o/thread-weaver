#!/bin/sh

sbt -Dmysql.host=$(minikube ip) -Dmysql.port=30306 'migrate-mysql/run'
DYNAMODB_HOST=$(minikube ip) DYNAMODB_PORT=32000 sbt 'migrate-dynamodb/run'
