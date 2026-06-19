# Plan: comparison-report-and-end-to-end-verification

## Prerequisites

Change 1/2 (`baseline-catalog-and-comparison-runner`) must be fully implemented before this change can begin. All model types (CommonExecutorPreset, NormalizedComparisonMetrics, MetricDelta, ComparisonResult, ComparableScenarioRunner) must be available in `experiment.scenario` package.

## Implementation Order

1. **ComparisonReportArtifact** (Task 1) — simple record, no dependencies beyond change 1 types
2. **toMap/fromMap on records** (Task 2) — extends models from change 1 with Map serialization
3. **ComparisonJsonWriter** (Task 3) — depends on task 2 (toMap/fromMap must exist)
4. **AcquisitionReportPaths Extension** (Task 4) — no dependencies, can proceed in parallel with 1-3
5. **End-to-End Integration Tests** (Task 5) — depends on all above + change 1's ComparableScenarioRunner
6. **Full Test Verification** (Task 6) — final gate

## Parallelism Opportunities

- Tasks 1, 4 can proceed in parallel
- Task 2 depends on change 1's existing record types but not on task 1
- Task 5 requires full change 1 + this change's tasks 1-4

## Test Strategy

- toMap/fromMap: round-trip unit tests for each record type
- ComparisonJsonWriter: write/read round-trip with temporary directory
- End-to-end: real scenario + real executors + file I/O
- Existing 646 tests must continue to pass

## Deliverable Files

**New source files**:
- `experiment/scenario/ComparisonReportArtifact.java`
- `experiment/acquisition/ComparisonJsonWriter.java`

**Modified source files** (add toMap/fromMap):
- `experiment/scenario/CommonExecutorPreset.java`
- `experiment/scenario/NormalizedComparisonMetrics.java`
- `experiment/scenario/MetricDelta.java`
- `experiment/scenario/ComparisonResult.java`
- `experiment/acquisition/AcquisitionReportPaths.java`

**New test files**:
- `ComparisonReportArtifactTest.java`
- `ComparisonJsonWriterTest.java`
- `ComparisonEndToEndTest.java`

## Verification Gate

- `mvn test` passes with zero failures (existing 646 + change 1 new + change 2 new)
- End-to-end comparison report file is valid JSON and contains correct delta values
