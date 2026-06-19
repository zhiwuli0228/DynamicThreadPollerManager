# Tasks: closed-loop-validation-and-evidence

## 1. ValidationMode + ValidationScenario

- [x] 1.1 Create `ValidationMode` enum: CLOSED_LOOP, STATIC_POLICY, BASELINE
- [x] 1.2 Create `ValidationScenario` record with 8 fields (scenarioId, workload, executorConfig, candidatePolicies, bestStaticPolicy, duration, minIterations, warmupPeriodMs)
- [x] 1.3 Add compact constructor validation: non-blank id, non-null fields, non-empty candidatePolicies, duration>=30s, minIterations>=5, warmupPeriodMs>=1000
- [x] 1.4 Write unit tests: valid construction, each invalid field

## 2. ValidationRunResult + MetricComparison + StatisticalSignificance + ValidationComparisonReport

- [x] 2.1 Create `ValidationRunResult` record: mode, runId, snapshotCount, metrics (Map<String,Double>), durationMs, adjustmentCount, finalPressureState
- [x] 2.2 Create `MetricComparison` record: metricName, closedLoopValue, staticPolicyValue, baselineValue, three deltas
- [x] 2.3 Create `StatisticalSignificance` record: metricName, pValue, ciLow, ciHigh, effectSize, isSignificant, sampleSize
- [x] 2.4 Create `ValidationComparisonReport` record: reportId, scenario, three run results, comparisons list, significanceTests list, overallConclusion, generatedAt
- [x] 2.5 Write unit tests: record construction validation

## 3. StatisticalSignificanceCalculator

- [x] 3.1 Create `StatisticalSignificanceCalculator` utility class (private constructor)
- [x] 3.2 Implement `compare(double[] modeA, double[] modeB, String metricName)` → StatisticalSignificance
- [x] 3.3 Implement paired differences, mean, stddev, stdErr
- [x] 3.4 Implement t-statistic: t = meanDiff / stdErr
- [x] 3.5 Implement t-distribution p-value using A&S 26.2.17 normal CDF + t-to-z transformation
- [x] 3.6 Implement Cohen's d: meanDiff / pooledStdDev
- [x] 3.7 Implement 95% CI: meanDiff ± t_critical * stdErr
- [x] 3.8 Handle edge cases: n<2 (return non-significant), zero variance (return non-significant)
- [x] 3.9 Write unit tests: known t-values → expected p-values, edge cases, CI bounds ordering

## 4. ClosedLoopValidationRunner

- [x] 4.1 Create `ClosedLoopValidationRunner` class: clock supplier
- [x] 4.2 Implement `validate(ValidationScenario)` → ValidationComparisonReport
- [x] 4.3 Implement `runBaselineMode()` — create executor, LivePressureSampler, run workload, collect metrics
- [x] 4.4 Implement `runStaticPolicyMode()` — apply bestStaticPolicy once, then same as baseline
- [x] 4.5 Implement `runClosedLoopMode()` — create AdjustmentLoop, start loop, run workload, stop loop, collect metrics
- [x] 4.6 Implement `runWorkload(ManagedExecutor, LoadScenario, durationMs)` — submit tasks continuously
- [x] 4.7 Implement `computeMetrics(snapshots, durationMs, executor)` → 7 metrics
- [x] 4.8 Implement `computeComparisons()` → 7 MetricComparison entries
- [x] 4.9 Implement `generateConclusion()` → overall summary string
- [x] 4.10 Write integration tests: 3-mode validation with real executor, closed-loop throughput >= baseline, rejection rate <= baseline

## 5. Full Verification

- [x] 5.1 Run `mvn test` — verify all 857 + Change 1 tests pass (zero regression)
- [x] 5.2 Verify all new Change 2 tests pass
