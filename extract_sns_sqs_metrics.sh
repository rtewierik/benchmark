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

list_sqs_queues() {
    local substring="$1"

    # List SQS queues and filter based on the provided substring
    aws sqs list-queues --output json | \
    jq -r --arg substring "$substring" '.QueueUrls[] | select(contains($substring))'
}

extract_topic_name() {
    local arn="$1"
    local topic_name="${arn##*:}"
    echo "$topic_name"
}

extract_queue_name() {
    local url="$1"
    local queue_name=$(basename "$url")
    echo "$queue_name"
}

queue_info=$(list_sqs_queues "sns-sqs-consumer-lambda")
echo $queue_info

topic_info=$(list_sns_topics "sns-sqs-consumer-lambda")
echo $topic_info

sns_metrics=()
sqs_metrics=()

for queue_url in $queue_info; do
  if [ "$queue_url" != "null" ] && [ -n "$queue_url" ]; then
    queue_name=$(extract_queue_name $queue_url)
    echo $queue_name

    output=$(aws cloudwatch get-metric-data \
      --start-time "$(date -u -v-4H)" \
      --end-time "$(date -u)" \
      --metric-data-queries '[
        {
          "Id": "sent_message_size",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/SQS",
              "MetricName": "SentMessageSize",
              "Dimensions": [
                {
                  "Name": "QueueName",
                  "Value": "'"$queue_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "approximate_number_of_messages_visible",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/SQS",
              "MetricName": "ApproximateNumberOfMessagesVisible",
              "Dimensions": [
                {
                  "Name": "QueueName",
                  "Value": "'"$queue_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "approximate_number_of_messages_not_visible",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/SQS",
              "MetricName": "ApproximateNumberOfMessagesNotVisible",
              "Dimensions": [
                {
                  "Name": "QueueName",
                  "Value": "'"$queue_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "approximate_number_of_messages_delayed",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/SQS",
              "MetricName": "ApproximateNumberOfMessagesDelayed",
              "Dimensions": [
                {
                  "Name": "QueueName",
                  "Value": "'"$queue_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "approximate_age_of_oldest_message",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/SQS",
              "MetricName": "ApproximateAgeOfOldestMessage",
              "Dimensions": [
                {
                  "Name": "QueueName",
                  "Value": "'"$queue_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "number_of_empty_receives",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/SQS",
              "MetricName": "NumberOfEmptyReceives",
              "Dimensions": [
                {
                  "Name": "QueueName",
                  "Value": "'"$queue_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "number_of_messages_sent",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/SQS",
              "MetricName": "NumberOfMessagesSent",
              "Dimensions": [
                {
                  "Name": "QueueName",
                  "Value": "'"$queue_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "number_of_messages_received",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/SQS",
              "MetricName": "NumberOfMessagesReceived",
              "Dimensions": [
                {
                  "Name": "QueueName",
                  "Value": "'"$queue_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "number_of_messages_deleted",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/SQS",
              "MetricName": "NumberOfMessagesDeleted",
              "Dimensions": [
                {
                  "Name": "QueueName",
                  "Value": "'"$queue_name"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        }
      ]' \
      --output json)
    sqs_metrics+="{\"queue_name\":\"$queue_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[0].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[0].Values')},\n"
    sqs_metrics+="{\"queue_name\":\"$queue_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[1].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[1].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[1].Values')},\n"
    sqs_metrics+="{\"queue_name\":\"$queue_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[2].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[2].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[2].Values')},\n"
    sqs_metrics+="{\"queue_name\":\"$queue_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[3].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[3].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[3].Values')},\n"
    sqs_metrics+="{\"queue_name\":\"$queue_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[4].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[4].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[4].Values')},\n"
    sqs_metrics+="{\"queue_name\":\"$queue_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[5].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[5].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[5].Values')},\n"
    sqs_metrics+="{\"queue_name\":\"$queue_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[6].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[6].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[6].Values')},\n"
    sqs_metrics+="{\"queue_name\":\"$queue_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[7].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[7].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[7].Values')},\n"
    sqs_metrics+="{\"queue_name\":\"$queue_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[8].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[8].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[8].Values')},\n"
  fi
done

for topic_arn in $topic_info; do
  if [ "$topic_arn" != "null" ] && [ -n "$topic_arn" ]; then
    echo $topic_arn
    topic_name=$(extract_topic_name $topic_arn)

    output=$(aws cloudwatch get-metric-data \
      --start-time "$(date -u -v-4H)" \
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
    sns_metrics+="{\"topic_name\":\"$topic_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[0].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[0].Values')},\n"
    sns_metrics+="{\"topic_name\":\"$topic_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[1].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[1].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[1].Values')},\n"
    sns_metrics+="{\"topic_name\":\"$topic_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[2].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[2].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[2].Values')},\n"
    sns_metrics+="{\"topic_name\":\"$topic_name\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[3].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[3].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[3].Values')},\n"
  fi
done

echo "[${sns_metrics%???}]" > sns_metrics.json
echo "[${sqs_metrics%???}]" > sqs_metrics.json