terraform {
  backend "s3" {
    key    = "kafka-aws"
    region = "eu-west-1"
  }
}