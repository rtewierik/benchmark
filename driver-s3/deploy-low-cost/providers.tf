provider "aws" {
  region = "${var.region}"
  allowed_account_ids = ["533267013102", "138945776678"]
}

provider "random" {}

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "5"
    }
    random = {
      version = "3.1"
    }
  }
}