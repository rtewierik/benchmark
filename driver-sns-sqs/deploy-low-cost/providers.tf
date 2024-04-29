provider "aws" {
  region = "${var.region}"
  allowed_account_ids = ["533267013102", "138945776678"]
  default_tags {
    tags = {
      Application = var.app_name
      Owner       = "Ruben te Wierik"
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