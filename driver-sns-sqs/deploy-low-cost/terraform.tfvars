public_key_path = "~/.ssh/sns_sqs_aws.pub"
region          = "eu-west-1"
az              = "eu-west-1a"
ami             = "ami-0f0f1c02e5e4d9d9f" // RHEL-8

is_tpc_h = true

instance_types = {
  "client" = "m5n.2xlarge"
}

num_instances = {
  "client" = 3
}

enable_cloud_monitoring = true
monitoring_sqs_uri      = "https://sqs.eu-west-1.amazonaws.com/138945776678/benchmark-monitoring"

is_debug = false

number_of_consumers = 800
account_id = "138945776678"