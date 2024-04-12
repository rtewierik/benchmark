## Benchmarking commands to execute

```
sudo bin/benchmark \
  --drivers driver-rabbitmq/rabbitmq-quorum.yaml \
  workloads/simple-workload-1-partition.yaml
```

```
sudo bin/benchmark \
  --drivers driver-rabbitmq/rabbitmq-quorum.yaml \
  --tpc-h-file workloads/tpc-h-default-4-reducers.yaml \
  workloads/simple-workload-1-partition.yaml
```

### Locations to configure memory in case of changing instance size

* `deploy.yaml`requires modification of the `Configure memory` tasks.
* The RabbitMQ installation does not reference any memory, so supposedly it uses as much memory as it needs. This should be investigated further to ensure no issues occur during future benchmarking.

