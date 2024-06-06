locals {
  role_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM",
    "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
  ]
}

variable "tpc_h_s3_bucket_arn" {
  type = string
}

variable "s3_benchmarking_bucket_arn" {
  type = string
}

resource "aws_iam_role" "kafka_iam_role" {
  name = "kafka-iam-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = "sts:AssumeRole",
        Effect = "Allow",
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  inline_policy {
    name = "allow_ebs_attachment_tpc_h_chunks_retrieval"

    policy = jsonencode({
      Version = "2012-10-17",
      Statement = [
        {
          Effect = "Allow",
          Action = [
            "ec2:AttachVolume",
            "ec2:DetachVolume",
          ],
          Resource = "*"
        },
        {
          Action   = "s3:GetObject",
          Effect   = "Allow",
          Resource = "${var.tpc_h_s3_bucket_arn}/*"
        },
        {
          Action   = "s3:ListBucket",
          Effect   = "Allow",
          Resource = var.tpc_h_s3_bucket_arn
        },
        {
          Effect = "Allow",
          Action = [
            "cloudwatch:PutMetricData",
            "cloudwatch:GetMetricStatistics",
            "logs:PutLogEvents"
          ],
          Resource = "*"
        },
        {
          Effect   = "Allow",
          Action   = "ssm:GetParameter",
          Resource = "*"
        },
        {
          Effect   = "Allow",
          Action   = "sqs:SendMessage",
          Resource = "arn:aws:sqs:eu-west-1:138945776678:benchmark-monitoring"
        }
      ]
    })
  }
}

resource "aws_iam_role_policy_attachment" "kafka_managed_policy_attachment" {
  count = length(local.role_policy_arns)

  role       = aws_iam_role.kafka_iam_role.name
  policy_arn = element(local.role_policy_arns, count.index)
}

resource "aws_iam_role" "pravega_iam_role" {
  name = "pravega-iam-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = "sts:AssumeRole",
        Effect = "Allow",
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  inline_policy {
    name = "allow_ebs_attachment_tpc_h_chunks_retrieval"

    policy = jsonencode({
      Version = "2012-10-17",
      Statement = [
        {
          Effect = "Allow",
          Action = [
            "ec2:AttachVolume",
            "ec2:DetachVolume",
          ],
          Resource = "*"
        },
        {
          Action   = "s3:GetObject",
          Effect   = "Allow",
          Resource = "${var.tpc_h_s3_bucket_arn}/*"
        },
        {
          Action   = "s3:ListBucket",
          Effect   = "Allow",
          Resource = var.tpc_h_s3_bucket_arn
        },
        {
          Effect = "Allow",
          Action = [
            "cloudwatch:PutMetricData",
            "cloudwatch:GetMetricStatistics",
            "logs:PutLogEvents"
          ],
          Resource = "*"
        },
        {
          Effect   = "Allow",
          Action   = "ssm:GetParameter",
          Resource = "*"
        },
        {
          Effect   = "Allow",
          Action   = "sqs:SendMessage",
          Resource = "arn:aws:sqs:eu-west-1:138945776678:benchmark-monitoring"
        }
      ]
    })
  }
}

resource "aws_iam_role_policy_attachment" "pravea_managed_policy_attachment" {
  count = length(local.role_policy_arns)

  role       = aws_iam_role.pravega_iam_role.name
  policy_arn = element(local.role_policy_arns, count.index)
}

resource "aws_iam_role" "pulsar_iam_role" {
  name = "pulsar-iam-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = "sts:AssumeRole",
        Effect = "Allow",
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  inline_policy {
    name = "allow_ebs_attachment_tpc_h_chunks_retrieval"

    policy = jsonencode({
      Version = "2012-10-17",
      Statement = [
        {
          Effect = "Allow",
          Action = [
            "ec2:AttachVolume",
            "ec2:DetachVolume",
          ],
          Resource = "*"
        },
        {
          Action   = "s3:GetObject",
          Effect   = "Allow",
          Resource = "${var.tpc_h_s3_bucket_arn}/*"
        },
        {
          Action   = "s3:ListBucket",
          Effect   = "Allow",
          Resource = var.tpc_h_s3_bucket_arn
        },
        {
          Effect = "Allow",
          Action = [
            "cloudwatch:PutMetricData",
            "cloudwatch:GetMetricStatistics",
            "logs:PutLogEvents"
          ],
          Resource = "*"
        },
        {
          Effect   = "Allow",
          Action   = "ssm:GetParameter",
          Resource = "*"
        },
        {
          Effect   = "Allow",
          Action   = "sqs:SendMessage",
          Resource = "arn:aws:sqs:eu-west-1:138945776678:benchmark-monitoring"
        }
      ]
    })
  }
}

resource "aws_iam_role_policy_attachment" "pulsar_managed_policy_attachment" {
  count = length(local.role_policy_arns)

  role       = aws_iam_role.pulsar_iam_role.name
  policy_arn = element(local.role_policy_arns, count.index)
}

resource "aws_iam_role" "rabbitmq_iam_role" {
  name = "rabbitmq-iam-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = "sts:AssumeRole",
        Effect = "Allow",
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  inline_policy {
    name = "allow_ebs_attachment_tpc_h_chunks_retrieval"

    policy = jsonencode({
      Version = "2012-10-17",
      Statement = [
        {
          Effect = "Allow",
          Action = [
            "ec2:AttachVolume",
            "ec2:DetachVolume",
          ],
          Resource = "*"
        },
        {
          Action   = "s3:GetObject",
          Effect   = "Allow",
          Resource = "${var.tpc_h_s3_bucket_arn}/*"
        },
        {
          Action   = "s3:ListBucket",
          Effect   = "Allow",
          Resource = var.tpc_h_s3_bucket_arn
        },
        {
          Effect = "Allow",
          Action = [
            "cloudwatch:PutMetricData",
            "cloudwatch:GetMetricStatistics",
            "logs:PutLogEvents"
          ],
          Resource = "*"
        },
        {
          Effect   = "Allow",
          Action   = "ssm:GetParameter",
          Resource = "*"
        },
        {
          Effect   = "Allow",
          Action   = "sqs:SendMessage",
          Resource = "arn:aws:sqs:eu-west-1:138945776678:benchmark-monitoring"
        }
      ]
    })
  }
}

resource "aws_iam_role_policy_attachment" "rabbitmq_managed_policy_attachment" {
  count = length(local.role_policy_arns)

  role       = aws_iam_role.rabbitmq_iam_role.name
  policy_arn = element(local.role_policy_arns, count.index)
}

resource "aws_iam_role" "redis_iam_role" {
  name = "redis-iam-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = "sts:AssumeRole",
        Effect = "Allow",
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  inline_policy {
    name = "allow_tpc_h_chunks_retrieval"

    policy = jsonencode({
      Version = "2012-10-17",
      Statement = [
        {
          Action   = "s3:GetObject",
          Effect   = "Allow",
          Resource = "${var.tpc_h_s3_bucket_arn}/*"
        },
        {
          Action   = "s3:ListBucket",
          Effect   = "Allow",
          Resource = var.tpc_h_s3_bucket_arn
        },
        {
          Effect = "Allow",
          Action = [
            "cloudwatch:PutMetricData",
            "cloudwatch:GetMetricStatistics",
            "logs:PutLogEvents"
          ],
          Resource = "*"
        },
        {
          Effect   = "Allow",
          Action   = "ssm:GetParameter",
          Resource = "*"
        },
        {
          Effect   = "Allow",
          Action   = "sqs:SendMessage",
          Resource = "arn:aws:sqs:eu-west-1:138945776678:benchmark-monitoring"
        }
      ]
    })
  }
}

resource "aws_iam_role_policy_attachment" "redis_managed_policy_attachment" {
  count = length(local.role_policy_arns)

  role       = aws_iam_role.redis_iam_role.name
  policy_arn = element(local.role_policy_arns, count.index)
}

resource "aws_iam_role" "sns_sqs_iam_role" {
  name = "sns-sqs-iam-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = "sts:AssumeRole",
        Effect = "Allow",
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  inline_policy {
    name = "allow_sns_publish_tpc_h_chunks_retrieval"

    policy = jsonencode({
      Version = "2012-10-17",
      Statement = [
        {
          Action   = "sns:Publish"
          Effect   = "Allow"
          Resource = "arn:aws:sns:*:*:sns-sqs-consumer-lambda-*"
        },
        {
          Action   = "s3:GetObject",
          Effect   = "Allow",
          Resource = "${var.tpc_h_s3_bucket_arn}/*"
        },
        {
          Action   = "s3:ListBucket",
          Effect   = "Allow",
          Resource = var.tpc_h_s3_bucket_arn
        },
        {
          Effect = "Allow",
          Action = [
            "cloudwatch:PutMetricData",
            "cloudwatch:GetMetricStatistics",
            "logs:PutLogEvents"
          ],
          Resource = "*"
        },
        {
          Effect   = "Allow",
          Action   = "ssm:GetParameter",
          Resource = "*"
        },
        {
          Effect   = "Allow",
          Action   = "sqs:SendMessage",
          Resource = "arn:aws:sqs:eu-west-1:138945776678:benchmark-monitoring"
        }
      ]
    })
  }
}

resource "aws_iam_role_policy_attachment" "sns_sqs_managed_policy_attachment" {
  count = length(local.role_policy_arns)

  role       = aws_iam_role.sns_sqs_iam_role.name
  policy_arn = element(local.role_policy_arns, count.index)
}


resource "aws_iam_role" "s3_iam_role" {
  name = "s3-iam-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = "sts:AssumeRole",
        Effect = "Allow",
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  inline_policy {
    name = "allow_s3_read_write_tpc_h_chunks_retrieval"

    policy = jsonencode({
      Version = "2012-10-17",
      Statement = [
        {
          Action = [
            "s3:GetObject",
            "s3:PutObject"
          ]
          Effect   = "Allow"
          Resource = "${var.s3_benchmarking_bucket_arn}*/*"
        },
        {
          Action   = "s3:ListBucket"
          Effect   = "Allow"
          Resource = "${var.s3_benchmarking_bucket_arn}*"
        },
        {
          Action   = "s3:GetObject",
          Effect   = "Allow",
          Resource = "${var.tpc_h_s3_bucket_arn}/*"
        },
        {
          Action   = "s3:ListBucket",
          Effect   = "Allow",
          Resource = var.tpc_h_s3_bucket_arn
        },
        {
          Effect = "Allow",
          Action = [
            "cloudwatch:PutMetricData",
            "cloudwatch:GetMetricStatistics",
            "logs:PutLogEvents"
          ],
          Resource = "*"
        },
        {
          Effect   = "Allow",
          Action   = "ssm:GetParameter",
          Resource = "*"
        },
        {
          Effect   = "Allow",
          Action   = "sqs:SendMessage",
          Resource = "arn:aws:sqs:eu-west-1:138945776678:benchmark-monitoring"
        }
      ]
    })
  }
}

resource "aws_iam_role_policy_attachment" "s3_managed_policy_attachment" {
  count = length(local.role_policy_arns)

  role       = aws_iam_role.s3_iam_role.name
  policy_arn = element(local.role_policy_arns, count.index)
}
