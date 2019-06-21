variable "aws_region" {}
variable "aws_profile" {}
variable "vpc_id" {}
variable "prefix" {}

variable "ecs-cluster-name" {}

variable "cidr_block" {
  default = "10.2.1.0/24"
}

variable "availability_zone" {
  default = "ap-northeast-1b"
}

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