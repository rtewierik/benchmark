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
  debug: false,
  functionTimeoutSeconds: 300,
  numberOfConsumers: 400,
  alertingEnabled: true,
  bucketName: 'benchmarking-events',
  isTpcH: true,
  isCloudMonitoringEnabled: true,
  monitoringSqsUri: 'https://sqs.eu-west-1.amazonaws.com/138945776678/benchmark-monitoring',
  monitoringSqsArn: 'arn:aws:sqs:eu-west-1:138945776678:benchmark-monitoring'
}

const batchSize = 20
const numStacks = stackProps.numberOfConsumers / batchSize

new ServiceStack(app, `s3-consumer-lambda`, stackProps, true, 0, 0)
new ServiceStack(app, `s3-consumer-lambda-event-sources-0`, stackProps, false, 0, 50, true)
new ServiceStack(app, `s3-consumer-lambda-event-sources-1`, stackProps, false, 50, 100, true)
new ServiceStack(app, `s3-consumer-lambda-event-sources-2`, stackProps, false, 100, 150, true)
new ServiceStack(app, `s3-consumer-lambda-event-sources-4`, stackProps, false, 150, 200, true)
new ServiceStack(app, `s3-consumer-lambda-event-sources-5`, stackProps, false, 200, 250, true)
new ServiceStack(app, `s3-consumer-lambda-event-sources-6`, stackProps, false, 250, 300, true)
new ServiceStack(app, `s3-consumer-lambda-event-sources-7`, stackProps, false, 300, 350, true)
new ServiceStack(app, `s3-consumer-lambda-event-sources-8`, stackProps, false, 350, 400, true)
for (let i = 0; i < numStacks; i++) {
  const start = i * batchSize
  const end = i * batchSize + batchSize
  new ServiceStack(app, `s3-consumer-lambda-${i}`, stackProps, false, start, end)
}
