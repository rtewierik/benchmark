public_key_path = "~/.ssh/pravega_aws.pub"
region          = "eu-west-1"
ami             = "ami-06211bde2f9c725e5" // RHEL-7.9 eu-west-1

instance_types = {
  "controller" = "m5.2xlarge"
  "bookkeeper" = "i3en.xlarge"
  "zookeeper"  = "i3en.xlarge"
  "client"     = "m5n.2xlarge"
  "metrics"    = "t2.large"
}

num_instances = {
  "controller" = 1
  "bookkeeper" = 3
  "zookeeper"  = 3
  "client"     = 3
  "metrics"    = 1
}

enable_cloud_monitoring = false
monitoring_sqs_uri      = "https://sqs.eu-west-1.amazonaws.com/138945776678/benchmark-monitoring"

is_debug = false
