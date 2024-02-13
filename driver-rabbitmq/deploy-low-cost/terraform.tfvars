public_key_path = "~/.ssh/rabbitmq_aws.pub"
region          = "us-west-2"
az              = "us-west-2a"
ami             = "ami-08970fb2e5767e3b8" // RHEL-8

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
