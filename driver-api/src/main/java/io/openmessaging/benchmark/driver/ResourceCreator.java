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
package io.openmessaging.benchmark.driver;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ResourceCreator<R, C> {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final String name;
    private final int maxBatchSize;
    private final long interBatchDelayMs;
    private final Function<
                    List<Pair<Integer, R>>, Map<Pair<Integer, R>, CompletableFuture<Pair<Integer, C>>>>
            invokeBatchFn;
    private final Function<CompletableFuture<Pair<Integer, C>>, CreationResult<Pair<Integer, C>>>
            complete;

    public CompletableFuture<List<C>> create(List<R> resources) throws IOException {
        List<C> result = createBlocking(resources);
        log.info("Created {}/{} resources using ResourceCreator", result.size(), resources.size());
        return CompletableFuture.completedFuture(result);
    }

    private List<C> createBlocking(List<R> resources) throws IOException {
        List<Pair<Integer, R>> indexedResources = new ArrayList<>();
        for (int i = 0; i < resources.size(); i++) {
            indexedResources.add(new Pair<>(i, resources.get(i)));
        }
        BlockingQueue<Pair<Integer, R>> queue =
                new ArrayBlockingQueue<>(indexedResources.size(), true, indexedResources);
        List<Pair<Integer, R>> batch = new ArrayList<>();
        List<Pair<Integer, C>> created = new ArrayList<>();
        AtomicInteger succeeded = new AtomicInteger();

        ScheduledFuture<?> loggingFuture =
                executor.scheduleAtFixedRate(
                        () -> log.info("Created {}s {}/{}", name, succeeded.get(), resources.size()),
                        10,
                        10,
                        SECONDS);

        try {
            while (succeeded.get() < resources.size()) {
                int batchSize = queue.drainTo(batch, maxBatchSize);
                if (batchSize > 0) {
                    executeBatch(batch)
                            .forEach(
                                    (resource, result) -> {
                                        if (result.success) {
                                            created.add(result.created);
                                            succeeded.incrementAndGet();
                                        } else {
                                            //noinspection ResultOfMethodCallIgnored
                                            queue.offer(resource);
                                        }
                                    });
                    batch.clear();
                }
            }
        } catch (Throwable t) {
            String message = t.getMessage();
            String stackTrace = writer.writeValueAsString(t.getStackTrace());
            log.error(
                    "Error occurred while creating producer using ResourceCreator: {} {}",
                    message,
                    stackTrace);
        } finally {
            loggingFuture.cancel(true);
        }
        created.sort(Comparator.comparing(Pair::getKey));
        return created.stream().map(Pair::getValue).collect(Collectors.toList());
    }

    @SneakyThrows
    private Map<Pair<Integer, R>, CreationResult<Pair<Integer, C>>> executeBatch(
            List<Pair<Integer, R>> batch) {
        log.debug("Executing batch, size: {}", batch.size());
        Thread.sleep(interBatchDelayMs);
        return invokeBatchFn.apply(batch).entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> complete.apply(e.getValue())));
    }

    @Value
    public static class CreationResult<C> {
        C created;
        boolean success;
    }

    private static final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
}
