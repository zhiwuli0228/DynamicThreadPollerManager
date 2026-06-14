package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable classification result produced by a {@link PressureClassifier}.
 */
public record PressureClassification(
        PressureState state,
        double confidence,
        List<String> evidence,
        NormalizedPressureMetrics metrics,
        Instant classifiedAt
) {
    public PressureClassification {
        Objects.requireNonNull(state, "state must not be null");
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException(
                    "confidence must be in [0.0, 1.0], was " + confidence);
        }
        Objects.requireNonNull(evidence, "evidence must not be null");
        Objects.requireNonNull(metrics, "metrics must not be null");
        Objects.requireNonNull(classifiedAt, "classifiedAt must not be null");
    }
}
