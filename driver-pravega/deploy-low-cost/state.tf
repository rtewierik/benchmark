terraform {
  backend "s3" {
    key    = "pravega-aws-low-cost"
    region = "eu-west-1"
  }
}