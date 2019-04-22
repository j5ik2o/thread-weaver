#!/bin/sh

minikube start
eval $(minikube docker-env)
sbt docker:publishLocal
# create serviceAccount and role
kubectl create -f k8s/rbac.yml
# create deployment
kubectl create -f k8s/deployment.yml
# create service
kubectl create -f k8s/service.yml
