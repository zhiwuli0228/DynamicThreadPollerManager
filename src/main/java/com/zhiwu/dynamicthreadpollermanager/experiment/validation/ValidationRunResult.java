package com.zhiwu.dynamicthreadpollermanager.experiment.validation;

import java.util.Map;
import java.util.Objects;

public record ValidationRunResult(
        ValidationMode mode,
        String runId,
        int snapshotCount,
        Map<String, Double> metrics,
        long durationMs,
        int adjustmentCount,
        String finalPressureState
) {
    public ValidationRunResult {
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(metrics, "metrics must not be null");
        Objects.requireNonNull(finalPressureState, "finalPressureState must not be null");
        metrics = Map.copyOf(metrics);
    }
}
