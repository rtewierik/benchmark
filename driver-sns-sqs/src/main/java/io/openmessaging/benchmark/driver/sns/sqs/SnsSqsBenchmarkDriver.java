package io.openmessaging.benchmark.driver.sns.sqs;

import io.openmessaging.benchmark.driver.BenchmarkConsumer;
import io.openmessaging.benchmark.driver.BenchmarkDriver;
import io.openmessaging.benchmark.driver.BenchmarkProducer;
import io.openmessaging.benchmark.driver.ConsumerCallback;
import io.openmessaging.benchmark.driver.TpcHInfo;
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
    public CompletableFuture<BenchmarkConsumer> createConsumer(String topic, String subscriptionName, ConsumerCallback consumerCallback, TpcHInfo info) {
        // Consumers are pre-created by AWS CDK project.
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() throws Exception {}

    private static final Logger log = LoggerFactory.getLogger(SnsSqsBenchmarkDriver.class);
}
