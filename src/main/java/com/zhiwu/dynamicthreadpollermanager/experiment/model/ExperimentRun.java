package com.zhiwu.dynamicthreadpollermanager.experiment.model;

import com.zhiwu.dynamicthreadpollermanager.experiment.state.RunState;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable representation of a single experiment run.
 */
public final class ExperimentRun {

    private final String runId;
    private final String experimentKey;
    private final String scenarioId;
    private final String policyId;
    private final Instant createdAt;
    private final RunState state;

    public ExperimentRun(String scenarioId, String policyId) {
        this(
                UUID.randomUUID().toString(),
                scenarioId,
                policyId,
                Instant.now(),
                RunState.CREATED
        );
    }

    public ExperimentRun(String runId, String scenarioId, String policyId,
                         Instant createdAt, RunState state) {
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.experimentKey = buildExperimentKey(scenarioId, policyId);
        this.scenarioId = Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        this.policyId = Objects.requireNonNull(policyId, "policyId must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.state = Objects.requireNonNull(state, "state must not be null");
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

    public Instant createdAt() {
        return createdAt;
    }

    public RunState state() {
        return state;
    }

    public ExperimentRun withState(RunState newState) {
        return new ExperimentRun(runId, scenarioId, policyId, createdAt, newState);
    }

    private static String buildExperimentKey(String scenarioId, String policyId) {
        return Objects.requireNonNull(scenarioId, "scenarioId must not be null")
                + "::"
                + Objects.requireNonNull(policyId, "policyId must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ExperimentRun that && Objects.equals(runId, that.runId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runId);
    }

    @Override
    public String toString() {
        return "ExperimentRun{runId='%s', experimentKey='%s', scenarioId='%s', policyId='%s', state=%s}"
                .formatted(runId, experimentKey, scenarioId, policyId, state);
    }
}
