#!/usr/bin/env bash

set -eu

cd $(dirname $0)

terraform plan --state=eks.tfstate --var-file=eks.tfvars
