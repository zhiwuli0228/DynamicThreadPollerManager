## Summary

Implement runtime rejection-policy replacement for v0.10.0: `RejectionPolicyCommand`, `ManagedExecutor.setRejectionPolicy()`, `RejectionPolicySafetyGate`, `RejectionPolicyAdjustmentAdapter`, `PolicyReplacementResult`, `PolicyReplacementEvidence`, plus the `ExecutorRebuildStrategy` policy preservation fix and `QueueResizeAdjustmentAdapter.isResizeInProgress()` exposure. This is change 1/2 of v0.10.0 — change 2/2 (`rejection-policy-end-to-end-verification`) provides integration testing with overload behavior verification across all four JDK built-in policies.

## Motivation

v0.9.0 completed queue capacity dynamic resizing — the second of three dynamic configuration dimensions for `DynamicThreadPollerManager`. The last remaining dimension is rejection-policy runtime replacement. Unlike queue resize, `ThreadPoolExecutor.setRejectedExecutionHandler()` is a public JDK API — no executor rebuild is needed. However, `ManagedExecutor` stores the policy as `private final` with no setter, and `ExecutorRebuildStrategy` hardcodes `AbortPolicy()` during rebuild, silently discarding the original policy. v0.10.0 closes both gaps and delivers the final dynamic configuration capability.

## Changes

### New Types
- **RejectionPolicyCommand** (record): targetPolicy (RejectedExecutionHandler), reason (String); null validation; fromCurrent() factory using class comparison for no-op detection
- **RejectionPolicySafetyGate**: independent class (not implementing ControlGate); checks executor RUNNING state, policy validity, no-op detection, concurrent resize protection via injected Predicate\<String\>
- **RejectionPolicyAdjustmentAdapter**: receives RejectionPolicyCommand → safety gate → executor.setRejectionPolicy(); returns PolicyReplacementResult; no rebuild dependency
- **PolicyReplacementResult**: dedicated result type with factories: success(evidence), denied(failureCode, reason, evidence), failed(failureCode, reason), failed(failureCode, reason, evidence)
- **PolicyReplacementEvidence** (record): beforePolicyClass, afterPolicyClass, executorState, replacedAt, success, reason

### Modified Types
- **ManagedExecutor**: add `setRejectionPolicy(RejectedExecutionHandler)` (delegates to TPE); `getRejectionPolicy()` changed to delegate directly to TPE (deletes `rejectionPolicy` field — single source of truth)
- **ExecutorRebuildStrategy**: fix 1-line bug — replace hardcoded `new ThreadPoolExecutor.AbortPolicy()` with `oldTpe.getRejectedExecutionHandler()` to preserve original policy during queue resize rebuild
- **QueueResizeAdjustmentAdapter**: add `public boolean isResizeInProgress(String executorId)` method (exposes existing ConcurrentHashMap state for safety gate)

### Test Additions
- `RejectionPolicyCommandTest`: validation, fromCurrent, null rejection
- `RejectionPolicySafetyGateTest`: PERMIT/DENY conditions, no-op detection, concurrent resize check
- `RejectionPolicyAdjustmentAdapterTest`: full apply() flow, safety gate denied, executor not found
- `PolicyReplacementResultTest`: factory method semantics, evidence access
- `PolicyReplacementEvidenceTest`: field correctness
- `ExecutorRebuildStrategyTest` (extended): non-default policy preservation after rebuild
- `ManagedExecutorTest` (extended): setRejectionPolicy/getRejectionPolicy consistency

## Impact

- **Existing tests**: 476 tests continue to pass (zero regression)
- **New tests**: ~15-20 unit + integration tests
- **Packages touched**: experiment.executor (new types + ManagedExecutor/ExecutorRebuildStrategy/QueueResizeAdjustmentAdapter modifications), experiment.policy (new safety gate)
- **No package modifications**: experiment.metrics, experiment.scenario, experiment.acquisition untouched
- **Breaking changes**: none — `ManagedExecutor.getRejectionPolicy()` return type unchanged; `ExecutorRebuildStrategy.rebuild()` behavior change is backward-compatible (only affects executors with non-default AbortPolicy)
