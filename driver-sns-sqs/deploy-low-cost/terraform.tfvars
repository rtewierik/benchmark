public_key_path = "~/.ssh/sns_sqs_aws.pub"
region          = "us-west-2"
az              = "us-west-2a"
ami             = "ami-08970fb2e5767e3b8" // RHEL-8

is_tpc_h = false
sns_uris = "arn:aws:sns:us-west-2:138945776678:sns-sqs-consumer-lambda-sns-topic-default"

instance_types = {
  "client"       = "t3.large"
}

num_instances = {
  "client"    = 3
}
