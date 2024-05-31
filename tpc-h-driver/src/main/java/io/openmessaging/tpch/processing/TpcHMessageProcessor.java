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
import io.openmessaging.tpch.algorithm.TpcHDataParser;
import io.openmessaging.tpch.algorithm.TpcHQueryResultGenerator;
import io.openmessaging.tpch.model.TpcHConsumerAssignment;
import io.openmessaging.tpch.model.TpcHIntermediateResult;
import io.openmessaging.tpch.model.TpcHMessage;
import io.openmessaging.tpch.model.TpcHMessageType;
import io.openmessaging.tpch.model.TpcHQueryResult;
import io.openmessaging.tpch.model.TpcHRow;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkAsyncHttpClientBuilder;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class TpcHMessageProcessor {
    private final Supplier<String> getExperimentId;
    private final List<BenchmarkProducer> producers;
    private volatile MessageProducer messageProducer;
    private final Runnable onTestCompleted;
    private final Logger log;
    private static final S3AsyncClient s3AsyncClient = S3AsyncClient
            .builder()
            .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                    .maxConcurrency(16)
                    .maxPendingConnectionAcquires(1000))
            .httpClientBuilder(new DefaultSdkAsyncHttpClientBuilder())
            .build();
    private static final ObjectWriter messageWriter = ObjectMappers.writer;
    private static final ObjectWriter writer = new ObjectMapper().writer();
    private static final ObjectMapper mapper =
            new ObjectMapper(new YAMLFactory())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Map<String, Semaphore> semaphores = new ConcurrentHashMap<>();

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

    private CompletableFuture<String> processConsumerAssignment(
            TpcHConsumerAssignment assignment, TpcHStateProvider stateProvider)
            throws URISyntaxException {
        String s3Uri = assignment.sourceDataS3Uri;
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Applying map to chunk \"{}\"...", s3Uri);
        }
        Set<String> processedMapMessageIds = stateProvider.getProcessedMapMessageIds();
        String mapMessageId = String.format("%s-%s", assignment.queryId, assignment.sourceDataS3Uri);
        if (processedMapMessageIds.contains(mapMessageId)) {
            log.warn(
                    "Ignored consumer assignment with map message ID {} due to duplicity!", mapMessageId);
            return CompletableFuture.completedFuture(assignment.queryId);
        } else {
            processedMapMessageIds.add(mapMessageId);
        }
        GetObjectRequest getObjectRequest = parseS3Uri(s3Uri);
        return s3AsyncClient
                .getObject(getObjectRequest, AsyncResponseTransformer.toBytes())
                .thenApply(BytesWrapper::asInputStream)
                .thenApply(
                        stream -> {
                            try {
                                List<TpcHRow> chunkData = TpcHDataParser.readTpcHRowsFromStream(stream);
                                stream.close();
                                TpcHIntermediateResult result =
                                        TpcHAlgorithm.applyQueryToChunk(chunkData, assignment.query, assignment);
                                int producerIndex =
                                        TpcHConstants.REDUCE_PRODUCER_START_INDEX + assignment.producerIndex;
                                BenchmarkProducer producer = this.producers.get(producerIndex);
                                KeyDistributor keyDistributor = KeyDistributor.build(KeyDistributorType.NO_KEY);
                                TpcHMessage message =
                                        new TpcHMessage(
                                                TpcHMessageType.IntermediateResult,
                                                messageWriter.writeValueAsString(result));
                                String key = keyDistributor.next();
                                Optional<String> optionalKey = key == null ? Optional.empty() : Optional.of(key);
                                String serializedMessage = messageWriter.writeValueAsString(message);
                                if (EnvironmentConfiguration.isDebug()) {
                                    log.info("Sending consumer assignment: {}", serializedMessage);
                                }
                                String experimentId = getExperimentId.get();
                                String queryId = assignment.queryId;
                                this.messageProducer.sendMessage(
                                        producer,
                                        optionalKey,
                                        messageWriter.writeValueAsBytes(message),
                                        experimentId == null ? queryId : experimentId.replace("QUERY_ID", queryId),
                                        message.messageId,
                                        true);
                                return queryId;
                            } catch (IOException e) {
                                log.error("Error occurred while transforming retrieved file.", e);
                                throw new RuntimeException(e);
                            }
                        })
                .exceptionally(
                        throwable -> {
                            log.error("Error occurred while retrieving file from S3.", throwable);
                            return null;
                        });
    }

    private CompletableFuture<String> processIntermediateResult(
            TpcHIntermediateResult intermediateResult, TpcHStateProvider stateProvider)
            throws IOException {
        String queryId = intermediateResult.queryId;
        String chunkId = this.getChunkId(intermediateResult);
        String batchId = intermediateResult.batchId;
        Map<String, Void> processedIntermediateResults =
                stateProvider.getProcessedIntermediateResults();
        Map<String, TpcHIntermediateResult> collectedIntermediateResults =
                stateProvider.getCollectedIntermediateResults();
        if (processedIntermediateResults.containsKey(chunkId)) {
            log.warn("Ignored intermediate result with chunk ID {} due to duplicity!", chunkId);
            return CompletableFuture.completedFuture(queryId);
        } else {
            processedIntermediateResults.put(chunkId, null);
        }
        if (!semaphores.containsKey(batchId)) {
            semaphores.put(batchId, new Semaphore(1));
        }
        Semaphore semaphore = semaphores.get(batchId);
        TpcHIntermediateResult existingIntermediateResult;
        try {
            semaphore.acquire();
            if (!collectedIntermediateResults.containsKey(batchId)) {
                collectedIntermediateResults.put(batchId, intermediateResult);
                existingIntermediateResult = intermediateResult;
            } else {
                existingIntermediateResult = collectedIntermediateResults.get(batchId);
                existingIntermediateResult.aggregateIntermediateResult(intermediateResult);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            semaphore.release();
        }
        if (existingIntermediateResult.numberOfAggregatedResults.intValue()
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
                    .thenApply(
                            (f) -> {
                                collectedIntermediateResults.remove(batchId);
                                return queryId;
                            });
        }
        return CompletableFuture.completedFuture(queryId);
    }

    private CompletableFuture<String> processReducedResult(
            TpcHIntermediateResult reducedResult, TpcHStateProvider stateProvider) throws IOException {
        String queryId = reducedResult.queryId;
        String batchId = reducedResult.batchId;
        Map<String, Void> processedReducedResults = stateProvider.getProcessedReducedResults();
        Map<String, TpcHIntermediateResult> collectedReducedResults =
                stateProvider.getCollectedReducedResults();
        if (processedReducedResults.containsKey(batchId)) {
            log.warn("Ignored reduced result with batch ID {} due to duplicity!", batchId);
            return CompletableFuture.completedFuture(queryId);
        } else {
            processedReducedResults.put(batchId, null);
        }

        if (!semaphores.containsKey(batchId)) {
            semaphores.put(batchId, new Semaphore(1));
        }
        Semaphore semaphore = semaphores.get(queryId);
        TpcHIntermediateResult existingReducedResult;
        try {
            semaphore.acquire();
            if (!collectedReducedResults.containsKey(reducedResult.queryId)) {
                collectedReducedResults.put(reducedResult.queryId, reducedResult);
                existingReducedResult = reducedResult;
            } else {
                existingReducedResult = collectedReducedResults.get(reducedResult.queryId);
                existingReducedResult.aggregateReducedResult(reducedResult);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            semaphore.release();
        }
        if (EnvironmentConfiguration.isDebug()) {
            log.info(
                    "Detected reduced result: {}\n\n{}",
                    writer.writeValueAsString(reducedResult),
                    writer.writeValueAsString(existingReducedResult));
        }
        if (existingReducedResult.numberOfAggregatedResults % 1000 < 50) {
            log.info("TPC-H progress: {}", existingReducedResult.numberOfAggregatedResults);
        }
        if (existingReducedResult.numberOfAggregatedResults.intValue()
                == reducedResult.numberOfChunks.intValue()) {
            TpcHQueryResult result = TpcHQueryResultGenerator.generateResult(existingReducedResult);
            log.info("[RESULT] TPC-H query result: {}", writer.writeValueAsString(result));
            stateProvider.getProcessedIntermediateResults().clear();
            processedReducedResults.clear();
            stateProvider.getCollectedIntermediateResults().clear();
            collectedReducedResults.clear();
            onTestCompleted.run();
        }
        return CompletableFuture.completedFuture(queryId);
    }

    private String getChunkId(TpcHIntermediateResult result) {
        return String.format("%s_%d", result.batchId, result.chunkIndex);
    }
}
