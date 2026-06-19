package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable input to a {@link PolicyEvaluator}.
 *
 * <p>The {@code evaluatedAt} timestamp is supplied by the caller so
 * the evaluator remains deterministic and free of wall-clock
 * dependencies.
 */
public final class PolicyEvaluationInput {

    private final String runId;
    private final PressureSnapshot snapshot;
    private final Instant evaluatedAt;

    public PolicyEvaluationInput(String runId, PressureSnapshot snapshot, Instant evaluatedAt) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot must not be null");
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        this.runId = runId;
    }

    public String runId() {
        return runId;
    }

    public PressureSnapshot snapshot() {
        return snapshot;
    }

    public Instant evaluatedAt() {
        return evaluatedAt;
    }
}
