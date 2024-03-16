﻿import { Stack, Duration } from 'aws-cdk-lib'
import { ComparisonOperator, MathExpression } from 'aws-cdk-lib/aws-cloudwatch'
import { SnsAction } from 'aws-cdk-lib/aws-cloudwatch-actions'
import { Function as LambdaFunction } from 'aws-cdk-lib/aws-lambda'
import { Topic } from 'aws-cdk-lib/aws-sns'
import { IQueue } from 'aws-cdk-lib/aws-sqs'
import { SnsSqsDriverStackProps } from '../lib/stack-configuration'
import { EmailSubscription } from 'aws-cdk-lib/aws-sns-subscriptions'

const PERSONAL_EMAIL = 'rtewierik64@gmail.com'
const STUDENT_EMAIL = 'rubeneduardconstantijn.tewierik@estudiants.urv.cat'

export function addAlerting(stack: Stack, lambda: LambdaFunction, deadLetterQueue: IQueue, ingestionDeadLetterQueue: IQueue, props: SnsSqsDriverStackProps) {
  const invocationsMetric = lambda.metricInvocations({
    period: Duration.minutes(1),
    statistic: 'sum',
  })
  const errorsMetric = lambda.metricErrors({
    period: Duration.minutes(1),
    statistic: 'sum',
  })

  const alertTopic = new Topic(stack, 'SnsSqsDriverAlertTopic', {
    displayName: 'SNS/SQS driver alert topic',
  })
  alertTopic.addSubscription(new EmailSubscription(PERSONAL_EMAIL))
  alertTopic.addSubscription(new EmailSubscription(STUDENT_EMAIL))

  const errorsAlarm = errorsMetric.createAlarm(stack, 'SnsSqsDriverErrorsAlarm', {
    alarmName: 'benchmark-sns-sqs-driver-errors',
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
  const errorsPercentageAlarm = errorPercentageMetric.createAlarm(stack, 'SnsSqsDriverErrorPercentageAlarm', {
    alarmName: 'benchmark-sns-sqs-driver-error-percentage',
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
  const deadLetterQueueMessagesAddedAlarm = deadLetterQueueMessageCountMetric.createAlarm(stack, 'SnsSqsDriverMessagesAddedToDlqAlarm', {
    alarmName: 'sns-sqs-driver-dlq-messages-added',
    actionsEnabled: props.alertingEnabled,
    comparisonOperator: ComparisonOperator.GREATER_THAN_THRESHOLD,
    threshold: 5,
    evaluationPeriods: 1,
    alarmDescription:
        'Number of messages in the DLQ above 5',
  })
  deadLetterQueueMessagesAddedAlarm.addAlarmAction(new SnsAction(alertTopic))
  deadLetterQueueMessagesAddedAlarm.addOkAction(new SnsAction(alertTopic))

  const deadLetterQueueMessageCount = deadLetterQueueMessageCountMetric.createAlarm(stack, 'SnsSqsDriverDlqMessageCountAlarm', {
    alarmName: 'sns-sqs-driver-dlq-message-count',
    actionsEnabled: props.alertingEnabled,
    comparisonOperator: ComparisonOperator.GREATER_THAN_THRESHOLD,
    threshold: 10,
    evaluationPeriods: 1,
    alarmDescription:
        'Number of messages in the DLQ above 100',
  })
  deadLetterQueueMessageCount.addAlarmAction(new SnsAction(alertTopic))
  deadLetterQueueMessageCount.addOkAction(new SnsAction(alertTopic))

  const ingestionDeadLetterQueueMessageCountMetric = ingestionDeadLetterQueue.metricApproximateNumberOfMessagesVisible()
  const ingestionDeadLetterQueueMessagesAddedAlarm = ingestionDeadLetterQueueMessageCountMetric.createAlarm(stack, 'SnsSqsDriverMessagesAddedToIngestionDlqAlarm', {
    alarmName: 'sns-sqs-driver-ingestion-dlq-messages-added',
    actionsEnabled: props.alertingEnabled,
    comparisonOperator: ComparisonOperator.GREATER_THAN_THRESHOLD,
    threshold: 5,
    evaluationPeriods: 1,
    alarmDescription:
        'Number of messages in the ingestion DLQ above 5',
  })
  ingestionDeadLetterQueueMessagesAddedAlarm.addAlarmAction(new SnsAction(alertTopic))
  ingestionDeadLetterQueueMessagesAddedAlarm.addOkAction(new SnsAction(alertTopic))

  const ingestionDeadLetterQueueMessageCount = ingestionDeadLetterQueueMessageCountMetric.createAlarm(stack, 'SnsSqsDriverIngestionDlqMessageCountAlarm', {
    alarmName: 'sns-sqs-driver-ingestion-dlq-message-count',
    actionsEnabled: props.alertingEnabled,
    comparisonOperator: ComparisonOperator.GREATER_THAN_THRESHOLD,
    threshold: 10,
    evaluationPeriods: 1,
    alarmDescription:
        'Number of messages in the ingestion DLQ above 100',
  })
  ingestionDeadLetterQueueMessageCount.addAlarmAction(new SnsAction(alertTopic))
  ingestionDeadLetterQueueMessageCount.addOkAction(new SnsAction(alertTopic))
}
