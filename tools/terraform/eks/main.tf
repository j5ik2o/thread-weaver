terraform {
  required_version = ">= 0.12"
  backend "s3" {
    bucket = "j5ik2o-terraform-state" # 作成したS3バケット
    region = "ap-northeast-1"
    key = "eks.tfstate"
    profile = "cw-test"
    encrypt = true
  }
}

provider "aws" {
  region  = "${var.aws_region}"
  profile = "${var.aws_profile}"
}

data "aws_region" "current" {}

module "aws_vpc" {
  source = "../modules/vpc"

  aws_vpc_id = "${var.aws_vpc_id}"
  aws_az = "${var.aws_az}"
  aws_subnet_lb_cidr = "${var.aws_subnet_lb_cidr}"
  aws_subnet_eks_cidr = "${var.aws_subnet_eks_cidr}"
  aws_eks_cluster_name = "${var.aws_eks_cluster_name}"
}

module "aws_vpc_securitygroup" {
  source = "../modules/vpc-securitygroup"

  aws_vpc_id = "${module.aws_vpc.aws_vpc_id}"
  aws_subnet_ids_eks = "${module.aws_vpc.aws_subnet_ids_eks}"
  aws_eks_cluster_name = "${var.aws_eks_cluster_name}"
}

module "aws_iam" {
  source = "../modules/iam"

  aws_eks_cluster_name = "${var.aws_eks_cluster_name}"
}

module "aws_aurora" {
  source = "../modules/aurora"
  enabled = true
  aws_vpc_id = "${module.aws_vpc.aws_vpc_id}"
  db_private_subnets_availability_zones = "${var.db_private_subnets_availability_zones}"
  db_private_subnets_cidr_blocks = "${var.db_private_subnets_cidr_blocks}"
  igw = "${module.aws_vpc.aws_igw}"
  aurora_db_master_password = "${var.aurora_db_master_password}"
}

module "aws_dynamodb" {
  source = "../modules/dynamodb"
  enabled = true
  journal_table_name = "thread_weaver_journal"
  snapshot_table_name = "thread_weaver_snapshot"
}

data "template_file" "subnet_private" {
  template = "${file("${path.module}/../templates/cluster-subnet.tpl")}"
  count = "${length(var.aws_az)}"
  vars = {
    availability_zones = "${element(var.aws_az, count.index)}"
    subnet_ids = "${element(module.aws_vpc.aws_subnet_ids_eks, count.index)}"
    subnet_cidrs = "${element(var.aws_subnet_eks_cidr, count.index)}"
  }
}

data "template_file" "subnet_public" {
  template = "${file("${path.module}/../templates/cluster-subnet.tpl")}"
  count = "${length(var.aws_az)}"
  vars = {
    availability_zones = "${element(var.aws_az, count.index)}"
    subnet_ids = "${element(module.aws_vpc.aws_subnet_ids_lb, count.index)}"
    subnet_cidrs = "${element(var.aws_subnet_lb_cidr, count.index)}"
  }
}

data "template_file" "nodegroup" {
  template = "${file("${path.module}/../templates/cluster-nodegroup.tpl")}"
  vars = {
    name = "ng-worker"
    instance_type = "${var.node_instance_type}"
    private_networking = "false"
    security_groups = "${module.aws_vpc_securitygroup.eks_node_sg_id}"
    min_size = 3
    max_size = 3
    role = "worker"
    instance_profile_arn = "${module.aws_iam.node_group_aws_instance_profile_arn}"
    allow_ssh = "true"
    ssh_public_key_path = "./ssh_id_rsa.pub"
  }
}

data "template_file" "cluster" {
  template = "${file("${path.module}/../templates/cluster.yaml.tpl")}"

  vars = {
    eks_cluster_name = "${var.aws_eks_cluster_name}"
    region           = "${data.aws_region.current.name}"
    version          = "${var.k8s_version}"

    iam_service_role_arn = "${module.aws_iam.aws_eks_service_role_arn}"

    vpc_id = "${module.aws_vpc.aws_vpc_id}"
    subnets_private = "${join("\n", data.template_file.subnet_private.*.rendered)}"
    subnets_public  = "${join("\n", data.template_file.subnet_public.*.rendered)}"
    security_group = "${module.aws_vpc_securitygroup.eks_control_plane_sg_id}"
    shared_node_security_group = "${module.aws_vpc_securitygroup.eks_node_sg_id}"
    nodegroups  = "${join("\n", data.template_file.nodegroup.*.rendered)}"
  }
}

resource "null_resource" "cluster" {
  provisioner "local-exec" {
      command = "echo '${data.template_file.cluster.rendered}' > ${var.cluster_yaml_file}"
  }
  triggers = {
      template = "${data.template_file.cluster.rendered}"
  }
}
