# v0.15.0 IR Review Disposition

## Header

- Document type: IR review disposition
- Version: `v0.15.0`
- Date: `2026-06-17`
- Status: `DISPOSITION_COMPLETE` — all findings disposed, pending closure verification
- Review artifact: `11-ir-review.md`
- IR artifact: `10-ir.md`

## Disposition Summary

| Finding | Severity | Disposition | Action |
|---|---|---|---|
| P0-01 | P0 | ACCEPTED | Resolve coordinator input type: accept `ScaleAdjustmentCommand` |
| P1-01 | P1 | ACCEPTED | Document safety gate + coordination interaction |
| P1-02 | P1 | ACCEPTED | Specify direct adapter preemption enforcement |
| P1-03 | P1 | ACCEPTED | Add group construction budget validation to IR-001 |
| P1-04 | P1 | ACCEPTED | Clarify workload execution: use ManagedExecutorScenarioRunner |
| P2-01 | P2 | ACCEPTED (deferred) | Remove queue reserve/release from ResourceBudget |
| P2-02 | P2 | ACCEPTED (residual risk) | Accept string-based warnings for v0.15.0 |
| P2-03 | P2 | ACCEPTED | Specify t-distribution approximation in SR |
| P3-01 | P3 | ACCEPTED | Document E2E test time parameters |
| P3-02 | P3 | ACCEPTED (residual risk) | Keep ValidationComparisonReport in-memory only |

## Detailed Disposition

---

### P0-01: Coordinator Input Type Resolution

**Disposition**: ACCEPTED. Coordinator accepts `ScaleAdjustmentCommand` directly.

**Rationale**:
- `ScaleAdjustmentCommand` already carries `targetPoolSize()`, `currentPoolSize()`, `reason()`, `sourceDecisionRef()`, `decisionTimestamp()`, `runId()` — sufficient for all budget arithmetic and traceability
- The coordinator does NOT need `PressureClassification`, `PolicyScore`, or `ThresholdPolicyConfig` for budget decisions (delta calculation, priority comparison, preemption calculation are all numeric)
- `sourceDecisionRef()` links the command back to the `AdjustmentDecision` for audit trail
- No modification to any v0.14.0 type required
- Simpler integration: `CoordinatedAdjustmentAdapter.apply(command)` calls `coordinator.coordinate(command, executor)` directly

**Changes to IR-v0.15-005**:
- Update `GroupCoordinator.coordinate()` signature to accept `ScaleAdjustmentCommand` instead of `AdjustmentDecision`
- Update `GroupCoordinationResult` to reference `ScaleAdjustmentCommand` instead of `AdjustmentDecision`
- Remove open question section; add decision rationale
- `CoordinatedAdjustmentAdapter.apply()` flow simplified:
  1. Call `coordinator.coordinate(command, executor)` → `GroupCoordinationResult`
  2. If not approved → return REJECTED result
  3. If CAPPED → modify command target, delegate
  4. If APPROVED → delegate

**Impact on related IR entries**:
- IR-v0.15-004: `GroupCoordinator.coordinate(ScaleAdjustmentCommand command, ManagedExecutor source)` — parameter change
- IR-v0.15-005: Remove decision reconstruction, simplify to command-based coordination
- IR-v0.15-007: `GroupCoordinationEntry.originalDecision` becomes `command` (ScaleAdjustmentCommand)
- IR-v0.15-008: CrossExecutorOscillationDetector receives command history instead of decision history

---

### P1-01: Safety Gate + Coordination Interaction

**Disposition**: ACCEPTED. Adopt Option A: CoordinatedAdjustmentAdapter bypasses delegate's internal safety gate.

**Rationale**:
- The `AdjustmentLoop` already performs safety gate evaluation at Step 8 (before adapter.apply)
- The coordinator performs resource conflict checks (budget, priority, cross-oscillation)
- Adding a third safety gate check in the delegate adapter is redundant and confusing
- The `CoordinatedAdjustmentAdapter` wraps the adapter; the loop's safety gate check passes, the coordinator passes, and the delegate adapter applies without re-checking
- Implementation: `CoordinatedAdjustmentAdapter` creates a modified `ScaleAdjustmentCommand` with a flag (`skipSafetyGate = true`), or uses a separate adapter that doesn't call safety gate

**Changes to IR-v0.15-005**:
- Add safety gate interaction specification:
  ```
  CoordinatedAdjustmentAdapter.apply(command):
    1. coordinator.coordinate(command, executor) → result
    2. If not approved → return REJECTED
    3. If approved → delegate.apply(command.withSkipSafetyGate())
    // delegate skips its internal safetyGate.evaluate() because
    // AdjustmentLoop already performed safety gate evaluation at Step 8
  ```
- Add risk note: "If delegate adapter's safety gate has tighter constraints than loop's safety gate, bypassing could allow adjustments that should be rejected. This is acceptable because both gates use the same `RuntimeAdjustmentSafetyGate` instance and configuration."

**No change to AdjustmentLoop or ManagedExecutorAdjustmentAdapter**:
- `AdjustmentLoop` continues to call `safetyGate.evaluate()` at Step 8
- `ManagedExecutorAdjustmentAdapter` unchanged — only bypassed when wrapped by `CoordinatedAdjustmentAdapter`

---

### P1-02: Preemption Enforcement Mechanism

**Disposition**: ACCEPTED. Coordinator directly applies preemption through the preempted executor's adapter.

**Rationale**:
- The "PREEMPT signal" metaphor in the IR is replaced with direct adapter invocation
- When coordinator decides to preempt executor-B:
  1. Calculate `newTargetPoolSize = currentAllocatedThreads - preemptionAmount`
  2. Build a `ScaleAdjustmentCommand` for executor-B with `targetPoolSize = newTargetPoolSize`
  3. Call `executorB.adapter.apply(preemptCommand)` directly (the coordinator holds references to all adapters)
  4. Record the preemption in B's `AdjustmentHistory` through B's `LoopEvidenceRecorder`
- B's `AdjustmentLoop` discovers the changed pool size on its next iteration:
  - `adapter.currentState()` → reflects new pool size
  - `DecisionOrchestrator.decide()` → classification based on new state
  - The loop may decide to scale back up, which triggers another coordination cycle
- This is simpler than an asynchronous signal/notification system and ensures preemption is immediate

**Changes to IR-v0.15-004**:
- Remove "PREEMPT signal" concept
- Add: "Coordinator holds `Map<String, ExecutorAdjustmentAdapter>` for all group members for direct preemption enforcement"
- Add: "`GroupCoordinator.applyPreemption(String targetExecutorId, int newTargetPoolSize, String reason)` — directly applies scale-down to a preempted executor"
- Add: "Preempted executor's AdjustmentLoop discovers the state change on its next iteration via `adapter.currentState()`"

**Changes to IR-v0.15-005**:
- `CoordinatedAdjustmentAdapter` constructor: remove `AdjustmentHistory`, `LoopEvidenceRecorder` fields (coordinator accesses these through the group)
- Coordinator receives `Map<String, CoordinatedAdjustmentAdapter>` or `Map<String, ManagedExecutor>` for direct preemption

---

### P1-03: Group Construction Budget Validation

**Disposition**: ACCEPTED. Add validation to ExecutorGroup construction.

**Changes to IR-v0.15-001**:
- Add to `ExecutorGroup` construction validation:
  ```
  Group construction validates:
  1. members not empty
  2. no duplicate executor IDs (use ExecutorRegistry names)
  3. sum(member.getCorePoolSize()) <= config.maxTotalThreads()
     → if violated: IllegalArgumentException with detail
  4. sum(member.getQueueCapacity()) <= config.maxTotalQueueCapacity()
     → if violated AND maxTotalQueueCapacity > 0: IllegalArgumentException with detail
  5. each member has an entry in memberPriorities, or uses defaultPriority
  ```
- Add AC: "ExecutorGroup constructor rejects members whose total corePoolSize exceeds budget" (P0)
- Add AC: "ExecutorGroup constructor rejects members whose total queue capacity exceeds budget" (P1)

---

### P1-04: Validation Runner Workload Execution Clarification

**Disposition**: ACCEPTED. Use `ManagedExecutorScenarioRunner` for all three modes; remove `ComparableScenarioRunner` from constructor.

**Rationale**:
- All three validation modes use a `ManagedExecutor` — `ManagedExecutorScenarioRunner` already knows how to run workloads against ManagedExecutors
- `ComparableScenarioRunner.compare()` runs baseline (non-ManagedExecutor) vs managed — this is the wrong comparison for closed-loop validation
- Closed-loop validation compares managed-vs-managed (three variants of the same executor config)

**Changes to IR-v0.15-009**:
- `ClosedLoopValidationRunner` constructor: `ClosedLoopValidationRunner(Supplier<Instant> clock)` — no `ComparableScenarioRunner` dependency
- Mode A: Create `ManagedExecutorScenarioRunner`, run workload, start `AdjustmentLoop` in parallel
- Mode B: Create `ManagedExecutorScenarioRunner` with policy pre-applied, run workload
- Mode C: Create `ManagedExecutorScenarioRunner` with no policy, run workload
- `ComparableScenarioRunner` is NOT used in v0.15.0 validation

---

### P2-01: ResourceBudget Queue Capacity Tracking

**Disposition**: ACCEPTED with modification. Keep maxTotalQueueCapacity in config for construction-time validation; remove runtime reserve/release for queue.

**Changes to IR-v0.15-002**:
- Remove `reserveQueue()`, `releaseQueue()`, `availableQueueCapacity()` methods
- Keep `maxTotalQueueCapacity` field for construction-time validation only
- Add note: "Queue capacity is immutable at runtime (set at executor construction). Queue budget is validated at group construction and tracked for reporting, but not enforced at runtime."

---

### P2-02: GroupHealth String-Based Warnings

**Disposition**: ACCEPTED as residual risk. String-based warnings adequate for v0.15.0.

**Rationale**: Warnings are informational — consumed by tests (assertion strings) and logs. Structural warning types add API complexity without functional benefit at this stage. If programmatic warning consumption becomes necessary, `GroupWarning` record can be added in a non-breaking way (deprecate `activeWarnings` Strings, add `warnings` List of GroupWarning).

**No changes to IR**. Recorded as residual risk in IR-v0.15-006.

---

### P2-03: t-Distribution CDF Specification

**Disposition**: ACCEPTED. Specification deferred to SR.

**No changes to IR-v0.15-010**. SR will specify the exact approximation algorithm and accuracy targets. The IR requirement "p-value accurate to ±0.01 of reference for n >= 10" is sufficient as an IR-level acceptance condition; algorithmic details belong in SR.

---

### P3-01: E2E Test Time Budget

**Disposition**: ACCEPTED. Document in IR-v0.15-011.

**Changes to IR-v0.15-011**:
- Add test configuration note:
  ```
  Test configuration for reliable CI execution:
  - LoopConfig: samplingIntervalMs = 500ms (shorter than default 2000ms)
  - snapshotWindowSize = 5 (smaller than default 20)
  - maxIterations = 50
  - Test timeout: 60s (allows for CI variance)
  - Expected runtime: ~20-30s for conflict detection scenario
  ```

---

### P3-02: ValidationComparisonReport Serialization

**Disposition**: ACCEPTED as residual risk. In-memory only for v0.15.0.

**Rationale**: The report is consumed by test assertions. If persistence is needed, a `ValidationReportWriter` (analogous to v0.12.0's `ComparisonReportArtifact`) can be added without modifying the report structure.

**No changes to IR**. Recorded as residual risk in IR-v0.15-009.

---

## Updates to 10-ir.md

The following IR sections are updated based on this disposition:

1. **IR-v0.15-001** (ExecutorGroup): Add construction-time budget validation, add 2 new ACs
2. **IR-v0.15-002** (ResourceBudget): Remove queue reserve/release methods
3. **IR-v0.15-004** (GroupCoordinator): Change input type to `ScaleAdjustmentCommand`, add direct preemption via adapter
4. **IR-v0.15-005** (CoordinatedAdjustmentAdapter): Simplify to command-based, add safety gate bypass, remove decision reconstruction
5. **IR-v0.15-006** (GroupLoopOrchestrator): Add P2-02 residual risk note
6. **IR-v0.15-009** (ClosedLoopValidationRunner): Switch to ManagedExecutorScenarioRunner, remove ComparableScenarioRunner
7. **IR-v0.15-010** (Statistics): Note P2-03 deferral to SR
8. **IR-v0.15-011** (E2E): Add test timing configuration
9. **IR-v0.15-012** (Validation E2E): No changes

## Disposition Conclusion

All 10 findings disposed:
- **P0**: 1 accepted and resolved (coordinator input type decided as `ScaleAdjustmentCommand`)
- **P1**: 4 accepted and resolved (safety gate interaction, preemption mechanism, construction validation, workload execution)
- **P2**: 3 accepted (1 deferred modification, 1 residual risk, 1 deferred to SR)
- **P3**: 2 accepted (1 documented, 1 residual risk)

No findings rejected. No findings remain open. Ready for closure verification.
