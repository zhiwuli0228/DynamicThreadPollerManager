# Change Proposal

## Why

The v0.15.0 closed-loop control system has been validated only under simple workload profiles (STEADY, RAMP, BURST). Production-like conditions — bursty arrivals, long-tail latency, mixed CPU/IO, and downstream blocking — expose gaps in stability verification and failure recovery. There is no mechanism to undo a harmful adjustment, cooldown uses a counter rather than injectable wall-clock time, and anti-oscillation is advisory-only across executors. Additionally, the statistical significance computation in `ClosedLoopValidationRunner.computeSignificance()` generates synthetic proxy arrays via `mean + Math.random() * noise`, violating the requirement for real paired observations. v0.16.0 must close these gaps with repeatable complex workload scenarios, verifiable rollback semantics, time-based cooldown, blocking anti-oscillation, and fixes to the identified v0.15 residual risks — all backed by real observation data.

## What Changes

### 1. Complex Workload Scenario Profiles

Extend `ScenarioProfile` enum with three new values: `LONG_TAIL`, `MIXED_CPU_IO`, `DOWNSTREAM_BLOCKED`. Extend `DeterministicScenarioPlanner.plan()` with deterministic formulas:

- **LONG_TAIL**: `workUnits = baseWorkUnits + (seed % 3 == 0 ? baseWorkUnits * 5 : 0)` — occasional spikes using seed-based determinism.
- **MIXED_CPU_IO**: alternating steps; even indices = CPU-bound (high work units), odd indices = IO-bound (low work units + increased delay).
- **DOWNSTREAM_BLOCKED**: constant work units with simulated queue backpressure via elevated `queueDepth` in the step metadata.

Each formula uses the `ScenarioDefinition.seed()` field (currently decorative) for reproducibility. The existing `BURST` profile is reused as the fourth required scenario.

### 2. Rollback-Aware Adjustment Adapter

Create `RollbackAwareAdjustmentAdapter` implementing `ExecutorAdjustmentAdapter`. This decorator wraps a delegate adapter:

- Before `apply()`: captures `ExecutorStateSnapshot` as the pre-adjustment baseline.
- Delegates `apply()` to the wrapped adapter.
- After `apply()`: samples a post-adjustment snapshot.
- If a configurable degradation metric (e.g., queue depth increase > threshold, throughput drop > threshold) worsens beyond the threshold, issues a rollback `ScaleAdjustmentCommand` restoring the prior `corePoolSize`.
- Rollback passes through the existing `RuntimeAdjustmentSafetyGate`.
- Rollback attempts are limited to 1 per adjustment decision to prevent infinite rollback loops.
- All rollback actions (success, failure, skip) are recorded via `LoopEvidenceRecorder` with the failure reason.

### 3. Time-Based Cooldown Safety Gate

Create `TimeBasedCooldownSafetyGate` implementing `RuntimeAdjustmentSafetyGate`. This replaces the counter-based cooldown in `DefaultRuntimeAdjustmentSafetyGate`:

- Accepts `Supplier<Instant>` for injectable time source (matches existing clock injection pattern in `AdjustmentLoop`).
- Maintains `Map<String, Instant> lastAppliedInstant` per executor.
- Cooldown check: `Duration.between(lastApplied, clock.get()).compareTo(cooldownDuration) < 0` → reject with `COOLDOWN_ACTIVE`.
- Emergency rollback bypasses cooldown: when the command carries an `emergencyRollback` flag (or is issued via a dedicated `emergencyRollback()` method targeting the previous safe state), the cooldown check is skipped.
- All other safety gate checks (readiness, per-run limit, opposite direction, no-op) are preserved identically to `DefaultRuntimeAdjustmentSafetyGate`.

### 4. Anti-Oscillation Guard

Create `AntiOscillationGuard` that consults `OscillationDetector` history and blocks non-emergency adjustments when sustained oscillation is detected:

- Wraps the oscillation detection logic: if `OscillationDetector.wouldOscillate()` returns true for more than a configurable `blockThreshold` consecutive evaluations, the guard activates.
- Once activated, all non-emergency adjustments are rejected with a `SafetyGateDecision.rejected(AdjustmentFailureCode.ANTI_OSCILLATION_ACTIVE, reason)`.
- Emergency rollback commands bypass the anti-oscillation guard.
- Block reason is recorded via `LoopEvidenceRecorder`.
- Guard resets when a stable adjustment (no oscillation detected) succeeds.
- Integrates into the `AdjustmentLoop` lifecycle between the oscillation check step and the safety gate evaluation step.

### 5. Complex Scenario Validation Report

Create `ComplexScenarioReport` record and `ComplexScenarioReportGenerator`:

- `ComplexScenarioReport` fields: `reportId`, `scenarioId`, `seed`, `scenarioConfig`, `adjustmentCount`, `blockedCount`, `rollbackCount`, `rollbackSuccessRate`, `recoveryTimeMs`, `p95LatencyMs`, `p99LatencyMs`, `rejectionCount`, `queueDepthDelta`, `throughputDelta`, `List<ObservationWindow> decisionWindows`, `Instant generatedAt`.
- `ObservationWindow` record: `decisionIndex`, `preDecisionSnapshots`, `postDecisionSnapshots`, `decisionTimestamp`.
- `ComplexScenarioReportGenerator` reads from `EvidenceRecorder` (real snapshot arrays), `LoopEvidenceRecorder`, and `AdjustmentHistory` to compute all metrics from real data.
- Observation windows are derived from real snapshot arrays around each decision timestamp — no synthetic data.

### 6. v0.15 Residual Risk Fixes

**6a. Statistical Significance — Replace proxy arrays with real paired observations:**
In `ClosedLoopValidationRunner.computeSignificance()`, replace the `mean + Math.random() * noise` proxy generation with real snapshot arrays from `InMemoryEvidenceRecorder.snapshots(runId)`. Extract the metric of interest (e.g., latency) per snapshot as paired samples. The `StatisticalSignificanceCalculator.compare(double[], double[])` API is unchanged.

**6b. Evidence Recorder Thread Safety — Add contention tests:**
Add tests for `InMemoryEvidenceRecorder` and `FileBackedEvidenceRecorder` using `CyclicBarrier` with concurrent writers (e.g., 4 threads × 50 writes) verifying no data loss and correct snapshot ordering. Document the thread-safety contract in the interface Javadoc.

**6c. GroupLoopOrchestrator — Reject null ExecutorRegistry:**
In `GroupLoopOrchestrator.startAll()`, the current code creates `new ExecutorRegistry(null)`. Fix: require a non-null `ExecutorRegistry` parameter or construct one with a valid `DeletionSafety` instance. Add validation at the top of `startAll()`.

**6d. CoordinatedAdjustmentAdapter and GroupLoopOrchestrator — Add independent behavior tests:**
- `CoordinatedAdjustmentAdapter`: test rejection path (coordinator rejects), capping path (coordinator caps target), approval path (coordinator approves as-is), and delegation to wrapped adapter for `currentState()`.
- `GroupLoopOrchestrator`: test `startAll()` with valid components, verify per-executor loops are created, verify `CoordinatedAdjustmentAdapter` wrapping, verify `emergencyStopAll()` propagates to all loops.

## Capabilities

### New Capabilities

- `complex-scenario-profiles`: Four deterministic complex workload profiles (BURST, LONG_TAIL, MIXED_CPU_IO, DOWNSTREAM_BLOCKED) with seed-based reproducibility.
- `rollback-aware-adjustment`: Decorator adapter that captures pre-adjustment state, detects post-adjustment degradation, and issues bounded rollback through the safety gate.
- `time-based-cooldown`: Injectable-clock cooldown gate that replaces counter-based cooldown while allowing emergency rollback bypass.
- `anti-oscillation-guard`: Blocking guard that prevents non-emergency adjustments when sustained oscillation is detected.
- `complex-scenario-report`: Report generator producing real-observation-based metrics for complex scenario validation.

### Modified Capabilities

- `ScenarioProfile` (enum): Extended with `LONG_TAIL`, `MIXED_CPU_IO`, `DOWNSTREAM_BLOCKED` values.
- `DeterministicScenarioPlanner`: Extended `plan()` switch with formulas for new profiles.
- `ClosedLoopValidationRunner`: `computeSignificance()` uses real paired observation arrays instead of synthetic proxies.
- `GroupLoopOrchestrator`: `startAll()` validates `ExecutorRegistry` is non-null.
- `LoopEvidenceRecorder`: Javadoc updated with explicit thread-safety contract.
- `EvidenceRecorder`: Javadoc updated with explicit thread-safety contract.

## Impact

### Allowed Areas

- `src/main/java/**/experiment/scenario/` — ScenarioProfile enum, DeterministicScenarioPlanner, ScenarioDefinition (read-only for seed field).
- `src/main/java/**/experiment/adjustment/` — New rollback adapter, new cooldown gate, AdjustmentFailureCode enum (add ANTI_OSCILLATION_ACTIVE).
- `src/main/java/**/experiment/loop/` — AntiOscillationGuard, AdjustmentLoop integration point.
- `src/main/java/**/experiment/coordination/` — GroupLoopOrchestrator fix.
- `src/main/java/**/experiment/validation/` — ClosedLoopValidationRunner fix, ComplexScenarioReport.
- `src/test/java/**/experiment/scenario/` — New scenario planner tests.
- `src/test/java/**/experiment/adjustment/` — Rollback adapter and cooldown gate tests.
- `src/test/java/**/experiment/loop/` — Anti-oscillation guard tests.
- `src/test/java/**/experiment/coordination/` — CoordinatedAdjustmentAdapter and GroupLoopOrchestrator behavior tests.
- `src/test/java/**/experiment/validation/` — Complex scenario report tests.
- `src/test/java/**/experiment/metrics/` — Evidence recorder contention tests.
- `openspec/changes/v0-16-0-complex-workload-and/` — OpenSpec artifacts.

### Protected Areas

- `provided-api/**` — No changes.
- `src/**/api/**` — No changes.
- `src/**/contract/**` — No changes.
- `.sdd/policy/**` — No changes.
- `.sdd/baseline/**` — No changes.
- `.sdd/bin/**` — No changes.
- `openspec/schemas/**` — No changes.
- `pom.xml` — No dependency changes.

### Dependencies

No dependency changes. Uses Java 21, existing Maven/JUnit5/Test体系. All new types compose with existing interfaces (`ExecutorAdjustmentAdapter`, `RuntimeAdjustmentSafetyGate`, `ScenarioPlanner`, `LoopEvidenceRecorder`, `EvidenceRecorder`).
