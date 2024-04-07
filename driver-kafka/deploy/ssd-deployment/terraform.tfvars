public_key_path = "~/.ssh/kafka_aws.pub"
region          = "eu-west-1"
az              = "eu-west-1a"
ami             = "ami-0b5c3f4fa254e17d0" // RHEL-8

instance_types = {
  "kafka"     = "i3en.6xlarge"
  "zookeeper" = "i3en.2xlarge"
  "client"    = "m5n.8xlarge"
}

num_instances = {
  "client"    = 4
  "kafka"     = 3
  "zookeeper" = 3
}
