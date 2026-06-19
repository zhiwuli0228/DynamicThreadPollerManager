package com.zhiwu.dynamicthreadpollermanager.experiment.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable summary of an experiment run's outcome.
 */
public final class AnalysisSummary {

    private final String runId;
    private final String experimentKey;
    private final String scenarioId;
    private final String policyId;
    private final Instant startTime;
    private final Instant endTime;
    private final String outcome;
    private final int snapshotCount;

    public AnalysisSummary(String runId, String experimentKey, String scenarioId, String policyId,
                           Instant startTime, Instant endTime, String outcome,
                           int snapshotCount) {
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.experimentKey = Objects.requireNonNull(experimentKey, "experimentKey must not be null");
        this.scenarioId = Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        this.policyId = Objects.requireNonNull(policyId, "policyId must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        this.outcome = outcome;
        this.snapshotCount = snapshotCount;
    }

    public String runId() {
        return runId;
    }

    public String experimentKey() {
        return experimentKey;
    }

    public String scenarioId() {
        return scenarioId;
    }

    public String policyId() {
        return policyId;
    }

    public Instant startTime() {
        return startTime;
    }

    public Instant endTime() {
        return endTime;
    }

    public Duration duration() {
        return Duration.between(startTime, endTime);
    }

    public String outcome() {
        return outcome;
    }

    public int snapshotCount() {
        return snapshotCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof AnalysisSummary that
                && Objects.equals(runId, that.runId)
                && Objects.equals(experimentKey, that.experimentKey)
                && Objects.equals(scenarioId, that.scenarioId)
                && Objects.equals(policyId, that.policyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runId, experimentKey, scenarioId, policyId);
    }

    @Override
    public String toString() {
        return "AnalysisSummary{runId='%s', experimentKey='%s', scenarioId='%s', policyId='%s', outcome='%s', duration=%s}"
                .formatted(runId, experimentKey, scenarioId, policyId, outcome, duration());
    }
}
