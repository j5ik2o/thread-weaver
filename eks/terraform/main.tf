locals {
  prefix = "j5ik2o"
}

resource "aws_vpc" "main" {
  cidr_block       = "10.0.0.0/16"
  enable_dns_support = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.vpc_name}"
    Product = "j5ik2o"
  }

  lifecycle {
    ignore_changes = [
      "tags"
    ]
  }
}

# subnet
resource "aws_subnet" "public-subnet" {
  vpc_id                  = "${aws_vpc.main.id}"
  count                   = "${length(var.public_subnets_availability_zones)}"
  availability_zone       = "${element(var.public_subnets_availability_zones, count.index)}"
  cidr_block              = "${element(var.public_subnets_cidr_blocks, count.index)}"

  tags {
    Name = "${local.prefix}-public-${element(var.public_subnets_availability_zones, count.index)}"
    Product = "j5ik2o"
  }

  lifecycle {
    ignore_changes = [
      "tags"
    ]
  }
}

resource "aws_subnet" "private-subnet" {
  vpc_id                  = "${aws_vpc.main.id}"
  count                   = "${length(var.private_subnets_availability_zones)}"
  availability_zone       = "${element(var.private_subnets_availability_zones, count.index)}"
  cidr_block              = "${element(var.private_subnets_cidr_blocks, count.index)}"

  tags {
    Name = "${local.prefix}-private-${element(var.private_subnets_availability_zones, count.index)}"
    Product = "j5ik2o"
  }

  lifecycle {
    ignore_changes = [
      "tags"
    ]
  }
}

# gateway
resource "aws_internet_gateway" "gw" {
  vpc_id = "${aws_vpc.main.id}"

  tags = {
    Name = "${local.prefix}-gw"
    Product = "j5ik2o"
  }
}

resource "aws_eip" "nat" {
  vpc      = true
}

resource "aws_nat_gateway" "nat" {
  allocation_id = "${aws_eip.nat.id}"
  subnet_id     = "${aws_subnet.public-subnet.0.id}"

  depends_on = ["aws_internet_gateway.gw"]

  tags = {
    Name = "${local.prefix}-nat-gw"
    Product = "j5ik2o"
  }
}

# Private Route Table
resource "aws_route_table" "private" {
  vpc_id = "${aws_vpc.main.id}"

  tags = {
    Name = "${local.prefix}-private-rtb"
  }
}

resource "aws_route" "private" {
  route_table_id         = "${aws_route_table.private.id}"
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = "${aws_nat_gateway.nat.id}"
}

resource "aws_route_table_association" "private" {
  count          = "${aws_subnet.private-subnet.count}"
  route_table_id = "${aws_route_table.private.id}"
  subnet_id      = "${element(aws_subnet.private-subnet.*.id, count.index)}"
}

resource "aws_route_table" "public" {
  vpc_id = "${aws_vpc.main.id}"

  tags = {
    Name = "${local.prefix}-public-rtb"
  }
}

resource "aws_route" "public" {
  route_table_id         = "${aws_route_table.public.id}"
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = "${aws_internet_gateway.gw.id}"
}

resource "aws_route_table_association" "public" {
  count          = "${aws_subnet.public-subnet.count}"
  route_table_id = "${aws_route_table.public.id}"
  subnet_id      = "${element(aws_subnet.public-subnet.*.id, count.index)}"
}