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
    private final MetricValue<Integer> queueSize;
    private final MetricValue<Double> cpuUtilization;

    public RuntimeObservation(Instant timestamp,
                              MetricValue<Integer> activeThreads,
                              MetricValue<Integer> queueSize,
                              MetricValue<Double> cpuUtilization) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.activeThreads = Objects.requireNonNull(activeThreads, "activeThreads must not be null");
        this.queueSize = Objects.requireNonNull(queueSize, "queueSize must not be null");
        this.cpuUtilization = Objects.requireNonNull(cpuUtilization, "cpuUtilization must not be null");
    }

    public Instant timestamp() {
        return timestamp;
    }

    public MetricValue<Integer> activeThreads() {
        return activeThreads;
    }

    public MetricValue<Integer> queueSize() {
        return queueSize;
    }

    public MetricValue<Double> cpuUtilization() {
        return cpuUtilization;
    }

    public RuntimeObservation withTimestamp(Instant newTimestamp) {
        return new RuntimeObservation(newTimestamp, activeThreads, queueSize, cpuUtilization);
    }
}
