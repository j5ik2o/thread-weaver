#!/usr/bin/env bash

if [[ -e ./env.sh ]]; then
    echo "env.sh is not found."
    exit 1
fi

source ./env.sh

eksctl \
    create cluster \
    --name ${CLUSTER_NAME} \
    --region ${AWS_REGION} \
	--nodes 3 \
	--nodes-min 3 \
	--nodes-max 3 \
	--node-type t2.medium \
	--full-ecr-access \
    --node-ami auto \
    --version 1.12 \
    --nodegroup-name standard-workers \
	--vpc-private-subnets=subnet-0d95b9b58619a28f0,subnet-0c925b21299b32e99,subnet-02dac1f21d5a615a5 \
	--vpc-public-subnets=subnet-07cf3036da717fc39,subnet-043eb530606a93243,subnet-0f57e4727746109ef