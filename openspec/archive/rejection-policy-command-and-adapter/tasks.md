## 1. RejectionPolicyCommand

- [ ] 1.1 Create `RejectionPolicyCommand` record with fields: targetPolicy (RejectedExecutionHandler), reason (String)
- [ ] 1.2 Validate targetPolicy != null in compact constructor (Objects.requireNonNull)
- [ ] 1.3 Validate reason not blank in compact constructor
- [ ] 1.4 Provide `fromCurrent(RejectedExecutionHandler current, RejectedExecutionHandler target, String reason)` factory returning `Optional<RejectionPolicyCommand>` using class comparison
- [ ] 1.5 Write `RejectionPolicyCommandTest` covering: valid creation, null policy rejection, blank reason rejection, fromCurrent same-class returns empty, fromCurrent different-class returns command

## 2. PolicyReplacementEvidence

- [ ] 2.1 Create `PolicyReplacementEvidence` record with fields: beforePolicyClass (String), afterPolicyClass (String), executorState (ExecutorStateSnapshot), replacedAt (Instant), success (boolean), reason (String)
- [ ] 2.2 Write `PolicyReplacementEvidenceTest` covering: success evidence fields, failure evidence fields, canonical class name correctness

## 3. PolicyReplacementResult

- [ ] 3.1 Create `PolicyReplacementResult` class with fields: success (boolean), evidence (PolicyReplacementEvidence), failureCode (String), reason (String)
- [ ] 3.2 Provide static factory `success(PolicyReplacementEvidence evidence)`
- [ ] 3.3 Provide static factory `denied(String failureCode, String reason, PolicyReplacementEvidence evidence)`
- [ ] 3.4 Provide static factory `failed(String failureCode, String reason)`
- [ ] 3.5 Provide static factory `failed(String failureCode, String reason, PolicyReplacementEvidence evidence)`
- [ ] 3.6 Write `PolicyReplacementResultTest` covering: success result, denied result, failed result without evidence, failed result with evidence, null validation

## 4. RejectionPolicySafetyGate

- [ ] 4.1 Create `RejectionPolicySafetyGate` as independent class (not implementing ControlGate) with constructor(Predicate\<String\> isResizeInProgress)
- [ ] 4.2 Define `GateResult` enum (PERMIT, DENY) and `EvaluationResult` record (GateResult result, String reason) with `permitted()` method
- [ ] 4.3 Implement `evaluate(RejectionPolicyCommand command, ManagedExecutor executor, String executorId)`: check executor RUNNING, policy non-null, no-op (class comparison), concurrent resize (Predicate test)
- [ ] 4.4 Write `RejectionPolicySafetyGateTest` covering: PERMIT valid replacement, DENY shutdown executor, DENY terminated executor, DENY same-class policy (no-op), DENY null policy, DENY resize in progress

## 5. ManagedExecutor Modification

- [ ] 5.1 Add `public void setRejectionPolicy(RejectedExecutionHandler newPolicy)` — validates non-null, delegates to `this.executor.setRejectedExecutionHandler(newPolicy)`
- [ ] 5.2 Change `getRejectionPolicy()` to `return this.executor.getRejectedExecutionHandler()` (direct delegation to TPE)
- [ ] 5.3 Delete `private final RejectedExecutionHandler rejectionPolicy` field
- [ ] 5.4 Delete `this.rejectionPolicy = rejectionHandler` line from 7-parameter constructor
- [ ] 5.5 Verify existing `getRejectionPolicy()` callers — none exist beyond toSnapshot() which doesn't use it
- [ ] 5.6 Write/Extend `ManagedExecutorTest` covering: set then get returns new policy, set null throws, get after construction returns AbortPolicy (default)

## 6. ExecutorRebuildStrategy Fix

- [ ] 6.1 In `rebuild()` method, change line 75 from `new ThreadPoolExecutor.AbortPolicy()` to `oldTpe.getRejectedExecutionHandler()`
- [ ] 6.2 Extend `ExecutorRebuildStrategyTest` with: `rebuildPreservesCallerRunsPolicy()` — create executor with CallerRunsPolicy, rebuild with EXPAND, verify new executor also has CallerRunsPolicy

## 7. QueueResizeAdjustmentAdapter Modification

- [ ] 7.1 Add `public boolean isResizeInProgress(String executorId)` method — returns `resizeInProgress.containsKey(executorId)`
- [ ] 7.2 Write unit test verifying: returns true during resize, returns false when no resize, returns false after resize completes

## 8. RejectionPolicyAdjustmentAdapter

- [ ] 8.1 Create `RejectionPolicyAdjustmentAdapter` with constructor(ExecutorRegistry registry, RejectionPolicySafetyGate safetyGate)
- [ ] 8.2 Implement `apply(String executorId, RejectionPolicyCommand command)`: find executor → read before policy → safety gate evaluate → setRejectionPolicy → build result
- [ ] 8.3 Handle EXECUTOR_NOT_FOUND
- [ ] 8.4 Handle SAFETY_GATE_DENIED
- [ ] 8.5 Handle POLICY_SET_FAILED (RuntimeException from setRejectionPolicy)
- [ ] 8.6 Write `RejectionPolicyAdjustmentAdapterTest` covering: successful apply, executor not found, safety gate denied, policy set failure, evidence completeness

## 9. Test Suite Verification

- [ ] 9.1 Run `mvn test` — all existing 476 tests pass (0 failures)
- [ ] 9.2 Run new tests — all pass (0 failures)
- [ ] 9.3 Verify ManagedExecutorAdjustmentAdapter tests unchanged and pass
- [ ] 9.4 Verify QueueResizeAdjustmentAdapter tests unchanged and pass (including new isResizeInProgress test)
- [ ] 9.5 Verify ScaleAdjustmentCommand tests unchanged and pass
- [ ] 9.6 Verify QueueResizeCommand tests unchanged and pass
