package com.zhiwu.dynamicthreadpollermanager.experiment.validation;

import java.util.Objects;

/**
 * Side-by-side metric values across three modes.
 * Deltas are signed: positive means closed-loop is better (higher throughput,
 * lower rejection rate = positive delta, etc.).
 */
public record MetricComparison(
        String metricName,
        double closedLoopValue,
        double staticPolicyValue,
        double baselineValue,
        double closedLoopVsStaticDelta,
        double closedLoopVsBaselineDelta,
        double staticVsBaselineDelta
) {
    public MetricComparison {
        Objects.requireNonNull(metricName, "metricName must not be null");
    }
}
