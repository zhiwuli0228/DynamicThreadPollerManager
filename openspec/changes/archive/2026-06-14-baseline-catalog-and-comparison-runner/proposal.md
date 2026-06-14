## Why

The system can independently run baseline executor scenarios and managed executor scenarios (v0.3.0, v0.7.0, v0.8.0), but cannot compare them. There is no catalog of common thread-pool presets, no runner that executes the same workload against two executor types, and no normalized metrics model for cross-executor comparison. This change builds the foundation for the roadmap's core strategic question: "under the same workload and safety constraints, when does the managed executor produce better outcomes than common thread-pool baselines?"

## What Changes

- **Baseline Executor Catalog**: Introduce `CommonExecutorPreset` record and `BaselineExecutorCatalog` registry with 6 JDK standard presets (fixed-2/4/8, cached, single, fixed-2-bounded)
- **Normalized Comparison Metrics**: Introduce `NormalizedComparisonMetrics` record with 9 cross-executor metrics computed from `List<ObservedSnapshot>`, plus `MetricDelta` for per-metric before/after comparison
- **Comparable Scenario Runner**: Introduce `ComparableScenarioRunner` that accepts a scenario, baseline preset ID, and managed executor config, runs both sequentially, and produces a `ComparisonResult` with full metric deltas
- **Managed Executor Rejection Counting**: Add `getRejectedTaskCount()` to `ManagedExecutor` via `AtomicLong` + transparent handler wrapper — prerequisite for the comparison framework's reliability dimension
- **ScenarioRunOutcome Extension**: Add `rejectedTaskCount` field (backward-compatible; old constructor defaults to 0)

All new components are in `experiment.scenario` package. No new external dependencies. No changes to existing interfaces (EvidenceRecorder, PressureSampler, ManagedExecutorConfig).

## Capabilities

### New Capabilities
- `baseline-executor-catalog`: Registry of common thread-pool executor presets with 6 JDK defaults, supporting registration and ID-based lookup
- `normalized-comparison-metrics`: Cross-executor normalized metrics record (9 fields) computable from ObservedSnapshot lists, plus per-metric delta computation
- `comparable-scenario-runner`: Runner that executes the same scenario against a baseline executor and a managed executor sequentially, producing a ComparisonResult
- `managed-executor-rejection-counting`: Rejection task counting via AtomicLong + transparent RejectedExecutionHandler wrapper, exposed as getRejectedTaskCount()

### Modified Capabilities
- (none — no existing spec-level requirement changes)

## Impact

- **New source files**: 6 records/classes in `experiment.scenario` (~500 lines)
- **Modified source files**: `ManagedExecutor.java` (~15 lines), `ScenarioRunOutcome.java` (~10 lines)
- **New test files**: ~8 unit/integration tests (~400 lines)
- **No changes to**: `EvidenceRecorder`, `PressureSampler`, `ManagedExecutorConfig`, `BaselineExecutorPreset`, `BaselineWorkloadExecutor`, `ScenarioExperimentRunner` interfaces
- **Breaking changes**: None (all additions are new or backward-compatible)
- **Dependencies**: No new external dependencies
