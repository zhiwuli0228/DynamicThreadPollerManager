## Why

v0.7.0 delivered `ManagedExecutor` (ThreadPoolExecutor wrapper), `ExecutorRegistry`, and `ManagedExecutorAdjustmentAdapter`, and proved a single closed-loop experiment works. But the experiment framework still acquires pressure data through `BaselineWorkloadExecutor` — a simulation that never creates real threads. `ExecutorStateSnapshot` has no path into the standard metrics pipeline (`SnapshotAssembler` → `PressureSampler` → `EvidenceRecorder`). The project needs a standardized, repeatable way to run multi-scenario (STEADY/RAMP/BURST) data acquisition on real `ManagedExecutor` instances and feed the results through the standard metrics pipeline.

## What Changes

**ManagedExecutorConfig**
- From: No standardized way to describe ManagedExecutor creation parameters.
- To: `ManagedExecutorConfig` record encapsulating corePoolSize, maxPoolSize, queueCapacity, keepAliveTime, keepAliveTimeUnit with invariants validation, `toManagedExecutor()`, and `toPresetSummary()`.
- Reason: Reusable, validated configuration object shared by runner and bridge.
- Impact: New record in `experiment.executor`, no existing code modified.

**ManagedExecutorScenarioRunner**
- From: No runner that acquires pressure data on real ThreadPoolExecutor.
- To: `ManagedExecutorScenarioRunner` running STEADY/RAMP/BURST profiles on real TPE with startedLatch sync barriers, direct `executor.toSnapshot()` sampling, and multi-phase cleanup.
- Reason: Core data acquisition engine for v0.8.0 and future versions.
- Impact: New class in `experiment.scenario`, no existing code modified.

**SnapshotAssembler.fromExecutorState()**
- From: `SnapshotAssembler` only accepts `RuntimeObservation`. `ExecutorStateSnapshot` cannot enter the metrics pipeline.
- To: New `fromExecutorState()` default method on `SnapshotAssembler` interface, bridging `ExecutorStateSnapshot` → `ObservedSnapshot`.
- Reason: Eliminates manual `PressureSnapshot` construction, unifies data flow through standard pipeline.
- Impact: Interface extension in `experiment.metrics` (default method, binary-compatible).

**ManualPressureSampler.sampleFromExecutorState()**
- From: `ManualPressureSampler` only accepts `RuntimeObservation`.
- To: New convenience overload `sampleFromExecutorState(String, ExecutorStateSnapshot)`.
- Reason: One-line sampling path for runner callers.
- Impact: New method on existing class, no existing signature modified.

## Capabilities

### New Capabilities
- `real-executor-data-acquisition`: `ManagedExecutorConfig`, `ManagedExecutorScenarioRunner` (7-phase, startedLatch sync, direct toSnapshot, 3 profiles), `SnapshotAssembler.fromExecutorState()`, `ManualPressureSampler.sampleFromExecutorState()`, integration tests.

### Modified Capabilities
- None. All new classes or interface default methods.

## Impact

New classes: `ManagedExecutorConfig` (`experiment.executor`), `ManagedExecutorScenarioRunner` (`experiment.scenario`). Modified interfaces: `SnapshotAssembler` (new default method, binary compatible), `ManualPressureSampler` (new overload). No existing tests modified, no dependencies added, no queue resizing or closed-loop scheduling.
