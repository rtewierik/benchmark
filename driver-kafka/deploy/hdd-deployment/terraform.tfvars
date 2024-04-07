public_key_path = "~/.ssh/kafka_aws.pub"
region          = "eu-west-1"
az              = "eu-west-1a"
ami             = "ami-bb9a6bc2" // RHEL-7.4

instance_types = {
  "kafka"     = "d2.4xlarge"
  "zookeeper" = "t2.small"
  "client"    = "c5.4xlarge"
}

num_instances = {
  "client"    = 4
  "kafka"     = 3
  "zookeeper" = 3
}
