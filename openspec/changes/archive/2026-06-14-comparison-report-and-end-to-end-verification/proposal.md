## Why

Change 1/2 delivers the comparison runner and metrics model, but the comparison results exist only in memory. There is no persistent report artifact that captures the full baseline-vs-managed comparison in a reviewable, serializable format. This change adds JSON report serialization using the project's established toMap/fromMap + render/parse pattern, plus end-to-end integration verification proving the complete baseline→managed→report→read flow.

## What Changes

- **ComparisonReportArtifact**: Record carrying comparisonId, scenarioId, baselinePreset, managedConfig, ComparisonResult, and optional conclusion text
- **toMap/fromMap on record types**: Add Map serialization to comparison model records (CommonExecutorPreset, NormalizedComparisonMetrics, MetricDelta, ComparisonResult, ComparisonReportArtifact) following v0.11.0's pattern
- **ComparisonJsonWriter**: JSON serializer in `experiment.acquisition` package, delegating to `AcquisitionJsonWriter.render()`/`parse()`
- **AcquisitionReportPaths Extension**: `comparisonReportFileName()` and `comparisonReportFile()` for centralized path management
- **End-to-end verification**: Integration tests covering catalog→compare→write report→read→verify deltas

All JSON serialization uses hand-written code (no external dependencies). File naming follows existing `{id}-{descriptor}.{ext}` convention.

## Capabilities

### New Capabilities
- `comparison-report-artifact`: JSON-serializable comparison report combining preset config, managed config, normalized metrics from both runs, per-metric deltas, and human-readable conclusion

### Modified Capabilities
- `baseline-executor-catalog`: Add `toMap()`/`fromMap()` to `CommonExecutorPreset`
- `normalized-comparison-metrics`: Add `toMap()`/`fromMap()` to `NormalizedComparisonMetrics` and `MetricDelta`
- `comparable-scenario-runner`: Add `toMap()`/`fromMap()` to `ComparisonResult`

## Impact

- **New source files**: `ComparisonReportArtifact.java` (scenario), `ComparisonJsonWriter.java` (acquisition)
- **Modified source files**: ~5 records get toMap/fromMap methods (~15 lines each), `AcquisitionReportPaths.java` (~15 lines)
- **New test files**: ~3 test files (~200 lines)
- **No changes to**: existing interfaces, ManagedExecutor, EvidenceRecorder, PressureSampler
- **Dependencies**: Depends on change 1/2 (`baseline-catalog-and-comparison-runner`) for all model types
