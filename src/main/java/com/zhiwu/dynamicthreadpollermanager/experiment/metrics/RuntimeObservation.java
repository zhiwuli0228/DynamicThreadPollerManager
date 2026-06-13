package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import java.time.Instant;
import java.util.Objects;

/**
 * Raw observation input supplied to the snapshot assembler. Each metric is
 * represented as a {@link MetricValue} so unavailable values can be carried
 * through to the recorded snapshot instead of being lost.
 */
public final class RuntimeObservation {

    private final Instant timestamp;
    private final MetricValue<Integer> activeThreads;
    private final MetricValue<Integer> poolSize;
    private final MetricValue<Integer> queueSize;
    private final MetricValue<Long> completedTaskCount;
    private final MetricValue<Double> cpuUtilization;
    private final MetricValue<Long> keepAliveTimeSeconds;
    private final MetricValue<Integer> largestPoolSize;
    private final MetricValue<Long> taskCount;

    public RuntimeObservation(Instant timestamp,
                              MetricValue<Integer> activeThreads,
                              MetricValue<Integer> queueSize,
                              MetricValue<Double> cpuUtilization) {
        this(timestamp, activeThreads, MetricValue.absent(), queueSize,
                MetricValue.absent(), cpuUtilization);
    }

    public RuntimeObservation(Instant timestamp,
                              MetricValue<Integer> activeThreads,
                              MetricValue<Integer> poolSize,
                              MetricValue<Integer> queueSize,
                              MetricValue<Long> completedTaskCount,
                              MetricValue<Double> cpuUtilization) {
        this(timestamp, activeThreads, poolSize, queueSize, completedTaskCount,
                cpuUtilization, MetricValue.absent(), MetricValue.absent(),
                MetricValue.absent());
    }

    public RuntimeObservation(Instant timestamp,
                              MetricValue<Integer> activeThreads,
                              MetricValue<Integer> poolSize,
                              MetricValue<Integer> queueSize,
                              MetricValue<Long> completedTaskCount,
                              MetricValue<Double> cpuUtilization,
                              MetricValue<Long> keepAliveTimeSeconds,
                              MetricValue<Integer> largestPoolSize,
                              MetricValue<Long> taskCount) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.activeThreads = Objects.requireNonNull(activeThreads, "activeThreads must not be null");
        this.poolSize = Objects.requireNonNull(poolSize, "poolSize must not be null");
        this.queueSize = Objects.requireNonNull(queueSize, "queueSize must not be null");
        this.completedTaskCount = Objects.requireNonNull(completedTaskCount, "completedTaskCount must not be null");
        this.cpuUtilization = Objects.requireNonNull(cpuUtilization, "cpuUtilization must not be null");
        this.keepAliveTimeSeconds = Objects.requireNonNull(keepAliveTimeSeconds, "keepAliveTimeSeconds must not be null");
        this.largestPoolSize = Objects.requireNonNull(largestPoolSize, "largestPoolSize must not be null");
        this.taskCount = Objects.requireNonNull(taskCount, "taskCount must not be null");
    }

    public Instant timestamp() { return timestamp; }
    public MetricValue<Integer> activeThreads() { return activeThreads; }
    public MetricValue<Integer> poolSize() { return poolSize; }
    public MetricValue<Integer> queueSize() { return queueSize; }
    public MetricValue<Long> completedTaskCount() { return completedTaskCount; }
    public MetricValue<Double> cpuUtilization() { return cpuUtilization; }
    public MetricValue<Long> keepAliveTimeSeconds() { return keepAliveTimeSeconds; }
    public MetricValue<Integer> largestPoolSize() { return largestPoolSize; }
    public MetricValue<Long> taskCount() { return taskCount; }

    public RuntimeObservation withTimestamp(Instant newTimestamp) {
        return new RuntimeObservation(newTimestamp, activeThreads, poolSize, queueSize,
                completedTaskCount, cpuUtilization, keepAliveTimeSeconds,
                largestPoolSize, taskCount);
    }
}
