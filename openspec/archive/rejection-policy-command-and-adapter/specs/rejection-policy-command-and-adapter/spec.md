## Purpose

Define the behavior of `RejectionPolicyCommand`, `ManagedExecutor.setRejectionPolicy/getRejectionPolicy`, `RejectionPolicySafetyGate`, `RejectionPolicyAdjustmentAdapter`, `PolicyReplacementResult`, `PolicyReplacementEvidence`, `ExecutorRebuildStrategy` policy preservation fix, and `QueueResizeAdjustmentAdapter.isResizeInProgress()` for runtime rejection-policy replacement.

## Requirements

### Requirement: RejectionPolicyCommand invariants and creation
The system MUST provide an immutable `RejectionPolicyCommand` record that validates creation parameters and detects no-op replacement requests using class comparison.

#### Scenario: Valid command creation
- **WHEN** `RejectionPolicyCommand` is created with targetPolicy=CallerRunsPolicy and reason="test"
- **THEN** the record MUST store the values and return them via `targetPolicy()` and `reason()`

#### Scenario: Null policy is rejected
- **WHEN** `RejectionPolicyCommand` is created with targetPolicy=null
- **THEN** a `NullPointerException` MUST be thrown

#### Scenario: Blank reason is rejected
- **WHEN** `RejectionPolicyCommand` is created with reason="" or reason="   "
- **THEN** an `IllegalArgumentException` MUST be thrown

#### Scenario: Same policy class returns empty
- **WHEN** `fromCurrent(abortPolicy, anotherAbortPolicy, "no-op")` is called
- **THEN** `Optional.empty()` MUST be returned

#### Scenario: Different policy class returns command
- **WHEN** `fromCurrent(abortPolicy, callerRunsPolicy, "switch")` is called
- **THEN** `Optional` containing a `RejectionPolicyCommand` MUST be returned

---

### Requirement: ManagedExecutor supports runtime rejection policy replacement
The system MUST provide `setRejectionPolicy()` to mutate the rejection policy at runtime, and `getRejectionPolicy()` MUST return the current policy directly from the underlying TPE (single source of truth, no cached field).

#### Scenario: Set then get returns new policy
- **WHEN** `setRejectionPolicy(CallerRunsPolicy)` is called, then `getRejectionPolicy()` is called
- **THEN** `getRejectionPolicy()` MUST return a CallerRunsPolicy instance

#### Scenario: Set null throws
- **WHEN** `setRejectionPolicy(null)` is called
- **THEN** a `NullPointerException` MUST be thrown

#### Scenario: Get after construction returns AbortPolicy
- **WHEN** a new `ManagedExecutor` is created with default constructor (5-param, using AbortPolicy)
- **THEN** `getRejectionPolicy()` MUST return an AbortPolicy instance

#### Scenario: Get delegates to TPE (no cached field)
- **WHEN** `getRejectionPolicy()` is called
- **THEN** the value MUST be obtained from `ThreadPoolExecutor.getRejectedExecutionHandler()`, not from a ManagedExecutor field

---

### Requirement: RejectionPolicySafetyGate evaluates replacement safety
The system MUST provide a `RejectionPolicySafetyGate` as an independent class (not implementing ControlGate) that checks safety conditions before allowing a rejection policy replacement.

#### Scenario: Valid replacement is permitted
- **WHEN** `evaluate()` is called with a CallerRunsPolicy command, a RUNNING executor, and no concurrent resize
- **THEN** `EvaluationResult.result()` MUST be `PERMIT`

#### Scenario: Shutdown executor denied
- **WHEN** `evaluate()` is called on an executor where `isShutdown()` returns true
- **THEN** `EvaluationResult.result()` MUST be `DENY` with reason containing "RUNNING"

#### Scenario: Terminated executor denied
- **WHEN** `evaluate()` is called on an executor where `isTerminated()` returns true
- **THEN** `EvaluationResult.result()` MUST be `DENY`

#### Scenario: Same policy class denied (no-op detection)
- **WHEN** `evaluate()` is called with targetPolicy class equal to current policy class
- **THEN** `EvaluationResult.result()` MUST be `DENY` with reason containing "same type"

#### Scenario: Concurrent resize denied
- **WHEN** `evaluate()` is called and `isResizeInProgress.test(executorId)` returns true
- **THEN** `EvaluationResult.result()` MUST be `DENY` with reason containing "resize"

#### Scenario: Null target policy denied
- **WHEN** `evaluate()` is called with command.targetPolicy() returning null
- **THEN** `EvaluationResult.result()` MUST be `DENY`

---

### Requirement: RejectionPolicyAdjustmentAdapter integrates policy replacement into adjustment pipeline
The system MUST provide a `RejectionPolicyAdjustmentAdapter` that receives `RejectionPolicyCommand`, evaluates safety, and executes policy replacement, returning `PolicyReplacementResult`.

#### Scenario: Successful policy replacement via adapter
- **WHEN** `apply(executorId, command)` is called with a valid CallerRunsPolicy command
- **THEN** `PolicyReplacementResult.success()` MUST be true and `evidence()` MUST return non-null PolicyReplacementEvidence with afterPolicyClass containing "CallerRunsPolicy"

#### Scenario: Safety gate denial via adapter
- **WHEN** `apply()` is called but safety gate denies
- **THEN** `PolicyReplacementResult.success()` MUST be false with failureCode="SAFETY_GATE_DENIED"

#### Scenario: Executor not found
- **WHEN** `apply()` is called with an executorId not in registry
- **THEN** `PolicyReplacementResult.success()` MUST be false with failureCode="EXECUTOR_NOT_FOUND"

#### Scenario: Policy set failure
- **WHEN** `apply()` is called but `executor.setRejectionPolicy()` throws RuntimeException
- **THEN** `PolicyReplacementResult.success()` MUST be false with failureCode="POLICY_SET_FAILED"

#### Scenario: Existing adapters unaffected
- **WHEN** `ManagedExecutorAdjustmentAdapter.apply(ScaleAdjustmentCommand)` or `QueueResizeAdjustmentAdapter.apply(String, QueueResizeCommand)` is called
- **THEN** both MUST continue to work without any change in behavior

---

### Requirement: PolicyReplacementResult provides dedicated result type
The system MUST provide `PolicyReplacementResult` as the dedicated return type for `RejectionPolicyAdjustmentAdapter`, with distinct factories for success, denied, and failed outcomes.

#### Scenario: Success result carries evidence
- **WHEN** `PolicyReplacementResult.success(evidence)` is called
- **THEN** `success()` MUST be true, `evidence()` MUST return the evidence, `failureCode()` MUST be null

#### Scenario: Denied result carries failure code and evidence
- **WHEN** `PolicyReplacementResult.denied("SAFETY_GATE_DENIED", "reason", evidence)` is called
- **THEN** `success()` MUST be false, `failureCode()` MUST be "SAFETY_GATE_DENIED", `evidence()` MUST return the evidence

#### Scenario: Failed result without evidence
- **WHEN** `PolicyReplacementResult.failed("EXECUTOR_NOT_FOUND", "reason")` is called
- **THEN** `success()` MUST be false, `failureCode()` MUST be "EXECUTOR_NOT_FOUND", `evidence()` MUST be null

#### Scenario: Failed result with evidence
- **WHEN** `PolicyReplacementResult.failed("POLICY_SET_FAILED", "reason", evidence)` is called
- **THEN** `success()` MUST be false, `failureCode()` MUST be "POLICY_SET_FAILED", `evidence()` MUST return the evidence

---

### Requirement: PolicyReplacementEvidence records replacement operation details
The system MUST record complete evidence for every rejection policy replacement operation.

#### Scenario: Successful replacement evidence
- **WHEN** a policy replacement completes successfully
- **THEN** `PolicyReplacementEvidence` MUST contain: beforePolicyClass (non-null), afterPolicyClass (non-null, different from before), executorState (non-null), replacedAt (non-null), success=true, reason (non-null)

#### Scenario: Denied replacement evidence
- **WHEN** a policy replacement is denied by safety gate
- **THEN** `PolicyReplacementEvidence` MUST contain: beforePolicyClass == afterPolicyClass (unchanged), success=false, reason (non-null describing denial)

---

### Requirement: ExecutorRebuildStrategy preserves original rejection policy
The system MUST preserve the original `RejectedExecutionHandler` during executor rebuild, rather than hardcoding `AbortPolicy`.

#### Scenario: Non-default policy preserved after rebuild
- **WHEN** `rebuild()` is called on an executor created with CallerRunsPolicy
- **THEN** the newly commissioned executor's rejection policy MUST be CallerRunsPolicy (not AbortPolicy)

#### Scenario: Default AbortPolicy still preserved
- **WHEN** `rebuild()` is called on an executor created with AbortPolicy (default)
- **THEN** the newly commissioned executor's rejection policy MUST be AbortPolicy (unchanged, backward compatible)

---

### Requirement: QueueResizeAdjustmentAdapter exposes resize-in-progress state
The system MUST provide a public method to query whether a resize is in progress for a given executor ID, enabling cross-adapter concurrency protection.

#### Scenario: Resize in progress returns true
- **WHEN** `isResizeInProgress(executorId)` is called during an active resize on that executor
- **THEN** the method MUST return true

#### Scenario: No resize returns false
- **WHEN** `isResizeInProgress(executorId)` is called when no resize is in progress
- **THEN** the method MUST return false
