#!/usr/bin/env bash

KUBE_IP=$(minikube ip)
API_PORT=$(kubectl get svc thread-weaver-api-server -n thread-weaver -ojsonpath="{.spec.ports[?(@.name==\"api\")].nodePort}")
curl -X POST "http://$KUBE_IP:$API_PORT/v1/threads/create" -H "accept: application/json" -H "Content-Type: application/json" -d "{\"accountId\":\"01DB5QXD4NP0XQTV92K42B3XBF\",\"title\":\"string\",\"remarks\":\"string\",\"administratorIds\":[\"01DB5QXD4NP0XQTV92K42B3XBF\"],\"memberIds\":[\"01DB5QXD4NP0XQTV92K42B3XBF\"],\"createAt\":10000}"
