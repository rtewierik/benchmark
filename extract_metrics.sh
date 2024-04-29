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
  if [ "$private_ip" != "null" ] && [ -n "$private_ip" ]; then
    echo $instance_id $private_ip

    # Retrieve metric data for the instance
    output=$(aws cloudwatch get-metric-data \
      --start-time "$(date -u -v-2H)" \
      --end-time "$(date -u)" \
      --metric-data-queries '[
        {
          "Id": "cpu_utilization",
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
                  "Name": "InstanceId",
                  "Value": "'"$instance_id"'"
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
                  "Name": "InstanceId",
                  "Value": "'"$instance_id"'"
                }
              ]
            },
            "Period": 10,
            "Stat": "Average"
          }
        }
      ]' \
      --output json)
    cpu_util+=",{\"instanceId\": \"$instance_id\", \"values\":$(echo "${output}" | jq -r '.MetricDataResults[0].Values')}"
    mem_util+=",{\"instanceId\": \"$instance_id\", \"values\":$(echo "${output}" | jq -r '.MetricDataResults[1].Values')}"
    disk_util+=",{\"instanceId\": \"$instance_id\", \"values\":$(echo "${output}" | jq -r '.MetricDataResults[2].Values')}"
  fi
done
echo "${cpu_util:1}]" >> cpu_utilization.json
echo "${mem_util:1}]" >> memory_utilization.json
echo "${disk_util:1}]" >> disk_utilization.json
