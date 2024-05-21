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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.HdrHistogram.Recorder;
import org.apache.bookkeeper.stats.Counter;
import org.apache.bookkeeper.stats.OpStatsLogger;
import org.apache.bookkeeper.stats.Stats;
import org.apache.bookkeeper.stats.StatsLogger;
import org.apache.bookkeeper.stats.StatsProvider;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceWorkerStats implements WorkerStats {

    protected final OpStatsLogger publishDelayLatencyStats;

    protected final Recorder endToEndLatencyRecorder = new Recorder(TimeUnit.HOURS.toMicros(12), 5);
    protected final Recorder endToEndCumulativeLatencyRecorder =
            new Recorder(TimeUnit.HOURS.toMicros(12), 5);
    protected final OpStatsLogger endToEndLatencyStats;

    protected final LongAdder messagesSent = new LongAdder();
    protected final LongAdder messageSendErrors = new LongAdder();
    protected final LongAdder bytesSent = new LongAdder();
    protected final Counter messageSendErrorCounter;
    protected final Counter messagesSentCounter;
    protected final Counter bytesSentCounter;

    protected final LongAdder messagesReceived = new LongAdder();
    protected final LongAdder bytesReceived = new LongAdder();
    protected final Counter messagesReceivedCounter;
    protected final Counter bytesReceivedCounter;

    protected final LongAdder totalMessagesSent = new LongAdder();
    protected final LongAdder totalMessageSendErrors = new LongAdder();
    protected final LongAdder totalMessagesReceived = new LongAdder();

    protected static final long highestTrackableValue = TimeUnit.SECONDS.toMicros(60);
    protected final Recorder publishLatencyRecorder = new Recorder(highestTrackableValue, 5);
    protected final Recorder cumulativePublishLatencyRecorder =
            new Recorder(highestTrackableValue, 5);
    protected final OpStatsLogger publishLatencyStats;

    protected final Recorder publishDelayLatencyRecorder = new Recorder(highestTrackableValue, 5);
    protected final Recorder cumulativePublishDelayLatencyRecorder =
            new Recorder(highestTrackableValue, 5);

    private static final ObjectWriter writer = new ObjectMapper().writer();

    public InstanceWorkerStats() {
        Configuration conf = new CompositeConfiguration();
        Stats.loadStatsProvider(conf);
        StatsProvider provider = Stats.get();
        provider.start(conf);
        StatsLogger statsLogger = provider.getStatsLogger("benchmark");

        StatsLogger producerStatsLogger = statsLogger.scope("producer");
        this.messagesSentCounter = producerStatsLogger.getCounter("messages_sent");
        this.messageSendErrorCounter = producerStatsLogger.getCounter("message_send_errors");
        this.bytesSentCounter = producerStatsLogger.getCounter("bytes_sent");
        this.publishDelayLatencyStats = producerStatsLogger.getOpStatsLogger("producer_delay_latency");
        this.publishLatencyStats = producerStatsLogger.getOpStatsLogger("produce_latency");

        StatsLogger consumerStatsLogger = statsLogger.scope("consumer");
        this.messagesReceivedCounter = consumerStatsLogger.getCounter("messages_recv");
        this.bytesReceivedCounter = consumerStatsLogger.getCounter("bytes_recv");
        this.endToEndLatencyStats = consumerStatsLogger.getOpStatsLogger("e2e_latency");
        log.info("Instance worker stats initialized.");
    }

    public InstanceWorkerStats(StatsLogger statsLogger) {
        StatsLogger producerStatsLogger = statsLogger.scope("producer");
        this.messagesSentCounter = producerStatsLogger.getCounter("messages_sent");
        this.messageSendErrorCounter = producerStatsLogger.getCounter("message_send_errors");
        this.bytesSentCounter = producerStatsLogger.getCounter("bytes_sent");
        this.publishDelayLatencyStats = producerStatsLogger.getOpStatsLogger("producer_delay_latency");
        this.publishLatencyStats = producerStatsLogger.getOpStatsLogger("produce_latency");

        StatsLogger consumerStatsLogger = statsLogger.scope("consumer");
        this.messagesReceivedCounter = consumerStatsLogger.getCounter("messages_recv");
        this.bytesReceivedCounter = consumerStatsLogger.getCounter("bytes_recv");
        this.endToEndLatencyStats = consumerStatsLogger.getOpStatsLogger("e2e_latency");
        log.info("Instance worker stats initialized.");
    }

    public void recordMessageReceived(
            long payloadLength,
            long endToEndLatencyMicros,
            String experimentId,
            String messageId,
            boolean isTpcH) {
        messagesReceived.increment();
        totalMessagesReceived.increment();
        messagesReceivedCounter.inc();
        bytesReceived.add(payloadLength);
        bytesReceivedCounter.add(payloadLength);

        if (endToEndLatencyMicros > 0) {
            endToEndCumulativeLatencyRecorder.recordValue(endToEndLatencyMicros);
            endToEndLatencyRecorder.recordValue(endToEndLatencyMicros);
            endToEndLatencyStats.registerSuccessfulEvent(endToEndLatencyMicros, TimeUnit.MICROSECONDS);
        }
    }

    public void recordMessageProduced(
            long payloadLength,
            long intendedSendTimeNs,
            long sendTimeNs,
            long nowNs,
            String experimentId,
            String messageId,
            boolean isTpcH,
            boolean isError) {
        if (!isError) {
            messagesSent.increment();
            totalMessagesSent.increment();
            messagesSentCounter.inc();
            bytesSent.add(payloadLength);
            bytesSentCounter.add(payloadLength);

            final long latencyMicros =
                    Math.min(highestTrackableValue, TimeUnit.NANOSECONDS.toMicros(nowNs - sendTimeNs));
            publishLatencyRecorder.recordValue(latencyMicros);
            cumulativePublishLatencyRecorder.recordValue(latencyMicros);
            publishLatencyStats.registerSuccessfulEvent(latencyMicros, TimeUnit.MICROSECONDS);

            final long sendDelayMicros =
                    Math.min(
                            highestTrackableValue,
                            TimeUnit.NANOSECONDS.toMicros(sendTimeNs - intendedSendTimeNs));
            publishDelayLatencyRecorder.recordValue(sendDelayMicros);
            cumulativePublishDelayLatencyRecorder.recordValue(sendDelayMicros);
            publishDelayLatencyStats.registerSuccessfulEvent(sendDelayMicros, TimeUnit.MICROSECONDS);
        } else {
            messageSendErrors.increment();
            messageSendErrorCounter.inc();
            totalMessageSendErrors.increment();
        }
    }

    public void recordMessageSent() {
        totalMessagesSent.increment();
    }

    public PeriodStats toPeriodStats() {
        PeriodStats stats = new PeriodStats();

        stats.messagesSent = messagesSent.sumThenReset();
        stats.messageSendErrors = messageSendErrors.sumThenReset();
        stats.bytesSent = bytesSent.sumThenReset();

        stats.messagesReceived = messagesReceived.sumThenReset();
        stats.bytesReceived = bytesReceived.sumThenReset();

        stats.totalMessagesSent = totalMessagesSent.sum();
        stats.totalMessageSendErrors = totalMessageSendErrors.sum();
        stats.totalMessagesReceived = totalMessagesReceived.sum();

        stats.publishLatency = publishLatencyRecorder.getIntervalHistogram();
        stats.publishDelayLatency = publishDelayLatencyRecorder.getIntervalHistogram();
        stats.endToEndLatency = endToEndLatencyRecorder.getIntervalHistogram();
        return stats;
    }

    public CumulativeLatencies toCumulativeLatencies() {
        CumulativeLatencies latencies = new CumulativeLatencies();
        latencies.publishLatency = cumulativePublishLatencyRecorder.getIntervalHistogram();
        latencies.publishDelayLatency = cumulativePublishDelayLatencyRecorder.getIntervalHistogram();
        latencies.endToEndLatency = endToEndCumulativeLatencyRecorder.getIntervalHistogram();
        return latencies;
    }

    public CountersStats toCountersStats() throws IOException {
        CountersStats stats = new CountersStats();
        stats.messagesSent = totalMessagesSent.sum();
        stats.messageSendErrors = totalMessageSendErrors.sum();
        stats.messagesReceived = totalMessagesReceived.sum();
        return stats;
    }

    public void resetLatencies() {
        publishLatencyRecorder.reset();
        cumulativePublishLatencyRecorder.reset();
        publishDelayLatencyRecorder.reset();
        cumulativePublishDelayLatencyRecorder.reset();
        endToEndLatencyRecorder.reset();
        endToEndCumulativeLatencyRecorder.reset();
    }

    public void reset() {
        resetLatencies();

        messagesSent.reset();
        messagesSent.reset();
        messageSendErrors.reset();
        bytesSent.reset();
        messagesReceived.reset();
        bytesReceived.reset();
        totalMessagesSent.reset();
        totalMessagesReceived.reset();
    }

    private static final Logger log = LoggerFactory.getLogger(InstanceWorkerStats.class);
}
