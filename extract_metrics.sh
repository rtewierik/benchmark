#!/bin/bash
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Query instances with the specific tag and retrieve instance IDs and private IP addresses
echo $1
instance_info=$(aws ec2 describe-instances \
  --query 'Reservations[*].Instances[?Tags[?Key==`Application` && Value==`'"$1"'`]].{InstanceId:InstanceId, PrivateIpAddress:PrivateIpAddress, Tags:Tags}' \
  --output json)
echo $instance_info

cpu_util=()
mem_util=()
disk_util=()

# Loop through the instance info and retrieve metric data for each instance
for info in $(echo "${instance_info}" | jq -c '.[]'); do
  if [[ "${info}" == *"]" ]]; then
    instance_id=$(echo "${info}" | jq -r '.[].InstanceId')
    private_ip=$(echo "${info}" | jq -r '.[].PrivateIpAddress')
    tags=$(echo "${info}" | jq -r '.[].Tags')
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
      cpu_util+="{\"instanceId\":\"$instance_id\",\"tags\":$tags,\"values\":$(echo "${output}" | jq -r '.MetricDataResults[0].Values')}\n"
      mem_util+="{\"instanceId\":\"$instance_id\",\"tags\":$tags,\"values\":$(echo "${output}" | jq -r '.MetricDataResults[1].Values')}\n"
      disk_util+="{\"instanceId\":\"$instance_id\",\"tags\":$tags,\"values\":$(echo "${output}" | jq -r '.MetricDataResults[2].Values')}\n"
    fi
  fi
done
echo $cpu_util > cpu_utilization.json
echo $mem_util > memory_utilization.json
echo $disk_util > disk_utilization.json
