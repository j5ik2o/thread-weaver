resource "aws_security_group" "eks_control_plane" {
  name = "eks_control_plane"
  description = "EKS control plane security group"
  vpc_id     = "${var.aws_vpc_id}"

  tags = "${merge(map("kubernetes.io/cluster/${var.aws_eks_cluster_name}", "shared"), map("Name", "eks_control_plane"))}"
}

resource "aws_security_group" "eks_node" {
  name = "eks_node"
  description = "Communication between the control plane and worker nodes in node group"
  vpc_id     = "${var.aws_vpc_id}"

  tags = "${merge(map("kubernetes.io/cluster/${var.aws_eks_cluster_name}", "owned"), map("Name", "eks_node"))}"
}

resource "aws_security_group_rule" "control_plane_outgoing_any" {
  type                     = "egress"
  protocol                 = "all"
  from_port                = 0
  to_port                  = 65535
  cidr_blocks              = ["0.0.0.0/0"]
  security_group_id        = "${aws_security_group.eks_control_plane.id}"
  description              = "Allow control plane to connect any hosts"
}

resource "aws_security_group_rule" "control_plane_outgoing_node_group_tcp_any" {
  type                     = "egress"
  protocol                 = "tcp"
  from_port                = 1025
  to_port                  = 65535
  source_security_group_id = "${aws_security_group.eks_node.id}"
  security_group_id        = "${aws_security_group.eks_control_plane.id}"
  description              = "Allow control plane to communicate with worker nodes in group ng-worker (kubelet and workload TCP ports)"
}

resource "aws_security_group_rule" "control_plane_outgoing_node_group_https" {
  type                     = "egress"
  protocol                 = "tcp"
  from_port                = 443
  to_port                  = 443
  source_security_group_id = "${aws_security_group.eks_node.id}"
  security_group_id        = "${aws_security_group.eks_control_plane.id}"
  description              = "Allow control plane to communicate with worker nodes in node group (workloads using HTTPS port, commonly used with extension API servers)"
}

resource "aws_security_group_rule" "node_group_outgoing_any" {
  type                     = "egress"
  protocol                 = "all"
  from_port                = 0
  to_port                  = 65535
  cidr_blocks              = ["0.0.0.0/0"]
  security_group_id        = "${aws_security_group.eks_node.id}"
  description              = "Allow node to connect any hosts"
}

resource "aws_security_group_rule" "node_group_allow_control_plane_tcp_any" {
  type                     = "ingress"
  protocol                 = "tcp"
  from_port                = 1025
  to_port                  = 65535
  source_security_group_id = "${aws_security_group.eks_control_plane.id}"
  security_group_id        = "${aws_security_group.eks_node.id}"
  description              = "Allow worker nodes in node group to communicate with control plane (kubelet and workload TCP ports)"
}

resource "aws_security_group_rule" "node_group_allow_control_plane_https" {
  type                     = "ingress"
  protocol                 = "tcp"
  from_port                = 443
  to_port                  = 443
  source_security_group_id = "${aws_security_group.eks_control_plane.id}"
  security_group_id        = "${aws_security_group.eks_node.id}"
  description              = "Allow worker nodes in node group to communicate with control plane (workloads using HTTPS port, commonly used with extension API servers)"
}

resource "aws_security_group_rule" "control_plane_allow_node_group_https" {
  type                     = "ingress"
  protocol                 = "tcp"
  from_port                = 443
  to_port                  = 443
  source_security_group_id = "${aws_security_group.eks_node.id}"
  security_group_id        = "${aws_security_group.eks_control_plane.id}"
  description              = "Allow control plane to receive API requests from worker nodes in node group"
}
