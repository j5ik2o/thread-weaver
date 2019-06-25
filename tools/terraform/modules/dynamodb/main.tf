resource "aws_dynamodb_table" "journal-table" {
  count          = "${var.enabled ? 1 : 0}"
  name           = "${var.journal_table_name}"
  billing_mode   = "PROVISIONED"
  read_capacity  = 20
  write_capacity = 20
  hash_key       = "pkey"
  range_key      = "sequence-nr"

  attribute {
    name = "pkey"
    type = "S"
  }

  attribute {
    name = "persistence-id"
    type = "S"
  }

  attribute {
    name = "sequence-nr"
    type = "N"
  }

  attribute {
    name = "tags"
    type = "S"
  }

  global_secondary_index {
    name            = "TagsIndex"
    hash_key        = "tags"
    write_capacity  = 10
    read_capacity   = 10
    projection_type = "ALL"
  }

  global_secondary_index {
    name            = "GetJournalRowsIndex"
    hash_key        = "persistence-id"
    range_key       = "sequence-nr"
    write_capacity  = 10
    read_capacity   = 10
    projection_type = "ALL"
  }
}

resource "aws_dynamodb_table" "snapshot-table" {
  count          = "${var.enabled ? 1 : 0}"
  name           = "${var.snapshot_table_name}"
  billing_mode   = "PROVISIONED"
  read_capacity  = 20
  write_capacity = 20
  hash_key       = "persistence-id"
  range_key      = "sequence-nr"

  attribute {
    name = "persistence-id"
    type = "S"
  }

  attribute {
    name = "sequence-nr"
    type = "N"
  }
}

