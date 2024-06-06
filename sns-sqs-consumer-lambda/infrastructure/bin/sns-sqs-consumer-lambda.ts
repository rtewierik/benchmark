import { App, Tags } from 'aws-cdk-lib'
import { SnsSqsConsumerLambdaStackProps } from '../lib/stack-configuration'
import { ServiceStack } from '../lib/service-stack'

const app = new App()

Tags.of(app).add('Application', 'sns-sqs-benchmark-ruben-te-wierik')
Tags.of(app).add('Owner', 'Ruben_te_Wierik')
Tags.of(app).add('Contact', 'rtewierik@uoc.edu')

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
  debug: false,
  functionTimeoutSeconds: 60,
  numberOfConsumers: 200,
  alertingEnabled: true,
  isTpcH: true,
  isCloudMonitoringEnabled: true,
  monitoringSqsUri: 'https://sqs.eu-west-1.amazonaws.com/138945776678/benchmark-monitoring',
  monitoringSqsArn: 'arn:aws:sqs:eu-west-1:138945776678:benchmark-monitoring'
}

const batchSize = 20
const numStacks = stackProps.numberOfConsumers / batchSize

new ServiceStack(app, `sns-sqs-consumer-lambda`, stackProps, true, 0, 0)
for (let i = 0; i < numStacks; i++) {
  const start = i * batchSize
  const end = i * batchSize + batchSize
  new ServiceStack(app, `sns-sqs-consumer-lambda-${i}`, stackProps, false, start, end)
}
