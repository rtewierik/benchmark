terraform {
  backend "s3" {
    key    = "tpc-h"
    region = "eu-west-1"
  }
}