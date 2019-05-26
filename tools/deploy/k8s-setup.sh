#!/bin/sh

helm init
kubectl create namespace thread-weaver
kubectl create serviceaccount thread-weaver

