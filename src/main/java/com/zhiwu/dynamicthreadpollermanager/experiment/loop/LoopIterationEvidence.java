package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PressureClassification;

import java.time.Instant;
import java.util.Objects;

/**
 * Evidence record for a single loop iteration.
 */
public record LoopIterationEvidence(
        String sessionId,
        int iterationIndex,
        AdjustmentDecision decision,
        AdjustmentResult result,
        PressureClassification beforeClassification,
        Instant recordedAt
) {
    public LoopIterationEvidence {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        if (sessionId.isBlank()) throw new IllegalArgumentException("sessionId must not be blank");
        if (iterationIndex < 0) throw new IllegalArgumentException("iterationIndex must be >= 0");
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
    }
}
