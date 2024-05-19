public_key_path = "~/.ssh/sns_sqs_aws.pub"
region          = "eu-west-1"
az              = "eu-west-1a"
ami             = "ami-0f0f1c02e5e4d9d9f" // RHEL-8

is_tpc_h = true
sns_uris = [
  "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-map",
  "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-result",
  "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-reduce0",
  "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-reduce1",
  "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-reduce2",
  "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-reduce3",
  "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-reduce4",
  "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-reduce5",
  "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-reduce6",
  "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-reduce7",
  "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-reduce8",
  "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-reduce9",
]
# sns_uris = [
#   "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-default0",
#   "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-default1",
#   "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-default2"
# ]

instance_types = {
  "client" = "t3.large"
}

num_instances = {
  "client" = 3
}

enable_cloud_monitoring = true
monitoring_sqs_uri      = "https://sqs.eu-west-1.amazonaws.com/138945776678/benchmark-monitoring"

is_debug = false
