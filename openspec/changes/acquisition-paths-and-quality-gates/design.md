# acquisition-paths-and-quality-gates Design

## Header

- Change identifier: `acquisition-paths-and-quality-gates`
- Design purpose: version acquisition report output paths, extend data quality gates for real TPE data, and bridge runner output to the report pipeline
- Authoritative inputs:
  - `docs/04-development/versions/v0.8.0/10-ir.md`
  - `docs/04-development/versions/v0.8.0/20-sr.md`
  - `docs/04-development/versions/v0.8.0/decision-log.md`
  - `docs/00-project/current-state.md`
- Depends on: `real-executor-data-acquisition` (provides `ManagedExecutorConfig`, `ManagedExecutorScenarioRunner`, `fromExecutorState()`)

## 1. Scope

In scope:
- `AcquisitionReportPaths.forVersion(String)` static factory
- `AcquisitionDataSet.RunSnapshot` extension (extendedFieldPresence, threadLeakFree)
- `AcquisitionDataQualityValidator` G7-G9 gates
- `AcquisitionReportBridge` (runner-to-report)
- `AcquisitionReportWriter(Path, AcquisitionReportPaths)` dual-arg constructor
- Full 9-run data acquisition test (3 profiles x 3 seeds)
- No regression in existing tests

Out of scope:
- `ManagedExecutorConfig`, `ManagedExecutorScenarioRunner` (belongs to `real-executor-data-acquisition`)
- `SnapshotAssembler.fromExecutorState()` (belongs to `real-executor-data-acquisition`)
- `ReadinessSummary` in acquisition mode (deferred to later version)
- Queue resizing, closed-loop scheduling, persistence, REST/API

## 2. Package and Class Layout

```
experiment.acquisition (modified / new)
├── AcquisitionReportPaths.java           ← +forVersion(), +dual-arg ctor, instance fields
├── AcquisitionDataSet.java               ← RunSnapshot: +extendedFieldPresence, +threadLeakFree
├── AcquisitionDataQualityValidator.java  ← +G7, +G8, +G9 gate logic
├── AcquisitionReportWriter.java          ← +AcquisitionReportWriter(Path, AcquisitionReportPaths)
└── AcquisitionReportBridge.java          ← NEW: runner-to-report aggregation
```

## 3. Key Design Decisions

### 3.1 Versioned Paths

- `forVersion("v0.8.0")` → `outputDirectory = "outputs/reports/v0.8.0"`.
- Existing static constants (`OUTPUT_DIRECTORY = "outputs/reports/v0.6.0"`, `VERSION_TAG = "v0.6.0"`) retained for backward compatibility.
- Version tag validation: reject null, blank, path separators (`/`, `\`), or `..`.
- No auto-inference — callers explicitly pass version tag.

### 3.2 RunSnapshot Extension

```java
Map<String, Boolean> extendedFieldPresence,  // null → Map.of()
Boolean threadLeakFree                        // null → null (G9 skipped when null)
```
- G7 data source: `extendedFieldPresence` keys include `"poolSize"`, `"completedTaskCount"`, `"keepAliveTimeSeconds"`, `"largestPoolSize"`, `"taskCount"`.
- When map is empty (v0.6.0 data), G7 is skipped entirely.
- `threadLeakFree` null means "not checked" — G9 skipped.

### 3.3 G7-G9 Gate Logic

**G7 (Extended field presence)**: For each run where `extendedFieldPresence` is non-empty, verify all 5 required keys have value `true`. Missing or false → G7 failed (P0 blocking).

**G8 (Per-profile queue pressure)**:
- STEADY: exempt (queueSize=0 is expected).
- RAMP: >= 1 snapshot with queueSize > 0.
- BURST: >= 2 snapshots with queueSize > 0.
- Unknown/null profile: fallback to >= 1.

**G9 (Thread leak free)**:
- If `threadLeakFree` is non-null and false → G9 failed (P0 blocking).
- If null → G9 skipped (backward compatible with v0.6.0 data).

### 3.4 AcquisitionReportBridge

- Independent class in `experiment.acquisition`, not embedded in runner.
- Constructor: `AcquisitionReportBridge(Path outputRoot, String versionTag)` → internally creates `AcquisitionReportPaths` and `AcquisitionReportWriter` (dual-arg constructor).
- `bridge(outcome, definition, config, snapshots)` → writes 4 artifacts: `RunManifest`, `PressureSummary`, `ReplaySummary` (default values), `EvidenceIndex`.
- Does NOT produce `ReadinessSummary` in acquisition-only mode. Readiness evaluation belongs to later offline replay.
- `EvidenceIndex.readinessSummaryPath` set to null.

### 3.5 Dependencies

```
experiment.acquisition (AcquisitionReportBridge)
    ├── experiment.executor (ManagedExecutorConfig — pure data record)
    └── experiment.scenario (ScenarioRunOutcome, ScenarioDefinition — pure data classes)
```
These are new allowed dependency directions, limited to pure data types.

## 4. 9-Run Data Acquisition Matrix

| Profile | Seeds | Steps per run | Expected artifact count |
|---------|-------|---------------|------------------------|
| STEADY  | 101, 102, 103 | 8 | 4 JSON artifacts each |
| RAMP    | 201, 202, 203 | 8 | 4 JSON artifacts each |
| BURST   | 301, 302, 303 | 9 | 4 JSON artifacts each |

All 9 runs must pass G1-G9. Output to `outputs/reports/v0.8.0/`.

## 5. Verification Requirements

- `mvn test` exits 0 with all existing + new tests passing.
- `AcquisitionReportPaths.forVersion("v0.8.0")` → outputDirectory = `"outputs/reports/v0.8.0"`.
- Invalid version tags (null, "../escape", "a/b") throw `IllegalArgumentException`.
- G7: all 5 required fields present → pass; any missing → fail.
- G8: RAMP with 0 queue pressure → fail; BURST with 1 → fail; STEADY with 0 → pass.
- G9: threadLeakFree=false → fail; null → skipped.
- Bridge produces 4 JSON files in correct versioned directory.
- 9-run test completes, all G1-G9 pass.
- Existing `AcquisitionContractsTest` path assertions unchanged.
- Existing G1-G6 gate tests unchanged.

## 6. Closeout Steps

- Proposal, spec, tasks, design, and plan artifacts created.
- Verification and finalize artifacts serve as delivery gate templates.
- Current-state synchronized to reflect this change as active.
- Implementation authorized only after `EXECUTION_AUTHORIZED`.
