import { App } from 'aws-cdk-lib'
import { SnsSqsConsumerLambdaStackProps } from '../lib/stack-configuration'
import { ServiceStack } from '../lib/service-stack'

const app = new App()

const stackProps: SnsSqsConsumerLambdaStackProps = {
  description: 'SNS-SQS driver',
  env: {
    account: '138945776678',
    region: 'eu-west-1'
  },
  appName: 'sns-sqs-consumer-lambda',
  maxBatchingWindow: undefined,
  batchSize: 1,
  reportBatchItemFailures: false,
  debug: true,
  functionTimeoutSeconds: 300,
  numberOfConsumers: 3,
  alertingEnabled: true,
  isTpcH: true,
  isCloudMonitoringEnabled: true,
  monitoringSqsUri: 'https://sqs.eu-west-1.amazonaws.com/138945776678/benchmark-monitoring',
  monitoringSqsArn: 'arn:aws:sqs:eu-west-1:138945776678:benchmark-monitoring'
}

new ServiceStack(app, 'sns-sqs-consumer-lambda', stackProps)