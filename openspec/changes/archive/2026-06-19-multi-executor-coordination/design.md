# Design: multi-executor-coordination

## Input Baseline

- SR: `docs/04-development/versions/v0.15.0/20-sr.md` (post-disposition, closure verified)
- IR: `docs/04-development/versions/v0.15.0/10-ir.md` (IR-v0.15-001 through IR-v0.15-008, IR-v0.15-011)
- Decision log: `docs/04-development/versions/v0.15.0/decision-log.md` (D1, D2, D4, D5)

## Architecture

All new components in `com.zhiwu.dynamicthreadpollermanager.experiment.coordination`.

Dependency direction: `experiment.coordination` → `experiment.loop`, `experiment.adjustment`, `experiment.executor`, `experiment.metrics`. No reverse dependencies.

## Component Design

See SR §2.1-2.10 for full pseudocode with verified API signatures. Key design points:

1. **AdjustmentPriority**: 4-level enum with numeric levels and `canPreempt(other)` method
2. **ExecutorGroupConfig**: 7-field record with static `defaults()` factory; validates all constraints in compact constructor
3. **ResourceBudget**: Thread-safe via `synchronized`; `ConcurrentHashMap` for allocations; `reserve(executorId, delta)` with negative delta = release
4. **GroupCoordinator**: Accepts `ScaleAdjustmentCommand` + executor name (not `AdjustmentDecision` — IR P0-01 resolution); `synchronized` on internal lock; fast path for no-op/scale-down; preemption scans lower-priority executors
5. **CoordinatedAdjustmentAdapter**: Decorator implements `ExecutorAdjustmentAdapter`; `apply()` calls `coordinator.coordinate(command, executorName)` before delegating; `currentState()` passes through
6. **ExecutorGroup**: Constructor validates `sum(corePoolSize) <= maxTotalThreads` (IR P1-03); uses explicit String executor names
7. **GroupCoordinationHistory**: `CopyOnWriteArrayList` (same pattern as `AdjustmentHistory` in v0.14.0)
8. **CrossExecutorOscillationDetector**: 3 pattern detectors — lockstep counter-adjustment, resource ping-pong, priority thrashing; advisory only (does not block)
9. **GroupLoopOrchestrator**: Wraps `ExecutorGroup`; creates `AdjustmentLoop` instances with `CoordinatedAdjustmentAdapter`; `GroupHealth` uses `Map<String, LoopState>` (SR P1-03)

## Test Strategy

- Unit tests for all records/enums/classes with mock `AdjustmentLoop` instances
- Mock `ExecutorAdjustmentAdapter` (InMemoryAdjustableExecutorProbe) for coordinator tests
- Real `ResourceBudget` with concurrent reserve/release tests (10 threads × 100 ops)
- Mock `CrossExecutorOscillationDetector` for orchestrator lifecycle tests
- Mock `LoopEvidenceRecorder` for evidence recording tests
- All algorithmic components (GroupCoordinator, ResourceBudget, CrossExecutorOscillationDetector) tested without real threads

## Executor Identity Model

Executors are identified by explicit String names throughout the coordination layer (SR P1-01). Names originate from `ExecutorRegistry` registration names. `CoordinatedAdjustmentAdapter` receives the executor name at construction and passes it to `GroupCoordinator.coordinate()`.
