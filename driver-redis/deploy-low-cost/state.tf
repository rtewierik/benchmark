terraform {
  backend "s3" {
    key    = "redis-aws-low-cost"
    region = "eu-west-1"
  }
}