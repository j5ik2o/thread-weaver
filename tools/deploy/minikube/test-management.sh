#!/usr/bin/env bash

API_HOST=$(minikube ip)
MANAGEMENT_PORT=$(kubectl get svc thread-weaver-api-server -n thread-weaver -ojsonpath="{.spec.ports[?(@.name==\"management\")].nodePort}")

curl http://${API_HOST}:${MANAGEMENT_PORT}/cluster/members | jq

