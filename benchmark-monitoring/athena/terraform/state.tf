terraform {
  backend "s3" {
    key    = "athena"
    region = "eu-west-1"
  }
}