#!/usr/bin/env bash

set -eu

cd $(dirname $0)

terraform apply --state=eks.tfstate --var-file=eks.tfvars
