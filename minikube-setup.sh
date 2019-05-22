#!/bin/sh

minikube delete
rm -fr ~/.minikube
minikube start --vm-driver hyperkit --cpus 6 --memory 4096 --disk-size 60g
kubectl create namespace thread-weaver
kubectl create serviceaccount thread-weaver
helm init


