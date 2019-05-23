variable "vpc_name" {
  default = "eks"
}

variable "public_subnets_availability_zones" {
  type = "list"
  default = ["ap-northeast-1a", "ap-northeast-1c", "ap-northeast-1d"]
}

variable "public_subnets_cidr_blocks" {
  type = "list"
  default = ["10.0.1.0/24","10.0.2.0/24", "10.0.3.0/24"]
}

variable "private_subnets_availability_zones" {
  type = "list"
  default = ["ap-northeast-1a", "ap-northeast-1c", "ap-northeast-1d"]
}

variable "private_subnets_cidr_blocks" {
  type = "list"
  default = ["10.0.4.0/24","10.0.5.0/24", "10.0.6.0/24"]
}

