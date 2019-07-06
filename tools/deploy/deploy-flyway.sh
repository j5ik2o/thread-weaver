#!/bin/sh

cd $(dirname $0)
if [[ $# == 0 ]]; then
  echo "Parameters are empty."
  exit 1
fi

while getopts e: OPT
do
    case ${OPT} in
        "e") ENV_NAME="$OPTARG" ;;
    esac
done

pushd ../../charts

BASE_DIR=./thread-weaver-flyway

if [[ "${ENV_NAME}" = "prod" ]]; then
ACCOUNT_ID=$(aws sts get-caller-identity --profile ${AWS_PROFILE} | jq -r '.Account') \
    envsubst < ${BASE_DIR}/environments/${ENV_NAME}-values.yml.tpl > ${BASE_DIR}/environments/${ENV_NAME}-values.yml
    echo "generated ${BASE_DIR}/environments/${ENV_NAME}-values.yaml"
fi

helm install ${BASE_DIR} -f ${BASE_DIR}/environments/${ENV_NAME}-values.yaml --wait

popd
