#!/bin/sh

set -eu

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

BASE_DIR=./thread-weaver-api-server

if [[ "${ENV_NAME}" = "prod" ]]; then
if [[ -z "${AWS_PROFILE}" ]]; then
    echo "please set AWS_PROFILE"
    exit 2
fi
ACCOUNT_ID=$(aws sts get-caller-identity --profile ${AWS_PROFILE} | jq -r '.Account') \
    envsubst < ${BASE_DIR}/environments/${ENV_NAME}-values.yaml.tpl > ${BASE_DIR}/environments/${ENV_NAME}-values.yaml
    echo "generated ${BASE_DIR}/environments/${ENV_NAME}-values.yaml"
fi

helm install ${BASE_DIR} -f ${BASE_DIR}/environments/${ENV_NAME}-values.yaml --namespace thread-weaver

popd
