package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Minimal summary derived from a recorded evidence stream. Time bounds are
 * empty for runs with no recorded samples so callers can distinguish "no
 * data" from "data with a coincidental zero duration".
 */
public final class EvidenceSummary {

    private final String runId;
    private final int sampleCount;
    private final Optional<Instant> firstTimestamp;
    private final Optional<Instant> lastTimestamp;

    public EvidenceSummary(String runId, int sampleCount,
                           Optional<Instant> firstTimestamp,
                           Optional<Instant> lastTimestamp) {
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.sampleCount = sampleCount;
        this.firstTimestamp = Objects.requireNonNull(firstTimestamp, "firstTimestamp must not be null");
        this.lastTimestamp = Objects.requireNonNull(lastTimestamp, "lastTimestamp must not be null");
    }

    public static EvidenceSummary empty(String runId) {
        return new EvidenceSummary(runId, 0, Optional.empty(), Optional.empty());
    }

    public String runId() {
        return runId;
    }

    public int sampleCount() {
        return sampleCount;
    }

    public Optional<Instant> firstTimestamp() {
        return firstTimestamp;
    }

    public Optional<Instant> lastTimestamp() {
        return lastTimestamp;
    }
}
