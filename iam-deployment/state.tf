terraform {
  backend "s3" {
    key    = "iam"
    region = "eu-west-1"
  }
}