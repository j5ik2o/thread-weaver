#!/usr/bin/env bash

cd $(dirname $0)

terraform destroy --var-file=gatling.tfvars --var-file=production.tfvars
