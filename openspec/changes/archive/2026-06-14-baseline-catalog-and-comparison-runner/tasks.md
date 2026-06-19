# Tasks: baseline-catalog-and-comparison-runner

## 1. CommonExecutorPreset

- [ ] 1.1 Create `CommonExecutorPreset` record in `experiment.scenario` with fields: presetId, executorType, corePoolSize, maxPoolSize, queueCapacity, description
- [ ] 1.2 Add compact constructor validation: non-blank presetId, valid executorType (FIXED_THREAD_POOL/CACHED_THREAD_POOL/SINGLE_THREAD_EXECUTOR), corePoolSize>=0, maxPoolSize>=corePoolSize, queueCapacity>=-1
- [ ] 1.3 Add `toBaselinePreset()` method with queueCapacity mapping (-1→Integer.MAX_VALUE, 0→0, >0→direct)
- [ ] 1.4 Write unit tests: valid construction, blank ID, invalid type, invalid bounds, toBaselinePreset() all 3 cases

## 2. BaselineExecutorCatalog

- [ ] 2.1 Create `BaselineExecutorCatalog` class in `experiment.scenario` with private constructor accepting Map<String, CommonExecutorPreset>
- [ ] 2.2 Add Builder inner class with `register(CommonExecutorPreset)` throwing on duplicate presetId, and `build()` returning immutable catalog (Map.copyOf)
- [ ] 2.3 Add `get(String)` throwing NoSuchElementException, `presetIds()`, `size()`
- [ ] 2.4 Implement `withDefaults()` registering 6 presets: fixed-2, fixed-4, fixed-8, cached, single, fixed-2-bounded
- [ ] 2.5 Write unit tests: withDefaults() size==6, get("fixed-4") values, get("nonexistent") throws, duplicate register throws, presetIds() immutability

## 3. NormalizedComparisonMetrics

- [ ] 3.1 Create `NormalizedComparisonMetrics` record in `experiment.scenario` with 9 fields: completedTaskCount, rejectedTaskCount, avgQueueDepth, maxQueueDepth, totalDurationMs, throughputPerSecond, avgActiveThreads, maxPoolSize, snapshotCount
- [ ] 3.2 Implement `fromSnapshots(List<ObservedSnapshot>, long totalDurationMs, int fallbackPoolSize)`: compute avg/max from PressureSnapshot fields, throughput=tasks/(duration/1000), handle empty list and zero duration
- [ ] 3.3 Add `withRejectedTaskCount(long)` returning new record with updated rejectedTaskCount
- [ ] 3.4 Write unit tests: non-empty list computes all 9 fields correctly, empty list returns zero defaults with fallback pool size, zero totalDurationMs yields zero throughput

## 4. MetricDelta

- [ ] 4.1 Create `MetricDelta` record in `experiment.scenario` with fields: metricName, baselineValue, managedValue, absoluteDelta, relativeDelta, direction
- [ ] 4.2 Implement `compute(String, double, double, boolean)` static factory: absolute=managed-baseline, relative=(abs/baseline)*100 (0 if baseline==0), NEUTRAL if abs(relative)<1%, IMPROVED/REGRESSED based on higherIsBetter flag
- [ ] 4.3 Write unit tests: improved/regressed/neutral directions, zero baseline, edge cases

## 5. ComparisonResult

- [ ] 5.1 Create `ComparisonResult` record in `experiment.scenario` with fields: comparisonId, scenarioId, baselinePresetId, managedConfigId, baselineOutcome, managedOutcome, baselineMetrics, managedMetrics, deltas (Map<String, MetricDelta>), createdAt
- [ ] 5.2 Write unit test: construction with all 9 deltas present

## 6. ManagedExecutor Rejection Counting

- [ ] 6.1 Add `private final AtomicLong rejectedTaskCount` field to `ManagedExecutor`
- [ ] 6.2 Wrap `RejectedExecutionHandler` in PLATFORM mode constructor: counting handler increments counter then delegates to original handler
- [ ] 6.3 Wrap `virtualRejectionHandler` in VIRTUAL mode constructor with same pattern
- [ ] 6.4 Add `public long getRejectedTaskCount()` method
- [ ] 6.5 Verify `getRejectionPolicy()` returns original handler (not wrapper) — existing behavior preserved
- [ ] 6.6 Write unit test: submit more tasks than queueCapacity+maxPoolSize, verify getRejectedTaskCount()>0
- [ ] 6.7 Run existing ManagedExecutor tests to verify zero regression

## 7. ScenarioRunOutcome Extension

- [ ] 7.1 Add `private final long rejectedTaskCount` field to `ScenarioRunOutcome` (default 0)
- [ ] 7.2 Keep existing 7-arg constructor, delegate to new 8-arg constructor with rejectedTaskCount=0
- [ ] 7.3 Add new 8-arg public constructor with rejectedTaskCount validation (>=0)
- [ ] 7.4 Add `rejectedTaskCount()` accessor
- [ ] 7.5 Update `ManagedExecutorScenarioRunner.run()` to read `executor.getRejectedTaskCount()` before shutdown and pass to 8-arg constructor
- [ ] 7.6 Write unit test: old 7-arg constructor produces rejectedTaskCount=0, new 8-arg constructor preserves value
- [ ] 7.7 Run existing ScenarioRunOutcome tests (ManagedExecutorScenarioRunnerTest, ScenarioExperimentRunnerTest) to verify zero regression

## 8. ComparableScenarioRunner

- [ ] 8.1 Create `ComparableScenarioRunner` class in `experiment.scenario` with constructor(BaselineExecutorCatalog, ScenarioPlanner, Supplier<Instant>)
- [ ] 8.2 Implement `compare(ScenarioDefinition, String baselinePresetId, ManagedExecutorConfig)`: 8-step flow per SR §4.6 — preset lookup + conversion → baseline run (timed) → snapshots → baseline metrics → managed run (timed) → snapshots → managed metrics (with rejectedTaskCount from outcome) → 9 MetricDeltas → ComparisonResult
- [ ] 8.3 Ensure dynamic runner creation each call (fresh ScenarioExperimentRunner, fresh ManagedExecutorScenarioRunner, fresh ExperimentCoordinators, fresh InMemoryEvidenceRecorders)
- [ ] 8.4 Implement fail-fast: if baseline preset not found, throw without attempting managed run
- [ ] 8.5 Write integration test: compare(scenario, "fixed-4", managedConfig) → ComparisonResult with different runIds, 9 deltas, both outcomes present
- [ ] 8.6 Write unit test: compare with nonexistent presetId throws NoSuchElementException
- [ ] 8.7 Write integration test: two consecutive compare() calls with different presets both succeed

## 9. Full Test Verification

- [ ] 9.1 Run `mvn test` — verify all 646 existing tests pass (zero regression)
- [ ] 9.2 Verify all new tests pass
