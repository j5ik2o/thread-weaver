#!/usr/bin/env bash

cd $(dirname $0)

if [[ ! -e ./env.sh ]]; then
    echo "env.sh is not found."
    exit 1
fi

source env.sh

eksctl delete cluster --name ${CLUSTER_NAME} --region ${AWS_REGION}