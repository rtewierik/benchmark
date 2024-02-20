variable "tpc_h_s3_bucket" {
  type = string
}

variable "source_account_iam_role_arns" {
  type  = list(string)
}

resource "aws_s3_bucket_policy" "tpc_h_bucket_policy" {
  bucket = var.tpc_h_s3_bucket

  policy = jsonencode({
      Version = "2012-10-17",
      Statement = [
        for role_arn in var.source_account_iam_role_arns : {
          Action = [
            "s3:GetObject",
            "s3:ListBucket"
          ],
          Effect = "Allow",
          Principal = {
            AWS = role_arn
          }
          Resource = "arn:aws:s3:::${var.tpc_h_s3_bucket}"
        }
      ]
    })
}
