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
import { SqsEventSource } from 'aws-cdk-lib/aws-lambda-event-sources'
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
    if (props.isTpcH) {
      const s3Prefixes = [
        this.getS3Prefix(props, MAP_ID),
        this.getS3Prefix(props, RESULT_ID)
      ]
      for (var i = 0; i < props.numberOfConsumers; i++) {
        const reduceTopicId = `${REDUCE_ID}${i}`
        s3Prefixes.push(this.getS3Prefix(props, reduceTopicId))
      }
      const aggregateConfig = { ...AGGREGATE_CONFIG, s3Prefixes }
      const mapTopic = this.createS3ConsumerLambdaSnsTopic(props, MAP_ID)
      const { topic, deadLetterQueue } = mapTopic
      const mapConfiguration = { s3Prefixes, ...props }
      const mapQueue = this.createS3ConsumerLambdaQueue(props, MAP_ID, mapConfiguration)
      topic.addSubscription(new SqsSubscription(mapQueue, { deadLetterQueue, rawMessageDelivery: true }))
      this.createDataIngestionLayer(props, MAP_ID, mapConfiguration, mapTopic, mapQueue)
      for (var i = 0; i < props.numberOfConsumers; i++) {
        const reduceTopicId = `${REDUCE_ID}${i}`
        this.createDataIngestionLayer(props, reduceTopicId, aggregateConfig)
      }
      this.createDataIngestionLayer(props, RESULT_ID, aggregateConfig)
    } else {
      // TO DO: Consider using `props.numberOfConsumers` here to create more than one SNS/SQS pair. Might not be necessary since infrastructure should be isolated.
      this.createDataIngestionLayer(props, DEFAULT_ID, AGGREGATE_CONFIG)
    }
  }

  private getS3Prefix(props: S3ConsumerLambdaStackProps, id: string) {
    // TODO: Use props.bucketName instead.
    return `${props.appName}-s3-${id.toLowerCase()}-${this.getRandomString()}`
  }
  
  private getRandomString() {
    return Math.random().toString(36).substr(2, 5);
  }

  private createDataIngestionLayer(props: S3ConsumerLambdaStackProps, id: string, lambdaConfiguration: LambdaConfiguration) {
    const lambdaDeadLetterQueue = this.createS3ConsumerLambdaDeadLetterQueue(props, id)
    const lambda = this.createS3ConsumerLambda(queue, lambdaDeadLetterQueue, props, id, lambdaConfiguration)
    addMonitoring(this, queue, lambda, lambdaDeadLetterQueue, props, id)
    addAlerting(this, lambda, lambdaDeadLetterQueue, props, id)
  }

  private createS3ConsumerLambdaQueue(props: S3ConsumerLambdaStackProps, id: string, lambdaConfiguration: LambdaConfiguration): IQueue {
    const lowerCaseId = id.toLowerCase()
    return new Queue(
      this,
      `S3ConsumerLambdaQueue${id}`,
      {
        queueName: `${props.appName}-${lowerCaseId}`,
        encryption: QueueEncryption.UNENCRYPTED,
        retentionPeriod: Duration.days(7),
        visibilityTimeout: Duration.seconds(lambdaConfiguration.functionTimeoutSeconds)
      })
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

  private createS3ConsumerLambda(S3ConsumerLambdaQueue: IQueue, deadLetterQueue: IQueue, props: S3ConsumerLambdaStackProps, id: string, lambdaConfiguration: LambdaConfiguration): LambdaFunction {
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
    
    // TO DO: Add permissions to read and write to S3 bucket whose name is provided in configuration.
    iamRole.addToPolicy(
      new PolicyStatement({
        actions: [],
        resources: []
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
        S3_PREFIXES: s3Prefixes.map(name => `arn:aws:sns:${this.region}:${this.account}:${name}`).join(','),
        IS_TPC_H: `${props.isTpcH}`,
        DEBUG: props.debug ? 'TRUE' : 'FALSE',
      },
      retryAttempts: 2
    });

    iamRole.addManagedPolicy(
      ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')
    )

    // TO DO: Create S3 event source for each S3 prefix.
    lambda.addEventSource(
      new SqsEventSource(S3ConsumerLambdaQueue,
        {
          batchSize: props.batchSize,
          maxBatchingWindow: props.maxBatchingWindow,
          reportBatchItemFailures: props.reportBatchItemFailures,
          maxConcurrency: numberOfConsumers
        })
    )

    return lambda
  }
}

export default {}
