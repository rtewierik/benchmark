import {Duration, Stack} from 'aws-cdk-lib'
import {ComparisonOperator, MathExpression} from 'aws-cdk-lib/aws-cloudwatch'
import {SnsAction} from 'aws-cdk-lib/aws-cloudwatch-actions'
import {Function as LambdaFunction} from 'aws-cdk-lib/aws-lambda'
import {Topic} from 'aws-cdk-lib/aws-sns'
import {IQueue} from 'aws-cdk-lib/aws-sqs'
import {SnsSqsConsumerLambdaStackProps} from '../lib/stack-configuration'
import {EmailSubscription} from 'aws-cdk-lib/aws-sns-subscriptions'

const PERSONAL_EMAIL = 'rtewierik64@gmail.com'
const STUDENT_EMAIL = 'rubeneduardconstantijn.tewierik@estudiants.urv.cat'

export function addAlerting(stack: Stack, lambda: LambdaFunction, deadLetterQueue: IQueue, snsDeadLetterQueue: IQueue, props: SnsSqsConsumerLambdaStackProps, id: string) {
    const lowerCaseId = id.toLowerCase()
    const invocationsMetric = lambda.metricInvocations({
        period: Duration.minutes(1),
        statistic: 'sum',
    })
    const errorsMetric = lambda.metricErrors({
        period: Duration.minutes(1),
        statistic: 'sum',
    })

    const alertTopic = new Topic(stack, `SnsSqsConsumerLambdaAlertTopic${id}`, {
        displayName: `SNS-SQS driver alert topic (${id})`,
    })
    alertTopic.addSubscription(new EmailSubscription(PERSONAL_EMAIL))
    alertTopic.addSubscription(new EmailSubscription(STUDENT_EMAIL))

    const errorsAlarm = errorsMetric.createAlarm(stack, `SnsSqsConsumerLambdaErrorsAlarm${id}`, {
        alarmName: `benchmark-sns-sqs-consumer-lambda-errors-${lowerCaseId}`,
        actionsEnabled: props.alertingEnabled,
        comparisonOperator: ComparisonOperator.GREATER_THAN_THRESHOLD,
        threshold: 1,
        evaluationPeriods: 5,
        alarmDescription:
            'Amount of failed Lambda invocations above threshold',
    })
    errorsAlarm.addAlarmAction(new SnsAction(alertTopic))
    errorsAlarm.addOkAction(new SnsAction(alertTopic))

    const errorPercentageMetric = new MathExpression({
        expression: '100 * errors / invocations',
        label: '% of failed invocations',
        usingMetrics: {
            invocations: invocationsMetric,
            errors: errorsMetric,
        }
    })
    const errorsPercentageAlarm = errorPercentageMetric.createAlarm(stack, `SnsSqsConsumerLambdaErrorPercentageAlarm${id}`, {
        alarmName: `benchmark-sns-sqs-consumer-lambda-error-percentage-${lowerCaseId}`,
        actionsEnabled: props.alertingEnabled,
        comparisonOperator: ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
        threshold: 1,
        evaluationPeriods: 1,
        alarmDescription:
            'Percentage of failed lambda invocations too high',
    })
    errorsPercentageAlarm.addAlarmAction(new SnsAction(alertTopic))
    errorsPercentageAlarm.addOkAction(new SnsAction(alertTopic))

    const deadLetterQueueMessageCountMetric = deadLetterQueue.metricApproximateNumberOfMessagesVisible()
    const deadLetterQueueMessagesAddedAlarm = deadLetterQueueMessageCountMetric.createAlarm(stack, `SnsSqsConsumerLambdaMessagesAddedToDlqAlarm${id}`, {
        alarmName: `sns-sqs-consumer-lambda-dlq-messages-added-${lowerCaseId}`,
        actionsEnabled: props.alertingEnabled,
        comparisonOperator: ComparisonOperator.GREATER_THAN_THRESHOLD,
        threshold: 5,
        evaluationPeriods: 1,
        alarmDescription:
            'Number of messages in the DLQ above 5',
    })
    deadLetterQueueMessagesAddedAlarm.addAlarmAction(new SnsAction(alertTopic))
    deadLetterQueueMessagesAddedAlarm.addOkAction(new SnsAction(alertTopic))

    const deadLetterQueueMessageCount = deadLetterQueueMessageCountMetric.createAlarm(stack, `SnsSqsConsumerLambdaDlqMessageCountAlarm${id}`, {
        alarmName: `sns-sqs-consumer-lambda-dlq-message-count-${lowerCaseId}`,
        actionsEnabled: props.alertingEnabled,
        comparisonOperator: ComparisonOperator.GREATER_THAN_THRESHOLD,
        threshold: 10,
        evaluationPeriods: 1,
        alarmDescription:
            'Number of messages in the DLQ above 100',
    })
    deadLetterQueueMessageCount.addAlarmAction(new SnsAction(alertTopic))
    deadLetterQueueMessageCount.addOkAction(new SnsAction(alertTopic))

    const snsDeadLetterQueueMessageCountMetric = snsDeadLetterQueue.metricApproximateNumberOfMessagesVisible()
    const snsDeadLetterQueueMessagesAddedAlarm = snsDeadLetterQueueMessageCountMetric.createAlarm(stack, `SnsSqsConsumerLambdaMessagesAddedToSnsDlqAlarm${id}`, {
        alarmName: `sns-sqs-consumer-lambda-sns-dlq-messages-added-${lowerCaseId}`,
        actionsEnabled: props.alertingEnabled,
        comparisonOperator: ComparisonOperator.GREATER_THAN_THRESHOLD,
        threshold: 5,
        evaluationPeriods: 1,
        alarmDescription:
            'Number of messages in the DLQ above 5',
    })
    snsDeadLetterQueueMessagesAddedAlarm.addAlarmAction(new SnsAction(alertTopic))
    snsDeadLetterQueueMessagesAddedAlarm.addOkAction(new SnsAction(alertTopic))

    const snsDeadLetterQueueMessageCount = snsDeadLetterQueueMessageCountMetric.createAlarm(stack, `SnsSqsConsumerLambdaSnsDlqMessageCountAlarm${id}`, {
        alarmName: `sns-sqs-consumer-lambda-sns-dlq-message-count-${lowerCaseId}`,
        actionsEnabled: props.alertingEnabled,
        comparisonOperator: ComparisonOperator.GREATER_THAN_THRESHOLD,
        threshold: 10,
        evaluationPeriods: 1,
        alarmDescription:
            'Number of messages in the DLQ above 100',
    })
    snsDeadLetterQueueMessageCount.addAlarmAction(new SnsAction(alertTopic))
    snsDeadLetterQueueMessageCount.addOkAction(new SnsAction(alertTopic))
}
