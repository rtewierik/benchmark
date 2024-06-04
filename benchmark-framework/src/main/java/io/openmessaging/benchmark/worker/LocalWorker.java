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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
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
    private AdaptiveRateLimitedTaskProcessor taskProcessor;
    private CommandHandler commandHandler;
    private final StatsLogger statsLogger;
    private final WorkerStats stats;
    private boolean testCompleted = false;
    private boolean consumersArePaused = false;
    private String experimentId = null;
    private boolean isTpcH = false;
    private static final ObjectWriter messageWriter = ObjectMappers.writer;

    public LocalWorker() {
        this(NullStatsLogger.INSTANCE, "local-worker");
    }

    public LocalWorker(StatsLogger statsLogger, String poolName) {
        if (Objects.equals(poolName, "local-worker")) {
            this.commandHandler = new CommandHandler(poolName);
            this.taskProcessor = new AdaptiveRateLimitedTaskProcessor(1);
        }
        this.statsLogger = statsLogger;
        this.stats = EnvironmentConfiguration.isCloudMonitoringEnabled()
            ? new CentralWorkerStats(statsLogger)
            : new InstanceWorkerStats(statsLogger);
        this.messageProducer = new MessageProducerImpl(new UniformRateLimiter(1.0), stats);
        this.tpcHMessageProcessor = new TpcHMessageProcessor(
            () -> this.experimentId,
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
                () -> this.experimentId,
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
        List<String> topics = getTopics(producerAssignment);
        List<BenchmarkDriver.ProducerInfo> producerInfo = topics.stream()
                .map(t -> new BenchmarkDriver.ProducerInfo(producerIndex.getAndIncrement(), t))
                .collect(toList());
        if (producerAssignment.isTpcH) {
            for (int i = 0; i < 7; i++) {
                String topic = topics.get(TpcHConstants.MAP_CMD_START_INDEX);
                producerInfo.add(new BenchmarkDriver.ProducerInfo(producerIndex.getAndIncrement(), topic));
            }
        }
        producers.addAll(benchmarkDriver.createProducers(producerInfo).join());
        String assignment = writer.writeValueAsString(producerAssignment);
        log.info("Created {} producers in {} ms from {}", producers.size(), timer.elapsedMillis(), assignment);
    }

    private static List<String> getTopics(ProducerAssignment producerAssignment) {
        List<String> topics;
        if (producerAssignment.isTpcH) {
            int mapIndex = TpcHConstants.MAP_CMD_START_INDEX + producerAssignment.producerIndex;
            topics = new ArrayList<>();
            topics.add(producerAssignment.topics.get(TpcHConstants.REDUCE_DST_INDEX));
            topics.add(producerAssignment.topics.get(mapIndex));
            for (int i = TpcHConstants.REDUCE_SRC_START_INDEX; i < producerAssignment.topics.size(); i++) {
                topics.add(producerAssignment.topics.get(i));
            }
        } else {
            topics = producerAssignment.topics;
        }
        return topics;
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
        if (this.isTpcH && assignment.topicsSubscriptions.size() > 1) {
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
            commandHandler = new CommandHandler("throughput-worker");
            startLoadForThroughputProducers(producerWorkAssignment);
        } else {
            commandHandler = new CommandHandler(8, "tpc-h-worker");
            int maxConcurrentTasks = 1;
            switch (producerWorkAssignment.tpcHArguments.numberOfChunks) {
                case 100:
                    maxConcurrentTasks = 125;
                    break;
                case 1000:
                    maxConcurrentTasks = 250;
                    break;
                case 10000:
                    maxConcurrentTasks = 500;
                    break;
                default:
                    break;
            }
            taskProcessor = new AdaptiveRateLimitedTaskProcessor(maxConcurrentTasks);
            startLoadForTpcHProducers(producerWorkAssignment);
        }
    }

    @Override
    public void probeProducers() throws IOException {
        producers.forEach(
            producer -> {
                if (producer != null) {
                    try {
                        producer.sendAsync(Optional.of("key"), new byte[10]).thenRun(stats::recordMessageSent);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        );
    }

    private void startLoadForTpcHProducers(ProducerWorkAssignment assignment) {
        int processors = 8;

        updateMessageProducer(assignment.publishRate);

        TpcHProducerAssignment tpcHAssignment = new TpcHProducerAssignment(
                assignment.tpcHArguments,
                assignment.producerIndex
        );
        int numBatchesForProducer = (int) Math.ceil((double) tpcHAssignment.producerNumberOfCommands
                / tpcHAssignment.commandsPerBatch);
        log.info("Number of commands {} | Commands per batch {} | Batches per producer {}",
                tpcHAssignment.producerNumberOfCommands, tpcHAssignment.commandsPerBatch, numBatchesForProducer);
        int numBatchesForProcessor = (int) Math.ceil((double) numBatchesForProducer / processors);
        ArrayList<List<Integer>> partitions = new ArrayList<>(IntStream.range(0, numBatchesForProducer)
                .boxed()
                .collect(Collectors.groupingBy(i -> i / numBatchesForProcessor))
                .values());
        BenchmarkProducer mainProducer = producers.get(TpcHConstants.MAP_CMD_START_INDEX);
        Integer extraStartIndex = TpcHConstants.REDUCE_PRODUCER_START_INDEX + assignment.tpcHArguments.numberOfWorkers;
        IntStream.range(0, processors).forEach(index ->
                submitTpcHProducersToExecutor(
                        assignment,
                        tpcHAssignment,
                        index == 0 ? mainProducer : producers.get(extraStartIndex + index - 1),
                        index < partitions.size() ? partitions.get(index) : new ArrayList<>(),
                        index
                ));
    }

    private void submitTpcHProducersToExecutor(
            ProducerWorkAssignment producerWorkAssignment,
            TpcHProducerAssignment assignment,
            BenchmarkProducer producer,
            List<Integer> batches,
            Integer processorProducerIndex
    ) {
        commandHandler.handleCommand(
                () -> {
                    if (batches.isEmpty()) {
                        log.info("[TpcHBenchmark] No work for TPC-H producer {}-{}. Shutting down.",
                                producerWorkAssignment.producerIndex,
                                processorProducerIndex);
                        return;
                    }
                    KeyDistributor keyDistributor = KeyDistributor.build(producerWorkAssignment.keyDistributorType);
                    Integer producerStart = producerWorkAssignment.producerIndex
                            * assignment.defaultProducerNumberOfCommands;
                    Integer max = producerStart + assignment.producerNumberOfCommands;
                    String folderUri = assignment.sourceDataS3FolderUri;
                    Integer messagesSent = 0;
                    List<CompletableFuture<Void>> futures = new ArrayList<>();
                    log.info("Number of batches {} | Start {} | Max {}", batches.size(), producerStart, max);
                    for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
                        Integer batch = batches.get(batchIdx);
                        Integer start = producerStart + batch * assignment.commandsPerBatch;
                        for (int commandIdx = 0; commandIdx < assignment.commandsPerBatch; commandIdx++) {
                            try {
                                Integer chunkIndex = start + commandIdx + 1;
                                if (chunkIndex > max) {
                                    continue;
                                }
                                Integer globalBatchIdx = (chunkIndex - 1) / assignment.commandsPerBatch;
                                Integer numberOfMapResults = assignment.getBatchSize(globalBatchIdx);
                                String batchId = String.format(
                                        "%s-batch-%d-%s", assignment.queryId, globalBatchIdx, numberOfMapResults);
                                Integer groupedChunkIndex = (chunkIndex - 1) / 1000;
                                String s3Uri = chunkIndex > 1010
                                        ? String.format("%s/%s/chunk_%d.csv", folderUri, groupedChunkIndex, chunkIndex)
                                        : String.format("%s/chunk_%d.csv", folderUri, chunkIndex);
                                TpcHConsumerAssignment consumerAssignment = new TpcHConsumerAssignment(
                                        assignment.query,
                                        assignment.queryId,
                                        batchId,
                                        chunkIndex,
                                        globalBatchIdx,
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
                                CompletableFuture<Void> future = messageProducer.sendMessage(
                                        producer,
                                        optionalKey,
                                        messageWriter.writeValueAsBytes(message),
                                        this.experimentId,
                                        message.messageId,
                                        true
                                );
                                futures.add(future);
                                messagesSent++;
                            } catch (Throwable t) {
                                log.error("Got error", t);
                            }
                        }
                    }
                    log.info("[TpcHBenchmark] Launched {} completable futures. Awaiting...", messagesSent);
                    CompletableFuture[] futuresArray = futures.toArray(new CompletableFuture[0]);
                    CompletableFuture<Void> allOf = CompletableFuture.allOf(futuresArray);
                    allOf.join();
                    log.info("[TpcHBenchmark] Finished TPC-H producer {}-{} after sending {} messages. Shutting down.",
                            producerWorkAssignment.producerIndex,
                            processorProducerIndex,
                            messagesSent);
                });
    }

    private void startLoadForThroughputProducers(ProducerWorkAssignment producerWorkAssignment) {
        int totalNumProducers = this.producers.size();
        int processors = 8;

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
                        producerWorkAssignment.payloadData,
                        totalNumProducers));
    }

    private void submitThroughputProducersToExecutor(
            List<BenchmarkProducer> producers,
            KeyDistributor keyDistributor,
            List<byte[]> payloads,
            int totalNumProducers
    ) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int payloadCount = payloads.size();
        Optional<Integer> maxSize = payloads.stream()
                .map(bytes -> bytes.length)
                .max(Integer::compareTo);
        log.info("Submitting throughput producers with maxSize {} and producer size {}", maxSize, totalNumProducers);
        boolean isSafeExperiment = maxSize.isPresent() && (maxSize.get() < 1000 || totalNumProducers < 200);
        boolean isLargeExperiment = maxSize.isPresent() && maxSize.get() > 2000 && totalNumProducers >= 200;
        int maxOutstandingMessages = isLargeExperiment ? 500 : (isSafeExperiment ? 10000 : 1000);
        log.info("Using max outstanding messages {}", maxOutstandingMessages);
        int ovrMaxOutstandingMessages = 10000;
        log.info("Overriding max outstanding messages to {}", ovrMaxOutstandingMessages);
        commandHandler.handleCommand(
                () -> {
                    try {
                        final AtomicInteger[] messagesSent = {new AtomicInteger()};
                        final CompletableFuture[] futureToAwait = new CompletableFuture[]{null};
                        while (!testCompleted) {
                            producers.forEach(
                                    p -> {
                                        Integer currMessagesSent = messagesSent[0].incrementAndGet();
                                        try {
                                            CompletableFuture<Void> newFuture = messageProducer.sendMessage(
                                                    p,
                                                    Optional.ofNullable(keyDistributor.next()),
                                                    payloads.get(r.nextInt(payloadCount)),
                                                    this.experimentId,
                                                    null,
                                                    false);
                                            if (currMessagesSent > ovrMaxOutstandingMessages) {
                                                messagesSent[0] = new AtomicInteger();
                                                try {
                                                    futureToAwait[0].get();
                                                } catch (InterruptedException | ExecutionException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }
                                            if (futureToAwait[0] == null
                                                    || currMessagesSent > ovrMaxOutstandingMessages) {
                                                futureToAwait[0] = newFuture;
                                            }
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                        }
                    } catch (Throwable t) {
                        log.error("Got error", t);
                    }
                });
    }

    @Override
    public void adjustPublishRate(double publishRate) {
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Adjusting publish rate to {}", publishRate);
        }
        if (publishRate < 1.0) {
            updateMessageProducer(1.0);
            return;
        }
        updateMessageProducer(publishRate);
    }

    private void updateMessageProducer(double publishRate) {
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Updating message producer with new publish rate {}", publishRate);
        }
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
    // This function is called in a fire-and-forget manner, many times for the same reducer.
    public void messageReceived(byte[] data, long publishTimestamp, BenchmarkConsumer consumer) throws IOException {
        if (this.isTpcH && data.length != 10) {
            try {
                TpcHMessage message = mapper.readValue(data, TpcHMessage.class);
                int size = data.length;
                processTpcHMessage(publishTimestamp, consumer, size, message);
            } catch (Throwable t) {
                TpcHMessage message = mapper.readValue(data, TpcHMessage.class);
                log.error("Exception occurred while handling command {}", writer.writeValueAsString(message), t);
            }
        } else {
            internalMessageReceived(data.length, publishTimestamp, this.experimentId, null);
        }
        while (consumersArePaused) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
                    processTpcHMessage(publishTimestamp, consumer, length, message);
                } catch (Throwable t) {
                    TpcHMessage message = mapper.readValue(byteArray, TpcHMessage.class);
                    log.error("Exception occurred while handling command {}", writer.writeValueAsString(message), t);
                }
        } else {
            internalMessageReceived(length, publishTimestamp, this.experimentId, null);
        }
        while (consumersArePaused) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void processTpcHMessage(long publishTimestamp, BenchmarkConsumer consumer, int length, TpcHMessage message)
            throws IOException {
        boolean isConsumerAssignment = message.type == TpcHMessageType.ConsumerAssignment;
        if (isConsumerAssignment) {
            log.info("Starting new task.");
            taskProcessor.startNewTask();
            log.info("Started new task!");
        }
        TpcHStateProvider stateProvider = stateProviders.get(consumer);
        tpcHMessageProcessor.processTpcHMessage(message, stateProvider).thenApply((queryId) -> {
            try {
                internalMessageReceived(length, publishTimestamp, queryId, message.messageId);
            } catch (IOException e) {
                log.error("Internal message received resulted in an error.", e);
                throw new RuntimeException(e);
            } finally {
                if (isConsumerAssignment) {
                    log.info("Finishing task.");
                    taskProcessor.finishedRunningTask();
                }
            }
            return null;
        });
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
                if (producer != null) {
                    producer.close();
                }
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
        commandHandler.close();
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
