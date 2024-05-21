provider "aws" {
  allowed_account_ids = ["533267013102", "138945776678", "730335367108"]
}

provider "random" {}

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "3.50"
    }
    random = {
      version = "3.1"
    }
  }
}