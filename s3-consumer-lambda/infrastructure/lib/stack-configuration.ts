import { Duration, StackProps } from 'aws-cdk-lib'

export interface S3ConsumerLambdaStackProps extends StackProps {
  appName: string
  maxBatchingWindow: Duration | undefined
  batchSize: number
  reportBatchItemFailures: boolean
  debug: boolean
  functionTimeoutSeconds: number
  alertingEnabled: boolean
  numberOfConsumers: number
  bucketName: string
  isTpcH: boolean
  isCloudMonitoringEnabled: boolean
  monitoringSqsUri: string
}
