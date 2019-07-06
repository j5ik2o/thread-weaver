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

pushd ../../../charts

helm install ./mysql --namespace thread-weaver -f ./mysql/environments/${ENV_NAME}-values.yaml --wait
helm install ./dynamodb --namespace thread-weaver -f ./dynamodb/environments/${ENV_NAME}-values.yaml --wait

popd
