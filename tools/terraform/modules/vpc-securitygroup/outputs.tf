output "eks_control_plane_sg_id" {
  value = "${aws_security_group.eks_control_plane.id}"
}

output "eks_node_sg_id" {
  value = "${aws_security_group.eks_node.id}"
}
