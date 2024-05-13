import { App, Tags } from 'aws-cdk-lib'
import { S3ConsumerLambdaStackProps } from '../lib/stack-configuration'
import { ServiceStack } from '../lib/service-stack'

const app = new App()

Tags.of(app).add('Application', 's3-benchmark-ruben-te-wierik')
Tags.of(app).add('Owner', 'Ruben_te_Wierik')
Tags.of(app).add('Contact', 'rtewierik@uoc.edu')

const stackProps: S3ConsumerLambdaStackProps = {
  description: 'S3 driver',
  env: {
    account: '138945776678',
    region: 'eu-west-1'
  },
  appName: 's3-consumer-lambda',
  maxBatchingWindow: undefined,
  batchSize: 1,
  reportBatchItemFailures: false,
  debug: true,
  functionTimeoutSeconds: 300,
  numberOfConsumers: 3,
  alertingEnabled: true,
  bucketName: 'benchmarking-events',
  isTpcH: true,
  isCloudMonitoringEnabled: true,
  monitoringSqsUri: 'https://sqs.eu-west-1.amazonaws.com/138945776678/benchmark-monitoring',
  monitoringSqsArn: 'arn:aws:sqs:eu-west-1:138945776678:benchmark-monitoring'
}

new ServiceStack(app, 's3-consumer-lambda', stackProps)