# Brainstorm: comparison-report-and-end-to-end-verification

## Design Summary

This change delivers the comparison report artifact and end-to-end verification for the v0.12.0 baseline comparison framework. It builds on change 1/2 (`baseline-catalog-and-comparison-runner`) which provides the `ComparableScenarioRunner`, `ComparisonResult`, and `NormalizedComparisonMetrics`.

The design follows v0.11.0's toMap/fromMap + render/parse pattern: each record type carries its own Map conversion logic, and `ComparisonJsonWriter` delegates to `AcquisitionJsonWriter.render()` and `AcquisitionJsonWriter.parse()`. This keeps `ComparisonJsonWriter` small (~40 lines) and consistent with the established architecture.

Output: `outputs/reports/v0.12.0/{comparisonId}-comparison.json` (following existing `{id}-{descriptor}.{ext}` naming convention).

## Alternatives Considered

### Alternative A: Direct StringBuilder JSON (SR original approach)
- **Approach**: `ComparisonJsonWriter` uses StringBuilder + String.format to serialize each record type directly
- **Pros**: No intermediate Map allocation, slightly faster
- **Cons**: ~120 lines of manual JSON formatting, coupling writer to all record internals, no escape handling consistency with v0.11.0
- **Why not chosen**: Violates v0.11.0's toMap/fromMap architecture. SR review F03 corrected this

### Alternative B: Jackson/Gson external dependency
- **Approach**: Use Jackson ObjectMapper for serialization
- **Cons**: Introduces external dependency, violates operational-and-evolution-boundaries.md
- **Why not chosen**: Project policy — no external serialization dependencies

## Agreed Approach

1. Add `toMap()`/`fromMap()` to records: ComparisonReportArtifact, CommonExecutorPreset (extend from change 1), NormalizedComparisonMetrics (extend), MetricDelta (extend), ComparisonResult (extend)
2. `ComparisonJsonWriter` in `experiment.acquisition`: delegates serialization to `AcquisitionJsonWriter.render(map)`, deserialization to `AcquisitionJsonWriter.parse(json)` + `fromMap()`
3. `AcquisitionReportPaths` extension: `comparisonReportFileName()` and `comparisonReportFile()`
4. End-to-end integration tests: catalog + compare + write report + read report + verify deltas

## Key Decisions

- D4 (decision-log): Single JSON file per comparison
- F03 fix (SR review): toMap/fromMap pattern alignment with v0.11.0
- F02 fix: `{comparisonId}-comparison.json` naming convention
- ManagedExecutorConfig JSON: 6 fields with `TimeUnit.name()`/`ThreadMode.name()` enum serialization

## Open Questions

None — all design questions resolved through IR/SR process.
