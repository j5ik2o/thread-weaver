variable "aws_region" {}
variable "aws_profile" {}
variable "vpc_id" {}
variable "prefix" {}

variable "ecs-cluster-name" {}

variable "S3PrivateRegistryBucketName" {
  default = ""
}

variable "MaxSize" {
  default = 1
}

variable "DesiredCapacity" {
  default = 1
}

variable "S3GatlingLogBucketName" {
  default = "thread-weaver-gatling"
}

variable "GatlingWriteBaseURL" {
  default = ""
}

variable "GatlingReadBaseURL" {
  default = ""
}