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


import java.util.concurrent.Semaphore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdaptiveRateLimitedTaskProcessor {

    private final Semaphore semaphore;

    public AdaptiveRateLimitedTaskProcessor(int maxConcurrentTasks) {
        log.info("Initialising with {} max concurrent tasks", maxConcurrentTasks);
        this.semaphore = new Semaphore(maxConcurrentTasks);
    }

    public void startNewTask() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Error occurred starting new task.", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public void finishedRunningTask() {
        this.semaphore.release();
    }
}
