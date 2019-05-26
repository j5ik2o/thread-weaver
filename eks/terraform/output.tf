output "thread_weaver_api_server_ecr_url" {
  value = "${aws_ecr_repository.thread-weaver-api-server.repository_url}"
}

output "rds_cluster_aurora_endpoint" {
  value = "${aws_rds_cluster.thread-weaver-aurora-cluster.endpoint}"
}

output "rds_cluster_instance_aurora_endpoints" {
  value = ["${aws_rds_cluster_instance.thread-weaver-aurora-instance.*.endpoint}"]
}