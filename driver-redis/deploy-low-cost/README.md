## Benchmarking commands to execute

```
sudo bin/benchmark \
  --drivers driver-redis/redis-default.yaml \
  workloads/max-rate-10-topics-1-partition-1kb.yaml
```

```
sudo bin/benchmark \
  --drivers driver-redis/redis-default.yaml \
  workloads/simple-workload.yaml
```

## Additional manual steps to fix deployment of Redis

**NOTE:** The current Terraform project creates default security groups to which the 0.0.0.0/0 inbound rule needs to be added for the Redis cluster to be reachable. A fix should be implemented.

### Locations to configure memory in case of changing instance size

* `deploy.yaml`requires modification of the `Configure memory` task
* The rest of the infrastructure is all provided by AWS. The instance size and number of nodes in the Redis cluster should be verified to ensure benchmark success.

