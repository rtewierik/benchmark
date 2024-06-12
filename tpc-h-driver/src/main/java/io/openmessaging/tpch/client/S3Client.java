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


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import io.openmessaging.tpch.algorithm.TpcHDataParser;
import io.openmessaging.tpch.model.TpcHConsumerAssignment;
import io.openmessaging.tpch.model.TpcHRow;
import io.openmessaging.tpch.processing.TpcHMessageProcessor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

@Slf4j
public class S3Client {

    private class Buffer {
        public final String name;
        public final TpcHConsumerAssignment assignment;
        public final StringBuilder data;
        public AtomicBoolean isDone = new AtomicBoolean(false);

        Buffer(String name, TpcHConsumerAssignment assignment, StringBuilder data) {
            this.name = name;
            this.assignment = assignment;
            this.data = data;
        }
    }

    private static final S3AsyncClient s3AsyncClient =
            S3AsyncClient.crtBuilder()
                    .region(Region.EU_WEST_1)
                    .maxConcurrency(2048)
                    .targetThroughputInGbps(25.0)
                    .maxNativeMemoryLimitInBytes(4L * 1024 * 1024 * 1024) // 3 GB
                    .initialReadBufferSizeInBytes(64L * 1024 * 1024) // 64 MB
                    .minimumPartSizeInBytes(8L * 1024 * 1024) // 8 MB
                    .build();
    private static final Map<String, ConcurrentLinkedQueue<TpcHRow>> rowsMap = new HashMap<>();
    private static final ConcurrentLinkedQueue<Buffer> buffers = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService rowProcessor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService executor;
    private final Throttler throttler;
    private final TpcHMessageProcessor processor;

    public S3Client(ExecutorService executor, TpcHMessageProcessor processor) {
        this.executor = executor;
        this.throttler = new Throttler(2500, 1, TimeUnit.SECONDS);
        this.processor = processor;
    }

    public void startRowProcessor() {
        this.rowProcessor.scheduleWithFixedDelay(() -> {
            try {
                while (true) {
                    boolean allNotReady = true;
                    for (Buffer buffer : buffers) {
                        StringBuilder data = buffer.data;
                        ConcurrentLinkedQueue<TpcHRow> rows = rowsMap.get(buffer.name);
                        int lastNewlineIndex = data.indexOf("\n");
                        if (lastNewlineIndex > 0) {
                            try {
                                allNotReady = false;
                                String line = data.substring(0, lastNewlineIndex + 1);
                                data.delete(0, lastNewlineIndex + 1);
                                if (line.isEmpty()) {
                                    continue;
                                }
                                TpcHRow row = TpcHDataParser.readTpcHRowFromLine(line);
                                rows.add(row);
                            } catch (IOException exception) {
                                log.error("Error occurred while trying to parse TPC-H row.", exception);
                                throw new RuntimeException(exception);
                            }
                        } else {
                            log.info("Buffer {} is {}. {}", buffer.name, buffer.isDone, lastNewlineIndex);
                            if (buffer.isDone.get()) {
                                try {
                                    log.info("Processing consumer assignment chunk {}...", buffer.name);
                                    processor.processConsumerAssignmentChunk(rows, buffer.assignment);
                                    rowsMap.remove(buffer.name);
                                    buffers.remove(buffer);
                                } catch (Exception exception) {
                                    log.error("Exception occurred while processing consumer assignment chunk.",
                                            exception);
                                    throw new RuntimeException(exception);
                                }
                            }
                        }
                    }
                    if (allNotReady) {
                        int size = buffers.size();
                        if (size != 0) {
                            log.info("Interrupting processor iteration. {}", size);
                        }
                        break;
                    }
                }
            } catch (Throwable t) {
                log.error("Exception occurred!", t);
            }
        }, 0, 10, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        rowProcessor.shutdown();
    }

    public CompletableFuture<InputStream> getObject(GetObjectRequest request) {
        return executeThrottled(
                () ->
                        s3AsyncClient
                                .getObject(request, AsyncResponseTransformer.toBytes())
                                .thenApplyAsync(BytesWrapper::asInputStream, executor));
    }

    public CompletableFuture<Void> fetchAndProcessCsvInChunks(TpcHConsumerAssignment assignment, int chunkSize)
            throws URISyntaxException {
        String s3Uri = assignment.sourceDataS3Uri;
        URI uri = new URI(s3Uri);
        String bucketName = uri.getHost();
        String key = uri.getPath().substring(1);
        StringBuilder data = new StringBuilder();
        Buffer buffer = new Buffer(s3Uri, assignment, data);
        rowsMap.put(s3Uri, new ConcurrentLinkedQueue<>());
        buffers.add(buffer);
        HeadObjectRequest headObjectRequest = HeadObjectRequest
                .builder()
                .bucket(bucketName)
                .key(key)
                .build();

        CompletableFuture<HeadObjectResponse> headObjectResponseFuture = s3AsyncClient.headObject(headObjectRequest);

        return headObjectResponseFuture.thenCompose(headObjectResponse -> {
            long objectSize = headObjectResponse.contentLength();
            return readCsvInChunks(buffer, bucketName, key, objectSize, chunkSize);
        });
    }

    public CompletableFuture<Void> readCsvInChunks(
            Buffer buffer, String bucketName, String key, long objectSize, int chunkSize) {
        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (long start = 0; start < objectSize; start += chunkSize) {
            long end = Math.min(start + chunkSize - 1, objectSize - 1);
            futures.add(readChunk(bucketName, key, start, end));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            for (CompletableFuture<String> f : futures) {
                try {
                    buffer.data.append(f.get());
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error occurred while appending data to buffer.", e);
                    throw new RuntimeException(e);
                }
            }
            log.info("Finished reading chunks, launching chunk processor... {}", System.currentTimeMillis());
            buffer.isDone.set(true);
        });
    }

    public CompletableFuture<String> readChunk(
            String bucketName,
            String key,
            long start,
            long end) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .range("bytes=" + start + "-" + end)
                .build();

        return s3AsyncClient
                .getObject(getObjectRequest, AsyncResponseTransformer.toBytes())
                .thenApply(responseBytes -> {
                    ByteBuffer byteBuffer = responseBytes.asByteBuffer();
                    return StandardCharsets.UTF_8.decode(byteBuffer).toString();
                });
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
