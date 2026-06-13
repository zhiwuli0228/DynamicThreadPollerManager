## 1. ManagedExecutorConfig

- [x] 1.1 Create `ManagedExecutorConfig` record in `experiment.executor` package: fields `corePoolSize` (int), `maximumPoolSize` (int), `queueCapacity` (int), `keepAliveTime` (long), `keepAliveTimeUnit` (TimeUnit).
- [x] 1.2 Implement compact constructor with invariants: corePoolSize > 0, maximumPoolSize >= corePoolSize, queueCapacity >= 0, keepAliveTime >= 0, keepAliveTimeUnit not null.
- [x] 1.3 Implement `defaultConfig()` static factory: core=2, max=4, queue=10, keepAlive=60s.
- [x] 1.4 Implement `toManagedExecutor()`: creates `ManagedExecutor` with `LinkedBlockingQueue<>(queueCapacity)`.
- [x] 1.5 Implement `toPresetSummary()`: returns `RunManifest.BaselinePresetSummary` with policyId="managed-executor-v0.8.0".
- [x] 1.6 Write unit test `ManagedExecutorConfigTest`: invariant violations, defaultConfig values, toManagedExecutor round-trip, toPresetSummary correctness.

## 2. SnapshotAssembler.fromExecutorState()

- [x] 2.1 Add `fromExecutorState(String runId, ExecutorStateSnapshot state)` default method to `SnapshotAssembler` interface.
- [x] 2.2 Map fields: activeThreads←activeCount, poolSize←poolSize, queueSize←queueSize, completedTaskCount←completedTaskCount, cpuUtilization←absent, timestamp←observedAt.
- [x] 2.3 Existing `DefaultSnapshotAssemblerTest` and `ManualPressureSamplerTest` pass without modification.

## 3. ManualPressureSampler.sampleFromExecutorState()

- [x] 3.1 Add `sampleFromExecutorState(String runId, ExecutorStateSnapshot state)` overload to `ManualPressureSampler`.
- [x] 3.2 Delegate to `SnapshotAssembler.fromExecutorState()`.

## 4. ManagedExecutorScenarioRunner

- [x] 4.1 Create `ManagedExecutorScenarioRunner` in `experiment.scenario` package: constructor accepts `ExperimentCoordinator`, `ScenarioPlanner`, `PressureSampler`, `EvidenceRecorder`, `Supplier<Instant>` clock.
- [x] 4.2 Implement `run(ScenarioDefinition, ManagedExecutorConfig)` method with 7 phases.
- [x] 4.3 Phase 1: Create `ManagedExecutor` from config, register to internal `ExecutorRegistry` (null DeletionSafety). Note: `ExecutorRegistry` constructor modified to accept nullable DeletionSafety.
- [x] 4.4 Phase 2: Coordinate `ExperimentCoordinator` lifecycle (createRun → startRun).
- [x] 4.5 Phase 3 (per-step): Submit blocking tasks with CountDownLatch → startedLatch.await(5s) → buildObservation from executor getters → sampler.sample → recorder.record → blocker.countDown → waitForIdle.
- [x] 4.6 Implement `taskCountFor(profile, stepIndex, config)` with cap protection.
- [x] 4.7 Implement `waitForIdle(executor)` with 10s timeout (warning, not failure).
- [x] 4.8 Phase 4: `coordinator.stopRun` → `finalizeRun`.
- [x] 4.9 Phase 5: `shutdownAndTerminate(executor)` — shutdown → awaitTermination(10s) → shutdownNow on timeout.
- [x] 4.10 Phase 6: Verify `isTerminated()` then `registry.remove()`.
- [x] 4.11 Phase 7: Return `ScenarioRunOutcome` with runId, scenarioId, policyId, stepCount, totalWorkUnits, observedSnapshotCount, finalRunState.
- [x] 4.12 Implement `buildObservation(ManagedExecutor, Instant)` helper — reads directly from executor getters to avoid experiment.adjustment package import (boundary isolation compliance).

## 5. Runner Integration Tests

- [x] 5.1 Create `ManagedExecutorScenarioRunnerTest` in test source root.
- [x] 5.2 STEADY profile test: verify 8 steps, expected snapshot count, all snapshots recorded.
- [x] 5.3 RAMP profile test: verify at least one snapshot has queueSize > 0.
- [x] 5.4 BURST profile test: verify burst steps have higher queueSize than adjacent non-burst.
- [x] 5.5 Cleanup test: run() completes successfully, executor terminated in Phase 6.
- [x] 5.6 Exception path test: simulated failure triggers RuntimeException, executor shutdown.
- [x] 5.7 All tests use real `ManagedExecutor` (no mock ThreadPoolExecutor).

## 6. Test Suite Verification

- [x] 6.1 `mvn test` exits 0 — 432 tests pass (412 original + 20 new), 0 failures.
- [x] 6.2 No regression in ScenarioExperimentRunner or BaselineWorkloadExecutor tests.
- [x] 6.3 No thread leaks across all test classes.

## Implementation Notes

- `buildObservation()` takes `ManagedExecutor` directly (reads getters) instead of `ExecutorStateSnapshot` to maintain `experiment.scenario` → `experiment.adjustment` boundary isolation per `AdjustmentBoundaryIsolationTest`.
- `ExecutorRegistry` constructor relaxed to accept nullable `DeletionSafety`, with null-safe `remove()`.
- Runner creates `ExecutorRegistry(null)` — single-threaded sequential execution needs no reference counting.
