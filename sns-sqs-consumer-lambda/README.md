# SNS/SQS driver

The mission of this service is to deploy required infrastructure for push-based SNS/SQS benchmarks to AWS.

## Getting Started

The project can be set up for local development as follows.

### Requirements

* Node 18.x

### Install

Use Git to clone this repository to your computer.

```
git clone https://github.com/rtewierik/benchmark.git
```

Navigate to the root directory of the Lambda project.

``
cd benchmark/sns-sqs-consumer-lambda/infrastructure
``

Install the project dependencies using NPM.

```
npm install
```

### Deploy

Ensure the environment variable `AWS_DEFAULT_PROFILE` is set if you want to use a specific profile (ex: `personal_prod`) over the default profile when deploying the CDK project to AWS.

Run the following commands locally to deploy the AWS CDK project to AWS.

* **Navigate to the `infrastructure` project, verify AWS environment and CDK version and build the CDK project:** `cd infrastructure && cdk doctor && npm install && npm run build`
* **Verify staged changes:** `npx aws-cdk diff sns-sqs-consumer-lambda`
* **Deploy staged changes:** `npx aws-cdk deploy sns-sqs-consumer-lambda --require-approval never`

## Extracting metrics from SNS topics and SQS queues after running the benchmarks

Run the command `sh ../../extract_sns_sqs_metrics.sh`.
