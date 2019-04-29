const util = require('util');

const AWS = require('aws-sdk');
AWS.config.update({region: 'ap-northeast-1'});

const moment = require('moment-timezone');

const ecs = new AWS.ECS();
const cloudformation = new AWS.CloudFormation();

const params = require("./runGatlingEcsTask.json")
const clusterStackName = params.clusterStackName;
const taskStackName = params.taskStackName;
const s3LogPrefix = params.s3LogPrefix || "";
// Output Name
const clusterName = "ecscluster";
const taskName = "GatlingSimulationTaskDefinition";

const executionIdEnvName = "MR_GATLING_EXECUTION_ID"


function main() {
  const promises = [
    getTaskDefinition(taskStackName),
    getCluster(clusterStackName)
  ];

  Promise.all(promises).then((res) => {
    const [task, cluster] = res;

    console.log("cluster: " + cluster + ", task: " + task);
    const param = taskParam(cluster, task);
    console.log("parameters: ");
    console.log(util.inspect(param, {depth: 6}));
    ecs.runTask(param, (err, result) => {
      if (err) {
        console.error(err);
        return;
      }
      console.log("result: ");
      console.log(result);
    });
  })
}

function makeEnvironment(obj) {
  if (!obj[executionIdEnvName]) {
    var now = moment.tz("Asia/Tokyo")
    obj[executionIdEnvName] = s3LogPrefix + now.format("YYYYMMDDHHmmss") + "-" + now.format("x");
  }
  return Object.keys(obj).map((key) => {
    return {
      name: key,
      value: obj[key]
    };
  });
}

function taskParam(cluster, taskDefinition) {
 return {
  taskDefinition: taskDefinition,
  cluster: cluster,
  count: params.count,
  overrides: {
    containerOverrides: [
      {
        environment: makeEnvironment(params.environment),
        name: "gatling-runner"
      }
    ]
  }
};
}

function getCluster(stackName) {
  return describeStack(stackName).then((data) => {
    return data.Stacks[0].Outputs.filter(output => output.OutputKey == clusterName)[0].OutputValue
  }).catch(error => console.error(error));
}

function getTaskDefinition(stackName) {
  return describeStack(stackName).then((data) => {
    return data.Stacks[0].Outputs.filter(output => output.OutputKey == taskName)[0].OutputValue;
  }).catch(error => console.error(error));
}

function describeStack(stackName) {
  return new Promise((resolve, reject) => {
    cloudformation.describeStacks({StackName: stackName}, (err, data) => {
      if (err) {
        reject(err);
        return;
      }
      resolve(data);
    })
  });
}

main();