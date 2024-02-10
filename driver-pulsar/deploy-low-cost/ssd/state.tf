terraform {
  backend "s3" {
    key    = "pulsar-aws-low-cost"
    region = "eu-west-1"
  }
}