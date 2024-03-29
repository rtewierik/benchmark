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
import io.openmessaging.benchmark.common.client.AmazonS3Client;
import io.openmessaging.benchmark.common.utils.UniformRateLimiter;
import io.openmessaging.benchmark.driver.BenchmarkConsumer;
import io.openmessaging.tpch.model.TpcHMessage;
import io.openmessaging.tpch.processing.TpcHMessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

public class S3BenchmarkConsumer implements RequestHandler<S3Event, Void>, BenchmarkConsumer {

    private static final ObjectMapper mapper =
            new ObjectMapper(new YAMLFactory())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
    private static final Logger log = LoggerFactory.getLogger(S3BenchmarkConsumer.class);
    private static final TpcHMessageProcessor messageProcessor = new TpcHMessageProcessor(
            S3BenchmarkConfiguration.getS3Uris().stream().map(S3BenchmarkS3Producer::new).collect(Collectors.toList()),
            new S3BenchmarkMessageProducer(new UniformRateLimiter(1.0)),
            () -> {
            },
            log
    );
    private static final AmazonS3Client s3Client = new AmazonS3Client();

    @Override
    public Void handleRequest(S3Event event, Context context) {
        if (S3BenchmarkConfiguration.isTpcH()) {
            handleTpcHRequest(event);
        } else {
            handleThroughputRequest(event);
        }
        return null;
    }

    private void handleTpcHRequest(S3Event event) {
        for (S3Event.S3EventNotificationRecord record : event.getRecords()) {
            try {
                log.info("Received message: {}", writer.writeValueAsString(record.getS3()));
                String bucketName = record.getS3().getBucket().getName();
                String key = record.getS3().getObject().getKey();
                try (InputStream stream = s3Client.readFileFromS3(bucketName, key)) {
                    TpcHMessage tpcHMessage = mapper.readValue(stream, TpcHMessage.class);
                    messageProcessor.processTpcHMessage(tpcHMessage);
                    this.deleteMessage(record);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleThroughputRequest(S3Event event) {
        for (S3Event.S3EventNotificationRecord record : event.getRecords()) {
            this.deleteMessage(record);
        }
    }

    private void deleteMessage(S3Event.S3EventNotificationRecord record) {
        String bucketName = record.getS3().getBucket().getName();
        String key = record.getS3().getObject().getKey();
        s3Client.deleteFileFromS3(bucketName, key);
    }

    @Override
    public void close() throws Exception {
    }
}