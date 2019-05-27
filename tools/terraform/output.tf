output "thread_weaver_api_server_ecr_url" {
  value = aws_ecr_repository.thread-weaver-api-server.repository_url
}

output "rds_cluster_aurora_endpoint" {
  value = aws_rds_cluster.aurora.endpoint
}

output "rds_cluster_instance_aurora_endpoints" {
  value = ["${aws_rds_cluster_instance.aurora.*.endpoint}"]
}

