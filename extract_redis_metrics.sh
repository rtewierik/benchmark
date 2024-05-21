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
cluster_info=$(aws elasticache describe-cache-clusters \
  --query 'CacheClusters[?ReplicationGroupId==`'"redis-benchmark-cluster"'`].{CacheClusterId:CacheClusterId}' \
  --output json)
echo $cluster_info

data=()

# Loop through the instance info and retrieve metric data for each instance
for info in $(echo "${cluster_info}" | jq -c '.[]'); do
  cache_cluster_id=$(echo "${info}" | jq -r '.CacheClusterId')
  if [ "$cache_cluster_id" != "null" ] && [ -n "$cache_cluster_id" ]; then
    echo $cache_cluster_id

    # Retrieve metric data for the instance
    output=$(aws cloudwatch get-metric-data \
      --start-time "$(date -u -v-4H)" \
      --end-time "$(date -u)" \
      --metric-data-queries '[
        {
          "Id": "cpu_credit_balance",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "CPUCreditBalance",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "curr_volatile_items",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "CurrVolatileItems",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "string_based_cmds",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "StringBasedCmds",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "db0_average_ttl",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "DB0AverageTTL",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "network_bandwidth_in_allowance_exceeded",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "NetworkBandwidthInAllowanceExceeded",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "key_based_cmds",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "KeyBasedCmds",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "active_defrag_hits",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "ActiveDefragHits",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "engine_cpu_utilization",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "EngineCPUUtilization",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "network_bytes_out",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "NetworkBytesOut",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "memory_fragmentation_ratio",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "MemoryFragmentationRatio",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "set_type_cmds",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "SetTypeCmds",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "string_based_cmds_latency",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "StringBasedCmdsLatency",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "replication_bytes",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "ReplicationBytes",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "cache_misses",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "CacheMisses",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "save_in_progress",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "SaveInProgress",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "network_max_bytes_in",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "NetworkMaxBytesIn",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "curr_items",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "CurrItems",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "network_max_packets_out",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "NetworkMaxPacketsOut",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "cache_hits",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "CacheHits",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "network_bytes_in",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "NetworkBytesIn",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "key_based_cmds_latency",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "KeyBasedCmdsLatency",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "evictions",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "Evictions",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "replication_lag",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "ReplicationLag",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "network_max_bytes_out",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "NetworkMaxBytesOut",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "network_conntrack_allowance_exceeded",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "NetworkConntrackAllowanceExceeded",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "non_key_type_cmds",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "NonKeyTypeCmds",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "master_link_health_status",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "MasterLinkHealthStatus",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "network_packets_in",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "NetworkPacketsIn",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "traffic_management_active",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "TrafficManagementActive",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "hash_based_cmds_latency",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "HashBasedCmdsLatency",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "hash_based_cmds",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "HashBasedCmds",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "non_key_type_cmds_latency",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "NonKeyTypeCmdsLatency",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "set_type_cmds_latency",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "SetTypeCmdsLatency",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "cpu_utilization",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "CPUUtilization",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "bytes_used_for_cache",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "BytesUsedForCache",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "curr_connections",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "CurrConnections",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "network_bandwidth_out_allowance_exceeded",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "NetworkBandwidthOutAllowanceExceeded",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "network_packets_out",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "NetworkPacketsOut",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "cpu_credit_usage",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "CPUCreditUsage",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "network_max_packets_in",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "NetworkMaxPacketsIn",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "swap_usage",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "SwapUsage",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "database_memory_usage_percentage",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "DatabaseMemoryUsagePercentage",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "network_packets_per_second_allowance_exceeded",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "NetworkPacketsPerSecondAllowanceExceeded",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "reclaimed",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "Reclaimed",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "freeable_memory",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "FreeableMemory",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "new_connections",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "NewConnections",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "database_capacity_usage_percentage",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "DatabaseCapacityUsagePercentage",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        },
        {
          "Id": "is_aster",
          "MetricStat": {
            "Metric": {
              "Namespace": "AWS/ElastiCache",
              "MetricName": "IsMaster",
              "Dimensions": [
                {
                  "Name": "CacheClusterId",
                  "Value": "'"$cache_cluster_id"'"
                }
              ]
            },
            "Period": 60,
            "Stat": "Average"
          }
        }
      ]' \
      --output json)
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[0].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[0].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[0].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[1].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[1].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[1].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[2].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[2].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[2].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[3].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[3].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[3].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[4].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[4].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[4].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[5].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[5].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[5].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[6].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[6].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[6].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[7].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[7].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[7].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[8].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[8].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[8].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[9].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[9].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[9].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[10].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[10].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[10].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[11].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[11].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[11].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[12].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[12].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[12].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[13].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[13].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[13].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[14].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[14].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[14].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[15].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[15].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[15].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[16].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[16].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[16].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[17].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[17].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[17].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[18].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[18].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[18].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[19].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[19].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[19].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[20].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[20].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[20].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[21].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[21].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[21].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[22].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[22].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[22].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[23].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[23].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[23].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[24].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[24].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[24].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[25].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[25].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[25].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[26].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[26].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[26].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[27].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[27].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[27].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[28].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[28].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[28].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[29].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[29].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[29].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[30].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[30].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[30].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[31].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[31].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[31].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[32].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[32].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[32].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[33].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[33].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[33].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[34].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[34].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[34].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[35].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[35].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[35].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[36].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[36].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[36].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[37].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[37].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[37].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[38].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[38].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[38].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[39].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[39].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[39].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[40].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[40].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[40].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[41].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[41].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[41].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[42].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[42].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[42].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[43].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[43].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[43].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[44].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[44].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[44].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[45].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[45].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[45].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[46].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[46].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[46].Values')},\n"
    data+="{\"cache_cluster_id\":\"$cache_cluster_id\",\"timestamps\":$(echo "${output}" | jq -r '.MetricDataResults[47].Timestamps'),\"label\":\"$(echo "${output}" | jq -r '.MetricDataResults[47].Label')\",\"values\":$(echo "${output}" | jq -r '.MetricDataResults[47].Values')},\n"
  fi
done
echo "[${data%???}]" > redis_metrics.json