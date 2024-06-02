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


import io.netty.util.concurrent.DefaultThreadFactory;
import io.openmessaging.benchmark.common.EnvironmentConfiguration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandHandler {

    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicInteger numCommandsSubmitted = new AtomicInteger();

    public CommandHandler(String poolName) {
        this.executorService =
                Objects.equals(poolName, "local-worker")
                        ? new ThreadPoolExecutor(
                                // Always one, and only one, thread required.
                                1,
                                1,
                                60L,
                                TimeUnit.SECONDS,
                                // The results reducer does not receive more than 1.000 reduced results.
                                new ArrayBlockingQueue<>(1000),
                                new DefaultThreadFactory(poolName),
                                new ThreadPoolExecutor.AbortPolicy())
                        : Executors.newCachedThreadPool(new DefaultThreadFactory(poolName));
    }

    public CommandHandler(Integer numWorkers, String poolName) {
        this.executorService =
                new ThreadPoolExecutor(
                        numWorkers,
                        numWorkers,
                        50L,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(50000),
                        new DefaultThreadFactory(poolName),
                        new ThreadPoolExecutor.AbortPolicy());
    }

    public void handleCommand(Runnable command) {
        executorService.submit(command);
        // submitTaskWithTimeoutAndRetry(scheduler, command, 1, TimeUnit.SECONDS, 3);
        Integer newNumCommandsSubmitted = numCommandsSubmitted.incrementAndGet();
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Number of commands submitted: {}", newNumCommandsSubmitted);
        }
    }

    public void submitTaskWithTimeoutAndRetry(
            ScheduledExecutorService scheduler,
            Runnable task,
            long timeout,
            TimeUnit timeUnit,
            int maxRetries) {
        final AtomicInteger retries = new AtomicInteger(0);

        Runnable taskWrapper =
                new Runnable() {
                    @SneakyThrows
                    @Override
                    public void run() {
                        Future<?> future = executorService.submit(task);

                        try {
                            future.get(timeout, timeUnit);
                        } catch (TimeoutException e) {
                            future.cancel(true);
                            if (retries.incrementAndGet() <= maxRetries) {
                                log.warn(String.format("Task timed out, retrying... (%d)", retries.get()));
                                scheduler.schedule(this, 1, TimeUnit.SECONDS);
                            } else {
                                log.warn("Task failed after max retries");
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            future.cancel(true);
                            log.warn("Task execution failed: {}", e.getMessage());
                        }
                    }
                };

        scheduler.submit(taskWrapper);
    }

    public void close() {
        log.info("Shutting down executor service...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(20, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
