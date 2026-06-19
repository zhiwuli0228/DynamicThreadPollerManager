# bridge-adjustment-to-real-executor

## Why

v0.7.0 change 1/3 (`establish-managed-executor-and-registry`) delivered `ManagedExecutor` wrapping `ThreadPoolExecutor`, `ExecutorRegistry`, `DeletionSafety`, and `RuntimeSetting`. But the adjustment pipeline (v0.5.0-v0.6.0) still operates exclusively on `InMemoryAdjustableExecutorProbe` — a mutable state simulation with no real threads. The `ExecutorAdjustmentAdapter` contract has no implementation that bridges to a real `ThreadPoolExecutor`.

This change creates `ManagedExecutorAdjustmentAdapter`, which implements `ExecutorAdjustmentAdapter` and connects the existing safety gate + adjustment command infrastructure to a real `ManagedExecutor`. This is the second of three serial changes needed for v0.7.0's first closed-loop experiment.

## What Changes

### New

- **`ManagedExecutorAdjustmentAdapter`** — implements `ExecutorAdjustmentAdapter`, bridges `ScaleAdjustmentCommand` to `ManagedExecutor` parameter setters, integrated with `RuntimeAdjustmentSafetyGate`
- **`AdjustmentFailureCode.EXECUTOR_NOT_FOUND`** — new failure code for when the target executor is not registered

### Modified

- **`experiment.adjustment.AdjustmentFailureCode`** — add `EXECUTOR_NOT_FOUND` enum constant

### Non-changes

- `experiment.adjustment` public interfaces (`ExecutorAdjustmentAdapter`, `RuntimeAdjustmentSafetyGate`, `AdjustmentResult`, etc.) are NOT modified
- `experiment.scenario`, `experiment.policy`, `experiment.analysis`, `experiment.metrics` packages are NOT touched
- No closed-loop experiment (deferred to change 3/3)
- No queue resizing, no persistence, no REST/API

## Capabilities

### New capability: `bridge-adjustment-to-real-executor`

The adapter bridges the gap between the adjustment decision pipeline and a real `ManagedExecutor`. It:
1. Looks up the target executor from `ExecutorRegistry` by name
2. Collects `beforeState` via `ManagedExecutor.toSnapshot()`
3. Invokes `RuntimeAdjustmentSafetyGate.evaluate()` with the command and current state
4. On ALLOW: applies `setCorePoolSize`/`setMaximumPoolSize` on the real executor, collects `afterState`, calls `recordApplied()`
5. On REJECTED/NO_OP: returns the appropriate `AdjustmentResult` without mutation

## Impact

- New source file: `ManagedExecutorAdjustmentAdapter.java`
- Modified: `AdjustmentFailureCode.java` (one enum constant)
- New test file: `ManagedExecutorAdjustmentAdapterTest.java`
- Zero impact on existing tests; `InMemoryAdjustableExecutorProbe` tests continue to pass unmodified
