public_key_path = "~/.ssh/kafka_aws.pub"
region          = "us-west-2"
az              = "us-west-2a"
ami             = "ami-08970fb2e5767e3b8" // RHEL-8

instance_types = {
  "kafka"     = "i3en.2xlarge"
  "zookeeper" = "i3en.2xlarge"
  "client"    = "m5dn.4xlarge"
}

num_instances = {
  "client"    = 1
  "kafka"     = 1
  "zookeeper" = 1
}
