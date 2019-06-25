output "aws_dynamodb_table_journal_table_name" {
  value = "${aws_dynamodb_table.journal-table.0.name}"
}

output "aws_dynamodb_table_snapshot_table_name" {
  value = "${aws_dynamodb_table.snapshot-table.0.name}"
}