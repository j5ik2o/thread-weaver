#!/bin/bash

PROFILE_NAME=
STACK_PREFIX=gatling-vpc

if [ -z $THREAD_WEAVER_GATLING_KEYNAME ]; then
THREAD_WEAVER_GATLING_KEYNAME=cw_kuoka
echo "[warning] THREAD_WEAVER_GATLING_KEYNAME is not set. $THREAD_WEAVER_GATLING_KEYNAME will be used."
fi


function vpc_stack_name() {
  echo "${STACK_PREFIX}-$1"
}

function vpc_update() {
  local stack_name=$(vpc_stack_name $1)
  aws cloudformation update-stack --template-body file://$(pwd)/vpc.template --region ap-northeast-1 --profile $AWS_PROFILE_NAME --stack-name $stack_name
}

function vpc_create() {
  local stack_name=$(vpc_stack_name $1)
  aws cloudformation create-stack --template-body file://$(pwd)/vpc.template --region ap-northeast-1 --profile $AWS_PROFILE_NAME --stack-name $stack_name
}

function vpc_destroy() {
  local stack_name=$(vpc_stack_name $1)
  aws cloudformation delete-stack --stack-name $stack_name --profile $AWS_PROFILE_NAME
}

function vpc_get_vpc_id() {
  local stack_name=$(vpc_stack_name $1)
  aws cloudformation describe-stacks --stack-name $stack_name --profile $AWS_PROFILE_NAME | jq -r '[.Stacks[0].Outputs[] | select (.OutputKey == "VpcId") | .OutputValue] | join(",")'
}

function vpc_get_subnet_ids() {
  local stack_name=$(vpc_stack_name $1)
  aws cloudformation describe-stacks --stack-name $stack_name --profile $AWS_PROFILE_NAME | jq -r '[.Stacks[0].Outputs[] | select (.OutputKey == "PublicSubnetAId") | .OutputValue] | join(",")'
}

function ecs_stack_name() {
  echo "gatling-ecs-$1"
}

function ecs_create() {
  local stack_name=$(vpc_stack_name $(echo "ecs-${1}"))
  aws cloudformation create-stack --template-body file://$(pwd)/ecs.template --region ap-northeast-1 --profile $AWS_PROFILE_NAME --stack-name $stack_name --parameters ParameterKey=KeyName,ParameterValue=$THREAD_WEAVER_GATLING_KEYNAME ParameterKey=SubnetID,ParameterValue=$(vpc_get_subnet_ids $1) ParameterKey=VpcId,ParameterValue=$(vpc_get_vpc_id $1) --capabilities CAPABILITY_IAM
}

function ecs_update() {
  local stack_name=$1
  aws cloudformation update-stack --template-body file://$(pwd)/ecs.template --region ap-northeast-1 --profile $AWS_PROFILE_NAME --stack-name $stack_name --parameters ParameterKey=KeyName,ParameterValue=$THREAD_WEAVER_GATLING_KEYNAME ParameterKey=SubnetID,ParameterValue=$(vpc_get_subnet_ids $1) ParameterKey=VpcId,ParameterValue=$(vpc_get_vpc_id $1) --capabilities CAPABILITY_IAM
}

function ecs_destroy() {
  local stack_name=$1
  aws cloudformation delete-stack --stack-name $stack_name --profile $AWS_PROFILE_NAME
}

function ecs_test_private_registry_connectivity() {
  sudo docker run -e EXTERNAL_API_ENDPOINT=http://ext-green.falcon-dev.chatwork.com -e STREAM_API_ENDPOINT=http://stream-green.falcon-dev.chatwork.com -e TEST_DURATION=5 -e AT_ONCE_USERS=2 -e FEEDER=basic --rm cwfalcon/falcon-gatling-runner:0.0.4
}

function ecs_run_gatling() {

  if [ $1 == "-h" ] || [ $1 == "--help" ]; then
    echo "
    usage:
    control.sh ecs_run_gatling [-c|--class] [-e|--execution-id] [-n|--count] StackName
    [-c|--class]        : Override gatling simulation class name in ECS task definition.
    [-e|--execution-id] : execution id. If not specified, current timestamp value is used.
    [-n|--count]        : number of tasks to execute. Default is 1.
    "
    exit 0
  fi

  local execution_id=`date +%s`
  local count=1

  while [[ $# > 1 ]]
  do
  key="$1"
  case $key in
    -c|--class)
    local simulation_class=$2
    shift
    ;;
    -e|--execution-id)
    local execution_id=$2
    shift
    ;;
    -n|--count)
    local count=$2
    shift
    ;;
    *)
    ;;
  esac
  shift
  done

  local stack_name=$1

  echo "stack_name: $stack_name"
  echo "simulation_class: $simulation_class"
  echo "execution_id: $execution_id"
  echo "count: $count"

  if [ ! -z $simulation_class ]; then
    local simulation_class_json=",
    {
    \"name\": \"THREAD_WEAVER_GATLING_SIMULATION_CLASS\",
    \"value\": \"$simulation_class\"
    }"
  fi

  aws ecs run-task \
    --task-definition $(ecs_get_gatling_task_definition $1) \
    --cluster $(ecs_get_cluster $1) \
    --count $count \
    --overrides "
{
    \"containerOverrides\": [
      {
        \"name\": \"falcon-gatling-runner\",
        \"environment\": [
          {
            \"name\": \"THREAD_WEAVER_GATLING_EXECUTION_ID\",
            \"value\": \"$execution_id\"
          }
          $simulation_class_json
        ]
      }
    ]
}
    "
}

function ecs_get_cluster() {
  local stack_name=$1
  aws cloudformation describe-stacks --stack-name $stack_name | jq -r '[.Stacks[0].Outputs[] | select (.OutputKey == "ecscluster") | .OutputValue] | join(",")'
}

function ecs_get_gatling_task_definition() {
  local stack_name=$1
  aws cloudformation describe-stacks --stack-name $stack_name | jq -r '[.Stacks[0].Outputs[] | select (.OutputKey == "gatlingRunnerTaskDefinition") | .OutputValue] | join(",")'
}

function ecs_list_tasks() {
  local cluster=$(ecs_get_cluster $1)
  aws ecs list-tasks --cluster $cluster
}

function s3_put_ecs_config() {
  # @see EC2 Container Service (ECS) Update – Access Private Docker Repos & Mount Volumes in Containers
  # https://aws.amazon.com/jp/blogs/aws/ec2-container-service-ecs-update-access-private-docker-repos-mount-volumes-in-containers/
  local bucket_name=${1:-cw-falcon-test-gatling-ecs-config}
  echo "bucket name is $bucket_name"
  echo "ECS_ENGINE_AUTH_TYPE=dockercfg" > ecs.config
  echo "ECS_ENGINE_AUTH_DATA=$(jq -M -c .auths ~/.docker/config.json)" >> ecs.config
  aws s3 cp ecs.config s3://$bucket_name/ --profile $AWS_PROFILE_NAME
  rm ecs.config
}

"$@"