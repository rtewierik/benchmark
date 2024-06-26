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
package io.openmessaging.benchmark;

import static io.openmessaging.benchmark.common.random.RandomUtils.RANDOM;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.openmessaging.benchmark.common.EnvironmentConfiguration;
import io.openmessaging.benchmark.common.monitoring.CountersStats;
import io.openmessaging.benchmark.common.monitoring.CumulativeLatencies;
import io.openmessaging.benchmark.common.monitoring.PeriodStats;
import io.openmessaging.benchmark.common.utils.RandomGenerator;
import io.openmessaging.benchmark.utils.PaddingDecimalFormat;
import io.openmessaging.benchmark.utils.Timer;
import io.openmessaging.benchmark.utils.payload.FilePayloadReader;
import io.openmessaging.benchmark.utils.payload.PayloadReader;
import io.openmessaging.benchmark.worker.BenchmarkWorkers;
import io.openmessaging.benchmark.worker.LocalWorker;
import io.openmessaging.benchmark.worker.Worker;
import io.openmessaging.benchmark.worker.commands.ConsumerAssignment;
import io.openmessaging.benchmark.worker.commands.ProducerAssignment;
import io.openmessaging.benchmark.worker.commands.ProducerWorkAssignment;
import io.openmessaging.benchmark.worker.commands.TopicSubscription;
import io.openmessaging.benchmark.worker.commands.TopicsInfo;
import io.openmessaging.tpch.TpcHConstants;
import io.openmessaging.tpch.model.TpcHArguments;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkloadGenerator implements AutoCloseable {

    private static final ThreadLocal<DateFormat> DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss"));

    private final String driverName;
    private final Workload workload;
    private final TpcHArguments arguments;
    private final Worker worker;
    private final LocalWorker localWorker;
    private final String experimentId;

    private final ExecutorService executor =
            Executors.newCachedThreadPool(new DefaultThreadFactory("messaging-benchmark"));

    private volatile boolean runCompleted = false;
    private volatile boolean needToWaitForBacklogDraining = false;

    private volatile double targetPublishRate;

    public WorkloadGenerator(
            String driverName, Workload workload, TpcHArguments arguments, BenchmarkWorkers workers) {
        this.driverName = driverName;
        this.workload = workload;
        this.arguments = arguments != null ? arguments.withQueryIdDate() : null;
        this.worker = workers.worker;
        this.localWorker = workers.localWorker;
        String date = DATE_FORMAT.get().format(new Date());
        String workloadName = this.workload.name;
        this.experimentId =
                arguments != null
                        ? String.format("%s-%s-%s", this.driverName, workloadName, this.arguments.queryId)
                        : String.format("%s-%s-%s", this.driverName, workloadName, date);

        if (workload.consumerBacklogSizeGB > 0 && workload.producerRate == 0) {
            throw new IllegalArgumentException(
                    "Cannot probe producer sustainable rate when building backlog");
        }
    }

    public TestResult run() throws Exception {
        if (this.arguments != null) {
            return runTpcH();
        }
        return runWorkload();
    }

    private TestResult runTpcH() throws Exception {
        Timer timer = new Timer();
        /*
         * 3 topics for Map commands, one per worker acting as both producer and consumer during TPC-H experiment;
         * 1 topic to send aggregated intermediate results to.
         * x topics to send intermediate results to;
         */
        int numberOfTopics = 3 + 1 + this.arguments.numberOfWorkers;
        List<String> topics =
                worker.createTopics(new TopicsInfo(numberOfTopics, workload.partitionsPerTopic));
        log.info("Created {} topics in {} ms", topics.size(), timer.elapsedMillis());

        ConsumerAssignment internalConsumerAssignment = createTpcHConsumers(topics);
        if (EnvironmentConfiguration.isDebug()) {
            log.info(
                    "Internal consumer assignment: {}",
                    writer.writeValueAsString(internalConsumerAssignment));
        }
        this.localWorker.createConsumers(internalConsumerAssignment);
        if (EnvironmentConfiguration.isDebug()) {
            log.info(
                    "Created {} internal consumers in {} ms",
                    internalConsumerAssignment.topicsSubscriptions.size(),
                    timer.elapsedMillis());
        }

        createTpcHProducers(topics);

        ensureTopicsAreReady();

        if (workload.producerRate > 0) {
            targetPublishRate = workload.producerRate;
        } else {
            // Producer rate is 0 and we need to discover the sustainable rate
            // ADDRESS: TPC-H queries do not support finding max sustainable rate.
            targetPublishRate = 10000;
        }

        ProducerWorkAssignment producerWorkAssignment = new ProducerWorkAssignment();
        producerWorkAssignment.keyDistributorType = workload.keyDistributor;
        producerWorkAssignment.publishRate = targetPublishRate;
        producerWorkAssignment.payloadData = new ArrayList<>();
        producerWorkAssignment.tpcHArguments = this.arguments;

        log.info(
                "[BenchmarkStart] Starting benchmark {} at {}", this.experimentId, new Date().getTime());
        worker.startLoad(producerWorkAssignment);

        TestResult result =
                printAndCollectStats(
                        workload.testDurationMinutes, TimeUnit.MINUTES, localWorker::getTestCompleted);
        runCompleted = true;

        log.info("[BenchmarkEnd] Ending benchmark {} at {}", this.experimentId, new Date().getTime());

        return result;
    }

    private TestResult runWorkload() throws Exception {
        Timer timer = new Timer();
        List<String> topics =
                worker.createTopics(new TopicsInfo(workload.topics, workload.partitionsPerTopic));
        log.info("Created {} topics in {} ms", topics.size(), timer.elapsedMillis());

        createConsumers(topics);
        createProducers(topics);

        ensureTopicsAreReady();

        if (workload.producerRate > 0) {
            targetPublishRate = workload.producerRate;
        } else {
            // Producer rate is 0 and we need to discover the sustainable rate
            targetPublishRate = 2000000;

            if (!EnvironmentConfiguration.isCloudMonitoringEnabled()) {
                executor.execute(
                        () -> {
                            // Run background controller to adjust rate
                            try {
                                findMaximumSustainableRate(targetPublishRate);
                            } catch (IOException e) {
                                log.warn("Failure in finding max sustainable rate", e);
                            }
                        });
            }
        }

        final PayloadReader payloadReader = new FilePayloadReader(workload.messageSize);

        ProducerWorkAssignment producerWorkAssignment = new ProducerWorkAssignment();
        producerWorkAssignment.keyDistributorType = workload.keyDistributor;
        producerWorkAssignment.publishRate = targetPublishRate;
        producerWorkAssignment.payloadData = new ArrayList<>();

        if (workload.useRandomizedPayloads) {
            // create messages that are part random and part zeros
            // better for testing effects of compression
            int randomBytes = (int) (workload.messageSize * workload.randomBytesRatio);
            int zerodBytes = workload.messageSize - randomBytes;
            for (int i = 0; i < workload.randomizedPayloadPoolSize; i++) {
                byte[] randArray = new byte[randomBytes];
                RANDOM.nextBytes(randArray);
                byte[] zerodArray = new byte[zerodBytes];
                byte[] combined = ArrayUtils.addAll(randArray, zerodArray);
                producerWorkAssignment.payloadData.add(combined);
            }
        } else {
            producerWorkAssignment.payloadData.add(payloadReader.load(workload.payloadFile));
        }

        worker.startLoad(producerWorkAssignment);

        if (workload.warmupDurationMinutes > 0) {
            log.info("----- Starting warm-up traffic ({}m) ------", workload.warmupDurationMinutes);
            printAndCollectStats(workload.warmupDurationMinutes, TimeUnit.MINUTES, () -> false);
        }

        if (workload.consumerBacklogSizeGB > 0) {
            executor.execute(
                    () -> {
                        try {
                            buildAndDrainBacklog(workload.testDurationMinutes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }

        worker.resetStats();
        log.info("----- Starting benchmark traffic ({}m)------", workload.testDurationMinutes);

        TestResult result =
                printAndCollectStats(workload.testDurationMinutes, TimeUnit.MINUTES, () -> false);
        runCompleted = true;

        log.info("----- Completed run. Stopping worker and yielding results ------");

        worker.stopAll();
        if (localWorker != worker) {
            localWorker.stopAll();
        }
        return result;
    }

    private void ensureTopicsAreReady() throws IOException {
        if (EnvironmentConfiguration.isProduceWithAllWorkers()) {
            return;
        }
        log.info("Waiting for consumers to be ready...");
        // This is work around the fact that there's no way to have a consumer ready in Kafka without
        // first publishing
        // some message on the topic, which will then trigger the partitions assignment to the consumers

        int expectedMessages = workload.topics * workload.subscriptionsPerTopic;

        // In this case we just publish 1 message and then wait for consumers to receive the data
        worker.probeProducers();

        long start = System.currentTimeMillis();
        long end = start + 60 * 1000;
        while (System.currentTimeMillis() < end) {
            CountersStats stats = worker.getCountersStats();

            log.info(
                    "Waiting for topics to be ready -- Sent: {}, Received: {}, Expected: {}",
                    stats.messagesSent,
                    stats.messagesReceived,
                    expectedMessages);
            if (stats.messagesReceived < expectedMessages) {
                try {
                    Thread.sleep(2_000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                break;
            }
        }

        if (System.currentTimeMillis() >= end) {
            throw new RuntimeException("Timed out waiting for consumers to be ready");
        } else {
            log.info("All consumers are ready!");
        }
    }

    /**
     * Adjust the publish rate to a level that is sustainable, meaning that we can consume all the
     * messages that are being produced.
     *
     * @param currentRate
     */
    private void findMaximumSustainableRate(double currentRate) throws IOException {
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Finding maximum sustainable rate...");
        }
        CountersStats stats = worker.getCountersStats();

        int controlPeriodMillis = 3000;
        long lastControlTimestamp = System.nanoTime();

        RateController rateController = new RateController();

        while (!runCompleted) {
            // Check every few seconds and adjust the rate
            try {
                Thread.sleep(controlPeriodMillis);
            } catch (InterruptedException e) {
                return;
            }

            // Consider multiple copies when using multiple subscriptions
            stats = worker.getCountersStats();
            long currentTime = System.nanoTime();
            long periodNanos = currentTime - lastControlTimestamp;

            lastControlTimestamp = currentTime;

            currentRate =
                    rateController.nextRate(
                            currentRate, periodNanos, stats.messagesSent, stats.messagesReceived);
            if (EnvironmentConfiguration.isDebug()) {
                log.info("Adjusting publish rate to sustainable rate {}", currentRate);
            }
            worker.adjustPublishRate(currentRate);
        }
    }

    @Override
    public void close() throws Exception {
        try {
            worker.stopAll();
            if (localWorker != worker) {
                localWorker.stopAll();
            }
        } catch (Throwable t) {
            log.error("Exception occurred while stopping all workers.", t);
        }
        executor.shutdownNow();
    }

    private void createConsumers(List<String> topics) throws IOException {
        ConsumerAssignment consumerAssignment = new ConsumerAssignment(this.experimentId);

        for (String topic : topics) {
            for (int i = 0; i < workload.subscriptionsPerTopic; i++) {
                String subscriptionName = generateSubscriptionName(i);
                for (int j = 0; j < workload.consumerPerSubscription; j++) {
                    consumerAssignment.topicsSubscriptions.add(
                            new TopicSubscription(topic, subscriptionName));
                }
            }
        }

        Collections.shuffle(consumerAssignment.topicsSubscriptions);

        Timer timer = new Timer();

        worker.createConsumers(consumerAssignment);
        log.info(
                "Created {} consumers in {} ms",
                consumerAssignment.topicsSubscriptions.size(),
                timer.elapsedMillis());
    }

    private void addMapSubscription(
            ConsumerAssignment consumerAssignment, List<String> topics, int offset) {
        int index = TpcHConstants.MAP_CMD_START_INDEX + offset;
        consumerAssignment.topicsSubscriptions.add(
                new TopicSubscription(topics.get(index), generateSubscriptionName(index)));
    }

    private ConsumerAssignment createTpcHConsumers(List<String> topics) throws IOException {
        ConsumerAssignment consumerAssignment = new ConsumerAssignment(experimentId, true);
        ConsumerAssignment orchestratorConsumerAssignment = new ConsumerAssignment(experimentId, true);

        TopicSubscription orchestratorSubscription =
                new TopicSubscription(
                        topics.get(TpcHConstants.REDUCE_DST_INDEX),
                        generateSubscriptionName(TpcHConstants.REDUCE_DST_INDEX));

        consumerAssignment.topicsSubscriptions.add(orchestratorSubscription);
        orchestratorConsumerAssignment.topicsSubscriptions.add(orchestratorSubscription);

        addMapSubscription(consumerAssignment, topics, 0);
        addMapSubscription(consumerAssignment, topics, 1);
        addMapSubscription(consumerAssignment, topics, 2);

        for (int i = 0; i < this.arguments.numberOfWorkers; i++) {
            int sourceIndex = TpcHConstants.REDUCE_SRC_START_INDEX + i;
            consumerAssignment.topicsSubscriptions.add(
                    new TopicSubscription(topics.get(sourceIndex), generateSubscriptionName(sourceIndex)));
        }

        if (EnvironmentConfiguration.isDebug()) {
            log.info(
                    "Creating the following consumers: {}", writer.writeValueAsString(consumerAssignment));
        }
        Timer timer = new Timer();
        worker.createConsumers(consumerAssignment);
        log.info(
                "Created {} external consumers in {} ms",
                consumerAssignment.topicsSubscriptions.size(),
                timer.elapsedMillis());

        return orchestratorConsumerAssignment;
    }

    private void createProducers(List<String> topics) throws IOException {
        ProducerAssignment producerAssignment = new ProducerAssignment();

        // Add the topic multiple times, one for each producer
        for (int i = 0; i < workload.producersPerTopic; i++) {
            producerAssignment.topics.addAll(topics);
        }

        Collections.shuffle(producerAssignment.topics);

        Timer timer = new Timer();

        worker.createProducers(producerAssignment);
        log.info(
                "Created {} producers in {} ms", producerAssignment.topics.size(), timer.elapsedMillis());
    }

    private void createTpcHProducers(List<String> topics) throws IOException {
        ProducerAssignment producerAssignment = new ProducerAssignment();
        producerAssignment.topics.addAll(topics);
        producerAssignment.isTpcH = true;
        Timer timer = new Timer();
        worker.createProducers(producerAssignment);
        log.info("Created {} producers in {} ms", topics.size(), timer.elapsedMillis());
    }

    private void buildAndDrainBacklog(int testDurationMinutes) throws IOException {
        Timer timer = new Timer();
        log.info("Stopping all consumers to build backlog...");
        worker.pauseConsumers();

        this.needToWaitForBacklogDraining = true;

        long requestedBacklogSize = workload.consumerBacklogSizeGB * 1024 * 1024 * 1024;

        while (true) {
            CountersStats stats = worker.getCountersStats();
            long currentBacklogSize =
                    (workload.subscriptionsPerTopic * stats.messagesSent - stats.messagesReceived)
                            * workload.messageSize;

            if (currentBacklogSize >= requestedBacklogSize) {
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        log.info("--- Completed backlog build in {} s ---", timer.elapsedSeconds());
        timer = new Timer();
        log.info("--- Start draining backlog ---");

        worker.resumeConsumers();

        long backlogMessageCapacity = requestedBacklogSize / workload.messageSize;
        long backlogEmptyLevel = (long) ((1.0 - workload.backlogDrainRatio) * backlogMessageCapacity);
        final long minBacklog = Math.max(1000L, backlogEmptyLevel);

        while (true) {
            CountersStats stats = worker.getCountersStats();
            long currentBacklog =
                    workload.subscriptionsPerTopic * stats.messagesSent - stats.messagesReceived;
            if (currentBacklog <= minBacklog) {
                log.info("--- Completed backlog draining in {} s ---", timer.elapsedSeconds());

                try {
                    Thread.sleep(MINUTES.toMillis(testDurationMinutes));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                needToWaitForBacklogDraining = false;
                return;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings({"checkstyle:LineLength", "checkstyle:MethodLength"})
    private TestResult printAndCollectStats(
            long testDurations, TimeUnit unit, BooleanSupplier isFinished) throws IOException {
        long startTime = System.nanoTime();

        // Print report stats
        long oldTime = System.nanoTime();

        long testEndTime = testDurations > 0 ? startTime + unit.toNanos(testDurations) : Long.MAX_VALUE;

        TestResult result = new TestResult();
        result.workload = workload.name;
        result.driver = driverName;
        result.topics = workload.topics;
        result.partitions = workload.partitionsPerTopic;
        result.messageSize = workload.messageSize;
        result.producersPerTopic = workload.producersPerTopic;
        result.consumersPerTopic = workload.consumerPerSubscription;

        try {
            while (true) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    break;
                }

                long now = System.nanoTime();

                if (!EnvironmentConfiguration.isCloudMonitoringEnabled()) {
                    PeriodStats stats = worker.getPeriodStats();

                    double elapsed = (now - oldTime) / 1e9;

                    double publishRate = stats.messagesSent / elapsed;
                    double publishThroughput = stats.bytesSent / elapsed / 1024 / 1024;
                    double errorRate = stats.messageSendErrors / elapsed;

                    double consumeRate = stats.messagesReceived / elapsed;
                    double consumeThroughput = stats.bytesReceived / elapsed / 1024 / 1024;

                    long currentBacklog =
                            Math.max(
                                    0L,
                                    workload.subscriptionsPerTopic * stats.totalMessagesSent
                                            - stats.totalMessagesReceived);

                    log.info(
                            "Pub rate {} msg/s / {} MB/s | Pub err {} err/s | Cons rate {} msg/s / {} MB/s | Backlog: {} K | Pub Latency (ms) avg: {} - 50%: {} - 99%: {} - 99.9%: {} - Max: {} | Pub Delay Latency (us) avg: {} - 50%: {} - 99%: {} - 99.9%: {} - Max: {}",
                            rateFormat.format(publishRate),
                            throughputFormat.format(publishThroughput),
                            rateFormat.format(errorRate),
                            rateFormat.format(consumeRate),
                            throughputFormat.format(consumeThroughput),
                            dec.format(currentBacklog / 1000.0),
                            dec.format(microsToMillis(stats.publishLatency.getMean())),
                            dec.format(microsToMillis(stats.publishLatency.getValueAtPercentile(50))),
                            dec.format(microsToMillis(stats.publishLatency.getValueAtPercentile(99))),
                            dec.format(microsToMillis(stats.publishLatency.getValueAtPercentile(99.9))),
                            throughputFormat.format(microsToMillis(stats.publishLatency.getMaxValue())),
                            dec.format(stats.publishDelayLatency.getMean()),
                            dec.format(stats.publishDelayLatency.getValueAtPercentile(50)),
                            dec.format(stats.publishDelayLatency.getValueAtPercentile(99)),
                            dec.format(stats.publishDelayLatency.getValueAtPercentile(99.9)),
                            throughputFormat.format(stats.publishDelayLatency.getMaxValue()));

                    result.publishRate.add(publishRate);
                    result.publishErrorRate.add(errorRate);
                    result.consumeRate.add(consumeRate);
                    result.backlog.add(currentBacklog);
                    result.publishLatencyAvg.add(microsToMillis(stats.publishLatency.getMean()));
                    result.publishLatency50pct.add(
                            microsToMillis(stats.publishLatency.getValueAtPercentile(50)));
                    result.publishLatency75pct.add(
                            microsToMillis(stats.publishLatency.getValueAtPercentile(75)));
                    result.publishLatency95pct.add(
                            microsToMillis(stats.publishLatency.getValueAtPercentile(95)));
                    result.publishLatency99pct.add(
                            microsToMillis(stats.publishLatency.getValueAtPercentile(99)));
                    result.publishLatency999pct.add(
                            microsToMillis(stats.publishLatency.getValueAtPercentile(99.9)));
                    result.publishLatency9999pct.add(
                            microsToMillis(stats.publishLatency.getValueAtPercentile(99.99)));
                    result.publishLatencyMax.add(microsToMillis(stats.publishLatency.getMaxValue()));

                    result.publishDelayLatencyAvg.add(stats.publishDelayLatency.getMean());
                    result.publishDelayLatency50pct.add(stats.publishDelayLatency.getValueAtPercentile(50));
                    result.publishDelayLatency75pct.add(stats.publishDelayLatency.getValueAtPercentile(75));
                    result.publishDelayLatency95pct.add(stats.publishDelayLatency.getValueAtPercentile(95));
                    result.publishDelayLatency99pct.add(stats.publishDelayLatency.getValueAtPercentile(99));
                    result.publishDelayLatency999pct.add(
                            stats.publishDelayLatency.getValueAtPercentile(99.9));
                    result.publishDelayLatency9999pct.add(
                            stats.publishDelayLatency.getValueAtPercentile(99.99));
                    result.publishDelayLatencyMax.add(stats.publishDelayLatency.getMaxValue());

                    result.endToEndLatencyAvg.add(microsToMillis(stats.endToEndLatency.getMean()));
                    result.endToEndLatency50pct.add(
                            microsToMillis(stats.endToEndLatency.getValueAtPercentile(50)));
                    result.endToEndLatency75pct.add(
                            microsToMillis(stats.endToEndLatency.getValueAtPercentile(75)));
                    result.endToEndLatency95pct.add(
                            microsToMillis(stats.endToEndLatency.getValueAtPercentile(95)));
                    result.endToEndLatency99pct.add(
                            microsToMillis(stats.endToEndLatency.getValueAtPercentile(99)));
                    result.endToEndLatency999pct.add(
                            microsToMillis(stats.endToEndLatency.getValueAtPercentile(99.9)));
                    result.endToEndLatency9999pct.add(
                            microsToMillis(stats.endToEndLatency.getValueAtPercentile(99.99)));
                    result.endToEndLatencyMax.add(microsToMillis(stats.endToEndLatency.getMaxValue()));
                }

                if ((now >= testEndTime || isFinished.getAsBoolean()) && !needToWaitForBacklogDraining) {
                    if (!EnvironmentConfiguration.isCloudMonitoringEnabled()) {
                        CumulativeLatencies agg = worker.getCumulativeLatencies();
                        log.info(
                                "----- Aggregated Pub Latency (ms) avg: {} - 50%: {} - 95%: {} - 99%: {} - 99.9%: {} - 99.99%: {} - Max: {} | Pub Delay (us)  avg: {} - 50%: {} - 95%: {} - 99%: {} - 99.9%: {} - 99.99%: {} - Max: {}",
                                dec.format(agg.publishLatency.getMean() / 1000.0),
                                dec.format(agg.publishLatency.getValueAtPercentile(50) / 1000.0),
                                dec.format(agg.publishLatency.getValueAtPercentile(95) / 1000.0),
                                dec.format(agg.publishLatency.getValueAtPercentile(99) / 1000.0),
                                dec.format(agg.publishLatency.getValueAtPercentile(99.9) / 1000.0),
                                dec.format(agg.publishLatency.getValueAtPercentile(99.99) / 1000.0),
                                throughputFormat.format(agg.publishLatency.getMaxValue() / 1000.0),
                                dec.format(agg.publishDelayLatency.getMean()),
                                dec.format(agg.publishDelayLatency.getValueAtPercentile(50)),
                                dec.format(agg.publishDelayLatency.getValueAtPercentile(95)),
                                dec.format(agg.publishDelayLatency.getValueAtPercentile(99)),
                                dec.format(agg.publishDelayLatency.getValueAtPercentile(99.9)),
                                dec.format(agg.publishDelayLatency.getValueAtPercentile(99.99)),
                                throughputFormat.format(agg.publishDelayLatency.getMaxValue()));

                        result.aggregatedPublishLatencyAvg = agg.publishLatency.getMean() / 1000.0;
                        result.aggregatedPublishLatency50pct =
                                agg.publishLatency.getValueAtPercentile(50) / 1000.0;
                        result.aggregatedPublishLatency75pct =
                                agg.publishLatency.getValueAtPercentile(75) / 1000.0;
                        result.aggregatedPublishLatency95pct =
                                agg.publishLatency.getValueAtPercentile(95) / 1000.0;
                        result.aggregatedPublishLatency99pct =
                                agg.publishLatency.getValueAtPercentile(99) / 1000.0;
                        result.aggregatedPublishLatency999pct =
                                agg.publishLatency.getValueAtPercentile(99.9) / 1000.0;
                        result.aggregatedPublishLatency9999pct =
                                agg.publishLatency.getValueAtPercentile(99.99) / 1000.0;
                        result.aggregatedPublishLatencyMax = agg.publishLatency.getMaxValue() / 1000.0;

                        result.aggregatedPublishDelayLatencyAvg = agg.publishDelayLatency.getMean();
                        result.aggregatedPublishDelayLatency50pct =
                                agg.publishDelayLatency.getValueAtPercentile(50);
                        result.aggregatedPublishDelayLatency75pct =
                                agg.publishDelayLatency.getValueAtPercentile(75);
                        result.aggregatedPublishDelayLatency95pct =
                                agg.publishDelayLatency.getValueAtPercentile(95);
                        result.aggregatedPublishDelayLatency99pct =
                                agg.publishDelayLatency.getValueAtPercentile(99);
                        result.aggregatedPublishDelayLatency999pct =
                                agg.publishDelayLatency.getValueAtPercentile(99.9);
                        result.aggregatedPublishDelayLatency9999pct =
                                agg.publishDelayLatency.getValueAtPercentile(99.99);
                        result.aggregatedPublishDelayLatencyMax = agg.publishDelayLatency.getMaxValue();

                        result.aggregatedEndToEndLatencyAvg = agg.endToEndLatency.getMean() / 1000.0;
                        result.aggregatedEndToEndLatency50pct =
                                agg.endToEndLatency.getValueAtPercentile(50) / 1000.0;
                        result.aggregatedEndToEndLatency75pct =
                                agg.endToEndLatency.getValueAtPercentile(75) / 1000.0;
                        result.aggregatedEndToEndLatency95pct =
                                agg.endToEndLatency.getValueAtPercentile(95) / 1000.0;
                        result.aggregatedEndToEndLatency99pct =
                                agg.endToEndLatency.getValueAtPercentile(99) / 1000.0;
                        result.aggregatedEndToEndLatency999pct =
                                agg.endToEndLatency.getValueAtPercentile(99.9) / 1000.0;
                        result.aggregatedEndToEndLatency9999pct =
                                agg.endToEndLatency.getValueAtPercentile(99.99) / 1000.0;
                        result.aggregatedEndToEndLatencyMax = agg.endToEndLatency.getMaxValue() / 1000.0;

                        agg.publishLatency
                                .percentiles(100)
                                .forEach(
                                        value -> {
                                            result.aggregatedPublishLatencyQuantiles.put(
                                                    value.getPercentile(), value.getValueIteratedTo() / 1000.0);
                                        });

                        agg.publishDelayLatency
                                .percentiles(100)
                                .forEach(
                                        value -> {
                                            result.aggregatedPublishDelayLatencyQuantiles.put(
                                                    value.getPercentile(), value.getValueIteratedTo());
                                        });

                        agg.endToEndLatency
                                .percentiles(100)
                                .forEach(
                                        value -> {
                                            result.aggregatedEndToEndLatencyQuantiles.put(
                                                    value.getPercentile(), microsToMillis(value.getValueIteratedTo()));
                                        });
                    }

                    break;
                }

                oldTime = now;
            }
        } catch (Throwable ignored) {
        }

        return result;
    }

    private static final DecimalFormat rateFormat = new PaddingDecimalFormat("0.0", 7);
    private static final DecimalFormat throughputFormat = new PaddingDecimalFormat("0.0", 4);
    private static final DecimalFormat dec = new PaddingDecimalFormat("0.0", 4);

    private static String generateSubscriptionName(int index) {
        return String.format("sub-%03d-%s", index, RandomGenerator.getRandomString());
    }

    private static double microsToMillis(double timeInMicros) {
        return timeInMicros / 1000.0;
    }

    private static double microsToMillis(long timeInMicros) {
        return timeInMicros / 1000.0;
    }

    private static final Logger log = LoggerFactory.getLogger(WorkloadGenerator.class);
    private static final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
}
