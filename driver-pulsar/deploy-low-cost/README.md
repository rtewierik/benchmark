## Benchmarking commands to execute

```
sudo bin/benchmark \
  --drivers driver-pulsar/pulsar-effectively-once.yaml \
  workloads/simple-workload-short.yaml
```

```
sudo bin/benchmark \
  --drivers driver-pulsar/pulsar-effectively-once.yaml \
  --tpc-h-file workloads/tpc-h-default.yaml \
  workloads/simple-workload.yaml
```

### Locations to configure memory in case of changing instance size

* `bkenv.sh` requires modification of allocated environment variables (`pulsar_env.sh` also applies memory configuration, but only as defaults)
* `deploy.yaml`requires modification of the `Configure memory` task.
* Either the memory configuration including `max_heap_memory` in `deploy.yaml` or the extra variables in `extra_vars.yaml` need to be modified ensure the correct memory configuration is applied to all hosts.

