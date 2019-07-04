#!/usr/bin/env bash

cd $(dirname $0)

terraform apply --state=eks.tfstate --var-file=eks.tfvars
