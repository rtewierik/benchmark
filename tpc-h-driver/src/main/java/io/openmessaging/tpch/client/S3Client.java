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
package io.openmessaging.tpch.client;


import java.io.InputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.crt.S3CrtRetryConfiguration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Slf4j
public class S3Client {
    private static final NettyNioAsyncHttpClient.Builder asyncHttpClientBuilder =
            NettyNioAsyncHttpClient.builder()
                    .maxConcurrency(500)
                    .maxPendingConnectionAcquires(10000)
                    .connectionMaxIdleTime(Duration.ofSeconds(600))
                    .connectionTimeout(Duration.ofSeconds(20))
                    .connectionAcquisitionTimeout(Duration.ofSeconds(60))
                    .readTimeout(Duration.ofSeconds(120));

    private static final long CLIENT_TIMEOUT_MILLIS = 600000;
    private static final int NUMBER_RETRIES = 60;
    private static final long RETRY_BACKOFF_MILLIS = 30000;
    private static final ClientOverrideConfiguration overrideConfiguration =
            ClientOverrideConfiguration.builder()
                    .apiCallTimeout(Duration.ofMillis(CLIENT_TIMEOUT_MILLIS))
                    .apiCallAttemptTimeout(Duration.ofMillis(CLIENT_TIMEOUT_MILLIS))
                    .retryPolicy(
                            RetryPolicy.builder()
                                    .numRetries(NUMBER_RETRIES)
                                    .backoffStrategy(
                                            FixedDelayBackoffStrategy.create(
                                                    Duration.of(RETRY_BACKOFF_MILLIS, ChronoUnit.MILLIS)))
                                    .throttlingBackoffStrategy(BackoffStrategy.none())
                                    .retryCondition(new AlwaysRetryCondition())
                                    .retryCapacityCondition(null)
                                    .build())
                    .build();

    S3AsyncClient s3AsyncClient =
            S3AsyncClient.crtBuilder()
                    .region(Region.EU_WEST_1)
                    .retryConfiguration(S3CrtRetryConfiguration.builder().numRetries(5).build())
                    .build();
    private final ExecutorService executor;
    private final Throttler throttler;

    public S3Client(ExecutorService executor) {
        this.throttler = new Throttler(2500, 1, TimeUnit.SECONDS);
        this.executor = executor;
    }

    public CompletableFuture<InputStream> getObject(GetObjectRequest request) {
        return executeThrottled(
                () ->
                        s3AsyncClient
                                .getObject(request, AsyncResponseTransformer.toBytes())
                                .thenApplyAsync(BytesWrapper::asInputStream, executor));
    }

    private <T> CompletableFuture<T> executeThrottled(Supplier<CompletableFuture<T>> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            throttler.acquire();
            supplier
                    .get()
                    .whenComplete(
                            (result, error) -> {
                                log.info("Finished downloading file.");
                                if (error != null) {
                                    future.completeExceptionally(error);
                                } else {
                                    future.complete(result);
                                }
                            });
        } catch (InterruptedException e) {
            future.completeExceptionally(e);
        }
        return future;
    }
}
