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

ALL-MAX-SINGLE
sudo bin/benchmark \
--drivers driver-pravega/pravega-experiment.yaml \
workloads/throughput-10kb-100-max.yaml workloads/throughput-100b-10-max.yaml workloads/throughput-100b-100-max.yaml workloads/throughput-100b-500-max.yaml workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml workloads/throughput-10kb-500-max.yaml

sudo bin/benchmark \
--drivers driver-pravega/pravega-experiment.yaml \
workloads/throughput-1kb-10-max.yaml workloads/throughput-1kb-100-max.yaml workloads/throughput-1kb-500-max.yaml workloads/throughput-10kb-10-max.yaml workloads/throughput-10kb-100-max.yaml workloads/throughput-10kb-500-max.yaml

TPC-H
sudo bin/benchmark \
--drivers driver-pravega/pravega-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml,

,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml,workloads/tpc-h-q1-1000-100.yaml,workloads/tpc-h-q1-100-10.yaml,workloads/tpc-h-q6-1000-100.yaml,workloads/tpc-h-q6-100-10.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-pravega/pravega-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-100.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-pravega/pravega-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-300.yaml,workloads/tpc-h-q1-100-30.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml,workloads/tpc-h-q1-1000-300.yaml,workloads/tpc-h-q1-100-30.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml,workloads/tpc-h-q1-1000-300.yaml,workloads/tpc-h-q1-100-30.yaml,workloads/tpc-h-q6-1000-300.yaml,workloads/tpc-h-q6-100-30.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-pravega/pravega-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-1000-500.yaml,workloads/tpc-h-q1-100-50.yaml,workloads/tpc-h-q6-1000-500.yaml,workloads/tpc-h-q6-100-50.yaml,workloads/tpc-h-q1-1000-500.yaml,workloads/tpc-h-q1-100-50.yaml,workloads/tpc-h-q6-1000-500.yaml,workloads/tpc-h-q6-100-50.yaml,workloads/tpc-h-q1-1000-500.yaml,workloads/tpc-h-q1-100-50.yaml,workloads/tpc-h-q6-1000-500.yaml,workloads/tpc-h-q6-100-50.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-pravega/pravega-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-10000-300.yaml,workloads/tpc-h-q6-10000-300.yaml,workloads/tpc-h-q1-10000-500.yaml,workloads/tpc-h-q6-10000-500.yaml,workloads/tpc-h-q1-10000-800.yaml,workloads/tpc-h-q6-10000-800.yaml \
workloads/tpc-h-base-long.yaml

sudo bin/benchmark \
--drivers driver-pravega/pravega-experiment.yaml \
--tpc-h-files workloads/tpc-h-q1-10000-300.yaml \
workloads/tpc-h-base-long.yaml

### Locations to configure memory in case of changing instance size

* `bkenv.sh` requires modification of allocated environment variables (`common.sh` also applies memory configuration, but only as defaults)
* `config.properties` requires modification of `pravegaservice.cache.size.max`. Note that this memory is always fully allocated even if not used, and the Pravega Segment Store is running on the Bookkeeper EC2 instance along with another service. **The sum of allocated memory for both services should be less than the total memory available for the EC2 instance type**.
* `pravega-segmentstore.service` requires modification of `Environment`
* `deploy.yaml`requires modification of the `Configure memory` task

## Extracting metrics from EC2 instances after running the benchmarks

Run the command `sh ../../extract_metrics.sh pravega-benchmark-ruben-te-wierik`.

## Insights gained
Many if not all of the cloud-agnostic drivers rely on direct memory as well as JVM-based memory (read: heap), and the direct memory limits are not specified for all of the processes.
For example, the Bookkeeper host runs two processes: the bookkeeper and the Pravega Segment Store. The Pravega Segment Store uses 6 GB of JVM memory and 24 GB of direct memory for byte buffers. THe i3en.2xlarge instance has 64 GB of memory in total of which 2-4 GB should be left to the operating system, so another 30 GB is allocated elsewhere, IMPLICITLY. The Bookkeeper process uses this memory, likely also for byte buffers, to persist data. The Pravega Segment Store cache is likely used for both read and writes initially, and the system aims to use this cache up to 90% after which cache entries are evicted and written to disk through Bookkeeper.