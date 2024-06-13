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
package io.openmessaging.tpch.processing;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openmessaging.benchmark.common.EnvironmentConfiguration;
import io.openmessaging.benchmark.common.ObjectMappers;
import io.openmessaging.benchmark.common.key.distribution.KeyDistributor;
import io.openmessaging.benchmark.common.key.distribution.KeyDistributorType;
import io.openmessaging.benchmark.driver.BenchmarkProducer;
import io.openmessaging.benchmark.driver.MessageProducer;
import io.openmessaging.tpch.TpcHConstants;
import io.openmessaging.tpch.algorithm.TpcHAlgorithm;
import io.openmessaging.tpch.algorithm.TpcHQueryResultGenerator;
import io.openmessaging.tpch.client.S3Client;
import io.openmessaging.tpch.model.TpcHConsumerAssignment;
import io.openmessaging.tpch.model.TpcHIntermediateResult;
import io.openmessaging.tpch.model.TpcHMessage;
import io.openmessaging.tpch.model.TpcHMessageType;
import io.openmessaging.tpch.model.TpcHQueryResult;
import io.openmessaging.tpch.model.TpcHRow;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class TpcHMessageProcessor {
    public static final int numProcessors = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executor = Executors.newFixedThreadPool(numProcessors);
    private static final Map<String, Semaphore> semaphores = new ConcurrentHashMap<>();
    private static final ObjectWriter messageWriter = ObjectMappers.writer;
    private static final ObjectWriter writer = new ObjectMapper().writer();
    private static final ObjectMapper mapper =
            new ObjectMapper(new YAMLFactory())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final AtomicInteger numResultsProcessed = new AtomicInteger();
    private final Supplier<String> getExperimentId;
    private final List<BenchmarkProducer> producers;
    private volatile MessageProducer messageProducer;
    private final Runnable onTestCompleted;
    private final Logger log;
    private final S3Client s3AsyncClient = new S3Client(executor, this);
    private ExecutorService executorOverride = null;

    public TpcHMessageProcessor(
            Supplier<String> getExperimentId,
            List<BenchmarkProducer> producers,
            MessageProducer messageProducer,
            Runnable onTestCompleted,
            Logger log) {
        this.getExperimentId = getExperimentId;
        this.producers = producers;
        this.messageProducer = messageProducer;
        this.onTestCompleted = onTestCompleted;
        this.log = log == null ? LoggerFactory.getLogger(TpcHMessageProcessor.class) : log;
    }

    public void updateMessageProducer(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    public void startRowProcessor(ExecutorService executorOverride) {
        if (executorOverride != null) {
            this.executorOverride = executorOverride;
        }
        log.info("Started row processor from TPC-H processor.");
    }

    public void shutdown() {
        executor.shutdown();
        s3AsyncClient.shutdown();
    }

    public CompletableFuture<String> processTpcHMessage(
            TpcHMessage message, TpcHStateProvider stateProvider) throws IOException {
        if (EnvironmentConfiguration.isDebug()) {
            log.info(
                    "Processing TPC-H message: {} {}",
                    this.producers.size(),
                    writer.writeValueAsString(message));
        }
        try {
            switch (message.type) {
                case ConsumerAssignment:
                    TpcHConsumerAssignment assignment =
                            mapper.readValue(message.message, TpcHConsumerAssignment.class);
                    return processConsumerAssignment(assignment, stateProvider);
                case IntermediateResult:
                    TpcHIntermediateResult intermediateResult =
                            mapper.readValue(message.message, TpcHIntermediateResult.class);
                    return processIntermediateResult(intermediateResult, stateProvider);
                case ReducedResult:
                    TpcHIntermediateResult reducedResult =
                            mapper.readValue(message.message, TpcHIntermediateResult.class);
                    return processReducedResult(reducedResult, stateProvider);
                default:
                    throw new IllegalArgumentException("Invalid message type detected!");
            }
        } catch (Throwable t) {
            log.error("Error occurred while processing TPC-H message", t);
            throw new RuntimeException(t);
        }
    }

    public static GetObjectRequest parseS3Uri(String s3Uri) throws URISyntaxException {
        URI uri = new URI(s3Uri);
        String bucketName = uri.getHost();
        String key = uri.getPath().substring(1);
        return GetObjectRequest.builder().bucket(bucketName).key(key).build();
    }

    private CompletableFuture<InputStream> getStreamFromS3(GetObjectRequest getObjectRequest) {
        try {
            int numRetries = 0;
            while (numRetries < 5) {
                try {
                    return s3AsyncClient.getObject(getObjectRequest);
                } catch (Throwable t) {
                    log.error("[Try {}] Error occurred while retrieving object from S3.", numRetries);
                    numRetries++;
                }
            }
        } catch (Exception exception) {
            log.error(
                    String.format(
                            "Could not retrieve object %s from bucket %s!",
                            getObjectRequest.key(), getObjectRequest.bucket()));
        }
        throw new RuntimeException("Allowed number of retries for retrieving a file from S3 exceeded!");
    }

    private CompletableFuture<String> processConsumerAssignment(
            TpcHConsumerAssignment assignment, TpcHStateProvider stateProvider) {
        String s3Uri = assignment.sourceDataS3Uri;
        String queryId = assignment.queryId;
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Applying map to chunk \"{}\"...", s3Uri);
        }
        Set<String> processedMapMessageIds = stateProvider.getProcessedMapMessageIds();
        String mapMessageId = String.format("%s-%s", assignment.queryId, assignment.sourceDataS3Uri);
        if (processedMapMessageIds.contains(mapMessageId)) {
            log.warn(
                    "Ignored consumer assignment with map message ID {} due to duplicity!", mapMessageId);
            return CompletableFuture.completedFuture(queryId);
        } else {
            processedMapMessageIds.add(mapMessageId);
        }
        ExecutorService executor = executorOverride != null ? executorOverride : TpcHMessageProcessor.executor;
        executor.submit(() -> {
            try {
                // 33,608 lines of 156 bytes
                s3AsyncClient.fetchAndProcessCsvInChunks(assignment, 5242848).thenApply((result) -> {
                    try {
                        processConsumerAssignmentChunk(result, assignment);
                    } catch (Throwable t) {
                        log.error("Error occurred while processing consumer assignment chunk.", t);
                        throw new RuntimeException(t);
                    }
                    return null;
                });
            } catch (Throwable t) {
                log.error("Error occurred while fetching and processing CSV in chunks.", t);
                throw new RuntimeException(t);
            }
        });
        return CompletableFuture.completedFuture(queryId);
    }

    public String processConsumerAssignmentChunk(
            TpcHIntermediateResult result, TpcHConsumerAssignment assignment) throws Exception {
        String queryId = assignment.queryId;
        log.info("Submitting task to process consumer assignment chunk...");
        executor.submit(
                () -> {
                    try {
                        log.info("[STARTED] Task to process consumer assignment chunk");
                        int producerIndex =
                                TpcHConstants.REDUCE_PRODUCER_START_INDEX + assignment.producerIndex;
                        BenchmarkProducer producer = this.producers.get(producerIndex);
                        KeyDistributor keyDistributor = KeyDistributor.build(KeyDistributorType.NO_KEY);
                        TpcHMessage message =
                                new TpcHMessage(
                                        TpcHMessageType.IntermediateResult, messageWriter.writeValueAsString(result));
                        String key = keyDistributor.next();
                        Optional<String> optionalKey = key == null ? Optional.empty() : Optional.of(key);
                        String serializedMessage = messageWriter.writeValueAsString(message);
                        if (EnvironmentConfiguration.isDebug()) {
                            log.info("Sending consumer assignment: {}", serializedMessage);
                        }
                        String experimentId = getExperimentId.get();
                        this.messageProducer.sendMessage(
                                producer,
                                optionalKey,
                                messageWriter.writeValueAsBytes(message),
                                experimentId == null ? queryId : experimentId.replace("QUERY_ID", queryId),
                                message.messageId,
                                true);
                    } catch (Exception exception) {
                        log.error("Exception occurred while processing consumer assignment chunk", exception);
                        throw new RuntimeException(exception);
                    }
                });
        return queryId;
    }

    private CompletableFuture<String> processIntermediateResult(
            TpcHIntermediateResult intermediateResult, TpcHStateProvider stateProvider) throws Exception {
        String queryId = intermediateResult.queryId;
        String chunkId = this.getChunkId(intermediateResult);
        String batchId = intermediateResult.batchId;
        Set<String> processedIntermediateResults = stateProvider.getProcessedIntermediateResults();
        Map<String, TpcHIntermediateResult> collectedIntermediateResults =
                stateProvider.getCollectedIntermediateResults();

        if (processedIntermediateResults.contains(chunkId)) {
            log.warn("Ignored intermediate result with chunk ID {} due to duplicity!", chunkId);
            return CompletableFuture.completedFuture(queryId);
        } else {
            processedIntermediateResults.add(chunkId);
        }

        semaphores.putIfAbsent(batchId, new Semaphore(1));
        Semaphore semaphore = semaphores.get(batchId);

        TpcHIntermediateResult existingIntermediateResult = null;
        try {
            if (!semaphore.tryAcquire(10, TimeUnit.SECONDS)) {
                throw new IllegalArgumentException("Could not acquire intermediate result lock!");
            }
            try {
                if (!collectedIntermediateResults.containsKey(batchId)) {
                    collectedIntermediateResults.put(batchId, intermediateResult);
                } else {
                    existingIntermediateResult = collectedIntermediateResults.get(batchId);
                    existingIntermediateResult.aggregateIntermediateResult(intermediateResult);
                }
            } finally {
                semaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (existingIntermediateResult != null
                && existingIntermediateResult.numberOfAggregatedResults.intValue()
                        == intermediateResult.numberOfMapResults.intValue()) {
            BenchmarkProducer producer = this.producers.get(TpcHConstants.REDUCE_DST_INDEX);
            KeyDistributor keyDistributor = KeyDistributor.build(KeyDistributorType.NO_KEY);
            String reducedResult = messageWriter.writeValueAsString(existingIntermediateResult);
            TpcHMessage message = new TpcHMessage(TpcHMessageType.ReducedResult, reducedResult);
            String key = keyDistributor.next();
            Optional<String> optionalKey = key == null ? Optional.empty() : Optional.of(key);
            if (EnvironmentConfiguration.isDebug()) {
                log.info("Sending reduced result: {}", reducedResult);
            }
            String experimentId = getExperimentId.get();
            return this.messageProducer
                    .sendMessage(
                            producer,
                            optionalKey,
                            messageWriter.writeValueAsBytes(message),
                            experimentId == null ? queryId : experimentId.replace("QUERY_ID", queryId),
                            message.messageId,
                            true)
                    .thenApplyAsync(
                            (f) -> {
                                collectedIntermediateResults.remove(batchId);
                                return queryId;
                            },
                            executor);
        }
        return CompletableFuture.completedFuture(queryId);
    }

    private CompletableFuture<String> processReducedResult(
            TpcHIntermediateResult reducedResult, TpcHStateProvider stateProvider) throws IOException {
        String queryId = reducedResult.queryId;
        String batchId = reducedResult.batchId;
        Set<String> processedReducedResults = stateProvider.getProcessedReducedResults();
        Map<String, TpcHIntermediateResult> collectedReducedResults =
                stateProvider.getCollectedReducedResults();
        if (processedReducedResults.contains(batchId)) {
            log.warn("Ignored reduced result with batch ID {} due to duplicity!", batchId);
            return CompletableFuture.completedFuture(queryId);
        } else {
            processedReducedResults.add(batchId);
        }

        if (!collectedReducedResults.containsKey(queryId)) {
            collectedReducedResults.put(queryId, reducedResult);
        } else {
            TpcHIntermediateResult existingReducedResult = collectedReducedResults.get(queryId);
            existingReducedResult.aggregateReducedResult(reducedResult);

            if (EnvironmentConfiguration.isDebug()) {
                log.info(
                        "Detected reduced result: {}\n\n{}",
                        writer.writeValueAsString(reducedResult),
                        writer.writeValueAsString(existingReducedResult));
            }
            Integer newNumResultsProcessed = numResultsProcessed.incrementAndGet();
            if (newNumResultsProcessed % 10 == 0) {
                log.info("TPC-H progress: {}", newNumResultsProcessed);
            }
            if (existingReducedResult.numberOfAggregatedResults.intValue()
                    == reducedResult.numberOfChunks.intValue()) {
                TpcHQueryResult result = TpcHQueryResultGenerator.generateResult(existingReducedResult);
                log.info("[RESULT] TPC-H query result: {}", writer.writeValueAsString(result));
                log.info("[RESULT] Observed at {}", new Date().getTime());
                stateProvider.getProcessedIntermediateResults().clear();
                processedReducedResults.clear();
                stateProvider.getCollectedIntermediateResults().clear();
                collectedReducedResults.clear();
                onTestCompleted.run();
            }
        }
        return CompletableFuture.completedFuture(queryId);
    }

    private String getChunkId(TpcHIntermediateResult result) {
        return String.format("%s_%d", result.batchId, result.chunkIndex);
    }
}
