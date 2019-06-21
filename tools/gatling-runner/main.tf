resource "aws_subnet" "gatling-subnet" {
  vpc_id = "${var.vpc_id}"
  cidr_block = "${var.cidr_block}"
  availability_zone = "${var.availability_zone}"
  map_public_ip_on_launch = false
  tags {
    Name = "${var.prefix}-subnet-1b"
  }
}

resource "aws_internet_gateway" "gatling-public-route" {
  vpc_id = "${var.vpc_id}"

  tags = {
    Name = "${var.prefix}-internet-gw"
    Network = "Public"
  }
}

resource "aws_route_table" "gatling-route-table" {
  vpc_id = "${var.vpc_id}"

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = "${aws_internet_gateway.gatling-public-route.id}"
  }

  tags = {
    Name = "${var.prefix}-route-table"
    Network = "Public"
  }
}

resource "aws_route_table_association" "public" {
  subnet_id = "${aws_subnet.gatling-subnet.id}"
  route_table_id = "${aws_route_table.gatling-route-table.id}"
}

resource "aws_network_acl" "gatling-network-acl" {
  vpc_id = "${var.vpc_id}"
}

resource "aws_network_acl_rule" "inbound-ssh" {
  network_acl_id = "${aws_network_acl.gatling-network-acl.id}"
  rule_number = "10"
  protocol = 6
  rule_action = "allow"
  egress = false
  cidr_block = "0.0.0.0/0"
  from_port = 22
  to_port = 22
}

resource "aws_network_acl_rule" "inbound-http" {
  network_acl_id = "${aws_network_acl.gatling-network-acl.id}"
  rule_number = "20"
  protocol = 6
  rule_action = "allow"
  egress = false
  cidr_block = "0.0.0.0/0"
  from_port = 80
  to_port = 80
}

resource "aws_network_acl_rule" "inbound-any" {
  network_acl_id = "${aws_network_acl.gatling-network-acl.id}"
  rule_number = "100"
  protocol = -1
  rule_action = "allow"
  egress = false
  cidr_block = "0.0.0.0/0"
}

resource "aws_network_acl_rule" "outbound-any" {
  network_acl_id = "${aws_network_acl.gatling-network-acl.id}"
  rule_number = "100"
  protocol = -1
  rule_action = "allow"
  egress = true
  cidr_block = "0.0.0.0/0"
}

resource "aws_security_group" "allow-ssh-http" {
  name = "${var.prefix}-allow-ssh-http"
  description = "${var.prefix}-allow-ssh-http"
  vpc_id = "${var.vpc_id}"
}

resource "aws_security_group_rule" "allow-ssh" {
  type              = "ingress"
  from_port         = 21
  to_port           = 21
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = "${aws_security_group.allow-ssh-http.id}"
}

resource "aws_security_group_rule" "allow-http" {
  type              = "ingress"
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = "${aws_security_group.allow-ssh-http.id}"
}

resource "aws_security_group_rule" "allow-egress" {
  type              = "egress"
  from_port         = 0
  to_port           = 0
  protocol          = -1
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = "${aws_security_group.allow-ssh-http.id}"
}

