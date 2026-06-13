## Purpose

Define end-to-end verification scenarios for runtime rejection-policy replacement, validating that policy switches produce correct observable behavior under overload, that safety gate correctly denies invalid replacements, and that executor rebuild preserves non-default policies.

## Requirements

### Requirement: Policy switch produces correct overload behavior
The system MUST verify that after a rejection policy switch, overload behavior matches the target policy's specification.

#### Scenario: AbortPolicy → CallerRunsPolicy
- **GIVEN** an executor with AbortPolicy, pool=1-1, queue=2
- **WHEN** all threads and queue slots are occupied, and an additional task is submitted
- **THEN** a `RejectedExecutionException` MUST be thrown (AbortPolicy behavior)
- **WHEN** the policy is switched to CallerRunsPolicy and the overload scenario is repeated
- **THEN** no `RejectedExecutionException` MUST be thrown (CallerRunsPolicy behavior — caller thread executes)

#### Scenario: AbortPolicy → DiscardPolicy
- **GIVEN** an executor with AbortPolicy, pool=1-1, queue=2
- **WHEN** overload task is submitted → RejectedExecutionException
- **WHEN** policy is switched to DiscardPolicy and overload scenario is repeated
- **THEN** no exception MUST be thrown and taskCount MUST remain unchanged (silent discard)

#### Scenario: DiscardOldestPolicy eviction
- **GIVEN** an executor with DiscardOldestPolicy, pool=1-1, queue=2
- **WHEN** the queue is filled with Task-A then Task-B, and Task-C is submitted as overload
- **THEN** Task-A (oldest) MUST be evicted and Task-C MUST be enqueued

---

### Requirement: Safety gate denies invalid policy replacements
The system MUST verify that `RejectionPolicySafetyGate` correctly denies invalid replacement attempts in an end-to-end context.

#### Scenario: Deny on shutdown executor
- **GIVEN** a shutdown executor
- **WHEN** a policy replacement is attempted
- **THEN** the result MUST be denied with failureCode="SAFETY_GATE_DENIED"

#### Scenario: Deny on same policy type (no-op)
- **GIVEN** an executor with AbortPolicy
- **WHEN** a policy replacement is attempted with another AbortPolicy instance
- **THEN** the result MUST be denied with failureCode="SAFETY_GATE_DENIED" (no-op detection)

---

### Requirement: Executor rebuild preserves non-default rejection policy
The system MUST verify that `ExecutorRebuildStrategy` retains the original rejection policy after a queue resize rebuild.

#### Scenario: CallerRunsPolicy preserved after EXPAND rebuild
- **GIVEN** an executor created with CallerRunsPolicy and queue=5
- **WHEN** a queue resize (EXPAND to 10) is performed
- **THEN** the newly commissioned executor's rejection policy MUST still be CallerRunsPolicy

---

### Requirement: Policy replacement evidence is complete
The system MUST verify that `PolicyReplacementEvidence` contains all required fields after a successful policy switch.

#### Scenario: Evidence fields populated
- **WHEN** a policy switch from AbortPolicy to CallerRunsPolicy completes successfully
- **THEN** evidence.beforePolicyClass MUST end with "AbortPolicy", evidence.afterPolicyClass MUST end with "CallerRunsPolicy", evidence.success MUST be true, evidence.replacedAt MUST be non-null, evidence.executorState MUST be non-null, evidence.reason MUST be non-null

---

### Requirement: Non-existent executor returns failure
The system MUST verify that attempting a policy switch on a non-existent executor returns a clear failure.

#### Scenario: Executor not found
- **GIVEN** no executor is registered with id "nonexistent"
- **WHEN** a policy replacement is attempted on "nonexistent"
- **THEN** result.success() MUST be false and result.failureCode() MUST be "EXECUTOR_NOT_FOUND"
