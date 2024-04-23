import { Duration, StackProps } from 'aws-cdk-lib'

export interface BenchmarkMonitoringStackProps extends StackProps {
  appName: string
  reservedConcurrentExecutions: number
  maxBatchingWindow: Duration
  batchSize: number
  debug: boolean
  functionTimeoutSeconds: number
  eventsVisibilityTimeoutSeconds: number
  readCapacity: number
  writeCapacity: number
  alertingEnabled: boolean
}
