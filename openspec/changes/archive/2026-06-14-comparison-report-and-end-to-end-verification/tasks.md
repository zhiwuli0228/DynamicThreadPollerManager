# Tasks: comparison-report-and-end-to-end-verification

## 1. ComparisonReportArtifact

- [ ] 1.1 Create `ComparisonReportArtifact` record in `experiment.scenario` with fields: comparisonId, scenarioId, createdAt, baselinePreset (CommonExecutorPreset), managedConfig (ManagedExecutorConfig), result (ComparisonResult), conclusion (nullable String)
- [ ] 1.2 Write unit test: construction with all fields, null conclusion allowed

## 2. toMap/fromMap on Comparison Model Records

- [ ] 2.1 Add `toMap()` and `fromMap()` to `CommonExecutorPreset` — include description only if non-null
- [ ] 2.2 Add `toMap()` and `fromMap()` to `NormalizedComparisonMetrics` — 9 numeric fields
- [ ] 2.3 Add `toMap()` and `fromMap()` to `MetricDelta` — 6 fields
- [ ] 2.4 Add `toMap()` and `fromMap()` to `ComparisonResult` — nested maps for metrics, deltas, createdAt as ISO-8601 string
- [ ] 2.5 Add `toMap()` and `fromMap()` to `ComparisonReportArtifact` — full graph serialization with managedConfigToMap helper
- [ ] 2.6 Write round-trip unit tests for each record type

## 3. ComparisonJsonWriter

- [ ] 3.1 Create `ComparisonJsonWriter` class in `experiment.acquisition` with constructor(AcquisitionReportPaths)
- [ ] 3.2 Implement `writeComparisonReport(ComparisonReportArtifact)`: artifact.toMap() → AcquisitionJsonWriter.render(map) → write to file at paths.outputDirectory()/comparisonReportFileName()
- [ ] 3.3 Implement `writeComparisonReport(ComparisonReportArtifact, Path)` overload for custom output paths (test convenience)
- [ ] 3.4 Implement `readComparisonReport(Path)`: read file → AcquisitionJsonWriter.parse(json) → cast to Map → ComparisonReportArtifact.fromMap(map)
- [ ] 3.5 Write unit test: write → file exists and is valid JSON
- [ ] 3.6 Write unit test: write → read → all fields identical (round-trip)
- [ ] 3.7 Write unit test: read from file with invalid JSON throws exception

## 4. AcquisitionReportPaths Extension

- [ ] 4.1 Add `comparisonReportFileName(String comparisonId)` static method: `{comparisonId}-comparison.json`
- [ ] 4.2 Add `comparisonReportFile(Path outputRoot, String comparisonId)` static method: resolves to `outputs/reports/v0.12.0/{comparisonId}-comparison.json`
- [ ] 4.3 Write unit test: verify file name format, verify path resolution

## 5. End-to-End Integration Tests

- [ ] 5.1 Test: `BaselineExecutorCatalog.withDefaults()` → verify 6 presets, get("fixed-4") values correct
- [ ] 5.2 Test: `NormalizedComparisonMetrics.fromSnapshots()` from a 100-step scenario → completedTaskCount==100, throughput>0
- [ ] 5.3 Test: `ComparableScenarioRunner.compare(scenario, "fixed-2-bounded", managedConfig)` → ComparisonResult with different runIds, 9 deltas present
- [ ] 5.4 Test: `ComparisonJsonWriter.writeComparisonReport(artifact)` → file exists → read back → verify all deltas match original ComparisonResult
- [ ] 5.5 Test: Complete e2e: catalog → compare → write report → read report → verify deltas → verify conclusion
- [ ] 5.6 Test: Verify managed executor regression results are preserved (not hidden) — if managed is slower, delta shows REGRESSED direction

## 6. Full Test Verification

- [ ] 6.1 Run `mvn test` — verify all existing 646 tests pass (zero regression)
- [ ] 6.2 Verify all new tests from change 1 and change 2 pass
