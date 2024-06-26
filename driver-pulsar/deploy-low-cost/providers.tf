provider "aws" {
  region              = var.region
  allowed_account_ids = ["533267013102", "138945776678", "730335367108"]
  default_tags {
    tags = {
      Application = var.app_name
      Owner       = "Ruben_te_Wierik"
      Contact     = "rtewierik@uoc.edu"
    }
  }
}

provider "random" {}

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "5.47"
    }
    random = {
      version = "3.1"
    }
  }
}