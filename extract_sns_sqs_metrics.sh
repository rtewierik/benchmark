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

list_sns_topics() {
    local substring="$1"

    # List SNS topics and filter based on the provided substring
    aws sns list-topics --output json | \
    jq -r --arg substring "$substring" '.Topics[] | select(.TopicArn | contains($substring)) | .TopicArn'
}

extract_topic_name() {
    local arn="$1"
    local topic_name="${arn##*:}"
    echo "$topic_name"
}

echo $1
topic_info=$(list_sns_topics "sns-sqs-consumer-lambda-sns-topic")
echo $topic_info

data=()

for topic_arn in $topic_info; do
  if [ "$topic_arn" != "null" ] && [ -n "$topic_arn" ]; then
    echo $topic_arn
    topic_name=$(extract_topic_name $topic_arn)

    output=$(aws cloudwatch get-metric-data \
      --start-time "$(date -u -v-1H)" \
      --end-time "$(date -u)" \
      --metric-data-queries '[
        {
          "Id": "number_of_notifications_failed",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/SNS",
              "MetricName": "NumberOfNotificationsFailed",
              "Dimensions": [
                {
                  "Name": "TopicName",
                  "Value": "'"$topic_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "number_of_notifications_delivered",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/SNS",
              "MetricName": "NumberOfNotificationsDelivered",
              "Dimensions": [
                {
                  "Name": "TopicName",
                  "Value": "'"$topic_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "publish_size",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/SNS",
              "MetricName": "PublishSize",
              "Dimensions": [
                {
                  "Name": "TopicName",
                  "Value": "'"$topic_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "number_of_messages_published",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/SNS",
              "MetricName": "NumberOfMessagesPublished",
              "Dimensions": [
                {
                  "Name": "TopicName",
                  "Value": "'"$topic_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        }
      ]' \
      --output json)
    data+="{\"topic_name\":\"$topic_name\",\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[0].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[0].Values')},\n"
    data+="{\"topic_name\":\"$topic_name\",\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[1].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[1].Values')},\n"
    data+="{\"topic_name\":\"$topic_name\",\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[2].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[2].Values')},\n"
    data+="{\"topic_name\":\"$topic_name\",\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[3].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[3].Values')},\n"
  fi
done
echo "[${data%???}]" > sns_sqs_metrics.json