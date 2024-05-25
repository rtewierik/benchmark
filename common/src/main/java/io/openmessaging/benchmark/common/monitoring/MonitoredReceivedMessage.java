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


import java.util.UUID;

public class MonitoredReceivedMessage {
    public final String transactionId;
    public final long payloadLength;
    public final long endToEndLatencyMicros;
    public final long publishTimestamp;
    public final long processTimestamp;
    public final String experimentId;
    public final String messageId;
    public final boolean isTpcH;

    public MonitoredReceivedMessage(
            long payloadLength,
            long endToEndLatencyMicros,
            long publishTimestamp,
            long processTimestamp,
            String experimentId,
            String messageId,
            boolean isTpcH) {
        this.transactionId = UUID.randomUUID().toString();
        this.payloadLength = payloadLength;
        this.endToEndLatencyMicros = endToEndLatencyMicros;
        this.publishTimestamp = publishTimestamp;
        this.processTimestamp = processTimestamp;
        this.experimentId = experimentId;
        this.messageId = messageId;
        this.isTpcH = isTpcH;
    }
}
