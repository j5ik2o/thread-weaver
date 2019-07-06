#!/bin/sh

set -eu

kubectl apply -f rbac-config.yaml
helm init --service-account tiller --wait
kubectl create namespace thread-weaver
kubectl create serviceaccount thread-weaver -n thread-weaver

