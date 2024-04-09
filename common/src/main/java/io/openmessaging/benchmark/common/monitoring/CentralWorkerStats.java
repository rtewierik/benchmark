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
import org.apache.bookkeeper.stats.NullStatsLogger;
import org.apache.bookkeeper.stats.StatsLogger;

import java.io.IOException;

public class CentralWorkerStats implements WorkerStats {

    private static final AmazonSQS sqsClient =
            AmazonSQSClientBuilder.standard()
                    .withRegion(EnvironmentConfiguration.getRegion())
                    .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                    .build();
    private static final PeriodStats periodStats = new PeriodStats();
    private static final CumulativeLatencies cumulativeLatencies = new CumulativeLatencies();
    private static final CountersStats countersStats = new CountersStats();
    protected final StatsLogger statsLogger;

    public CentralWorkerStats() {
        this(NullStatsLogger.INSTANCE);
    }

    public CentralWorkerStats(StatsLogger statsLogger) {
        this.statsLogger = statsLogger;
    }

    @Override
    public void recordMessageReceived(
            long payloadLength, long endToEndLatencyMicros, String experimentId, String messageId, boolean isTpcH)
            throws IOException {
        MonitoredReceivedMessage message = new MonitoredReceivedMessage(
            payloadLength,
            endToEndLatencyMicros,
            experimentId != null ? experimentId : "UNAVAILABLE",
            messageId,
            isTpcH
        );
        String body = writer.writeValueAsString(message);
        SendMessageRequest request = new SendMessageRequest(EnvironmentConfiguration.getMonitoringSqsUri(), body);
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
            boolean isError
    ) throws IOException {
        MonitoredProducedMessage message = new MonitoredProducedMessage(
                payloadLength,
                intendedSendTimeNs,
                sendTimeNs,
                nowNs,
                experimentId != null ? experimentId : "UNAVAILABLE",
                messageId,
                isTpcH,
                isError
        );
        String body = writer.writeValueAsString(message);
        SendMessageRequest request = new SendMessageRequest(EnvironmentConfiguration.getMonitoringSqsUri(), body);
        sqsClient.sendMessage(request);
    }

    @Override
    public StatsLogger getStatsLogger() {
        return statsLogger;
    }

    @Override
    public void recordMessageSent() {}

    @Override
    public PeriodStats toPeriodStats() {
        return periodStats;
    }

    @Override
    public CumulativeLatencies toCumulativeLatencies() {
        return cumulativeLatencies;
    }

    @Override
    public CountersStats toCountersStats() {
        return countersStats;
    }

    @Override
    public void resetLatencies() {}

    @Override
    public void reset() {}

    private static final ObjectWriter writer = ObjectMappers.writer;
}
