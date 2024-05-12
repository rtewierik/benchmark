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
package io.openmessaging.benchmark.driver.s3;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.openmessaging.benchmark.common.EnvironmentConfiguration;
import io.openmessaging.benchmark.common.client.AmazonS3Client;
import io.openmessaging.benchmark.common.monitoring.CentralWorkerStats;
import io.openmessaging.benchmark.common.monitoring.WorkerStats;
import io.openmessaging.benchmark.common.producer.MessageProducerImpl;
import io.openmessaging.benchmark.common.utils.UniformRateLimiter;
import io.openmessaging.benchmark.driver.BenchmarkConsumer;
import io.openmessaging.tpch.model.TpcHMessage;
import io.openmessaging.tpch.processing.TpcHMessageProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3BenchmarkConsumer implements RequestHandler<S3Event, Void>, BenchmarkConsumer {

    private static final ObjectMapper mapper =
            new ObjectMapper(new YAMLFactory())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
    private static final Logger log = LoggerFactory.getLogger(S3BenchmarkConsumer.class);
    private static final WorkerStats stats = new CentralWorkerStats();
    private static final TpcHMessageProcessor messageProcessor =
            new TpcHMessageProcessor(
                    S3BenchmarkConfiguration.s3Uris.stream()
                            .map(S3BenchmarkS3Producer::new)
                            .collect(Collectors.toList()),
                    new MessageProducerImpl(new UniformRateLimiter(1.0), stats),
                    () -> {},
                    log);
    private static final AmazonS3Client s3Client = new AmazonS3Client();

    @Override
    public Void handleRequest(S3Event event, Context context) {
        if (S3BenchmarkConfiguration.isTpcH) {
            handleTpcHRequest(event);
        } else {
            handleThroughputRequest(event);
        }
        return null;
    }

    private void handleTpcHRequest(S3Event event) {
        for (S3Event.S3EventNotificationRecord record : event.getRecords()) {
            try {
                if (EnvironmentConfiguration.isDebug()) {
                    log.info("Received message: {}", writer.writeValueAsString(record.getS3()));
                }
                String bucketName = record.getS3().getBucket().getName();
                String key = record.getS3().getObject().getKey();
                try (InputStream stream = s3Client.readFileFromS3(bucketName, key)) {
                    int payloadLength = stream.available();
                    TpcHMessage tpcHMessage = mapper.readValue(stream, TpcHMessage.class);
                    String experimentId = messageProcessor.processTpcHMessage(tpcHMessage);
                    long now = System.currentTimeMillis();
                    long publishTimestamp = record.getEventTime().getMillis();
                    long endToEndLatencyMicros = TimeUnit.MILLISECONDS.toMicros(now - publishTimestamp);
                    stats.recordMessageReceived(
                            payloadLength, endToEndLatencyMicros, experimentId, tpcHMessage.messageId, true);
                    this.deleteMessage(record);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleThroughputRequest(S3Event event) {
        for (S3Event.S3EventNotificationRecord record : event.getRecords()) {
            try {
                if (EnvironmentConfiguration.isDebug()) {
                    log.info("Received message: {}", writer.writeValueAsString(record.getS3()));
                }
                String bucketName = record.getS3().getBucket().getName();
                String key = record.getS3().getObject().getKey();
                try (InputStream stream = s3Client.readFileFromS3(bucketName, key)) {
                    int payloadLength = stream.available();
                    long now = System.currentTimeMillis();
                    long publishTimestamp = record.getEventTime().getMillis();
                    long endToEndLatencyMicros = TimeUnit.MILLISECONDS.toMicros(now - publishTimestamp);
                    stats.recordMessageReceived(
                            payloadLength,
                            endToEndLatencyMicros,
                            "THROUGHPUT_S3",
                            String.format("%s-%s", record.getEventName(), record.getEventTime().getMillis()),
                            false);
                    this.deleteMessage(record);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void deleteMessage(S3Event.S3EventNotificationRecord record) {
        String bucketName = record.getS3().getBucket().getName();
        String key = record.getS3().getObject().getKey();
        s3Client.deleteFileFromS3(bucketName, key);
    }

    @Override
    public void close() throws Exception {}
}
