## Implementation Plan

### Order of Work

1. **QueueResizeEndToEndTest class skeleton** — @BeforeEach/@AfterEach lifecycle, field declarations
2. **EXPAND tests** (2 tests) — depends on full resize pipeline from change 1
3. **SHRINK tests** (2 tests) — depends on full resize pipeline from change 1
4. **Safety gate DENY test** (1 test) — depends on safety gate + adapter from change 1
5. **ResizeEvidence completeness tests** (2 tests) — depends on ResizeEvidence from change 1
6. **No-op detection test** (1 test) — depends on QueueResizeCommand from change 1
7. **Regression verification** — `mvn test` full suite

### Files to Create

```
src/test/java/.../experiment/executor/
  QueueResizeEndToEndTest.java
```

### Files to NOT Modify

- `ManagedExecutorAdjustmentAdapterTest.java`
- `ManagedExecutorScenarioRunner` (production and test)
- All production code from change 1/2

### Prerequisites

- Change 1/2 (`queue-resize-command-and-rebuild`) must be fully implemented
- All 5 components (QueueResizeCommand, ExecutorRebuildStrategy, QueueResizeSafetyGate, QueueResizeAdjustmentAdapter, ResizeEvidence) must exist and pass their own tests

### Verification

- `mvn test` with 433 + change-1 tests + 6-8 new e2e tests, all passing
- No regression in existing adapter/runner/registry tests
- STEADY scenario passes both before and after resize
