resource "aws_subnet" "db-private" {
  vpc_id            = "${var.vpc_id}"
  count             = "${length(var.db_private_subnets_availability_zones)}"
  availability_zone = "${element(var.db_private_subnets_availability_zones, count.index)}"
  cidr_block        = "${element(var.db_private_subnets_cidr_blocks, count.index)}"

  tags = {
    Name    = "${var.prefix}-subnet-db-private-${element(var.db_private_subnets_availability_zones, count.index)}"
    Product = "j5ik2o"
  }

  lifecycle {
    ignore_changes = ["tags"]
  }
}

resource "aws_security_group" "aurora" {
  name   = "${var.prefix}-security-group-aurora"
  vpc_id = "${var.vpc_id}"
  tags = {
    Name  = "${var.prefix}-security-group-aurora"
    Owner = "j5ik2o"
  }
}

## flyway実行時だけ利用 ---
resource "aws_route_table" "db_public" {
  count  = "${var.aurora_public_access ? 1 : 0}"
  vpc_id = "${var.vpc_id}"

  tags = {
    Name  = "${var.prefix}-route-table-db-public"
    Owner = "j5ik2o"
  }
}

resource "aws_route" "db_public" {
  count                  = "${var.aurora_public_access ? 1 : 0}"
  route_table_id         = "${aws_route_table.db_public.0.id}"
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = "${aws_internet_gateway.gw.id}"
}

resource "aws_route_table_association" "db_public" {
  count          = "${var.aurora_public_access ? length(aws_subnet.private) : 0}"
  route_table_id = "${aws_route_table.db_public.0.id}"
  subnet_id      = "${element(aws_subnet.db-private.*.id, count.index)}"
}

## ---

resource "aws_security_group_rule" "aurora" {
  type              = "ingress"
  from_port         = 3306
  to_port           = 3306
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = "${aws_security_group.aurora.id}"
}

resource "aws_db_subnet_group" "aurora" {
  name       = "tf_dbsubnet"
  subnet_ids = "${aws_subnet.db-private.*.id}"

  tags = {
    Name  = "${var.prefix}-db-subnet-group-aurora"
    Owner = "j5ik2o"
  }
}

resource "aws_db_parameter_group" "default" {
  name        = "${var.prefix}-db-parameter-group-default"
  family      = "aurora5.6"
  description = "Managed by Terraform"

  tags = {
    Name  = "${var.prefix}-db-parameter-group-default"
    Owner = "j5ik2o"
  }
}

resource "aws_rds_cluster_parameter_group" "default" {
  name        = "${var.prefix}-rds-cluster-parameter-group-default"
  family      = "aurora5.6"
  description = "Managed by Terraform"

  tags = {
    Name  = "${var.prefix}-rds-cluster-parameter-group-default"
    Owner = "j5ik2o"
  }
}

resource "aws_rds_cluster" "aurora" {
  cluster_identifier              = "${var.prefix}-rds-cluster-aurora"
  database_name                   = "${var.aurora_db_name}"
  master_username                 = "${var.aurora_db_master_username}"
  master_password                 = "${var.aurora_db_master_password}"
  port                            = 3306
  vpc_security_group_ids          = ["${aws_security_group.aurora.id}"]
  db_subnet_group_name            = "${aws_db_subnet_group.aurora.name}"
  db_cluster_parameter_group_name = "${aws_rds_cluster_parameter_group.default.name}"

  tags = {
    Name  = "${var.prefix}-rds-cluster-aurora"
    Owner = "j5ik2o"
  }
}

resource "aws_rds_cluster_instance" "aurora" {
  count              = 2
  identifier         = "${aws_rds_cluster.aurora.cluster_identifier}-${count.index}"
  cluster_identifier = "${aws_rds_cluster.aurora.id}"
  instance_class     = "${var.aurora_instance_type}"

  db_subnet_group_name    = "${aws_db_subnet_group.aurora.name}"
  db_parameter_group_name = "${aws_db_parameter_group.default.name}"

  publicly_accessible = "${var.aurora_public_access}"

  tags = {
    Name  = "${var.prefix}-rds-cluster-instance-aurora"
    Owner = "j5ik2o"
  }
}

