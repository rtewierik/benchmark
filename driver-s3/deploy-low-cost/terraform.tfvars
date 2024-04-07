public_key_path = "~/.ssh/s3_aws.pub"
region          = "eu-west-1"
az              = "eu-west-1a"
ami             = "ami-0b5c3f4fa254e17d0" // RHEL-8

is_tpc_h = false
s3_uris = "s3://benchmarking-events/s3-consumer-lambda-s3-map-2ymi6,s3://benchmarking-events/s3-consumer-lambda-s3-result-kyoib,s3://benchmarking-events/s3-consumer-lambda-s3-reduce0-mk86h,s3://benchmarking-events/s3-consumer-lambda-s3-reduce1-u7imm,s3://benchmarking-events/s3-consumer-lambda-s3-reduce2-vgliv"

instance_types = {
  "client"       = "t3.large"
}

num_instances = {
  "client"    = 3
}

enable_cloud_monitoring = true
monitoring_sqs_uri = ""
