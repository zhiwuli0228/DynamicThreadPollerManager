# Design: baseline-catalog-and-comparison-runner

## Overview

This change implements the core building blocks of the v0.12.0 baseline comparison framework: a catalog of common executor presets, a comparable scenario runner, and normalized comparison metrics. The full functional design is documented in `docs/04-development/versions/v0.12.0/20-sr.md` §4.1-4.6, 4.10-4.11.

## Module Boundaries

| Module | Change | Component |
|---|---|---|
| `experiment.scenario` | **New** | `CommonExecutorPreset` (record) |
| `experiment.scenario` | **New** | `BaselineExecutorCatalog` (class + Builder) |
| `experiment.scenario` | **New** | `NormalizedComparisonMetrics` (record) |
| `experiment.scenario` | **New** | `MetricDelta` (record) |
| `experiment.scenario` | **New** | `ComparisonResult` (record) |
| `experiment.scenario` | **New** | `ComparableScenarioRunner` (class) |
| `experiment.scenario` | **Modify** | `ScenarioRunOutcome` — add `rejectedTaskCount` field |
| `experiment.executor` | **Modify** | `ManagedExecutor` — add `getRejectedTaskCount()` + handler wrapper |

## Dependency Direction

```text
experiment.scenario (new components)
    ├── experiment.model (ScenarioDefinition, ExperimentRun — unchanged)
    ├── experiment.metrics (EvidenceRecorder, PressureSampler, ObservedSnapshot — read-only)
    ├── experiment.executor (ManagedExecutorConfig, ManagedExecutor — read-only getter)
    ├── experiment.coordinator (ExperimentCoordinator)
    └── experiment.acquisition (AcquisitionReportPaths — path query)

experiment.executor (ManagedExecutor modification)
    └── java.util.concurrent.atomic (AtomicLong)
```

No new cross-package dependencies. No circular dependencies.

## Component Design Summary

### CommonExecutorPreset
- Record: `presetId`, `executorType` (FIXED_THREAD_POOL/CACHED_THREAD_POOL/SINGLE_THREAD_EXECUTOR), `corePoolSize`, `maxPoolSize`, `queueCapacity` (-1=unbounded, 0=SynchronousQueue), `description`
- `toBaselinePreset()` conversion: queueCapacity -1 → Integer.MAX_VALUE
- Validation: non-null presetId, valid executorType, core≤max, queueCapacity≥-1

### BaselineExecutorCatalog
- Builder pattern with `register(CommonExecutorPreset)` → `build()`
- `get(String presetId)` → NoSuchElementException if missing
- `withDefaults()`: 6 presets (fixed-2, fixed-4, fixed-8, cached, single, fixed-2-bounded)
- Immutable post-build (Map.copyOf)

### NormalizedComparisonMetrics
- Record: 9 fields (completedTaskCount, rejectedTaskCount, avgQueueDepth, maxQueueDepth, totalDurationMs, throughputPerSecond, avgActiveThreads, maxPoolSize, snapshotCount)
- `fromSnapshots(List<ObservedSnapshot>, long totalDurationMs, int fallbackPoolSize)`
- `withRejectedTaskCount(long)` for managed executor injection
- throughput=0.0 when totalDurationMs==0 (no division by zero)

### MetricDelta
- Record: metricName, baselineValue, managedValue, absoluteDelta, relativeDelta, direction
- `compute(name, baseline, managed, higherIsBetter)` static factory
- NEUTRAL threshold: abs(relativeDelta) < 1%

### ComparisonResult
- Record: comparisonId, scenarioId, baselinePresetId, managedConfigId, baselineOutcome, managedOutcome, baselineMetrics, managedMetrics, deltas (Map<String, MetricDelta>, 9 entries), createdAt

### ComparableScenarioRunner
- Constructor: `(BaselineExecutorCatalog, ScenarioPlanner, Supplier<Instant>)`
- `compare(ScenarioDefinition, String baselinePresetId, ManagedExecutorConfig)` → ComparisonResult
- 8-step execution: preset lookup → conversion → baseline run (wall-clock timed) → snapshots from recorder → managed run (wall-clock timed) → snapshots → metrics → deltas
- Dynamic runner creation each call (no shared state)

### ManagedExecutor Rejection Counting
- `getRejectedTaskCount()` → AtomicLong
- Handler wrapper in constructor: increment counter → delegate to original handler
- Transparent to existing API (constructor signatures unchanged)
- PLATFORM and VIRTUAL modes both supported

### ScenarioRunOutcome Extension
- New 8-field constructor: adds `long rejectedTaskCount` (default 0 for backward compat)
- Old 7-field constructor delegates to 8-field with rejectedTaskCount=0
- `ManagedExecutorScenarioRunner.run()` reads `executor.getRejectedTaskCount()` before shutdown

## Testing Strategy

| Type | Scope | Key Assertions |
|---|---|---|
| Unit | CommonExecutorPreset | Validation, toBaselinePreset conversion |
| Unit | BaselineExecutorCatalog | withDefaults()=6, get/NoSuchElement, duplicate register |
| Unit | NormalizedComparisonMetrics | fromSnapshots() all 9 fields, empty list, zero division |
| Unit | MetricDelta | IMPROVED/REGRESSED/NEUTRAL directions |
| Unit | ComparisonResult | 9 deltas in map |
| Unit | ComparableScenarioRunner | Dual-run, fail-fast, different runIds |
| Unit | ManagedExecutor | getRejectedTaskCount() > 0 after overflow submit |
| Unit | ScenarioRunOutcome | Backward compat (old 7-arg ctor still works) |
| Regression | All existing | 646 tests pass |

## References
- IR: `docs/04-development/versions/v0.12.0/10-ir.md` (IR-v0.12-001 through 005, 009)
- SR: `docs/04-development/versions/v0.12.0/20-sr.md` (§4.1-4.6, 4.10-4.11)
- Decisions: `docs/04-development/versions/v0.12.0/decision-log.md` (D1-D3, D5-D6)
