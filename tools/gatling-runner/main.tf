resource "aws_subnet" "thread-weaver-subnet-1b" {
  vpc_id = "${var.vpc_id}"
  cidr_block = "10.2.1.0/24"
  availability_zone = "ap-northeast-1b"
  map_public_ip_on_launch = false
  tags {
    Name = "${var.prefix}-subnet-1b"
  }
}

resource "aws_internet_gateway" "thread-weaver-public-route" {
  vpc_id = "${var.vpc_id}"

  tags = {
    Name = "${var.prefix}-internet-gw"
    Network = "Public"
  }
}

resource "aws_route_table" "thread-weaver-route-table" {
  vpc_id = "${var.vpc_id}"

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
  vpc_id = "${var.vpc_id}"
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
  vpc_id = "${var.vpc_id}"

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

resource "aws_ecs_cluster" "ecs-cluster" {
  name = "${var.ecs-cluster-name}"
  tags = {
    Name = "${var.ecs-cluster-name}"
  }
}

data "template_file" "cloud-config" {
  template = "${file("init.tpl")}"
}

// S3PrivateRegistryBucketNameあるときだけにしたい
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
echo ECS_CLUSTER=${aws_ecs_cluster.ecs-cluster.name} >> /etc/ecs/ecs.config
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
    content_type = "text/x-shellscript"
    content = "${data.template_file.add-instance-to-cluster.rendered}"
  }

}


data "template_cloudinit_config" "config-with-private" {
  gzip = true
  base64_encode = true

  # Main cloud-config configuration file.
  part {
    filename = "init.cfg"
    content_type = "text/cloud-config"
    content = "${data.template_file.cloud-config.rendered}"
  }

  part {
    content_type = "text/x-shellscript"
    content = "${data.template_file.get-docker-private-repository-credentials.rendered}"
  }

  part {
    content_type = "text/x-shellscript"
    content = "${data.template_file.add-instance-to-cluster.rendered}"
  }

}

resource "aws_launch_configuration" "thread-weaver" {
  image_id = "ami-06cd52961ce9f0d85"
  instance_type = "t2.micro"
  key_name = "j5ik2o-root"
  associate_public_ip_address = true
  security_groups = [
    "${aws_security_group.allow-ssh-http.id}"]
  iam_instance_profile = "${aws_iam_instance_profile.gatling.name}"
  user_data_base64 = "${var.S3PrivateRegistryBucketName == "" ? data.template_cloudinit_config.config.rendered : data.template_cloudinit_config.config-with-private.rendered}"
}

resource "aws_autoscaling_group" "thread-weaver" {
  name = "thread-weaver-gatling"
  vpc_zone_identifier = [
    "${aws_subnet.thread-weaver-subnet-1b.id}"]
  launch_configuration = "${aws_launch_configuration.thread-weaver.name}"
  min_size = 1
  max_size = "${var.MaxSize}"
  desired_capacity = "${var.DesiredCapacity}"

}


resource "aws_iam_role" "ecs-service" {
  path = "/"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": {
    "Effect": "Allow",
    "Action": [ "sts:AssumeRole" ],
    "Principal": {
      "Service": [ "ecs.amazonaws.com" ]
    }
  }
}
EOF
  tags = {
    Name = "${var.prefix}-iam-role-ecs-service"
  }
}

resource "aws_iam_role_policy" "ecs-service" {
  role = "${aws_iam_role.ecs-service.id}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "elasticloadbalancing:Describe*",
        "elasticloadbalancing:DeregisterInstancesFromLoadBalancer",
        "elasticloadbalancing:RegisterInstancesWithLoadBalancer",
        "ec2:Describe*",
        "ec2:AuthorizeSecurityGroupIngress"
      ],
      "Resource": "*"
    }
  ]
}
EOF
}

resource "aws_iam_role" "ec2" {
  path = "/"
  assume_role_policy = <<EOF
{
  "Version":"2008-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      }
    }
  ]
}
EOF
  tags = {
    Name = "${var.prefix}-route-iam-role-ec2"
  }
}

resource "aws_iam_role_policy" "ec2" {
  role = "${aws_iam_role.ec2.id}"
  count = "${var.S3PrivateRegistryBucketName == "" ? 1 : 0}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecs:CreateCluster",
        "ecs:RegisterContainerInstance",
        "ecs:DeregisterContainerInstance",
        "ecs:DiscoverPollEndpoint",
        "ecs:Submit*",
        "ecs:Poll"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListAllMyBuckets",
        "s3:GetBucketLocation",
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::*"
    },
    {
      "Effect": "Allow",
      "Action": "s3:*",
      "Resource": [
        "arn:aws:s3:::firebase-load-testing-logs",
        "arn:aws:s3:::firebase-load-testing-logs/*"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_role_policy" "ec2-with-private" {
  role = "${aws_iam_role.ec2.id}"
  count = "${var.S3PrivateRegistryBucketName != "" ? 1 : 0}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecs:CreateCluster",
        "ecs:RegisterContainerInstance",
        "ecs:DeregisterContainerInstance",
        "ecs:DiscoverPollEndpoint",
        "ecs:Submit*",
        "ecs:Poll"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [ "s3:GetObject" ],
      "Sid": "Stmt0123456789",
      "Resource": [
        "arn:aws:s3:::${var.S3PrivateRegistryBucketName}/ecs.config"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListAllMyBuckets",
        "s3:GetBucketLocation",
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::*"
    },
    {
      "Effect": "Allow",
      "Action": "s3:*",
      "Resource": [
        "arn:aws:s3:::firebase-load-testing-logs",
        "arn:aws:s3:::firebase-load-testing-logs/*"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_instance_profile" "gatling" {
  name = "${var.prefix}-gatling"
  path = "/"
  role = "${aws_iam_role.ec2.name}"
}

//resource "aws_ecs_task_definition" "random-bot-simulation" {
//  family                = "random-bot-simulation"
//  container_definitions = <<EOF
//[
//  {
//    "name": "thread-weaver-gatling-runner",
//    "cpu": "512",
//    "memory": "497",
//    "essential": true,
//    "image": "thread-weaver/falcon-gatling-runner:1.0.0-SNAPSHOT",
//    "environment": [
//      { "name": "FALCON_GATLING_SIMULATION_CLASS", "value": "com.chatwork.falcon.gatling.RandomBotSimulation" },
//      { "name": "FALCON_GATLING_WRITE_BASE_URL", "value": "${var.GatlingWriteBaseURL}" },
//      { "name": "FALCON_GATLING_READ_BASE_URL", "value": "${var.GatlingReadBaseURL}" },
//      { "name": "FALCON_GATLING_S3_BUCKET_NAME", "value": "${var.S3GatlingLogBucketName}" }
//    ]
//  }
//]
//EOF
//
//}

