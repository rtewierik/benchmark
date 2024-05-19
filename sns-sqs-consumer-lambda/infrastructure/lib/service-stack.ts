import { App, Duration, Stack } from 'aws-cdk-lib'
import {
  Tracing,
  Runtime,
  Code,
  Function as LambdaFunction,
  LayerVersion,
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
import { SnsSqsConsumerLambdaStackProps } from './stack-configuration'

import { addMonitoring } from '../modules/monitoring'
import { addAlerting } from '../modules/alerting'
import path = require('path')
import { ITopic, Topic } from 'aws-cdk-lib/aws-sns'
import { SqsSubscription } from 'aws-cdk-lib/aws-sns-subscriptions'
import { Bucket, IBucket } from 'aws-cdk-lib/aws-s3'

interface SnsTopic {
  topic: ITopic
  deadLetterQueue: IQueue
}

interface DataIngestionLayer {
  topic: ITopic
  queue: IQueue
  snsDeadLetterQueue: IQueue
  lambdaDeadLetterQueue: IQueue
}

interface LambdaConfiguration {
  snsTopicNames: string[]
  numberOfConsumers?: number
  functionTimeoutSeconds: number
}

const MAP_ID = 'Map'
const REDUCE_ID = 'Reduce'
const RESULT_ID = 'Result'
const DEFAULT_ID = 'Default'

const AGGREGATE_CONFIG = {
  snsTopicNames: [],
  functionTimeoutSeconds: 15
}

export class ServiceStack extends Stack {
  constructor(scope: App, id: string, props: SnsSqsConsumerLambdaStackProps) {
    super(scope, id, props)
    const chunksBucket = Bucket.fromBucketName(this, 'S3ConsumerChunksBucket', 'tpc-h-chunks')
    const monitoringSqsQueue = Queue.fromQueueArn(this, 'S3ConsumerMonitoringSqsQueue', props.monitoringSqsArn)
    if (props.isTpcH) {
      const snsTopicNames = [
        this.getSnsTopicName(props, MAP_ID),
        this.getSnsTopicName(props, RESULT_ID)
      ]
      for (var i = 0; i < props.numberOfConsumers; i++) {
        const reduceTopicId = `${REDUCE_ID}${i}`
        snsTopicNames.push(this.getSnsTopicName(props, reduceTopicId))
      }
      const aggregateConfig = { ...AGGREGATE_CONFIG, snsTopicNames }
      const mapTopic = this.createSnsSqsConsumerLambdaSnsTopic(props, MAP_ID)
      const { topic, deadLetterQueue } = mapTopic
      const mapConfiguration = { snsTopicNames, ...props }
      const mapQueue = this.createSnsSqsConsumerLambdaQueue(props, MAP_ID, mapConfiguration)
      topic.addSubscription(new SqsSubscription(mapQueue, { deadLetterQueue, rawMessageDelivery: true }))
      this.createDataIngestionLayer(props, MAP_ID, chunksBucket, monitoringSqsQueue, mapConfiguration, mapTopic, mapQueue)
      for (var i = 0; i < props.numberOfConsumers; i++) {
        const reduceTopicId = `${REDUCE_ID}${i}`
        this.createDataIngestionLayer(props, reduceTopicId, chunksBucket, monitoringSqsQueue, aggregateConfig)
      }
      this.createDataIngestionLayer(props, RESULT_ID, chunksBucket, monitoringSqsQueue, aggregateConfig)
    } else {
      for (var i = 0; i < props.numberOfConsumers; i++) {
        const consumerTopicId = `${DEFAULT_ID}${i}`
        this.createDataIngestionLayer(props, consumerTopicId, chunksBucket, monitoringSqsQueue, AGGREGATE_CONFIG)
      }
    }
  }

  private getSnsTopicName(props: SnsSqsConsumerLambdaStackProps, id: string) {
    return `${props.appName}-${id.toLowerCase()}`
  }

  private createDataIngestionLayer(props: SnsSqsConsumerLambdaStackProps, id: string, chunksBucket: IBucket, monitoringSqsQueue: IQueue, lambdaConfiguration: LambdaConfiguration, existingTopic?: SnsTopic, existingQueue?: IQueue) {
    const { queue, lambdaDeadLetterQueue } = this.createSnsSqsConsumerLambdaDataIngestionLayer(props, id, lambdaConfiguration, existingTopic, existingQueue)
    this.createSnsSqsConsumerLambda(chunksBucket, monitoringSqsQueue, queue, lambdaDeadLetterQueue, props, id, lambdaConfiguration)
    // addMonitoring(this, queue, lambda, lambdaDeadLetterQueue, snsDeadLetterQueue, props, id)
    // addAlerting(this, lambda, lambdaDeadLetterQueue, snsDeadLetterQueue, props, id)
  }

  private createSnsSqsConsumerLambdaDataIngestionLayer(props: SnsSqsConsumerLambdaStackProps, id: string, lambdaConfiguration: LambdaConfiguration, existingTopic: SnsTopic | undefined, existingQueue: IQueue | undefined): DataIngestionLayer {
    const { topic, deadLetterQueue } = existingTopic ?? this.createSnsSqsConsumerLambdaSnsTopic(props, id)
    const queue = existingQueue ?? this.createSnsSqsConsumerLambdaQueue(props, id, lambdaConfiguration)
    const lambdaDeadLetterQueue = this.createSnsSqsConsumerLambdaDeadLetterQueue(props, id)
    !existingQueue && topic.addSubscription(new SqsSubscription(queue, { deadLetterQueue, rawMessageDelivery: true }))
    return { topic, queue, snsDeadLetterQueue: deadLetterQueue, lambdaDeadLetterQueue }
  }

  private createSnsSqsConsumerLambdaSnsTopic(props: SnsSqsConsumerLambdaStackProps, id: string): SnsTopic {
    const lowerCaseId = id.toLowerCase()
    const deadLetterQueue = new Queue(
      this,
      `SnsSqsConsumerLambdaSnsDeadLetterQueue${id}`,
      {
        queueName: `${props.appName}-sns-dlq-${lowerCaseId}`,
        encryption: QueueEncryption.UNENCRYPTED,
        retentionPeriod: Duration.days(7)
      })
    const topic = new Topic(this, `SnsSqsConsumerLambdaSnsTopic${id}`, {
      topicName: this.getSnsTopicName(props, id),
      enforceSSL: false
    })
    return { topic, deadLetterQueue }
  }

  private createSnsSqsConsumerLambdaQueue(props: SnsSqsConsumerLambdaStackProps, id: string, lambdaConfiguration: LambdaConfiguration): IQueue {
    const lowerCaseId = id.toLowerCase()
    return new Queue(
      this,
      `SnsSqsConsumerLambdaQueue${id}`,
      {
        queueName: `${props.appName}-${lowerCaseId}`,
        encryption: QueueEncryption.UNENCRYPTED,
        retentionPeriod: Duration.days(7),
        visibilityTimeout: Duration.seconds(lambdaConfiguration.functionTimeoutSeconds)
      })
  }

  private createSnsSqsConsumerLambdaDeadLetterQueue(props: SnsSqsConsumerLambdaStackProps, id: string): IQueue {
    const lowerCaseId = id.toLowerCase()
    return new Queue(
      this,
      `SnsSqsConsumerLambdaDeadLetterQueue${id}`,
      {
        queueName: `${props.appName}-dlq-${lowerCaseId}`,
        encryption: QueueEncryption.UNENCRYPTED,
        retentionPeriod: Duration.days(7)
      })
  }

  private createSnsSqsConsumerLambda(chunksBucket: IBucket, monitoringSqsQueue: IQueue, snsSqsConsumerLambdaQueue: IQueue, deadLetterQueue: IQueue, props: SnsSqsConsumerLambdaStackProps, id: string, lambdaConfiguration: LambdaConfiguration): LambdaFunction {
    const { snsTopicNames, numberOfConsumers, functionTimeoutSeconds } = lambdaConfiguration
    const lowerCaseId = id.toLowerCase()
    const iamRole = new Role(
      this,
      `SnsSqsConsumerLambdaIamRole${id}`,
      {
        assumedBy: new ServicePrincipal('lambda.amazonaws.com'),
        roleName: `${props.appName}-lambda-role-${lowerCaseId}`,
        description:
          'IAM Role for granting Lambda receive message to the Benchmark Monitoring queue and send message to the DLQ',
      }
    )
    
    chunksBucket.grantRead(iamRole)
    monitoringSqsQueue.grantSendMessages(iamRole)
    iamRole.addToPolicy(
      new PolicyStatement({
        actions: ['SQS:ReceiveMessage', 'SQS:SendMessage', 'SQS:DeleteMessage'],
        resources: [snsSqsConsumerLambdaQueue.queueArn],
      })
    )
    iamRole.addToPolicy(
      new PolicyStatement({
        actions: ['SQS:SendMessage'],
        resources: [deadLetterQueue.queueArn],
      })
    )
    iamRole.addToPolicy(
      new PolicyStatement({
        actions: ['SNS:Publish'],
        resources: [`arn:aws:sns:${this.region}:${this.account}:sns-sqs-consumer-lambda-sns-topic-*`]
      })
    )

    const lambda = new LambdaFunction(this, `SnsSqsConsumerLambdaFunction${id}`, {
      description: 'This Lambda function processes messages from SNS/SQS in the context of throughput- and TPC-H benchmarks',
      runtime: Runtime.JAVA_8_CORRETTO,
      code: Code.fromAsset(path.join(__dirname, '../../../driver-sns-sqs-package/target/driver-sns-sqs-package-0.0.1-SNAPSHOT.jar')),
      functionName: `${props.appName}-${lowerCaseId}`,
      handler: 'io.openmessaging.benchmark.driver.sns.sqs.SnsSqsBenchmarkConsumer::handleRequest',
      reservedConcurrentExecutions: numberOfConsumers ?? 1,
      timeout: Duration.seconds(functionTimeoutSeconds),
      memorySize: 1024,
      tracing: Tracing.ACTIVE,
      role: iamRole,
      environment: {
        REGION: this.region,
        SNS_URIS: snsTopicNames.map(name => `arn:aws:sns:${this.region}:${this.account}:${name}`).join(','),
        SQS_URI: snsSqsConsumerLambdaQueue.queueUrl,
        IS_TPC_H: `${props.isTpcH}`,
        DEBUG: props.debug ? 'TRUE' : 'FALSE',
        IS_CLOUD_MONITORING_ENABLED: props.isCloudMonitoringEnabled ? 'TRUE' : 'FALSE',
        MONITORING_SQS_URI: props.monitoringSqsUri,
        NUMBER_OF_CONSUMERS: `${props.numberOfConsumers}`,
        ACCOUNT_ID: props.env?.account!
      },
      retryAttempts: 2
    });

    iamRole.addManagedPolicy(
      ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')
    )
    iamRole.addManagedPolicy(
      ManagedPolicy.fromAwsManagedPolicyName('CloudWatchLambdaInsightsExecutionRolePolicy')
     );

    const layerArn = 'arn:aws:lambda:eu-west-1:580247275435:layer:LambdaInsightsExtension:52';
    const layer = LayerVersion.fromLayerVersionArn(this, `SnsSqsConsumerLambdaFunctionLambdaInsightsLayerFromArn${id}`, layerArn);

    lambda.addLayers(layer);

    lambda.addEventSource(
      new SqsEventSource(snsSqsConsumerLambdaQueue,
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
