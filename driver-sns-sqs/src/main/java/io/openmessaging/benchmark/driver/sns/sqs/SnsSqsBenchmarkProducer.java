package io.openmessaging.benchmark.driver.sns.sqs;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import io.openmessaging.benchmark.driver.BenchmarkProducer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SnsSqsBenchmarkProducer implements BenchmarkProducer {

    private final AmazonSQS sqsClient;

    public SnsSqsBenchmarkProducer() {
        sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();
    }

    @Override
    public CompletableFuture<Void> sendAsync(Optional<String> key, byte[] payload) {
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}
