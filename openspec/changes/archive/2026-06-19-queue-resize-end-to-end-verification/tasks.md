## 1. QueueResizeEndToEndTest — setup and helpers

- [ ] 1.1 Create `QueueResizeEndToEndTest` class in `src/test/java/.../experiment/executor/`
- [ ] 1.2 Add fields: ExecutorRegistry, QueueResizeSafetyGate, ExecutorRebuildStrategy, QueueResizeAdjustmentAdapter, ManagedExecutorScenarioRunner
- [ ] 1.3 Add `@BeforeEach` to initialize registry, safety gate, rebuild strategy, adapter, scenario runner
- [ ] 1.4 Add `@AfterEach` to release latches → shutdown executors → awaitTermination(10s) → shutdownNow → registry.clear()

## 2. EXPAND end-to-end tests

- [ ] 2.1 Implement `expandResizeAndRerun()`: create executor (core=2, max=4, queue=10) → run STEADY → verify passes → adapter.apply(EXPAND to 20) → verify success → verify queueCapacity=20 → verify core=2, max=4 unchanged → re-run STEADY → verify passes
- [ ] 2.2 Implement `threadConfigPreservedAfterExpand()`: create executor with specific config → EXPAND → verify corePoolSize, maxPoolSize, keepAliveTime unchanged

## 3. SHRINK end-to-end tests

- [ ] 3.1 Implement `shrinkResize()`: create executor (queue=20) → adapter.apply(SHRINK to 5) → verify success → verify queueCapacity=5
- [ ] 3.2 Implement `oldExecutorTerminatedAfterRebuild()`: create executor → apply resize → verify old executor.isTerminated() → verify new executor in registry with same executorId

## 4. Safety gate DENY end-to-end test

- [ ] 4.1 Implement `safetyGateDenyShrinkWithQueueDepth()`: create executor (queue=10) → submit 8 blocking tasks (use CountDownLatch to block) → adapter.apply(SHRINK to 5) → verify SAFETY_GATE_DENIED → verify original executor still RUNNING with queueCapacity=10
- [ ] 4.2 Ensure all latches are released in @AfterEach to prevent test hangs

## 5. ResizeEvidence completeness tests

- [ ] 5.1 Implement `resizeEvidenceComplete()`: EXPAND 10→20 → verify evidence.success=true, beforeState.queueCapacity=10, afterState.queueCapacity=20, direction="EXPAND", rebuildDurationMs > 0, errorMessage=null
- [ ] 5.2 Implement `evidenceOnFailure()`: if possible trigger a failure path (or verify evidence fields on DENY result)

## 6. No-op detection test

- [ ] 6.1 Implement `noOpWhenCapacityUnchanged()`: QueueResizeCommand.fromCurrent(10, 10, "no change") → verify Optional.empty()

## 7. Regression verification

- [ ] 7.1 Run `mvn test` — all existing 433 tests pass
- [ ] 7.2 Run new end-to-end tests — all pass
- [ ] 7.3 Verify ManagedExecutorAdjustmentAdapterTest unchanged and passes
- [ ] 7.4 Verify ManagedExecutorScenarioRunner tests unchanged and pass
