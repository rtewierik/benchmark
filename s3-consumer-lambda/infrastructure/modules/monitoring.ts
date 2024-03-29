import {S3ConsumerLambdaStackProps} from '../lib/stack-configuration'
import {Dashboard, GraphWidget, Metric, Row} from 'aws-cdk-lib/aws-cloudwatch'
import {Duration, Stack} from 'aws-cdk-lib'
import {Function as LambdaFunction} from 'aws-cdk-lib/aws-lambda'
import {IQueue} from 'aws-cdk-lib/aws-sqs'

export function addMonitoring(stack: Stack, lambda: LambdaFunction, deadLetterQueue: IQueue, props: S3ConsumerLambdaStackProps, id: string) {
    const lowerCaseId = id.toLowerCase()
    const widgetWidth = 8
    const dashboardName = `${props.appName}-dashboard-${lowerCaseId}`

    const sqsTimeInStream = new GraphWidget({
        width: widgetWidth,
        title: 'SQS: max message time spent in queue',
        left: [new Metric({
            metricName: 'ApproximateAgeOfOldestMessage',
            namespace: 'AWS/SQS',
            dimensionsMap: {'FunctionName': lambda.functionName},
            statistic: 'max',
            label: 'Time spent in SQS',
            period: Duration.minutes(1)
        })],
        leftYAxis: {
            min: 0
        }
    })

    const lambdaInvocations = new GraphWidget({
        width: widgetWidth,
        title: 'Lambda: # of invocations',
        left: [new Metric({
            metricName: 'Invocations',
            namespace: 'AWS/Lambda',
            dimensionsMap: {'FunctionName': lambda.functionName},
            statistic: 'sum',
            label: 'Invocations',
            period: Duration.minutes(1)
        })],
        leftYAxis: {
            min: 0
        }
    })

    const lambdaExecutionDuration = new GraphWidget({
        width: widgetWidth,
        title: 'Lambda: average execution duration',
        left: [new Metric({
            metricName: 'Duration',
            namespace: 'AWS/Lambda',
            dimensionsMap: {'FunctionName': lambda.functionName},
            statistic: 'avg',
            label: 'Average',
            period: Duration.minutes(1)
        }),
            new Metric({
                metricName: 'Duration',
                namespace: 'AWS/Lambda',
                dimensionsMap: {'FunctionName': lambda.functionName},
                statistic: 'max',
                label: 'Maximum',
                period: Duration.minutes(1)
            })],
        leftYAxis: {
            min: 0
        }
    })

    const lambdaErrors = new GraphWidget({
        width: widgetWidth,
        title: 'Lambda: # of errors',
        left: [new Metric({
            metricName: 'Errors',
            namespace: 'AWS/Lambda',
            dimensionsMap: {'FunctionName': lambda.functionName},
            statistic: 'sum',
            label: 'Errors',
            period: Duration.minutes(1),
            color: '#FF0000'
        })],
        leftYAxis: {
            min: 0
        }
    })

    const dlqMessagesCount = new GraphWidget({
        width: widgetWidth,
        title: 'DLQ: # of messages in queue',
        left: [new Metric({
            metricName: 'ApproximateNumberOfMessagesVisible',
            namespace: 'AWS/SQS',
            dimensionsMap: {'QueueName': deadLetterQueue.queueName},
            statistic: 'sum',
            label: 'Amount in queue',
            period: Duration.minutes(1)
        })],
        leftYAxis: {
            min: 0
        }
    })

    const dashboard = new Dashboard(stack, dashboardName, {
        dashboardName: dashboardName
    })

    dashboard.addWidgets(new Row(sqsTimeInStream, lambdaInvocations, lambdaErrors))
    dashboard.addWidgets(new Row(lambdaErrors, lambdaExecutionDuration, dlqMessagesCount))
}