package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Replaces an executor's work queue by decommissioning the old
 * ThreadPoolExecutor and commissioning a new one with the resized
 * queue. Preserves core/max/keepAlive/threadFactory configuration.
 */
public final class ExecutorRebuildStrategy {

    private final ExecutorRegistry registry;
    private final Supplier<Instant> clock;

    public ExecutorRebuildStrategy(ExecutorRegistry registry, Supplier<Instant> clock) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public RebuildResult rebuild(String executorId, ManagedExecutor oldExecutor,
                                  QueueResizeCommand command) {
        Instant startTime = clock.get();

        ExecutorStateSnapshot beforeState = oldExecutor.toSnapshot();
        int oldQueueCapacity = oldExecutor.getQueueCapacity();
        QueueResizeCommand.Direction direction = command.direction(oldQueueCapacity);
        ThreadPoolExecutor oldTpe = oldExecutor.unwrap();
        int corePoolSize = oldExecutor.getCorePoolSize();
        int maxPoolSize = oldExecutor.getMaximumPoolSize();
        long keepAliveMs = oldExecutor.getKeepAliveTime(TimeUnit.MILLISECONDS);

        // --- Decommission ---
        oldExecutor.shutdown();

        List<Runnable> drainedTasks = new ArrayList<>();
        BlockingQueue<Runnable> oldQueue = oldTpe.getQueue();
        oldQueue.drainTo(drainedTasks);

        boolean terminated = false;
        try {
            terminated = oldExecutor.awaitTermination(command.timeoutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (!terminated) {
            oldExecutor.shutdownNow();
            try {
                oldExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // --- Commission ---
        ManagedExecutor newExecutor;
        try {
            newExecutor = new ManagedExecutor(
                    corePoolSize,
                    maxPoolSize,
                    keepAliveMs, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(command.targetQueueCapacity()),
                    oldTpe.getThreadFactory(),
                    new ThreadPoolExecutor.AbortPolicy());
        } catch (Exception e) {
            long durationMs = Duration.between(startTime, clock.get()).toMillis();
            return new RebuildResult(
                    false, beforeState, null, durationMs,
                    drainedTasks.size(), drainedTasks.size(),
                    direction, oldQueueCapacity, command.targetQueueCapacity(),
                    "Commission failed: " + e.getMessage());
        }

        registry.remove(executorId);
        registry.register(executorId, newExecutor);

        // --- Replay drained tasks ---
        int replayedCount = 0;
        int rejectedCount = 0;
        if (!drainedTasks.isEmpty() && direction == QueueResizeCommand.Direction.EXPAND) {
            for (Runnable task : drainedTasks) {
                try {
                    newExecutor.submit(task);
                    replayedCount++;
                } catch (RejectedExecutionException e) {
                    rejectedCount++;
                }
            }
        }

        ExecutorStateSnapshot afterState = newExecutor.toSnapshot();
        long durationMs = Duration.between(startTime, clock.get()).toMillis();

        return new RebuildResult(
                true, beforeState, afterState, durationMs,
                drainedTasks.size(), rejectedCount,
                direction, oldQueueCapacity, command.targetQueueCapacity(),
                null);
    }
}
