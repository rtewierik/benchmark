import { App, Duration } from 'aws-cdk-lib'
import { SnsSqsConsumerLambdaStackProps } from '../lib/stack-configuration'
import { ServiceStack } from '../lib/service-stack'

const app = new App()

const stackProps: SnsSqsConsumerLambdaStackProps = {
  description: 'SNS/SQS driver',
  env: {
    account: '138945776678',
    region: 'eu-west-1'
  },
  appName: 'sns-sqs-consumer-lambda',
  maxBatchingWindow: Duration.minutes(1),
  batchSize: 100,
  debug: true,
  functionTimeoutSeconds: 30,
  eventsVisibilityTimeoutSeconds: 30,
  readCapacity: 1,
  writeCapacity: 1,
  alertingEnabled: true
}

new ServiceStack(app, 'sns-sqs-consumer-lambda', stackProps)