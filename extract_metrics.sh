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

data=()

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
        --start-time "$(date -u -v-4H)" \
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
              "Stat": "Average"
            }
          },
          {
            "Id": "mem_used_percent",
            "MetricStat": {
              "Metric": {
                "Namespace": "CWAgent",
                "MetricName": "mem_used_percent",
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
            "Id": "swap_used_percent",
            "MetricStat": {
              "Metric": {
                "Namespace": "CWAgent",
                "MetricName": "swap_used_percent",
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
          },
          {
            "Id": "network_out",
            "MetricStat": {
              "Metric": {
                "Namespace": "AWS/EC2",
                "MetricName": "NetworkOut",
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
            "Id": "network_in",
            "MetricStat": {
              "Metric": {
                "Namespace": "AWS/EC2",
                "MetricName": "NetworkIn",
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
            "Id": "network_packets_out",
            "MetricStat": {
              "Metric": {
                "Namespace": "AWS/EC2",
                "MetricName": "NetworkPacketsOut",
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
            "Id": "network_packets_in",
            "MetricStat": {
              "Metric": {
                "Namespace": "AWS/EC2",
                "MetricName": "NetworkPacketsIn",
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
            "Id": "disk_write_bytes",
            "MetricStat": {
              "Metric": {
                "Namespace": "AWS/EC2",
                "MetricName": "DiskWriteBytes",
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
            "Id": "disk_write_ops",
            "MetricStat": {
              "Metric": {
                "Namespace": "AWS/EC2",
                "MetricName": "DiskWriteOps",
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
            "Id": "disk_read_bytes",
            "MetricStat": {
              "Metric": {
                "Namespace": "AWS/EC2",
                "MetricName": "DiskReadBytes",
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
            "Id": "disk_read_ops",
            "MetricStat": {
              "Metric": {
                "Namespace": "AWS/EC2",
                "MetricName": "DiskReadOps",
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
      data+="{\"instanceId\":\"$instance_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"tags\":$tags,\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[0].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[0].Values')}\n"
      data+="{\"instanceId\":\"$instance_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"tags\":$tags,\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[1].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[1].Values')}\n"
      data+="{\"instanceId\":\"$instance_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"tags\":$tags,\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[2].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[2].Values')}\n"
      data+="{\"instanceId\":\"$instance_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"tags\":$tags,\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[3].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[3].Values')}\n"
      data+="{\"instanceId\":\"$instance_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"tags\":$tags,\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[4].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[4].Values')}\n"
      data+="{\"instanceId\":\"$instance_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"tags\":$tags,\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[5].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[5].Values')}\n"
      data+="{\"instanceId\":\"$instance_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"tags\":$tags,\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[6].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[6].Values')}\n"
      data+="{\"instanceId\":\"$instance_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"tags\":$tags,\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[7].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[7].Values')}\n"
      data+="{\"instanceId\":\"$instance_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"tags\":$tags,\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[8].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[8].Values')}\n"
      data+="{\"instanceId\":\"$instance_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"tags\":$tags,\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[9].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[9].Values')}\n"
      data+="{\"instanceId\":\"$instance_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"tags\":$tags,\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[10].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[10].Values')}\n"
      data+="{\"instanceId\":\"$instance_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"tags\":$tags,\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[11].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[11].Values')}\n"
      data+="{\"instanceId\":\"$instance_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"tags\":$tags,\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[12].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[12].Values')}\n"
    fi
  fi
done
echo "[${data%???}]" > ec2_metrics.json
