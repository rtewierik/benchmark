## Benchmarking commands to execute

```
sudo bin/benchmark \
  --drivers driver-pulsar/pulsar-effectively-once.yaml \
  workloads/simple-workload-short-1-partition.yaml
```

```
sudo bin/benchmark \
  --drivers driver-pulsar/pulsar-experiment.yaml \
  workloads/simple-workload-short-1-partition.yaml
```

```
sudo bin/benchmark \
  --drivers driver-pulsar/pulsar-experiment.yaml \
  workloads/throughput-10kb-500.yaml
```

```
sudo bin/benchmark \
  --drivers driver-pulsar/pulsar-effectively-once.yaml \
  --tpc-h-file workloads/tpc-h-default.yaml \
  workloads/simple-workload.yaml
```

```
sudo bin/benchmark \
  --drivers driver-pulsar/pulsar-experiment.yaml \
  --tpc-h-file workloads/tpc-h-default.yaml \
  workloads/simple-workload-1-partition.yaml
```

sudo bin/benchmark \
--drivers driver-pulsar/pulsar-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-pulsar/pulsar-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-100.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-pulsar/pulsar-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml,workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml,workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-pulsar/pulsar-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-300.yaml,workloads/tpc-h-q1-100-30.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml,workloads/tpc-h-q1-1000-300.yaml,workloads/tpc-h-q1-100-30.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml,workloads/tpc-h-q1-1000-300.yaml,workloads/tpc-h-q1-100-30.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-pulsar/pulsar-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-500.yaml,workloads/tpc-h-q1-100-50.yaml,workloads/tpc-h-q6-1000-500.yaml,workloads/tpc-h-q6-100-50.yaml,workloads/tpc-h-q1-1000-500.yaml,workloads/tpc-h-q1-100-50.yaml,workloads/tpc-h-q6-1000-500.yaml,workloads/tpc-h-q6-100-50.yaml,workloads/tpc-h-q1-1000-500.yaml,workloads/tpc-h-q1-100-50.yaml,workloads/tpc-h-q6-1000-500.yaml,workloads/tpc-h-q6-100-50.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-pulsar/pulsar-experiment.yaml \
workloads/throughput-100b-10-max.yaml workloads/throughput-100b-100-max.yaml workloads/throughput-100b-500-max.yaml workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml

sudo bin/benchmark \
--drivers driver-pulsar/pulsar-experiment.yaml \
workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-500-max.yaml

sudo bin/benchmark \
--drivers driver-pulsar/pulsar-experiment.yaml \
workloads/throughput-1kb-250-max.yaml

### Locations to configure memory in case of changing instance size

* `bkenv.sh` requires modification of allocated environment variables (`pulsar_env.sh` also applies memory configuration, but only as defaults)
* `deploy.yaml`requires modification of the `Configure memory` task.
* Either the memory configuration including `max_heap_memory` in `deploy.yaml` or the extra variables in `extra_vars.yaml` need to be modified ensure the correct memory configuration is applied to all hosts.

## Extracting metrics from EC2 instances after running the benchmarks

Run the command `sh ../../extract_metrics.sh pulsar-benchmark-ruben-te-wierik`.
