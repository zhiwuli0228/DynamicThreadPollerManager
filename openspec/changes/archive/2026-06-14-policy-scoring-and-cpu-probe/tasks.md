# Tasks: policy-scoring-and-cpu-probe

## 1. PolicyScore

- [ ] 1.1 Create `PolicyScore` record in `experiment.classification` with fields: policyId (String, non-blank), compositeScore (double [0.0-1.0]), responsivenessScore (double [0.0-1.0]), safetyScore (double [0.0-1.0]), stabilityScore (double [0.0-1.0]), efficiencyScore (double [0.0-1.0]), explanation (String, non-null)
- [ ] 1.2 Add compact constructor validation: non-blank policyId, all scores in [0.0, 1.0], non-null explanation
- [ ] 1.3 Write unit tests: valid construction, blank policyId throws, score out of range throws

## 2. PolicyScorer Interface

- [ ] 2.1 Create `PolicyScorer` interface in `experiment.classification` with method: `PolicyScore score(PressureClassification classification, ThresholdPolicyConfig config)`
- [ ] 2.2 Add javadoc: implementation must guarantee compositeScore = weighted sum of dimension scores

## 3. ThresholdPolicyScorer

- [ ] 3.1 Create `ThresholdPolicyScorer` class in `experiment.classification` implementing PolicyScorer
- [ ] 3.2 Accept configurable weights via constructor (default: 0.35/0.30/0.20/0.15); validate weights sum to 1.0
- [ ] 3.3 Implement `scoreResponsiveness()`: uses threadUtilizationRatio (consistent with classifier). OVERLOAD/QUEUE_BUILDUP → higher utilization → higher score via utilizationProximity(). UNDER_UTILIZED/RECOVERY → lower utilization → higher score. REJECTION_ACTIVE → 1.0. NORMAL → 0.7
- [ ] 3.4 Implement `scoreSafety()`: capacity adequacy (maxPoolSize < observedMax → -0.4), step reasonableness (scaleStep > maxPoolSize*0.5 → -0.3), boundary reasonableness (maxPoolSize > 128 or < 1 → -0.3)
- [ ] 3.5 Implement `scoreStability()`: volatility (abs(queueGrowthRate)) vs step size matrix
- [ ] 3.6 Implement `scoreEfficiency()`: configMax/observedMax ratio → ratio<=1.5→0.9, ratio<=3.0→0.6, ratio>3.0→0.3
- [ ] 3.7 Compute compositeScore = weighted sum, build explanation string
- [ ] 3.8 Write unit tests: OVERLOAD state → aggressive policy (low threshold) has higher responsiveness than conservative; UNDER_UTILIZED state → conservative policy has higher efficiency; capacity-insufficient policy has lower safety; all scores in [0.0, 1.0]; compositeScore equals weighted sum

## 4. PolicyRanker

- [ ] 4.1 Create `PolicyRanker` class in `experiment.classification` with constructor(PolicyScorer)
- [ ] 4.2 Implement `rank(PressureClassification, List<ThresholdPolicyConfig>)`: scores all candidates, returns List sorted by compositeScore descending
- [ ] 4.3 Implement `best(PressureClassification, List<ThresholdPolicyConfig>)`: returns Optional of highest-scoring PolicyScore
- [ ] 4.4 Write unit tests: rank returns descending order, best returns highest score, empty candidates returns empty list / Optional.empty(), 3-policy ranking matches expected order

## 5. SystemCpuProbe

- [ ] 5.1 Create `SystemCpuProbe` class in `experiment.probe` package
- [ ] 5.2 Constructor calls `ManagementFactory.getOperatingSystemMXBean()`
- [ ] 5.3 Implement `sampleProcessCpuLoad()`: check instanceof `com.sun.management.OperatingSystemMXBean`, call `getProcessCpuLoad()`, return 0.0 if unavailable or result < 0
- [ ] 5.4 Implement `sampleSystemCpuLoad()`: call `getSystemLoadAverage()`, return 0.0 if result < 0
- [ ] 5.5 Write integration test: `sampleProcessCpuLoad()` returns >= 0 (real platform value), `sampleSystemCpuLoad()` does not throw

## 6. RuntimeObservation Modification

- [ ] 6.1 Add new public static method `fromExecutor(ManagedExecutor executor, Instant timestamp, SystemCpuProbe cpuProbe)` in `RuntimeObservation`
- [ ] 6.2 New method: use `cpuProbe.sampleProcessCpuLoad()` if probe non-null and result >= 0; otherwise `MetricValue.absent()`
- [ ] 6.3 Modify existing 2-arg `fromExecutor(ManagedExecutor, Instant)` to delegate: `return fromExecutor(executor, timestamp, new SystemCpuProbe())`
- [ ] 6.4 Write unit test: with mock probe returning 0.5 → cpuUtilization is Present(0.5); null probe → absent; probe returning -1 → absent

## 7. End-to-End Integration Test

- [ ] 7.1 Create `ClassificationScoringRankingE2ETest` in test source
- [ ] 7.2 Run 100-step managed executor scenario → capture snapshots via InMemoryEvidenceRecorder
- [ ] 7.3 Classify: `classifier.classify(snapshots, config, rejectedTaskCount, durationMs)` → PressureClassification
- [ ] 7.4 Score 3 policies (conservative: thresholds high, moderate: defaults, aggressive: thresholds low) against classification
- [ ] 7.5 Rank: `ranker.rank(classification, policies)` → verify descending compositeScore order, explanations non-empty
- [ ] 7.6 best(): verify returns highest-scoring policy

## 8. Full Test Verification

- [ ] 8.1 Run `mvn test` — verify all 708 existing tests pass (zero regression)
- [ ] 8.2 Verify all new tests pass
- [ ] 8.3 Verify no new compiler warnings
