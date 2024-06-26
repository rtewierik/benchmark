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

public class MonitoredProducedMessage {
    public final String transactionId;
    public final long payloadLength;
    public final long intendedSendTimeNs;
    public final long sendTimeNs;
    public final long nowNs;
    public final long timestamp;
    public final String experimentId;
    public final String messageId;
    public final boolean isTpcH;
    public final boolean isError;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public MonitoredProducedMessage(
            long payloadLength,
            long intendedSendTimeNs,
            long sendTimeNs,
            long nowNs,
            long timestamp,
            String experimentId,
            String messageId,
            boolean isTpcH,
            boolean isError) {
        this.transactionId = UUID.randomUUID().toString();
        this.payloadLength = payloadLength;
        this.intendedSendTimeNs = intendedSendTimeNs;
        this.sendTimeNs = sendTimeNs;
        this.nowNs = nowNs;
        this.timestamp = timestamp;
        this.experimentId = experimentId;
        this.messageId = messageId;
        this.isTpcH = isTpcH;
        this.isError = isError;
    }
}
