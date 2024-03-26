import { App, Duration } from 'aws-cdk-lib'
import { SnsSqsConsumerLambdaStackProps } from '../lib/stack-configuration'
import { ServiceStack } from '../lib/service-stack'

const app = new App()

const stackProps: SnsSqsConsumerLambdaStackProps = {
  description: 'SNS/SQS driver',
  env: {
    account: '138945776678',
    region: 'us-west-2'
  },
  appName: 'sns-sqs-consumer-lambda',
  maxBatchingWindow: undefined,
  batchSize: 10,
  debug: true,
  functionTimeoutSeconds: 30,
  eventsVisibilityTimeoutSeconds: 30,
  numberOfConsumers: 3,
  alertingEnabled: true,
  isTpcH: false
}

new ServiceStack(app, 'sns-sqs-consumer-lambda', stackProps)