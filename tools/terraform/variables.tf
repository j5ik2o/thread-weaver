variable "aws_region" {
  default = "ap-northeast-1"
}

variable "aws_profile" {
}

variable "prefix" {}

variable "owner" {}

variable "aws_az" {
  type = "list"
}

variable "vpc_name" {}

variable "vpc_cidr" {
  default = "10.0.0.0/16"
}

variable "aws_subnet_public" {
  type = "list"
}

variable "aws_subnet_private" {
  type = "list"
}

variable "aws_subnet_db" {
  type = "list"
}

variable "api_server_ecr_name" {}

variable "fly_job_ecr_name" {}

variable "eks_cluster_name" {}

variable "eks_node_instance_type" {
  default = "t2.medium"
}

variable "eks_asg_desired_capacity" {
  default = 3
}

variable "aurora_instance_type" {
  default = "db.t2.medium"
}
variable "aurora_database_name" {}
variable "aurora_username" {}
variable "aurora_password" {}

variable "aws_dyanmodb_journal_table_name" {}
variable "aws_dyanmodb_snapshot_table_name" {}

variable "gatling_ecs_cluster_name" {}
variable "gatling_s3_log_bucket_name" {}

variable "gatling_runner_ecr_name" {}
variable "gatling_s3_reporter_ecr_name" {}
variable "gatling_aggregate_runner_ecr_name" {}
variable "gatling_dd_api_key" {}

variable "map_accounts" {
  description = "Additional AWS account numbers to add to the aws-auth configmap."
  type        = "list"

  default = [
    "777777777777",
    "888888888888",
  ]
}

variable "map_roles" {
  description = "Additional IAM roles to add to the aws-auth configmap."
  type        = "list"

  default = [
    {
      role_arn = "arn:aws:iam::66666666666:role/role1"
      username = "role1"
      group    = "system:masters"
    },
  ]
}

variable "map_users" {
  description = "Additional IAM users to add to the aws-auth configmap."
  type        = "list"

  default = [
    {
      user_arn = "arn:aws:iam::66666666666:user/user1"
      username = "user1"
      group    = "system:masters"
    },
    {
      user_arn = "arn:aws:iam::66666666666:user/user2"
      username = "user2"
      group    = "system:masters"
    },
  ]
}