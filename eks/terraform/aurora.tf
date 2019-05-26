resource "aws_subnet" "aurora_private1" {
  vpc_id            = "${aws_vpc.main.id}"
  cidr_block        = "10.0.9.0/24"
  availability_zone = "ap-northeast-1a"
  tags {
    Name = "tf_private_db1"
  }
}

resource "aws_subnet" "aurora_private2" {
  vpc_id            = "${aws_vpc.main.id}"
  cidr_block        = "10.0.10.0/24"
  availability_zone = "ap-northeast-1c"
  tags {
    Name = "tf_private_db2"
  }
}

resource "aws_security_group" "aurora" {
  name        = "aurora"
  description = "It is a security group on db of tf_vpc."
  vpc_id      = "${aws_vpc.main.id}"
  tags {
    Name = "aurora-sg"
  }
}

## flyway実行時だけ利用 ---
resource "aws_route_table" "db_public" {
  count = "${var.aurora_public_access ? 1 : 0}"
  vpc_id = "${aws_vpc.main.id}"
  tags = {
    Name = "${local.prefix}-db-public-rtb"
  }
}

resource "aws_route" "db_public" {
  count = "${var.aurora_public_access ? 1 : 0}"
  route_table_id         = "${aws_route_table.db_public.id}"
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = "${aws_internet_gateway.gw.id}"
}

resource "aws_route_table_association" "db_public1" {
  count = "${var.aurora_public_access ? 1 : 0}"
  route_table_id = "${aws_route_table.db_public.id}"
  subnet_id      = "${aws_subnet.aurora_private1.id}"
}

resource "aws_route_table_association" "db_public2" {
  count = "${var.aurora_public_access ? 1 : 0}"
  route_table_id = "${aws_route_table.db_public.id}"
  subnet_id      = "${aws_subnet.aurora_private2.id}"
}
## ---

resource "aws_security_group_rule" "aurora" {
  type                     = "ingress"
  from_port                = 3306
  to_port                  = 3306
  protocol                 = "tcp"
  cidr_blocks              = ["0.0.0.0/0"]
  security_group_id        = "${aws_security_group.aurora.id}"
}

resource "aws_db_subnet_group" "aurora" {
  name        = "tf_dbsubnet"
  description = "It is a DB subnet group on tf_vpc."
  subnet_ids  = ["${aws_subnet.aurora_private1.id}", "${aws_subnet.aurora_private2.id}"]
  tags {
    Name = "tf_dbsubnet"
  }
}

resource "aws_db_parameter_group" "default" {
  name        = "j5ik2o-eks-thread-weaver-pg"
  family      = "aurora5.6"
  description = "Managed by Terraform"
}

resource "aws_rds_cluster_parameter_group" "default" {
  name        = "j5ik2o-eks-thread-weaver-cluster-pg"
  family      = "aurora5.6"
  description = "Managed by Terraform"
}

resource "aws_rds_cluster" "thread-weaver-aurora-cluster" {
  cluster_identifier = "j5ik2o-eks-thread-weaver-aurora-cluster"
  database_name = "${var.aurora_db_name}"
  master_username = "${var.aurora_db_master_username}"
  master_password = "${var.aurora_db_master_password}"
  port = 3306
  vpc_security_group_ids = ["${aws_security_group.aurora.id}"]
  db_subnet_group_name = "${aws_db_subnet_group.aurora.name}"
  db_cluster_parameter_group_name = "${aws_rds_cluster_parameter_group.default.name}"
}

resource "aws_rds_cluster_instance" "thread-weaver-aurora-instance" {
  count = 2
  identifier         = "${aws_rds_cluster.thread-weaver-aurora-cluster.cluster_identifier}-${count.index}"
  cluster_identifier = "${aws_rds_cluster.thread-weaver-aurora-cluster.id}"
  instance_class     = "${var.aurora_instance_type}"

  db_subnet_group_name = "${aws_db_subnet_group.aurora.name}"
  db_parameter_group_name = "${aws_db_parameter_group.default.name}"

  publicly_accessible = "${var.aurora_public_access}"
}


