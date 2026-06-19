package com.zhiwu.dynamicthreadpollermanager.experiment.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable record of an adjustment event applied to the thread pool during an experiment.
 */
public final class AdjustmentEvent {

    private final Instant timestamp;
    private final String runId;
    private final int previousPoolSize;
    private final int newPoolSize;
    private final String adjustmentReason;

    public AdjustmentEvent(Instant timestamp, String runId, int previousPoolSize,
                           int newPoolSize, String adjustmentReason) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.previousPoolSize = previousPoolSize;
        this.newPoolSize = newPoolSize;
        this.adjustmentReason = adjustmentReason;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public String runId() {
        return runId;
    }

    public int previousPoolSize() {
        return previousPoolSize;
    }

    public int newPoolSize() {
        return newPoolSize;
    }

    public String adjustmentReason() {
        return adjustmentReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof AdjustmentEvent that
                && timestamp.equals(that.timestamp)
                && runId.equals(that.runId)
                && newPoolSize == that.newPoolSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, runId, newPoolSize);
    }

    @Override
    public String toString() {
        return "AdjustmentEvent{timestamp=%s, runId='%s', previousPoolSize=%d, newPoolSize=%d}"
                .formatted(timestamp, runId, previousPoolSize, newPoolSize);
    }
}
