output "aws_eks_service_role_arn" {
  value = "${aws_iam_role.eks_service_role.arn}"
}

output "node_group_aws_instance_profile_arn" {
  value = "${aws_iam_instance_profile.node_group.arn}"
}
