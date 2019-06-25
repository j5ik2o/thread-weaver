output "aws_rds_cluster_endpoint" {
  value = "${aws_rds_cluster.aurora.0.endpoint}"
}

output "aws_rds_cluster_database_name" {
  value = "${aws_rds_cluster.aurora.0.database_name}"
}

output "aws_rds_cluster_database_master_username" {
  value = "${aws_rds_cluster.aurora.0.master_username}"
}
