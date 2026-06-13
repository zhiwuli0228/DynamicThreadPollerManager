## Implementation Plan

### Order of Work

1. **RejectionPolicyCommand** — pure data record, no dependencies
2. **PolicyReplacementEvidence** — pure data record
3. **PolicyReplacementResult** — depends on PolicyReplacementEvidence
4. **RejectionPolicySafetyGate** — depends on ManagedExecutor (read-only), Predicate\<String\>
5. **ManagedExecutor modification** — add setRejectionPolicy(), modify getRejectionPolicy(), remove field
6. **ExecutorRebuildStrategy fix** — 1-line change in rebuild()
7. **QueueResizeAdjustmentAdapter modification** — add isResizeInProgress()
8. **RejectionPolicyAdjustmentAdapter** — depends on all above
9. **Tests** — written alongside each component

### Files to Create

```
src/main/java/.../experiment/executor/
  RejectionPolicyCommand.java
  PolicyReplacementEvidence.java
  PolicyReplacementResult.java
  RejectionPolicyAdjustmentAdapter.java

src/main/java/.../experiment/policy/
  RejectionPolicySafetyGate.java

src/test/java/.../experiment/executor/
  RejectionPolicyCommandTest.java
  PolicyReplacementEvidenceTest.java
  PolicyReplacementResultTest.java
  RejectionPolicyAdjustmentAdapterTest.java

src/test/java/.../experiment/policy/
  RejectionPolicySafetyGateTest.java
```

### Files to Modify

```
src/main/java/.../experiment/executor/
  ManagedExecutor.java              — add setRejectionPolicy(), delegate getRejectionPolicy(), remove field
  ExecutorRebuildStrategy.java       — fix line 75: oldTpe.getRejectedExecutionHandler()
  QueueResizeAdjustmentAdapter.java  — add isResizeInProgress() public method
```

### Files to NOT Modify

- `ManagedExecutorAdjustmentAdapter.java`
- `ScaleAdjustmentCommand.java`
- `QueueResizeCommand.java`
- `QueueResizeResult.java`
- `ExecutorRegistry.java`
- `ControlGate.java`
- `AdjustmentResult.java`

### Verification

- `mvn test` with 476 + new tests, all passing
- No regression in existing adapter/runner/registry/resize tests
- `ManagedExecutorTest` extended for setRejectionPolicy/getRejectionPolicy consistency
- `ExecutorRebuildStrategyTest` extended for non-default policy preservation
