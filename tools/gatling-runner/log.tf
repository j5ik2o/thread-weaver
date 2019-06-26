resource "aws_cloudwatch_log_group" "gatling-log-group" {
  name = "/ecs/logs/${var.prefix}-gatling-ecs-group"
}