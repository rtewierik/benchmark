public_key_path = "~/.ssh/pravega_aws.pub"
region          = "eu-west-1"
ami             = "ami-06211bde2f9c725e5" // RHEL-7.9 eu-west-1

instance_types = {
  "controller"   = "m5.large"
  "bookkeeper"   = "t3.xlarge"
  "zookeeper"    = "t2.small"
  "client"       = "t3.large"
  "metrics"      = "t2.large"
}

num_instances = {
  "controller"   = 1
  "bookkeeper"   = 3
  "zookeeper"    = 3
  "client"       = 2
  "metrics"      = 1
}

enable_cloud_monitoring = true
monitoring_sqs_uri = "https://sqs.eu-west-1.amazonaws.com/138945776678/benchmark-monitoring"
