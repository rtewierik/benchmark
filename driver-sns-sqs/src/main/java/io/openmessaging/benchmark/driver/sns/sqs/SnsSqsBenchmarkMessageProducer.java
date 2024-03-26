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
package io.openmessaging.benchmark.driver.sns.sqs;

import io.openmessaging.benchmark.common.utils.UniformRateLimiter;
import io.openmessaging.benchmark.driver.BenchmarkProducer;
import io.openmessaging.benchmark.driver.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Supplier;

import static io.openmessaging.benchmark.common.utils.UniformRateLimiter.uninterruptibleSleepNs;

public class SnsSqsBenchmarkMessageProducer implements MessageProducer {
    private final UniformRateLimiter rateLimiter;
    private Supplier<Long> nanoClock;

    SnsSqsBenchmarkMessageProducer(UniformRateLimiter rateLimiter) {
        this(System::nanoTime, rateLimiter);
    }

    SnsSqsBenchmarkMessageProducer(Supplier<Long> nanoClock, UniformRateLimiter rateLimiter) {
        this.nanoClock = nanoClock;
        this.rateLimiter = rateLimiter;
    }

    public void sendMessage(BenchmarkProducer producer, Optional<String> key, byte[] payload) {
        final long intendedSendTime = rateLimiter.acquire();
        uninterruptibleSleepNs(intendedSendTime);
        // final long sendTime = nanoClock.get();
        producer
            .sendAsync(key, payload)
            .exceptionally(this::failure);
    }

    private void success(long payloadLength, long intendedSendTime, long sendTime) {
        // TO DO: Implement 'recordProducerSuccess'.
    }

    private Void failure(Throwable t) {
        // TO DO: Implement 'recordProducerFailure'.
        log.warn("Write error on message", t);
        return null;
    }

    private static final Logger log = LoggerFactory.getLogger(SnsSqsBenchmarkMessageProducer.class);
}
