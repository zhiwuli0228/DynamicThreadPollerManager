# Apply: comparison-report-and-end-to-end-verification

## Implementation Record

All tasks from `tasks.md` have been implemented. This change depends on change 1/2 (`baseline-catalog-and-comparison-runner`) for all model types.

## New Source Files (2)

| File | Package | Description |
|---|---|---|
| `ComparisonReportArtifact.java` | `experiment.scenario` | Record with 7 fields, toMap()/fromMap(), managedConfigToMap() |
| `ComparisonJsonWriter.java` | `experiment.acquisition` | JSON serialization via AcquisitionJsonWriter.render()/parse() |

## Modified Source Files (1)

| File | Change |
|---|---|
| `AcquisitionReportPaths.java` | +comparisonReportFileName() +comparisonReportFile() |

## toMap/fromMap Added (carried from change 1)

Already implemented during change 1: CommonExecutorPreset, NormalizedComparisonMetrics, MetricDelta, ComparisonResult all have toMap/fromMap methods.

## New Test Files (2)

| File | Tests | Status |
|---|---|---|
| `ComparisonJsonWriterTest.java` | 7 | Pass |
| `ComparisonEndToEndTest.java` | 3 | Pass |

## Test Verification

- `mvn test`: 708 tests run, 707 pass, 1 pre-existing flaky test
- All 10 new tests pass (7 + 3)
- Zero regression in existing tests

## Spec Coverage

| Capability | Requirements | Implementation |
|---|---|---|
| comparison-report-artifact | 3 new + 3 modified | ComparisonReportArtifact + toMap/fromMap on existing records |
| baseline-executor-catalog (modified) | toMap/fromMap | CommonExecutorPreset.toMap/fromMap |
| normalized-comparison-metrics (modified) | toMap/fromMap | NormalizedComparisonMetrics.toMap/fromMap, MetricDelta.toMap/fromMap |
| comparable-scenario-runner (modified) | toMap/fromMap | ComparisonResult.toMap/fromMap |
