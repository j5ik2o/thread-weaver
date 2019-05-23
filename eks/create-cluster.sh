#!/usr/bin/env bash

AWS_PROFILE=cw-test eksctl create cluster --name j5ik2o-eks --region ap-northeast-1 \
	--nodes 3 --nodes-min 3 --nodes-max 3 --node-type t2.medium --full-ecr-access \
    --node-ami auto \
    --version 1.12 \
    --nodegroup-name standard-workers \
	--vpc-private-subnets=subnet-0d95b9b58619a28f0,subnet-0c925b21299b32e99,subnet-02dac1f21d5a615a5 \
	--vpc-public-subnets=subnet-07cf3036da717fc39,subnet-043eb530606a93243,subnet-0f57e4727746109ef