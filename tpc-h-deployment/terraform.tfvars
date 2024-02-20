tpc_h_s3_bucket               = "tpc-h-chunks"
source_account_iam_role_arns  = [
  "arn:aws:iam::533267013102:role/kafka-iam-role",
  "arn:aws:iam::533267013102:role/pravega-iam-role",
  "arn:aws:iam::533267013102:role/pulsar-iam-role",
  "arn:aws:iam::533267013102:role/rabbitmq-iam-role",
  "arn:aws:iam::533267013102:role/redis-iam-role"
]