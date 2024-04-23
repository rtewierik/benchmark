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
  IRole,
  ManagedPolicy,
  PolicyStatement,
  Role,
  ServicePrincipal,
} from 'aws-cdk-lib/aws-iam'
import { BenchmarkMonitoringStackProps } from './stack-configuration'

import { addMonitoring } from '../modules/monitoring'
import { addAlerting } from '../modules/alerting'
import { ApiGatewayToSqs } from '@aws-solutions-constructs/aws-apigateway-sqs'
import { AttributeType, BillingMode, Table, TableEncryption } from 'aws-cdk-lib/aws-dynamodb'

const IAM_ROLE_NAMES = [
  "kafka-iam-role",
  "pravega-iam-role",
  "pulsar-iam-role",
  "rabbitmq-iam-role",
  "redis-iam-role",
  "s3-iam-role",
  "sns-sqs-iam-role",
]

interface DataIngestionLayer {
  sqsQueue: IQueue
  ingestionDeadLetterQueue: IQueue
}

export class ServiceStack extends Stack {
  constructor(scope: App, id: string, props: BenchmarkMonitoringStackProps) {
    super(scope, id, props)

    const iamRoles = this.getIamRoles()
    const { sqsQueue, ingestionDeadLetterQueue } = this.createBenchmarkMonitoringDataIngestionLayer(iamRoles, props)
    const deadLetterQueue = this.createBenchmarkMonitoringLambdaDeadLetterQueue(props)
    const lambda = this.createBenchmarkMonitoringLambda(sqsQueue, deadLetterQueue, props)
    this.createBenchmarkMonitoringDynamoDb(lambda, props)
    addMonitoring(this, sqsQueue, lambda, deadLetterQueue, ingestionDeadLetterQueue, props)
    addAlerting(this, lambda, deadLetterQueue, ingestionDeadLetterQueue, props)
  }

  private getIamRoles() {
    return IAM_ROLE_NAMES.map((roleName, index) => Role.fromRoleName(this, `BenchmarkMonitoringRole${index}`, roleName))
  }

  private createBenchmarkMonitoringDataIngestionLayer(iamRoles: IRole[], props: BenchmarkMonitoringStackProps): DataIngestionLayer {
    const { sqsQueue, deadLetterQueue } = new ApiGatewayToSqs(this, 'BenchmarkMonitoringDataIngestion', {
      queueProps: {
        queueName: props.appName,
        visibilityTimeout: Duration.seconds(props.eventsVisibilityTimeoutSeconds),
        encryption: QueueEncryption.KMS_MANAGED,
        dataKeyReuse: Duration.seconds(300),
        retentionPeriod: Duration.days(14),
      },
      deployDeadLetterQueue: true,
      maxReceiveCount: 10,
      allowCreateOperation: true
    })
    iamRoles.forEach(role => sqsQueue.grantSendMessages(role));
    if (!deadLetterQueue) {
      throw new Error('The ApiGatewayToSqs dependency did not yield a dead letter queue!')
    }
    return { sqsQueue, ingestionDeadLetterQueue: deadLetterQueue.queue }
  }

  private createBenchmarkMonitoringLambdaDeadLetterQueue(props: BenchmarkMonitoringStackProps): IQueue {
    const sqsQueue = new Queue(
      this,
      'BenchmarkMonitoringLambdaDeadLetterQueue',
      {
        queueName: `${props.appName}-dlq`,
        encryption: QueueEncryption.KMS_MANAGED,
        dataKeyReuse: Duration.seconds(300),
        retentionPeriod: Duration.days(14)
      })

    return sqsQueue
  }

  private createBenchmarkMonitoringLambda(benchmarkMonitoringQueue: IQueue, deadLetterQueue: IQueue, props: BenchmarkMonitoringStackProps): LambdaFunction {
    const iamRole = new Role(
      this,
      'BenchmarkMonitoringLambdaIamRole',
      {
        assumedBy: new ServicePrincipal('lambda.amazonaws.com'),
        roleName: `${props.appName}-lambda-role`,
        description:
          'IAM Role for granting Lambda receive message to the Benchmark Monitoring queue and send message to the DLQ',
      }
    )

    iamRole.addToPolicy(
      new PolicyStatement({
        actions: ['SQS:ReceiveMessage', 'SQS:SendMessage'],
        resources: [benchmarkMonitoringQueue.queueArn],
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
      'BenchmarkMonitoringLambda',
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
        reservedConcurrentExecutions: props.reservedConcurrentExecutions,
        environment: {
          REGION: this.region,
          DEBUG: props.debug ? 'TRUE' : 'FALSE',
        },
        retryAttempts: 0
      }
    )

    lambda.addEventSource(
      new SqsEventSource(benchmarkMonitoringQueue,
        {
          batchSize: props.batchSize,
          maxBatchingWindow: props.maxBatchingWindow,
          reportBatchItemFailures: true
        })
    )

    return lambda
  }

  private createBenchmarkMonitoringDynamoDb(lambda: LambdaFunction, props: BenchmarkMonitoringStackProps) {
    const table = new Table(this, 'BenchmarkMonitoringDynamoDbTable', {
      tableName: props.appName,
      encryption: TableEncryption.CUSTOMER_MANAGED,
      partitionKey: {
        name: 'transactionId',
        type: AttributeType.STRING,
      },
      readCapacity: props.readCapacity,
      writeCapacity: props.writeCapacity,
      billingMode: BillingMode.PROVISIONED,
      removalPolicy: RemovalPolicy.DESTROY,
    })

    table.grantReadData(lambda)
    table.grantWriteData(lambda)
  }
}

export default {}
