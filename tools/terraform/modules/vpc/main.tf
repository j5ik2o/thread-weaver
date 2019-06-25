resource "aws_internet_gateway" "vpc" {
  vpc_id = "${var.aws_vpc_id}"

  tags = "${merge(local.default_tags, map("Name", "main"))}"
}

resource "aws_route_table" "public" {
  vpc_id = "${var.aws_vpc_id}"

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = "${aws_internet_gateway.vpc.id}"
  }

  tags = "${merge(local.default_tags, map("Name", "route_table_public"))}"
}

resource "aws_subnet" "lb" {
  count = "${length(var.aws_az)}"
  availability_zone = "${element(var.aws_az, count.index)}"
  cidr_block = "${element(var.aws_subnet_lb_cidr, count.index)}"
  map_public_ip_on_launch = false
  assign_ipv6_address_on_creation = false
  vpc_id     = "${var.aws_vpc_id}"

  tags = "${merge(local.default_tags, map("Name", "lb"))}"
}

resource "aws_subnet" "eks" {
  count = "${length(var.aws_az)}"
  availability_zone = "${element(var.aws_az, count.index)}"
  cidr_block = "${element(var.aws_subnet_eks_cidr, count.index)}"
  map_public_ip_on_launch = false
  assign_ipv6_address_on_creation = false
  vpc_id     = "${var.aws_vpc_id}"

  tags = "${merge(local.default_tags, map("Name", "eks"))}"
}

resource "aws_route_table_association" "lb" {
  count = "${length(var.aws_az)}"
  subnet_id      = "${element(aws_subnet.lb.*.id, count.index)}"
  route_table_id = "${aws_route_table.public.id}"
}

resource "aws_route_table_association" "eks" {
  count = "${length(var.aws_az)}"
  subnet_id      = "${element(aws_subnet.eks.*.id, count.index)}"
  route_table_id = "${aws_route_table.public.id}"
}
