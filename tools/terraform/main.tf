# subnet
resource "aws_subnet" "public" {
  vpc_id            = "${var.vpc_id}"
  count             = "${length(var.public_subnets_availability_zones)}"
  availability_zone = "${element(var.public_subnets_availability_zones, count.index)}"
  cidr_block        = "${element(var.public_subnets_cidr_blocks, count.index)}"

  tags = {
    Name    = "${var.prefix}-subnet-public-${element(var.public_subnets_availability_zones, count.index)}"
    Owner = "${var.owner}"
  }

  lifecycle {
    ignore_changes = ["tags"]
  }
}

resource "aws_subnet" "private" {
  vpc_id            = "${var.vpc_id}"
  count             = "${length(var.private_subnets_availability_zones)}"
  availability_zone = "${element(var.private_subnets_availability_zones, count.index)}"
  cidr_block        = "${element(var.private_subnets_cidr_blocks, count.index)}"

  tags = {
    Name    = "${var.prefix}-subnet-private-${element(var.private_subnets_availability_zones, count.index)}"
    Owner = "${var.owner}"
  }

  lifecycle {
    ignore_changes = ["tags"]
  }
}

# gateway
resource "aws_internet_gateway" "gw" {
  vpc_id = "${var.vpc_id}"

  tags = {
    Name  = "${var.prefix}-internet-gateway"
    Owner = "${var.owner}"
  }
}

resource "aws_eip" "nat" {
  vpc = true
  tags = {
    Name  = "${var.prefix}-eip-nat"
    Owner = "${var.owner}"
  }
}

resource "aws_nat_gateway" "nat" {
  allocation_id = "${aws_eip.nat.id}"
  subnet_id     = "${aws_subnet.public.0.id}"

  depends_on = ["aws_internet_gateway.gw"]

  tags = {
    Name  = "${var.prefix}-nat-gateway"
    Owner = "${var.owner}"
  }
}

# Private Route Table
resource "aws_route_table" "private" {
  vpc_id = "${var.vpc_id}"

  tags = {
    Name  = "${var.prefix}-route-table-private"
    Owner = "${var.owner}"
  }
}

resource "aws_route" "private" {
  route_table_id         = "${aws_route_table.private.id}"
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = "${aws_nat_gateway.nat.id}"
}

resource "aws_route_table_association" "private" {
  count          = "${length(aws_subnet.private)}"
  route_table_id = "${aws_route_table.private.id}"
  subnet_id      = "${element(aws_subnet.private.*.id, count.index)}"
}

# Public Route Table
resource "aws_route_table" "public" {
  vpc_id = "${var.vpc_id}"

  tags = {
    Name  = "${var.prefix}-route-table-public"
    Owner = "${var.owner}"
  }
}

resource "aws_route" "public" {
  route_table_id         = "${aws_route_table.public.id}"
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = "${aws_internet_gateway.gw.id}"
}

resource "aws_route_table_association" "public" {
  count          = "${length(aws_subnet.public)}"
  route_table_id = "${aws_route_table.public.id}"
  subnet_id      = "${element(aws_subnet.public.*.id, count.index)}"
}

