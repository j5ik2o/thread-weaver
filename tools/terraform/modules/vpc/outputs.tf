output "aws_vpc_id" {
    value = "${var.aws_vpc_id}"
}

output "aws_igw" {
    value = "${aws_internet_gateway.vpc.id}"
}

output "aws_subnet_ids_eks" {
    value = "${aws_subnet.eks.*.id}"
}

output "aws_subnet_ids_lb" {
    value = "${aws_subnet.lb.*.id}"
}
