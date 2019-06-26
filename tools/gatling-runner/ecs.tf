resource "aws_ecs_cluster" "ecs-cluster" {
  name = "${var.ecs-cluster-name}"
  tags = {
    Name = "${var.ecs-cluster-name}"
  }
}
