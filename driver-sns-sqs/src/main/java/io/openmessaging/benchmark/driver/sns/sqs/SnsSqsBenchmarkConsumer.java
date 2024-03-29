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
package io.openmessaging.benchmark.driver.sns.sqs;


import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openmessaging.benchmark.common.utils.UniformRateLimiter;
import io.openmessaging.benchmark.driver.BenchmarkConsumer;
import io.openmessaging.tpch.model.TpcHMessage;
import io.openmessaging.tpch.processing.TpcHMessageProcessor;
import java.io.IOException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnsSqsBenchmarkConsumer implements RequestHandler<SQSEvent, Void>, BenchmarkConsumer {

    private static final ObjectMapper mapper =
            new ObjectMapper(new YAMLFactory())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
    private static final Logger log = LoggerFactory.getLogger(SnsSqsBenchmarkConsumer.class);
    private static final TpcHMessageProcessor messageProcessor =
            new TpcHMessageProcessor(
                    SnsSqsBenchmarkConfiguration.getSnsUris().stream()
                            .map(SnsSqsBenchmarkSnsProducer::new)
                            .collect(Collectors.toList()),
                    new SnsSqsBenchmarkMessageProducer(new UniformRateLimiter(1.0)),
                    () -> {},
                    log);
    private static final AmazonSQS sqsClient =
            AmazonSQSClientBuilder.standard()
                    .withRegion(SnsSqsBenchmarkConfiguration.getRegion())
                    .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                    .build();
    private static final String sqsUri = SnsSqsBenchmarkConfiguration.getSqsUri();

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        if (SnsSqsBenchmarkConfiguration.isTpcH()) {
            handleTpcHRequest(event);
        } else {
            handleThroughputRequest(event);
        }
        return null;
    }

    private void handleTpcHRequest(SQSEvent event) {
        for (SQSMessage message : event.getRecords()) {
            try {
                log.info("Received message: {}", writer.writeValueAsString(message));
                String body = message.getBody();
                TpcHMessage tpcHMessage = mapper.readValue(body, TpcHMessage.class);
                messageProcessor.processTpcHMessage(tpcHMessage);
                this.deleteMessage(message.getReceiptHandle());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleThroughputRequest(SQSEvent event) {
        for (SQSMessage message : event.getRecords()) {
            this.deleteMessage(message.getReceiptHandle());
        }
    }

    private void deleteMessage(String receiptHandle) {
        sqsClient.deleteMessage(new DeleteMessageRequest(sqsUri, receiptHandle));
        System.out.println("Message deleted from the queue");
    }

    @Override
    public void close() throws Exception {}
}
