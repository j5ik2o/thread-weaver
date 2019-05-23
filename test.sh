#!/usr/bin/env bash

KUBE_IP=$(minikube ip)
MANAGEMENT_PORT=$(kubectl get svc thread-weaver-api-server -n thread-weaver -ojsonpath="{.spec.ports[?(@.name==\"management\")].nodePort}")
curl http://$KUBE_IP:$MANAGEMENT_PORT/cluster/members | jq
API_PORT=$(kubectl get svc thread-weaver-api-server -n thread-weaver -ojsonpath="{.spec.ports[?(@.name==\"api\")].nodePort}")
curl http://$KUBE_IP:$API_PORT/
curl -X POST "http://$KUBE_IP:$API_PORT/v1/threads/create" -H "accept: application/json" -H "Content-Type: application/json" -d "{\"accountId\":\"01DB5QXD4NP0XQTV92K42B3XBF\",\"title\":\"string\",\"remarks\":\"string\",\"administratorIds\":[\"01DB5QXD4NP0XQTV92K42B3XBF\"],\"memberIds\":[\"01DB5QXD4NP0XQTV92K42B3XBF\"],\"createAt\":10000}"
