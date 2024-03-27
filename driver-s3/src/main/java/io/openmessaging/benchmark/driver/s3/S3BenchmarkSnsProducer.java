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

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import io.openmessaging.benchmark.driver.BenchmarkProducer;
import io.openmessaging.tpch.client.AmazonS3Client;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class S3BenchmarkSnsProducer implements BenchmarkProducer {

    private final AmazonS3Client s3Client;
    private final String snsUri;

    public S3BenchmarkSnsProducer(String snsUri) {
        this.s3Client = new AmazonS3Client();
        this.snsUri = snsUri;
    }

    @Override
    public CompletableFuture<Void> sendAsync(Optional<String> key, byte[] payload) {
        PublishRequest request = new PublishRequest(snsUri, new String(payload, StandardCharsets.UTF_8));
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            this.s3C.publish(request);
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void close() throws Exception {}
}
