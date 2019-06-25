variable "aws_region" {
  default = "ap-northeast-1"
}

variable "aws_profile" {
}

variable "owner" {
  default = "j5ik2o"
}

variable "prefix" {
  default = "j5ik2o"
}

variable "vpc_id" {
}

variable "public_subnets_availability_zones" {
  type    = list
  default = ["ap-northeast-1a", "ap-northeast-1c", "ap-northeast-1d"]
}

variable "public_subnets_cidr_blocks" {
  type    = list
  default = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
}

variable "private_subnets_availability_zones" {
  type    = list
  default = ["ap-northeast-1a", "ap-northeast-1c", "ap-northeast-1d"]
}

variable "private_subnets_cidr_blocks" {
  type    = list
  default = ["10.0.4.0/24", "10.0.5.0/24", "10.0.6.0/24"]
}

variable "db_private_subnets_availability_zones" {
  type    = list
  default = ["ap-northeast-1a", "ap-northeast-1c"]
}

variable "db_private_subnets_cidr_blocks" {
  type    = list
  default = ["10.0.7.0/24", "10.0.8.0/24"]
}

variable "aurora_instance_type" {
  default = "db.t2.medium"
}

variable "num_of_db_instance" {
  default = 2
}

variable "aurora_db_name" {
  default = "thread_weaver"
}

variable "aurora_db_master_username" {
  default = "thread_weaver"
}

variable "aurora_db_master_password" {
}

variable "aurora_public_access" {
  default = false
}

