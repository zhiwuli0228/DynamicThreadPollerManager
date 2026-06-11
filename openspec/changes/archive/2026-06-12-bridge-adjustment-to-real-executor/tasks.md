## 1. AdjustmentFailureCode extension

- [x] 1.1 Add `EXECUTOR_NOT_FOUND` to `AdjustmentFailureCode` enum in `experiment.adjustment` package.
- [x] 1.2 Verify existing enum constants unchanged and new constant accessible via `valueOf`.

## 2. ManagedExecutorAdjustmentAdapter

- [x] 2.1 Create `ManagedExecutorAdjustmentAdapter` in `experiment.executor` package, implementing `ExecutorAdjustmentAdapter`.
- [x] 2.2 Constructor: `(ExecutorRegistry registry, RuntimeAdjustmentSafetyGate safetyGate, String executorName, ReadinessAssessment readiness)`.
- [x] 2.3 Implement `currentState()`: look up executor from registry by name, call `executor.toSnapshot()`, throw `IllegalStateException` if not found.
- [x] 2.4 Implement `apply(ScaleAdjustmentCommand)`: full flow — registry lookup, beforeState, safety gate evaluation, parameter application with max-before-core ordering, afterState, result construction.
- [x] 2.5 Implement safety gate integration: call `safetyGate.evaluate(command, beforeState, readiness)`, handle ALLOW/REJECTED/NO_OP outcomes.
- [x] 2.6 Implement `recordApplied` contract: call only after successful APPLIED, never after REJECTED/NO_OP/FAILED.
- [x] 2.7 Implement parameter application logic: if target > current max, set max first; then set core; catch `RuntimeException` → FAILED with INVALID_COMMAND.

## 3. Test coverage

- [x] 3.1 Create `ManagedExecutorAdjustmentAdapterTest` in `experiment.executor` test package.
- [x] 3.2 Test: APPLIED path — adapter successfully applies adjustment to real `ManagedExecutor` + `ThreadPoolExecutor`.
- [x] 3.3 Test: APPLIED path with target <= current max — only core is set, max unchanged.
- [x] 3.4 Test: REJECTED path — safety gate blocks, no mutation, correct failure code returned.
- [x] 3.5 Test: NO_OP path — safety gate returns NO_OP, no mutation.
- [x] 3.6 Test: FAILED path — executor not found in registry, `EXECUTOR_NOT_FOUND` code.
- [x] 3.7 Test: Permissive safety gate allows edge-case values.
- [x] 3.8 Test: `currentState()` returns correct snapshot from real executor.
- [x] 3.9 Test: `currentState()` throws `IllegalStateException` when executor not registered.
- [x] 3.10 Test: `recordApplied` called/not-called contract verification.
- [x] 3.11 Test: Existing `AdjustmentFailureCode` enum constants unchanged.
- [x] 3.12 All tests clean up executors in `@AfterEach`.

## 4. Verification

- [x] 4.1 `mvn test` exits 0 — all existing tests + new tests pass.
- [x] 4.2 Existing `InMemoryAdjustableExecutorProbe` tests pass unmodified.
- [x] 4.3 `openspec validate --all --json` passes.
