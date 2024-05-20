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

list_lambda_functions() {
    local substring="$1"

    # List Lambda functions and filter based on the provided substring
    aws lambda list-functions --output json | \
    jq -r --arg substring "$substring" '.Functions[] | select(.FunctionName | contains($substring)) | .FunctionName'
}

lambda_metrics=()
lambda_name=$1
echo $lambda_name

lambda_functions=$(list_lambda_functions $lambda_name)

for function_name in $lambda_functions; do
  if [ "$function_name" != "null" ] && [ -n "$function_name" ]; then
  echo $function_name
    output=$(aws cloudwatch get-metric-data \
      --start-time "$(date -u -v-3H)" \
      --end-time "$(date -u)" \
      --metric-data-queries '[
        {
          "Id": "throttles",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/Lambda",
              "MetricName": "Throttles",
              "Dimensions": [
                {
                  "Name": "FunctionName",
                  "Value": "'"$function_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "duration",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/Lambda",
              "MetricName": "Duration",
              "Dimensions": [
                {
                  "Name": "FunctionName",
                  "Value": "'"$function_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "concurrent_executions",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/Lambda",
              "MetricName": "ConcurrentExecutions",
              "Dimensions": [
                {
                  "Name": "FunctionName",
                  "Value": "'"$function_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "invocations",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/Lambda",
              "MetricName": "Invocations",
              "Dimensions": [
                {
                  "Name": "FunctionName",
                  "Value": "'"$function_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "errors",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/Lambda",
              "MetricName": "Errors",
              "Dimensions": [
                {
                  "Name": "FunctionName",
                  "Value": "'"$function_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "rx_bytes",
          "MetricStat": {
            "Metric": {
              "Namespace": "LambdaInsights",
              "MetricName": "rx_bytes",
              "Dimensions": [
                {
                  "Name": "function_name",
                  "Value": "'"$function_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "total_network",
          "MetricStat": {
            "Metric": {
              "Namespace": "LambdaInsights",
              "MetricName": "total_network",
              "Dimensions": [
                {
                  "Name": "function_name",
                  "Value": "'"$function_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "tx_bytes",
          "MetricStat": {
            "Metric": {
              "Namespace": "LambdaInsights",
              "MetricName": "tx_bytes",
              "Dimensions": [
                {
                  "Name": "function_name",
                  "Value": "'"$function_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "used_memory_max",
          "MetricStat": {
            "Metric": {
              "Namespace": "LambdaInsights",
              "MetricName": "used_memory_max",
              "Dimensions": [
                {
                  "Name": "function_name",
                  "Value": "'"$function_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "tmp_used",
          "MetricStat": {
            "Metric": {
              "Namespace": "LambdaInsights",
              "MetricName": "tmp_used",
              "Dimensions": [
                {
                  "Name": "function_name",
                  "Value": "'"$function_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "init_duration",
          "MetricStat": {
            "Metric": {
              "Namespace": "LambdaInsights",
              "MetricName": "init_duration",
              "Dimensions": [
                {
                  "Name": "function_name",
                  "Value": "'"$function_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "cpu_total_time",
          "MetricStat": {
            "Metric": {
              "Namespace": "LambdaInsights",
              "MetricName": "cpu_total_time",
              "Dimensions": [
                {
                  "Name": "function_name",
                  "Value": "'"$function_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "memory_utilization",
          "MetricStat": {
            "Metric": {
              "Namespace": "LambdaInsights",
              "MetricName": "memory_utilization",
              "Dimensions": [
                {
                  "Name": "function_name",
                  "Value": "'"$function_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "total_memory",
          "MetricStat": {
            "Metric": {
              "Namespace": "LambdaInsights",
              "MetricName": "total_memory",
              "Dimensions": [
                {
                  "Name": "function_name",
                  "Value": "'"$function_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        }
      ]' \
      --output json)
    lambda_metrics+="{\"function_name\":\"$function_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[0].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[0].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[1].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[1].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[1].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[2].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[2].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[2].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[3].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[3].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[3].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[4].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[4].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[4].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[5].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[5].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[5].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[6].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[6].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[6].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[7].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[7].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[7].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[8].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[8].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[8].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[9].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[9].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[9].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[10].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[10].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[10].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[11].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[11].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[11].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[12].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[12].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[12].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[13].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[13].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[13].Values')},\n"
  fi
done

echo "[${lambda_metrics%???}]" > lambda_metrics.json