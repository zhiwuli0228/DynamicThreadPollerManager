## Context

v0.9.0 implements runtime queue capacity resizing — the most persistently deferred capability since v0.1.0. All surrounding infrastructure (ManagedExecutor, ExecutorRegistry, ControlGate, ExecutorStateSnapshot) is already in place from v0.7.0-v0.8.0. The core technical challenge: `ThreadPoolExecutor` does not support runtime work-queue replacement.

## Goals / Non-Goals

- Goals: QueueResizeCommand, ExecutorRebuildStrategy (with safety ordering: shutdown→drain→awaitTermination→new TPE→re-register), QueueResizeSafetyGate, QueueResizeAdjustmentAdapter (new, separate), ResizeEvidence
- Non-Goals: reflection hack, rejection policy switching, closed-loop auto-trigger, CLI entry, multi-executor coordination, any modification to ManagedExecutorAdjustmentAdapter

## Decisions

1. **Executor Rebuild** (not reflection, not TPE subclass) — uses existing ExecutorRegistry register/remove cycle
2. **New QueueResizeAdjustmentAdapter** (not modifying ManagedExecutorAdjustmentAdapter) — follows v0.8.0 D1 principle
3. **Synchronous blocking** with configurable timeout — consistent with ScaleAdjustmentCommand
4. **Decommission order**: shutdown → drainTo → awaitTermination (eliminates race condition from IR F05)
5. **SHRINK = drain-and-discard; EXPAND = drain-and-replay** (resolved IR F08)
6. **ResizeEvidence via AdjustmentResult<T>** — generic type compatible, no EvidenceRecorder modification needed
7. **Same executorId across rebuild** — rebuild = same logical executor config change

## Risks / Trade-offs

- Brief executor unavailability during rebuild (shutdown→new TPE window)
- Drain-and-discard on SHRINK means in-flight tasks are lost (documented behavior)
- Commission failure leaves old executor terminated (cannot recover; accepted as best-effort)
- Idempotency guard via ConcurrentHashMap in adapter (handles concurrent resize requests)

## Dependencies

- v0.7.0: ManagedExecutor, ExecutorRegistry, ExecutorStateSnapshot
- v0.4.0: ControlGate interface, SafetyGateResult
- v0.5.0: AdjustmentResult<T>
- No new external dependencies

## Migration Plan

No migration needed — new capability, no existing API changes. Existing ManagedExecutorAdjustmentAdapter and ScaleAdjustmentCommand are untouched.
