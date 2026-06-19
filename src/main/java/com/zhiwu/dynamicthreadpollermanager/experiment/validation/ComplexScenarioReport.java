package com.zhiwu.dynamicthreadpollermanager.experiment.validation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable report produced after a complex scenario validation run.
 * All metrics are computed from real observation data — no synthetic proxies.
 *
 * @param reportId           unique identifier for this report
 * @param scenarioId         identifier of the scenario that was run
 * @param seed               seed used for deterministic scenario planning
 * @param scenarioConfig     human-readable description of scenario configuration
 * @param adjustmentCount    total adjustments attempted during the run
 * @param blockedCount       adjustments blocked by safety gates or anti-oscillation
 * @param rollbackCount      rollbacks performed during the run
 * @param rollbackSuccessRate successful rollbacks / total rollback attempts (0.0–1.0)
 * @param recoveryTimeMs     time from degradation onset to metric recovery, in milliseconds
 * @param p95LatencyMs       95th percentile latency from real snapshots
 * @param p99LatencyMs       99th percentile latency from real snapshots
 * @param rejectionCount     tasks rejected by the executor during the run
 * @param queueDepthDelta    change in queue depth from start to end (final - initial)
 * @param throughputDelta    change in throughput from start to end (final - initial)
 * @param decisionWindows    per-decision observation windows with pre/post snapshots
 * @param generatedAt        when this report was generated
 */
public record ComplexScenarioReport(
        String reportId,
        String scenarioId,
        long seed,
        String scenarioConfig,
        int adjustmentCount,
        int blockedCount,
        int rollbackCount,
        double rollbackSuccessRate,
        long recoveryTimeMs,
        long p95LatencyMs,
        long p99LatencyMs,
        int rejectionCount,
        int queueDepthDelta,
        double throughputDelta,
        List<ObservationWindow> decisionWindows,
        Instant generatedAt
) {
    public ComplexScenarioReport {
        Objects.requireNonNull(reportId, "reportId must not be null");
        Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        Objects.requireNonNull(scenarioConfig, "scenarioConfig must not be null");
        if (adjustmentCount < 0) {
            throw new IllegalArgumentException("adjustmentCount must be >= 0, was " + adjustmentCount);
        }
        if (blockedCount < 0) {
            throw new IllegalArgumentException("blockedCount must be >= 0, was " + blockedCount);
        }
        if (rollbackCount < 0) {
            throw new IllegalArgumentException("rollbackCount must be >= 0, was " + rollbackCount);
        }
        if (rollbackSuccessRate < 0.0 || rollbackSuccessRate > 1.0) {
            throw new IllegalArgumentException(
                    "rollbackSuccessRate must be in [0.0, 1.0], was " + rollbackSuccessRate);
        }
        if (recoveryTimeMs < 0) {
            throw new IllegalArgumentException("recoveryTimeMs must be >= 0, was " + recoveryTimeMs);
        }
        if (p95LatencyMs < 0) {
            throw new IllegalArgumentException("p95LatencyMs must be >= 0, was " + p95LatencyMs);
        }
        if (p99LatencyMs < 0) {
            throw new IllegalArgumentException("p99LatencyMs must be >= 0, was " + p99LatencyMs);
        }
        if (rejectionCount < 0) {
            throw new IllegalArgumentException("rejectionCount must be >= 0, was " + rejectionCount);
        }
        Objects.requireNonNull(decisionWindows, "decisionWindows must not be null");
        Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        decisionWindows = List.copyOf(decisionWindows);
    }
}
