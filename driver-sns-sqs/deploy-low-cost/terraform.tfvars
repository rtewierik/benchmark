public_key_path = "~/.ssh/sns_sqs_aws.pub"
region          = "eu-west-1"
az              = "eu-west-1a"
ami             = "ami-0f0f1c02e5e4d9d9f" // RHEL-8

is_tpc_h = false
sns_uris = "arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-reduce0,arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-reduce1,arn:aws:sns:eu-west-1:138945776678:sns-sqs-consumer-lambda-sns-topic-reduce2"

instance_types = {
  "client"       = "t3.large"
}

num_instances = {
  "client"    = 3
}

enable_cloud_monitoring = true
monitoring_sqs_uri = "https://sqs.eu-west-1.amazonaws.com/138945776678/benchmark-monitoring"
