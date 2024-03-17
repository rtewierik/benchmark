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

import io.openmessaging.benchmark.driver.BenchmarkConsumer;
import io.openmessaging.benchmark.driver.BenchmarkDriver;
import io.openmessaging.benchmark.driver.BenchmarkProducer;
import io.openmessaging.benchmark.driver.ConsumerCallback;
import org.apache.bookkeeper.stats.StatsLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class SnsSqsBenchmarkDriver implements BenchmarkDriver {

    @Override
    public void initialize(File configurationFile, StatsLogger statsLogger) throws IOException, InterruptedException {}

    @Override
    public String getTopicNamePrefix() {
        // Topics are pre-created by AWS CDK project.
        return "";
    }

    @Override
    public CompletableFuture<Void> createTopic(String topic, int partitions) {
        // Topics are pre-created by AWS CDK project.
        return CompletableFuture.runAsync(() -> {});
    }

    @Override
    public CompletableFuture<BenchmarkProducer> createProducer(String topic) {
        return CompletableFuture.completedFuture(new SnsSqsBenchmarkProducer());
    }

    @Override
    public CompletableFuture<BenchmarkConsumer> createConsumer(String topic, String subscriptionName, ConsumerCallback consumerCallback) {
        // Consumers are pre-created by AWS CDK project.
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() throws Exception {}

    private static final Logger log = LoggerFactory.getLogger(SnsSqsBenchmarkDriver.class);
}
