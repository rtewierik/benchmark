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

import static io.openmessaging.benchmark.common.utils.UniformRateLimiter.uninterruptibleSleepNs;

import io.openmessaging.benchmark.common.utils.UniformRateLimiter;
import io.openmessaging.benchmark.driver.BenchmarkProducer;
import io.openmessaging.benchmark.driver.MessageProducer;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageProducerImpl implements MessageProducer {

    private final WorkerStats stats;
    private UniformRateLimiter rateLimiter;
    private Supplier<Long> nanoClock;

    MessageProducerImpl(UniformRateLimiter rateLimiter, WorkerStats stats) {
        this(System::nanoTime, rateLimiter, stats);
    }

    MessageProducerImpl(Supplier<Long> nanoClock, UniformRateLimiter rateLimiter, WorkerStats stats) {
        this.nanoClock = nanoClock;
        this.rateLimiter = rateLimiter;
        this.stats = stats;
    }

    public void sendMessage(BenchmarkProducer producer, Optional<String> key, byte[] payload) {
        final long intendedSendTime = rateLimiter.acquire();
        uninterruptibleSleepNs(intendedSendTime);
        final long sendTime = nanoClock.get();
        producer
                .sendAsync(key, payload)
                .thenRun(() -> success(payload.length, intendedSendTime, sendTime))
                .exceptionally(this::failure);
    }

    private void success(long payloadLength, long intendedSendTime, long sendTime) {
        long nowNs = nanoClock.get();
        if (stats != null) {
            stats.recordProducerSuccess(payloadLength, intendedSendTime, sendTime, nowNs);
        }
    }

    private Void failure(Throwable t) {
        if (stats != null) {
            stats.recordProducerFailure();
        }
        log.warn("Write error on message", t);
        return null;
    }

    private static final Logger log = LoggerFactory.getLogger(MessageProducerImpl.class);
}
