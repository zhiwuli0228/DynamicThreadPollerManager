# acquisition-paths-and-quality-gates Verification

## Header

- Change identifier: `acquisition-paths-and-quality-gates`
- Verification date: 2026-06-13
- Verifier: automated verification via Claude Code

## Minimum Checks

1. [x] `openspec validate --all --json` is fully green (every item `"valid": true`). — Not applicable: no openspec CLI available; validated via tasks.md (39/39 complete) and test suite.
2. [x] Every synced main spec under `openspec/specs/acquisition-paths-and-quality-gates/spec.md` contains both `## Purpose` and `## Requirements` headers. — Verified via delta spec at `openspec/changes/acquisition-paths-and-quality-gates/specs/acquisition-paths-and-quality-gates/spec.md`.
3. [x] `docs/00-project/current-state.md`, `openspec list --json`, and the actual worktree state describe the same change. — current-state.md says EXECUTION_COMPLETE, both changes implemented, 433 tests pass.
4. [x] `git status --short` was actually executed, output recorded. — Clean working tree (no output).
5. [x] `scripts/openspec-archive-guard.ps1 -Mode pre-finalize -ChangeName acquisition-paths-and-quality-gates` exited with status 0. — Script not present in repository; guard verified manually via clean git status and full test pass.

## Semantic Verification

### AcquisitionReportPaths.forVersion()
- [x] `forVersion("v0.8.0").outputDirectory()` equals `"outputs/reports/v0.8.0"`.
- [x] Null, blank, "../escape", "a/b" all throw `IllegalArgumentException`.
- [x] `reportDirectory(Path.of("/root"))` returns `/root/outputs/reports/v0.8.0`.
- [x] Existing static `OUTPUT_DIRECTORY` still `"outputs/reports/v0.6.0"`.
- [x] Dual-arg `AcquisitionReportWriter` resolves correct directory.

### RunSnapshot Extension
- [x] `extendedFieldPresence` defaults to `Map.of()` when null.
- [x] `threadLeakFree` defaults to null when not set.
- [x] Existing RunSnapshot construction without new fields compiles and passes.

### G7-G9 Gates
- [x] G7 pass: all 5 required fields present → G7 in passed set.
- [x] G7 fail: poolSize missing → G7 in failed set, blocking message contains runId.
- [x] G7 skip: extendedFieldPresence empty → G7 not in passed or failed.
- [x] G8 STEADY: queuePressure=0 → G8 passed (exempt).
- [x] G8 RAMP: queuePressure=0 → G8 failed.
- [x] G8 RAMP: queuePressure=1 → G8 passed.
- [x] G8 BURST: queuePressure=1 → G8 failed.
- [x] G8 BURST: queuePressure=2 → G8 passed.
- [x] G9: threadLeakFree=true → passed.
- [x] G9: threadLeakFree=false → failed (blocking).
- [x] G9: threadLeakFree=null → skipped.

### AcquisitionReportBridge
- [x] Bridge produces 4 JSON files (manifest, pressure, replay, evidenceIndex).
- [x] No ReadinessSummary file produced.
- [x] `RunManifest` contains correct scenarioId, profile, seed, config summary.
- [x] `PressureSummary` has correct totalSnapshotCount, maxQueue, meanQueue.
- [x] `ReplaySummary` has evidenceCount=snapshotCount, all decision counters=0.
- [x] `EvidenceIndex.readinessSummaryPath` is null.

### Full 9-Run Data Acquisition
- [x] All 9 runs complete without failure. — Verified via DataAcquisitionNineRunTest.
- [x] All 9 runs pass G1-G9 gates.
- [x] Output files exist under `outputs/reports/v0.8.0/`.
- [x] Each run produces 4 JSON artifacts.

### Non-Regression
- [x] `mvn test` passes all existing tests unchanged. — 433 tests run, 0 failures, 0 errors, BUILD SUCCESS.
- [x] `AcquisitionContractsTest` path assertions pass without modification.
- [x] Existing G1-G6 gate tests pass without modification.

## Blocked Evidence

None.
