package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PressureClassification;

import java.time.Instant;
import java.util.Objects;

/**
 * A single adjustment record in the adjustment history.
 */
public record HistoryEntry(
        AdjustmentDecision decision,
        AdjustmentResult result,
        PressureClassification beforeClassification,
        PressureClassification afterClassification,
        Instant recordedAt
) {
    public HistoryEntry {
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(beforeClassification, "beforeClassification must not be null");
        Objects.requireNonNull(afterClassification, "afterClassification must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
    }
}
