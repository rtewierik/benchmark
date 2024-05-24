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

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Preconditions;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.openmessaging.benchmark.DriverConfiguration;
import io.openmessaging.benchmark.common.ObjectMappers;
import io.openmessaging.benchmark.common.key.distribution.KeyDistributor;
import io.openmessaging.benchmark.common.producer.MessageProducerImpl;
import io.openmessaging.benchmark.common.utils.RandomGenerator;
import io.openmessaging.benchmark.common.utils.UniformRateLimiter;
import io.openmessaging.benchmark.driver.BenchmarkConsumer;
import io.openmessaging.benchmark.driver.BenchmarkDriver;
import io.openmessaging.benchmark.driver.BenchmarkProducer;
import io.openmessaging.benchmark.driver.ConsumerCallback;
import io.openmessaging.benchmark.driver.BenchmarkDriver.ConsumerInfo;
import io.openmessaging.benchmark.driver.BenchmarkDriver.TopicInfo;
import io.openmessaging.benchmark.common.EnvironmentConfiguration;
import io.openmessaging.benchmark.common.monitoring.CentralWorkerStats;
import io.openmessaging.benchmark.common.monitoring.InstanceWorkerStats;
import io.openmessaging.benchmark.common.monitoring.WorkerStats;
import io.openmessaging.benchmark.driver.sns.sqs.SnsSqsBenchmarkConfiguration;
import io.openmessaging.benchmark.utils.Timer;
import io.openmessaging.benchmark.worker.commands.ConsumerAssignment;
import io.openmessaging.benchmark.common.monitoring.CountersStats;
import io.openmessaging.benchmark.common.monitoring.CumulativeLatencies;
import io.openmessaging.benchmark.common.monitoring.PeriodStats;
import io.openmessaging.benchmark.worker.commands.ProducerAssignment;
import io.openmessaging.benchmark.worker.commands.ProducerWorkAssignment;
import io.openmessaging.benchmark.worker.commands.TopicsInfo;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import io.openmessaging.tpch.TpcHConstants;
import io.openmessaging.tpch.model.TpcHConsumerAssignment;
import io.openmessaging.tpch.model.TpcHMessage;
import io.openmessaging.tpch.model.TpcHMessageType;
import io.openmessaging.tpch.model.TpcHProducerAssignment;
import io.openmessaging.tpch.processing.SingleThreadTpcHStateProvider;
import io.openmessaging.tpch.processing.TpcHMessageProcessor;
import io.openmessaging.tpch.processing.TpcHStateProvider;
import org.apache.bookkeeper.stats.NullStatsLogger;
import org.apache.bookkeeper.stats.StatsLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalWorker implements Worker, ConsumerCallback {

    private BenchmarkDriver benchmarkDriver = null;
    /*
        For TPC-H queries, the producers list is allocated with the producer for Map messages and producers for all the
        assigned reducers.
     */
    private final List<BenchmarkProducer> producers = new ArrayList<>();
    /*
        For TPC-H queries, the consumers list is allocated with the consumers for all the assigned reducers.
     */
    private final List<BenchmarkConsumer> consumers = new ArrayList<>();
    private volatile MessageProducerImpl messageProducer;
    private final Map<BenchmarkConsumer, TpcHStateProvider> stateProviders = new HashMap<>();
    private TpcHMessageProcessor tpcHMessageProcessor;
    private final ExecutorService executor =
            Executors.newCachedThreadPool(new DefaultThreadFactory("local-worker"));
    private final StatsLogger statsLogger;
    private final WorkerStats stats;
    private boolean testCompleted = false;
    private boolean consumersArePaused = false;
    private String experimentId = null;
    private boolean isTpcH = false;
    private static final ObjectWriter messageWriter = ObjectMappers.writer;

    public LocalWorker() {
        this(NullStatsLogger.INSTANCE);
    }

    public LocalWorker(StatsLogger statsLogger) {
        this.statsLogger = statsLogger;
        this.stats = EnvironmentConfiguration.isCloudMonitoringEnabled()
            ? new CentralWorkerStats(statsLogger)
            : new InstanceWorkerStats(statsLogger);
        this.messageProducer = new MessageProducerImpl(new UniformRateLimiter(1.0), stats);
        this.tpcHMessageProcessor = new TpcHMessageProcessor(
            this.producers,
            this.messageProducer,
            () -> testCompleted = true,
            log
        );
    }

    @Override
    public void initializeDriver(File driverConfigFile) throws IOException {
        Preconditions.checkArgument(benchmarkDriver == null);
        testCompleted = false;

        DriverConfiguration driverConfiguration =
                mapper.readValue(driverConfigFile, DriverConfiguration.class);

        log.info("Driver: {}", writer.writeValueAsString(driverConfiguration));

        if (EnvironmentConfiguration.isDebug()) {
            log.info(
                    "Configuration: {} {} {}",
                    SnsSqsBenchmarkConfiguration.snsUris,
                    SnsSqsBenchmarkConfiguration.region,
                    SnsSqsBenchmarkConfiguration.isTpcH
            );
        }

        try {
            benchmarkDriver =
                    (BenchmarkDriver) Class.forName(driverConfiguration.driverClass).newInstance();
            benchmarkDriver.initialize(driverConfigFile, this.statsLogger);
        } catch (InstantiationException
                | IllegalAccessException
                | ClassNotFoundException
                | InterruptedException e) {
            throw new RuntimeException(e);
        }
        this.tpcHMessageProcessor = new TpcHMessageProcessor(
                this.producers,
                this.messageProducer,
                () -> testCompleted = true,
                log
        );
    }

    @Override
    public List<String> createTopics(TopicsInfo topicsInfo) {
        Timer timer = new Timer();

        List<TopicInfo> topicInfos =
                IntStream.range(0, topicsInfo.numberOfTopics)
                        .mapToObj(
                                i -> new TopicInfo(generateTopicName(i), topicsInfo.numberOfPartitionsPerTopic))
                        .collect(toList());

        List<String> topics = benchmarkDriver
                .createTopics(topicInfos).join().stream().map(TopicInfo::getTopic).collect(toList());

        log.info("Created {} topics in {} ms", topics.size(), timer.elapsedMillis());
        return topics;
    }

    private String generateTopicName(int i) {
        return String.format(
                "%s-%07d-%s", benchmarkDriver.getTopicNamePrefix(), i, RandomGenerator.getRandomString());
    }

    @Override
    public void createProducers(ProducerAssignment producerAssignment) throws IOException {
        Timer timer = new Timer();
        AtomicInteger producerIndex = new AtomicInteger();
        producers.addAll(
                benchmarkDriver
                        .createProducers(
                                producerAssignment.topics.stream()
                                        .map(t -> new BenchmarkDriver.ProducerInfo(producerIndex.getAndIncrement(), t))
                                        .collect(toList()))
                        .join());

        String assignment = writer.writeValueAsString(producerAssignment);
        log.info("Created {} producers in {} ms from {}", producers.size(), timer.elapsedMillis(), assignment);
    }

    @Override
    public void createConsumers(ConsumerAssignment assignment) throws IOException {
        Timer timer = new Timer();
        AtomicInteger consumerIndex = new AtomicInteger();
        this.experimentId = assignment.experimentId;
        this.isTpcH = assignment.isTpcH;
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Raw consumers: {}", writer.writeValueAsString(assignment));
        }
        if (this.isTpcH && assignment.topicsSubscriptions.size() > TpcHConstants.REDUCE_DST_INDEX) {
            assignment.topicsSubscriptions.remove(TpcHConstants.REDUCE_DST_INDEX);
        }
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Creating consumers: {}", writer.writeValueAsString(assignment));
        }
        consumers.addAll(
                benchmarkDriver
                        .createConsumers(
                                assignment.topicsSubscriptions.stream()
                                        .map(
                                                c -> new ConsumerInfo(
                                                        consumerIndex.getAndIncrement(), c.topic, c.subscription, this))
                                        .collect(toList()))
                        .join()
                        .stream()
                        .filter(Objects::nonNull)
                        .collect(toList())
        );
        consumers.forEach(c -> stateProviders.put(c, new SingleThreadTpcHStateProvider()));

        log.info("Created {} consumers in {} ms", consumers.size(), timer.elapsedMillis());
    }

    @Override
    public void startLoad(ProducerWorkAssignment producerWorkAssignment) {
        if (producerWorkAssignment.tpcHArguments == null) {
            startLoadForThroughputProducers(producerWorkAssignment);
        } else {
            startLoadForTpcHProducers(producerWorkAssignment);
        }
    }

    @Override
    public void probeProducers() throws IOException {
        producers.forEach(
            producer ->
                producer.sendAsync(Optional.of("key"), new byte[10]).thenRun(stats::recordMessageSent));
    }

    private void startLoadForTpcHProducers(ProducerWorkAssignment producerWorkAssignment) {
        int processors = Runtime.getRuntime().availableProcessors();

        updateMessageProducer(producerWorkAssignment.publishRate);

        BenchmarkProducer producer = producers.get(TpcHConstants.MAP_CMD_INDEX);
        IntStream.range(0, processors).forEach(index ->
                submitTpcHProducersToExecutor(producerWorkAssignment, processors, producer, index));
    }

    public int getProcessorNumberOfCommands(int numberOfCommands, int numberOfProcessors, int index) {
        int defaultNumberOfCommands = getDefaultProcessorNumberOfCommands(numberOfCommands, numberOfProcessors);
        int commandsLeft = numberOfCommands - index * defaultNumberOfCommands;
        if (commandsLeft < 0) {
            return 0;
        }
        return Math.min(commandsLeft, defaultNumberOfCommands);
    }

    public int getDefaultProcessorNumberOfCommands(int numberOfCommands, int numberOfProcessors) {
         return (int) Math.ceil((double) numberOfCommands / numberOfProcessors);
    }

    private void submitTpcHProducersToExecutor(
            ProducerWorkAssignment producerWorkAssignment,
            Integer numProcessors,
            BenchmarkProducer producer,
            Integer processorProducerIndex
    ) {
        executor.submit(
                () -> {
                    TpcHProducerAssignment assignment = new TpcHProducerAssignment(
                            producerWorkAssignment.tpcHArguments,
                            producerWorkAssignment.producerIndex
                    );
                    KeyDistributor keyDistributor = KeyDistributor.build(producerWorkAssignment.keyDistributorType);
                    Integer defaultProcessorNumberOfCommands = getDefaultProcessorNumberOfCommands(
                            assignment.producerNumberOfCommands,
                            numProcessors
                    );
                    Integer processorNumberOfCommands = getProcessorNumberOfCommands(
                            assignment.producerNumberOfCommands,
                            numProcessors,
                            processorProducerIndex);
                    Integer producerStart = producerWorkAssignment.producerIndex * assignment.producerNumberOfCommands;
                    String folderUri = assignment.sourceDataS3FolderUri;
                    for (int commandIdx = 1; commandIdx <= processorNumberOfCommands; commandIdx++) {
                        try {
                            Integer chunkIndex = producerStart
                                    + processorProducerIndex * defaultProcessorNumberOfCommands
                                    + commandIdx;
                            Integer batchIdx = chunkIndex / assignment.commandsPerBatch;
                            Integer numberOfMapResults = assignment.getBatchSize(batchIdx);
                            String batchId = String.format(
                                    "%s-batch-%d-%s", assignment.queryId, batchIdx, numberOfMapResults);
                            Integer groupedChunkIndex = (chunkIndex - 1) / 1000;
                            String s3Uri = chunkIndex > 1000
                                    ? String.format("%s/%s/chunk_%d.csv", folderUri, groupedChunkIndex, chunkIndex)
                                    : String.format("%s/chunk_%d.csv", folderUri, chunkIndex);
                            TpcHConsumerAssignment consumerAssignment = new TpcHConsumerAssignment(
                                    assignment.query,
                                    assignment.queryId,
                                    batchId,
                                    chunkIndex,
                                    batchIdx,
                                    numberOfMapResults,
                                    assignment.numberOfChunks,
                                    s3Uri
                            );
                            TpcHMessage message = new TpcHMessage(
                                    TpcHMessageType.ConsumerAssignment,
                                    messageWriter.writeValueAsString(consumerAssignment)
                            );
                            String key = keyDistributor.next();
                            Optional<String> optionalKey = key == null ? Optional.empty() : Optional.of(key);
                            messageProducer.sendMessage(
                                    producer,
                                    optionalKey,
                                    messageWriter.writeValueAsBytes(message),
                                    this.experimentId,
                                    message.messageId,
                                    true
                            );
                        } catch (Throwable t) {
                            log.error("Got error", t);
                        }
                    }
//                    while (!testCompleted) {
//                        try {
//                            Thread.sleep(1000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
                });
    }

    private void startLoadForThroughputProducers(ProducerWorkAssignment producerWorkAssignment) {
        int processors = Runtime.getRuntime().availableProcessors();

        updateMessageProducer(producerWorkAssignment.publishRate);

        Map<Integer, List<BenchmarkProducer>> processorAssignment = new TreeMap<>();

        int processorIdx = 0;
        for (BenchmarkProducer p : producers) {
            processorAssignment
                .computeIfAbsent(processorIdx, x -> new ArrayList<>())
                .add(p);

            processorIdx = (processorIdx + 1) % processors;
        }

        processorAssignment
            .values()
            .forEach(
                producers ->
                    submitThroughputProducersToExecutor(
                        producers,
                        KeyDistributor.build(producerWorkAssignment.keyDistributorType),
                        producerWorkAssignment.payloadData));
    }

    private void submitThroughputProducersToExecutor(
            List<BenchmarkProducer> producers, KeyDistributor keyDistributor, List<byte[]> payloads) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int payloadCount = payloads.size();
        executor.submit(
                () -> {
                    try {
                        while (!testCompleted) {
                            producers.forEach(
                                    p ->
                                            messageProducer.sendMessage(
                                                    p,
                                                    Optional.ofNullable(keyDistributor.next()),
                                                    payloads.get(r.nextInt(payloadCount)),
                                                    this.experimentId,
                                                    null,
                                                    false));
                        }
                    } catch (Throwable t) {
                        log.error("Got error", t);
                    }
                });
    }

    @Override
    public void adjustPublishRate(double publishRate) {
        if (publishRate < 1.0) {
            updateMessageProducer(1.0);
            return;
        }
        updateMessageProducer(publishRate);
    }

    private void updateMessageProducer(double publishRate) {
        messageProducer = new MessageProducerImpl(new UniformRateLimiter(publishRate), stats);
        tpcHMessageProcessor.updateMessageProducer(this.messageProducer);
    }

    @Override
    public PeriodStats getPeriodStats() {
        return stats.toPeriodStats();
    }

    @Override
    public CumulativeLatencies getCumulativeLatencies() {
        return stats.toCumulativeLatencies();
    }

    @Override
    public CountersStats getCountersStats() throws IOException {
        CountersStats countersStats = stats.toCountersStats();
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Returning counters stats: {}", writer.writeValueAsString(countersStats));
        }
        return countersStats;
    }

    @Override
    public void messageReceived(byte[] data, long publishTimestamp, BenchmarkConsumer consumer) throws IOException {
        if (this.isTpcH && data.length != 10) {
            TpcHMessage message = mapper.readValue(data, TpcHMessage.class);
            String queryId = handleTpcHMessage(message, consumer);
            internalMessageReceived(data.length, publishTimestamp, queryId, message.messageId);
        } else {
            internalMessageReceived(data.length, publishTimestamp, this.experimentId, null);
        }
    }

    @Override
    public void messageReceived(ByteBuffer data, long publishTimestamp, BenchmarkConsumer consumer) throws IOException {
        int length = data.remaining();
        if (this.isTpcH && length != 10) {
            byte[] byteArray = new byte[length];
            data.get(byteArray);
            try {
                TpcHMessage message = mapper.readValue(byteArray, TpcHMessage.class);
                String queryId = handleTpcHMessage(message, consumer);
                internalMessageReceived(length, publishTimestamp, queryId, message.messageId);
            } catch (Throwable t) {
                String message = new String(byteArray, StandardCharsets.UTF_8);
                log.error("Error occurred while parsing message to TPC-H message: {}", message);
            }
        } else {
            internalMessageReceived(length, publishTimestamp, this.experimentId, null);
        }
    }

    private String handleTpcHMessage(TpcHMessage message, BenchmarkConsumer consumer) throws IOException {
        TpcHStateProvider stateProvider = stateProviders.get(consumer);
        return tpcHMessageProcessor.processTpcHMessage(message, stateProvider);
    }

    public boolean getTestCompleted() {
        return this.testCompleted;
    }

    public void internalMessageReceived(int size, long publishTimestamp, String experimentId, String messageId)
            throws IOException {
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Message received: {} {} {} {} {}", size, publishTimestamp, experimentId, messageId, this.stats);
        }
        long now = System.currentTimeMillis();
        long endToEndLatencyMicros = TimeUnit.MILLISECONDS.toMicros(now - publishTimestamp);
        long processTimestamp = new Date().getTime();
        stats.recordMessageReceived(
                size, endToEndLatencyMicros, publishTimestamp, processTimestamp, experimentId, messageId, this.isTpcH);

        while (consumersArePaused) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void pauseConsumers() throws IOException {
        consumersArePaused = true;
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Pausing consumers");
        }
    }

    @Override
    public void resumeConsumers() throws IOException {
        consumersArePaused = false;
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Resuming consumers");
        }
    }

    @Override
    public void resetStats() throws IOException {
        stats.resetLatencies();
    }

    @Override
    public void stopAll() {
        testCompleted = true;
        consumersArePaused = false;
        stats.reset();

        try {
            Thread.sleep(100);

            if (EnvironmentConfiguration.isDebug()) {
                log.info("Attempting to shut down producers...");
            }
            for (BenchmarkProducer producer : producers) {
                producer.close();
            }
            producers.clear();

            if (EnvironmentConfiguration.isDebug()) {
                log.info("Attempting to shut down consumers...");
            }
            for (BenchmarkConsumer consumer : consumers) {
                consumer.close();
            }
            consumers.clear();
            stateProviders.clear();

            if (EnvironmentConfiguration.isDebug()) {
                log.info("Attempting to shut down benchmark driver...");
            }
            if (benchmarkDriver != null) {
                benchmarkDriver.close();
                benchmarkDriver = null;
            }
        } catch (Exception e) {
            log.error("Error occurred while stopping all local resources.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String id() {
        return "local";
    }

    @Override
    public void close() throws Exception {
        executor.shutdown();
    }

    private static final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();

    private static final ObjectMapper mapper =
            new ObjectMapper(new YAMLFactory())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static {
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    private static final Logger log = LoggerFactory.getLogger(LocalWorker.class);
}
