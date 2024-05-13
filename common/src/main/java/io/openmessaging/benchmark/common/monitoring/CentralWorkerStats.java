/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openmessaging.benchmark.common.monitoring;


import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.openmessaging.benchmark.common.EnvironmentConfiguration;
import io.openmessaging.benchmark.common.ObjectMappers;
import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;
import org.apache.bookkeeper.stats.Counter;
import org.apache.bookkeeper.stats.NullStatsLogger;
import org.apache.bookkeeper.stats.StatsLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CentralWorkerStats implements WorkerStats {

    private static final AmazonSQS sqsClient =
            AmazonSQSClientBuilder.standard()
                    .withRegion(EnvironmentConfiguration.getRegion())
                    .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                    .build();
    private static final CumulativeLatencies cumulativeLatencies = new CumulativeLatencies();
    protected final LongAdder messagesSent = new LongAdder();
    protected final Counter messagesSentCounter;
    protected final LongAdder totalMessagesSent = new LongAdder();
    protected final LongAdder messagesReceived = new LongAdder();
    protected final Counter messagesReceivedCounter;
    protected final LongAdder totalMessagesReceived = new LongAdder();

    public CentralWorkerStats() {
        this(NullStatsLogger.INSTANCE);
    }

    public CentralWorkerStats(StatsLogger statsLogger) {
        StatsLogger producerStatsLogger = statsLogger.scope("producer");
        this.messagesSentCounter = producerStatsLogger.getCounter("messages_sent");
        StatsLogger consumerStatsLogger = statsLogger.scope("consumer");
        this.messagesReceivedCounter = consumerStatsLogger.getCounter("messages_recv");
        log.info("Central worker stats initialized");
    }

    @Override
    public void recordMessageReceived(
            long payloadLength,
            long endToEndLatencyMicros,
            String experimentId,
            String messageId,
            boolean isTpcH)
            throws IOException {
        messagesReceived.increment();
        totalMessagesReceived.increment();
        messagesReceivedCounter.inc();
        MonitoredReceivedMessage message =
                new MonitoredReceivedMessage(
                        payloadLength,
                        endToEndLatencyMicros,
                        experimentId != null ? experimentId : "UNAVAILABLE",
                        messageId,
                        isTpcH);
        String body = writer.writeValueAsString(message);
        log.info("Sending received message to cloud: {}", body);
        SendMessageRequest request =
                new SendMessageRequest(EnvironmentConfiguration.getMonitoringSqsUri(), body);
        sqsClient.sendMessage(request);
    }

    @Override
    public void recordMessageProduced(
            long payloadLength,
            long intendedSendTimeNs,
            long sendTimeNs,
            long nowNs,
            String experimentId,
            String messageId,
            boolean isTpcH,
            boolean isError)
            throws IOException {
        if (!isError) {
            messagesSent.increment();
            totalMessagesSent.increment();
            messagesSentCounter.inc();
        }
        MonitoredProducedMessage message =
                new MonitoredProducedMessage(
                        payloadLength,
                        intendedSendTimeNs,
                        sendTimeNs,
                        nowNs,
                        experimentId != null ? experimentId : "UNAVAILABLE",
                        messageId,
                        isTpcH,
                        isError);
        String body = writer.writeValueAsString(message);
        log.info("Sending produced message to cloud: {}", body);
        SendMessageRequest request =
                new SendMessageRequest(EnvironmentConfiguration.getMonitoringSqsUri(), body);
        sqsClient.sendMessage(request);
    }

    @Override
    public void recordMessageSent() {
        totalMessagesSent.increment();
    }

    @Override
    public PeriodStats toPeriodStats() {
        PeriodStats stats = new PeriodStats();
        stats.messagesSent = messagesSent.sumThenReset();
        stats.totalMessagesSent = totalMessagesSent.sum();
        stats.messagesReceived = messagesReceived.sumThenReset();
        stats.totalMessagesReceived = totalMessagesReceived.sum();
        return stats;
    }

    @Override
    public CumulativeLatencies toCumulativeLatencies() {
        return cumulativeLatencies;
    }

    @Override
    public CountersStats toCountersStats() {
        CountersStats stats = new CountersStats();
        stats.messagesSent = totalMessagesSent.sum();
        stats.messagesReceived = totalMessagesReceived.sum();
        return stats;
    }

    @Override
    public void resetLatencies() {}

    @Override
    public void reset() {
        messagesSent.reset();
        messagesReceived.reset();
        totalMessagesSent.reset();
        totalMessagesReceived.reset();
    }

    private static final ObjectWriter writer = ObjectMappers.writer;
    private static final Logger log = LoggerFactory.getLogger(CentralWorkerStats.class);
}
