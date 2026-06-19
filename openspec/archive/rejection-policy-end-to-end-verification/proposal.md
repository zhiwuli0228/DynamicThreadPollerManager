## Summary

Provide end-to-end integration testing for v0.10.0 rejection-policy replacement: verify that all four JDK built-in policies can be switched at runtime, that overload behavior matches the target policy, that safety gate correctly denies invalid replacements, and that `ExecutorRebuildStrategy` preserves non-default policies during queue resize rebuild. This is change 2/2 of v0.10.0 — depends on change 1/2 (`rejection-policy-command-and-adapter`) for all core components.

## Motivation

Change 1/2 delivers all core components for rejection-policy replacement, but the behavioral correctness of policy switching can only be validated end-to-end: a policy switch must be followed by an overload scenario that proves the new policy's behavior is active. Each of the four JDK policies has distinct observable behavior under overload (AbortPolicy throws, CallerRunsPolicy executes in caller thread, DiscardPolicy silently drops, DiscardOldestPolicy evicts oldest). Additionally, the `ExecutorRebuildStrategy` fix must be verified: a rebuild must preserve non-default policies.

## Changes

### Test Additions
- **RejectionPolicyEndToEndTest**: 8 end-to-end scenarios

#### Test Scenarios
1. **AbortPolicy → CallerRunsPolicy switch + overload verification**: fill queue+threads, verify RejectedExecutionException, switch policy, verify no exception (caller runs instead)
2. **AbortPolicy → DiscardPolicy switch + silent discard verification**: fill queue+threads, switch to DiscardPolicy, submit overload task, verify no exception and taskCount unchanged
3. **AbortPolicy → DiscardOldestPolicy switch**: fill queue with identifiable tasks, submit overload, verify oldest task evicted
4. **Safety gate DENY on shutdown executor**: shutdown executor, attempt policy switch, verify DENIED
5. **Safety gate DENY on same policy type (no-op)**: AbortPolicy → AbortPolicy, verify DENIED
6. **Rebuild preserves non-default policy**: create executor with CallerRunsPolicy, queue resize EXPAND, verify new executor retains CallerRunsPolicy
7. **Evidence completeness**: verify beforePolicyClass/afterPolicyClass/executorState/replacedAt/success/reason all populated correctly
8. **Executor not found**: attempt policy switch on non-existent executorId, verify EXECUTOR_NOT_FOUND

### No Production Code Changes
This change contains only test code. All production components are delivered by change 1/2.

## Impact

- **Existing tests**: 476 + change 1 tests continue to pass
- **New tests**: 8 end-to-end test methods
- **Packages touched**: experiment.executor (test only)
- **No production code modifications**
