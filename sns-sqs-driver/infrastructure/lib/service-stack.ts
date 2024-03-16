import { App, Duration, RemovalPolicy, Stack } from 'aws-cdk-lib'
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
import { SnsSqsDriverStackProps } from './stack-configuration'

import { addMonitoring } from '../modules/monitoring'
import { addAlerting } from '../modules/alerting'
import { IKey, Key } from 'aws-cdk-lib/aws-kms'
import { ApiGatewayToSqs } from '@aws-solutions-constructs/aws-apigateway-sqs'
import { AttributeType, BillingMode, Table, TableEncryption } from 'aws-cdk-lib/aws-dynamodb'

interface DataIngestionLayer {
  sqsQueue: IQueue
  ingestionDeadLetterQueue: IQueue
}

export class ServiceStack extends Stack {
  constructor(scope: App, id: string, props: SnsSqsDriverStackProps) {
    super(scope, id, props)

    const kmsKey = this.createSnsSqsDriverKmsKey(props)
    const { sqsQueue, ingestionDeadLetterQueue } = this.createSnsSqsDriverDataIngestionLayer(kmsKey, props)
    const deadLetterQueue = this.createSnsSqsDriverLambdaDeadLetterQueue(props)
    const lambda = this.createSnsSqsDriverLambda(sqsQueue, kmsKey, deadLetterQueue, props)
    this.createSnsSqsDriverDynamoDb(lambda, kmsKey, props)
    addMonitoring(this, sqsQueue, lambda, deadLetterQueue, ingestionDeadLetterQueue, props)
    addAlerting(this, lambda, deadLetterQueue, ingestionDeadLetterQueue, props)
  }

  private createSnsSqsDriverKmsKey(props: SnsSqsDriverStackProps): Key {
    return new Key(this, 'SnsSqsDriverKmsKey', {
      description:
        'The KMS key used to encrypt the SQS queue used in Benchmark Monitoring',
      alias: `${props.appName}-sqs-encryption`,
      enableKeyRotation: true,
      enabled: true,
    })
  }

  private createSnsSqsDriverDataIngestionLayer(kmsKey: Key, props: SnsSqsDriverStackProps): DataIngestionLayer {
    const { sqsQueue, deadLetterQueue, apiGatewayRole } = new ApiGatewayToSqs(this, 'SnsSqsDriverDataIngestion', {
      queueProps: {
        queueName: props.appName,
        visibilityTimeout: Duration.seconds(props.eventsVisibilityTimeoutSeconds),
        encryption: QueueEncryption.KMS,
        dataKeyReuse: Duration.seconds(300),
        retentionPeriod: Duration.days(14),
      },
      deployDeadLetterQueue: true,
      maxReceiveCount: 10,
      allowCreateOperation: true,
      encryptionKey: kmsKey
    })
    if (!deadLetterQueue) {
      throw new Error('The ApiGatewayToSqs dependency did not yield a dead letter queue!')
    }
    kmsKey.grantEncryptDecrypt(apiGatewayRole);
    return { sqsQueue, ingestionDeadLetterQueue: deadLetterQueue.queue }
  }

  private createSnsSqsDriverLambdaDeadLetterQueue(props: SnsSqsDriverStackProps): IQueue {
    const sqsQueue = new Queue(
      this,
      'SnsSqsDriverLambdaDeadLetterQueue',
      {
        queueName: `${props.appName}-dlq`,
        encryption: QueueEncryption.KMS_MANAGED,
        dataKeyReuse: Duration.seconds(300),
        retentionPeriod: Duration.days(14)
      })

    return sqsQueue
  }

  private createSnsSqsDriverLambda(SnsSqsDriverQueue: IQueue, SnsSqsDriverKmsKey: IKey, deadLetterQueue: IQueue, props: SnsSqsDriverStackProps): LambdaFunction {
    const iamRole = new Role(
      this,
      'SnsSqsDriverLambdaIamRole',
      {
        assumedBy: new ServicePrincipal('lambda.amazonaws.com'),
        roleName: `${props.appName}-lambda-role`,
        description:
          'IAM Role for granting Lambda receive message to the Benchmark Monitoring queue and send message to the DLQ',
      }
    )

    iamRole.addToPolicy(
      new PolicyStatement({
        actions: ['kms:Decrypt', 'kms:GenerateDataKey'],
        resources: [SnsSqsDriverKmsKey.keyArn],
      })
    )
    iamRole.addToPolicy(
      new PolicyStatement({
        actions: ['SQS:ReceiveMessage', 'SQS:SendMessage'],
        resources: [SnsSqsDriverQueue.queueArn],
      })
    )
    iamRole.addToPolicy(
      new PolicyStatement({
        actions: ['SQS:SendMessage'],
        resources: [deadLetterQueue.queueArn],
      })
    )

    iamRole.addManagedPolicy(
      ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')
    )

    const lambda = new LambdaFunction(
      this,
      'SnsSqsDriverLambda',
      {
        description: 'This Lambda function ingests experimental results from infrastructure participating in experiments and stores collected data in a DynamoDB table',
        runtime: Runtime.NODEJS_18_X,
        code: Code.fromAsset('../lambda/build'),
        functionName: props.appName,
        handler: 'index.handler',
        timeout: Duration.seconds(props.functionTimeoutSeconds),
        memorySize: 512,
        tracing: Tracing.ACTIVE,
        role: iamRole,
        environment: {
          REGION: this.region,
          DEBUG: props.debug ? 'TRUE' : 'FALSE',
        },
        retryAttempts: 0
      }
    )

    lambda.addEventSource(
      new SqsEventSource(SnsSqsDriverQueue,
        {
          batchSize: props.batchSize,
          maxBatchingWindow: props.maxBatchingWindow,
          reportBatchItemFailures: true
        })
    )

    return lambda
  }

  private createSnsSqsDriverDynamoDb(lambda: LambdaFunction, kmsKey: IKey, props: SnsSqsDriverStackProps) {
    const table = new Table(this, 'SnsSqsDriverDynamoDbTable', {
      tableName: props.appName,
      encryption: TableEncryption.CUSTOMER_MANAGED,
      encryptionKey: kmsKey,
      partitionKey: {
        name: 'experimentId',
        type: AttributeType.STRING,
      },
      readCapacity: 1,
      writeCapacity: 1,
      billingMode: BillingMode.PROVISIONED,
      removalPolicy: RemovalPolicy.DESTROY,
    })

    table.grantReadData(lambda)
    table.grantWriteData(lambda)
  }
}

export default {}
