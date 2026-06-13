package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.acquisition.RunManifest;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public record ManagedExecutorConfig(
        int corePoolSize,
        int maximumPoolSize,
        int queueCapacity,
        long keepAliveTime,
        TimeUnit keepAliveTimeUnit,
        ThreadMode threadMode
) {
    public ManagedExecutorConfig(int corePoolSize, int maximumPoolSize,
                                  int queueCapacity, long keepAliveTime,
                                  TimeUnit keepAliveTimeUnit) {
        this(corePoolSize, maximumPoolSize, queueCapacity, keepAliveTime,
                keepAliveTimeUnit, ThreadMode.PLATFORM);
    }

    public ManagedExecutorConfig {
        if (corePoolSize <= 0) {
            throw new IllegalArgumentException("corePoolSize must be positive, was " + corePoolSize);
        }
        if (maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException(
                    "maximumPoolSize must be >= corePoolSize, was " + maximumPoolSize);
        }
        if (queueCapacity < 0) {
            throw new IllegalArgumentException("queueCapacity must be non-negative, was " + queueCapacity);
        }
        if (keepAliveTime < 0) {
            throw new IllegalArgumentException("keepAliveTime must be non-negative, was " + keepAliveTime);
        }
        Objects.requireNonNull(keepAliveTimeUnit, "keepAliveTimeUnit must not be null");
        Objects.requireNonNull(threadMode, "threadMode must not be null");
    }

    public static ManagedExecutorConfig defaultConfig() {
        return new ManagedExecutorConfig(2, 4, 10, 60, TimeUnit.SECONDS);
    }

    public ManagedExecutor toManagedExecutor() {
        if (threadMode == ThreadMode.VIRTUAL) {
            return ManagedExecutor.virtual(maximumPoolSize, queueCapacity,
                    keepAliveTime, keepAliveTimeUnit,
                    new ThreadPoolExecutor.AbortPolicy());
        }
        return new ManagedExecutor(corePoolSize, maximumPoolSize,
                keepAliveTime, keepAliveTimeUnit,
                new LinkedBlockingQueue<>(queueCapacity));
    }

    public RunManifest.BaselinePresetSummary toPresetSummary() {
        return new RunManifest.BaselinePresetSummary(
                "managed-executor-v0.8.0",
                corePoolSize,
                maximumPoolSize,
                queueCapacity);
    }
}
