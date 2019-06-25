resource "aws_iam_role" "eks_service_role" {
  name = "${var.aws_eks_cluster_name}_eks_service_role"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": ["sts:AssumeRole"],
      "Effect": "Allow",
      "Principal": {
        "Service": ["eks.amazonaws.com"]
      }
    }
  ]
}
EOF

  tags = {
      tag-key = "${var.aws_eks_cluster_name}_eks_service_role"
  }
}

resource "aws_iam_role_policy_attachment" "aws_eks_service" {
  role       = "${aws_iam_role.eks_service_role.id}"
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSServicePolicy"
}

resource "aws_iam_role_policy_attachment" "aws_eks_cluster" {
  role       = "${aws_iam_role.eks_service_role.id}"
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

resource "aws_iam_role_policy" "eks_policy_cloud_watch_metrics" {
  name        = "${var.aws_eks_cluster_name}_eks_policy_cloud_watch_metrics"
  role        = "${aws_iam_role.eks_service_role.id}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "cloudwatch:PutMetricData"
      ],
      "Effect": "Allow",
      "Resource": "*"
    }
  ]
}
EOF
}

resource "aws_iam_role_policy" "eks_policy_nlb" {
  name        = "${var.aws_eks_cluster_name}_eks_policy_nlb"
  role        = "${aws_iam_role.eks_service_role.id}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "elasticloadbalancing:*",
        "ec2:CreateSecurityGroup",
        "ec2:Describe*"
      ],
      "Effect": "Allow",
      "Resource": "*"
    }
  ]
}
EOF
}

resource "aws_iam_role" "node_group" {
  name = "node_group"
  path = "/"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Effect": "Allow"
    }
  ]
}
EOF
}

resource "aws_iam_instance_profile" "node_group" {
  name = "node_group"
  role = "${aws_iam_role.node_group.name}"
}

resource "aws_iam_role_policy_attachment" "node_group_eks" {
  role       = "${aws_iam_role.node_group.name}"
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
}

resource "aws_iam_role_policy_attachment" "node_group_eks_cni" {
  role       = "${aws_iam_role.node_group.name}"
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
}

resource "aws_iam_role_policy_attachment" "node_group_ecr_read" {
  role       = "${aws_iam_role.node_group.name}"
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

// Allow to create AWS SSM session to connect EKS node
resource "aws_iam_role_policy_attachment" "node_group_ssm" {
  role       = "${aws_iam_role.node_group.name}"
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM"
}
