#!/usr/bin/env bash

helm upgrade dd-agent --install -f dd-agent-values.yaml --namespace gaudi-poc stable/datadog --recreate-pods
