public_key_path = "~/.ssh/s3_aws.pub"
region          = "eu-west-1"
az              = "eu-west-1a"
ami             = "ami-0f0f1c02e5e4d9d9f" // RHEL-8

is_tpc_h = true
s3_uris = [
  "s3://benchmarking-events/s3-consumer-lambda-s3-map-a6d0u",
  "s3://benchmarking-events/s3-consumer-lambda-s3-result-sseq3",
  "s3://benchmarking-events/s3-consumer-lambda-s3-reduce0-pd5yf",
  "s3://benchmarking-events/s3-consumer-lambda-s3-reduce1-1z63x",
  "s3://benchmarking-events/s3-consumer-lambda-s3-reduce2-x6llf"
]
# s3_uris = [
#   "s3://benchmarking-events/s3-consumer-lambda-s3-default0",
#   "s3://benchmarking-events/s3-consumer-lambda-s3-default1",
#   "s3://benchmarking-events/s3-consumer-lambda-s3-default2"
# ]

instance_types = {
  "client"       = "t3.large"
}

num_instances = {
  "client"    = 3
}

enable_cloud_monitoring = true
monitoring_sqs_uri = "https://sqs.eu-west-1.amazonaws.com/138945776678/benchmark-monitoring"
