resource "aws_subnet" "db-private" {
  vpc_id            = "${var.aws_vpc_id}"
  count             = "${var.enabled ? length(var.db_private_subnets_availability_zones) : 0}"
  availability_zone = "${element(var.db_private_subnets_availability_zones, count.index)}"
  cidr_block        = "${element(var.db_private_subnets_cidr_blocks, count.index)}"

  tags = {
    Name  = "${var.prefix}-subnet-db-private-${element(var.db_private_subnets_availability_zones, count.index)}"
    Owner = "${var.owner}"
  }

  lifecycle {
    ignore_changes = ["tags"]
  }
}

resource "aws_security_group" "aurora" {
  count = "${var.enabled ? 1 : 0}"
  name   = "${var.prefix}-security-group-aurora"
  vpc_id = "${var.aws_vpc_id}"
  tags = {
    Name  = "${var.prefix}-security-group-aurora"
    Owner = "${var.owner}"
  }
}

## flyway実行時だけ利用 ---
resource "aws_route_table" "db_public" {
  count                  = "${var.enabled && var.aurora_public_access ? 1 : 0}"
  vpc_id = "${var.aws_vpc_id}"

  tags = {
    Name  = "${var.prefix}-route-table-db-public"
    Owner = "${var.owner}"
  }
}

resource "aws_route" "db_public" {
  count                  = "${var.enabled && var.aurora_public_access ? 1 : 0}"
  route_table_id         = "${aws_route_table.db_public.0.id}"
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = "${var.igw}"
}

resource "aws_route_table_association" "db_public" {
  count          = "${var.enabled && var.aurora_public_access ? length(aws_subnet.db-private) : 0}"
  route_table_id = "${aws_route_table.db_public.0.id}"
  subnet_id      = "${element(aws_subnet.db-private.*.id, count.index)}"
}

## ---

resource "aws_security_group_rule" "aurora" {
  count = "${var.enabled ? 1 : 0}"
  type              = "ingress"
  from_port         = 3306
  to_port           = 3306
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = "${aws_security_group.aurora.0.id}"
}

resource "aws_db_subnet_group" "aurora" {
  count = "${var.enabled ? 1 : 0}"
  name       = "${var.prefix}-db-subnet-group-aurora"
  subnet_ids = "${aws_subnet.db-private.*.id}"

  tags = {
    Name  = "${var.prefix}-db-subnet-group-aurora"
    Owner = "${var.owner}"
  }
}

resource "aws_db_parameter_group" "default" {
  count = "${var.enabled ? 1 : 0}"
  name        = "${var.prefix}-db-parameter-group-default"
  family      = "aurora5.6"
  description = "Managed by Terraform"

  tags = {
    Name  = "${var.prefix}-db-parameter-group-default"
    Owner = "${var.owner}"
  }
}

resource "aws_rds_cluster_parameter_group" "default" {
  count = "${var.enabled ? 1 : 0}"
  name        = "${var.prefix}-rds-cluster-parameter-group-default"
  family      = "aurora5.6"
  description = "Managed by Terraform"

  tags = {
    Name  = "${var.prefix}-rds-cluster-parameter-group-default"
    Owner = "${var.owner}"
  }
}

resource "aws_rds_cluster" "aurora" {
  count                           = "${var.enabled ? 1 : 0}"
  cluster_identifier              = "${var.prefix}-rds-cluster-aurora"
  database_name                   = "${var.aurora_db_name}"
  master_username                 = "${var.aurora_db_master_username}"
  master_password                 = "${var.aurora_db_master_password}"
  port                            = 3306
  vpc_security_group_ids          = ["${aws_security_group.aurora.0.id}"]
  db_subnet_group_name            = "${aws_db_subnet_group.aurora.0.name}"
  db_cluster_parameter_group_name = "${aws_rds_cluster_parameter_group.default.0.name}"

  tags = {
    Name  = "${var.prefix}-rds-cluster-aurora"
    Owner = "${var.owner}"
  }
}

resource "aws_rds_cluster_instance" "aurora" {
  count              = "${var.enabled ? var.nr_of_instances : 0}"
  identifier         = "${aws_rds_cluster.aurora.0.cluster_identifier}-${count.index}"
  cluster_identifier = "${aws_rds_cluster.aurora.0.id}"
  instance_class     = "${var.aurora_instance_type}"

  db_subnet_group_name    = "${aws_db_subnet_group.aurora.0.name}"
  db_parameter_group_name = "${aws_db_parameter_group.default.0.name}"

  publicly_accessible = "${var.aurora_public_access}"

  tags = {
    Name  = "${var.prefix}-rds-cluster-instance-aurora"
    Owner = "${var.owner}"
  }
}

