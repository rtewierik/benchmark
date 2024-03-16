import { Duration, StackProps } from 'aws-cdk-lib'

export interface SnsSqsDriverStackProps extends StackProps {
  appName: string
  maxBatchingWindow: Duration
  batchSize: number
  debug: boolean
  functionTimeoutSeconds: number
  eventsVisibilityTimeoutSeconds: number
  readCapacity: number
  writeCapacity: number
  alertingEnabled: boolean
}
