#!/usr/bin/env bash

cd $(dirname $0)

if [[ ! -e ./eks-terraform-env.sh ]]; then
    echo "eks-terraform-env.sh is not found."
    exit 1
fi

source ./eks-terraform-env.sh

terraform init -backend=true \
  -backend-config="bucket=${TF_BUCKET_NAME}" \
  -backend-config="key=${TF_STATE_NAME}" \
  -backend-config="region=${AWS_REGION}" \
  -backend-config="profile=${AWS_PROFILE}" \
  -backend-config="encrypt=true" \
  --var-file=eks.tfvars
