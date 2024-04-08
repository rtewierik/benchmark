public_key_path = "~/.ssh/pulsar_aws.pub"
region          = "eu-west-1"
az              = "eu-west-1a"
ami             = "ami-0b5c3f4fa254e17d0" // RHEL-8

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

enable_cloud_monitoring = true
monitoring_sqs_uri = "https://sqs.eu-west-1.amazonaws.com/138945776678/benchmark-monitoring"
