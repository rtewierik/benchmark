variable "public_key_path" {
  description = <<DESCRIPTION
Path to the SSH public key to be used for authentication.
Ensure this keypair is added to your local SSH agent so provisioners can
connect.

Example: ~/.ssh/pulsar_aws.pub
DESCRIPTION
}

resource "random_id" "hash" {
  byte_length = 8
}

variable "key_name" {
  default     = "pulsar-benchmark-key"
  description = "Desired name prefix for the AWS key pair"
}

variable "region" {}
variable "az" {}
variable "ami" {}
variable "instance_types" {}
variable "num_instances" {}

# Create a VPC to launch our instances into
resource "aws_vpc" "benchmark_vpc" {
  cidr_block = "10.0.0.0/16"

  tags = {
    Name = "Pulsar-Benchmark-VPC-${random_id.hash.hex}"
  }
}

# Create an internet gateway to give our subnet access to the outside world
resource "aws_internet_gateway" "pulsar" {
  vpc_id = aws_vpc.benchmark_vpc.id
}

# Grant the VPC internet access on its main route table
resource "aws_route" "internet_access" {
  route_table_id         = aws_vpc.benchmark_vpc.main_route_table_id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.pulsar.id
}

# Create a subnet to launch our instances into
resource "aws_subnet" "benchmark_subnet" {
  vpc_id                  = aws_vpc.benchmark_vpc.id
  cidr_block              = "10.0.0.0/24"
  map_public_ip_on_launch = true
  availability_zone       = var.az
}

resource "aws_security_group" "benchmark_security_group" {
  name   = "terraform-pulsar-${random_id.hash.hex}"
  vpc_id = aws_vpc.benchmark_vpc.id

  # SSH access from anywhere
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # All ports open within the VPC
  ingress {
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }

  # Prometheus/Dashboard access
  ingress {
    from_port   = 9090
    to_port     = 9090
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # outbound internet access
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "Benchmark-Security-Group-${random_id.hash.hex}"
  }
}

resource "aws_key_pair" "auth" {
  key_name   = "${var.key_name}-${random_id.hash.hex}"
  public_key = file(var.public_key_path)
}

resource "aws_iam_instance_profile" "pulsar_ec2_instance_profile" {
  name = "pulsar_ec2_instance_profile"

  role = "pulsar-iam-role"
}

resource "aws_spot_instance_request" "zookeeper" {
  ami                     = var.ami
  instance_type           = var.instance_types["zookeeper"]
  key_name                = aws_key_pair.auth.id
  subnet_id               = aws_subnet.benchmark_subnet.id
  vpc_security_group_ids  = [aws_security_group.benchmark_security_group.id]
  spot_type               = "one-time"
  wait_for_fulfillment    = true
  count                   = var.num_instances["zookeeper"]

  iam_instance_profile = aws_iam_instance_profile.pulsar_ec2_instance_profile.name

  user_data = <<-EOF
              #!/bin/bash
              # Attach the EBS volume
              sudo yum install -y unzip
              curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
              unzip awscliv2.zip
              sudo ./aws/install
              aws configure set region "${var.region}"
              aws configure set output "json"
              aws ec2 attach-volume --volume-id ${aws_ebs_volume.ebs_zookeeper[count.index].id} --instance-id $(curl -s http://169.254.169.254/latest/meta-data/instance-id) --device /dev/sdh
              EOF

  tags = {
    Name      = "zk-${count.index}"
    Benchmark = "Pulsar"
  }
}

resource "aws_spot_instance_request" "pulsar" {
  ami                     = var.ami
  instance_type           = var.instance_types["pulsar"]
  key_name                = aws_key_pair.auth.id
  subnet_id               = aws_subnet.benchmark_subnet.id
  vpc_security_group_ids  = [aws_security_group.benchmark_security_group.id]
  spot_type               = "one-time"
  wait_for_fulfillment    = true
  count                   = var.num_instances["pulsar"]

  iam_instance_profile = aws_iam_instance_profile.pulsar_ec2_instance_profile.name

  user_data = <<-EOF
              #!/bin/bash
              # Attach the EBS volume
              sudo yum install -y unzip
              curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
              unzip awscliv2.zip
              sudo ./aws/install
              aws configure set region "${var.region}"
              aws configure set output "json"
              aws ec2 attach-volume --volume-id ${aws_ebs_volume.ebs_pulsar[count.index].id} --instance-id $(curl -s http://169.254.169.254/latest/meta-data/instance-id) --device /dev/sdh
              EOF

  tags = {
    Name = "pulsar-${count.index}"
    Benchmark = "Pulsar"
  }
}

resource "aws_spot_instance_request" "client" {
  ami                     = var.ami
  instance_type           = var.instance_types["client"]
  key_name                = aws_key_pair.auth.id
  subnet_id               = aws_subnet.benchmark_subnet.id
  vpc_security_group_ids  = [aws_security_group.benchmark_security_group.id]
  spot_type               = "one-time"
  wait_for_fulfillment    = true
  count                   = var.num_instances["client"]

  tags = {
    Name = "pulsar-client-${count.index}"
    Benchmark = "Pulsar"
  }
}

# resource "aws_spot_instance_request" "prometheus" {
#   ami                     = var.ami
#   instance_type           = var.instance_types["prometheus"]
#   key_name                = aws_key_pair.auth.id
#   subnet_id               = aws_subnet.benchmark_subnet.id
#   vpc_security_group_ids  = [aws_security_group.benchmark_security_group.id]
#   spot_type               = "one-time"
#   wait_for_fulfillment    = true
#   count = var.num_instances["prometheus"]

#   tags = {
#     Name = "prometheus-${count.index}"
#     Benchmark = "Pulsar"
#   }
# }

resource "aws_ebs_volume" "ebs_zookeeper" {
  count             = "${var.num_instances["zookeeper"]}"

  availability_zone = "us-west-2a"
  size              = 30
  type              = "gp3"

  tags = {
    Name            = "zookeeper_ebs_${count.index}"
  }
}

resource "aws_ebs_volume" "ebs_pulsar" {
  count             = "${var.num_instances["pulsar"]}"

  availability_zone = "us-west-2a"
  size              = 40
  type              = "gp3"

  tags = {
    Name            = "pulsar_ebs_${count.index}"
  }
}

output "zookeeper" {
  value = {
    for instance in aws_spot_instance_request.zookeeper :
    instance.public_ip => instance.private_ip
  }
}

output "pulsar" {
  value = {
    for instance in aws_spot_instance_request.pulsar :
    instance.public_ip => instance.private_ip
  }
}

output "client" {
  value = {
    for instance in aws_spot_instance_request.client :
    instance.public_ip => instance.private_ip
  }
}

# output "prometheus" {
#   value = {
#     for instance in aws_spot_instance_request.prometheus :
#     instance.public_ip => instance.private_ip
#   }
# }

output "client_ssh_host" {
  value = aws_spot_instance_request.client.0.public_ip
}

# output "prometheus_host" {
#   value = aws_spot_instance_request.prometheus.0.public_ip
# }
