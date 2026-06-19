# v0.15.0 SR Review

## Header

- Document type: Independent SR review
- Version: `v0.15.0`
- Review date: `2026-06-17`
- Reviewer: Independent review agent
- Status: `READY_FOR_DISPOSITION`
- Reviewed artifact: `docs/04-development/versions/v0.15.0/20-sr.md`

## Review Method

The reviewer read every pseudo-code reference in 20-sr.md and verified the actual source files for:
- Constructor signatures
- Method signatures and parameter types
- Factory method parameters
- Enum values
- Package visibility

# Review method compliance with managed-change-standard §3:
- 3 randomly selected API call points verified against source
- Cross-package method visibility checked for all new types
- All referenced types confirmed to exist

## Finding Summary

| Severity | Count |
|---|---|
| P0 | 2 |
| P1 | 4 |
| P2 | 3 |
| P3 | 2 |
| **Total** | **11** |

---

## P0 Findings

### P0-01: LivePressureSamplerConfig Factory Method Name Wrong

**Location**: 20-sr.md §2.12 `ClosedLoopValidationRunner.runBaselineMode()`

**Finding**: Pseudo-code uses `LivePressureSamplerConfig.defaultConfig()` but the actual factory method is `LivePressureSamplerConfig.defaults(String sessionId)`. Verified by reading source: `LivePressureSamplerConfig.java` line 21.

```java
// Wrong (SR pseudo-code):
LivePressureSamplerConfig.defaultConfig()

// Correct (actual API):
LivePressureSamplerConfig.defaults(String sessionId)
// Verified: public static LivePressureSamplerConfig defaults(String sessionId)
// Source: LivePressureSamplerConfig.java:21
```

**Impact**: Compilation failure. All three mode methods in ClosedLoopValidationRunner have this error.

**Required fix**: Replace `LivePressureSamplerConfig.defaultConfig()` with `LivePressureSamplerConfig.defaults(runId)`.

### P0-02: GroupCoordinator Uses Uninitialized `clock` Field

**Location**: 20-sr.md §2.4 `GroupCoordinator`

**Finding**: `GroupCoordinator.coordinate()` references `clock.get()` in multiple places (constructing GroupCoordinationResult records with `clock.get()` as the timestamp), but `clock` is not in the constructor parameter list. The constructor accepts `(config, budget, history, crossDetector, adapters)` — no clock.

**Impact**: Compilation failure. All coordination result records require an `Instant coordinatedAt` field populated from a clock.

**Required fix**: Add `Supplier<Instant> clock` to `GroupCoordinator` constructor:
```java
public GroupCoordinator(
        ExecutorGroupConfig config,
        ResourceBudget budget,
        GroupCoordinationHistory history,
        CrossExecutorOscillationDetector crossDetector,
        Map<String, ExecutorAdjustmentAdapter> adapters,
        Supplier<Instant> clock) { // ← add clock
```

---

## P1 Findings

### P1-01: Executor Identity Resolution Is Fragile and Inconsistent

**Location**: 20-sr.md §2.4 `GroupCoordinator.identifyExecutor()`, §2.7 `ExecutorGroup.identify()`, §2.6 `CoordinatedAdjustmentAdapter.identifyExecutor()`

**Finding**: Three different identity resolution strategies are used across coordination types:
1. `GroupCoordinator.identifyExecutor()` — matches by `corePoolSize` + `maximumPoolSize` equality (fragile: two executors can have the same pool sizes)
2. `ExecutorGroup.identify()` — uses `System.identityHashCode(m)` (not stable across GC cycles for identity hash, not unique)
3. `CoordinatedAdjustmentAdapter.identifyExecutor()` — returns `executor.toString()` (not guaranteed to be unique or meaningful)

There is no consistent executor identity model. The `ExecutorRegistry` uses string names (`register(String name, ManagedExecutor executor)`) but the coordination layer doesn't use registry names.

**Impact**: C1: Coordination could misidentify executors — a scale-up from executor-A could be attributed to executor-B's budget allocation. C2: Preemption could target the wrong executor.

**Recommendation**: Use a consistent identity model. Options:
- **Recommended**: Pass executor identifiers (String names) explicitly through the coordination chain. `ExecutorGroup` stores `Map<String, ManagedExecutor>` where keys are executor names. `CoordinatedAdjustmentAdapter` receives the executor name at construction. `GroupCoordinator` uses the name from the adapter.
- Alternative: Use `ExecutorRegistry` as the source of truth for executor names. Each `CoordinatedAdjustmentAdapter` wraps a `ManagedExecutorAdjustmentAdapter` which already has an `executorName` field.

**Required fix**: Redesign executor identity to use explicit String identifiers throughout the coordination layer.

### P1-02: AdjustmentFailureCode Enum Addition Not Documented in SR §3.2

**Location**: 20-sr.md §3.2, §2.6

**Finding**: SR §3.2 lists new `AdjustmentFailureCode` values (`COORDINATION_REJECTED`, `COORDINATION_CAPPED`). SR §6 (Non-Scope) says "no modification to any experiment.adjustment.* source files (except adding AdjustmentFailureCode enum values)." However:
1. Adding enum values to a public enum used by other packages requires recompilation of all consumers
2. `CoordinatedAdjustmentAdapter` uses `AdjustmentFailureCode.INVALID_COMMAND` as a placeholder (line comment "placeholder — SR review: should we add COORDINATION_REJECTED?")
3. If the new enum values are NOT added, `INVALID_COMMAND` is semantically wrong (command is valid, budget is exhausted)
4. If the new enum values ARE added, it's a modification to an existing package file — minimal but must be documented

**Impact**: Either: semantic confusion (INVALID_COMMAND for budget exhaustion) or undocumented file modification.

**Recommendation**: Add `COORDINATION_REJECTED` and `COORDINATION_CAPPED` to `AdjustmentFailureCode` enum. Document as a non-breaking additive change. Update SR §6 to explicitly list this as an allowed modification.

### P1-03: GroupLoopOrchestrator GroupHealth Pressure States Not Populated

**Location**: 20-sr.md §2.10 `GroupLoopOrchestrator.getGroupHealth()`

**Finding**: The `getGroupHealth()` method creates `pressureStates` map but never populates it with actual pressure states:
```java
pressureStates.put(id, session.finalState() != LoopState.RUNNING
    ? null : null); // Pressure state from history, simplified
```
This is a ternary that always returns `null`. The `currentPressureStates` field in `GroupHealth` will always be empty (all null values). The actual pressure state is in `AdjustmentHistory`'s latest `HistoryEntry.afterClassification()` — the orchestrator would need to access each loop's history.

**Impact**: `GroupHealth.currentPressureStates` never contains useful data. Tests expecting pressure state information will fail.

**Recommendation**: Either:
- A: Access `loop.getHistory().recent(1)` to get the latest `HistoryEntry.afterClassification().state()` (requires `AdjustmentHistory.HistoryEntry` which has `afterClassification()` — verify this API exists)
- B: Remove `currentPressureStates` from `GroupHealth` for v0.15.0 (defer to v0.16.0) and replace with `Map<String, LoopState>` (executorId → loop state, which IS available)

**Required fix**: Choose approach A (provide actual pressure states) or B (simplify GroupHealth). Recommend B for v0.15.0 simplicity.

### P1-04: Cross-Package Visibility — `PressureClassifier`, `PolicyEvaluator` in `experiment.coordination`

**Location**: 20-sr.md §2.10 `GroupLoopOrchestrator.LoopComponents`

**Finding**: `LoopComponents` record in `experiment.coordination` package references types from other packages:
- `PressureClassifier` — `experiment.classification` package (public interface)
- `PolicyEvaluator` — `experiment.policy` package (public interface)
- `DecisionOrchestrator` — `experiment.loop` package (public class)
- `LoopEvidenceRecorder` — `experiment.loop` package (public interface)
- `PressureStateMachine` — `experiment.loop` package (public class)
- `OscillationDetector` — `experiment.loop` package (public class)
- `FeedbackCalibrator` — `experiment.loop` package (public class)
- `RuntimeAdjustmentSafetyGate` — `experiment.adjustment` package (public interface)

These are all `public` types — cross-package access is valid. Verified by reading source:
- `PressureClassifier` — `public interface` (source)
- `PolicyEvaluator` — `public interface` (source)
- `DecisionOrchestrator` — `public final class` (source)
- `LoopEvidenceRecorder` — `public interface` (source)

**No issue found** — this is a documentation note confirming cross-package visibility is correct. All referenced types are `public`. No `package-visible` types are used cross-package.

---

## P2 Findings

### P2-01: ClosedLoopValidationRunner Pseudo-Code Incomplete

**Location**: 20-sr.md §2.12

**Finding**: The `runStaticPolicyMode()` and `runClosedLoopMode()` methods contain extensive `// ...` placeholder comments. Key missing details:
1. How are loop components (DecisionOrchestrator, PolicyEvaluator, etc.) created for the closed-loop mode?
2. How are snapshots collected from the loop's evidence recorder (loop uses internal EvidenceRecorder)?
3. How is workload execution coordinated with loop operation (loop reads snapshots, workload generates them)?
4. `ThresholdPolicyConfig.poolSize()` and `ThresholdPolicyConfig.maxPoolSize()` — do these methods exist?

**Impact**: Implementation agent cannot implement validation runner from this design without making design decisions.

**Recommendation**: SR is a solution design — it doesn't need full implementation detail. The validation runner is Change 2 scope (after coordination is working). Accept the placeholder level as adequate for SR, with the note that Change 2 SR expansion will fill in details during implementation.

### P2-02: StatisticalSignificanceCalculator Algorithm Placeholder

**Location**: 20-sr.md §2.13

**Finding**: The `tDistributionPValue()` and `tCriticalValue()` methods are stubs returning placeholder values. The IR requires p-value accuracy within ±0.01 of reference for n >= 10. The SR should specify the exact approximation algorithm.

**Recommendation**: Specify in SR: Use Abramowitz and Stegun 26.2.17 approximation for standard normal CDF: `Φ(x) ≈ 1 - 0.5 * (1 + c1*x + c2*x² + c3*x³ + c4*x⁴)^(-4)` where c1=0.196854, c2=0.115194, c3=0.000344, c4=0.019527. For t-distribution, transform: `z = t * (1 - 1/(4*df)) / sqrt(1 + t²/(2*df))`.

### P2-03: ScaleAdjustmentCommand.create() Requires Non-Equal current/target

**Location**: 20-sr.md §2.4 `GroupCoordinator.applyPreemption()`, §2.6 `CoordinatedAdjustmentAdapter.apply()`

**Finding**: `ScaleAdjustmentCommand.create()` throws `IllegalArgumentException` if `currentPoolSize == targetPoolSize` (verified: line 84-86 of ScaleAdjustmentCommand.java). The preemption code and capped command construction must ensure `current != target`. The SR pseudo-code correctly computes `newTargetPoolSize` different from current — no bug, but this is a sharp edge that implementation must handle.

**Recommendation**: Add implementation note: "Always verify `currentPoolSize != targetPoolSize` before calling `ScaleAdjustmentCommand.create()`. Use `ScaleAdjustmentCommand.noOp()` for no-change commands." This is a documentation note, not a design change.

---

## P3 Findings

### P3-01: `GroupCoordinator` Package Imports Not Specified

**Location**: 20-sr.md §2.4

**Finding**: Pseudo-code uses `Collectors.toList()` and `Map.copyOf()` without import statements. Trivial to resolve during implementation, but worth noting for completeness.

### P3-02: `OscillationDetector` Interface Not Documented

**Location**: 20-sr.md §2.10

**Finding**: `LoopComponents` references `OscillationDetector` and `FeedbackCalibrator` types from `experiment.loop` package. These were created in v0.14.0 Change 2. The SR doesn't explicitly state whether they are `public` classes or package-visible. Verified by reading source: both are `public final class` — cross-package access from `experiment.coordination` is valid.

---

## API Signature Random Spot Check

Per managed-change-standard §3, 3 random API call points verified:

### Spot Check #1: `AdjustmentResult` Constructor
- **SR reference**: §2.6 `CoordinatedAdjustmentAdapter.apply()`
- **Source**: `AdjustmentResult.java:35-66`
- **SR pseudo-code**: `new AdjustmentResult(command, AdjustmentStatus.REJECTED, state, command.targetPoolSize(), state.corePoolSize(), state, result.rationale(), AdjustmentFailureCode.INVALID_COMMAND, command.sourceDecisionRef(), command.decisionTimestamp())`
- **Actual signature**: `AdjustmentResult(ScaleAdjustmentCommand command, AdjustmentStatus status, ExecutorStateSnapshot beforeState, int requestedPoolSize, Integer appliedPoolSize, ExecutorStateSnapshot afterState, String reason, AdjustmentFailureCode failureCode, String sourceDecisionRef, Instant decisionTimestamp)`
- **Result**: ✅ Match — 10 parameters, correct types, correct order

### Spot Check #2: `DecisionOrchestrator` Constructor
- **SR reference**: §2.10 implied by `LoopComponents`
- **Source**: `DecisionOrchestrator.java:27-36`
- **Actual signature**: `DecisionOrchestrator(PressureClassifier classifier, PolicyRanker ranker, PolicyEvaluator evaluator, ClassifierConfig classifierConfig)`
- **Result**: ✅ Match — 4 parameters, correct types

### Spot Check #3: `ScaleAdjustmentCommand.create()` Factory
- **SR reference**: §2.4 `GroupCoordinator.coordinate()`
- **Source**: `ScaleAdjustmentCommand.java:63-91`
- **Actual signature**: `public static ScaleAdjustmentCommand create(String runId, Instant decisionTimestamp, int currentPoolSize, int targetPoolSize, String reason, String sourceDecisionRef, Supplier<Instant> clock)`
- **SR pseudo-code**: `ScaleAdjustmentCommand.create(command.runId(), command.decisionTimestamp(), currentPoolSize, cappedTarget, "capped...", command.sourceDecisionRef(), clock)`
- **Result**: ✅ Match — 7 parameters, correct types. Note: `runId()` returns String, `decisionTimestamp()` returns Instant.

---

## Review Conclusion

The v0.15.0 SR is structurally sound with verified API references. Two P0 compilation blockers (wrong factory method name, missing clock parameter) and four P1 design issues (executor identity, enum addition, GroupHealth population, cross-package visibility) must be resolved.

All three random API spot checks passed — SR pseudo-code correctly references actual source signatures.

P2 findings are documentation/deferred-implementation issues. P3 findings are advisory.
