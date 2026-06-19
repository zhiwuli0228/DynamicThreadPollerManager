# bridge-adjustment-to-real-executor Design

## Scope

This change creates `ManagedExecutorAdjustmentAdapter` — the only new class — and adds one enum constant to `AdjustmentFailureCode`. It bridges the existing adjustment pipeline to a real `ManagedExecutor` without modifying any public interfaces in `experiment.adjustment`.

## Package layout

```
experiment.executor (existing)
    └── ManagedExecutorAdjustmentAdapter  ← NEW

experiment.adjustment (existing)
    └── AdjustmentFailureCode             ← MODIFIED (+EXECUTOR_NOT_FOUND)
```

## Design decisions

1. **Single pool size mapping**: `ScaleAdjustmentCommand.targetPoolSize()` maps to `ManagedExecutor.setCorePoolSize()`. If the target exceeds the current max pool size, the adapter first calls `setMaximumPoolSize(target)` to preserve the `max >= core` invariant, then calls `setCorePoolSize(target)`. This is consistent with how `InMemoryAdjustableExecutorProbe` interprets `targetPoolSize`.

2. **Safety gate integration**: Uses the existing `RuntimeAdjustmentSafetyGate.evaluate(command, currentState, readiness)` three-argument signature. The `readiness` parameter is obtained from the caller (injected at construction time or passed per-call). The adapter does not generate readiness assessments.

3. **Executor lookup**: The adapter is constructed with an `ExecutorRegistry`, a `RuntimeAdjustmentSafetyGate`, and an `executorName`. The `executorName` is used to look up the target `ManagedExecutor` from the registry on each call.

4. **No parameter bounds enforcement in adapter**: Bounds enforcement is the safety gate's responsibility. The adapter applies what the gate allows. If `ThreadPoolExecutor.setCorePoolSize()` or `setMaximumPoolSize()` throws `IllegalArgumentException` (e.g., invalid value), the adapter catches it and returns an `AdjustmentResult(FAILED, ..., INVALID_COMMAND)`.

5. **`EXECUTOR_NOT_FOUND` failure code**: Added to `AdjustmentFailureCode` enum. Used when the target executor name is not found in the registry.

6. **Snapshot collection**: Uses `ManagedExecutor.toSnapshot()` for both beforeState and afterState. The method was implemented in change 1/3 and collects all fields from the underlying `ThreadPoolExecutor`.

7. **`recordApplied` contract**: The adapter calls `safetyGate.recordApplied(decision)` only after successfully applying the command and only for `ALLOW` decisions. This matches the safety gate contract.

## Adjustment flow

```
ScaleAdjustmentCommand
    │
    ├── 1. registry.get(executorName) → Optional<ManagedExecutor>
    │       └── empty → AdjustmentResult(FAILED, EXECUTOR_NOT_FOUND)
    ├── 2. beforeState = executor.toSnapshot()
    ├── 3. decision = safetyGate.evaluate(command, beforeState, readiness)
    │       ├── REJECTED → AdjustmentResult(REJECTED, beforeState, reason)
    │       └── NO_OP → AdjustmentResult(NO_OP, beforeState, reason)
    ├── 4. If target > executor.getMaximumPoolSize():
    │       executor.setMaximumPoolSize(target)
    ├── 5. executor.setCorePoolSize(target)
    │       └── RuntimeException → AdjustmentResult(FAILED, beforeState, failureCode)
    ├── 6. safetyGate.recordApplied(decision)
    ├── 7. afterState = executor.toSnapshot()
    └── 8. AdjustmentResult(APPLIED, beforeState, afterState)
```

## Dependency direction

```
experiment.executor
    ├── java.util.concurrent
    ├── experiment.adjustment (ExecutorAdjustmentAdapter, ExecutorStateSnapshot,
    │       ScaleAdjustmentCommand, RuntimeAdjustmentSafetyGate, SafetyGateDecision,
    │       AdjustmentResult, AdjustmentStatus, AdjustmentFailureCode)
    └── (no dependency on scenario, policy, analysis, or metrics)
```

## Test strategy

- **Unit tests**: Adapter with a real `ManagedExecutor` and a real `ThreadPoolExecutor` (no mocking of the executor)
- **Safety gate**: Use the real `RuntimeAdjustmentSafetyGate` implementation (no mocking)
- **Readiness**: Use a test-provided `ReadinessAssessment` (can be hardcoded)
- **Executor registry**: Real `ExecutorRegistry` with a real `ManagedExecutor`
- **Coverage targets**: APPLIED path, REJECTED path (safety gate blocks), FAILED path (executor not found, runtime exception), NO_OP path
