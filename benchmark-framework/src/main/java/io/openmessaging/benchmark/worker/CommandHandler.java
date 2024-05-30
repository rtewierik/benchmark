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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandHandler {

    private final ExecutorService executorService;

    public CommandHandler(String poolName) {
        this.executorService =
                new ThreadPoolExecutor(
                        0, // Core pool size
                        1, // Maximum pool size
                        60L,
                        TimeUnit.SECONDS, // Keep-alive time for idle threads
                        new ArrayBlockingQueue<>(10), // Bounded queue for tasks
                        new DefaultThreadFactory(poolName),
                        new ThreadPoolExecutor.AbortPolicy() // Rejected execution policy
                );
    }

    public CommandHandler(int numConsumers, String poolName, Integer defaultThreadCapacity) {
        this.executorService =
                new ThreadPoolExecutor(
                        defaultThreadCapacity, // Core pool size
                        256, // Maximum pool size
                        0L,
                        TimeUnit.MILLISECONDS, // Keep-alive time for idle threads
                        new ArrayBlockingQueue<>(10000), // Bounded queue for tasks
                        new DefaultThreadFactory(poolName),
                        new ThreadPoolExecutor.AbortPolicy() // Rejected execution policy
                        );
    }

    public void handleCommand(Runnable command) {
        executorService.submit(command);
    }

    public void close() {
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
