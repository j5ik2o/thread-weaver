#!/usr/bin/env bash

cd $(dirname $0)

terraform apply --var-file=gatling.tfvars --var-file=gatling.tfvars
