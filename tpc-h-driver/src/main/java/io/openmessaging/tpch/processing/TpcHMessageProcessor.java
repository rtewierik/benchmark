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


import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openmessaging.benchmark.common.EnvironmentConfiguration;
import io.openmessaging.benchmark.common.ObjectMappers;
import io.openmessaging.benchmark.common.client.AmazonS3Client;
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
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TpcHMessageProcessor {
    private final Map<String, TpcHIntermediateResult> collectedIntermediateResults =
            new ConcurrentHashMap<>();
    private final Map<String, TpcHIntermediateResult> collectedReducedResults =
            new ConcurrentHashMap<>();
    private final Set<String> processedMessages = new ConcurrentSkipListSet<>();
    private final Set<String> processedIntermediateResults = new ConcurrentSkipListSet<>();
    private final Set<String> processedReducedResults = new ConcurrentSkipListSet<>();
    private final List<BenchmarkProducer> producers;
    private volatile MessageProducer messageProducer;
    private final Runnable onTestCompleted;
    private final Logger log;
    private static final AmazonS3Client s3Client = new AmazonS3Client();
    private static final ObjectWriter messageWriter = ObjectMappers.writer;
    private static final ObjectWriter writer = new ObjectMapper().writer();
    private static final ObjectMapper mapper =
            new ObjectMapper(new YAMLFactory())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public TpcHMessageProcessor(
            List<BenchmarkProducer> producers,
            MessageProducer messageProducer,
            Runnable onTestCompleted,
            Logger log) {
        this.producers = producers;
        this.messageProducer = messageProducer;
        this.onTestCompleted = onTestCompleted;
        this.log = log == null ? LoggerFactory.getLogger(TpcHMessageProcessor.class) : log;
    }

    public void updateMessageProducer(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    public String processTpcHMessage(TpcHMessage message) throws IOException {
        String messageId = message.messageId;
        if (EnvironmentConfiguration.isDebug()) {
            log.info(
                    "Processing TPC-H message: {} {}",
                    this.producers.size(),
                    writer.writeValueAsString(message));
        }
        if (processedMessages.contains(messageId)) {
            return null;
        } else {
            processedMessages.add(messageId);
        }
        try {
            switch (message.type) {
                case ConsumerAssignment:
                    TpcHConsumerAssignment assignment =
                            mapper.readValue(message.message, TpcHConsumerAssignment.class);
                    return processConsumerAssignment(assignment);
                case IntermediateResult:
                    TpcHIntermediateResult intermediateResult =
                            mapper.readValue(message.message, TpcHIntermediateResult.class);
                    return processIntermediateResult(intermediateResult);
                case ReducedResult:
                    TpcHIntermediateResult reducedResult =
                            mapper.readValue(message.message, TpcHIntermediateResult.class);
                    return processReducedResult(reducedResult);
                default:
                    throw new IllegalArgumentException("Invalid message type detected!");
            }
        } catch (Throwable t) {
            String messageStr = t.getMessage();
            String stackTrace = writer.writeValueAsString(t.getStackTrace());
            int size = this.producers.size();
            log.error(
                    "Error occurred while processing TPC-H message: {} {} {}", size, messageStr, stackTrace);
            throw new RuntimeException(t);
        }
    }

    private String processConsumerAssignment(TpcHConsumerAssignment assignment) {
        String s3Uri = assignment.sourceDataS3Uri;
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Applying map to chunk \"{}\"...", s3Uri);
        }
        try (S3Object object = s3Client.readFileFromS3(s3Uri)) {
            InputStream stream = object.getObjectContent();
            List<TpcHRow> chunkData = TpcHDataParser.readTpcHRowsFromStream(stream);
            stream.close();
            object.close();
            TpcHIntermediateResult result =
                    TpcHAlgorithm.applyQueryToChunk(chunkData, assignment.query, assignment);
            int producerIndex = TpcHConstants.REDUCE_PRODUCER_START_INDEX + assignment.producerIndex;
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
            this.messageProducer.sendMessage(
                    producer,
                    optionalKey,
                    messageWriter.writeValueAsBytes(message),
                    assignment.queryId,
                    message.messageId,
                    true);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return assignment.queryId;
    }

    private String processIntermediateResult(TpcHIntermediateResult intermediateResult)
            throws IOException {
        String queryId = intermediateResult.queryId;
        String chunkId = this.getChunkId(intermediateResult);
        String batchId = intermediateResult.batchId;
        if (processedIntermediateResults.contains(chunkId)) {
            log.warn("Ignored intermediate result with chunk ID {} due to duplicity!", chunkId);
            return queryId;
        } else {
            processedIntermediateResults.add(chunkId);
        }
        TpcHIntermediateResult existingIntermediateResult;
        if (!this.collectedIntermediateResults.containsKey(batchId)) {
            this.collectedIntermediateResults.put(batchId, intermediateResult);
            existingIntermediateResult = intermediateResult;
        } else {
            existingIntermediateResult = this.collectedIntermediateResults.get(batchId);
            existingIntermediateResult.aggregateIntermediateResult(intermediateResult);
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
            this.messageProducer.sendMessage(
                    producer,
                    optionalKey,
                    messageWriter.writeValueAsBytes(message),
                    queryId,
                    message.messageId,
                    true);
            this.collectedIntermediateResults.remove(batchId);
        }
        return queryId;
    }

    private String processReducedResult(TpcHIntermediateResult reducedResult) throws IOException {
        String queryId = reducedResult.queryId;
        String batchId = reducedResult.batchId;
        if (processedReducedResults.contains(batchId)) {
            log.warn("Ignored reduced result with batch ID {} due to duplicity!", batchId);
            return queryId;
        } else {
            processedReducedResults.add(batchId);
        }
        TpcHIntermediateResult existingReducedResult;
        if (!this.collectedReducedResults.containsKey(reducedResult.queryId)) {
            this.collectedReducedResults.put(reducedResult.queryId, reducedResult);
            existingReducedResult = reducedResult;
        } else {
            existingReducedResult = this.collectedReducedResults.get(reducedResult.queryId);
            existingReducedResult.aggregateReducedResult(reducedResult);
        }
        if (EnvironmentConfiguration.isDebug()) {
            log.info(
                    "Detected reduced result: {}\n\n{}",
                    writer.writeValueAsString(reducedResult),
                    writer.writeValueAsString(existingReducedResult));
        }
        if (existingReducedResult.numberOfAggregatedResults.intValue()
                == reducedResult.numberOfChunks.intValue()) {
            TpcHQueryResult result = TpcHQueryResultGenerator.generateResult(existingReducedResult);
            log.info("[RESULT] TPC-H query result: {}", writer.writeValueAsString(result));
            processedMessages.clear();
            processedIntermediateResults.clear();
            processedReducedResults.clear();
            collectedIntermediateResults.clear();
            collectedReducedResults.clear();
            onTestCompleted.run();
        }
        return queryId;
    }

    private String getChunkId(TpcHIntermediateResult result) {
        return String.format("%s_%d", result.batchId, result.chunkIndex);
    }
}
