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
package io.openmessaging.benchmark.common.producer;

import static io.openmessaging.benchmark.common.utils.UniformRateLimiter.uninterruptibleSleepNs;

import io.openmessaging.benchmark.common.EnvironmentConfiguration;
import io.openmessaging.benchmark.common.monitoring.WorkerStats;
import io.openmessaging.benchmark.common.utils.UniformRateLimiter;
import io.openmessaging.benchmark.driver.BenchmarkProducer;
import io.openmessaging.benchmark.driver.MessageProducer;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageProducerImpl implements MessageProducer {

    private final WorkerStats stats;
    private final UniformRateLimiter rateLimiter;
    private final Supplier<Long> nanoClock;

    public MessageProducerImpl(UniformRateLimiter rateLimiter, WorkerStats stats) {
        this(System::nanoTime, rateLimiter, stats);
    }

    MessageProducerImpl(Supplier<Long> nanoClock, UniformRateLimiter rateLimiter, WorkerStats stats) {
        this.nanoClock = nanoClock;
        this.rateLimiter = rateLimiter;
        this.stats = stats;
    }

    public CompletableFuture<Void> sendMessage(
            BenchmarkProducer producer,
            Optional<String> key,
            byte[] payload,
            String experimentId,
            String messageId,
            boolean isTpcH) {
        boolean isDebug = EnvironmentConfiguration.isDebug();
        final long intendedSendTime = rateLimiter.acquire();
        if (isDebug) {
            long waitNs = intendedSendTime - System.nanoTime();
            log.info(
                    "Rate limiter suggested sleep for {} ns with {}} ops/sec.",
                    waitNs,
                    rateLimiter.getOpsPerSec());
        }
        uninterruptibleSleepNs(intendedSendTime);
        final long sendTime = nanoClock.get();
        long length = payload.length;
        if (isDebug) {
            log.info("Sending message in producer implementation.");
        }
        return producer
                .sendAsync(key, payload)
                .thenRun(
                        () -> {
                            if (isDebug) {
                                log.info("Trying to record result.");
                            }
                            try {
                                recordResult(
                                        length, intendedSendTime, sendTime, experimentId, messageId, isTpcH, null);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                .exceptionally(
                        (t) -> {
                            if (isDebug) {
                                log.info("Trying to record exception result.", t);
                            }
                            try {
                                return recordResult(
                                        length, intendedSendTime, sendTime, experimentId, messageId, isTpcH, t);
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
            Throwable t)
            throws IOException {
        long nowNs = nanoClock.get();
        long timestamp = new Date().getTime();
        boolean isError = t != null;
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Recording result in message producer with stats {} and t {}", stats, t);
        }
        if (stats != null) {
            stats.recordMessageProduced(
                    payloadLength,
                    intendedSendTime,
                    sendTime,
                    nowNs,
                    timestamp,
                    experimentId,
                    messageId,
                    isTpcH,
                    isError);
        }
        if (isError) {
            log.warn("Write error on message", t);
        }
        return null;
    }

    private static final Logger log = LoggerFactory.getLogger(MessageProducerImpl.class);
}
