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

```
sudo bin/benchmark \
  --drivers driver-sns-sqs/sns-sqs-default.yaml \
  --tpc-h-file workloads/tpc-h-default.yaml \
  workloads/simple-workload.yaml
```

### Locations to configure memory in case of changing instance size

* `deploy.yaml`requires modification of the `Configure memory` task

## Running the Ansible playbook

With the appropriate infrastructure in place, you can install and start the Kafka cluster using Ansible with just one
command:

```
$ ansible-playbook \
  --user ec2-user \
  --inventory `which terraform-inventory` \
  deploy.yaml
```

If you’re using an SSH private key path different from `~/.ssh/sns_sqs_aws`, you can specify that path using
the `--private-key` flag, for example `--private-key=~/.ssh/my_key`.

## Downloading your benchmarking results

The OpenMessaging benchmarking suite stores results in JSON files in the `/opt/benchmark` folder on the client host from
which the benchmarks are run. You can download those results files onto your local machine using scp. You can download
all generated JSON results files using this command:

```
$ scp -i ~/.ssh/sns_sqs_aws ec2-user@$(terraform output client_ssh_host):/opt/benchmark/*.json .
```