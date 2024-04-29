#!bin/sh
# Query instances with the specific tag and retrieve instance IDs and private IP addresses
echo $1
instance_info=$(aws ec2 describe-instances \
  --query 'Reservations[*].Instances[?Tags[?Key==`Application` && Value==`'"$1"'`]].{InstanceId:InstanceId, PrivateIpAddress:PrivateIpAddress}' \
  --output json)
echo $instance_info

echo '[' > cpu_utilization.json
echo '[' > memory_utilization.json
echo '[' > disk_utilization.json

cpu_util=()
mem_util=()
disk_util=()

# Loop through the instance info and retrieve metric data for each instance
for info in $(echo "${instance_info}" | jq -c '.[]'); do
  instance_id=$(echo "${info}" | jq -r '.[].InstanceId')
  private_ip=$(echo "${info}" | jq -r '.[].PrivateIpAddress')
  echo $instance_id $private_ip

  # Construct private hostname from private IP address
  private_hostname="ip-${private_ip//./-}.eu-west-1.compute.internal"

  # Retrieve metric data for the instance
  output=$(aws cloudwatch get-metric-data \
    --start-time "$(date -u -v-2H)" \
    --end-time "$(date -u)" \
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
                "Value": "'"$instance_id"'"
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
                "Value": "'"$private_hostname"'"
              }
            ]
          },
          "Period": 10,
          "Stat": "Average"
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
                "Name": "host",
                "Value": "'"$private_hostname"'"
              }
            ]
          },
          "Period": 10,
          "Stat": "Average"
        }
      }
    ]' \
    --output json)
  cpu_util_item=$(echo "${output}" | jq -r '.MetricDataResults[0].Values')
  mem_util_item=$(echo "${output}" | jq -r '.MetricDataResults[1].Values')
  disk_util_item=$(echo "${output}" | jq -r '.MetricDataResults[2].Values')
  cpu_util+=",${cpu_util_item}"
  mem_util+=",${mem_util_item}"
  disk_util+=",${disk_util_item}"
done
echo "${cpu_util:1}"
echo "${mem_util:1}"
echo "${disk_util:1}"