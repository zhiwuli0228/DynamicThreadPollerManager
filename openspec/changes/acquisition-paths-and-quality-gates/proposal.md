## Why

v0.6.0's acquisition report pipeline hardcodes `outputs/reports/v0.6.0` and its data quality gates (G1-G6) only validate BaselineWorkloadExecutor data — they don't check whether real ThreadPoolExecutor extended fields (poolSize, completedTaskCount, etc.) are present, whether queue pressure evidence exists per profile, or whether threads have leaked. Without versioned output paths, each version's acquisition data overwrites the previous. Without G7-G9, there's no gate ensuring real-executor data quality matches what `ManagedExecutorScenarioRunner` produces.

## What Changes

**AcquisitionReportPaths.forVersion()**
- From: `OUTPUT_DIRECTORY` hardcoded to `outputs/reports/v0.6.0`.
- To: `forVersion(String versionTag)` static factory, version-tagged output directories. Existing constants retained for backward compatibility.
- Reason: Multiple version datasets coexist without overwriting.
- Impact: Modified `AcquisitionReportPaths` in `experiment.acquisition`. Existing tests unaffected.

**RunSnapshot extension (extendedFieldPresence, threadLeakFree)**
- From: `RunSnapshot` has no awareness of TPE extended fields.
- To: Two new nullable fields: `extendedFieldPresence` (Map<String, Boolean>) and `threadLeakFree` (Boolean). Both default to null/empty when absent.
- Reason: G7 and G9 gates need this data to validate real-executor quality.
- Impact: Modified `RunSnapshot` record. Backward compatible (new fields nullable with defaults).

**G7-G9 data quality gates**
- From: Validator checks G1-G6 only (profile coverage, snapshot count, time ordering, metadata).
- To: G7 (extended field presence — poolSize, completedTaskCount, keepAliveTimeSeconds, largestPoolSize, taskCount all non-null), G8 (per-profile queue pressure — STEADY exempt, RAMP >= 1, BURST >= 2 snapshots with queueSize > 0), G9 (thread leak free).
- Reason: Real TPE data has quality requirements beyond what BaselineWorkloadExecutor data needs.
- Impact: Modified `AcquisitionDataQualityValidator` in `experiment.acquisition`. G1-G6 unchanged.

**AcquisitionReportBridge**
- From: No bridge between runner output and acquisition report pipeline.
- To: `AcquisitionReportBridge` aggregating runner output into `RunManifest`, `PressureSummary`, `ReplaySummary` (default), and `EvidenceIndex` (4 artifacts, no `ReadinessSummary` in acquisition-only mode).
- Reason: Runner and report pipeline are separate concerns; bridge connects them without coupling.
- Impact: New class in `experiment.acquisition`. Depends on `ManagedExecutorConfig` (pure data record) and `ScenarioRunOutcome`/`ScenarioDefinition` (pure data classes).

**Full 9-run data acquisition**
- From: No standardized multi-scenario acquisition dataset exists.
- To: Execute 3 profiles × 3 seeds = 9 runs, all passing G1-G9, producing versioned reports.
- Reason: Concrete evidence that the pipeline works end-to-end.
- Impact: Test class executing 9 runs, output to `outputs/reports/v0.8.0/`.

## Capabilities

### New Capabilities
- `acquisition-paths-and-quality-gates`: `AcquisitionReportPaths.forVersion()`, `RunSnapshot` extended fields, G7-G9 gates, `AcquisitionReportBridge`, 9-run data acquisition.

### Modified Capabilities
- `pressure-data-acquisition-and-baseline`: `RunSnapshot` record gains two nullable fields. `AcquisitionDataQualityValidator` gains G7-G9 checks. Existing path assertions and G1-G6 behavior unchanged.

## Impact

Modified: `AcquisitionReportPaths` (new factory, backward compatible), `AcquisitionDataSet.RunSnapshot` (2 new nullable fields), `AcquisitionDataQualityValidator` (G7-G9). New: `AcquisitionReportBridge` (`experiment.acquisition`). New dependency direction: `experiment.acquisition` → `experiment.executor` (ManagedExecutorConfig, pure data record) and `experiment.acquisition` → `experiment.scenario` (ScenarioRunOutcome, ScenarioDefinition, pure data classes). No existing tests modified.
