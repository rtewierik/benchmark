terraform {
  backend "s3" {
    key    = "rabbitmq-aws-low-cost"
    region = "eu-west-1"
  }
}