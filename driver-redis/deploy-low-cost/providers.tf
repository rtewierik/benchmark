provider "aws" {
  region = "${var.region}"
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