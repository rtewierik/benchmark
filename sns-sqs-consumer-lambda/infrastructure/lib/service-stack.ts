import {App, Duration, Stack} from 'aws-cdk-lib'
import {Code, Function as LambdaFunction, Runtime, Tracing,} from 'aws-cdk-lib/aws-lambda'
import {IQueue, Queue, QueueEncryption,} from 'aws-cdk-lib/aws-sqs'
import {SqsEventSource} from 'aws-cdk-lib/aws-lambda-event-sources'
import {ManagedPolicy, PolicyStatement, Role, ServicePrincipal,} from 'aws-cdk-lib/aws-iam'
import {SnsSqsConsumerLambdaStackProps} from './stack-configuration'

import {addMonitoring} from '../modules/monitoring'
import {addAlerting} from '../modules/alerting'
import {ITopic, Topic} from 'aws-cdk-lib/aws-sns'
import {SqsSubscription} from 'aws-cdk-lib/aws-sns-subscriptions'
import path = require('path');

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
        if (props.isTpcH) {
            const snsTopicNames = [
                this.getSnsTopicName(props, MAP_ID),
                this.getSnsTopicName(props, RESULT_ID)
            ]
            for (var i = 0; i < props.numberOfConsumers; i++) {
                const reduceTopicId = `${REDUCE_ID}${i}`
                snsTopicNames.push(this.getSnsTopicName(props, reduceTopicId))
            }
            const aggregateConfig = {...AGGREGATE_CONFIG, snsTopicNames}
            const mapTopic = this.createSnsSqsConsumerLambdaSnsTopic(props, MAP_ID)
            const {topic, deadLetterQueue} = mapTopic
            const mapConfiguration = {snsTopicNames, ...props}
            const mapQueue = this.createSnsSqsConsumerLambdaQueue(props, MAP_ID, mapConfiguration)
            topic.addSubscription(new SqsSubscription(mapQueue, {deadLetterQueue, rawMessageDelivery: true}))
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

    private getSnsTopicName(props: SnsSqsConsumerLambdaStackProps, id: string) {
        return `${props.appName}-sns-topic-${id.toLowerCase()}`
    }

    private createDataIngestionLayer(props: SnsSqsConsumerLambdaStackProps, id: string, lambdaConfiguration: LambdaConfiguration, existingTopic?: SnsTopic, existingQueue?: IQueue) {
        const {
            queue,
            snsDeadLetterQueue,
            lambdaDeadLetterQueue
        } = this.createSnsSqsConsumerLambdaDataIngestionLayer(props, id, lambdaConfiguration, existingTopic, existingQueue)
        const lambda = this.createSnsSqsConsumerLambda(queue, lambdaDeadLetterQueue, props, id, lambdaConfiguration)
        addMonitoring(this, queue, lambda, lambdaDeadLetterQueue, snsDeadLetterQueue, props, id)
        addAlerting(this, lambda, lambdaDeadLetterQueue, snsDeadLetterQueue, props, id)
    }

    private createSnsSqsConsumerLambdaDataIngestionLayer(props: SnsSqsConsumerLambdaStackProps, id: string, lambdaConfiguration: LambdaConfiguration, existingTopic: SnsTopic | undefined, existingQueue: IQueue | undefined): DataIngestionLayer {
        const {topic, deadLetterQueue} = existingTopic ?? this.createSnsSqsConsumerLambdaSnsTopic(props, id)
        const queue = existingQueue ?? this.createSnsSqsConsumerLambdaQueue(props, id, lambdaConfiguration)
        const lambdaDeadLetterQueue = this.createSnsSqsConsumerLambdaDeadLetterQueue(props, id)
        !existingQueue && topic.addSubscription(new SqsSubscription(queue, {deadLetterQueue, rawMessageDelivery: true}))
        return {topic, queue, snsDeadLetterQueue: deadLetterQueue, lambdaDeadLetterQueue}
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
        return {topic, deadLetterQueue}
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

    private createSnsSqsConsumerLambda(snsSqsConsumerLambdaQueue: IQueue, deadLetterQueue: IQueue, props: SnsSqsConsumerLambdaStackProps, id: string, lambdaConfiguration: LambdaConfiguration): LambdaFunction {
        const {snsTopicNames, numberOfConsumers, functionTimeoutSeconds} = lambdaConfiguration
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
            },
            retryAttempts: 2
        });

        iamRole.addManagedPolicy(
            ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')
        )

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
