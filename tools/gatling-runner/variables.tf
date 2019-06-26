variable "aws_region" {}
variable "aws_profile" {}

variable "prefix" {}

variable "ecs-cluster-name" {}

variable "gatling-s3-log-bucket-name" {
  default = "gatling-logs"
}

variable "gatling-runner-ecr-name" {}

variable "gatling-s3-reporter-ecr-name" {}

variable "api-base-url" {}

