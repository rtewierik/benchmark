variable "public_key_path" {
  description = <<DESCRIPTION
Path to the SSH public key to be used for authentication.
Ensure this keypair is added to your local SSH agent so provisioners can
connect.

Example: ~/.ssh/s3_aws.pub
DESCRIPTION
}

resource "random_id" "hash" {
  byte_length = 8
}

variable "key_name" {
  default     = "benchmark-key-s3"
  description = "Desired name of AWS key pair"
}

variable "region" {
  type = string
}

variable "ami" {
  type = string
}

variable "az" {
  type = string
}

variable "instance_types" {
  type = map(string)
}

variable "num_instances" {
  type = map(string)
}

variable s3_uris {
  type = string
}

variable is_tpc_h {
  type = bool
}

variable monitoring_sqs_uri {
  type = string
}

variable enable_cloud_monitoring {
  type = bool
}