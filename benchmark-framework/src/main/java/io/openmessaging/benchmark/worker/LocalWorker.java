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
import io.openmessaging.benchmark.client.AmazonS3Client;
import io.openmessaging.benchmark.driver.*;
import io.openmessaging.benchmark.driver.BenchmarkDriver.ConsumerInfo;
import io.openmessaging.benchmark.driver.BenchmarkDriver.TopicInfo;
import io.openmessaging.benchmark.tpch.*;
import io.openmessaging.benchmark.utils.RandomGenerator;
import io.openmessaging.benchmark.utils.Timer;
import io.openmessaging.benchmark.utils.UniformRateLimiter;
import io.openmessaging.benchmark.utils.distributor.KeyDistributor;
import io.openmessaging.benchmark.utils.distributor.KeyDistributorType;
import io.openmessaging.benchmark.worker.commands.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import io.openmessaging.benchmark.worker.jackson.ObjectMappers;
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
    private volatile MessageProducer messageProducer;
    private final ExecutorService executor =
            Executors.newCachedThreadPool(new DefaultThreadFactory("local-worker"));
    private final WorkerStats stats;
    private boolean testCompleted = false;
    private boolean consumersArePaused = false;
    private final Map<String, TpcHIntermediateResult> collectedIntermediateResults = new ConcurrentHashMap<>();
    private final Map<String, TpcHIntermediateResult> collectedReducedResults = new ConcurrentHashMap<>();
    private final Set<String> processedMessages = new ConcurrentSkipListSet<>();
    private final AmazonS3Client s3Client = new AmazonS3Client();
    private static final ObjectWriter messageWriter = ObjectMappers.DEFAULT.writer();

    public LocalWorker() {
        this(NullStatsLogger.INSTANCE);
    }

    public LocalWorker(StatsLogger statsLogger) {
        stats = new WorkerStats(statsLogger);
        updateMessageProducer(1.0);
    }

    @Override
    public void initializeDriver(File driverConfigFile) throws IOException {
        Preconditions.checkArgument(benchmarkDriver == null);
        testCompleted = false;

        DriverConfiguration driverConfiguration =
                mapper.readValue(driverConfigFile, DriverConfiguration.class);

        log.info("Driver: {}", writer.writeValueAsString(driverConfiguration));

        try {
            benchmarkDriver =
                    (BenchmarkDriver) Class.forName(driverConfiguration.driverClass).newInstance();
            benchmarkDriver.initialize(driverConfigFile, stats.getStatsLogger());
        } catch (InstantiationException
                | IllegalAccessException
                | ClassNotFoundException
                | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> createTopics(TopicsInfo topicsInfo) {
        Timer timer = new Timer();

        List<TopicInfo> topicInfos =
                IntStream.range(0, topicsInfo.numberOfTopics)
                        .mapToObj(
                                i -> new TopicInfo(generateTopicName(i), topicsInfo.numberOfPartitionsPerTopic))
                        .collect(toList());

        benchmarkDriver.createTopics(topicInfos).join();

        List<String> topics = topicInfos.stream().map(TopicInfo::getTopic).collect(toList());

        log.info("Created {} topics in {} ms", topics.size(), timer.elapsedMillis());
        return topics;
    }

    private String generateTopicName(int i) {
        return String.format(
                "%s-%07d-%s", benchmarkDriver.getTopicNamePrefix(), i, RandomGenerator.getRandomString());
    }

    @Override
    public void createProducers(ProducerAssignment producerAssignment) {
        Timer timer = new Timer();
        AtomicInteger index = new AtomicInteger();

        producers.addAll(
                benchmarkDriver
                        .createProducers(
                                producerAssignment.topics.stream()
                                        .map(t -> new BenchmarkDriver.ProducerInfo(index.getAndIncrement(), t))
                                        .collect(toList()))
                        .join());

        log.info("Created {} producers in {} ms", producers.size(), timer.elapsedMillis());
    }

    @Override
    public void createConsumers(ConsumerAssignment consumerAssignment) {
        Timer timer = new Timer();
        AtomicInteger index = new AtomicInteger();

        // This subscription should only be done on the orchestrator host.
        if (consumerAssignment.isTpcH && consumerAssignment.topicsSubscriptions.size() > TpcHConstants.REDUCE_DST_INDEX) {
            consumerAssignment.topicsSubscriptions.remove(TpcHConstants.REDUCE_DST_INDEX);
        }
        consumers.addAll(
                benchmarkDriver
                        .createConsumers(
                                consumerAssignment.topicsSubscriptions.stream()
                                        .map(
                                                c ->
                                                        new ConsumerInfo(
                                                                index.getAndIncrement(), c.topic, c.subscription, this, c.info))
                                        .collect(toList()))
                        .join());

        log.info("Created {} consumers in {} ms", consumers.size(), timer.elapsedMillis());
    }

    @Override
    public void startLoad(ProducerWorkAssignment producerWorkAssignment) {
        if (producerWorkAssignment.tpcH == null) {
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
        updateMessageProducer(producerWorkAssignment.publishRate);
        executor.submit(
                () -> {
                    BenchmarkProducer producer = producers.get(TpcHConstants.MAP_CMD_INDEX);
                    TpcHArguments command = producerWorkAssignment.tpcH;
                    TpcHProducerAssignment tpcH = new TpcHProducerAssignment(command, producerWorkAssignment.producerIndex);
                    AtomicInteger currentAssignment = new AtomicInteger();
                    KeyDistributor keyDistributor = KeyDistributor.build(producerWorkAssignment.keyDistributorType);
                    int limit = (tpcH.offset * tpcH.batchSize) + tpcH.batchSize;
                    String batchId = String.format("%s-batch-%d-%s", tpcH.queryId, tpcH.offset, RandomGenerator.getRandomString());
                    try {
                        while (currentAssignment.get() < limit) {
                            TpcHConsumerAssignment assignment = new TpcHConsumerAssignment(
                                tpcH.query,
                                tpcH.queryId,
                                batchId,
                                producerWorkAssignment.producerIndex,
                                String.format("%s/chunk_%d.csv", tpcH.sourceDataS3FolderUri, currentAssignment.incrementAndGet())
                            );
                            TpcHMessage message = new TpcHMessage(
                                TpcHMessageType.ConsumerAssignment,
                                messageWriter.writeValueAsString(assignment)
                            );
                            String key = keyDistributor.next();
                            Optional<String> optionalKey = key == null ? Optional.empty() : Optional.of(key);
                            messageProducer.sendMessage(
                                producer,
                                optionalKey,
                                messageWriter.writeValueAsBytes(message)
                            );
                        }
                    } catch (Throwable t) {
                        log.error("Got error", t);
                    }
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
                                                    payloads.get(r.nextInt(payloadCount))));
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
        messageProducer = new MessageProducer(new UniformRateLimiter(publishRate), stats);
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
        return stats.toCountersStats();
    }

    @Override
    public void messageReceived(byte[] data, long publishTimestamp, TpcHInfo info) throws IOException {
        internalMessageReceived(data.length, publishTimestamp);
        if (info != null && data.length != 10) {
            TpcHMessage message = mapper.readValue(data, TpcHMessage.class);
            handleTpcHMessage(message, info);
        }
        // TO DO: Add separate call to stats to record message processed.
    }

    @Override
    public void messageReceived(ByteBuffer data, long publishTimestamp, TpcHInfo info) throws IOException {
        internalMessageReceived(data.remaining(), publishTimestamp);
        if (info != null && data.remaining() != 10) {
            TpcHMessage message = mapper.readValue(data.array(), TpcHMessage.class);
            handleTpcHMessage(message, info);
        }
        // TO DO: Add separate call to stats to record message processed.
    }

    private void handleTpcHMessage(TpcHMessage message, TpcHInfo info) throws IOException {
        String messageId = message.messageId;
        if (processedMessages.contains(messageId)) {
            return;
        } else {
            processedMessages.add(messageId);
        }
        switch (message.type) {
            case ConsumerAssignment:
                TpcHConsumerAssignment assignment = mapper.readValue(message.message, TpcHConsumerAssignment.class);
                processConsumerAssignment(assignment, info);
                break;
            case IntermediateResult:
                TpcHIntermediateResultDto intermediateResult = mapper.readValue(message.message, TpcHIntermediateResultDto.class);
                processIntermediateResult(TpcHIntermediateResult.fromDto(intermediateResult), info);
                break;
            case ReducedResult:
                TpcHIntermediateResultDto reducedResult = mapper.readValue(message.message, TpcHIntermediateResultDto.class);
                processReducedResult(TpcHIntermediateResult.fromDto(reducedResult), info);
                break;
            default:
                throw new IllegalArgumentException("Invalid message type detected!");
        }
    }

    public boolean getTestCompleted() {
        return this.testCompleted;
    }

    private void processConsumerAssignment(TpcHConsumerAssignment assignment, TpcHInfo info) {
        String s3Uri = assignment.sourceDataS3Uri;
        log.info("[INFO] Applying map to chunk \"{}\"...", s3Uri);
        try (InputStream stream = this.s3Client.readTpcHChunkFromS3(s3Uri)) {
            List<TpcHRow> chunkData = TpcHDataParser.readTpcHRowsFromStream(stream);
            TpcHIntermediateResult result = TpcHAlgorithm.applyQueryToChunk(chunkData, info.query, assignment);
            int index = TpcHConstants.REDUCE_PRODUCER_START_INDEX + assignment.index;
            BenchmarkProducer producer = this.producers.get(index);
            KeyDistributor keyDistributor = KeyDistributor.build(KeyDistributorType.NO_KEY);
            TpcHMessage message = new TpcHMessage(
                TpcHMessageType.ReducedResult,
                messageWriter.writeValueAsString(result.toDto())
            );
            String key = keyDistributor.next();
            Optional<String> optionalKey = key == null ? Optional.empty() : Optional.of(key);
            this.messageProducer.sendMessage(
                producer,
                optionalKey,
                messageWriter.writeValueAsBytes(message)
            );
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void processIntermediateResult(TpcHIntermediateResult intermediateResult, TpcHInfo info) throws IOException {
        if (!this.collectedIntermediateResults.containsKey(intermediateResult.batchId)) {
            this.collectedIntermediateResults.put(intermediateResult.batchId, intermediateResult);
            return;
        }
        TpcHIntermediateResult existingIntermediateResult = this.collectedIntermediateResults.get(intermediateResult.batchId);
        existingIntermediateResult.aggregateIntermediateResult(intermediateResult);
        if (existingIntermediateResult.numberOfAggregatedResults == info.numberOfMapResults) {
            BenchmarkProducer producer = this.producers.get(TpcHConstants.REDUCE_DST_INDEX);
            KeyDistributor keyDistributor = KeyDistributor.build(KeyDistributorType.NO_KEY);
            TpcHMessage message = new TpcHMessage(
                TpcHMessageType.ReducedResult,
                messageWriter.writeValueAsString(existingIntermediateResult.toDto())
            );
            this.messageProducer.sendMessage(
                producer,
                Optional.of(keyDistributor.next()),
                messageWriter.writeValueAsBytes(message)
            );
        }
    }

    private void processReducedResult(TpcHIntermediateResult reducedResult, TpcHInfo info) throws IOException {
        if (!this.collectedReducedResults.containsKey(reducedResult.batchId)) {;
            this.collectedReducedResults.put(reducedResult.batchId, reducedResult);
            return;
        }
        TpcHIntermediateResult existingIntermediateResult = this.collectedReducedResults.get(reducedResult.batchId);
        existingIntermediateResult.aggregateIntermediateResult(reducedResult);
        if (existingIntermediateResult.numberOfAggregatedResults == info.numberOfReduceResults) {
            TpcHQueryResult result = TpcHQueryResultGenerator.generateResult(existingIntermediateResult, info.query);
            log.info("[LocalWorker] TPC-H query result: {}", writer.writeValueAsString(result));
            testCompleted = true;
        }
    }

    public void internalMessageReceived(int size, long publishTimestamp) {
        long now = System.currentTimeMillis();
        long endToEndLatencyMicros = TimeUnit.MILLISECONDS.toMicros(now - publishTimestamp);
        stats.recordMessageReceived(size, endToEndLatencyMicros);

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
        log.info("Pausing consumers");
    }

    @Override
    public void resumeConsumers() throws IOException {
        consumersArePaused = false;
        log.info("Resuming consumers");
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

            log.info("Attempting to shut down producers...");
            for (BenchmarkProducer producer : producers) {
                producer.close();
            }
            producers.clear();

            log.info("Attempting to shut down consumers...");
            for (BenchmarkConsumer consumer : consumers) {
                consumer.close();
            }
            consumers.clear();

            log.info("Attempting to shut down benchmark driver...");
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
    private static final ObjectMapper jsonMapper = ObjectMappers.DEFAULT.mapper();

    static {
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    private static final Logger log = LoggerFactory.getLogger(LocalWorker.class);
}
