## 1. Test Infrastructure

- [ ] 1.1 Create `RejectionPolicyEndToEndTest` class with fields: ExecutorRegistry, QueueResizeAdjustmentAdapter, RejectionPolicyAdjustmentAdapter, RejectionPolicySafetyGate, ExecutorRebuildStrategy
- [ ] 1.2 Implement `@BeforeEach`: create fresh ExecutorRegistry, RejectionPolicySafetyGate (with resizeInProgress predicate from QueueResizeAdjustmentAdapter), QueueResizeAdjustmentAdapter, RejectionPolicyAdjustmentAdapter
- [ ] 1.3 Implement `@AfterEach`: countDown all latches → shutdown all executors → awaitTermination → registry.remove
- [ ] 1.4 Create helper method for blocking task creation (Runnable with CountDownLatch)

## 2. Policy Switch + Overload Scenarios

- [ ] 2.1 `switchAbortToCallerRunsAndVerifyOverloadBehavior`: create executor (AbortPolicy, pool=1-1, queue=2), submit 2 blocking tasks (fill threads+queue), verify RejectedExecutionException on 3rd submit, release latches, switch to CallerRunsPolicy, re-fill queue+threads, verify no exception on overload submit
- [ ] 2.2 `switchAbortToDiscardAndVerifySilentDiscard`: create executor (AbortPolicy, pool=1-1, queue=2), verify overload throws, switch to DiscardPolicy, fill queue+threads, record taskCount before overload, submit overload task (no exception), wait briefly, verify taskCount unchanged
- [ ] 2.3 `switchToDiscardOldestAndVerifyEviction`: create executor (DiscardOldestPolicy, pool=1-1, queue=2), submit blocking task (occupies thread), submit NamedTask("A") and NamedTask("B") to queue, submit NamedTask("C") (overload — DiscardOldestPolicy evicts A), release blocking task, verify C executed and A not executed

## 3. Safety Gate DENY Scenarios

- [ ] 3.1 `safetyGateDenyOnShutdownExecutor`: create executor, shutdown + awaitTermination, attempt policy switch → verify result failureCode=SAFETY_GATE_DENIED
- [ ] 3.2 `safetyGateDenyOnSamePolicyType`: create executor (AbortPolicy), create RejectionPolicyCommand with new AbortPolicy(), adapter.apply() → verify result failureCode=SAFETY_GATE_DENIED (no-op detection)

## 4. Rebuild Policy Preservation

- [ ] 4.1 `rebuildPreservesRejectionPolicy`: create executor (CallerRunsPolicy, queue=5), verify getRejectionPolicy() instanceof CallerRunsPolicy, execute QueueResizeCommand EXPAND (5→10), get new executor from registry, verify getRejectionPolicy() instanceof CallerRunsPolicy

## 5. Evidence Completeness

- [ ] 5.1 `policyReplacementEvidenceComplete`: create executor (AbortPolicy), switch to CallerRunsPolicy, verify evidence.beforePolicyClass ends with "AbortPolicy", evidence.afterPolicyClass ends with "CallerRunsPolicy", evidence.success=true, evidence.replacedAt non-null, evidence.executorState non-null

## 6. Error Path

- [ ] 6.1 `executorNotFoundReturnsFailure`: attempt policy switch with non-existent executorId → verify result.failureCode=EXECUTOR_NOT_FOUND, result.success=false

## 7. Test Suite Verification

- [ ] 7.1 Run `mvn test` — all existing 476 tests + change 1 tests + 8 end-to-end tests pass (0 failures)
- [ ] 7.2 Verify no thread leaks (all CountDownLatch released in @AfterEach)
