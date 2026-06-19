## Implementation Plan

### Order of Work

1. **QueueResizeCommand** — pure data record, no dependencies
2. **QueueResizeSafetyGate** — depends on ControlGate interface, no internal deps
3. **RebuildResult** + **ResizeEvidence** — pure data records
4. **ExecutorRebuildStrategy** — depends on ManagedExecutor, ExecutorRegistry, RebuildResult
5. **QueueResizeAdjustmentAdapter** — depends on all above
6. **Tests** — written alongside each component

### Files to Create

```
src/main/java/.../experiment/executor/
  QueueResizeCommand.java
  RebuildResult.java
  ResizeEvidence.java
  ExecutorRebuildStrategy.java
  QueueResizeAdjustmentAdapter.java

src/main/java/.../experiment/policy/
  QueueResizeSafetyGate.java

src/test/java/.../experiment/executor/
  QueueResizeCommandTest.java
  ExecutorRebuildStrategyTest.java
  QueueResizeAdjustmentAdapterTest.java
  ResizeEvidenceTest.java

src/test/java/.../experiment/policy/
  QueueResizeSafetyGateTest.java
```

### Files to NOT Modify

- `ManagedExecutorAdjustmentAdapter.java`
- `ScaleAdjustmentCommand.java`
- `ManagedExecutor.java`
- `ExecutorRegistry.java`
- `ControlGate.java`
- `AdjustmentResult.java`

### Verification

- `mvn test` with 433 + new tests, all passing
- No regression in existing adapter/runner/registry tests
