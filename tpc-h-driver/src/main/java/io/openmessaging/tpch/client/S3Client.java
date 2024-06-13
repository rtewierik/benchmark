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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    public class Buffer {
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
                    .minimumPartSizeInBytes(4L * 1024 * 1024) // 5 MB
                    .build();
    private static final Map<String, List<TpcHRow>> rowsMap = new HashMap<>();
    private static final ConcurrentLinkedQueue<Buffer> buffers = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService rowProcessor = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentMap<String, Future<?>> rowProcessorTasks = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final Throttler throttler;
    private final TpcHMessageProcessor processor;
    private ExecutorService executorOverride;

    public S3Client(ExecutorService executor, TpcHMessageProcessor processor) {
        this.executor = executor;
        this.throttler = new Throttler(2500, 1, TimeUnit.SECONDS);
        this.processor = processor;
    }

    public void startRowProcessor(ExecutorService executor) {
        log.info("Starting row processor from S3 client.");
        if (executor != null) {
            executorOverride = executor;
        }
        this.rowProcessor.scheduleAtFixedRate(() -> {
            log.info("Buffer size: {}", buffers.size());
            for (Buffer buffer : buffers) {
                Future<?> bufferProcessorTask = rowProcessorTasks.get(buffer.name);
                boolean isRunningTaskAbsent = bufferProcessorTask == null || bufferProcessorTask.isDone();
                if (isRunningTaskAbsent && !buffer.isDone.get()) {
                    log.info("Scheduling new task!");
                    rowProcessorTasks.put(buffer.name, submitRowProcessorTask(buffer));
                    log.info("Scheduled new task.");
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    public Future<?> submitRowProcessorTask(Buffer buffer) {
        ExecutorService executor = executorOverride != null ? executorOverride : this.executor;
        return executor.submit(() -> {
            try {
                while (true) {
                    StringBuilder data = buffer.data;
                    List<TpcHRow> rows = rowsMap.get(buffer.name);
                    int firstNewLineIndex = data.indexOf("\n");
                    if (firstNewLineIndex > 0) {
                        try {
                            String line = data.substring(0, firstNewLineIndex + 1);
                            data.delete(0, firstNewLineIndex + 1);
                            log.info("Length after delete: {}", data.length());
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
                        log.info("Buffer {} is {}. {}", buffer.name, buffer.isDone, firstNewLineIndex);
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
                        } else {
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                log.error("Exception occurred!", t);
            }
        });
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
        rowsMap.put(s3Uri, new ArrayList<>());
        buffers.add(buffer);
        log.info("Added {} to buffer. New size: {}", s3Uri, buffers.size());
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
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (long start = 0; start < objectSize; start += chunkSize) {
            long end = Math.min(start + chunkSize - 1, objectSize - 1);
            futures.add(readChunk(bucketName, key, start, end, buffer.data));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            log.info("Finished reading chunks, launching chunk processor... {}", System.currentTimeMillis());
            buffer.isDone.set(true);
        });
    }

    public CompletableFuture<Void> readChunk(
            String bucketName,
            String key,
            long start,
            long end,
            StringBuilder buffer) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .range("bytes=" + start + "-" + end)
                .build();

        return s3AsyncClient
                .getObject(getObjectRequest, AsyncResponseTransformer.toBytes())
                .thenApply(responseBytes -> {
                    ByteBuffer byteBuffer = responseBytes.asByteBuffer();
                    String data = StandardCharsets.UTF_8.decode(byteBuffer).toString();
                    // Should modify a buffer one at a time since this is run by consumer threads in blocking fashion.
                    buffer.append(data);
                    return null;
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
