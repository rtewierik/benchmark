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
                new ThreadPoolExecutor(
                        // Always one, and only one, thread required.
                        1, 1, 60L, TimeUnit.SECONDS,
                        // The results reducer does not receive more than 1.000 reduced results.
                        new ArrayBlockingQueue<>(50000),
                        new DefaultThreadFactory(poolName),
                        new ThreadPoolExecutor.AbortPolicy()
                );
    }

    public CommandHandler(Integer numConsumers, String poolName) {
        this.executorService =
                new ThreadPoolExecutor(
                        numConsumers,
                        numConsumers, // Maximum pool size
                        50L,
                        TimeUnit.MILLISECONDS, // Keep-alive time for idle threads
                        new ArrayBlockingQueue<>(50000), // Bounded queue for tasks
                        new DefaultThreadFactory(poolName),
                        new ThreadPoolExecutor.AbortPolicy() // Rejected execution policy
                        );
    }

    public void handleCommand(Runnable command) {
        submitTaskWithTimeoutAndRetry(scheduler, command, 1, TimeUnit.SECONDS, 10);
        Integer latest = numCommandsSubmitted.incrementAndGet();
        log.info("Number of commands submitted: {}", latest);
    }

    public void submitTaskWithTimeoutAndRetry(ScheduledExecutorService scheduler,
                                             Runnable task,
                                             long timeout,
                                             TimeUnit timeUnit,
                                             int maxRetries) {
        final AtomicInteger retries = new AtomicInteger(0);

        Runnable taskWrapper = new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                Future<?> future = executorService.submit(task);

                try {
                    // Wait for the task to complete within the timeout
                    future.get(timeout, timeUnit);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    if (retries.incrementAndGet() <= maxRetries) {
                        System.out.println("Task timed out, retrying... (" + retries.get() + ")");
                        scheduler.schedule(this, 1, TimeUnit.SECONDS);
                    } else {
                        System.out.println("Task failed after max retries");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    future.cancel(true);
                    System.out.println("Task execution failed: " + e.getMessage());
                }
            }
        };

        // Submit the initial task
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
