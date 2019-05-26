#!/usr/bin/env bash

API_HOST=$(minikube ip)
API_PORT=$(kubectl get svc thread-weaver-api-server -n thread-weaver -ojsonpath="{.spec.ports[?(@.name==\"api\")].port}")

ACCOUNT_ID=01DB5QXD4NP0XQTV92K42B3XBF
ADMINISTRATOR_ID=01DB5QXD4NP0XQTV92K42B3XBF

THREAD_ID=$(curl -v -X POST "http://$API_HOST:$API_PORT/v1/threads/create" -H "accept: application/json" -H "Content-Type: application/json" \
    -d "{\"accountId\":\"${ACCOUNT_ID}\",\"title\":\"string\",\"remarks\":\"string\",\"administratorIds\":[\"${ADMINISTRATOR_ID}\"],\"memberIds\":[\"${ACCOUNT_ID}\"],\"createAt\":10000}" | jq -r .threadId)
echo "THREAD_ID=$THREAD_ID"
sleep 3
curl -v -X GET "http://$API_HOST:$API_PORT/v1/threads/${THREAD_ID}?account_id=${ACCOUNT_ID}" -H "accept: application/json"

