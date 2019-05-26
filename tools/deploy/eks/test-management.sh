#!/usr/bin/env bash

API_HOST=$(kubectl get svc thread-weaver-api-server -n thread-weaver -ojsonpath="{.status.loadBalancer.ingress[0].hostname}")
MANAGEMENT_PORT=$(kubectl get svc thread-weaver-api-server -n thread-weaver -ojsonpath="{.spec.ports[?(@.name==\"management\")].port}")

curl http://${API_HOST}:${MANAGEMENT_PORT}/cluster/members | jq

