# Technical Design

## Context

The v0.15.0 closed-loop control system validates only under simple workload profiles (`STEADY`, `RAMP`, `BURST`). Production-like conditions — bursty arrivals, long-tail latency, mixed CPU/IO, and downstream blocking — expose gaps in stability verification and failure recovery. There is no mechanism to undo a harmful adjustment, cooldown uses a counter rather than injectable wall-clock time, anti-oscillation is advisory-only across executors, and the statistical significance computation generates synthetic proxy arrays via `mean + Math.random() * noise`. v0.16.0 closes these gaps.

The codebase has ~150 main source files and ~150 test files across 15 packages under `com.zhiwu.dynamicthreadpollermanager.experiment`. Key types: `AdjustmentLoop` (15-step cycle), `ExecutorAdjustmentAdapter` (interface), `RuntimeAdjustmentSafetyGate` (interface), `OscillationDetector`, `ScenarioProfile` (enum with 3 values), `DeterministicScenarioPlanner`, `EvidenceRecorder` / `LoopEvidenceRecorder` (interfaces), `GroupLoopOrchestrator`, `CoordinatedAdjustmentAdapter`.

## Goals

1. Four deterministic complex workload scenario profiles (`BURST`, `LONG_TAIL`, `MIXED_CPU_IO`, `DOWNSTREAM_BLOCKED`) with seed-based reproducibility.
2. Verifiable rollback semantics: capture pre-adjustment state, detect post-adjustment degradation, restore safe state through safety gate, bounded to 1 rollback per decision.
3. Time-based cooldown with injectable `Supplier<Instant>` clock; emergency rollback bypasses cooldown.
4. Blocking anti-oscillation guard that prevents non-emergency adjustments when sustained oscillation is detected.
5. Complex scenario validation report with real-observation-based metrics (p95/p99 latency, rejection count, queue depth delta, throughput delta, per-decision observation windows).
6. Fix v0.15 residual risks: replace proxy arrays with real paired observations, document thread-safety contracts, fix null `ExecutorRegistry`, add independent behavior tests for `CoordinatedAdjustmentAdapter` and `GroupLoopOrchestrator`.

## Non-Goals

- Cross-JVM or distributed coordination.
- New external dependencies (Java 21, Maven/JUnit5 only).
- Frontend, database, message queue, auth, or monitoring integration.
- Multi-dimensional simultaneous adjustment.
- Policy auto-generation.
- Changes to `pom.xml` dependencies or Java version.

## Existing API Verification

| Existing type/API | Source path read | Verified signature or behavior |
|---|---|---|
| `ScenarioProfile` | `src/main/java/.../scenario/ScenarioProfile.java` | `public enum ScenarioProfile { STEADY, RAMP, BURST }` — 3 values, line 8 |
| `DeterministicScenarioPlanner` | `src/main/java/.../scenario/DeterministicScenarioPlanner.java` | `public ScenarioPlan plan(ScenarioDefinition definition)` — line 24; `workUnitsFor(profile, baseWorkUnits, index)` — line 34, switch on 3 profiles |
| `ScenarioDefinition` | `src/main/java/.../scenario/ScenarioDefinition.java` | Immutable class with fields: `scenarioId`, `profile`, `seed`, `stepCount`, `baseWorkUnits`, `description` — constructor line 17 |
| `ScenarioStep` | `src/main/java/.../scenario/ScenarioStep.java` | `public ScenarioStep(int index, int workUnits, long plannedDelayMillis)` — line 17 |
| `ScenarioPlan` | `src/main/java/.../scenario/ScenarioPlan.java` | `public ScenarioPlan(String scenarioId, List<ScenarioStep> steps)` — line 18 |
| `ExecutorAdjustmentAdapter` | `src/main/java/.../adjustment/ExecutorAdjustmentAdapter.java` | `ExecutorStateSnapshot currentState()` — line 18; `AdjustmentResult apply(ScaleAdjustmentCommand command)` — line 26 |
| `ExecutorStateSnapshot` | `src/main/java/.../adjustment/ExecutorStateSnapshot.java` | Builder pattern; fields: `observedAt`, `corePoolSize`, `maximumPoolSize`, `activeCount`, `poolSize`, `queueSize`, `queueCapacity`, `completedTaskCount`, `keepAliveTimeSeconds`, `largestPoolSize`, `taskCount` |
| `AdjustmentResult` | `src/main/java/.../adjustment/AdjustmentResult.java` | Fields: `command`, `status`, `beforeState`, `requestedPoolSize`, `appliedPoolSize`, `afterState`, `reason`, `failureCode`, `sourceDecisionRef`, `decisionTimestamp` — constructor line 35 |
| `ScaleAdjustmentCommand` | `src/main/java/.../adjustment/ScaleAdjustmentCommand.java` | Package-private constructor; public factory `create(runId, decisionTimestamp, currentPoolSize, targetPoolSize, reason, sourceDecisionRef, clock)` — line 63; `noOp(...)` — line 98 |
| `RuntimeAdjustmentSafetyGate` | `src/main/java/.../adjustment/RuntimeAdjustmentSafetyGate.java` | `SafetyGateDecision evaluate(ScaleAdjustmentCommand command, ExecutorStateSnapshot currentState, ReadinessAssessment readiness)` — line 17; `void recordApplied(SafetyGateDecision decision)` — line 47 |
| `DefaultRuntimeAdjustmentSafetyGate` | `src/main/java/.../adjustment/DefaultRuntimeAdjustmentSafetyGate.java` | Counter-based cooldown via `cooldownRemaining` (int); `synchronized evaluate()` — line 47; `synchronized recordApplied()` — line 106; NO injectable clock |
| `SafetyGateDecision` | `src/main/java/.../adjustment/SafetyGateDecision.java` | `enum Outcome { ALLOW, REJECTED, NO_OP }` — line 13; factories: `allow()`, `noOp()`, `rejected(code, reason)` — lines 47-57 |
| `AdjustmentFailureCode` | `src/main/java/.../adjustment/AdjustmentFailureCode.java` | Values: `NOT_READY`, `RISK_NOT_ACCEPTED`, `COOLDOWN_ACTIVE`, `OPPOSITE_DIRECTION`, `RUN_LIMIT_EXCEEDED`, `INVALID_COMMAND`, `PROBE_FAILURE`, `UNSUPPORTED`, `EXECUTOR_NOT_FOUND`, `COORDINATION_REJECTED`, `COORDINATION_CAPPED` — 11 values |
| `OscillationDetector` | `src/main/java/.../loop/OscillationDetector.java` | `boolean wouldOscillate(AdjustmentDecision pending, AdjustmentHistory history)` — line 45; detects ping-pong, over-adjustment, policy switching |
| `AdjustmentLoop` | `src/main/java/.../loop/AdjustmentLoop.java` | Constructor takes 14 parameters including `adapter`, `safetyGate`, `oscillationDetector`, `clock` — line 54; `start(ManagedExecutor)` returns `LoopSession` — line 87 |
| `LoopEvidenceRecorder` | `src/main/java/.../loop/LoopEvidenceRecorder.java` | `void recordIteration(session, iterationIndex, decision, result, beforeClassification)` — line 14; `void recordSessionStart/End(session)` — lines 18,20; `List<LoopIterationEvidence> getIterationEvidence(sessionId)` — line 22 |
| `EvidenceRecorder` | `src/main/java/.../metrics/EvidenceRecorder.java` | `void record(ObservedSnapshot snapshot)` — line 13; `List<ObservedSnapshot> snapshots(String runId)` — line 15; `Set<String> runIds()` — line 17 |
| `InMemoryEvidenceRecorder` | `src/main/java/.../metrics/InMemoryEvidenceRecorder.java` | Uses `ConcurrentHashMap<String, CopyOnWriteArrayList>` — thread-safe; `snapshots()` returns `List.copyOf()` |
| `FileBackedEvidenceRecorder` | `src/main/java/.../acquisition/FileBackedEvidenceRecorder.java` | Constructor `(Path outputRoot, String versionTag)` — line 38; writes JSONL immediately; uses `ConcurrentHashMap` buffer |
| `GroupLoopOrchestrator` | `src/main/java/.../coordination/GroupLoopOrchestrator.java` | `startAll(Map<String, LoopComponents>)` returns `Map<String, LoopSession>` — line 67; creates `ExecutorRegistry(null)` on line 70 (BUG); inner record `LoopComponents` — line 51 |
| `CoordinatedAdjustmentAdapter` | `src/main/java/.../coordination/CoordinatedAdjustmentAdapter.java` | `CoordinatedAdjustmentAdapter(delegate, coordinator, executorName, clock)` — line 26; `apply()` calls `coordinator.coordinate()` then delegates — line 46 |
| `GroupCoordinator` | `src/main/java/.../coordination/GroupCoordinator.java` | `GroupCoordinationResult coordinate(ScaleAdjustmentCommand command, String executorName)` — line 48 |
| `GroupCoordinationResult` | `src/main/java/.../coordination/GroupCoordinationResult.java` | Record with `command`, `approvedCommand`, `outcome`, `rationale`, `conflicts`, `crossOscillationDetected`, `coordinatedAt` — line 13 |
| `CoordinationOutcome` | `src/main/java/.../coordination/CoordinationOutcome.java` | `APPROVED_AS_IS`, `MODIFIED`, `REJECTED`, `CAPPED` — line 4 |
| `ClosedLoopValidationRunner` | `src/main/java/.../validation/ClosedLoopValidationRunner.java` | `computeSignificance()` at line 364 uses proxy arrays (lines 389-391: `cl + (Math.random() - 0.5) * cl * 0.1`) — BUG |
| `StatisticalSignificanceCalculator` | `src/main/java/.../validation/StatisticalSignificanceCalculator.java` | `static StatisticalSignificance compare(double[] modeA, double[] modeB, String metricName)` — line 21 |
| `ValidationComparisonReport` | `src/main/java/.../validation/ValidationComparisonReport.java` | Record: `reportId`, `scenario`, `closedLoopResult`, `staticPolicyResult`, `baselineResult`, `comparisons`, `significanceTests`, `overallConclusion`, `generatedAt` |
| `ScenarioExperimentRunner` | `src/main/java/.../scenario/ScenarioExperimentRunner.java` | `ScenarioRunOutcome run(ScenarioDefinition definition, BaselineExecutorPreset preset)` — line 49 |
| `FeedbackCalibrator` | `src/main/java/.../loop/FeedbackCalibrator.java` | `ThresholdPolicyScorer calibrate(AdjustmentHistory, ThresholdPolicyScorer, int windowSize)` — line 64 |

## Architecture and Boundaries

```
┌─────────────────────────────────────────────────────────┐
│                  Scenario Infrastructure                 │
│  ScenarioProfile (enum) ─► DeterministicScenarioPlanner │
│  ScenarioDefinition ─► ScenarioPlan ─► ScenarioStep     │
│  ScenarioExperimentRunner drives baseline execution      │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                  Closed-Loop Control                     │
│  AdjustmentLoop (15-step cycle)                          │
│    ├─ snapshot → classify → decide                       │
│    ├─ OscillationDetector.wouldOscillate()               │
│    ├─ AntiOscillationGuard.evaluate()        [NEW]       │
│    ├─ TimeBasedCooldownSafetyGate.evaluate() [NEW]       │
│    ├─ adapter.apply() → RollbackAwareAdapter [NEW]       │
│    └─ record via LoopEvidenceRecorder                    │
│  DecisionOrchestrator, FeedbackCalibrator                │
│  PressureClassifier, PolicyEvaluator                     │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                  Coordination Layer                      │
│  GroupLoopOrchestrator (fix null registry)               │
│  CoordinatedAdjustmentAdapter                            │
│  GroupCoordinator, ResourceBudget                        │
│  CrossExecutorOscillationDetector                        │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                  Validation & Evidence                   │
│  ClosedLoopValidationRunner (fix proxy arrays)           │
│  ComplexScenarioReport + Generator           [NEW]       │
│  EvidenceRecorder / LoopEvidenceRecorder                 │
│  StatisticalSignificanceCalculator                       │
└─────────────────────────────────────────────────────────┘
```

**Boundary rules:**
- New types compose with existing interfaces; no interface method additions.
- `AdjustmentLoop` unchanged in public API; new guard/gate injected via constructor parameters.
- `ScenarioProfile` enum extended (safe: no polymorphic dispatch beyond planner switch).
- Forbidden paths: `provided-api/`, `src/**/api/**`, `src/**/contract/**`, `.sdd/policy/`, `.sdd/baseline/`, `.sdd/bin/`, `openspec/schemas/`, `pom.xml`.

## Decisions

### Decision 1: Hybrid — Extend Enums, Compose Behaviors

- **Choice**: Extend `ScenarioProfile` enum with 3 new values; create new decorator/guard types for rollback, cooldown, and anti-oscillation; fix v0.15 risks in-place.
- **Rationale**: `ScenarioProfile` is a simple enum with no downstream polymorphic dispatch beyond the planner's switch — adding values is safe and minimal. Rollback, cooldown, and anti-oscillation are distinct behavioral concerns that map cleanly to separate types, following the existing decorator pattern established by `CoordinatedAdjustmentAdapter`. v0.15 risk fixes are small, localized changes done in-place. `AdjustmentLoop` is already 315 lines with 15-step cycle logic — adding rollback and anti-oscillation inline would violate single responsibility.
- **Alternatives rejected**: Option A (extend existing types in-place for all concerns) rejected because it mixes concerns in already-complex classes. Option B (full decorator for everything including enums) rejected because enum extension is low-risk and doesn't warrant a new planner wrapper.

### Decision 2: Rollback via Adapter Decorator with Safety Gate Injection

- **Choice**: `RollbackAwareAdjustmentAdapter` implements `ExecutorAdjustmentAdapter`, wraps a delegate, accepts `RuntimeAdjustmentSafetyGate` in constructor for rollback command evaluation.
- **Rationale**: The adapter pattern is already established by `CoordinatedAdjustmentAdapter`. Rollback is a per-executor concern that belongs in the adapter chain. The safety gate is needed to evaluate rollback commands — injecting it avoids creating a circular dependency with the loop.
- **Alternatives rejected**: Inline rollback in `AdjustmentLoop` rejected because it adds complexity to an already 315-line class and violates single responsibility.

### Decision 3: Time-Based Cooldown as Separate Gate Implementation

- **Choice**: `TimeBasedCooldownSafetyGate` implements `RuntimeAdjustmentSafetyGate`, replaces `DefaultRuntimeAdjustmentSafetyGate`. Accepts `Supplier<Instant>` for injectable time. Emergency rollback bypasses cooldown only when target equals previous safe state.
- **Rationale**: The existing `DefaultRuntimeAdjustmentSafetyGate` uses integer-based cooldown (`cooldownRemaining` decremented per `evaluate()` call). Time-based cooldown is a fundamentally different mechanism. A separate implementation avoids modifying the existing gate and allows both to coexist if needed. The `Supplier<Instant>` pattern is already used by `AdjustmentLoop` and `ScaleAdjustmentCommand`.
- **Alternatives rejected**: Modifying `DefaultRuntimeAdjustmentSafetyGate` in-place rejected because it changes existing behavior and risks breaking existing tests. Adding time-based logic alongside counter-based logic would create a dual-mode gate with unclear semantics.

### Decision 4: Anti-Oscillation as Standalone Guard

- **Choice**: `AntiOscillationGuard` is a standalone class that consults `OscillationDetector` history. Integrated into `AdjustmentLoop` between oscillation check and safety gate evaluation. Uses the existing `SafetyGateDecision` return type with a new `AdjustmentFailureCode.ANTI_OSCILLATION_ACTIVE`.
- **Rationale**: The guard is a distinct concern from both oscillation detection (which identifies patterns) and safety gating (which enforces policy). Making it standalone allows independent testing and configuration. Using the existing `SafetyGateDecision` type avoids creating a parallel decision type.
- **Alternatives rejected**: Extending `OscillationDetector` to block adjustments rejected because detection and blocking are separate responsibilities. Extending `RuntimeAdjustmentSafetyGate` to consult oscillation rejected because the gate should not depend on the detector.

### Decision 5: ComplexScenarioReport as Separate Report Type

- **Choice**: New `ComplexScenarioReport` record and `ComplexScenarioReportGenerator` class, separate from `ValidationComparisonReport`.
- **Rationale**: The existing `ValidationComparisonReport` is tied to the 3-mode comparison (baseline, static, closed-loop). The complex scenario report has different fields (rollback counts, recovery time, observation windows) and different data sources. Keeping them separate follows single responsibility.
- **Alternatives rejected**: Extending `ValidationComparisonReport` rejected because it would bloat a record that serves a different purpose and has a different lifecycle.

### Decision 6: AdjustmentLoop Integration via Constructor Injection

- **Choice**: `AdjustmentLoop` constructor gains two new optional parameters: `AntiOscillationGuard antiOscillationGuard` (nullable) and the existing `RuntimeAdjustmentSafetyGate` is replaced by `TimeBasedCooldownSafetyGate` when time-based cooldown is desired. The loop iteration adds a guard evaluation step between oscillation check and safety gate.
- **Rationale**: Constructor injection follows the existing pattern. Making the guard nullable preserves backward compatibility — existing callers that don't need anti-oscillation can pass `null`. The safety gate is already injected, so swapping implementations is a caller-side change only.
- **Alternatives rejected**: Modifying the loop's internal 15-step cycle to add a new interface rejected because it adds unnecessary abstraction. Using a chain-of-responsibility pattern rejected because the step ordering is fixed and known.

## Data and State Model

**New types (all immutable unless noted):**

- `ComplexScenarioReport` record: `reportId`, `scenarioId`, `seed`, `scenarioConfig` (Map), `adjustmentCount`, `blockedCount`, `rollbackCount`, `rollbackSuccessRate` (double), `recoveryTimeMs`, `p95LatencyMs`, `p99LatencyMs`, `rejectionCount`, `queueDepthDelta`, `throughputDelta`, `decisionWindows` (List<ObservationWindow>), `generatedAt` (Instant).
- `ObservationWindow` record: `decisionIndex`, `preDecisionSnapshots` (List<ObservedSnapshot>), `postDecisionSnapshots` (List<ObservedSnapshot>), `decisionTimestamp` (Instant).
- `DegradationConfig` record: `queueDepthThreshold` (int), `throughputDropThreshold` (double), `latencyIncreaseThreshold` (double).

**Modified types:**

- `ScenarioProfile` enum: add `LONG_TAIL`, `MIXED_CPU_IO`, `DOWNSTREAM_BLOCKED`.
- `AdjustmentFailureCode` enum: add `ANTI_OSCILLATION_ACTIVE`.
- `ScaleAdjustmentCommand`: add `emergencyRollback` boolean field (new `create` overload or builder extension).

**Stateful types (mutable):**

- `TimeBasedCooldownSafetyGate`: maintains `Map<String, Instant> lastAppliedInstant` per executor. Synchronized on `this`.
- `AntiOscillationGuard`: maintains `int consecutiveOscillations`, `boolean activated`. Synchronized on `this`.
- `RollbackAwareAdjustmentAdapter`: stateless per-call (captures pre-snapshot within `apply()`), but records rollback evidence via injected `LoopEvidenceRecorder`.

## Failure Semantics

| Failure | Code | Behavior |
|---|---|---|
| Cooldown active | `COOLDOWN_ACTIVE` | Reject non-emergency command; allow emergency rollback |
| Anti-oscillation active | `ANTI_OSCILLATION_ACTIVE` | Reject non-emergency command; allow emergency rollback |
| Rollback degradation detected | N/A (rollback) | Issue rollback command through safety gate; if rollback also degrades, return rollback result (no recursion) |
| Safety gate rejects rollback | Gate's failure code | Record rejection; return original (non-rolled-back) AdjustmentResult |
| Null delegate in RollbackAwareAdapter | `NullPointerException` | Fail-fast at construction |
| Null clock in TimeBasedCooldownGate | `NullPointerException` | Fail-fast at construction |
| Null ExecutorRegistry in GroupLoopOrchestrator | `IllegalArgumentException` | Fail-fast in `startAll()` |
| Degradation threshold not met | N/A | Return original AdjustmentResult; no rollback |

**Rollback bounded to 1 per decision**: `RollbackAwareAdjustmentAdapter.apply()` sets a `rollingBack` flag before issuing the rollback command. If the rollback itself causes degradation, the flag prevents re-entry and the rollback result is returned as-is.

## Concurrency and Resource Ownership

- `TimeBasedCooldownSafetyGate`: `synchronized evaluate()` and `recordApplied()` — consistent with `DefaultRuntimeAdjustmentSafetyGate`. `lastAppliedInstant` map accessed only within synchronized methods.
- `AntiOscillationGuard`: `synchronized evaluate()` and `reset()`. `consecutiveOscillations` counter accessed only within synchronized methods.
- `RollbackAwareAdjustmentAdapter`: stateless per-call; the `apply()` method captures pre-snapshot, delegates, samples post-snapshot, and optionally issues rollback — all within a single `apply()` invocation. No shared mutable state across calls.
- `InMemoryEvidenceRecorder`: already thread-safe (`ConcurrentHashMap` + `CopyOnWriteArrayList`). Thread-safety contract will be documented in Javadoc.
- `FileBackedEvidenceRecorder`: already thread-safe (`ConcurrentHashMap` buffer + `Files.writeString` with APPEND). Thread-safety contract will be documented in Javadoc.
- `AdjustmentLoop` integration: the guard and gate are evaluated within the loop's daemon thread. The loop already holds no locks during guard/gate evaluation — this is safe because the guard and gate are synchronized internally.

## Security and Competition Constraints

- No new dependencies. Java 21, Maven/JUnit5 only.
- Public API compatibility: existing types unchanged; new types implement existing interfaces.
- No changes to `pom.xml`, `provided-api/`, `src/**/api/**`, `src/**/contract/**`.
- Forbidden paths: `.sdd/policy/`, `.sdd/baseline/`, `.sdd/bin/`, `openspec/schemas/`.
- No synthetic statistical data — all metrics from real observation arrays.
- No `Thread.sleep()` in tests — use `Supplier<Instant>`, `CountDownLatch`, `CyclicBarrier`.

## Testing Strategy

| Requirement/Scenario | Test level | Planned test |
|---|---|---|
| ScenarioProfile has LONG_TAIL, MIXED_CPU_IO, DOWNSTREAM_BLOCKED | Unit | `ScenarioProfileTest` — verify enum values exist and are distinct |
| LONG_TAIL plan with seed % 3 == 0 produces spike steps | Unit | `DeterministicScenarioPlannerTest` — plan with seed=6, verify workUnits=600 |
| LONG_TAIL plan with seed % 3 != 0 produces base steps | Unit | `DeterministicScenarioPlannerTest` — plan with seed=7, verify workUnits=100 |
| MIXED_CPU_IO alternates CPU and IO steps | Unit | `DeterministicScenarioPlannerTest` — plan with baseWorkUnits=50, verify even=150/odd=50 with delays |
| DOWNSTREAM_BLOCKED uses constant work with high delay | Unit | `DeterministicScenarioPlannerTest` — plan with baseWorkUnits=200, verify workUnits=200, delay=2000 |
| All new profiles produce deterministic plans | Unit | `DeterministicScenarioPlannerTest` — call plan() twice, assert equality |
| RollbackAwareAdapter delegates currentState() | Unit | `RollbackAwareAdjustmentAdapterTest` — mock delegate, verify delegation |
| RollbackAwareAdapter rejects null delegate | Unit | `RollbackAwareAdjustmentAdapterTest` — expect NPE |
| Pre-adjustment snapshot captured before apply | Unit | `RollbackAwareAdjustmentAdapterTest` — verify beforeState in result |
| Degradation triggers rollback | Unit | `RollbackAwareAdjustmentAdapterTest` — mock post-state with high queue depth, verify rollback command issued |
| No degradation does not trigger rollback | Unit | `RollbackAwareAdjustmentAdapterTest` — mock post-state within threshold, verify no rollback |
| Rollback bounded to 1 (no recursion) | Unit | `RollbackAwareAdjustmentAdapterTest` — mock rollback causing degradation, verify no second rollback |
| Safety gate rejects rollback | Unit | `RollbackAwareAdjustmentAdapterTest` — mock gate rejecting rollback, verify original result returned |
| Rollback evidence recorded | Unit | `RollbackAwareAdjustmentAdapterTest` — verify LoopEvidenceRecorder.recordIteration() called with rollback details |
| Degradation threshold configurable | Unit | `RollbackAwareAdjustmentAdapterTest` — construct with custom threshold, verify boundary behavior |
| TimeBasedCooldownGate evaluates allowed command | Unit | `TimeBasedCooldownSafetyGateTest` — no prior adjustment, verify ALLOW |
| TimeBasedCooldownGate rejects null clock | Unit | `TimeBasedCooldownSafetyGateTest` — expect NPE |
| Cooldown rejects command within window | Unit | `TimeBasedCooldownSafetyGateTest` — apply at T0, evaluate at T0+500ms with 1s cooldown, verify REJECTED |
| Cooldown allows command after window | Unit | `TimeBasedCooldownSafetyGateTest` — apply at T0, evaluate at T0+1500ms with 1s cooldown, verify ALLOW |
| Emergency rollback bypasses cooldown | Unit | `TimeBasedCooldownSafetyGateTest` — apply at T0, emergency rollback at T0+100ms, verify ALLOW |
| Non-emergency blocked during cooldown | Unit | `TimeBasedCooldownSafetyGateTest` — apply at T0, non-emergency at T0+100ms, verify REJECTED |
| Emergency flag on non-rollback does not bypass | Unit | `TimeBasedCooldownSafetyGateTest` — scale-up with emergency flag during cooldown, verify REJECTED |
| Emergency flag on rollback bypasses | Unit | `TimeBasedCooldownSafetyGateTest` — rollback with emergency flag during cooldown, verify ALLOW |
| All other safety checks preserved | Unit | `TimeBasedCooldownSafetyGateTest` — NOT_READY, RUN_LIMIT_EXCEEDED, NO_OP scenarios |
| Test with controllable clock (no sleep) | Unit | `TimeBasedCooldownSafetyGateTest` — use `AtomicReference<Instant>`, advance clock, verify behavior |
| AntiOscillationGuard blocks non-emergency on sustained oscillation | Unit | `AntiOscillationGuardTest` — mock detector returning true for blockThreshold+1 times, verify REJECTED |
| No oscillation allows adjustment | Unit | `AntiOscillationGuardTest` — mock detector returning false, verify pass-through |
| Emergency rollback bypasses guard | Unit | `AntiOscillationGuardTest` — guard activated, emergency rollback, verify pass-through |
| Non-emergency blocked when guard active | Unit | `AntiOscillationGuardTest` — guard activated, non-emergency, verify REJECTED |
| Block reason recorded in evidence | Unit | `AntiOscillationGuardTest` — verify LoopEvidenceRecorder called with block reason |
| Guard resets on stable adjustment | Unit | `AntiOscillationGuardTest` — activate guard, then non-oscillating decision, verify deactivated |
| Guard remains active during continued oscillation | Unit | `AntiOscillationGuardTest` — oscillation continues, verify guard stays active |
| Configurable block threshold | Unit | `AntiOscillationGuardTest` — threshold=3, 2 oscillations → not active, 3 → active |
| Guard integrates between oscillation check and safety gate | Integration | `AdjustmentLoopIntegrationTest` — verify guard.evaluate() called after oscillationDetector.wouldOscillate() and before safetyGate.evaluate() |
| ANTI_OSCILLATION_ACTIVE exists in AdjustmentFailureCode | Unit | `AdjustmentFailureCodeTest` — verify constant exists |
| ComplexScenarioReport has all required fields | Unit | `ComplexScenarioReportTest` — construct with all fields, verify accessors |
| Rollback success rate computed correctly | Unit | `ComplexScenarioReportTest` — 2/3 success → 0.667 |
| Zero rollbacks yields zero success rate | Unit | `ComplexScenarioReportTest` — 0 rollbacks → 0.0 |
| Generator reads from real evidence | Unit | `ComplexScenarioReportGeneratorTest` — populate recorders, generate, verify metrics match |
| Generator rejects null evidence | Unit | `ComplexScenarioReportGeneratorTest` — expect NPE |
| Observation windows from real snapshots | Unit | `ComplexScenarioReportGeneratorTest` — verify window count and snapshot counts |
| computeSignificance uses real paired arrays | Unit | `ClosedLoopValidationRunnerTest` — verify no proxy array generation |
| GroupLoopOrchestrator.startAll() rejects null registry | Unit | `GroupLoopOrchestratorTest` — verify no null passed to ExecutorRegistry |
| InMemoryEvidenceRecorder concurrent writes | Concurrency | `InMemoryEvidenceRecorderConcurrencyTest` — 4 threads × 50 writes via CyclicBarrier, verify 200 snapshots in order |
| FileBackedEvidenceRecorder concurrent writes | Concurrency | `FileBackedEvidenceRecorderConcurrencyTest` — 4 threads × 50 writes, verify no data loss |
| CoordinatedAdjustmentAdapter rejection path | Unit | `CoordinatedAdjustmentAdapterTest` — coordinator returns REJECTED, verify result and no delegate call |
| CoordinatedAdjustmentAdapter capping path | Unit | `CoordinatedAdjustmentAdapterTest` — coordinator returns CAPPED, verify capped command delegated |
| CoordinatedAdjustmentAdapter approval path | Unit | `CoordinatedAdjustmentAdapterTest` — coordinator returns APPROVED_AS_IS, verify original command delegated |
| CoordinatedAdjustmentAdapter currentState delegation | Unit | `CoordinatedAdjustmentAdapterTest` — verify delegation |
| GroupLoopOrchestrator startAll creates loops | Unit | `GroupLoopOrchestratorTest` — 3 executors, verify 3 LoopSessions |
| GroupLoopOrchestrator emergencyStopAll propagates | Unit | `GroupLoopOrchestratorTest` — verify all loops EMERGENCY_STOPPED |
| GroupLoopOrchestrator getGroupHealth reflects state | Unit | `GroupLoopOrchestratorTest` — verify health after start |
| Complex scenario: BURST profile end-to-end | Integration | `ComplexScenarioIntegrationTest` — run BURST scenario, verify report fields populated |
| Complex scenario: LONG_TAIL with degradation triggers rollback | Integration | `ComplexScenarioIntegrationTest` — run LONG_TAIL with high seed, verify rollback in report |
| Complex scenario: cooldown prevents rapid adjustments | Integration | `ComplexScenarioIntegrationTest` — run scenario, verify blockedCount > 0 |
| Complex scenario: anti-oscillation blocks sustained ping-pong | Integration | `ComplexScenarioIntegrationTest` — run MIXED_CPU_IO, verify anti-oscillation blocks |
| All existing tests still pass | Regression | `mvn test` — full suite |

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Rollback loop (rollback itself degrades) | Infinite rollback cycles | Limit to 1 rollback per decision; return rollback result as-is if it also degrades |
| Cooldown bypass abuse via emergency flag | Rapid adjustments circumvent cooldown | Emergency bypass only applies to rollback commands (target == previous safe state), not arbitrary scale-up |
| Anti-oscillation false positives in complex scenarios | Legitimate direction changes blocked | Configurable `blockThreshold` per scenario; larger windows for LONG_TAIL/MIXED_CPU_IO |
| Statistical significance with real data changes outcomes | Different significance results than before | This is correct behavior — the v0.15 approach was wrong. Tests verify calculator works with real data patterns |
| AdjustmentLoop integration complexity | Harder to reason about 15-step cycle | Composition — loop calls guard/gate as black boxes; each concern independently testable |
| Test duration for complex scenario integration tests | Slow CI | Use minimal step counts and short durations for unit tests; reserve longer runs for tagged integration tests |
| ScaleAdjustmentCommand emergency flag extension | Need to add field without breaking existing callers | Add new `create` overload with `emergencyRollback` parameter; existing callers use existing overload (default false) |
