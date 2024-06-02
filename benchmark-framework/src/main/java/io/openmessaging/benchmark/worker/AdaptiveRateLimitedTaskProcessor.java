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


import com.google.common.util.concurrent.RateLimiter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AdaptiveRateLimitedTaskProcessor {

    private final AtomicInteger runningTasks;
    private final int maxConcurrentTasks;
    private final ScheduledExecutorService rateAdjustmentService;
    private final RateLimiter rateLimiter;

    public AdaptiveRateLimitedTaskProcessor(int maxConcurrentTasks, double initialTasksPerSecond) {
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.rateLimiter = RateLimiter.create(initialTasksPerSecond);
        this.runningTasks = new AtomicInteger(0);
        this.rateAdjustmentService = Executors.newScheduledThreadPool(1);
        this.rateAdjustmentService.scheduleAtFixedRate(this::adjustRateLimiter, 1, 1, TimeUnit.SECONDS);
    }

    public void finishedRunningTask() {
        this.runningTasks.decrementAndGet();
    }

    public void startNewTask() {
        rateLimiter.acquire();
        runningTasks.incrementAndGet();
    }

    private void adjustRateLimiter() {
        int currentlyRunningTasks = runningTasks.get();
        double currentRate = rateLimiter.getRate();

        if (currentlyRunningTasks >= maxConcurrentTasks * 0.8) {
            // Reduce rate if we're nearing the max capacity
            rateLimiter.setRate(Math.max(currentRate / 2, 1.0));
        } else if (currentlyRunningTasks <= maxConcurrentTasks * 0.5) {
            // Increase rate if we're well below capacity
            rateLimiter.setRate(currentRate * 1.5);
        }
    }

    public void shutdown() {
        rateAdjustmentService.shutdown();
    }
}
