## Benchmarking commands to execute

```
sudo bin/benchmark \
  --drivers driver-rabbitmq/rabbitmq-quorum.yaml \
  workloads/simple-workload-1-partition.yaml
```

```
sudo bin/benchmark \
  --drivers driver-rabbitmq/rabbitmq-quorum.yaml \
  workloads/throughput-10kb-500.yaml
```

```
sudo bin/benchmark \
  --drivers driver-rabbitmq/rabbitmq-quorum.yaml \
  --tpc-h-file workloads/tpc-h-default-4-reducers.yaml \
  workloads/simple-workload-1-partition.yaml
```

```
sudo bin/benchmark \
  --drivers driver-rabbitmq/rabbitmq-quorum.yaml \
  --tpc-h-files workloads/tpc-h-q6-10000-800.yaml \
  workloads/tpc-h-base-long.yaml
```

```
sudo bin/benchmark \
  --drivers driver-rabbitmq/rabbitmq-quorum.yaml \
  --tpc-h-files workloads/tpc-h-q6-10000-10.yaml \
  workloads/tpc-h-base-long.yaml
```

```
sudo bin/benchmark \
  --drivers driver-rabbitmq/rabbitmq-quorum.yaml \
  workloads/throughput-100b-100.yaml
```

```
sudo bin/benchmark \
  --drivers driver-rabbitmq/rabbitmq-quorum.yaml \
  --tpc-h-files workloads/tpc-h-q6-10000-500.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml,workloads/tpc-h-q1-10000-300.yaml,workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-10000-300.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml,workloads/tpc-h-q1-10000-300.yaml,workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-10000-300.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml,workloads/tpc-h-q1-10000-300.yaml,workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-10000-300.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml,workloads/tpc-h-q1-10000-500.yaml,workloads/tpc-h-q1-1000-300.yaml,workloads/tpc-h-q1-100-30.yaml,workloads/tpc-h-q6-10000-500.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml,workloads/tpc-h-q1-10000-500.yaml,workloads/tpc-h-q1-1000-300.yaml,workloads/tpc-h-q1-100-30.yaml,workloads/tpc-h-q6-10000-500.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml,workloads/tpc-h-q1-10000-500.yaml,workloads/tpc-h-q1-1000-300.yaml,workloads/tpc-h-q1-100-30.yaml,workloads/tpc-h-q6-10000-500.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml,workloads/tpc-h-q1-10000-800.yaml,workloads/tpc-h-q1-1000-500.yaml,workloads/tpc-h-q1-100-50.yaml,workloads/tpc-h-q6-10000-800.yaml,workloads/tpc-h-q6-1000-500.yaml,workloads/tpc-h-q6-100-50.yaml,workloads/tpc-h-q1-10000-800.yaml,workloads/tpc-h-q1-1000-500.yaml,workloads/tpc-h-q1-100-50.yaml,workloads/tpc-h-q6-10000-800.yaml,workloads/tpc-h-q6-1000-500.yaml,workloads/tpc-h-q6-100-50.yaml,workloads/tpc-h-q1-10000-800.yaml,workloads/tpc-h-q1-1000-500.yaml,workloads/tpc-h-q1-100-50.yaml,workloads/tpc-h-q6-10000-800.yaml,workloads/tpc-h-q6-1000-500.yaml,workloads/tpc-h-q6-100-50.yaml \
  workloads/tpc-h-base-long.yaml
```

```
sudo bin/benchmark \
  --drivers driver-rabbitmq/rabbitmq-experiment.yaml \
  --tpc-h-files workloads/tpc-h-q6-100-50.yaml,workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml,workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml,workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml \
  workloads/tpc-h-base-long.yaml
```

sudo bin/benchmark \
--drivers driver-rabbitmq/rabbitmq-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml,workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml,workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-rabbitmq/rabbitmq-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-300.yaml,workloads/tpc-h-q1-100-30.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml,workloads/tpc-h-q1-1000-300.yaml,workloads/tpc-h-q1-100-30.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml,workloads/tpc-h-q1-1000-300.yaml,workloads/tpc-h-q1-100-30.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-rabbitmq/rabbitmq-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-500.yaml,workloads/tpc-h-q1-100-50.yaml,workloads/tpc-h-q6-1000-500.yaml,workloads/tpc-h-q6-100-50.yaml,workloads/tpc-h-q1-1000-500.yaml,workloads/tpc-h-q1-100-50.yaml,workloads/tpc-h-q6-1000-500.yaml,workloads/tpc-h-q6-100-50.yaml,workloads/tpc-h-q1-1000-500.yaml,workloads/tpc-h-q1-100-50.yaml,workloads/tpc-h-q6-1000-500.yaml,workloads/tpc-h-q6-100-50.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-rabbitmq/rabbitmq-experiment.yaml \
--tpc-h-files workloads/tpc-h-q6-1000-500.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-rabbitmq/rabbitmq-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-10000-300.yaml workloads/tpc-h-q1-10000-500.yaml workloads/tpc-h-q1-10000-800.yaml workloads/tpc-h-q1-10000-300.yaml workloads/tpc-h-q1-10000-500.yaml workloads/tpc-h-q1-10000-800.yaml workloads/tpc-h-q1-10000-300.yaml workloads/tpc-h-q1-10000-500.yaml workloads/tpc-h-q1-10000-800.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-rabbitmq/rabbitmq-experiment.yaml \
workloads/throughput-100b-10.yaml workloads/throughput-100b-100.yaml workloads/throughput-100b-500.yaml workloads/throughput-100b-10.yaml workloads/throughput-100b-100.yaml workloads/throughput-100b-500.yaml workloads/throughput-100b-10.yaml workloads/throughput-100b-100.yaml workloads/throughput-100b-500.yaml

sudo bin/benchmark \
--drivers driver-rabbitmq/rabbitmq-experiment.yaml \
workloads/throughput-1kb-10.yaml workloads/throughput-1kb-100.yaml workloads/throughput-1kb-500.yaml workloads/throughput-1kb-10.yaml workloads/throughput-1kb-100.yaml workloads/throughput-1kb-500.yaml workloads/throughput-1kb-10.yaml workloads/throughput-1kb-100.yaml workloads/throughput-1kb-500.yaml

sudo bin/benchmark \
--drivers driver-rabbitmq/rabbitmq-experiment.yaml \
workloads/throughput-10kb-10.yaml workloads/throughput-10kb-100.yaml workloads/throughput-10kb-500.yaml workloads/throughput-10kb-10.yaml workloads/throughput-10kb-100.yaml workloads/throughput-10kb-500.yaml workloads/throughput-10kb-10.yaml workloads/throughput-10kb-100.yaml workloads/throughput-10kb-500.yaml

sudo bin/benchmark \
--drivers driver-rabbitmq/rabbitmq-experiment.yaml \
workloads/max-rate-1-topic-10-partitions-10p-10c-1kb.yaml

### Locations to configure memory in case of changing instance size

* `deploy.yaml`requires modification of the `Configure memory` tasks.
* The RabbitMQ installation does not reference any memory, so supposedly it uses as much memory as it needs. This should be investigated further to ensure no issues occur during future benchmarking.

## Extracting metrics from EC2 instances after running the benchmarks

Run the command `sh ../../extract_metrics.sh rabbitmq-benchmark-ruben-te-wierik`.
