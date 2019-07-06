#!/usr/bin/env bash

set -eu

cd $(dirname $0)

terraform destroy --state=eks.tfstate --var-file=eks.tfvars
