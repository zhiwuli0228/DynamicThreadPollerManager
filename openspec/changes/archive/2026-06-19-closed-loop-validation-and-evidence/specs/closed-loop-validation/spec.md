# Closed-Loop Validation

## Overview
3-way comparison runner: executes identical workload through closed-loop, static-policy, and baseline modes; produces side-by-side metric comparison.

## ADDED Requirements

### Requirement: Three Mode Execution
ClosedLoopValidationRunner SHALL execute all three validation modes sequentially with identical workload.

#### Scenario: Baseline mode
- GIVEN a ValidationScenario with duration=30s
- WHEN validate() is called
- THEN baseline mode result has mode=BASELINE
- AND result.snapshotCount >= 30
- AND result.metrics contains 7 entries (throughput, latency, p99, rejectionRate, queueDepth, stability, cpuEfficiency)
- AND result.adjustmentCount == 0

#### Scenario: Static policy mode
- GIVEN a ValidationScenario with bestStaticPolicy
- WHEN validate() executes static policy mode
- THEN executor is configured with bestStaticPolicy at start
- AND no further adjustments occur
- AND result.mode == STATIC_POLICY

#### Scenario: Closed-loop mode
- GIVEN a ValidationScenario with candidatePolicies
- WHEN validate() executes closed-loop mode
- THEN AdjustmentLoop is started with candidatePolicies
- AND loop runs concurrently with workload
- AND result.mode == CLOSED_LOOP
- AND result.adjustmentCount >= 0

### Requirement: Metric Comparison
ValidationComparisonReport SHALL contain side-by-side metric comparisons across all three modes.

#### Scenario: Seven metric comparisons
- GIVEN all three modes completed
- WHEN report is generated
- THEN report.comparisons.size() == 7
- AND each MetricComparison has closedLoopValue, staticPolicyValue, baselineValue
- AND each MetricComparison has valid deltas (positive = closed-loop better)

#### Scenario: Throughput comparison
- GIVEN closed-loop completes more tasks than baseline
- WHEN throughput comparison is computed
- THEN closedLoopVsBaselineDelta > 0
- AND closedLoopValue > baselineValue

#### Scenario: Rejection rate comparison
- GIVEN closed-loop has fewer rejections than baseline
- WHEN rejection rate comparison is computed
- THEN closedLoopVsBaselineDelta > 0 (lower rejection = positive delta for the "rejection rate" metric)

### Requirement: Report Completeness
ValidationComparisonReport SHALL include statistical significance tests and overall conclusion.

#### Scenario: Fourteen significance tests
- GIVEN all three modes completed with >= 30 snapshots each
- WHEN report is generated
- THEN report.significanceTests.size() == 14 (7 metrics × 2 comparisons: closed-loop vs baseline, closed-loop vs static)
- AND each test has p-value, confidence interval, effect size
- AND report.overallConclusion is non-blank
