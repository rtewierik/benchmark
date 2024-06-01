package io.openmessaging.benchmark.worker;


import com.google.common.util.concurrent.RateLimiter;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AdaptiveRateLimitedTaskProcessor {

    private final ThreadPoolExecutor executorService;
    private final AtomicInteger runningTasks;
    private final int maxConcurrentTasks;
    private final ScheduledExecutorService rateAdjustmentService;
    private RateLimiter rateLimiter;

    public AdaptiveRateLimitedTaskProcessor(int maxConcurrentTasks, double initialTasksPerSecond) {
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.rateLimiter = RateLimiter.create(initialTasksPerSecond);
        this.runningTasks = new AtomicInteger(0);
        this.executorService =
                new ThreadPoolExecutor(
                        maxConcurrentTasks,
                        maxConcurrentTasks,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>(),
                        new ThreadPoolExecutor.AbortPolicy());
        this.executorService.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        this.rateAdjustmentService = Executors.newScheduledThreadPool(1);
        this.rateAdjustmentService.scheduleAtFixedRate(this::adjustRateLimiter, 1, 1, TimeUnit.SECONDS);
    }

    public <T> CompletableFuture<T> submitTask(Callable<T> task) {
        rateLimiter.acquire();

        return CompletableFuture.supplyAsync(
                () -> {
                    runningTasks.incrementAndGet();
                    try {
                        return task.call();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    } finally {
                        runningTasks.decrementAndGet();
                    }
                },
                executorService);
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
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(20, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(20, TimeUnit.SECONDS)) {
                    System.err.println("Executor service did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
