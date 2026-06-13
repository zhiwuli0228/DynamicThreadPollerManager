# acquisition-paths-and-quality-gates Verification

## Header

- Change identifier: `acquisition-paths-and-quality-gates`
- Verification date: (filled after implementation)
- Verifier: (filled after implementation)

## Minimum Checks

1. [ ] `openspec validate --all --json` is fully green (every item `"valid": true`).
2. [ ] Every synced main spec under `openspec/specs/acquisition-paths-and-quality-gates/spec.md` contains both `## Purpose` and `## Requirements` headers.
3. [ ] `docs/00-project/current-state.md`, `openspec list --json`, and the actual worktree state describe the same change.
4. [ ] `git status --short` was actually executed, output recorded.
5. [ ] `scripts/openspec-archive-guard.ps1 -Mode pre-finalize -ChangeName acquisition-paths-and-quality-gates` exited with status 0.

## Semantic Verification

### AcquisitionReportPaths.forVersion()
- [ ] `forVersion("v0.8.0").outputDirectory()` equals `"outputs/reports/v0.8.0"`.
- [ ] Null, blank, "../escape", "a/b" all throw `IllegalArgumentException`.
- [ ] `reportDirectory(Path.of("/root"))` returns `/root/outputs/reports/v0.8.0`.
- [ ] Existing static `OUTPUT_DIRECTORY` still `"outputs/reports/v0.6.0"`.
- [ ] Dual-arg `AcquisitionReportWriter` resolves correct directory.

### RunSnapshot Extension
- [ ] `extendedFieldPresence` defaults to `Map.of()` when null.
- [ ] `threadLeakFree` defaults to null when not set.
- [ ] Existing RunSnapshot construction without new fields compiles and passes.

### G7-G9 Gates
- [ ] G7 pass: all 5 required fields present → G7 in passed set.
- [ ] G7 fail: poolSize missing → G7 in failed set, blocking message contains runId.
- [ ] G7 skip: extendedFieldPresence empty → G7 not in passed or failed.
- [ ] G8 STEADY: queuePressure=0 → G8 passed (exempt).
- [ ] G8 RAMP: queuePressure=0 → G8 failed.
- [ ] G8 RAMP: queuePressure=1 → G8 passed.
- [ ] G8 BURST: queuePressure=1 → G8 failed.
- [ ] G8 BURST: queuePressure=2 → G8 passed.
- [ ] G9: threadLeakFree=true → passed.
- [ ] G9: threadLeakFree=false → failed (blocking).
- [ ] G9: threadLeakFree=null → skipped.

### AcquisitionReportBridge
- [ ] Bridge produces 4 JSON files (manifest, pressure, replay, evidenceIndex).
- [ ] No ReadinessSummary file produced.
- [ ] `RunManifest` contains correct scenarioId, profile, seed, config summary.
- [ ] `PressureSummary` has correct totalSnapshotCount, maxQueue, meanQueue.
- [ ] `ReplaySummary` has evidenceCount=snapshotCount, all decision counters=0.
- [ ] `EvidenceIndex.readinessSummaryPath` is null.

### Full 9-Run Data Acquisition
- [ ] All 9 runs complete without failure.
- [ ] All 9 runs pass G1-G9 gates.
- [ ] Output files exist under `outputs/reports/v0.8.0/`.
- [ ] Each run produces 4 JSON artifacts.

### Non-Regression
- [ ] `mvn test` passes all existing tests unchanged.
- [ ] `AcquisitionContractsTest` path assertions pass without modification.
- [ ] Existing G1-G6 gate tests pass without modification.

## Blocked Evidence

(List any evidence that could not be collected, with reason.)
