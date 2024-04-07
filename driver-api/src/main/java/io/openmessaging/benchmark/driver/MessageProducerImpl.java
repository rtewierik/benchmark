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
package io.openmessaging.benchmark.driver;

import static io.openmessaging.benchmark.common.utils.UniformRateLimiter.uninterruptibleSleepNs;

import io.openmessaging.benchmark.common.utils.UniformRateLimiter;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import io.openmessaging.benchmark.driver.monitoring.WorkerStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageProducerImpl implements MessageProducer {

    private final WorkerStats stats;
    private UniformRateLimiter rateLimiter;
    private Supplier<Long> nanoClock;

    public MessageProducerImpl(UniformRateLimiter rateLimiter, WorkerStats stats) {
        this(System::nanoTime, rateLimiter, stats);
    }

    MessageProducerImpl(Supplier<Long> nanoClock, UniformRateLimiter rateLimiter, WorkerStats stats) {
        this.nanoClock = nanoClock;
        this.rateLimiter = rateLimiter;
        this.stats = stats;
    }

    public void sendMessage(
        BenchmarkProducer producer,
        Optional<String> key,
        byte[] payload,
        String experimentId,
        String messageId,
        boolean isTpcH
    ) {
        final long intendedSendTime = rateLimiter.acquire();
        uninterruptibleSleepNs(intendedSendTime);
        final long sendTime = nanoClock.get();
        long length = payload.length;
        producer
                .sendAsync(key, payload)
                .thenRun(() -> {
                    try {
                        recordResult(length, intendedSendTime, sendTime, experimentId, messageId, isTpcH, null);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .exceptionally((t) -> {
                    try {
                        return recordResult(length, intendedSendTime, sendTime, experimentId, messageId, isTpcH, t);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private Void recordResult(
        long payloadLength,
        long intendedSendTime,
        long sendTime,
        String experimentId,
        String messageId,
        boolean isTpcH,
        Throwable t
    ) throws IOException {
        long nowNs = nanoClock.get();
        boolean isError = t != null;
        if (stats != null) {
            stats.recordMessageProduced(
                    payloadLength, intendedSendTime, sendTime, nowNs, experimentId, messageId, isTpcH, isError);
        }
        if (isError) {
            log.warn("Write error on message", t);
        }
        return null;
    }
    private static final Logger log = LoggerFactory.getLogger(MessageProducerImpl.class);
}
