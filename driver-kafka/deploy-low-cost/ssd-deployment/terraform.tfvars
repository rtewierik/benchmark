public_key_path = "~/.ssh/kafka_aws.pub"
region          = "eu-west-1"
az              = "eu-west-1a"
ami             = "ami-0b5c3f4fa254e17d0" // RHEL-8

instance_types = {
  "kafka"     = "t3.large"
  "zookeeper" = "t3.large"
  "client"    = "t3.large"
}

num_instances = {
  "client"    = 4
  "kafka"     = 3
  "zookeeper" = 3
}

enable_cloud_monitoring = true
monitoring_sqs_uri = ""
