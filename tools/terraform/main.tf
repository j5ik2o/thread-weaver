terraform {
  required_version = ">= 0.12"
  backend "s3" {}
}

provider "aws" {
  region  = "${var.aws_region}"
  profile = "${var.aws_profile}"
}

data "aws_availability_zones" "available" {
}

locals {
  cluster_name = "${var.eks_cluster_name}-${random_string.suffix.result}"
}

resource "random_string" "suffix" {
  length  = 8
  special = false
}

data "aws_security_group" "default" {
  name   = "default"
  vpc_id = module.vpc.vpc_id
}

module "vpc" {
  source = "terraform-aws-modules/vpc/aws"

  name = "${var.vpc_name}"
  cidr = "${var.vpc_cidr}"

  azs              = data.aws_availability_zones.available.names
  private_subnets  = "${var.aws_subnet_public}"
  public_subnets   = "${var.aws_subnet_private}"
  database_subnets = "${var.aws_subnet_db}"

  enable_dns_hostnames = true
  enable_dns_support   = true

  enable_nat_gateway = true
  enable_vpn_gateway = true

  tags = {
    "kubernetes.io/cluster/${local.cluster_name}" = "shared"
  }

  public_subnet_tags = {
    "kubernetes.io/cluster/${local.cluster_name}" = "shared"
  }

  private_subnet_tags = {
    "kubernetes.io/cluster/${local.cluster_name}" = "shared"
    "kubernetes.io/role/internal-elb"             = "true"
  }
}


module "eks" {
  source       = "terraform-aws-modules/eks/aws"
  cluster_name = local.cluster_name
  subnets      = module.vpc.private_subnets
  kubeconfig_aws_authenticator_env_variables = {
    AWS_PROFILE = "${var.aws_profile}"
  }
  tags = {
    Environment = "${var.eks_cluster_name}"
    GithubRepo  = "terraform-aws-eks"
    GithubOrg   = "terraform-aws-modules"
  }

  vpc_id = module.vpc.vpc_id

  worker_groups = [
    {
      name                 = "node-group-1"
      instance_type        = var.eks_node_instance_type
      asg_desired_capacity = var.eks_asg_desired_capacity
    },
  ]
  workers_additional_policies = [
    "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess",
    "arn:aws:iam::aws:policy/AmazonRDSFullAccess"
  ]
  config_output_path = "./config/"
}

module "ecr_api_server" {
  source   = "./ecr"
  prefix   = "${var.prefix}"
  owner    = "${var.owner}"
  enabled  = true
  ecr_name = "j5ik2o/thread-weaver-api-server"
}

module "ecr_flyway" {
  source   = "./ecr"
  prefix   = "${var.prefix}"
  owner    = "${var.owner}"
  enabled  = true
  ecr_name = "j5ik2o/thread-waever-flyway"
}


module "aurora" {
  source                              = "terraform-aws-modules/rds-aurora/aws"
  name                                = "${var.prefix}-rds-cluster-aurora"
  engine                              = "aurora-mysql"
  engine_version                      = "5.7.12"
  subnets                             = module.vpc.database_subnets
  vpc_id                              = "${module.vpc.vpc_id}"
  replica_count                       = 1
  database_name                       = "${var.aurora_database_name}"
  username                            = "${var.aurora_username}"
  password                            = "${var.aurora_password}"
  instance_type                       = "${var.aurora_instance_type}"
  allowed_security_groups             = ["${module.eks.worker_security_group_id}"]
  allowed_security_groups_count       = 1
  apply_immediately                   = true
  skip_final_snapshot                 = true
  db_parameter_group_name             = "${aws_db_parameter_group.aurora_db_57_parameter_group.id}"
  db_cluster_parameter_group_name     = "${aws_rds_cluster_parameter_group.aurora_57_cluster_parameter_group.id}"
  iam_database_authentication_enabled = true
  enabled_cloudwatch_logs_exports     = ["audit", "error", "general", "slowquery"]
  monitoring_interval                 = 10
}

resource "aws_db_parameter_group" "aurora_db_57_parameter_group" {
  name        = "test-aurora-db-57-parameter-group"
  family      = "aurora-mysql5.7"
  description = "test-aurora-db-57-parameter-group"
}

resource "aws_rds_cluster_parameter_group" "aurora_57_cluster_parameter_group" {
  name        = "test-aurora-57-cluster-parameter-group"
  family      = "aurora-mysql5.7"
  description = "test-aurora-57-cluster-parameter-group"
}

module "dynamodb_journal_table" {
  source         = "git::https://github.com/cloudposse/terraform-aws-dynamodb.git?ref=master"
  enabled        = true
  namespace      = "${var.prefix}"
  name           = "${var.aws_dyanmodb_journal_table_name}"
  hash_key       = "pkey"
  hash_key_type  = "S"
  range_key      = "sequence-nr"
  range_key_type = "N"

  dynamodb_attributes = [
    {
      name = "persistence-id"
      type = "S"
    },
    {
      name = "sequence-nr"
      type = "N"
    },
    {
      name = "tags"
      type = "S"
    }
  ]

  global_secondary_index_map = [
    {
      name               = "GetJournalRowsIndex"
      hash_key           = "persistence-id"
      range_key          = "sequence-nr"
      write_capacity     = 5
      read_capacity      = 20
      projection_type    = "ALL"
      non_key_attributes = [] // ["pkey", "tags"]
    },
    {
      name               = "TagsIndex"
      hash_key           = "tags"
      range_key          = ""
      write_capacity     = 5
      read_capacity      = 20
      projection_type    = "ALL"
      non_key_attributes = [] // ["pkey", "persistence-d", "sequence-id"]
    }
  ]

  enable_autoscaler            = true
  autoscale_write_target       = 50
  autoscale_read_target        = 50
  autoscale_min_read_capacity  = 1000
  autoscale_max_read_capacity  = 2000
  autoscale_min_write_capacity = 60
  autoscale_max_write_capacity = 120
}

module "dynamodb_snapshot_table" {
  source         = "git::https://github.com/cloudposse/terraform-aws-dynamodb.git?ref=master"
  enabled        = true
  namespace      = "${var.prefix}"
  name           = "${var.aws_dyanmodb_snapshot_table_name}"
  hash_key       = "persistence-id"
  hash_key_type  = "S"
  range_key      = "sequence-nr"
  range_key_type = "N"

  enable_autoscaler            = true
  autoscale_write_target       = 50
  autoscale_read_target        = 50
  autoscale_min_read_capacity  = 5
  autoscale_max_read_capacity  = 20
  autoscale_min_write_capacity = 5
  autoscale_max_write_capacity = 20
}

module "gatling" {
  source     = "./gatling"
  enabled    = true
  aws_region = "${var.aws_region}"
  prefix     = "${var.prefix}"
  owner      = "${var.owner}"

  gatling_ecs_cluster_name          = "${var.gatling_ecs_cluster_name}"
  gatling_runner_ecr_name           = "${var.gatling_runner_ecr_name}"
  gatling_aggregate_runner_ecr_name = "${var.gatling_aggregate_runner_ecr_name}"

  gatling_s3_reporter_ecr_name = "${var.gatling_s3_reporter_ecr_name}"
  gatling_s3_log_bucket_name   = "${var.gatling_s3_log_bucket_name}"

  gatling_dd_api_key = "${var.gatling_dd_api_key}"
}
