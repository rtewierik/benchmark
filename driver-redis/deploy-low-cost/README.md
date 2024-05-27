## Benchmarking commands to execute

```
sudo bin/benchmark \
  --drivers driver-redis/redis-default.yaml \
  workloads/max-rate-10-topics-1-partition-1kb.yaml
```

```
sudo bin/benchmark \
  --drivers driver-redis/redis-default.yaml \
  workloads/simple-workload-short.yaml
```

```
sudo bin/benchmark \
  --drivers driver-redis/redis-default.yaml \
  workloads/throughput-10kb-500.yaml
```

```
sudo bin/benchmark \
  --drivers driver-redis/redis-default.yaml \
  --tpc-h-file workloads/tpc-h-default.yaml \
  workloads/simple-workload.yaml
```

sudo bin/benchmark \
--drivers driver-redis/redis-experiment.yaml \
--tpc-h-files workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-redis/redis-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-100.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-redis/redis-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml,workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml,workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-redis/redis-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-300.yaml,workloads/tpc-h-q1-100-30.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml,workloads/tpc-h-q1-1000-300.yaml,workloads/tpc-h-q1-100-30.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml,workloads/tpc-h-q1-1000-300.yaml,workloads/tpc-h-q1-100-30.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-redis/redis-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-500.yaml,workloads/tpc-h-q1-100-50.yaml,workloads/tpc-h-q6-1000-500.yaml,workloads/tpc-h-q6-100-50.yaml,workloads/tpc-h-q1-1000-500.yaml,workloads/tpc-h-q1-100-50.yaml,workloads/tpc-h-q6-1000-500.yaml,workloads/tpc-h-q6-100-50.yaml,workloads/tpc-h-q1-1000-500.yaml,workloads/tpc-h-q1-100-50.yaml,workloads/tpc-h-q6-1000-500.yaml,workloads/tpc-h-q6-100-50.yaml \
workloads/tpc-h-base-long.yaml

ALL-MAX-SINGLE
sudo bin/benchmark \
--drivers driver-redis/redis-experiment.yaml \
workloads/throughput-100b-10-max.yaml workloads/throughput-100b-100-max.yaml workloads/throughput-100b-500-max.yaml workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-500-max.yaml

## Additional manual steps to fix deployment of Redis

**NOTE:** The current Terraform project creates default security groups to which the 0.0.0.0/0 inbound rule needs to be added for the Redis cluster to be reachable. A fix should be implemented.

### Locations to configure memory in case of changing instance size

* `deploy.yaml`requires modification of the `Configure memory` task
* The rest of the infrastructure is all provided by AWS. The instance size and number of nodes in the Redis cluster should be verified to ensure benchmark success.

## Extracting metrics from EC2 instances after running the benchmarks

Run the command `sh ../../extract_metrics.sh redis-benchmark-ruben-te-wierik`.

## Extracting metrics from Redis cache nodes after running the benchmarks

Run the command `sh ../../extract_redis_metrics.sh`.
