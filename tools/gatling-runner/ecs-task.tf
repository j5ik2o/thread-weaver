resource "aws_iam_role" "gatling-ecs-task-execution-role" {
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": [
          "ecs-tasks.amazonaws.com"
        ]
      },
      "Action": [
        "sts:AssumeRole"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_policy" "gatling-ecs-policy" {
  name = "${var.prefix}-ecs-policy"
  path        = "/"
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:CreateBucket",
        "s3:PutObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::${var.gatling-s3-log-bucket-name}",
        "arn:aws:s3:::${var.gatling-s3-log-bucket-name}/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "*"
    }
  ]
}
EOF

}

resource "aws_iam_role_policy_attachment" "gatling_attach_ec2_policy" {
  role       = "${aws_iam_role.gatling-ecs-task-execution-role.name}"
  policy_arn = "${aws_iam_policy.gatling-ecs-policy.arn}"
}

resource "aws_ecs_task_definition" "gatling" {
  family                = "${var.prefix}-gatling-runner"
  requires_compatibilities = ["FARGATE"]
  network_mode = "awsvpc"
  task_role_arn = "${aws_iam_role.gatling-ecs-task-execution-role.arn}"
  execution_role_arn = "${aws_iam_role.gatling-ecs-task-execution-role.arn}"
  cpu = "512"
  memory = "1024"
  container_definitions = <<EOF
[
  {
    "name": "gatling-runner",
    "essential": true,
    "image": "${aws_ecr_repository.gatling-runner-ecr.repository_url}",
    "environment": [
      { "name": "AWS_REGION", "value": "${var.aws_region}" },
      { "name": "TW_GATLING_SIMULATION_CLASS", "value": "com.chatwork.gaudiPoc.gatling.GaudiSimulation" },
      { "name": "TW_GATLING_EXECUTION_ID", "value": "default" },
      { "name": "TW_GATLING_USERS", "value": "1" },
      { "name": "TW_GATLING_RAMP_DURATION", "value": "8m" },
      { "name": "TW_GATLING_HOLD_DURATION", "value": "2m" },
      { "name": "TW_GATLING_S3_BUCKET_NAME", "value": "${var.gatling-s3-log-bucket-name}" },
      { "name": "TW_GATLING_RESULT_DIR", "value": "target/gatling" },
      { "name": "TW_GATLING_ENDPOINT", "value": "${var.api-base-url}" }
    ],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group":  "${aws_cloudwatch_log_group.gatling-log-group.name}",
        "awslogs-region": "ap-northeast-1",
        "awslogs-stream-prefix": "${var.prefix}-gatling-runner"
      }
    }
  }
]
EOF

}

resource "aws_ecs_task_definition" "gatling-s3-reporter" {
  family                = "${var.prefix}-gatling-s3-reporter"
  requires_compatibilities = ["FARGATE"]
  network_mode = "awsvpc"
  task_role_arn = "${aws_iam_role.gatling-ecs-task-execution-role.arn}"
  execution_role_arn = "${aws_iam_role.gatling-ecs-task-execution-role.arn}"
  cpu = "512"
  memory = "1024"
  container_definitions = <<EOF
[
  {
    "name": "gatling-runner",
    "essential": true,
    "image": "${aws_ecr_repository.gatling-s3-reporter-ecr.repository_url}",
    "environment": [
      { "name": "AWS_REGION", "value": "${var.aws_region}" },
      { "Name": "S3_GATLING_BUCKET_NAME", "Value": "${var.gatling-s3-log-bucket-name}" },
      { "Name": "S3_GATLING_RESULT_DIR_PATH", "Value": "target/gatling" },
    ],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group":  "${aws_cloudwatch_log_group.gatling-log-group.name}",
        "awslogs-region": "ap-northeast-1",
        "awslogs-stream-prefix": "${var.prefix}-gatling-s3-reporter"
      }
    }
  }
]
EOF

}
