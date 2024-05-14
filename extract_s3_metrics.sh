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

s3_metrics=()
bucket_name=$1
echo $bucket_name

output=$(aws cloudwatch get-metric-data \
  --start-time "$(date -u -v-1H)" \
  --end-time "$(date -u)" \
  --metric-data-queries '[
    {
      "Id": "number_of_objects",
      "MetricStat": {
        "Metric": {
          "Namespace": "AWS/S3",
          "MetricName": "NumberOfObjects",
          "Dimensions": [
            {
              "Name": "BucketName",
              "Value": "'"$bucket_name"'"
            }
          ]
        },
        "Period": 60,
        "Stat": "Average"
      }
    },
    {
      "Id": "bucket_size_bytes",
      "MetricStat": {
        "Metric": {
          "Namespace": "AWS/S3",
          "MetricName": "BucketSizeBytes",
          "Dimensions": [
            {
              "Name": "BucketName",
              "Value": "'"$bucket_name"'"
            }
          ]
        },
        "Period": 60,
        "Stat": "Average"
      }
    }
  ]' \
  --output json)
s3_metrics+="{\"bucket_name\":\"$bucket_name\",\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[0].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[0].Values')},\n"
s3_metrics+="{\"bucket_name\":\"$bucket_name\",\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[1].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[1].Values')},\n"

echo "[${s3_metrics%???}]" > s3_metrics.json