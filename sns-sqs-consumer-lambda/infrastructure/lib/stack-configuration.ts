import { Duration, StackProps } from 'aws-cdk-lib'

export interface SnsSqsConsumerLambdaStackProps extends StackProps {
  appName: string
  maxBatchingWindow: Duration | undefined
  batchSize: number
  debug: boolean
  functionTimeoutSeconds: number
  eventsVisibilityTimeoutSeconds: number
  alertingEnabled: boolean
  numberOfConsumers: number
  isTpcH: boolean
}
