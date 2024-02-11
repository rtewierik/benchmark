provider "aws" {
  region = "${var.region}"
}

provider "random" {}

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "2.7"
    }
    random = {
      version = "2.1"
    }
  }
}