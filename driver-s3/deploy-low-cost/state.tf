terraform {
  backend "s3" {
    key    = "s3-aws-low-cost"
    region = "eu-west-1"
  }
}
