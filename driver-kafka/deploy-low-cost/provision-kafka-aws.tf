variable "app_name" {
  type    = string
  default = "kafka-benchmark-ruben-te-wierik"
}

variable "public_key_path" {
  description = <<DESCRIPTION
Path to the SSH public key to be used for authentication.
Ensure this keypair is added to your local SSH agent so provisioners can
connect.

Example: ~/.ssh/kafka_aws.pub
DESCRIPTION
}

resource "random_id" "hash" {
  byte_length = 8
}

variable "key_name" {
  default     = "kafka-benchmark-key"
  description = "Desired name prefix for the AWS key pair"
}

variable "region" {}

variable "ami" {}

variable "az" {}

variable "instance_types" {
  type = map(string)
}

variable "num_instances" {
  type = map(string)
}

variable "monitoring_sqs_uri" {
  type = string
}

variable "enable_cloud_monitoring" {
  type = bool
}

variable "is_debug" {
  type = bool
}

# Create a VPC to launch our instances into
resource "aws_vpc" "benchmark_vpc" {
  cidr_block = "10.0.0.0/16"

  tags = {
    Name = "Kafka_Benchmark_VPC_${random_id.hash.hex}"
  }
}

# Create an internet gateway to give our subnet access to the outside world
resource "aws_internet_gateway" "kafka" {
  vpc_id = aws_vpc.benchmark_vpc.id
}

# Grant the VPC internet access on its main route table
resource "aws_route" "internet_access" {
  route_table_id         = aws_vpc.benchmark_vpc.main_route_table_id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.kafka.id
}

# Create a subnet to launch our instances into
resource "aws_subnet" "benchmark_subnet" {
  vpc_id                  = aws_vpc.benchmark_vpc.id
  cidr_block              = "10.0.0.0/24"
  map_public_ip_on_launch = true
  availability_zone       = var.az
}

resource "aws_security_group" "benchmark_security_group" {
  name   = "terraform-kafka-${random_id.hash.hex}"
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

resource "aws_ssm_parameter" "cw_agent" {
  description = "Cloudwatch agent config to configure custom metrics."
  name        = "/cloudwatch-agent/config"
  type        = "String"
  value       = file("../../cw_agent_config.json")
}

resource "aws_iam_instance_profile" "kafka_ec2_instance_profile" {
  name = "kafka_ec2_instance_profile"

  role = "kafka-iam-role"
}

resource "aws_instance" "zookeeper" {
  ami                    = var.ami
  instance_type          = var.instance_types["zookeeper"]
  key_name               = aws_key_pair.auth.id
  subnet_id              = aws_subnet.benchmark_subnet.id
  vpc_security_group_ids = ["${aws_security_group.benchmark_security_group.id}"]
  count                  = var.num_instances["zookeeper"]

  monitoring = true

  iam_instance_profile = aws_iam_instance_profile.kafka_ec2_instance_profile.name

  instance_market_options {
    market_type = "spot"
    spot_options {
      instance_interruption_behavior = "terminate"
      spot_instance_type             = "one-time"
    }
  }

  connection {
    type        = "ssh"
    user        = "ec2-user"
    private_key = file(replace(var.public_key_path, ".pub", ""))
    host        = self.public_ip
  }

  provisioner "remote-exec" {
    inline = [
      "curl https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm -O",
      "sudo rpm -U ./amazon-cloudwatch-agent.rpm",
      "sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c ssm:${aws_ssm_parameter.cw_agent.name}",
    ]
  }

  tags = {
    Name      = "zk_${count.index}"
    Benchmark = "Kafka"
  }
}

resource "aws_instance" "kafka" {
  ami                    = var.ami
  instance_type          = var.instance_types["kafka"]
  key_name               = aws_key_pair.auth.id
  subnet_id              = aws_subnet.benchmark_subnet.id
  vpc_security_group_ids = ["${aws_security_group.benchmark_security_group.id}"]
  count                  = var.num_instances["kafka"]

  monitoring = true

  iam_instance_profile = aws_iam_instance_profile.kafka_ec2_instance_profile.name

  instance_market_options {
    market_type = "spot"
    spot_options {
      instance_interruption_behavior = "terminate"
      spot_instance_type             = "one-time"
    }
  }

  connection {
    type        = "ssh"
    user        = "ec2-user"
    private_key = file(replace(var.public_key_path, ".pub", ""))
    host        = self.public_ip
  }

  provisioner "remote-exec" {
    inline = [
      "curl https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm -O",
      "sudo rpm -U ./amazon-cloudwatch-agent.rpm",
      "sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c ssm:${aws_ssm_parameter.cw_agent.name}",
    ]
  }

  tags = {
    Name      = "kafka_${count.index}"
    Benchmark = "Kafka"
  }
}

resource "aws_instance" "client" {
  ami                    = var.ami
  instance_type          = var.instance_types["client"]
  key_name               = aws_key_pair.auth.id
  subnet_id              = aws_subnet.benchmark_subnet.id
  vpc_security_group_ids = ["${aws_security_group.benchmark_security_group.id}"]
  count                  = var.num_instances["client"]

  monitoring = true

  iam_instance_profile = aws_iam_instance_profile.kafka_ec2_instance_profile.name

  instance_market_options {
    market_type = "spot"
    spot_options {
      instance_interruption_behavior = "terminate"
      spot_instance_type             = "one-time"
    }
  }

  connection {
    type        = "ssh"
    user        = "ec2-user"
    private_key = file(replace(var.public_key_path, ".pub", ""))
    host        = self.public_ip
  }

  provisioner "remote-exec" {
    inline = [
      "curl https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm -O",
      "sudo rpm -U ./amazon-cloudwatch-agent.rpm",
      "sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c ssm:${aws_ssm_parameter.cw_agent.name}",
    ]
  }

  user_data = <<-EOF
    #!/bin/bash
    echo "export IS_CLOUD_MONITORING_ENABLED=${var.enable_cloud_monitoring}" >> /etc/profile.d/myenvvars.sh
    echo "export MONITORING_SQS_URI=${var.monitoring_sqs_uri}" >> /etc/profile.d/myenvvars.sh
    echo "export REGION=${var.region}" >> /etc/profile.d/myenvvars.sh
    echo "export DEBUG=${var.is_debug}" >> /etc/profile.d/myenvvars.sh
    echo "IS_CLOUD_MONITORING_ENABLED=${var.enable_cloud_monitoring}" >> /etc/environment
    echo "MONITORING_SQS_URI=${var.monitoring_sqs_uri}" >> /etc/environment
    echo "REGION=${var.region}" >> /etc/environment
    echo "DEBUG=${var.is_debug}" >> /etc/environment
    EOF

  tags = {
    Name      = "kafka_client_${count.index}"
    Benchmark = "Kafka"
  }
}

output "kafka_ssh_host" {
  value = aws_instance.kafka.0.public_ip
}

output "client_ssh_host" {
  value = aws_instance.client.0.public_ip
}
