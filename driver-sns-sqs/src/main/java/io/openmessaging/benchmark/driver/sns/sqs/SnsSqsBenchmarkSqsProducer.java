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
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import io.openmessaging.benchmark.driver.BenchmarkProducer;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SnsSqsBenchmarkSqsProducer implements BenchmarkProducer {

    private final AmazonSQS sqsClient;
    private final String sqsUri;

    public SnsSqsBenchmarkSqsProducer(String sqsUri, AmazonSQS sqsClient) {
        this.sqsUri = sqsUri;
        this.sqsClient = sqsClient;
    }

    public SnsSqsBenchmarkSqsProducer() {
        this(
            SnsSqsBenchmarkConfiguration.getSqsUri(),
            AmazonSQSClientBuilder
                .standard()
                .withRegion(SnsSqsBenchmarkConfiguration.getRegion())
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build()
        );
    }

    @Override
    public CompletableFuture<Void> sendAsync(Optional<String> key, byte[] payload) {
        SendMessageRequest request = new SendMessageRequest(sqsUri, new String(payload, StandardCharsets.UTF_8));
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            this.sqsClient.sendMessage(request);
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void close() throws Exception {}
}
