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
package io.openmessaging.benchmark.worker;


import io.openmessaging.benchmark.common.monitoring.InstanceWorkerStats;
import io.openmessaging.benchmark.worker.commands.CountersStats;
import io.openmessaging.benchmark.worker.commands.CumulativeLatencies;
import io.openmessaging.benchmark.worker.commands.PeriodStats;
import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;

import org.apache.bookkeeper.stats.Counter;
import org.apache.bookkeeper.stats.StatsLogger;

public class WorkerStats extends InstanceWorkerStats {

    private final LongAdder messageSendErrors = new LongAdder();
    private final Counter messageSendErrorCounter;
    private final LongAdder totalMessageSendErrors = new LongAdder();

    WorkerStats(StatsLogger statsLogger) {
        super(statsLogger);
        StatsLogger producerStatsLogger = statsLogger.scope("producer");
        this.messageSendErrorCounter = producerStatsLogger.getCounter("message_send_errors");
    }

    public StatsLogger getStatsLogger() {
        return statsLogger;
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

    public void recordProducerFailure() {
        messageSendErrors.increment();
        messageSendErrorCounter.inc();
        totalMessageSendErrors.increment();
    }
}
