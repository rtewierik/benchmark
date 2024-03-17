package io.openmessaging.benchmark.driver.sns.sqs;/*
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
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openmessaging.benchmark.common.utils.UniformRateLimiter;
import io.openmessaging.benchmark.driver.BenchmarkConsumer;
import io.openmessaging.benchmark.driver.BenchmarkProducer;
import io.openmessaging.benchmark.driver.MessageProducer;
import io.openmessaging.tpch.model.TpcHMessage;
import io.openmessaging.tpch.processing.TpcHMessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

public class SnsSqsBenchmarkConsumer implements RequestHandler<SQSEvent, Void>, BenchmarkConsumer {

    private final BenchmarkProducer producer;
    private final MessageProducer messageProducer;
    private final TpcHMessageProcessor messageProcessor;

    // TO DO: How to implement updating rate limiting?
    public SnsSqsBenchmarkConsumer() {
        this.producer = new SnsSqsBenchmarkSqsProducer();
        this.messageProducer = new SnsSqsBenchmarkMessageProducer(new UniformRateLimiter(1.0));
        this.messageProcessor = new TpcHMessageProcessor(
            Collections.singletonList(this.producer),
            this.messageProducer,
            () -> {},
            log
        );
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSMessage message : event.getRecords()) {
            String messageBody = message.getBody();
            if (SnsSqsBenchmarkConfiguration.isTpcH()) {
                try {
                    TpcHMessage tpcHMessage = mapper.readValue(messageBody, TpcHMessage.class);
                    messageProcessor.processTpcHMessage(tpcHMessage);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    @Override
    public void close() throws Exception {}

    private static final ObjectMapper mapper =
            new ObjectMapper(new YAMLFactory())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(SnsSqsBenchmarkConsumer.class);
}