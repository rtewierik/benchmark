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
import { SnsSqsConsumerLambdaStackProps } from './stack-configuration'

import { addMonitoring } from '../modules/monitoring'
import { addAlerting } from '../modules/alerting'
import { IKey, Key } from 'aws-cdk-lib/aws-kms'
import { ApiGatewayToSqs } from '@aws-solutions-constructs/aws-apigateway-sqs'
import { AttributeType, BillingMode, Table, TableEncryption } from 'aws-cdk-lib/aws-dynamodb'
import path = require('path')

interface DataIngestionLayer {
  sqsQueue: IQueue
  ingestionDeadLetterQueue: IQueue
}

export class ServiceStack extends Stack {
  constructor(scope: App, id: string, props: SnsSqsConsumerLambdaStackProps) {
    super(scope, id, props)

    const kmsKey = this.createSnsSqsConsumerLambdaKmsKey(props)
    const { sqsQueue, ingestionDeadLetterQueue } = this.createSnsSqsConsumerLambdaDataIngestionLayer(kmsKey, props)
    const deadLetterQueue = this.createSnsSqsConsumerLambdaLambdaDeadLetterQueue(props)
    const lambda = this.createSnsSqsConsumerLambdaLambda(sqsQueue, kmsKey, deadLetterQueue, props)
    this.createSnsSqsConsumerLambdaDynamoDb(lambda, kmsKey, props)
    addMonitoring(this, sqsQueue, lambda, deadLetterQueue, ingestionDeadLetterQueue, props)
    addAlerting(this, lambda, deadLetterQueue, ingestionDeadLetterQueue, props)
  }

  private createSnsSqsConsumerLambdaKmsKey(props: SnsSqsConsumerLambdaStackProps): Key {
    return new Key(this, 'SnsSqsConsumerLambdaKmsKey', {
      description:
        'The KMS key used to encrypt the SQS queue used in Benchmark Monitoring',
      alias: `${props.appName}-sqs-encryption`,
      enableKeyRotation: true,
      enabled: true,
    })
  }

  private createSnsSqsConsumerLambdaDataIngestionLayer(kmsKey: Key, props: SnsSqsConsumerLambdaStackProps): DataIngestionLayer {
    const { sqsQueue, deadLetterQueue, apiGatewayRole } = new ApiGatewayToSqs(this, 'SnsSqsConsumerLambdaDataIngestion', {
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

  private createSnsSqsConsumerLambdaLambdaDeadLetterQueue(props: SnsSqsConsumerLambdaStackProps): IQueue {
    const sqsQueue = new Queue(
      this,
      'SnsSqsConsumerLambdaLambdaDeadLetterQueue',
      {
        queueName: `${props.appName}-dlq`,
        encryption: QueueEncryption.KMS_MANAGED,
        dataKeyReuse: Duration.seconds(300),
        retentionPeriod: Duration.days(14)
      })

    return sqsQueue
  }

  private createSnsSqsConsumerLambdaLambda(snsSqsConsumerLambdaQueue: IQueue, SnsSqsConsumerLambdaKmsKey: IKey, deadLetterQueue: IQueue, props: SnsSqsConsumerLambdaStackProps): LambdaFunction {
    const iamRole = new Role(
      this,
      'SnsSqsConsumerLambdaLambdaIamRole',
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
        resources: [SnsSqsConsumerLambdaKmsKey.keyArn],
      })
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

    const lambda = new LambdaFunction(this, 'SnsSqsConsumerLambdaFunction', {
      description: 'This Lambda function ingests experimental results from infrastructure participating in experiments and stores collected data in a DynamoDB table',
      runtime: Runtime.JAVA_8_CORRETTO,
      code: Code.fromAsset(path.join(__dirname, '../path/to/your/jar'), {
        bundling: {
          image: Runtime.JAVA_8_CORRETTO.bundlingImage,
          command: [
            'bash', '-c',
            'cp -R /asset-input/* /asset-output/',
          ],
        },
      }),
      functionName: props.appName,
      handler: 'com.example.MyLambdaHandler::handleRequest', // Your Lambda handler
      timeout: Duration.seconds(props.functionTimeoutSeconds),
      memorySize: 512,
      tracing: Tracing.ACTIVE,
      role: iamRole,
      environment: {
        REGION: this.region,
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

  private createSnsSqsConsumerLambdaDynamoDb(lambda: LambdaFunction, kmsKey: IKey, props: SnsSqsConsumerLambdaStackProps) {
    const table = new Table(this, 'SnsSqsConsumerLambdaDynamoDbTable', {
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
