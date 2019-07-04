#!/usr/bin/env bash

cd $(dirname $0)

terraform init --var-file=eks.tfvars
