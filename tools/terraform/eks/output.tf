output "cluster" {
  value = "${data.template_file.cluster.rendered}"
}

output "aws_rds_cluster_endpoint" {
  value = "${module.aws_aurora.aws_rds_cluster_endpoint}"
}

output "aws_rds_cluster_database_name" {
  value = "${module.aws_aurora.aws_rds_cluster_database_name}"
}

output "aws_rds_cluster_database_master_username" {
  value = "${module.aws_aurora.aws_rds_cluster_database_master_username}"
}
