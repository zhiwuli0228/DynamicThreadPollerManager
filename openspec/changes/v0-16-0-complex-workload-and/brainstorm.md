# Brainstorm

## Objective

v0.16.0 must prove that the closed-loop control system remains stable under complex, repeatable workloads and can recover from failures via rollback. The system must demonstrate cooldown prevents excessive adjustments, rollback restores safe state on degradation, and anti-oscillation blocks sustained ping-pong in complex scenarios. All claims must be backed by real observation data, not synthetic proxies.

## Current State

The v0.15.0 codebase provides a layered experiment framework with ~150 main source files and ~150 test files across 15 packages:

**Scenario infrastructure** (`experiment.scenario`):
- `ScenarioProfile` enum: `STEADY`, `RAMP`, `BURST` — no `LONG_TAIL`, `MIXED_CPU_IO`, or `DOWNSTREAM_BLOCKED`.
- `DeterministicScenarioPlanner` maps profiles to step work units deterministically.
- `ScenarioExperimentRunner` drives baseline workload execution and evidence recording.
- `ScenarioDefinition` carries `seed` but the planner does not use it — seed is decorative.

**Closed-loop control** (`experiment.loop`):
- `AdjustmentLoop` runs a 15-step cycle: snapshot → classify → decide → oscillation check → safety gate → apply → record. Uses `Thread.sleep()` for sampling intervals.
- `OscillationDetector` detects ping-pong, over-adjustment, and policy switching per executor.
- `DecisionOrchestrator` classifies pressure, ranks policies, evaluates and returns `AdjustmentDecision`.
- `FeedbackCalibrator` adjusts scorer weights via median-split correlation.
- `InMemoryLoopEvidenceRecorder` uses `CopyOnWriteArrayList` — thread-safe for recording.

**Safety and adjustment** (`experiment.adjustment`):
- `DefaultRuntimeAdjustmentSafetyGate` enforces: readiness → cooldown → per-run limit → opposite direction → no-op → allow. Cooldown is counter-based (decision intervals), not time-based.
- `ExecutorAdjustmentAdapter` interface: `currentState()` + `apply(command)`.
- `AdjustmentResult` captures before/after state, status, failure code.

**Coordination** (`experiment.coordination`):
- `GroupLoopOrchestrator` manages multiple `AdjustmentLoop` instances. `startAll()` passes `null` to `ExecutorRegistry` constructor (v0.15 risk).
- `CoordinatedAdjustmentAdapter` decorates per-executor adapters with `GroupCoordinator` interception. No independent behavior tests.
- `CrossExecutorOscillationDetector` is advisory only — flags but does not block.
- `GroupCoordinator` resolves budget conflicts by priority with preemption.
- `ResourceBudget` tracks per-executor thread allocations with `synchronized` methods.

**Validation** (`experiment.validation`):
- `ClosedLoopValidationRunner` runs 3-mode comparison (baseline, static, closed-loop).
- `StatisticalSignificanceCalculator` implements paired t-test. However, `computeSignificance()` in the runner creates proxy arrays via `mean + random noise` — the exact anti-pattern the task packet forbids.
- `ValidationScenario` requires `durationMs >= 30_000` and `minIterations >= 5`.

**Evidence** (`experiment.metrics`, `experiment.acquisition`):
- `InMemoryEvidenceRecorder` uses `ConcurrentHashMap<String, CopyOnWriteArrayList>` — thread-safe.
- `FileBackedEvidenceRecorder` writes JSONL immediately and buffers in memory.

**Key v0.15 risks identified in the task packet**:
1. Statistical significance uses synthetic proxy arrays instead of real paired observations.
2. Evidence recorder thread safety is present (`ConcurrentHashMap` + `CopyOnWriteArrayList`) but not explicitly documented or tested under contention.
3. `GroupLoopOrchestrator.startAll()` passes `null` to `ExecutorRegistry` constructor.
4. `CoordinatedAdjustmentAdapter` and `GroupLoopOrchestrator` lack independent behavior tests.

## Binding Constraints

- **Java 21, Maven, JUnit** — no new dependencies.
- **Public API compatibility** — existing types must not break; extend via overloads or new types.
- **No `Thread.sleep()` in tests** — use `CountDownLatch`, `Barrier`, `Supplier<Instant>`, or condition waits.
- **No synthetic statistical data** — paired samples must come from real observation streams.
- **Minimal coherent change** — single responsibility, explicit failure semantics, no dead code.
- **Existing tests must not weaken** — all ~150 test files must continue passing.
- **Forbidden paths**: `.sdd/policy/`, `.sdd/baseline/`, `.sdd/bin/`, `openspec/schemas/`, `provided-api/`, `src/**/api/**`, `src/**/contract/**`.

## Scope

### In Scope

1. **Complex workload scenario profiles**: Add `LONG_TAIL`, `MIXED_CPU_IO`, `DOWNSTREAM_BLOCKED` to `ScenarioProfile` and extend `DeterministicScenarioPlanner` with deterministic step formulas. Each scenario must accept a seed for reproducibility.

2. **Rollback semantics in closed-loop**: Record pre-adjustment `ExecutorStateSnapshot` before each `apply()`. After apply, sample post-adjustment observations. If metrics degrade beyond a configurable threshold, issue a rollback command restoring the prior state. Rollback must pass through the safety gate. Rollback evidence must be recorded with failure reason.

3. **Cooldown control with injectable time**: Replace counter-based cooldown in `DefaultRuntimeAdjustmentSafetyGate` with time-based cooldown using `Supplier<Instant>`. Emergency rollback must bypass cooldown. Tests use controllable clock.

4. **Anti-oscillation escalation**: Extend `OscillationDetector` (or add a new `AntiOscillationGate`) to block non-emergency adjustments when sustained oscillation is detected across a configurable window in complex scenarios. Block reason must enter evidence.

5. **Complex scenario validation report**: Extend `ValidationComparisonReport` (or create `ComplexScenarioReport`) with: scenario ID/seed/config, adjustment/blocked/rollback counts, rollback success rate, recovery time, p95/p99 latency, rejection count, queue depth, throughput delta, and per-decision observation windows.

6. **v0.15 risk fixes**:
   - Replace proxy-array significance computation with real paired observation arrays from `EvidenceRecorder`.
   - Add explicit thread-safety contract documentation and contention tests for `InMemoryEvidenceRecorder` and `FileBackedEvidenceRecorder`.
   - Fix `GroupLoopOrchestrator.startAll()` to reject null `ExecutorRegistry` or construct a valid one.
   - Add independent behavior tests for `CoordinatedAdjustmentAdapter` and `GroupLoopOrchestrator`.

### Out of Scope

- Cross-JVM or distributed coordination.
- New external dependencies.
- Frontend, database, message queue, auth, or monitoring integration.
- Multi-dimensional simultaneous adjustment.
- Policy auto-generation.
- Changes to `pom.xml` dependencies or Java version.

## Alternatives

### Option A: Extend Existing Types In-Place

**Approach**: Add new enum values to `ScenarioProfile`, add fields to existing records, modify `DeterministicScenarioPlanner` switch, extend `DefaultRuntimeAdjustmentSafetyGate` cooldown logic.

**Pros**: Minimal new types; stays close to existing patterns; fewer files to create.

**Cons**: Risk of violating the "modify existing types only after reading source" constraint; larger diffs to existing files increase merge conflict surface; mixing concerns in already-complex classes like `AdjustmentLoop`.

### Option B: Decorator and Composition Pattern

**Approach**: Keep existing types unchanged. Create new decorator/wrapper types:
- `ComplexScenarioPlanner` wraps `DeterministicScenarioPlanner` with new profile support.
- `RollbackAwareAdapter` wraps `ExecutorAdjustmentAdapter` adding pre/post snapshot comparison and rollback.
- `TimeBasedCooldownGate` wraps or replaces `DefaultRuntimeAdjustmentSafetyGate` with injectable clock.
- `AntiOscillationGuard` sits between oscillation detection and adjustment application.
- `ComplexScenarioReportGenerator` produces the extended report.

**Pros**: Single responsibility per new type; existing types untouched; easier to test each concern independently; follows existing decorator pattern (cf. `CoordinatedAdjustmentAdapter`).

**Cons**: More files; wiring complexity in the runner; potential for decorator stack depth.

### Option C: Hybrid — Extend Enums, Compose Behaviors

**Approach**: Extend `ScenarioProfile` enum (minimal, safe change) and add new composed behavior types for rollback, cooldown, and anti-oscillation. Fix v0.15 risks in-place where the fix is small and isolated.

**Pros**: Balance between minimal changes to stable types and clean separation of new concerns. Enum extension is low-risk. Behavioral additions are isolated in new types.

**Cons**: Requires careful interface design to ensure new composed types integrate cleanly with existing `AdjustmentLoop` lifecycle.

## Decision

**Option C: Hybrid — Extend Enums, Compose Behaviors.**

Rationale:
- `ScenarioProfile` is a simple enum with no downstream polymorphic dispatch beyond the planner's switch — adding values is safe and minimal.
- Rollback, cooldown, and anti-oscillation are distinct behavioral concerns that map cleanly to separate types, following the existing decorator pattern established by `CoordinatedAdjustmentAdapter`.
- v0.15 risk fixes are small, localized changes that should be done in-place.
- The `AdjustmentLoop` class is already 315 lines with 15-step cycle logic — adding rollback and anti-oscillation inline would violate single responsibility.

**Specific design decisions**:

1. **Scenario profiles**: Add `LONG_TAIL`, `MIXED_CPU_IO`, `DOWNSTREAM_BLOCKED` to `ScenarioProfile`. Extend `DeterministicScenarioPlanner` with formulas using the seed for `LONG_TAIL` jitter and `MIXED_CPU_IO` step-type alternation. `DOWNSTREAM_BLOCKED` uses high queue depth simulation.

2. **Rollback**: Create `RollbackAwareAdjustmentAdapter` implementing `ExecutorAdjustmentAdapter`. It wraps a delegate adapter, captures `ExecutorStateSnapshot` before `apply()`, delegates, then samples post-state. If degradation detected (configurable metric threshold), issues a rollback `ScaleAdjustmentCommand` through the safety gate. Records rollback evidence via `LoopEvidenceRecorder`.

3. **Cooldown**: Create `TimeBasedCooldownSafetyGate` implementing `RuntimeAdjustmentSafetyGate`. Accepts `Supplier<Instant>` for time source. Maintains `lastAppliedInstant` per executor. Emergency rollback checks bypass cooldown via a `force` flag on the command or a dedicated `emergencyRollback()` method.

4. **Anti-oscillation**: Create `AntiOscillationGuard` that consults `OscillationDetector` history and blocks non-emergency adjustments when sustained oscillation pattern count exceeds threshold. Returns a `SafetyGateDecision`-like result with block reason. Integrates between oscillation detection and safety gate evaluation in the loop.

5. **Report**: Create `ComplexScenarioReport` record and `ComplexScenarioReportGenerator` that reads from `EvidenceRecorder`, `LoopEvidenceRecorder`, `AdjustmentHistory`, and produces the required metrics. Observation windows are derived from real snapshot arrays around each decision timestamp.

6. **v0.15 fixes**:
   - `ClosedLoopValidationRunner.computeSignificance()`: Replace proxy arrays with real snapshot arrays from `InMemoryEvidenceRecorder.snapshots(runId)`, extracting the metric of interest per snapshot.
   - `GroupLoopOrchestrator.startAll()`: Add null check for `ExecutorRegistry` parameter or require it via constructor.
   - Add contention tests for evidence recorders using `CyclicBarrier` with concurrent writers.
   - Add independent tests for `CoordinatedAdjustmentAdapter` and `GroupLoopOrchestrator` covering rejection, capping, and lifecycle paths.

## Risks

1. **Rollback loop risk**: If rollback itself degrades metrics, infinite rollback cycles could occur. **Mitigation**: Limit rollback attempts to 1 per adjustment decision. If rollback also fails, escalate to emergency stop.

2. **Cooldown bypass abuse**: Emergency rollback bypassing cooldown could be exploited to make rapid adjustments. **Mitigation**: Emergency bypass only applies to rollback commands (target == previous safe state), not arbitrary scale-up commands.

3. **Anti-oscillation false positives**: In `LONG_TAIL` or `MIXED_CPU_IO` scenarios, legitimate direction changes may look like oscillation. **Mitigation**: Use larger window sizes for anti-oscillation in complex scenarios (configurable per scenario).

4. **Statistical significance with real data**: Real observation arrays may have less variance than synthetic proxies, potentially changing significance outcomes. **Mitigation**: This is the correct behavior — the v0.15 approach was wrong. Tests should verify the calculator works with real data patterns.

5. **`AdjustmentLoop` integration complexity**: Adding rollback, cooldown override, and anti-oscillation guard to the 15-step cycle increases complexity. **Mitigation**: Use composition — the loop calls the guard/gate as black boxes; each concern is independently testable.

6. **Test duration**: Complex scenario tests with real workload execution may be slow. **Mitigation**: Use minimal step counts and short durations for unit-level scenario tests; reserve longer runs for integration tests that can be tagged/skipped.

## Blocking Ambiguities

Write `None` when no blocking ambiguity remains.

None.
