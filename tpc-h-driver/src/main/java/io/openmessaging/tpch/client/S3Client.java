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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class S3Client {
    private static final S3AsyncClient s3AsyncClient =
            S3AsyncClient.builder()
                    .httpClientBuilder(
                            NettyNioAsyncHttpClient.builder()
                                    .maxConcurrency(512)
                                    .maxPendingConnectionAcquires(512)
                                    .connectionAcquisitionTimeout(Duration.ofSeconds(30)))
                    .build();
    private final Throttler throttler;

    public S3Client() {
        this.throttler = new Throttler(1000, 1, TimeUnit.SECONDS);
    }

    public CompletableFuture<InputStream> getObject(GetObjectRequest request) {
        return executeThrottled(
                () ->
                        s3AsyncClient
                                .getObject(request, AsyncResponseTransformer.toBytes())
                                .thenApply(BytesWrapper::asInputStream));
    }

    private <T> CompletableFuture<T> executeThrottled(Supplier<CompletableFuture<T>> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            throttler.acquire();
            supplier
                    .get()
                    .whenComplete(
                            (result, error) -> {
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
