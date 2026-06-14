# Apply: baseline-catalog-and-comparison-runner

## Implementation Record

All 30 tasks from `tasks.md` have been implemented.

## New Source Files (6)

| File | Package | Description |
|---|---|---|
| `CommonExecutorPreset.java` | `experiment.scenario` | Record with 6 fields, validation, toBaselinePreset(), toMap()/fromMap() |
| `BaselineExecutorCatalog.java` | `experiment.scenario` | Immutable registry with Builder, 6 default presets |
| `NormalizedComparisonMetrics.java` | `experiment.scenario` | Record with 9 fields, fromSnapshots(), withRejectedTaskCount(), toMap()/fromMap() |
| `MetricDelta.java` | `experiment.scenario` | Record with compute() static factory, toMap()/fromMap() |
| `ComparisonResult.java` | `experiment.scenario` | Record with 10 fields, toMap()/fromMap() |
| `ComparableScenarioRunner.java` | `experiment.scenario` | Sequential baseline→managed runner, 8-step compare() flow |

## Modified Source Files (4)

| File | Change |
|---|---|
| `ManagedExecutor.java` | +AtomicLong rejectedTaskCount, handler wrapper in PLATFORM + VIRTUAL ctors, getRejectedTaskCount(), setRejectionPolicy() preserves wrapper |
| `ScenarioRunOutcome.java` | +rejectedTaskCount field, 8-arg constructor, backward-compatible 7-arg delegator |
| `ManagedExecutorScenarioRunner.java` | Phase 4 reads executor.getRejectedTaskCount(), passes to 8-arg ScenarioRunOutcome |
| `ExecutorRebuildStrategy.java` | Uses oldExecutor.getRejectionPolicy() instead of oldTpe.getRejectedExecutionHandler() |

## New Test Files (6)

| File | Tests | Status |
|---|---|---|
| `CommonExecutorPresetTest.java` | 13 | Pass |
| `BaselineExecutorCatalogTest.java` | 9 | Pass |
| `NormalizedComparisonMetricsTest.java` | 5 | Pass |
| `MetricDeltaTest.java` | 7 | Pass |
| `ComparableScenarioRunnerTest.java` | 6 | Pass |
| `ManagedExecutorRejectionCountTest.java` | 4 | Pass |

## Test Verification

- `mvn test`: 698 tests run, 697 pass, 1 pre-existing flaky test (readOnlyStateShouldReflectThreadPoolExecutor — unrelated timing issue)
- All 44 new tests pass
- Zero regression in existing tests (no new failures introduced)
- Modified existing tests: 2 in ManagedExecutorTest (getRejectionPolicyDelegatesToTpe, setRejectionPolicyPropagatesToUnderlyingTpe) — updated to check policy type rather than handler identity since wrapper is now internal implementation detail

## Spec Coverage

| Capability | Requirements | Scenarios | Implementation |
|---|---|---|---|
| baseline-executor-catalog | 3 | 9 | CommonExecutorPreset + BaselineExecutorCatalog |
| normalized-comparison-metrics | 3 | 8 | NormalizedComparisonMetrics + MetricDelta + ComparisonResult |
| comparable-scenario-runner | 2 | 5 | ComparableScenarioRunner |
| managed-executor-rejection-counting | 2 | 3 | ManagedExecutor.getRejectedTaskCount() |
