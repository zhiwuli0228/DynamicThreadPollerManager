## 1. AcquisitionReportPaths.forVersion()

- [x] 1.1 Add `forVersion(String versionTag)` static factory method.
- [x] 1.2 Implement version tag validation: reject null, blank, "/", "\", "..".
- [x] 1.3 Add private constructor `AcquisitionReportPaths(String versionTag)` setting `outputDirectory = "outputs/reports/" + versionTag`.
- [x] 1.4 Add instance methods: `outputDirectory()`, `versionTag()`, `reportDirectory(Path outputRoot)`, `runManifestFileName(String runId)`.
- [x] 1.5 Retain existing static constants `OUTPUT_DIRECTORY` and `VERSION_TAG` (backward compatible).
- [x] 1.6 Write unit test: `forVersion("v0.8.0")` returns correct paths, invalid tags rejected.

## 2. AcquisitionReportWriter Dual-Arg Constructor

- [x] 2.1 Add `AcquisitionReportWriter(Path outputRoot, AcquisitionReportPaths paths)` constructor.
- [x] 2.2 Delegate to existing single-arg constructor: `this(outputRoot.resolve(paths.outputDirectory()))`.
- [x] 2.3 Write unit test: dual-arg constructor resolves correct output path.

## 3. RunSnapshot Extension

- [x] 3.1 Add `Map<String, Boolean> extendedFieldPresence` field to `RunSnapshot` record (nullable, default Map.of()).
- [x] 3.2 Add `Boolean threadLeakFree` field to `RunSnapshot` record (nullable).
- [x] 3.3 Update compact constructor: null extendedFieldPresence → Map.of().
- [x] 3.4 Write unit test: new fields default correctly, settable via builder/constructor.

## 4. G7-G9 Data Quality Gates

- [x] 4.1 Define `REQUIRED_EXTENDED_FIELDS` constant: set of "poolSize", "completedTaskCount", "keepAliveTimeSeconds", "largestPoolSize", "taskCount".
- [x] 4.2 Implement G7: for each run with non-empty extendedFieldPresence, verify all required keys are true.
- [x] 4.3 Implement G8 per-profile: STEADY exempt, RAMP >= 1 queue pressure, BURST >= 2 queue pressure.
- [x] 4.4 Implement G9: threadLeakFree != null && !threadLeakFree → fail.
- [x] 4.5 G7 skipped when extendedFieldPresence is empty (backward compatible with v0.6.0 data).
- [x] 4.6 G9 skipped when threadLeakFree is null.
- [x] 4.7 Write unit tests: G7 pass/fail, G8 per-profile pass/fail, G9 pass/fail/skip, G1-G6 unchanged.

## 5. AcquisitionReportBridge

- [x] 5.1 Create `AcquisitionReportBridge` in `experiment.acquisition` package.
- [x] 5.2 Constructor: `AcquisitionReportBridge(Path outputRoot, String versionTag)`.
- [x] 5.3 Implement `bridge(ScenarioRunOutcome, ScenarioDefinition, ManagedExecutorConfig, List<ObservedSnapshot>)` method.
- [x] 5.4 Build `RunManifest` from outcome + definition + config.
- [x] 5.5 Aggregate `PressureSummary` from snapshots: totalCount, maxQueue, meanQueue.
- [x] 5.6 Build default `ReplaySummary` (evidenceCount=snapshotCount, all counters=0).
- [x] 5.7 Build `EvidenceIndex` with readinessPath=null.
- [x] 5.8 Write 4 artifacts via `AcquisitionReportWriter.writeAll()`.
- [x] 5.9 Write unit test: bridge produces files in correct directory, all 4 JSON files exist.

## 6. Full 9-Run Data Acquisition Test

- [x] 6.1 Create test class with 9 parameterized or sequential runs.
- [x] 6.2 STEADY seeds 1, 2, 3.
- [x] 6.3 RAMP seeds 1, 2, 3.
- [x] 6.4 BURST seeds 1, 2, 3.
- [x] 6.5 Each run: execute runner → bridge → validate G1-G9 all pass.
- [x] 6.6 Verify all output files in `outputs/reports/v0.8.0/`.
- [x] 6.7 Verify existing tests not modified.

## 7. Test Suite Verification

- [x] 7.1 `mvn test` exits 0 — all existing + new tests pass.
- [x] 7.2 `AcquisitionContractsTest` path assertions pass without modification.
- [x] 7.3 No regression in existing G1-G6 gate behavior.
