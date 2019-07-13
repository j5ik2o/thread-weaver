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

KUBE_NAMESPACE=thread-weaver
APP_NAME=thread-weaver-api-server

helm upgrade ${APP_NAME} ./${APP_NAME} -i -f ${APP_NAME}/environments/${ENV_NAME}-values.yaml --namespace ${KUBE_NAMESPACE} --recreate-pods

popd