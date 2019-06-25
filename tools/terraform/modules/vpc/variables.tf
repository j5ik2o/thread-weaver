
variable "aws_vpc_id" {
  type = "string"
}

variable "aws_az" {
  description = "AWS Availability Zones Used"
  type = "list"
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

locals {
  default_tags = "${map("kubernetes.io/cluster/${var.aws_eks_cluster_name}", "shared")}"
}
