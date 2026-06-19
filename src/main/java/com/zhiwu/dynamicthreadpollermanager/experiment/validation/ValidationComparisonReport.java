package com.zhiwu.dynamicthreadpollermanager.experiment.validation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ValidationComparisonReport(
        String reportId,
        ValidationScenario scenario,
        ValidationRunResult closedLoopResult,
        ValidationRunResult staticPolicyResult,
        ValidationRunResult baselineResult,
        List<MetricComparison> comparisons,
        List<StatisticalSignificance> significanceTests,
        String overallConclusion,
        Instant generatedAt
) {
    public ValidationComparisonReport {
        Objects.requireNonNull(reportId, "reportId must not be null");
        Objects.requireNonNull(scenario, "scenario must not be null");
        Objects.requireNonNull(closedLoopResult, "closedLoopResult must not be null");
        Objects.requireNonNull(staticPolicyResult, "staticPolicyResult must not be null");
        Objects.requireNonNull(baselineResult, "baselineResult must not be null");
        Objects.requireNonNull(comparisons, "comparisons must not be null");
        Objects.requireNonNull(significanceTests, "significanceTests must not be null");
        Objects.requireNonNull(overallConclusion, "overallConclusion must not be null");
        Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        comparisons = List.copyOf(comparisons);
        significanceTests = List.copyOf(significanceTests);
    }
}
