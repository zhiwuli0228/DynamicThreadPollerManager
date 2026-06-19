## Summary

Implement the core queue capacity resizing capability for v0.9.0: `QueueResizeCommand`, `ExecutorRebuildStrategy`, `QueueResizeSafetyGate`, `QueueResizeAdjustmentAdapter`, and `ResizeEvidence`. This is change 1/2 of v0.9.0 — change 2/2 (`queue-resize-end-to-end-verification`) provides integration testing and post-resize data acquisition verification.

## Motivation

Since v0.1.0, the project named `DynamicThreadPollerManager` has been unable to dynamically resize work queue capacity. `ThreadPoolExecutor` allows core/max pool size to change at runtime, but the work queue is immutable after construction. Every version from v0.5.0 through v0.8.0 explicitly deferred queue resizing. v0.9.0 finally addresses this by introducing executor rebuild as the safe mechanism for capacity changes.

## Changes

### New Types
- **QueueResizeCommand** (record): targetQueueCapacity, resizeReason, timeoutMs; validation against <=0; fromCurrent() factory returning Optional.empty() for no-op
- **ExecutorRebuildStrategy**: decommission (shutdown→drainTo→awaitTermination) → commission (new TPE→wrap ManagedExecutor→re-register same executorId)
- **RebuildResult** (record): success, beforeState, afterState, duration, drainedTaskCount, rejectedTaskCount, direction, old/new capacity, errorMessage
- **QueueResizeSafetyGate**: implements ControlGate<QueueResizeCommand>; checks executor RUNNING state, capacity change validity, SHRINK safety (queue depth ≤ new capacity)
- **QueueResizeAdjustmentAdapter** (new, separate class): receives QueueResizeCommand → safety gate → rebuild → AdjustmentResult<ResizeEvidence>; includes ConcurrentHashMap idempotency guard
- **ResizeEvidence** (record): beforeState, afterState, rebuildDurationMs, drainedTaskCount, rejectedTaskCount, direction, oldQueueCapacity, newQueueCapacity, success, errorMessage

### Modified Types
- None. Existing ManagedExecutorAdjustmentAdapter, ScaleAdjustmentCommand, ManagedExecutor, ExecutorRegistry are all unchanged.

### Test Additions
- `QueueResizeCommandTest`: validation, fromCurrent, direction
- `QueueResizeSafetyGateTest`: PERMIT/DENY conditions, SHRINK safety
- `ExecutorRebuildStrategyTest`: EXPAND/SHRINK rebuild cycles, task drain
- `QueueResizeAdjustmentAdapterTest`: full apply() flow, safety gate denied, RESIZE_IN_PROGRESS
- `ResizeEvidenceTest`: field mapping, null safety

## Impact

- **Existing tests**: 433 tests continue to pass (zero regression)
- **New tests**: ~15-20 unit + integration tests
- **Packages touched**: experiment.executor (new types), experiment.policy (new safety gate)
- **No package modifications**: experiment.metrics, experiment.scenario, experiment.acquisition untouched
