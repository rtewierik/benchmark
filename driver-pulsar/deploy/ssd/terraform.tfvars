public_key_path = "~/.ssh/pulsar_aws.pub"
region          = "eu-west-1"
az              = "eu-west-1a"
ami             = "ami-0b5c3f4fa254e17d0" // RHEL-8

instance_types = {
  "pulsar"     = "i3en.6xlarge"
  "zookeeper"  = "i3en.2xlarge"
  "client"     = "m5n.8xlarge"
  "prometheus" = "t2.large"
}

num_instances = {
  "client"     = 4
  "pulsar"     = 5
  "zookeeper"  = 3
  "prometheus" = 1
}
