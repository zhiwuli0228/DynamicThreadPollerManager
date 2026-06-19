# comparable-scenario-runner

## ADDED Requirements

### Requirement: ComparableScenarioRunner SHALL execute baseline and managed runs sequentially

The system MUST provide a `ComparableScenarioRunner` that accepts a scenario, baseline preset ID, and managed executor config, runs both executor types sequentially, and returns a `ComparisonResult`.

#### Scenario: Sequential execution produces outcomes for both runs
- **WHEN** `compare(scenario, "fixed-4", managedConfig)` is called
- **THEN** the returned `ComparisonResult` contains a `baselineOutcome` and a `managedOutcome` with the same `scenarioId`, and their `runId` values are different

#### Scenario: Baseline preset not found fails fast before managed run
- **WHEN** `compare(scenario, "nonexistent", managedConfig)` is called
- **THEN** `NoSuchElementException` is thrown and no managed run is attempted

#### Scenario: ComparisonResult includes normalized metrics for both runs
- **WHEN** `compare(scenario, "fixed-2-bounded", managedConfig)` completes successfully
- **THEN** both `baselineMetrics` and `managedMetrics` are non-null `NormalizedComparisonMetrics` with `snapshotCount > 0`

#### Scenario: ComparisonResult includes 9 per-metric deltas
- **WHEN** `compare(scenario, "fixed-4", managedConfig)` completes successfully
- **THEN** `result.deltas()` contains exactly 9 entries, each keyed by metric name with a `MetricDelta` value

#### Scenario: managed executor rejectedTaskCount is read from outcome
- **WHEN** the managed executor rejects some tasks during a run
- **THEN** `managedMetrics.rejectedTaskCount()` reflects the actual rejection count (non-zero if rejections occurred)

### Requirement: ComparableScenarioRunner SHALL create runner instances dynamically

The `ComparableScenarioRunner` MUST create fresh `ScenarioExperimentRunner` and `ManagedExecutorScenarioRunner` instances for each `compare()` call.

#### Scenario: Two consecutive compare calls with different presets both succeed
- **WHEN** `compare(scenario, "fixed-2", config)` followed by `compare(scenario, "fixed-8", config)` are called
- **THEN** both calls return valid `ComparisonResult` with the expected `baselinePresetId` ("fixed-2" and "fixed-8" respectively)
