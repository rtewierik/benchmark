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


import java.io.IOException;

public interface WorkerStats {
    void recordMessageReceived(
            long payloadLength,
            long endToEndLatencyMicros,
            long publishTimestamp,
            long processTimestamp,
            String experimentId,
            String messageId,
            boolean isTpcH)
            throws IOException;

    @SuppressWarnings("checkstyle:ParameterNumber")
    void recordMessageProduced(
            long payloadLength,
            long intendedSendTimeNs,
            long sendTimeNs,
            long nowNs,
            long timestamp,
            String experimentId,
            String messageId,
            boolean isTpcH,
            boolean isError)
            throws IOException;

    void recordMessageSent();

    PeriodStats toPeriodStats();

    CumulativeLatencies toCumulativeLatencies();

    CountersStats toCountersStats() throws IOException;

    void resetLatencies();

    void reset();
}
