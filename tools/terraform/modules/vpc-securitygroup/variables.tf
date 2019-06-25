variable "aws_vpc_id" {
  description = "AWS VPC id"
  type = "string"
}

variable "aws_subnet_ids_eks" {
  description = "EKS Subnet ids for AWS VPC"
  type = "list"
}

variable "aws_eks_cluster_name" {
  description = "AWS EKS Cluster Name"
  type = "string"
}
