variable "tpc_h_s3_bucket" {
  type = string
}

variable "iam_roles_account_number" {
  type = string
}

variable "iam_roles" {
  type = list(string)
}

resource "aws_s3_bucket_policy" "tpc_h_bucket_policy" {
  bucket = var.tpc_h_s3_bucket

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = flatten([
      for role_arn in var.iam_roles : [
        {
          Action = [
            "s3:ListBucket"
          ],
          Effect = "Allow",
          Principal = {
            AWS = role_arn
          }
          Resource = "arn:aws:s3:::${var.tpc_h_s3_bucket}"
        },
        {
          Action = [
            "s3:GetObject",
          ],
          Effect = "Allow",
          Principal = {
            AWS = role_arn
          }
          Resource = "arn:aws:s3:::${var.tpc_h_s3_bucket}/*"
        }
      ]
    ])
  })
}
