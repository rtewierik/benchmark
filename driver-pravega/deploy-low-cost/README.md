## Benchmarking commands to execute

```
sudo bin/benchmark \
  --drivers driver-pravega/pravega.yaml \
  workloads/simple-workload-short.yaml
```

```
sudo bin/benchmark \
  --drivers driver-pravega/pravega.yaml \
  workloads/throughput-10kb-500.yaml
```

```
sudo bin/benchmark \
  --drivers driver-pravega/pravega.yaml \
  --tpc-h-file workloads/tpc-h-default-2-reducers.yaml \
  workloads/simple-workload.yaml
```
ALL-MAX
sudo bin/benchmark \
--drivers driver-pravega/pravega-experiment.yaml \
workloads/throughput-100b-10-max.yaml workloads/throughput-100b-100-max.yaml workloads/throughput-100b-500-max.yaml workloads/throughput-100b-10-max.yaml workloads/throughput-100b-100-max.yaml workloads/throughput-100b-500-max.yaml workloads/throughput-100b-10-max.yaml workloads/throughput-100b-100-max.yaml workloads/throughput-100b-500-max.yaml workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-500-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-500-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-500-max.yaml

ALL-MAX
sudo bin/benchmark \
--drivers driver-pravega/pravega-experiment.yaml \
workloads/throughput-100b-500-max.yaml workloads/throughput-100b-500-max.yaml workloads/throughput-100b-500-max.yaml workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-500-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-500-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-500-max.yaml

ALL-MAX
sudo bin/benchmark \
--drivers driver-pravega/pravega-experiment.yaml \
workloads/throughput-1kb-500-max.yaml
workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml

ALL-MAX
sudo bin/benchmark \
--drivers driver-pravega/pravega-experiment.yaml \
workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-500-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-500-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-500-max.yaml

sudo bin/benchmark \
--drivers driver-pravega/pravega-experiment-autoscaling.yaml \
workloads/throughput-1kb-10-max.yaml

ALL-MAX + AUTO-SCALING
sudo bin/benchmark \
--drivers driver-pravega/pravega-experiment-autoscaling.yaml \
workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-500-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-500-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-500-max.yaml

### Locations to configure memory in case of changing instance size

* `bkenv.sh` requires modification of allocated environment variables (`common.sh` also applies memory configuration, but only as defaults)
* `config.properties` requires modification of `pravegaservice.cache.size.max`. Note that this memory is always fully allocated even if not used, and the Pravega Segment Store is running on the Bookkeeper EC2 instance along with another service. **The sum of allocated memory for both services should be less than the total memory available for the EC2 instance type**.
* `pravega-segmentstore.service` requires modification of `Environment`
* `deploy.yaml`requires modification of the `Configure memory` task

## Extracting metrics from EC2 instances after running the benchmarks

Run the command `sh ../../extract_metrics.sh pravega-benchmark-ruben-te-wierik`.
