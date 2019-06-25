#!/usr/bin/env bash

cd $(dirname $0)

terraform destroy --state=eks.tfstate --var-file=eks.tfvars ./eks
