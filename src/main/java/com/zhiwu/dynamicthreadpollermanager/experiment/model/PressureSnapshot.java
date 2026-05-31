package com.zhiwu.dynamicthreadpollermanager.experiment.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable snapshot of thread pool pressure metrics at a point in time.
 */
public final class PressureSnapshot {

    private final Instant timestamp;
    private final int activeThreads;
    private final int queueSize;
    private final double cpuUtilization;

    public PressureSnapshot(Instant timestamp, int activeThreads, int queueSize, double cpuUtilization) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.activeThreads = activeThreads;
        this.queueSize = queueSize;
        this.cpuUtilization = cpuUtilization;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public int activeThreads() {
        return activeThreads;
    }

    public int queueSize() {
        return queueSize;
    }

    public double cpuUtilization() {
        return cpuUtilization;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof PressureSnapshot that
                && timestamp.equals(that.timestamp)
                && activeThreads == that.activeThreads
                && queueSize == that.queueSize
                && Double.compare(that.cpuUtilization, cpuUtilization) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, activeThreads, queueSize, cpuUtilization);
    }

    @Override
    public String toString() {
        return "PressureSnapshot{timestamp=%s, activeThreads=%d, queueSize=%d, cpuUtilization=%.2f}"
                .formatted(timestamp, activeThreads, queueSize, cpuUtilization);
    }
}
