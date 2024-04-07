public_key_path = "~/.ssh/pravega_aws.pub"
region          = "eu-west-1"
ami             = "ami-06211bde2f9c725e5" // RHEL-7.9 eu-west-1

instance_types = {
  "controller"   = "m5.large"
  "bookkeeper"   = "i3en.2xlarge"
  "zookeeper"    = "t2.small"
  "client"       = "m5n.xlarge"
  "metrics"      = "t2.large"
}

num_instances = {
  "controller"   = 1
  "bookkeeper"   = 3
  "zookeeper"    = 3
  "client"       = 1
  "metrics"      = 1
}
