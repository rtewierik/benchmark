public_key_path = "~/.ssh/rabbitmq_aws.pub"
region          = "eu-west-1"
az              = "eu-west-1a"
ami             = "ami-0b5c3f4fa254e17d0" // RHEL-8

instance_types = {
  "rabbitmq"   = "t3.large"
  "client"     = "t3.large"
  "prometheus" = "t2.medium"
}

num_instances = {
  "rabbitmq"   = 3
  "client"     = 4
  "prometheus" = 1
}

enable_cloud_monitoring = true
monitoring_sqs_uri = ""
