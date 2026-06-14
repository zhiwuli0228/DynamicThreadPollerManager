package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import com.zhiwu.dynamicthreadpollermanager.experiment.state.RunState;

import java.util.Objects;

/**
 * Immutable summary returned by a {@link ScenarioExperimentRunner}
 * after it has driven a scenario through the experiment lifecycle
 * and recorded metrics evidence.
 */
public final class ScenarioRunOutcome {

    private final String runId;
    private final String scenarioId;
    private final String policyId;
    private final int completedStepCount;
    private final long totalWorkUnits;
    private final int evidenceCount;
    private final RunState finalState;
    private final long rejectedTaskCount;

    public ScenarioRunOutcome(String runId,
                              String scenarioId,
                              String policyId,
                              int completedStepCount,
                              long totalWorkUnits,
                              int evidenceCount,
                              RunState finalState) {
        this(runId, scenarioId, policyId, completedStepCount, totalWorkUnits,
                evidenceCount, finalState, 0L);
    }

    public ScenarioRunOutcome(String runId,
                              String scenarioId,
                              String policyId,
                              int completedStepCount,
                              long totalWorkUnits,
                              int evidenceCount,
                              RunState finalState,
                              long rejectedTaskCount) {
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.scenarioId = Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        this.policyId = Objects.requireNonNull(policyId, "policyId must not be null");
        if (completedStepCount < 0) {
            throw new IllegalArgumentException("completedStepCount must be non-negative, was " + completedStepCount);
        }
        if (totalWorkUnits < 0) {
            throw new IllegalArgumentException("totalWorkUnits must be non-negative, was " + totalWorkUnits);
        }
        if (evidenceCount < 0) {
            throw new IllegalArgumentException("evidenceCount must be non-negative, was " + evidenceCount);
        }
        if (rejectedTaskCount < 0) {
            throw new IllegalArgumentException("rejectedTaskCount must be non-negative, was " + rejectedTaskCount);
        }
        this.finalState = Objects.requireNonNull(finalState, "finalState must not be null");
        this.completedStepCount = completedStepCount;
        this.totalWorkUnits = totalWorkUnits;
        this.evidenceCount = evidenceCount;
        this.rejectedTaskCount = rejectedTaskCount;
    }

    public String runId() {
        return runId;
    }

    public String scenarioId() {
        return scenarioId;
    }

    public String policyId() {
        return policyId;
    }

    public int completedStepCount() {
        return completedStepCount;
    }

    public long totalWorkUnits() {
        return totalWorkUnits;
    }

    public int evidenceCount() {
        return evidenceCount;
    }

    public RunState finalState() {
        return finalState;
    }

    public long rejectedTaskCount() {
        return rejectedTaskCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ScenarioRunOutcome that
                && completedStepCount == that.completedStepCount
                && totalWorkUnits == that.totalWorkUnits
                && evidenceCount == that.evidenceCount
                && rejectedTaskCount == that.rejectedTaskCount
                && runId.equals(that.runId)
                && scenarioId.equals(that.scenarioId)
                && policyId.equals(that.policyId)
                && finalState == that.finalState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(runId, scenarioId, policyId, completedStepCount,
                totalWorkUnits, evidenceCount, finalState, rejectedTaskCount);
    }

    @Override
    public String toString() {
        return "ScenarioRunOutcome{runId='%s', scenarioId='%s', policyId='%s', completedStepCount=%d, totalWorkUnits=%d, evidenceCount=%d, rejectedTaskCount=%d, finalState=%s}"
                .formatted(runId, scenarioId, policyId, completedStepCount, totalWorkUnits, evidenceCount, rejectedTaskCount, finalState);
    }
}
