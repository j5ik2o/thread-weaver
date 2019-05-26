#!/usr/bin/env bash

cd $(dirname $0)

if [[ ! -e ./env.sh ]]; then
    echo "env.sh is not found."
    exit 1
fi

source env.sh

eksctl \
    create cluster \
    --name ${CLUSTER_NAME} \
    --region ${AWS_REGION} \
	--nodes ${NODES} \
	--nodes-min ${NODES_MIN} \
	--nodes-max ${NODES_MAX} \
	--node-type ${INSTANCE_TYPE} \
	--full-ecr-access \
    --node-ami ${NODE_AMI} \
    --version ${K8S_VERSION} \
    --nodegroup-name ${NODE_GROUP_NAME} \
	--vpc-private-subnets=${SUBNET_PRIVATE1},${SUBNET_PRIVATE2},${SUBNET_PRIVATE3} \
	--vpc-public-subnets=${SUBNET_PUBLIC1},${SUBNET_PUBLIC2},${SUBNET_PUBLIC3}