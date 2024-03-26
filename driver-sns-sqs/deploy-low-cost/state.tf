terraform {
  backend "s3" {
    key    = "sns-sqs-aws-low-cost"
    region = "eu-west-1"
  }
}
