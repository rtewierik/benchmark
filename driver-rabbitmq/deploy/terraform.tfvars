public_key_path = "~/.ssh/rabbitmq_aws.pub"
region          = "eu-west-1"
az              = "eu-west-1a"
ami             = "ami-0b5c3f4fa254e17d0" // RHEL-8

instance_types = {
  "rabbitmq"   = "i3en.6xlarge"
  "client"     = "m5n.8xlarge"
  "prometheus" = "t2.large"
}

num_instances = {
  "rabbitmq"   = 3
  "client"     = 4
  "prometheus" = 1
}
