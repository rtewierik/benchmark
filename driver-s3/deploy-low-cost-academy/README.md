## Benchmarking commands to execute

```
sudo bin/benchmark \
  --drivers driver-s3/s3-default.yaml \
  workloads/max-rate-10-topics-1-partition-1kb.yaml
```
```
sudo bin/benchmark \
  --drivers driver-s3/s3-default.yaml \
  workloads/simple-workload.yaml
```

### Locations to configure memory in case of changing instance size

* `deploy.yaml`requires modification of the `Configure memory` task

## AWS Academy instructions

* Create three EC2 `t3.large` instances with RHEL image and arm64 architecture (launch template saved in Academy account)
* Add `inventory.ini` as follows

```
  [ec2_instances]
  ec2-01 ansible_host=xx.xx.xx.xx
  ec2-02 ansible_host=yy.yy.yy.yy
  ec2-03 ansible_host=zz.zz.zz.zz
```
* Update `host_vars/*` with the private IP addresses of each of the EC2 instances.
* Execute playbook e.g. `ansible-playbook -i inventory.ini your_playbook.yml`

```
ansible-playbook -K \
  --user ec2-user \
  -i inventory.ini \
  deploy.yaml
```

* Need to use s3_aws_academy PEM key with 400 file permissions

### Local test of TPC-H queries with data from S3 and events from S3

* Deploy the infrastructure to the AWS Academy account (S3 and any EC2 instance with the Academy SSH key).
* Build the project using `mvn clean package`.
* Update `workloads/s3-default.yaml` and `workloads/tpc-h-default.yaml` based on your preferences.
* Run the following command to start the TPC-H benchmark.
```
  sudo bin/benchmark \
  --drivers workloads/s3-default.yaml \
  --tpc-h-file workloads/tpc-h-default.yaml \
  workloads/simple-workload.yaml
```