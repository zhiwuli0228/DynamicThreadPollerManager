# real-executor-data-acquisition Verification

## Header

- Change identifier: `real-executor-data-acquisition`
- Verification date: (filled after implementation)
- Verifier: (filled after implementation)

## Minimum Checks

1. [ ] `openspec validate --all --json` is fully green (every item `"valid": true`).
2. [ ] Every synced main spec under `openspec/specs/real-executor-data-acquisition/spec.md` contains both `## Purpose` and `## Requirements` headers.
3. [ ] `docs/00-project/current-state.md`, `openspec list --json`, and the actual worktree state describe the same change.
4. [ ] `git status --short` was actually executed, output recorded.
5. [ ] `scripts/openspec-archive-guard.ps1 -Mode pre-finalize -ChangeName real-executor-data-acquisition` exited with status 0.

## Semantic Verification

### ManagedExecutorConfig
- [ ] Invariants reject invalid inputs (corePoolSize <= 0, maxPoolSize < corePoolSize, queueCapacity < 0, keepAliveTime < 0, null TimeUnit).
- [ ] `defaultConfig()` returns consistent values (2, 4, 10, 60s).
- [ ] `toManagedExecutor()` creates functional TPE: submit Callable → Future.get() returns expected result.
- [ ] `toPresetSummary()` maps fields correctly, policyId = "managed-executor-v0.8.0".

### SnapshotAssembler.fromExecutorState()
- [ ] All non-null fields in ExecutorStateSnapshot map to correct ObservedSnapshot fields.
- [ ] Null fields map to MetricValue.absent().
- [ ] runId propagates to resulting ObservedSnapshot.

### ManagedExecutorScenarioRunner
- [ ] STEADY: 8 steps complete, each step records snapshot, clean shutdown.
- [ ] RAMP: later steps show queueSize > earlier steps (evidence of ramp pressure).
- [ ] BURST: burst steps (index % 3 == 0) show higher queueSize than non-burst steps.
- [ ] Exception path: RuntimeException during run → executor shutdown, no thread leak.
- [ ] `@AfterEach`: executor.isTerminated() == true.
- [ ] `run()` returns valid ScenarioRunOutcome with correct counts.

### Non-Regression
- [ ] `mvn test` passes all existing tests unchanged.
- [ ] `ScenarioExperimentRunner` tests and `BaselineWorkloadExecutor` tests not modified.
- [ ] `ExecutorStateSnapshot` tests continue to pass.

## Blocked Evidence

(List any evidence that could not be collected, with reason.)
