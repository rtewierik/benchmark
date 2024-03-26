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
import { SnsSqsConsumerLambdaStackProps } from './stack-configuration'

import { addMonitoring } from '../modules/monitoring'
import { addAlerting } from '../modules/alerting'
import path = require('path')
import { ITopic, Topic } from 'aws-cdk-lib/aws-sns'
import { SqsSubscription } from 'aws-cdk-lib/aws-sns-subscriptions'

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

const MAP_ID = 'Map'
const REDUCE_ID = 'Reduce'
const RESULT_ID = 'Result'
const DEFAULT_ID = 'Default'

export class ServiceStack extends Stack {
  constructor(scope: App, id: string, props: SnsSqsConsumerLambdaStackProps) {
    super(scope, id, props)
    if (props.isTpcH) {
      const mapTopic = this.createSnsSqsConsumerLambdaSnsTopic(props, MAP_ID)
      for (var i = 0; i < props.numberOfConsumers; i++) {
        const reduceTopicId = `${REDUCE_ID}${i}`
        this.createDataIngestionLayer(props, `${MAP_ID}${i}`, this.getSnsTopicName(props, reduceTopicId), mapTopic)
        this.createDataIngestionLayer(props, reduceTopicId, this.getSnsTopicName(props, RESULT_ID))
      }
      this.createDataIngestionLayer(props, RESULT_ID)
    } else {
      // TO DO: Consider using `props.numberOfConsumers` here to create more than one SNS/SQS pair. Might not be necessary since infrastructure should be isolated.
      this.createDataIngestionLayer(props, DEFAULT_ID)
    }
  }

  private getSnsTopicName(props: SnsSqsConsumerLambdaStackProps, id: string) {
    return `${props.appName}-sns-topic-${id.toLowerCase()}`
  }

  private createDataIngestionLayer(props: SnsSqsConsumerLambdaStackProps, id: string, targetTopic?: string, existingTopic?: SnsTopic) {
    const { queue, snsDeadLetterQueue, lambdaDeadLetterQueue } = this.createSnsSqsConsumerLambdaDataIngestionLayer(props, id, existingTopic)
    const lambda = this.createSnsSqsConsumerLambda(queue, lambdaDeadLetterQueue, props, id, targetTopic)
    addMonitoring(this, queue, lambda, lambdaDeadLetterQueue, snsDeadLetterQueue, props, id)
    addAlerting(this, lambda, lambdaDeadLetterQueue, snsDeadLetterQueue, props, id)
  }

  private createSnsSqsConsumerLambdaDataIngestionLayer(props: SnsSqsConsumerLambdaStackProps, id: string, existingTopic: SnsTopic | undefined): DataIngestionLayer {
    const { topic, deadLetterQueue } = existingTopic ?? this.createSnsSqsConsumerLambdaSnsTopic(props, id)
    const queue = this.createSnsSqsConsumerLambdaQueue(props, id)
    const lambdaDeadLetterQueue = this.createSnsSqsConsumerLambdaDeadLetterQueue(props, id)
    topic.addSubscription(new SqsSubscription(queue, { deadLetterQueue }))
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

  private createSnsSqsConsumerLambdaQueue(props: SnsSqsConsumerLambdaStackProps, id: string): IQueue {
    const lowerCaseId = id.toLowerCase()
    return new Queue(
      this,
      `SnsSqsConsumerLambdaQueue${id}`,
      {
        queueName: `${props.appName}-${lowerCaseId}`,
        encryption: QueueEncryption.UNENCRYPTED,
        retentionPeriod: Duration.days(7)
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

  private createSnsSqsConsumerLambda(snsSqsConsumerLambdaQueue: IQueue, deadLetterQueue: IQueue, props: SnsSqsConsumerLambdaStackProps, id: string, targetTopic: string | undefined): LambdaFunction {
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
    
    iamRole.addToPolicy(
      new PolicyStatement({
        actions: ['SQS:ReceiveMessage', 'SQS:SendMessage'],
        resources: [snsSqsConsumerLambdaQueue.queueArn],
      })
    )
    iamRole.addToPolicy(
      new PolicyStatement({
        actions: ['SQS:SendMessage'],
        resources: [deadLetterQueue.queueArn],
      })
    )

    const lambda = new LambdaFunction(this, `SnsSqsConsumerLambdaFunction${id}`, {
      description: 'This Lambda function ingests experimental results from infrastructure participating in experiments and stores collected data in a DynamoDB table',
      runtime: Runtime.JAVA_8_CORRETTO,
      code: Code.fromAsset(path.join(__dirname, '../../../driver-sns-sqs-package/target/openmessaging-benchmark-driver-sns-sqs-0.0.1-SNAPSHOT-jar-with-dependencies.jar')),
      functionName: `${props.appName}-${lowerCaseId}`,
      handler: 'io.openmessaging.benchmark.driver.sns.sqsSnsSqsBenchmarkConsumer::handleRequest',
      timeout: Duration.seconds(props.functionTimeoutSeconds),
      memorySize: 512,
      tracing: Tracing.ACTIVE,
      role: iamRole,
      environment: {
        REGION: this.region,
        SNS_URI: targetTopic ? `arn:aws:sns:${this.region}:${this.account}:${targetTopic}` : '',
        SNS_URIS: targetTopic ? targetTopic : '',
        IS_TPC_H: `${props.isTpcH}`,
        DEBUG: props.debug ? 'TRUE' : 'FALSE',
      },
      retryAttempts: 0
    });

    iamRole.addManagedPolicy(
      ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')
    )

    lambda.addEventSource(
      new SqsEventSource(snsSqsConsumerLambdaQueue,
        {
          batchSize: props.batchSize,
          maxBatchingWindow: props.maxBatchingWindow,
          reportBatchItemFailures: true
        })
    )

    return lambda
  }
}

export default {}
