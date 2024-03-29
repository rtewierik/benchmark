import { App } from 'aws-cdk-lib'
import { S3ConsumerLambdaStackProps } from '../lib/stack-configuration'
import { ServiceStack } from '../lib/service-stack'

const app = new App()

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
  isTpcH: true
}

new ServiceStack(app, 's3-consumer-lambda', stackProps)