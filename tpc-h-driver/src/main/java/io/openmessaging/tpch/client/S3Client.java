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
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import io.openmessaging.tpch.algorithm.TpcHAlgorithm;
import io.openmessaging.tpch.model.TpcHConsumerAssignment;
import io.openmessaging.tpch.model.TpcHIntermediateResult;
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

    private static final S3AsyncClient s3AsyncClient =
            S3AsyncClient.crtBuilder()
                    .region(Region.EU_WEST_1)
                    .maxConcurrency(2048)
                    .targetThroughputInGbps(8.125)
                    .maxNativeMemoryLimitInBytes(5L * 1024 * 1024 * 1024) // 3 GB
                    .initialReadBufferSizeInBytes(32L * 1024 * 1024) // 64 MB
                    .minimumPartSizeInBytes(5L * 1024 * 1024) // 5 MB
                    .build();
    private final ScheduledExecutorService rowProcessor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService executor;
    private final Throttler throttler;
    private final TpcHMessageProcessor processor;

    public S3Client(ExecutorService executor, TpcHMessageProcessor processor) {
        this.executor = executor;
        this.throttler = new Throttler(2500, 1, TimeUnit.SECONDS);
        this.processor = processor;
    }

    public void shutdown() {
        rowProcessor.shutdown();
    }

    public CompletableFuture<TpcHIntermediateResult> getIntermediateResultFromS3(TpcHConsumerAssignment assignment) {
        return executeThrottled(
                () -> {
                    try {
                        String s3Uri = assignment.sourceDataS3Uri;
                        URI uri = new URI(s3Uri);
                        String bucketName = uri.getHost();
                        String key = uri.getPath().substring(1);
                        GetObjectRequest request = GetObjectRequest.builder().bucket(bucketName).key(key).build();
                        return s3AsyncClient
                                .getObject(request, AsyncResponseTransformer.toBytes())
                                .thenApplyAsync((response) -> {
                                    try (Reader reader = new InputStreamReader(response.asInputStream())) {
                                        CsvToBean<TpcHRow> csvToBean = new CsvToBeanBuilder<TpcHRow>(reader)
                                                .withType(TpcHRow.class)
                                                .withSeparator('|')
                                                .withIgnoreLeadingWhiteSpace(true)
                                                .withSkipLines(0)
                                                .build();
                                        Iterator<TpcHRow> iterator = csvToBean.iterator();
                                        return TpcHAlgorithm.applyQueryToChunk(iterator, assignment.query, assignment);
                                    } catch (Throwable t) {
                                        log.error("Error occurred while attempting to parse chunk.", t);
                                        throw new RuntimeException(t);
                                    }
                                }, executor);
                    } catch (Throwable t) {
                        log.error("Error occurred while getting intermediate result from S3.", t);
                        throw new RuntimeException(t);
                    }
                });
    }

    public CompletableFuture<TpcHIntermediateResult> fetchAndProcessCsvInChunks(
            TpcHConsumerAssignment assignment, int chunkSize) throws URISyntaxException {
        log.info("Starting to process CSV in chunks ({}).", assignment.sourceDataS3Uri);
        String s3Uri = assignment.sourceDataS3Uri;
        URI uri = new URI(s3Uri);
        String bucketName = uri.getHost();
        String key = uri.getPath().substring(1);
        HeadObjectRequest headObjectRequest = HeadObjectRequest
                .builder()
                .bucket(bucketName)
                .key(key)
                .build();

        CompletableFuture<HeadObjectResponse> headObjectResponseFuture = s3AsyncClient.headObject(headObjectRequest);

        return headObjectResponseFuture.thenCompose(headObjectResponse -> {
            long objectSize = headObjectResponse.contentLength();
            return readCsvInChunks(bucketName, key, objectSize, chunkSize, assignment);
        });
    }

    public CompletableFuture<TpcHIntermediateResult> readCsvInChunks(
            String bucketName, String key, long objectSize, int chunkSize, TpcHConsumerAssignment assignment) {
        List<CompletableFuture<TpcHIntermediateResult>> futures = new ArrayList<>();

        for (long start = 0; start < objectSize; start += chunkSize) {
            long end = Math.min(start + chunkSize - 1, objectSize - 1);
            futures.add(readChunk(bucketName, key, start, end, assignment));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply((ignored) -> {
            log.info("Finished reading chunks, aggregating chunks... {}", System.currentTimeMillis());
            try {
                TpcHIntermediateResult result = futures.get(0).get();
                for (int i = 1; i < futures.size(); i++) {
                    result.aggregateChunkResult(futures.get(i).get());
                }
                // processor.processConsumerAssignmentChunk(result, assignment);
                return result;
            } catch (Throwable t) {
                log.error("Exception occurred while aggregating chunks.", t);
                throw new RuntimeException(t);
            }
        });
    }

    public CompletableFuture<TpcHIntermediateResult> readChunk(
            String bucketName, String key, long start, long end, TpcHConsumerAssignment assignment) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .range("bytes=" + start + "-" + end)
                .build();

        return s3AsyncClient
                .getObject(getObjectRequest, AsyncResponseTransformer.toBytes())
                .thenApply(responseBytes -> {
                    try (Reader reader = new InputStreamReader(responseBytes.asInputStream())) {
                        CsvToBean<TpcHRow> csvToBean = new CsvToBeanBuilder<TpcHRow>(reader)
                                .withType(TpcHRow.class)
                                .withSeparator('|')
                                .withIgnoreLeadingWhiteSpace(true)
                                .withSkipLines(0)
                                .build();
                        Iterator<TpcHRow> iterator = csvToBean.iterator();
                        return TpcHAlgorithm.applyQueryToChunk(iterator, assignment.query, assignment);
                    } catch (Throwable t) {
                        log.error("Error occurred while attempting to parse chunk.", t);
                        throw new RuntimeException(t);
                    }
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
