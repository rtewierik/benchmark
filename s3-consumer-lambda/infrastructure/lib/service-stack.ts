import { App, Duration, Stack } from 'aws-cdk-lib'
import {
  Tracing,
  Runtime,
  Code,
  Function as LambdaFunction,
} from 'aws-cdk-lib/aws-lambda'
import {
  IQueue,
  Queue,
  QueueEncryption,
} from 'aws-cdk-lib/aws-sqs'
import { S3EventSourceV2 } from 'aws-cdk-lib/aws-lambda-event-sources'
import {
  ManagedPolicy,
  PolicyStatement,
  Role,
  ServicePrincipal,
} from 'aws-cdk-lib/aws-iam'
import { S3ConsumerLambdaStackProps } from './stack-configuration'

import { addMonitoring } from '../modules/monitoring'
import { addAlerting } from '../modules/alerting'
import path = require('path')
import { Bucket, EventType, IBucket } from 'aws-cdk-lib/aws-s3'

interface LambdaConfiguration {
  s3Prefixes: string[]
  numberOfConsumers?: number
  functionTimeoutSeconds: number
}

const MAP_ID = 'Map'
const REDUCE_ID = 'Reduce'
const RESULT_ID = 'Result'
const DEFAULT_ID = 'Default'

const AGGREGATE_CONFIG = {
  s3Prefixes: [],
  functionTimeoutSeconds: 15
}

export class ServiceStack extends Stack {
  constructor(scope: App, id: string, props: S3ConsumerLambdaStackProps) {
    super(scope, id, props)
    const bucket = Bucket.fromBucketName(this, 'S3ConsumerLambdaSourceBucket', props.bucketName)
    if (props.isTpcH) {
      const mapPrefix = this.getS3Prefix(props, MAP_ID)
      const resultPrefix = this.getS3Prefix(props, RESULT_ID)
      const s3Prefixes = [mapPrefix, resultPrefix]
      for (var i = 0; i < props.numberOfConsumers; i++) {
        const reduceTopicId = `${REDUCE_ID}${i}`
        s3Prefixes.push(this.getS3Prefix(props, reduceTopicId))
      }
      const aggregateConfig = { ...AGGREGATE_CONFIG, s3Prefixes }
      const mapConfiguration = { s3Prefixes, ...props }
      this.createDataIngestionLayer(props, MAP_ID, bucket, mapConfiguration, mapPrefix)
      for (var i = 0; i < props.numberOfConsumers; i++) {
        const prefix = s3Prefixes[2 + i]
        const reduceTopicId = `${REDUCE_ID}${i}`
        this.createDataIngestionLayer(props, reduceTopicId, bucket, aggregateConfig, prefix)
      }
      this.createDataIngestionLayer(props, RESULT_ID, bucket, aggregateConfig, resultPrefix)
    } else {
      // TO DO: Consider using `props.numberOfConsumers` here to create more than one SNS/SQS pair. Might not be necessary since infrastructure should be isolated.
      const prefix = this.getS3Prefix(props, DEFAULT_ID)
      this.createDataIngestionLayer(props, DEFAULT_ID, bucket, AGGREGATE_CONFIG, prefix)
    }
  }

  private getS3Prefix(props: S3ConsumerLambdaStackProps, id: string) {
    return `${props.appName}-s3-${id.toLowerCase()}-${this.getRandomString()}`
  }
  
  private getRandomString() {
    return Math.random().toString(36).substr(2, 5);
  }

  private createDataIngestionLayer(props: S3ConsumerLambdaStackProps, id: string, bucket: IBucket, lambdaConfiguration: LambdaConfiguration, s3Prefix: string) {
    const lambdaDeadLetterQueue = this.createS3ConsumerLambdaDeadLetterQueue(props, id)
    const lambda = this.createS3ConsumerLambda(bucket, lambdaDeadLetterQueue, props, id, lambdaConfiguration, s3Prefix)
    addMonitoring(this, lambda, lambdaDeadLetterQueue, props, id)
    addAlerting(this, lambda, lambdaDeadLetterQueue, props, id)
  }

  private createS3ConsumerLambdaDeadLetterQueue(props: S3ConsumerLambdaStackProps, id: string): IQueue {
    const lowerCaseId = id.toLowerCase()
    return new Queue(
      this,
      `S3ConsumerLambdaDeadLetterQueue${id}`,
      {
        queueName: `${props.appName}-dlq-${lowerCaseId}`,
        encryption: QueueEncryption.UNENCRYPTED,
        retentionPeriod: Duration.days(7)
      })
  }

  private createS3ConsumerLambda(bucket: IBucket, deadLetterQueue: IQueue, props: S3ConsumerLambdaStackProps, id: string, lambdaConfiguration: LambdaConfiguration, s3Prefix: string): LambdaFunction {
    const { s3Prefixes, numberOfConsumers, functionTimeoutSeconds } = lambdaConfiguration
    const lowerCaseId = id.toLowerCase()
    const iamRole = new Role(
      this,
      `S3ConsumerLambdaIamRole${id}`,
      {
        assumedBy: new ServicePrincipal('lambda.amazonaws.com'),
        roleName: `${props.appName}-lambda-role-${lowerCaseId}`,
        description:
          'IAM Role for granting Lambda receive message to the Benchmark Monitoring queue and send message to the DLQ',
      }
    )

    bucket.grantReadWrite(iamRole)
    iamRole.addToPolicy(
      new PolicyStatement({
        actions: ['SQS:SendMessage'],
        resources: [deadLetterQueue.queueArn],
      })
    )

    const lambda = new LambdaFunction(this, `S3ConsumerLambdaFunction${id}`, {
      description: 'This Lambda function processes messages from S3 in the context of throughput- and TPC-H benchmarks',
      runtime: Runtime.JAVA_8_CORRETTO,
      code: Code.fromAsset(path.join(__dirname, '../../../driver-s3-package/target/driver-s3-package-0.0.1-SNAPSHOT.jar')),
      functionName: `${props.appName}-${lowerCaseId}`,
      handler: 'io.openmessaging.benchmark.driver.s3.S3BenchmarkConsumer::handleRequest',
      reservedConcurrentExecutions: numberOfConsumers ?? 1,
      timeout: Duration.seconds(functionTimeoutSeconds),
      memorySize: 1024,
      tracing: Tracing.ACTIVE,
      role: iamRole,
      environment: {
        REGION: this.region,
        S3_PREFIXES: s3Prefixes.map(prefix => `s3://:${props.bucketName}/${prefix}`).join(','),
        IS_TPC_H: `${props.isTpcH}`,
        DEBUG: props.debug ? 'TRUE' : 'FALSE',
      },
      retryAttempts: 2
    });

    iamRole.addManagedPolicy(
      ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')
    )

    lambda.addEventSource(
      new S3EventSourceV2(bucket, {
        events: [EventType.OBJECT_CREATED],
        filters: [{ prefix: s3Prefix }]
      })
    )

    return lambda
  }
}

export default {}
