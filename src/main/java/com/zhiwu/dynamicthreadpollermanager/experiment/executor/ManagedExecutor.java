package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ManagedExecutor implements AutoCloseable {

    private final ThreadPoolExecutor executor;
    private final int queueCapacity;
    private final RejectedExecutionHandler rejectionPolicy;

    public ManagedExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime,
                           TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maxPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
    }

    public ManagedExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime,
                           TimeUnit unit, BlockingQueue<Runnable> workQueue,
                           ThreadFactory threadFactory, RejectedExecutionHandler rejectionHandler) {
        Objects.requireNonNull(unit, "time unit must not be null");
        Objects.requireNonNull(workQueue, "workQueue must not be null");
        Objects.requireNonNull(threadFactory, "threadFactory must not be null");
        Objects.requireNonNull(rejectionHandler, "rejectionHandler must not be null");
        if (corePoolSize < 1) {
            throw new IllegalArgumentException("corePoolSize must be >= 1, was " + corePoolSize);
        }
        if (maxPoolSize < corePoolSize) {
            throw new IllegalArgumentException(
                    "maxPoolSize must be >= corePoolSize, was " + maxPoolSize);
        }
        if (keepAliveTime < 0) {
            throw new IllegalArgumentException("keepAliveTime must be >= 0, was " + keepAliveTime);
        }
        this.executor = new ThreadPoolExecutor(
                corePoolSize, maxPoolSize, keepAliveTime, unit,
                workQueue, threadFactory, rejectionHandler);
        this.queueCapacity = workQueue.remainingCapacity() + workQueue.size();
        this.rejectionPolicy = rejectionHandler;
    }

    public <T> Future<T> submit(Callable<T> task) {
        Objects.requireNonNull(task, "task must not be null");
        return executor.submit(task);
    }

    public Future<?> submit(Runnable task) {
        Objects.requireNonNull(task, "task must not be null");
        return executor.submit(task);
    }

    public int getCorePoolSize() {
        return executor.getCorePoolSize();
    }

    public void setCorePoolSize(int corePoolSize) {
        executor.setCorePoolSize(corePoolSize);
    }

    public int getMaximumPoolSize() {
        return executor.getMaximumPoolSize();
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        executor.setMaximumPoolSize(maximumPoolSize);
    }

    public long getKeepAliveTime(TimeUnit unit) {
        return executor.getKeepAliveTime(unit);
    }

    public void setKeepAliveTime(long time, TimeUnit unit) {
        executor.setKeepAliveTime(time, unit);
    }

    public int getActiveCount() {
        return executor.getActiveCount();
    }

    public int getPoolSize() {
        return executor.getPoolSize();
    }

    public int getQueueSize() {
        return executor.getQueue().size();
    }

    public long getCompletedTaskCount() {
        return executor.getCompletedTaskCount();
    }

    public int getLargestPoolSize() {
        return executor.getLargestPoolSize();
    }

    public long getTaskCount() {
        return executor.getTaskCount();
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public RejectedExecutionHandler getRejectionPolicy() {
        return rejectionPolicy;
    }

    public void shutdown() {
        executor.shutdown();
    }

    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }

    public boolean isStopped() {
        return executor.isTerminating() || executor.isTerminated();
    }

    public boolean isTerminated() {
        return executor.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    public ThreadPoolExecutor unwrap() {
        return executor;
    }

    public ExecutorStateSnapshot toSnapshot() {
        return ExecutorStateSnapshot.builder(Instant.now())
                .corePoolSize(getCorePoolSize())
                .maximumPoolSize(getMaximumPoolSize())
                .activeCount(getActiveCount())
                .poolSize(getPoolSize())
                .queueSize(getQueueSize())
                .queueCapacity(getQueueCapacity())
                .completedTaskCount(getCompletedTaskCount())
                .keepAliveTimeSeconds(getKeepAliveTime(TimeUnit.SECONDS))
                .largestPoolSize(getLargestPoolSize())
                .taskCount(getTaskCount())
                .build();
    }

    @Override
    public void close() {
        shutdown();
    }
}
