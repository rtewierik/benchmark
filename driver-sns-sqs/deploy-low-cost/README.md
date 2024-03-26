## Benchmarking commands to execute

```
sudo bin/benchmark \
  --drivers driver-sns-sqs/sns-sqs-default.yaml \
  workloads/max-rate-10-topics-1-partition-1kb.yaml
```
```
sudo bin/benchmark \
  --drivers driver-sns-sqs/sns-sqs-default.yaml \
  workloads/simple-workload.yaml
```

### Locations to configure memory in case of changing instance size

* `deploy.yaml`requires modification of the `Configure memory` task