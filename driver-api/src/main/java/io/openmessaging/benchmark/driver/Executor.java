package io.openmessaging.benchmark.driver;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Executor {

    public final ExecutorService regularExecutor;
    public final ScheduledExecutorService scheduledExecutor;

    public Executor(ExecutorService regularExecutor) {
        this.regularExecutor = regularExecutor;
        this.scheduledExecutor = null;
    }

    public Executor(ScheduledExecutorService scheduledExecutor) {
        this.regularExecutor = null;
        this.scheduledExecutor = scheduledExecutor;
    }

    public Future<?> submit(Runnable task) {
        if (this.regularExecutor != null) {
            return this.regularExecutor.submit(task);
        }
        return this.scheduledExecutor.scheduleAtFixedRate(task, 0, 0, TimeUnit.MILLISECONDS);
    }
}
