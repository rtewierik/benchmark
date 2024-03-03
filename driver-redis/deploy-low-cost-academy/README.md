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

## AWS Academy instructions

* Create Redis cache in CLUSTER mode manually using AWS console
* Create three EC2 `t3.large` instances with RHEL image and arm64 architecture (launch template saved in Academy account)
* Add `inventory.ini` as follows

```
  [ec2_instances]
  ec2-01 ansible_host=xx.xx.xx.xx
  ec2-02 ansible_host=yy.yy.yy.yy
  ec2-03 ansible_host=zz.zz.zz.zz
```

* Execute playbook e.g. `ansible-playbook -i inventory.ini your_playbook.yml`

```
ansible-playbook -K \
  --user ec2-user \
  -i inventory.ini \
  deploy.yaml
```

* Need to use redis_aws_academy PEM key with 400 file permissions