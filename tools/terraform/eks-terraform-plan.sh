#!/usr/bin/env bash

cd $(dirname $0)

terraform plan --state=eks.tfstate --var-file=eks.tfvars
