#!/usr/bin/env bash

cd $(dirname $0)

if [[ ! -e ./eksctl-env.sh ]]; then
    echo "eksctl-env.sh is not found."
    exit 1
fi

source eksctl-env.sh

eksctl create cluster -f ./cluster.yaml