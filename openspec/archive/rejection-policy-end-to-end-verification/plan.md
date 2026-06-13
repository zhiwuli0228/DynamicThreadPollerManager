## Implementation Plan

### Pre-requisite
Change 1/2 (`rejection-policy-command-and-adapter`) must be fully implemented and all its tests passing.

### Files to Create

```
src/test/java/.../experiment/executor/
  RejectionPolicyEndToEndTest.java    — 8 end-to-end scenarios
```

### Files to NOT Modify

- All production code (change 1/2 already delivered)
- All existing tests

### Test Scenarios

1. `switchAbortToCallerRunsAndVerifyOverloadBehavior` — AbortPolicy overload (expect exception) → switch → CallerRunsPolicy overload (expect no exception)
2. `switchAbortToDiscardAndVerifySilentDiscard` — DiscardPolicy overload (expect no exception, taskCount unchanged)
3. `switchToDiscardOldestAndVerifyEviction` — DiscardOldestPolicy overload (expect oldest task evicted)
4. `safetyGateDenyOnShutdownExecutor` — shutdown → policy switch → DENIED
5. `safetyGateDenyOnSamePolicyType` — AbortPolicy → AbortPolicy → DENIED (no-op)
6. `rebuildPreservesRejectionPolicy` — CallerRunsPolicy executor → EXPAND resize → new executor retains CallerRunsPolicy
7. `policyReplacementEvidenceComplete` — switch → verify all evidence fields populated
8. `executorNotFoundReturnsFailure` — non-existent executorId → EXECUTOR_NOT_FOUND

### Verification

- `mvn test` with 476 + change 1 tests + 8 end-to-end tests, all passing
- Zero regression in existing tests
