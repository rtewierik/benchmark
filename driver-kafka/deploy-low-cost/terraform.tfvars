public_key_path = "~/.ssh/kafka_aws.pub"
region          = "eu-west-1"
az              = "eu-west-1a"
ami             = "ami-07d4917b6f95f5c2a" // RHEL-9

instance_types = {
  "kafka"     = "i3en.2xlarge"
  "zookeeper" = "i3en.xlarge"
  "client"    = "m5n.2xlarge"
}

num_instances = {
  "client"    = 3
  "kafka"     = 3
  "zookeeper" = 3
}

enable_cloud_monitoring = false
monitoring_sqs_uri      = "https://sqs.eu-west-1.amazonaws.com/138945776678/benchmark-monitoring"

is_debug = false
