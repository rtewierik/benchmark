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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandHandler {

    private final ExecutorService executorService;
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
                        0L,
                        TimeUnit.SECONDS, // Keep-alive time for idle threads
                        new ArrayBlockingQueue<>(50000), // Bounded queue for tasks
                        new DefaultThreadFactory(poolName),
                        new ThreadPoolExecutor.AbortPolicy() // Rejected execution policy
                        );
    }

    public void handleCommand(Runnable command) {
        executorService.submit(command);
        Integer latest = numCommandsSubmitted.incrementAndGet();
        log.info("Number of commands submitted: {}", latest);
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
