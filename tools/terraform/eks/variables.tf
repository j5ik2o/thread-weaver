variable "aws_region" {
  default = "ap-northeast-1"
}

variable "access_key" {}
variable "secret_key" {}

variable "aws_profile" {
}

variable "aws_vpc_id" {}

variable "aws_az" {
  type = "list"
}

variable "owner" {
  default = "j5ik2o"
}

variable "aws_subnet_lb_cidr" {
  description = "CIDR Blocks for Load Balancer Public Subnet"
  type = "list"
}

variable "aws_subnet_eks_cidr" {
  description = "CIDR Blocks for EKS Public Subnet"
  type = "list"
}

variable "aws_eks_cluster_name" {
  description = "AWS EKS Cluster Name"
  type = "string"
}

variable "k8s_version" {
  default = "1.12"
}

variable "node_instance_type" {
  default = "t2.medium"
}

variable "api-server-ecr-name" {

}

variable "cluster_yaml_file" {
  description = "Where to store the generated cluster yaml file"
  type = "string"
}

variable "db_private_subnets_availability_zones" {
  type    = list
  default = ["ap-northeast-1a", "ap-northeast-1c"]
}

variable "db_private_subnets_cidr_blocks" {
  type    = list
  default = ["10.0.7.0/24", "10.0.8.0/24"]
}

variable "aurora_db_master_password" {}