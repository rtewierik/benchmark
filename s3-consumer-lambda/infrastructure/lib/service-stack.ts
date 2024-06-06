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
import { S3EventSourceV2 } from 'aws-cdk-lib/aws-lambda-event-sources'
import {
  ManagedPolicy,
  PolicyStatement,
  Role,
  ServicePrincipal,
} from 'aws-cdk-lib/aws-iam'
import { S3ConsumerLambdaStackProps } from './stack-configuration'

import path = require('path')
import { Bucket, EventType, IBucket } from 'aws-cdk-lib/aws-s3'
import { LambdaDestination } from 'aws-cdk-lib/aws-s3-notifications'

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

const getFunctionName = (props: S3ConsumerLambdaStackProps, id: string) =>
  `${props.appName}-${id.toLowerCase()}`


export class ServiceStack extends Stack {
  constructor(scope: App, id: string, props: S3ConsumerLambdaStackProps, createMapAndResult: boolean, start: number, end: number, createEventSources = false) {
    super(scope, id, props)
    const bucket = Bucket.fromBucketName(this, 'S3ConsumerLambdaSourceBucket', props.bucketName + '0')
    const chunksBucket = Bucket.fromBucketName(this, 'S3ConsumerChunksBucket', 'tpc-h-chunks')
    const monitoringSqsQueue = Queue.fromQueueArn(this, 'S3ConsumerMonitoringSqsQueue', props.monitoringSqsArn)
    if (props.isTpcH) {
      if (createEventSources) {
        if (start == 0)  {
          const resultLambda = LambdaFunction.fromFunctionName(this, `ResultLambda`, getFunctionName(props, RESULT_ID));
          bucket.addEventNotification(EventType.OBJECT_CREATED, new LambdaDestination(resultLambda), {
            prefix: this.getS3Prefix(props, RESULT_ID, 333)
          });
          const mapLambda = LambdaFunction.fromFunctionName(this, `MapLambda`, getFunctionName(props, MAP_ID));
          bucket.addEventNotification(EventType.OBJECT_CREATED, new LambdaDestination(mapLambda), {
            prefix: this.getS3Prefix(props, MAP_ID, 666)
          });
        }
        const iterativeBucketIndex = 1 + (start / 50);
        const iterativeBucket = Bucket.fromBucketName(this, 'S3ConsumerLambdaSourceBucketIterative', props.bucketName + `${iterativeBucketIndex}`)
        for (var i = start; i < end; i++) {
          const reduceId =`${REDUCE_ID}${i}`;
          const reduceLambda = LambdaFunction.fromFunctionName(this, `ReduceLambda${i}`, getFunctionName(props, reduceId));
          iterativeBucket.addEventNotification(EventType.OBJECT_CREATED, new LambdaDestination(reduceLambda), {
            prefix: this.getS3Prefix(props, reduceId, i)
          });
        }
      } else {
        const resultPrefix = this.getS3Prefix(props, RESULT_ID, 333)
        const mapPrefix = this.getS3Prefix(props, MAP_ID, 666)
        const s3Prefixes = [resultPrefix, mapPrefix]
        for (var i = 0; i < props.numberOfConsumers; i++) {
          const reducePrefixId = `${REDUCE_ID}${i}`
          s3Prefixes.push(this.getS3Prefix(props, reducePrefixId, i))
        }
        const aggregateConfig = { ...AGGREGATE_CONFIG, s3Prefixes }
        if (createMapAndResult) {
          const mapConfiguration = { s3Prefixes, ...props }
          this.createDataIngestionLayer(props, MAP_ID, bucket, chunksBucket, monitoringSqsQueue, mapConfiguration, mapPrefix)
        }
        for (var i = start; i < end; i++) {
          const reducePrefixId = `${REDUCE_ID}${i}`
          const prefix = s3Prefixes[2 + i]
          this.createDataIngestionLayer(props, reducePrefixId, bucket, chunksBucket, monitoringSqsQueue, aggregateConfig, prefix)
        }
        if (createMapAndResult) {
          this.createDataIngestionLayer(props, RESULT_ID, bucket, chunksBucket, monitoringSqsQueue, aggregateConfig, resultPrefix)
        }
      }
    } else {
      const iterativeBucketIndex = 1 + (start / 50);
      const iterativeBucket = Bucket.fromBucketName(this, 'S3ConsumerLambdaSourceBucketIterative', props.bucketName + `${iterativeBucketIndex}`)
      for (var i = start; i < end; i++) {
        const consumerPrefixId = `${DEFAULT_ID}${i}`
        const prefix = this.getS3Prefix(props, consumerPrefixId, i)
        this.createDataIngestionLayer(props, consumerPrefixId, iterativeBucket, chunksBucket, monitoringSqsQueue, AGGREGATE_CONFIG, prefix)
      }
    }
  }

  private getS3Prefix(props: S3ConsumerLambdaStackProps, id: string, index: number) {
    return `${index}-${props.appName}-${id.toLowerCase()}`
  }

  private createDataIngestionLayer(props: S3ConsumerLambdaStackProps, id: string, bucket: IBucket, chunksBucket: IBucket, monitoringSqsQueue: IQueue, lambdaConfiguration: LambdaConfiguration, s3Prefix: string) {
    const lambdaDeadLetterQueue = this.createS3ConsumerLambdaDeadLetterQueue(props, id)
    this.createS3ConsumerLambda(bucket, chunksBucket, monitoringSqsQueue, lambdaDeadLetterQueue, props, id, lambdaConfiguration, s3Prefix)
    // addMonitoring(this, lambda, lambdaDeadLetterQueue, props, id)
    // addAlerting(this, lambda, lambdaDeadLetterQueue, props, id)
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

  private createS3ConsumerLambda(bucket: IBucket, chunksBucket: IBucket, monitoringSqsQueue: IQueue, deadLetterQueue: IQueue, props: S3ConsumerLambdaStackProps, id: string, lambdaConfiguration: LambdaConfiguration, s3Prefix: string): LambdaFunction {
    const { numberOfConsumers, functionTimeoutSeconds } = lambdaConfiguration
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
    chunksBucket.grantRead(iamRole)
    monitoringSqsQueue.grantSendMessages(iamRole)
    iamRole.addToPolicy(
      new PolicyStatement({
        actions: ['SQS:SendMessage'],
        resources: [deadLetterQueue.queueArn],
      })
    )
    iamRole.addToPolicy(
      new PolicyStatement({
        actions: ['s3:*'],
        resources: [
          'arn:aws:s3:::benchmarking-events0',
          'arn:aws:s3:::benchmarking-events1',
          'arn:aws:s3:::benchmarking-events2',
          'arn:aws:s3:::benchmarking-events3',
          'arn:aws:s3:::benchmarking-events4',
          'arn:aws:s3:::benchmarking-events5',
          'arn:aws:s3:::benchmarking-events6',
          'arn:aws:s3:::benchmarking-events7',
          'arn:aws:s3:::benchmarking-events8',
          'arn:aws:s3:::benchmarking-events0/*',
          'arn:aws:s3:::benchmarking-events1/*',
          'arn:aws:s3:::benchmarking-events2/*',
          'arn:aws:s3:::benchmarking-events3/*',
          'arn:aws:s3:::benchmarking-events4/*',
          'arn:aws:s3:::benchmarking-events5/*',
          'arn:aws:s3:::benchmarking-events6/*',
          'arn:aws:s3:::benchmarking-events7/*',
          'arn:aws:s3:::benchmarking-events8/*'
        ]
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
    const layer = LayerVersion.fromLayerVersionArn(this, `S3ConsumerLambdaFunctionLambdaInsightsLayerFromArn${id}`, layerArn);

    lambda.addLayers(layer);

    return lambda
  }
}

export default {}
