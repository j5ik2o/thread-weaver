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

helm install ./thread-weaver-api-server --namespace thread-weaver -f ./thread-weaver-api-server/environments/${ENV_NAME}-values.yaml

popd
