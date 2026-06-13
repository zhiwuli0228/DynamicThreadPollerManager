# real-executor-data-acquisition Design

## Header

- Change identifier: `real-executor-data-acquisition`
- Design purpose: build the core data acquisition engine on real ManagedExecutor with metrics pipeline integration
- Authoritative inputs:
  - `docs/04-development/versions/v0.8.0/10-ir.md`
  - `docs/04-development/versions/v0.8.0/20-sr.md`
  - `docs/04-development/versions/v0.8.0/decision-log.md`
  - `docs/00-project/current-state.md`

## 1. Scope

In scope:
- `ManagedExecutorConfig` record with invariants, `toManagedExecutor()`, `toPresetSummary()`
- `ManagedExecutorScenarioRunner` with 7-phase execution, startedLatch sync, direct `toSnapshot()`
- `SnapshotAssembler.fromExecutorState()` default method
- `ManualPressureSampler.sampleFromExecutorState()` convenience overload
- Unit tests for config, assembler; integration tests for runner (3 profiles x min 1 seed)
- No regression in existing 412 tests

Out of scope:
- `AcquisitionReportPaths.forVersion()` (belongs to `acquisition-paths-and-quality-gates`)
- G7-G9 gates (belongs to `acquisition-paths-and-quality-gates`)
- `AcquisitionReportBridge` (belongs to `acquisition-paths-and-quality-gates`)
- Full 9-run data acquisition execution (belongs to `acquisition-paths-and-quality-gates`)
- Queue resizing, closed-loop scheduling, persistence, REST/API

## 2. Package and Class Layout

```
experiment.executor (new class)
└── ManagedExecutorConfig.java          ← record, invariants, toManagedExecutor, toPresetSummary

experiment.scenario (new class)
└── ManagedExecutorScenarioRunner.java  ← 7-phase, startedLatch, executor.toSnapshot() direct

experiment.metrics (modified)
├── SnapshotAssembler.java              ← +fromExecutorState() default method
└── ManualPressureSampler.java          ← +sampleFromExecutorState() overload
```

## 3. Key Design Decisions

- `ManagedExecutorConfig` is an immutable record; compact constructor validates all invariants.
- `toPresetSummary().policyId()` returns fixed string `"managed-executor-v0.8.0"` — semantic placeholder, not a real policy ID. Known cost of reusing existing schema.
- Runner creates `ExecutorRegistry` internally with `null` DeletionSafety — single-threaded sequential execution needs no reference counting.
- Runner uses `executor.toSnapshot()` directly, not `ManagedExecutorAdjustmentAdapter` — runner only samples, never adjusts.
- `startedLatch` sync barrier (5s timeout) before each step's sampling ensures threads have started.
- Idle check: `queueSize == 0 && activeCount == 0` with 10s timeout; timeout is a warning, not failure.
- Cleanup follows v0.7.0 P6: `@AfterEach` countDown all latches → shutdown → awaitTermination → shutdownNow on timeout.
- `fromExecutorState()` is a default method → binary compatible for all existing `SnapshotAssembler` implementations.

## 4. Profile Task Count Rules

| Profile | Steps | Task count per step |
|---------|-------|---------------------|
| STEADY  | 8     | fixed 2             |
| RAMP    | 8     | 2 + stepIndex, capped at `queueCapacity + maxPoolSize` (14 for defaults) |
| BURST   | 9     | 6 at stepIndex % 3 == 0, else 2 |

## 5. Verification Requirements

- `mvn test` exits 0 with all existing + new tests passing.
- `ManagedExecutorConfig` invariants: reject corePoolSize <= 0, maxPoolSize < corePoolSize, queueCapacity < 0, keepAliveTime < 0, null keepAliveTimeUnit.
- `toManagedExecutor()` creates working TPE: submit task → Future.get() returns expected value.
- Runner STEADY: 8 steps × 2 tasks = 16 snapshots recorded.
- Runner RAMP: queueSize trend increases across steps.
- Runner BURST: burst steps show higher queueSize than non-burst steps.
- Runner cleanup: @AfterEach verifies executor.isTerminated() == true.
- `fromExecutorState()` maps all fields correctly including null→absent.
- No thread leaks in any test.

## 6. Closeout Steps

- Proposal, spec, tasks, design, and plan artifacts created.
- Verify, finalize artifacts serve as delivery gate templates.
- Current-state synchronized to reflect this change as active.
- Implementation authorized only after `EXECUTION_AUTHORIZED`.
