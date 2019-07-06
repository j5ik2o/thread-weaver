#!/usr/bin/env bash

set -eu

API_HOST=$(kubectl get svc thread-weaver-api-server -n thread-weaver -ojsonpath="{.status.loadBalancer.ingress[0].hostname}")
MANAGEMENT_PORT=$(kubectl get svc thread-weaver-api-server -n thread-weaver -ojsonpath="{.spec.ports[?(@.name==\"management\")].port}")

echo "API_HOST=${API_HOST}"
echo "MANAGEMENT_PORT=${MANAGEMENT_PORT}"

if [[ -z "${MANAGEMENT_PORT}" ]]; then
    echo "Failed to get management port"
    exit 1
fi

curl -s -v http://${API_HOST}:${MANAGEMENT_PORT}/cluster/members | jq

