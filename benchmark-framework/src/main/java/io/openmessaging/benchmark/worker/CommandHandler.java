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
package io.openmessaging.benchmark.worker;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandHandler {

    private final ThreadPoolExecutor executor;
    private Map<String, Integer> counts = new HashMap<>();

    public CommandHandler(String poolName) {
        this.executor =
                new ThreadPoolExecutor(
                        // Always one, and only one, thread required.
                        1,
                        1,
                        60L,
                        TimeUnit.SECONDS,
                        // The results reducer does not receive more than 1.000 reduced results.
                        new ArrayBlockingQueue<>(10000),
                        r -> new Thread(r, poolName),
                        new ThreadPoolExecutor.AbortPolicy());
    }

    public CommandHandler(Integer numConsumers, String poolName) {
        this.executor =
                new ThreadPoolExecutor(
                        16,
                        numConsumers, // Maximum pool size
                        60L,
                        TimeUnit.SECONDS, // Keep-alive time for idle threads
                        new ArrayBlockingQueue<>(50000), // Bounded queue for tasks
                        r -> new Thread(r, poolName),
                        new ThreadPoolExecutor.AbortPolicy() // Rejected execution policy
                        );
    }

    public void handleCommand(String source, Runnable command) throws InterruptedException {
        if (!counts.containsKey(source)) {
            counts.put(source, 0);
        }
        counts.put(source, counts.get(source) + 1);
        Future<?> future = executor.submit(command);
        CompletableFuture.supplyAsync(() -> {
            try {
                return future.get(3, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException ignored) {}
            return null;
        });
        log.info("==== COUNTS ====");
        counts.forEach((key, value) -> log.info(String.format("%s: %d", key, value)));
    }

    public void close() {
        log.info("Shutting down executor service...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(20, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
