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

import io.openmessaging.benchmark.common.client.AmazonS3Client;
import io.openmessaging.benchmark.driver.BenchmarkProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class S3BenchmarkS3Producer implements BenchmarkProducer {

    private static final AmazonS3Client s3Client = new AmazonS3Client();
    private final String bucketName;
    private final String key;

    public S3BenchmarkS3Producer(String s3Uri) {
        URI uri = URI.create(s3Uri);
        this.bucketName = uri.getHost();
        this.key = uri.getPath().substring(1);
    }

    @Override
    public CompletableFuture<Void> sendAsync(Optional<String> key, byte[] payload) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            s3Client.writeMessageToS3(this.bucketName, this.key, payload);
            future.complete(null);
        } catch (Exception e) {
            log.error("Failed sendAsync for {} {} due to {}", this.bucketName, this.key, e.getMessage());
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void close() throws Exception {}

    private static final Logger log = LoggerFactory.getLogger(S3BenchmarkS3Producer.class);
}
