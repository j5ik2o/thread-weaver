#!/usr/bin/env bash

helm upgrade dd-agent --install -f dd-agent-values.yaml --namespace thread-weaver stable/datadog --recreate-pods
