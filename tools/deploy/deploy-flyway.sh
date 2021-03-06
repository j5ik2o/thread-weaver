#!/bin/sh

set -eu

cd $(dirname $0)

if [[ $# == 0 ]]; then
  echo "Parameters are empty."
  exit 1
fi

FLYWAY_HOST=
FLYWAY_PORT=3306
FLYWAY_DB=
FLYWAY_USER=
FLYWAY_PASSWORD=

while getopts e:H:P:d:u:p: OPT
do
  case ${OPT} in
    "e") ENV_NAME="$OPTARG" ;;
    "H") FLYWAY_HOST="$OPTARG" ;;
    "P") FLYWAY_PORT="$OPTARG" ;;
    "d") FLYWAY_DB="$OPTARG" ;;
    "u") FLYWAY_USER="$OPTARG" ;;
    "p") FLYWAY_PASSWORD="$OPTARG" ;;
  esac
done

pushd ../../charts

BASE_DIR=./thread-weaver-flyway

if [[ "${ENV_NAME}" = "prod" ]]; then

  if [[ -z "${AWS_PROFILE}" ]]; then
    echo "plz set AWS_PROFILE"
    exit 2
  fi

  if [[ -z "${FLYWAY_HOST}" ]] || [[ -z "${FLYWAY_PORT}" ]] || \
    [[ -z "${FLYWAY_DB}" ]] || [[ -z "${FLYWAY_USER}" ]] || [[ -z "${FLYWAY_PASSWORD}" ]]; then
    echo "plz set FLYWAY_* Parameters"
    exit 3;
  fi
  FLYWAY_HOST=${FLYWAY_HOST} \
  FLYWAY_PORT=${FLYWAY_PORT} \
  FLYWAY_DB=${FLYWAY_DB} \
  FLYWAY_USER=${FLYWAY_USER} \
  FLYWAY_PASSWORD=${FLYWAY_PASSWORD} \
    ACCOUNT_ID=$(aws sts get-caller-identity --profile ${AWS_PROFILE} | jq -r '.Account') \
    envsubst < ${BASE_DIR}/environments/${ENV_NAME}-values.yaml.tpl > ${BASE_DIR}/environments/${ENV_NAME}-values.yaml

  echo "generated ${BASE_DIR}/environments/${ENV_NAME}-values.yaml"

fi

helm install ${BASE_DIR} -f ${BASE_DIR}/environments/${ENV_NAME}-values.yaml --namespace thread-weaver

popd
