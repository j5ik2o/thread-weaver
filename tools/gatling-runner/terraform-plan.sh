#!/usr/bin/env bash

cd $(dirname $0)

terraform plan --var-file=gatling.tfvars --var-file=production.tfvars
