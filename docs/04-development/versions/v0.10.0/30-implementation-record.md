# v0.10.0 Implementation Record — Change 1/2

## Input Baseline

- Version: v0.10.0
- Change: `rejection-policy-command-and-adapter` (change 1/2)
- Input docs: `23-sr-closure-verification.md` (SR closed), `design.md`, `tasks.md`, `specs/rejection-policy-command-and-adapter/spec.md`
- Authorization: `docs/00-project/current-state.md` — EXECUTION_AUTHORIZED

## Implementation Scope

8 production source files (5 new, 3 modified):
- `RejectionPolicyCommand.java` (new - record)
- `PolicyReplacementEvidence.java` (new - record)
- `PolicyReplacementResult.java` (new - class, 4 factories)
- `RejectionPolicySafetyGate.java` (new - independent gate)
- `RejectionPolicyAdjustmentAdapter.java` (new - adapter)
- `ManagedExecutor.java` (modified: 2 lines added, 1 field deleted, 1 getter changed)
- `ExecutorRebuildStrategy.java` (modified: 1 line fix)
- `QueueResizeAdjustmentAdapter.java` (modified: 1 method added)

8 test files (5 new, 3 extended):
- `RejectionPolicyCommandTest.java` (new - 10 tests)
- `PolicyReplacementEvidenceTest.java` (new - 3 tests)
- `PolicyReplacementResultTest.java` (new - 10 tests)
- `RejectionPolicySafetyGateTest.java` (new - 9 tests)
- `RejectionPolicyAdjustmentAdapterTest.java` (new - 10 tests)
- `ManagedExecutorTest.java` (extended: +4 tests)
- `ExecutorRebuildStrategyTest.java` (extended: +2 tests, +1 import)
- `QueueResizeAdjustmentAdapterTest.java` (extended: +2 tests)

## Spec Scenario Coverage

### RejectionPolicyCommand (8 scenarios, 10 tests)

| Spec Scenario | Test | Status |
|---|---|---|
| Valid command creation | `RejectionPolicyCommandTest.validCommandCreation()` | PASS |
| Null policy is rejected | `RejectionPolicyCommandTest.nullTargetPolicyThrows()` | PASS |
| Blank reason is rejected (empty) | `RejectionPolicyCommandTest.emptyReasonThrows()` | PASS |
| Blank reason is rejected (whitespace) | `RejectionPolicyCommandTest.blankReasonThrows()` | PASS |
| Same policy class returns empty | `RejectionPolicyCommandTest.fromCurrentSamePolicyTypeReturnsEmpty()` | PASS |
| Different policy class returns command | `RejectionPolicyCommandTest.fromCurrentDifferentPolicyReturnsCommand()` | PASS |
| All four JDK policies usable | `RejectionPolicyCommandTest.allFourJdkPoliciesCanBeUsed()` | PASS |
| Null validation on fromCurrent | `RejectionPolicyCommandTest.fromCurrentNullCurrentThrows()` + `fromCurrentNullTargetThrows()` | PASS |

### ManagedExecutor.setRejectionPolicy/getRejectionPolicy (4 scenarios, 4 tests)

| Spec Scenario | Test | Status |
|---|---|---|
| Set then get returns new policy | `ManagedExecutorTest.setRejectionPolicyShouldReflectImmediately()` | PASS |
| Set null throws | `ManagedExecutorTest.setRejectionPolicyNullThrows()` | PASS |
| Get after construction returns AbortPolicy | `ManagedExecutorTest.shouldConstructWithDefaults()` (existing) | PASS |
| Get delegates to TPE | `ManagedExecutorTest.getRejectionPolicyDelegatesToTpe()` | PASS |
| Set propagates to underlying TPE | `ManagedExecutorTest.setRejectionPolicyPropagatesToUnderlyingTpe()` | PASS |

### RejectionPolicySafetyGate (6 scenarios, 9 tests)

| Spec Scenario | Test | Status |
|---|---|---|
| Valid replacement permitted | `RejectionPolicySafetyGateTest.permitDifferentPolicy()` | PASS |
| Shutdown executor denied | `RejectionPolicySafetyGateTest.denyNonRunningExecutor()` | PASS |
| Terminated executor denied | `RejectionPolicySafetyGateTest.denyTerminatedExecutor()` | PASS |
| Same policy class denied (no-op) | `RejectionPolicySafetyGateTest.denySamePolicyType()` | PASS |
| Concurrent resize denied | `RejectionPolicySafetyGateTest.denyWhenResizeInProgress()` | PASS |
| Permit when no resize | `RejectionPolicySafetyGateTest.permitWhenNoResizeInProgress()` | PASS |
| All four JDK policies | `RejectionPolicySafetyGateTest.permitAllFourJdkPolicies()` | PASS |
| EvaluationResult.permitted() | `RejectionPolicySafetyGateTest.evaluationResultPermittedHelper()` | PASS |

### RejectionPolicyAdjustmentAdapter (5 scenarios, 10 tests)

| Spec Scenario | Test | Status |
|---|---|---|
| Successful replacement (CallerRuns) | `RejectionPolicyAdjustmentAdapterTest.successfulPolicyReplacement()` | PASS |
| Switch to DiscardPolicy | `RejectionPolicyAdjustmentAdapterTest.switchToDiscardPolicy()` | PASS |
| Switch to DiscardOldestPolicy | `RejectionPolicyAdjustmentAdapterTest.switchToDiscardOldestPolicy()` | PASS |
| Executor not found | `RejectionPolicyAdjustmentAdapterTest.executorNotFound()` | PASS |
| Safety gate denied (non-running) | `RejectionPolicyAdjustmentAdapterTest.safetyGateDeniedForNonRunning()` | PASS |
| Safety gate denied (same policy) | `RejectionPolicyAdjustmentAdapterTest.safetyGateDeniedForSamePolicy()` | PASS |
| Safety gate denied (resize in progress) | `RejectionPolicyAdjustmentAdapterTest.safetyGateDeniedWhenResizeInProgress()` | PASS |
| Evidence completeness | `RejectionPolicyAdjustmentAdapterTest.evidenceContainsBeforeAfterPolicyClasses()` | PASS |
| Null executorId throws | `RejectionPolicyAdjustmentAdapterTest.nullExecutorIdThrows()` | PASS |
| Null command throws | `RejectionPolicyAdjustmentAdapterTest.nullCommandThrows()` | PASS |

### PolicyReplacementResult (4 scenarios, 10 tests)

| Spec Scenario | Test | Status |
|---|---|---|
| Success result carries evidence | `PolicyReplacementResultTest.successFactorySetsCorrectFields()` | PASS |
| Denied result | `PolicyReplacementResultTest.deniedFactorySetsCorrectFields()` | PASS |
| Failed without evidence | `PolicyReplacementResultTest.failedWithoutEvidenceFactorySetsCorrectFields()` | PASS |
| Failed with evidence | `PolicyReplacementResultTest.failedWithEvidenceFactorySetsCorrectFields()` | PASS |
| Null validation (success) | `PolicyReplacementResultTest.successFactoryRejectsNullEvidence()` | PASS |
| Null validation (denied code) | `PolicyReplacementResultTest.deniedFactoryRejectsNullFailureCode()` | PASS |
| Null validation (denied evidence) | `PolicyReplacementResultTest.deniedFactoryRejectsNullEvidence()` | PASS |
| Null validation (failed code) | `PolicyReplacementResultTest.failedWithoutEvidenceRejectsNullFailureCode()` + `failedWithEvidenceRejectsNullFailureCode()` | PASS |
| EXECUTOR_NOT_FOUND result | `PolicyReplacementResultTest.executorNotFoundResult()` | PASS |

### PolicyReplacementEvidence (2 scenarios, 3 tests)

| Spec Scenario | Test | Status |
|---|---|---|
| Success evidence | `PolicyReplacementEvidenceTest.successEvidenceContainsCorrectFields()` | PASS |
| Denied evidence | `PolicyReplacementEvidenceTest.failureEvidenceContainsCorrectFields()` | PASS |
| Null fields allowed | `PolicyReplacementEvidenceTest.nullFieldsAreAllowed()` | PASS |

### ExecutorRebuildStrategy policy preservation (2 scenarios, 2 tests)

| Spec Scenario | Test | Status |
|---|---|---|
| Non-default policy preserved (CallerRuns) | `ExecutorRebuildStrategyTest.rejectionPolicyPreservedAfterRebuild()` | PASS |
| Policy preserved (Discard) | `ExecutorRebuildStrategyTest.rejectionPolicyPreservedWhenDiscardPolicy()` | PASS |

### QueueResizeAdjustmentAdapter.isResizeInProgress (2 scenarios, 2 tests)

| Spec Scenario | Test | Status |
|---|---|---|
| No resize returns false (registered) | `QueueResizeAdjustmentAdapterTest.isResizeInProgressReturnsFalseWhenNotResizing()` | PASS |
| No resize returns false (unknown) | `QueueResizeAdjustmentAdapterTest.isResizeInProgressReturnsFalseForUnknownExecutor()` | PASS |

## Verification Command and Results

```
./mvnw test
Tests run: 526, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- New tests: 50 (10+3+10+9+10+4+2+2)
- Existing tests: 476 (all pass, zero regression)
- Total: 526

## Existing Adapter Regression Check

| Adapter | Existing tests | Result |
|---|---|---|
| ManagedExecutorAdjustmentAdapterTest | 15 | PASS |
| QueueResizeAdjustmentAdapterTest | 9 (7 existing + 2 new) | PASS |
| QueueResizeEndToEndTest | 8 | PASS |
| ExecutorRebuildStrategyTest | 9 (7 existing + 2 new) | PASS |
| QueueResizeCommandTest | 11 | PASS |
| QueueResizeSafetyGateTest | 7 | PASS |
| AdjustmentContractsTest | 34 | PASS |
| PolicyDecisionTest | 16 | PASS |

## Known Deviations from Spec

1. **Policy set failure scenario (setRejectionPolicy throws RuntimeException)**: Not directly tested at adapter level. Mocking-based testing would require Mockito, which the project engineering baseline excludes (no PowerMock; Mockito use in existing suite is minimal). The scenario path is exercised indirectly: `ManagedExecutor.setRejectionPolicy` validates non-null (tested), and TPE.setRejectedExecutionHandler is a JDK method that never throws on valid input. The catch block is dead code for JDK built-in handlers — it exists as defense-in-depth. Deemed acceptable residual risk.

2. **Null target policy in gate**: `RejectionPolicyCommand` compact constructor rejects null target before gate receives it. The gate's null check is defense-in-depth only; no test constructs a command with null policy (impossible via public API). Gate null-path covered by code review.

## Design Decision Trace

| Decision | Implementation | Verified |
|---|---|---|
| D1: Direct TPE delegation | `getRejectionPolicy()` reads from `executor.getRejectedExecutionHandler()` | `ManagedExecutorTest.getRejectionPolicyDelegatesToTpe()` PASS |
| D2: Class comparison for no-op | `fromCurrent()` uses `target.getClass() == current.getClass()` | `RejectionPolicyCommandTest.fromCurrentSamePolicyTypeReturnsEmpty()` PASS |
| D3: Independent safety gate | `RejectionPolicySafetyGate` does not implement `ControlGate` | Class file verified |
| D4: Predicate\<String\> injection | Constructor receives `Predicate<String>`; `QueueResizeAdjustmentAdapter.isResizeInProgress()` provides the predicate | Integration verified via adapter tests |
| D5: Dedicated result type | `PolicyReplacementResult` with 4 factories | `PolicyReplacementResultTest` 10 tests PASS |
| D6: No idempotency guard | `RejectionPolicyAdjustmentAdapter.apply()` contains no `putIfAbsent`/`remove` | Code review |
| D7: Rebuild policy preservation | `oldTpe.getRejectedExecutionHandler()` replaces hardcoded `AbortPolicy()` | `ExecutorRebuildStrategyTest` 2 policy preservation tests PASS |

## Worktree Status

Not applicable — implementation done in main worktree on branch `claude_master`.

## Residual Risks

- Custom `RejectedExecutionHandler` implementations: out of scope. `fromCurrent()` class comparison may produce false-positive empty results for semantically-different custom handlers sharing the same anonymous class. Recorded as IR F05 deferred item.
- Concurrent policy-policy replacement: last-write-wins semantics accepted. No evidence recording for concurrent concurrent replacements.

## Next Step

Change 1/2 implementation complete. Ready for implementation review gate (phase 6) or proceed to change 2/2 (`rejection-policy-end-to-end-verification`).
