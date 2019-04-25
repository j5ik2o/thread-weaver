resource "aws_vpc" "thread-weaver-vpc" {
  cidr_block = "10.2.0.0/16"
  enable_dns_support = true
  enable_dns_hostnames = true
  instance_tenancy = "default"

  tags {
    Name = "thread-weaver-vpc"
    Network = "Public"
  }
}

resource "aws_vpc_dhcp_options" "thread-weaver-dhcp-options" {
  domain_name_servers = [
    "8.8.8.8",
    "8.8.4.4"]
}

resource "aws_subnet" "thread-weaver-subnet-1b" {
  vpc_id = "${aws_vpc.thread-weaver-vpc.id}"
  cidr_block = "10.2.1.0/24"
  availability_zone = "ap-northeast-1b"
  map_public_ip_on_launch = false
  tags {
    Name = "thread-weaver-subnet-1b"
  }
}

resource "aws_internet_gateway" "thread-weaver-public-route" {
  vpc_id = "${aws_vpc.thread-weaver-vpc.id}"

  tags = {
    Name = "main"
    Network = "Public"
  }
}

resource "aws_route_table" "thread-weaver-route-table" {
  vpc_id = "${aws_vpc.thread-weaver-vpc.id}"

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = "${aws_internet_gateway.thread-weaver-public-route.id}"
  }

  tags = {
    Name = "thread-weaver-route-table"
    Network = "Public"
  }
}

resource "aws_route_table_association" "public" {
  subnet_id = "${aws_subnet.thread-weaver-subnet-1b.id}"
  route_table_id = "${aws_route_table.thread-weaver-route-table.id}"
}

resource "aws_network_acl" "thread-weaver-network-acl" {
  vpc_id = "${aws_vpc.thread-weaver-vpc.id}"
}

resource "aws_network_acl_rule" "inbound-ssh" {
  network_acl_id = "${aws_network_acl.thread-weaver-network-acl.id}"
  rule_number = "10"
  protocol = 6
  rule_action = "allow"
  egress = false
  cidr_block = "0.0.0.0/0"
  from_port = 22
  to_port = 22
}

resource "aws_network_acl_rule" "inbound-http" {
  network_acl_id = "${aws_network_acl.thread-weaver-network-acl.id}"
  rule_number = "20"
  protocol = 6
  rule_action = "allow"
  egress = false
  cidr_block = "0.0.0.0/0"
  from_port = 80
  to_port = 80
}

resource "aws_network_acl_rule" "inbound-any" {
  network_acl_id = "${aws_network_acl.thread-weaver-network-acl.id}"
  rule_number = "100"
  protocol = -1
  rule_action = "allow"
  egress = false
  cidr_block = "0.0.0.0/0"
}

resource "aws_network_acl_rule" "outbound-any" {
  network_acl_id = "${aws_network_acl.thread-weaver-network-acl.id}"
  rule_number = "100"
  protocol = -1
  rule_action = "allow"
  egress = true
  cidr_block = "0.0.0.0/0"
}

resource "aws_security_group" "allow-ssh-http" {
  name = "allow-ssh-http"
  description = "allow-ssh-http"
  vpc_id = "${aws_vpc.thread-weaver-vpc.id}"

  ingress {
    from_port = 80
    to_port = 80
    protocol = "tcp"
    cidr_blocks = [
      "0.0.0.0/0"]
  }
  ingress {
    from_port = 22
    to_port = 22
    protocol = "tcp"
    cidr_blocks = [
      "0.0.0.0/0"]
  }
  egress {
    from_port = 0
    to_port = 0
    protocol = -1
    cidr_blocks = [
      "0.0.0.0/0"]
  }
}

resource "aws_ecs_cluster" "thread-weaver-ecs-cluster" {
  name = "thread-weaver-route-cluster"
  tags = {
    Name = "thread-weaver-route-cluster"
  }
}

data "template_file" "cloud-config" {
  template = "${file("init.tpl")}"
}

data "template_file" "get-docker-private-repository-credentials" {
  template = <<EOT
#!/bin/bash
yum install -y aws-cli
aws s3 cp s3://${var.S3PrivateRegistryBucketName}/ecs.config /etc/ecs/ecs.config
EOT
}

data "template_file" "add-instance-to-cluster" {
  template = <<EOT
#!/bin/bash
echo ECS_CLUSTER=${aws_ecs_cluster.thread-weaver-ecs-cluster.name} >> /etc/ecs/ecs.config
EOT
}


data "template_cloudinit_config" "config" {
  gzip = true
  base64_encode = true

  # Main cloud-config configuration file.
  part {
    filename = "init.cfg"
    content_type = "text/cloud-config"
    content = "${data.template_file.cloud-config.rendered}"
  }

  part {
    count = "${var.S3PrivateRegistryBucketName != "" ? 1 : 0 }"
    content_type = "text/x-shellscript"
    content = "${data.template_file.get-docker-private-repository-credentials.rendered}"
  }

  part {
    content_type = "text/x-shellscript"
    content = "${data.template_file.add-instance-to-cluster.rendered}"
  }
}

# Start an AWS instance with the cloud-init config as user data
resource "aws_instance" "web" {
  ami = "ami-06cd52961ce9f0d85"
  instance_type = "t2.micro"
  key_name = "j5ik2o-root"
  subnet_id = "${aws_subnet.thread-weaver-subnet-1b.id}"
  vpc_security_group_ids = [
    "${aws_security_group.allow-ssh-http.id}"]
  user_data_base64 = "${data.template_cloudinit_config.config.rendered}"
}

resource "aws_eip" "web" {
  instance = "${aws_instance.web.id}"
  vpc = true
}


output "vpc_id" {
  value = "${aws_vpc.thread-weaver-vpc.id}"
}