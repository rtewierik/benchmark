import { Duration, StackProps } from 'aws-cdk-lib'

export interface SnsSqsConsumerLambdaStackProps extends StackProps {
  appName: string
  maxBatchingWindow: Duration | undefined
  batchSize: number
  reportBatchItemFailures: boolean
  debug: boolean
  functionTimeoutSeconds: number
  alertingEnabled: boolean
  numberOfConsumers: number
  isTpcH: boolean
}
