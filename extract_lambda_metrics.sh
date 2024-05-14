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
      --start-time "$(date -u -v-1H)" \
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
        }
      ]' \
      --output json)
    lambda_metrics+="{\"function_name\":\"$function_name\",\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[0].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[0].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[1].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[1].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[2].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[2].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[3].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[3].Values')},\n"
    lambda_metrics+="{\"function_name\":\"$function_name\",\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[4].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[4].Values')},\n"
  fi
done

echo "[${lambda_metrics%???}]" > lambda_metrics.json