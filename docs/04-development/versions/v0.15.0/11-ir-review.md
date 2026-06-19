# v0.15.0 IR Review

## Header

- Document type: Independent IR review
- Version: `v0.15.0`
- Review date: `2026-06-17`
- Reviewer: Independent review agent
- Status: `READY_FOR_DISPOSITION` — findings documented, pending disposition
- Reviewed artifact: `docs/04-development/versions/v0.15.0/10-ir.md`

## Review Scope

This review covers the v0.15.0 IR draft (10-ir.md) and its consistency with:
- Version design objectives (`00-objectives-and-scope.md`)
- Decision log (`decision-log.md`)
- Existing component APIs (verified by reading source)
- Governance rules (`managed-change-standard.md`)
- Operational boundaries (`operational-and-evolution-boundaries.md`)

## Finding Summary

| Severity | Count |
|---|---|
| P0 | 1 |
| P1 | 4 |
| P2 | 3 |
| P3 | 2 |
| **Total** | **10** |

---

## P0 Findings

### P0-01: IR-v0.15-005 Coordinator Input Type Unresolved — Blocks SR Entry

**Location**: IR-v0.15-005, "Open question for IR review"

**Finding**: The IR leaves unresolved whether `GroupCoordinator.coordinate()` accepts `AdjustmentDecision` (full diagnosis chain) or `ScaleAdjustmentCommand` (simpler, no reconstruction). This decision has cascading effects on:
- `CoordinatedAdjustmentAdapter` implementation approach (Option A/B/C)
- Whether `ScaleAdjustmentCommand` needs to carry an `AdjustmentDecision` reference (modifying v0.14.0 type)
- The coordinator's ability to make informed decisions (does it need classification/score context?)

**Impact**: SR cannot design the coordinator interface without this decision. The three options listed in the IR (A: store decision in command metadata, B: change adapter interface, C: coordinator accepts command directly) have different implications for v0.14.0 code stability.

**Recommendation**: Resolve at IR stage. Analysis:

- `ScaleAdjustmentCommand` already carries: `targetPoolSize()`, `currentPoolSize()`, `reason()`, `sourceDecisionRef()`, `decisionTimestamp()`, `runId()` — sufficient for budget arithmetic (the coordinator only needs the delta, which is `target - current`)
- `AdjustmentDecision` carries additional context: `classification` (PressureClassification), `selectedScore` (PolicyScore), `selectedPolicy` (ThresholdPolicyConfig) — valuable for logging/rationale but not needed for budget decisions
- **Recommendation**: Coordinator accepts `ScaleAdjustmentCommand` directly (Option C). Rationale:
  1. Budget coordination (delta check, priority comparison, preemption calculation) only needs pool size numbers — not pressure classification or policy scores
  2. `sourceDecisionRef()` already links the command back to the decision for traceability
  3. Does NOT require modifying any v0.14.0 type (no carrying `AdjustmentDecision` in command)
  4. `CoordinatedAdjustmentAdapter.apply(command)` already has the command — no reconstruction needed
  5. If future coordination needs classification context (e.g., "preempt only if requestor is in OVERLOAD"), that can be added as a separate parameter without changing the core interface
- The `AdjustmentDecision.rationale()` carries the human-readable reason — this is informational and not required for coordination logic.

**Required action**: Decide coordinator input type and update IR-v0.15-005 to remove the open question.

---

## P1 Findings

### P1-01: CoordinatedAdjustmentAdapter and SafetyGate Double-Gating Not Addressed

**Location**: IR-v0.15-005, existing code in `AdjustmentLoop.runLoop()` Steps 8-10 and `ManagedExecutorAdjustmentAdapter.apply()`

**Finding**: The current `AdjustmentLoop` calls `safetyGate.evaluate()` at Step 8 before `adapter.apply()` at Step 10. `ManagedExecutorAdjustmentAdapter.apply()` also calls `safetyGate.evaluate()` internally. This means the safety gate is already invoked twice (loop pre-check + adapter enforcement). With `CoordinatedAdjustmentAdapter` inserted between them, the coordination step adds a third gate-like check.

The IR does not address whether this triple-checking (safetyGate → coordinator → safetyGate) is intentional or whether one of the existing checks should be bypassed when coordination is active.

**Impact**: Potential for redundant safety gate evaluations, confusing failure attribution (which layer rejected the adjustment?), and inconsistent cooldown management (both loop and adapter call `safetyGate.recordApplied()`).

**Recommendation**: SR must clarify the safety gate + coordination interaction:
- Option A: Loop safety gate check runs first; if ALLOW, coordinator runs; if coordinator approves, adapter's internal safety gate check is bypassed (adapter recognizes it was already checked)
- Option B: Loop safety gate check runs first; if ALLOW, coordinator runs; if coordinator approves, adapter runs its safety gate check again (triple-check, safest but redundant)
- Option C: Loop safety gate check is bypassed when coordination is active; coordinator runs; adapter runs its safety gate (double-check: coordination + adapter)

Recommend Option A — the loop's safety gate check is the pre-filter, coordinator handles resource conflicts, and the adapter trusts the coordinator (skips re-check). This requires `CoordinatedAdjustmentAdapter` to suppress the delegate adapter's internal safety gate call, or use a different adapter that doesn't call safety gate.

**Required action**: Add IR entry or risk item documenting safety gate + coordination interaction. SR must resolve.

### P1-02: Preemption Communication Mechanism Underspecified

**Location**: IR-v0.15-004 (GroupCoordinator), IR-v0.15-006 (GroupLoopOrchestrator)

**Finding**: When coordinator preempts executor-B (reduces its pool size to free budget for executor-A), the IR mentions "preemption is communicated to B's loop via `PREEMPT` signal in the coordination result" but does not define:
1. What exactly is a `PREEMPT` signal? Is it a method call, an event, a field in GroupCoordinationResult?
2. How does executor-B's loop receive this signal? Its `AdjustmentLoop` is in a `while(sleep)` loop — it's not polling for preemption events.
3. Does the coordinator directly call `executor.setCorePoolSize()` to enforce preemption, or does it wait for B's loop to adjust voluntarily?
4. If the coordinator directly modifies executor-B's state (option 3), how does B's loop reconcile its view of executor state on the next iteration?

**Impact**: Without a clear preemption mechanism, the priority-based allocation model is incomplete. The coordinator can decide to preempt but cannot enforce the decision.

**Recommendation**: Specify the preemption enforcement path:
- **Recommended approach**: Coordinator directly applies preemption through B's `ExecutorAdjustmentAdapter` (the coordinator holds references to all adapters). B's loop discovers the changed pool size on its next iteration via `adapter.currentState()` and adjusts its decision accordingly. The coordinator records the preemption in B's `AdjustmentHistory` through B's `LoopEvidenceRecorder`.
- The "PREEMPT signal" concept should be replaced with "direct adapter call" — simpler, immediate, and auditable.
- B's loop does not need explicit notification; it naturally observes the changed state on its next `classify → score → decide` cycle.

**Required action**: Add specific IR entry for preemption enforcement mechanism, or clarify in IR-v0.15-004/005.

### P1-03: Group Construction Validation — Initial Budget Exceeded Not Addressed

**Location**: IR-v0.15-001, `ExecutorGroupConfig`, `ResourceBudget`

**Finding**: `ExecutorGroup` constructor takes a `List<ManagedExecutor> members`. Each member already has a `maxPoolSize` and `corePoolSize` set at construction time. If `sum(member.maxPoolSize) > maxTotalThreads`, the group starts with its budget already violated. The IR does not specify validation at group construction time.

**Impact**: A group constructed with budget-violating executors can never reach a stable state — the coordinator will perpetually reject scale-ups, and the budget invariant is violated before the first coordination cycle.

**Recommendation**: `ExecutorGroup` constructor must validate:
1. `sum(member.corePoolSize) <= maxTotalThreads` — initial allocation must fit within budget
2. `sum(member.getQueueCapacity()) <= maxTotalQueueCapacity` — queue budget (if queue capacity > 0)
3. If validation fails → `IllegalArgumentException` with details: which executors exceed, by how much

This is a construction-time invariant, not a runtime coordination concern.

**Required action**: Add validation requirements to IR-v0.15-001.

### P1-04: ClosedLoopValidationRunner Workload Execution Method Underspecified

**Location**: IR-v0.15-009

**Finding**: The IR states "Run workload via workload executor (not ComparableScenarioRunner — this is a loop scenario, not a baseline comparison)" but does not specify WHICH workload executor to use. The `ComparableScenarioRunner` uses `BaselineWorkloadExecutor` for baseline and `ManagedExecutorScenarioRunner` for managed — but neither is designed for closed-loop mode. The ClosedLoopValidationRunner needs to:
1. Run a workload that generates enough pressure for the closed-loop to react
2. Record snapshots for later metric computation
3. Not interfere with the closed-loop's own sampling

The IR also states that `ComparableScenarioRunner` is injected into `ClosedLoopValidationRunner` but then says "not ComparableScenarioRunner" for the closed-loop mode. There's a contradiction.

**Impact**: Without a clear workload execution strategy, the validation runner cannot be implemented.

**Recommendation**: The validation runner should use `ManagedExecutorScenarioRunner` (v0.8.0) for all three modes, since all three use a `ManagedExecutor`:
- Mode A (closed-loop): `ManagedExecutorScenarioRunner` runs workload + `AdjustmentLoop` runs independently
- Mode B (static): `ManagedExecutorScenarioRunner` runs workload + policy applied once at start
- Mode C (baseline): `ManagedExecutorScenarioRunner` runs workload + no policy

The `ComparableScenarioRunner` is NOT used because it runs baseline (non-ManagedExecutor) vs managed comparison — v0.15.0 validation compares managed vs managed (three variants of the same executor type).

Update IR: Remove `ComparableScenarioRunner` from `ClosedLoopValidationRunner` constructor; use `ManagedExecutorScenarioRunner` instead.

**Required action**: Clarify workload execution method in IR-v0.15-009.

---

## P2 Findings

### P2-01: ResourceBudget Queue Capacity Tracking Inconsistency

**Location**: IR-v0.15-002

**Finding**: `ResourceBudget` tracks `maxTotalQueueCapacity` with the note "0 = unbounded queue, no enforcement." However, `ManagedExecutor` uses `LinkedBlockingQueue` with a capacity set at construction time. The executor's queue capacity is a fixed property — it cannot change at runtime (unlike pool size, which can via `setCorePoolSize`/`setMaximumPoolSize`). Tracking queue capacity in the budget makes sense for group-level reporting, but queue capacity enforcement is a one-time check at group construction, not a runtime coordination concern.

**Impact**: Low — queue budget tracking doesn't block coordination. But having unused queue budget methods (`reserveQueue`, `releaseQueue`, `availableQueueCapacity`) adds API surface with no runtime purpose if queue capacity cannot change.

**Recommendation**: Keep `maxTotalQueueCapacity` in config and validation at group construction time, but remove queue-specific reserve/release methods from `ResourceBudget`. If future versions add dynamic queue resizing (v0.9.0 already has `QueueResizeCommand`), queue budget tracking can be added then.

### P2-02: GroupHealth.activeWarnings String-Based — Structural Weakness

**Location**: IR-v0.15-006

**Finding**: `GroupHealth.activeWarnings` is `List<String>`. Warnings like "budget >= 90% utilized" and "cross-executor oscillation suspected" are unstructured strings. Consumers (tests, monitoring) must parse strings to extract structured information.

**Impact**: Tests will use `assertTrue(warnings.stream().anyMatch(s -> s.contains("budget")))` — fragile string matching. Adding new warning types requires no type changes, but extracting warning details (e.g., WHICH executors are oscillating) requires parsing.

**Recommendation**: Either:
- A: Accept string-based warnings as adequate for v0.15.0 (warnings are informational, not API)
- B: Define `GroupWarning` record with `warningType` enum and `details` map

Recommend A for v0.15.0 — warnings are for human consumption in tests and logs. Structural warnings can be added in a later version if needed.

### P2-03: StatisticalSignificanceCalculator t-Distribution CDF Approximation Not Specified

**Location**: IR-v0.15-010

**Finding**: The IR mentions "Abramowitz and Stegun approximation or Student's t table lookup" for the t-distribution CDF but doesn't specify which approximation will be used or what accuracy is required for specific degrees of freedom.

**Impact**: The SR designer will need to choose and implement an approximation. This is a self-contained algorithmic task but needs clear requirements.

**Recommendation**: Specify in SR:
- Use Abramowitz and Stegun 26.7.10 approximation for the standard normal CDF, combined with the t-to-z transformation
- Target accuracy: p-value within 0.005 of reference (Apache Commons Math or R) for df >= 5
- Test with known reference values at df = [5, 10, 30, 100] and t = [-3, -2, -1, 0, 1, 2, 3]

---

## P3 Findings

### P3-01: IR-v0.15-011 E2E Test Time Budget Unstated

**Location**: IR-v0.15-011, "Run until: at least 1 resource conflict resolved by priority OR 30s timeout"

**Finding**: The E2E test has a 30s timeout but no expected runtime. The `AdjustmentLoop.samplingIntervalMs` defaults to 2000ms, meaning 30s allows ~15 iterations. With snapshots needing to accumulate first, actual adjustment cycles may start around iteration 5-7, leaving only ~8 cycles for coordination to occur. If the workload is queue-building, pressure classification may take additional cycles.

**Recommendation**: Document the expected test duration. For reliable E2E results, consider:
- Shortened sampling interval (500ms) for tests via `LoopConfig` override
- Explicitly wait for first adjustment before counting coordination cycles
- Test timeout of 60s to allow for CI variance

### P3-02: ValidationComparisonReport Serialization Not Addressed

**Location**: IR-v0.15-009

**Finding**: `ValidationComparisonReport` contains records with `List<MetricComparison>` and `List<StatisticalSignificance>`. v0.12.0's `ComparisonReportArtifact` supports JSON serialization. The IR does not state whether `ValidationComparisonReport` should support serialization or remain in-memory only.

**Recommendation**: For v0.15.0, in-memory only is sufficient. The report is consumed by tests and assertions, not persisted. If persistence is needed later, a `ValidationReportWriter` can be added (similar to v0.12.0's `ComparisonReportArtifact`).

---

## Cross-Cutting Observations

### O1: Decorator Pattern Consistency

The IR correctly identifies `CoordinatedAdjustmentAdapter` as the integration point, following the established pattern from v0.9.0 (`QueueResizeAdjustmentAdapter`) and v0.10.0 (`RejectionPolicyAdjustmentAdapter`). This is consistent with the project's adapter-chain architecture. Good.

### O2: Coordination Centralization

The centralized `GroupCoordinator` is the right choice for single-JVM multi-executor coordination. However, the coordinator becomes a single point of serialization for all adjustments. The IR mentions the non-blocking fast path for scale-down decisions but doesn't address the worst-case: what happens if coordinator is blocked (e.g., by a slow oscillation check) while multiple loops wait? The `coordinationTimeoutMs` in config should be enforced — if `coordinate()` takes longer than the timeout, the caller should receive a `REJECTED` (if `failOpen = false`) or proceed uncoordinated (if `failOpen = true`).

### O3: Scope Alignment

The 12 IR entries align well with the version objectives and decision log. Notable scope exclusions are correctly documented. The DFR list in the decision log is consistent with the out-of-scope declarations in the IR.

### O4: Missing IR — Executor Departure/Shutdown

When an executor in a group is shut down (`executor.shutdown()` or `executor.close()`), the group should detect this and:
- Release the executor's budget allocation back to the pool
- Stop the executor's `AdjustmentLoop`
- Update group health

This is not covered by any IR entry. For v0.15.0, this can be a P2 gap — executor departure during active coordination is a corner case. The group construction validation (P1-03) ensures the group starts in a valid state; runtime departure handling can be deferred to v0.16.0.

---

## Review Conclusion

The v0.15.0 IR draft is comprehensive and well-structured. The 12 IR entries cover the required capabilities with clear acceptance conditions. The primary blockers are:

1. **P0-01**: Unresolved coordinator input type (AdjustmentDecision vs ScaleAdjustmentCommand) — must be decided before SR
2. **P1-01**: Safety gate + coordination interaction undefined — SR must design the triple-check flow
3. **P1-02**: Preemption enforcement mechanism underspecified — how coordinator physically reduces preempted executor's pool size
4. **P1-03**: Missing group construction budget validation
5. **P1-04**: Validation runner workload execution method contradictory

All P0 and P1 findings must be resolved before SR entry. P2 findings are non-blocking with recorded rationale. P3 findings are advisory.
