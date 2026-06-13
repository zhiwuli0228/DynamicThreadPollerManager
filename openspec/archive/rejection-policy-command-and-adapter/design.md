## Context

v0.10.0 implements runtime rejection-policy replacement — the last of three dynamic configuration dimensions for `DynamicThreadPollerManager` (thread count: v0.7.0, queue capacity: v0.9.0, rejection policy: v0.10.0). Unlike v0.9.0's queue resize which required executor rebuild, `ThreadPoolExecutor.setRejectedExecutionHandler()` is a public, thread-safe JDK API (volatile field). The design is correspondingly simpler. Two existing code defects are also addressed: `ManagedExecutor` has no policy setter, and `ExecutorRebuildStrategy` loses the original policy during rebuild.

Input: IR closure verified (`13-ir-closure-verification.md`), SR closure verified (`23-sr-closure-verification.md`).

## Goals / Non-Goals

- Goals: RejectionPolicyCommand, ManagedExecutor.setRejectionPolicy/getRejectionPolicy (direct TPE delegation), RejectionPolicySafetyGate (independent class with Predicate injection), RejectionPolicyAdjustmentAdapter (no rebuild), PolicyReplacementResult (dedicated result type), PolicyReplacementEvidence, ExecutorRebuildStrategy policy preservation fix (1 line), QueueResizeAdjustmentAdapter.isResizeInProgress() exposure
- Non-Goals: custom RejectedExecutionHandler implementations, closed-loop auto-trigger, multi-executor coordination, CLI entry, any modification to ManagedExecutorAdjustmentAdapter or QueueResizeAdjustmentAdapter.apply()

## Decisions

1. **Direct TPE delegation** (not volatile field cache) — `getRejectionPolicy()` reads directly from `this.executor.getRejectedExecutionHandler()`; `rejectionPolicy` field deleted. Single source of truth, zero inconsistency risk. (Resolved IR F07: Scheme B)
2. **Class comparison for no-op detection** — `fromCurrent()` uses `target.getClass() == current.getClass()`. JDK four built-in policies are all stateless singletons; class comparison equals semantic comparison. Custom handlers out of scope. (Resolved IR F05)
3. **Independent safety gate** (not ControlGate) — follows v0.9.0 retrospective improvement #3. Rejection policy safety gate does not need ExecutorStateSnapshot or ReadinessSummary. Same pattern as QueueResizeSafetyGate.
4. **Predicate\<String\> injection** for resize-in-progress check — `RejectionPolicySafetyGate` receives `Predicate<String> isResizeInProgress` via constructor. Decouples safety gate from `QueueResizeAdjustmentAdapter` type. (Resolved IR F02)
5. **Dedicated result type** — `PolicyReplacementResult` follows v0.9.0 retrospective improvement #2. Each adapter returns its own result type: AdjustmentResult (Scale), QueueResizeResult (Queue), PolicyReplacementResult (Policy).
6. **No idempotency guard in policy adapter** — unlike QueueResizeAdjustmentAdapter. `TPE.setRejectedExecutionHandler()` is atomic volatile write; concurrent policy replacements are last-write-wins with no intermediate state. (Resolved IR F03)
7. **Rebuild policy preservation** — 1-line fix: `new ThreadPoolExecutor.AbortPolicy()` → `oldTpe.getRejectedExecutionHandler()`. Minimal change, zero architectural impact.

## Risks / Trade-offs

- Policy-policy concurrent replacements: last-write-wins (acceptable; TPE volatile write is atomic)
- Policy-resize concurrent: protected by Predicate injection checking resize-in-progress
- `getRejectionPolicy()` now incurs volatile read on every call — negligible (not a hot path)
- Constructor field deletion (`this.rejectionPolicy = rejectionHandler` removal) — compile-time safety, no runtime risk
- Existing tests using default AbortPolicy are unaffected by rebuild fix

## Dependencies

- v0.7.0: ManagedExecutor, ExecutorRegistry, ExecutorStateSnapshot
- v0.9.0: QueueResizeAdjustmentAdapter (isResizeInProgress exposure), ExecutorRebuildStrategy (fix)
- JDK: ThreadPoolExecutor.setRejectedExecutionHandler(), RejectedExecutionHandler (AbortPolicy, CallerRunsPolicy, DiscardPolicy, DiscardOldestPolicy)
- No new external dependencies

## Migration Plan

No migration needed. `ManagedExecutor.getRejectionPolicy()` return type unchanged. `ExecutorRebuildStrategy.rebuild()` behavior change is backward-compatible — only affects executors created with non-default AbortPolicy, and the new behavior (preserving original policy) is strictly more correct than the old behavior (overwriting with AbortPolicy).
