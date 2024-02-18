public_key_path = "~/.ssh/pulsar_aws.pub"
region          = "us-west-2"
az              = "us-west-2a"
ami             = "ami-08970fb2e5767e3b8" // RHEL-8

instance_types = {
  "pulsar"     = "t3.xlarge"
  "zookeeper"  = "t3.large"
  "client"     = "t3.large"
  "prometheus" = "t2.medium"
}

num_instances = {
  "client"     = 3
  "pulsar"     = 3
  "zookeeper"  = 3
}
