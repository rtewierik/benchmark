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

## Running the Ansible playbook
With the appropriate infrastructure in place, you can install and start the Kafka cluster using Ansible with just one command:

```
$ ansible-playbook \
  --user ec2-user \
  --inventory `which terraform-inventory` \
  deploy.yaml
```

If you’re using an SSH private key path different from `~/.ssh/sns_sqs_aws`, you can specify that path using the `--private-key` flag, for example `--private-key=~/.ssh/my_key`.