public_key_path = "~/.ssh/redis_aws.pub"
region          = "us-west-2"
az              = "us-west-2a"
ami             = "ami-08970fb2e5767e3b8" // RHEL-8

instance_types = {
  "client"       = "t3.large"
}

num_instances = {
  "client"    = 3
}
