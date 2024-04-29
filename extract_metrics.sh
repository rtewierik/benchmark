#!bin/sh
# Query instances with the specific tag and retrieve instance IDs and private IP addresses
echo $1
instance_info=$(aws ec2 describe-instances \
  --query 'Reservations[*].Instances[?Tags[?Key==`TagName` && Value==`TagValue`]].{InstanceId:InstanceId, PrivateIpAddress:PrivateIpAddress}' \
  --output json)

# Loop through the instance info and retrieve metric data for each instance
for info in $(echo "${instance_info}" | jq -c '.[]'); do
  instance_id=$(echo "${info}" | jq -r '.InstanceId')
  private_ip=$(echo "${info}" | jq -r '.PrivateIpAddress')

  # Construct private hostname from private IP address
  private_hostname="ip-${private_ip//./-}.eu-west-1.compute.internal"

  # Retrieve metric data for the instance
  aws cloudwatch get-metric-data \
    --start-time "$(date -u -d '1 hour ago' '+%Y-%m-%dT%H:%M:%S')" \
    --end-time "$(date -u '+%Y-%m-%dT%H:%M:%S')" \
    --metric-data-queries '[
      {
        "Id": "aaaa",
        "MetricStat": {
          "Metric": {
            "Namespace": "AWS/EC2",
            "MetricName": "CPUUtilization",
            "Dimensions": [
              {
                "Name": "InstanceId",
                "Value": "i-01daec0d655ef1bfc"
              }
            ]
          },
          "Period": 10,
          "Stat": "Maximum"
        }
      },
      {
        "Id": "mem_available_percent",
        "MetricStat": {
          "Metric": {
            "Namespace": "CWAgent",
            "MetricName": "mem_available_percent",
            "Dimensions": [
              {
                "Name": "host",
                "Value": "ip-10-0-0-10.eu-west-1.compute.internal"
              }
            ]
          },
          "Period": 10,
          "Stat": "Maximum"
        }
      },
      {
        "Id": "disk_used_percent",
        "MetricStat": {
          "Metric": {
            "Namespace": "CWAgent",
            "MetricName": "disk_used_percent",
            "Dimensions": [
              {
                "Name": "InstanceId",
                "Value": "'"$instance_id"'"
              }
            ]
          },
          "Period": 3600,
          "Stat": "Average"
        }
      }
    ]' \
    --output json

done