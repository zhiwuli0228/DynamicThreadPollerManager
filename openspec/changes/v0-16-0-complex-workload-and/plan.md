# Execution Plan

## Execution Strategy

Use one fresh OpenCode session per unchecked task. The external Runner is the only lifecycle writer.

**Overall approach**: Hybrid — extend enums in-place, compose behaviors via new decorator/guard types, fix v0.15 risks in-place where the fix is small and isolated. Follow the existing decorator pattern established by `CoordinatedAdjustmentAdapter`. Each task is independently testable and verifiable.

**Dependency ordering**: Tasks within sections 1-6 are largely independent (cross-cutting concerns compose via interfaces). Section 7 (integration) depends on all prior sections. Section 8 (verification) depends on all prior sections.

**Quality gate per task**: Each task must compile (`mvn -DskipTests test-compile`), pass its focused tests, and not break existing tests. Checkpoint after each section completes and passes focused verification.

## Tasks

### Task 1.1 — Extend ScenarioProfile enum

- Requirement/scenario: `ScenarioProfile` SHALL include `LONG_TAIL`, `MIXED_CPU_IO`, `DOWNSTREAM_BLOCKED` values
- Files to read: `src/main/java/**/scenario/ScenarioProfile.java`
- Allowed files: `src/main/java/**/scenario/ScenarioProfile.java`
- Test to add or modify: `src/test/java/**/scenario/ScenarioProfileTest.java` — verify enum values exist and are distinct from `STEADY`, `RAMP`, `BURST`
- Expected failing signal: Enum constants `LONG_TAIL`, `MIXED_CPU_IO`, `DOWNSTREAM_BLOCKED` do not exist
- Minimum implementation: Add three new enum constants to `ScenarioProfile`
- Focused verification: `mvn -DskipTests test-compile` then run `ScenarioProfileTest`

### Task 1.2 — Extend DeterministicScenarioPlanner for LONG_TAIL

- Requirement/scenario: LONG_TAIL plan with seed % 3 == 0 produces spike steps; seed % 3 != 0 produces base steps; plan is deterministic
- Files to read: `src/main/java/**/scenario/DeterministicScenarioPlanner.java`, `src/main/java/**/scenario/ScenarioStep.java`, `src/main/java/**/scenario/ScenarioPlan.java`
- Allowed files: `src/main/java/**/scenario/DeterministicScenarioPlanner.java`
- Test to add or modify: `src/test/java/**/scenario/DeterministicScenarioPlannerTest.java` — LONG_TAIL with seed=6 → workUnits=600; seed=7 → workUnits=100; determinism check
- Expected failing signal: `workUnitsFor()` switch has no `LONG_TAIL` case
- Minimum implementation: Add `LONG_TAIL` case: `workUnits = baseWorkUnits + (seed % 3 == 0 ? baseWorkUnits * 5 : 0)`
- Focused verification: Run `DeterministicScenarioPlannerTest` LONG_TAIL tests

### Task 1.3 — Extend DeterministicScenarioPlanner for MIXED_CPU_IO

- Requirement/scenario: MIXED_CPU_IO alternates CPU (even index: workUnits = baseWorkUnits * 3) and IO (odd index: workUnits = baseWorkUnits, delay = baseWorkUnits * 2) steps
- Files to read: `src/main/java/**/scenario/DeterministicScenarioPlanner.java`
- Allowed files: `src/main/java/**/scenario/DeterministicScenarioPlanner.java`
- Test to add or modify: `src/test/java/**/scenario/DeterministicScenarioPlannerTest.java` — verify alternation with baseWorkUnits=50
- Expected failing signal: `workUnitsFor()` switch has no `MIXED_CPU_IO` case
- Minimum implementation: Add `MIXED_CPU_IO` case with even/odd index logic
- Focused verification: Run `DeterministicScenarioPlannerTest` MIXED_CPU_IO tests

### Task 1.4 — Extend DeterministicScenarioPlanner for DOWNSTREAM_BLOCKED

- Requirement/scenario: DOWNSTREAM_BLOCKED uses constant work units with high delay (workUnits = baseWorkUnits, delay = baseWorkUnits * 10)
- Files to read: `src/main/java/**/scenario/DeterministicScenarioPlanner.java`
- Allowed files: `src/main/java/**/scenario/DeterministicScenarioPlanner.java`
- Test to add or modify: `src/test/java/**/scenario/DeterministicScenarioPlannerTest.java` — verify with baseWorkUnits=200 → workUnits=200, delay=2000
- Expected failing signal: `workUnitsFor()` switch has no `DOWNSTREAM_BLOCKED` case
- Minimum implementation: Add `DOWNSTREAM_BLOCKED` case with constant work and high delay
- Focused verification: Run `DeterministicScenarioPlannerTest` DOWNSTREAM_BLOCKED tests

### Task 1.5 — Verify seed reproducibility

- Requirement/scenario: Different seeds produce different LONG_TAIL plans when seed % 3 differs; same seed produces identical plans for any new profile
- Files to read: (same as 1.2-1.4)
- Allowed files: `src/test/java/**/scenario/DeterministicScenarioPlannerTest.java`
- Test to add or modify: Add seed reproducibility tests to `DeterministicScenarioPlannerTest`
- Expected failing signal: N/A (tests only)
- Minimum implementation: Test-only task
- Focused verification: Run full `DeterministicScenarioPlannerTest`

### Task 2.1 — Create RollbackAwareAdjustmentAdapter class with null checks

- Requirement/scenario: Implement `ExecutorAdjustmentAdapter` interface; reject null delegate with NPE
- Files to read: `src/main/java/**/adjustment/ExecutorAdjustmentAdapter.java`, `src/main/java/**/adjustment/AdjustmentResult.java`, `src/main/java/**/adjustment/ScaleAdjustmentCommand.java`, `src/main/java/**/adjustment/ExecutorStateSnapshot.java`, `src/main/java/**/adjustment/RuntimeAdjustmentSafetyGate.java`, `src/main/java/**/loop/LoopEvidenceRecorder.java`
- Allowed files: `src/main/java/**/adjustment/RollbackAwareAdjustmentAdapter.java` (new), `src/test/java/**/adjustment/RollbackAwareAdjustmentAdapterTest.java` (new)
- Test to add or modify: `RollbackAwareAdjustmentAdapterTest` — rejects null delegate; currentState delegates
- Expected failing signal: Class does not exist
- Minimum implementation: Create class implementing `ExecutorAdjustmentAdapter` with delegate field, null check in constructor, `currentState()` delegation
- Focused verification: Compile and run `RollbackAwareAdjustmentAdapterTest`

### Task 2.2 — Implement pre-adjustment snapshot capture

- Requirement/scenario: Pre-adjustment snapshot is captured before delegate apply
- Files to read: (same as 2.1)
- Allowed files: `src/main/java/**/adjustment/RollbackAwareAdjustmentAdapter.java`
- Test to add or modify: `RollbackAwareAdjustmentAdapterTest` — verify beforeState in result
- Expected failing signal: No pre-snapshot capture in `apply()`
- Minimum implementation: Call `currentState()` before `delegate.apply()`, store as `preSnapshot`
- Focused verification: Run focused test

### Task 2.3 — Implement degradation detection and rollback

- Requirement/scenario: Degradation detected triggers rollback; no degradation does not trigger rollback
- Files to read: `src/main/java/**/adjustment/RollbackAwareAdjustmentAdapter.java`, `src/main/java/**/adjustment/AdjustmentFailureCode.java`
- Allowed files: `src/main/java/**/adjustment/RollbackAwareAdjustmentAdapter.java`, `src/main/java/**/adjustment/DegradationConfig.java` (new)
- Test to add or modify: `RollbackAwareAdjustmentAdapterTest` — mock post-state with high queue depth → verify rollback; mock post-state within threshold → no rollback
- Expected failing signal: No degradation check or rollback logic
- Minimum implementation: After delegate apply, sample post-state, compare against `DegradationConfig` thresholds, issue rollback `ScaleAdjustmentCommand` if degraded
- Focused verification: Run degradation trigger tests

### Task 2.4 — Implement rollback bounding (max 1 per decision)

- Requirement/scenario: Rollback does not recurse when rollback itself degrades
- Files to read: `src/main/java/**/adjustment/RollbackAwareAdjustmentAdapter.java`
- Allowed files: `src/main/java/**/adjustment/RollbackAwareAdjustmentAdapter.java`
- Test to add or modify: `RollbackAwareAdjustmentAdapterTest` — mock rollback causing degradation → verify no second rollback
- Expected failing signal: Unbounded recursion in rollback
- Minimum implementation: Use `rollingBack` flag to prevent re-entry
- Focused verification: Run rollback bounding test

### Task 2.5 — Implement safety gate integration for rollback

- Requirement/scenario: Safety gate allows rollback; safety gate rejects rollback → return original result
- Files to read: `src/main/java/**/adjustment/RollbackAwareAdjustmentAdapter.java`
- Allowed files: `src/main/java/**/adjustment/RollbackAwareAdjustmentAdapter.java`
- Test to add or modify: `RollbackAwareAdjustmentAdapterTest` — mock gate allow/reject rollback
- Expected failing signal: Rollback bypasses safety gate
- Minimum implementation: Pass rollback command through injected `RuntimeAdjustmentSafetyGate`; if rejected, return original result
- Focused verification: Run safety gate integration tests

### Task 2.6 — Implement rollback evidence recording

- Requirement/scenario: Successful and failed rollbacks are recorded in evidence
- Files to read: `src/main/java/**/adjustment/RollbackAwareAdjustmentAdapter.java`
- Allowed files: `src/main/java/**/adjustment/RollbackAwareAdjustmentAdapter.java`
- Test to add or modify: `RollbackAwareAdjustmentAdapterTest` — verify `LoopEvidenceRecorder.recordIteration()` called with rollback details
- Expected failing signal: No evidence recording for rollback actions
- Minimum implementation: Record all rollback actions via `LoopEvidenceRecorder`
- Focused verification: Run evidence recording tests

### Task 2.7 — Verify configurable degradation threshold

- Requirement/scenario: Custom threshold controls rollback behavior correctly
- Files to read: `src/main/java/**/adjustment/RollbackAwareAdjustmentAdapter.java`, `src/main/java/**/adjustment/DegradationConfig.java`
- Allowed files: `src/test/java/**/adjustment/RollbackAwareAdjustmentAdapterTest.java`
- Test to add or modify: Add boundary test with custom threshold
- Expected failing signal: N/A (test only)
- Minimum implementation: Test-only task
- Focused verification: Run `RollbackAwareAdjustmentAdapterTest`

### Task 3.1 — Create TimeBasedCooldownSafetyGate class

- Requirement/scenario: Implement `RuntimeAdjustmentSafetyGate`; accept `Supplier<Instant>`; reject null clock
- Files to read: `src/main/java/**/adjustment/RuntimeAdjustmentSafetyGate.java`, `src/main/java/**/adjustment/DefaultRuntimeAdjustmentSafetyGate.java`, `src/main/java/**/adjustment/SafetyGateDecision.java`, `src/main/java/**/adjustment/ScaleAdjustmentCommand.java`, `src/main/java/**/adjustment/AdjustmentFailureCode.java`
- Allowed files: `src/main/java/**/adjustment/TimeBasedCooldownSafetyGate.java` (new), `src/test/java/**/adjustment/TimeBasedCooldownSafetyGateTest.java` (new)
- Test to add or modify: `TimeBasedCooldownSafetyGateTest` — evaluates allowed command; rejects null clock
- Expected failing signal: Class does not exist
- Minimum implementation: Create class implementing `RuntimeAdjustmentSafetyGate` with `Supplier<Instant>` clock field, `Map<String, Instant> lastAppliedInstant`, `Duration cooldownDuration`
- Focused verification: Compile and run `TimeBasedCooldownSafetyGateTest`

### Task 3.2 — Implement time-based cooldown check

- Requirement/scenario: Command rejected during cooldown window; command allowed after cooldown expires
- Files to read: `src/main/java/**/adjustment/TimeBasedCooldownSafetyGate.java`
- Allowed files: `src/main/java/**/adjustment/TimeBasedCooldownSafetyGate.java`
- Test to add or modify: `TimeBasedCooldownSafetyGateTest` — apply at T0, evaluate at T0+500ms with 1s cooldown → REJECTED; evaluate at T0+1500ms → ALLOW
- Expected failing signal: No cooldown check
- Minimum implementation: `Duration.between(lastApplied, clock.get()).compareTo(cooldownDuration) < 0` → reject with `COOLDOWN_ACTIVE`
- Focused verification: Run cooldown timing tests

### Task 3.3 — Implement emergency rollback bypass

- Requirement/scenario: Emergency rollback bypasses active cooldown; non-emergency blocked during cooldown; emergency flag on non-rollback does not bypass
- Files to read: `src/main/java/**/adjustment/TimeBasedCooldownSafetyGate.java`, `src/main/java/**/adjustment/ScaleAdjustmentCommand.java`
- Allowed files: `src/main/java/**/adjustment/TimeBasedCooldownSafetyGate.java`, `src/main/java/**/adjustment/ScaleAdjustmentCommand.java` (add `emergencyRollback` field overload)
- Test to add or modify: `TimeBasedCooldownSafetyGateTest` — 4 emergency bypass scenarios
- Expected failing signal: Emergency rollback blocked by cooldown
- Minimum implementation: Check `command.isEmergencyRollback()` before cooldown; only bypass when target == previous safe state
- Focused verification: Run emergency bypass tests

### Task 3.4 — Preserve all other safety checks

- Requirement/scenario: NOT_READY, RUN_LIMIT_EXCEEDED, OPPOSITE_DIRECTION, NO_OP checks preserved
- Files to read: `src/main/java/**/adjustment/DefaultRuntimeAdjustmentSafetyGate.java`
- Allowed files: `src/main/java/**/adjustment/TimeBasedCooldownSafetyGate.java`
- Test to add or modify: `TimeBasedCooldownSafetyGateTest` — NOT_READY rejection, per-run limit rejection, no-op detection
- Expected failing signal: Missing safety checks
- Minimum implementation: Port readiness, per-run limit, opposite direction, and no-op checks from `DefaultRuntimeAdjustmentSafetyGate`
- Focused verification: Run all safety check tests

### Task 3.5 — Verify controllable clock testability

- Requirement/scenario: Test controls time advancement without sleep
- Files to read: (same as 3.1)
- Allowed files: `src/test/java/**/adjustment/TimeBasedCooldownSafetyGateTest.java`
- Test to add or modify: Use `AtomicReference<Instant>` to control time
- Expected failing signal: N/A (test only)
- Minimum implementation: Test-only task
- Focused verification: Run `TimeBasedCooldownSafetyGateTest`

### Task 4.1 — Add ANTI_OSCILLATION_ACTIVE to AdjustmentFailureCode

- Requirement/scenario: `ANTI_OSCILLATION_ACTIVE` constant exists and can be used in `SafetyGateDecision.rejected()`
- Files to read: `src/main/java/**/adjustment/AdjustmentFailureCode.java`, `src/main/java/**/adjustment/SafetyGateDecision.java`
- Allowed files: `src/main/java/**/adjustment/AdjustmentFailureCode.java`
- Test to add or modify: `src/test/java/**/adjustment/AdjustmentFailureCodeTest.java` — verify constant exists
- Expected failing signal: `ANTI_OSCILLATION_ACTIVE` not found
- Minimum implementation: Add `ANTI_OSCILLATION_ACTIVE` enum constant
- Focused verification: Compile and run `AdjustmentFailureCodeTest`

### Task 4.2 — Create AntiOscillationGuard class

- Requirement/scenario: Threshold controls activation; no oscillation allows adjustment
- Files to read: `src/main/java/**/loop/OscillationDetector.java`, `src/main/java/**/adjustment/SafetyGateDecision.java`, `src/main/java/**/loop/LoopEvidenceRecorder.java`
- Allowed files: `src/main/java/**/loop/AntiOscillationGuard.java` (new), `src/test/java/**/loop/AntiOscillationGuardTest.java` (new)
- Test to add or modify: `AntiOscillationGuardTest` — threshold controls activation; no oscillation allows
- Expected failing signal: Class does not exist
- Minimum implementation: Create class with `OscillationDetector` and `blockThreshold` fields, `consecutiveOscillations` counter, `activated` flag, `synchronized evaluate()` method
- Focused verification: Compile and run `AntiOscillationGuardTest`

### Task 4.3 — Implement sustained oscillation blocking and emergency bypass

- Requirement/scenario: Sustained oscillation blocks non-emergency; emergency rollback bypasses guard
- Files to read: `src/main/java/**/loop/AntiOscillationGuard.java`
- Allowed files: `src/main/java/**/loop/AntiOscillationGuard.java`
- Test to add or modify: `AntiOscillationGuardTest` — sustained oscillation blocks; emergency bypasses; non-emergency blocked when active
- Expected failing signal: No blocking logic
- Minimum implementation: Consult `OscillationDetector.wouldOscillate()`, increment counter, activate when exceeds threshold, block non-emergency, allow emergency rollback
- Focused verification: Run blocking and bypass tests

### Task 4.4 — Implement guard reset on stable adjustment

- Requirement/scenario: Guard resets after stable adjustment; guard remains active during continued oscillation
- Files to read: `src/main/java/**/loop/AntiOscillationGuard.java`
- Allowed files: `src/main/java/**/loop/AntiOscillationGuard.java`
- Test to add or modify: `AntiOscillationGuardTest` — reset on stable; remain active on continued oscillation
- Expected failing signal: Guard never resets
- Minimum implementation: Reset `consecutiveOscillations` to 0 when stable decision succeeds
- Focused verification: Run reset tests

### Task 4.5 — Implement evidence recording for blocked adjustments

- Requirement/scenario: Blocked adjustment is recorded in evidence with reason
- Files to read: `src/main/java/**/loop/AntiOscillationGuard.java`
- Allowed files: `src/main/java/**/loop/AntiOscillationGuard.java`
- Test to add or modify: `AntiOscillationGuardTest` — verify `LoopEvidenceRecorder` called with block reason
- Expected failing signal: No evidence recording
- Minimum implementation: Record block reason via `LoopEvidenceRecorder` when guard blocks
- Focused verification: Run evidence recording test

### Task 4.6 — Integrate guard into AdjustmentLoop lifecycle

- Requirement/scenario: Guard is consulted after oscillationDetector and before safetyGate
- Files to read: `src/main/java/**/loop/AdjustmentLoop.java`
- Allowed files: `src/main/java/**/loop/AdjustmentLoop.java`
- Test to add or modify: `src/test/java/**/loop/AdjustmentLoopIntegrationTest.java` — verify guard.evaluate() called in correct position
- Expected failing signal: Guard not integrated into loop
- Minimum implementation: Add `AntiOscillationGuard` as optional constructor parameter (nullable); evaluate between oscillation check and safety gate
- Focused verification: Run integration test

### Task 5.1 — Create ComplexScenarioReport record

- Requirement/scenario: Report has all identification fields, counts, rollback success rate, recovery time, percentiles, deltas, observation windows, timestamp
- Files to read: `src/main/java/**/metrics/ObservedSnapshot.java`
- Allowed files: `src/main/java/**/validation/ComplexScenarioReport.java` (new), `src/main/java/**/validation/ObservationWindow.java` (new), `src/test/java/**/validation/ComplexScenarioReportTest.java` (new)
- Test to add or modify: `ComplexScenarioReportTest` — construct with all fields; rollback success rate computation; zero rollbacks
- Expected failing signal: Class does not exist
- Minimum implementation: Create `ComplexScenarioReport` record and `ObservationWindow` record with all required fields
- Focused verification: Compile and run `ComplexScenarioReportTest`

### Task 5.2 — Create DegradationConfig record

- Requirement/scenario: Config is immutable and accessible
- Files to read: (none beyond existing types)
- Allowed files: `src/main/java/**/adjustment/DegradationConfig.java` (new), `src/test/java/**/adjustment/DegradationConfigTest.java` (new)
- Test to add or modify: `DegradationConfigTest` — construct with fields, verify accessors
- Expected failing signal: Class does not exist
- Minimum implementation: Create `DegradationConfig` record with `queueDepthThreshold`, `throughputDropThreshold`, `latencyIncreaseThreshold`
- Focused verification: Compile and run `DegradationConfigTest`

### Task 5.3 — Create ComplexScenarioReportGenerator class

- Requirement/scenario: Generator reads from real evidence; rejects null evidence; produces report
- Files to read: `src/main/java/**/metrics/EvidenceRecorder.java`, `src/main/java/**/loop/LoopEvidenceRecorder.java`, `src/main/java/**/loop/AdjustmentHistory.java`, `src/main/java/**/metrics/ObservedSnapshot.java`
- Allowed files: `src/main/java/**/validation/ComplexScenarioReportGenerator.java` (new), `src/test/java/**/validation/ComplexScenarioReportGeneratorTest.java` (new)
- Test to add or modify: `ComplexScenarioReportGeneratorTest` — produce report from real evidence; reject null evidence; observation windows from real snapshots
- Expected failing signal: Class does not exist
- Minimum implementation: Create generator that reads from `EvidenceRecorder`, `LoopEvidenceRecorder`, `AdjustmentHistory` and computes all metrics from real data
- Focused verification: Compile and run `ComplexScenarioReportGeneratorTest`

### Task 5.4 — Implement percentile and delta computation

- Requirement/scenario: Percentile latencies computed from real snapshots; deltas reflect start-to-end change
- Files to read: `src/main/java/**/validation/ComplexScenarioReportGenerator.java`
- Allowed files: `src/main/java/**/validation/ComplexScenarioReportGenerator.java`
- Test to add or modify: `ComplexScenarioReportGeneratorTest` — percentile computation; delta computation
- Expected failing signal: Synthetic data or missing computation
- Minimum implementation: Implement p95/p99 percentile calculation and queue depth/throughput delta from real snapshot arrays
- Focused verification: Run percentile and delta tests

### Task 6.1 — Fix ClosedLoopValidationRunner.computeSignificance()

- Requirement/scenario: Significance computation uses real snapshot data; rejects synthetic proxy arrays
- Files to read: `src/main/java/**/validation/ClosedLoopValidationRunner.java`, `src/main/java/**/metrics/InMemoryEvidenceRecorder.java`
- Allowed files: `src/main/java/**/validation/ClosedLoopValidationRunner.java`
- Test to add or modify: `src/test/java/**/validation/ClosedLoopValidationRunnerTest.java` — verify no proxy array generation; real data patterns
- Expected failing signal: Lines 389-391 contain `cl + (Math.random() - 0.5) * cl * 0.1`
- Minimum implementation: Replace proxy generation with `InMemoryEvidenceRecorder.snapshots(runId)`, extract metric per snapshot as paired arrays
- Focused verification: Run `ClosedLoopValidationRunnerTest`

### Task 6.2 — Fix GroupLoopOrchestrator.startAll() null registry

- Requirement/scenario: startAll() does not create ExecutorRegistry with null; valid components succeed
- Files to read: `src/main/java/**/coordination/GroupLoopOrchestrator.java`, `src/main/java/**/coordination/ExecutorRegistry.java`
- Allowed files: `src/main/java/**/coordination/GroupLoopOrchestrator.java`
- Test to add or modify: `src/test/java/**/coordination/GroupLoopOrchestratorTest.java` — verify null not passed to ExecutorRegistry
- Expected failing signal: Line 70 creates `new ExecutorRegistry(null)`
- Minimum implementation: Add null check or accept non-null `ExecutorRegistry` parameter
- Focused verification: Run `GroupLoopOrchestratorTest`

### Task 6.3 — Document EvidenceRecorder thread-safety contract

- Requirement/scenario: Javadoc specifies concurrent safety guarantees
- Files to read: `src/main/java/**/metrics/EvidenceRecorder.java`
- Allowed files: `src/main/java/**/metrics/EvidenceRecorder.java`
- Test to add or modify: Verify Javadoc content (documented in test or manual check)
- Expected failing signal: No thread-safety documentation
- Minimum implementation: Add Javadoc documenting concurrent safety of `record()` and `snapshots()`
- Focused verification: Read file and verify Javadoc present

### Task 6.4 — Document LoopEvidenceRecorder thread-safety contract

- Requirement/scenario: Javadoc specifies concurrent safety guarantees for all methods
- Files to read: `src/main/java/**/loop/LoopEvidenceRecorder.java`
- Allowed files: `src/main/java/**/loop/LoopEvidenceRecorder.java`
- Test to add or modify: Verify Javadoc content
- Expected failing signal: No thread-safety documentation
- Minimum implementation: Add Javadoc documenting concurrent safety of all methods
- Focused verification: Read file and verify Javadoc present

### Task 6.5 — Add InMemoryEvidenceRecorder contention tests

- Requirement/scenario: 4 threads × 50 writes via CyclicBarrier, verify 200 snapshots in order; concurrent writes to different runIds are independent
- Files to read: `src/main/java/**/metrics/InMemoryEvidenceRecorder.java`
- Allowed files: `src/test/java/**/metrics/InMemoryEvidenceRecorderConcurrencyTest.java` (new)
- Test to add or modify: `InMemoryEvidenceRecorderConcurrencyTest` — CyclicBarrier concurrent write test
- Expected failing signal: N/A (new test)
- Minimum implementation: Create concurrency test with `CyclicBarrier`, 4 threads × 50 writes, verify 200 snapshots in insertion order
- Focused verification: Run `InMemoryEvidenceRecorderConcurrencyTest`

### Task 6.6 — Add FileBackedEvidenceRecorder contention tests

- Requirement/scenario: 4 threads × 50 writes, verify no data loss
- Files to read: `src/main/java/**/acquisition/FileBackedEvidenceRecorder.java`
- Allowed files: `src/test/java/**/acquisition/FileBackedEvidenceRecorderConcurrencyTest.java` (new)
- Test to add or modify: `FileBackedEvidenceRecorderConcurrencyTest` — CyclicBarrier concurrent write test
- Expected failing signal: N/A (new test)
- Minimum implementation: Create concurrency test verifying no data loss under concurrent writes
- Focused verification: Run `FileBackedEvidenceRecorderConcurrencyTest`

### Task 6.7 — Add CoordinatedAdjustmentAdapter behavior tests

- Requirement/scenario: Rejection path, capping path, approval path, currentState delegation
- Files to read: `src/main/java/**/coordination/CoordinatedAdjustmentAdapter.java`, `src/main/java/**/coordination/GroupCoordinator.java`, `src/main/java/**/coordination/GroupCoordinationResult.java`, `src/main/java/**/coordination/CoordinationOutcome.java`
- Allowed files: `src/test/java/**/coordination/CoordinatedAdjustmentAdapterTest.java` (new)
- Test to add or modify: `CoordinatedAdjustmentAdapterTest` — 4 scenarios (reject, cap, approve, delegation)
- Expected failing signal: N/A (new test)
- Minimum implementation: Create tests using mocks for coordinator and delegate
- Focused verification: Run `CoordinatedAdjustmentAdapterTest`

### Task 6.8 — Add GroupLoopOrchestrator behavior tests

- Requirement/scenario: startAll() creates loops; emergencyStopAll() stops all; getGroupHealth() reflects state
- Files to read: `src/main/java/**/coordination/GroupLoopOrchestrator.java`
- Allowed files: `src/test/java/**/coordination/GroupLoopOrchestratorTest.java` (new or extend existing)
- Test to add or modify: `GroupLoopOrchestratorTest` — 3 executor scenarios
- Expected failing signal: N/A (new test)
- Minimum implementation: Create tests verifying loop creation, emergency stop propagation, and health reporting
- Focused verification: Run `GroupLoopOrchestratorTest`

### Task 7.1 — Complex scenario end-to-end tests

- Requirement/scenario: BURST end-to-end; LONG_TAIL with degradation triggers rollback; cooldown prevents rapid adjustments; anti-oscillation blocks sustained ping-pong
- Files to read: All types created in tasks 1-6
- Allowed files: `src/test/java/**/validation/ComplexScenarioIntegrationTest.java` (new)
- Test to add or modify: `ComplexScenarioIntegrationTest` — 4 integration scenarios
- Expected failing signal: N/A (new test)
- Minimum implementation: Create integration tests wiring scenario profiles, rollback adapter, cooldown gate, anti-oscillation guard, and report generator
- Focused verification: Run `ComplexScenarioIntegrationTest`

### Task 7.2 — Regression verification

- Requirement/scenario: All existing tests pass; no changes to protected APIs, dependencies, or build config
- Files to read: N/A
- Allowed files: N/A
- Test to add or modify: N/A
- Expected failing signal: `mvn test` failure
- Minimum implementation: Run `mvn test` and verify all pass
- Focused verification: `mvn test` full suite

## Verification

1. **Focused per-task**: Each task runs its focused test class after implementation.
2. **Section checkpoint**: After each section (1-6), run `mvn -DskipTests test-compile` to verify compilation.
3. **Integration checkpoint**: After section 7, run `mvn test` to verify all tests pass.
4. **Final verification** (Task 8):
   - Run `mvn test` — all existing and new tests must pass.
   - Verify no changes to `provided-api/`, `src/**/api/**`, `src/**/contract/**`, `pom.xml`.
   - Verify all spec requirements have corresponding tests (traceability matrix in design.md).
   - Verify no synthetic statistical data in any computation (grep for `Math.random` in validation package).
   - Verify no `Thread.sleep()` in new tests (grep for `Thread.sleep` in test files).

## Checkpoint Strategy

Checkpoint only after deterministic gates pass:

- **Checkpoint 1**: Section 1 complete — `ScenarioProfile` extended, all planner tests pass
- **Checkpoint 2**: Section 2 complete — `RollbackAwareAdjustmentAdapter` fully tested
- **Checkpoint 3**: Section 3 complete — `TimeBasedCooldownSafetyGate` fully tested
- **Checkpoint 4**: Section 4 complete — `AntiOscillationGuard` integrated and tested
- **Checkpoint 5**: Section 5 complete — `ComplexScenarioReport` and generator tested
- **Checkpoint 6**: Section 6 complete — All v0.15 risk fixes verified
- **Checkpoint 7**: Section 7 complete — Integration tests pass, `mvn test` full suite passes
