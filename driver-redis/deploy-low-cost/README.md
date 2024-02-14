```
sudo bin/benchmark \
  --drivers driver-redis/redis-default.yaml \
  workloads/max-rate-10-topics-1-partition-1kb.yaml
```

**NOTE:** The current Terraform project creates default security groups to which the 0.0.0.0/0 inbound rule needs to be added for the Redis cluster to be reachable. A fix should be implemented.