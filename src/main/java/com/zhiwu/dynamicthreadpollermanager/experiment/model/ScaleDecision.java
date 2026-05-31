package com.zhiwu.dynamicthreadpollermanager.experiment.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable record of a scaling decision made during an experiment run.
 */
public final class ScaleDecision {

    private final Instant timestamp;
    private final String runId;
    private final int currentPoolSize;
    private final int proposedPoolSize;
    private final String reasoning;

    public ScaleDecision(Instant timestamp, String runId, int currentPoolSize,
                         int proposedPoolSize, String reasoning) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.currentPoolSize = currentPoolSize;
        this.proposedPoolSize = proposedPoolSize;
        this.reasoning = reasoning;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public String runId() {
        return runId;
    }

    public int currentPoolSize() {
        return currentPoolSize;
    }

    public int proposedPoolSize() {
        return proposedPoolSize;
    }

    public String reasoning() {
        return reasoning;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ScaleDecision that
                && timestamp.equals(that.timestamp)
                && runId.equals(that.runId)
                && proposedPoolSize == that.proposedPoolSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, runId, proposedPoolSize);
    }

    @Override
    public String toString() {
        return "ScaleDecision{timestamp=%s, runId='%s', currentPoolSize=%d, proposedPoolSize=%d}"
                .formatted(timestamp, runId, currentPoolSize, proposedPoolSize);
    }
}
