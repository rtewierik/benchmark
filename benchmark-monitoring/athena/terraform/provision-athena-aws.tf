variable resource_prefix {
  type = string
}

resource "aws_s3_bucket" "dynamodb_exports" {
  bucket = "${var.resource_prefix}-benchmark-monitoring-exports"
}

resource "aws_s3_bucket" "athena_query_results" {
  bucket        = "${var.resource_prefix}-benchmark-monitoring-athena-query-results"
  force_destroy = true

  lifecycle_rule {
    enabled = true
    prefix  = ""
    tags    = {}

    expiration {
      days = 1
    }
  }
}

resource "aws_athena_workgroup" "workgroup" {
  name = "${var.resource_prefix}-athena-workgroup"

  configuration {
    result_configuration {
      output_location = "s3://${aws_s3_bucket.athena_query_results.bucket}/"
    }
  }
}
