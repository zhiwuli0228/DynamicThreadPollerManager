# Tasks

## 1. Complex Scenario Profiles

### 1.1 Extend ScenarioProfile enum
- [ ] Add `LONG_TAIL`, `MIXED_CPU_IO`, `DOWNSTREAM_BLOCKED` values to `ScenarioProfile` enum
- [ ] Test: Verify enum values exist and are distinct from existing `STEADY`, `RAMP`, `BURST`

### 1.2 Extend DeterministicScenarioPlanner for LONG_TAIL
- [ ] Implement formula: `workUnits = baseWorkUnits + (seed % 3 == 0 ? baseWorkUnits * 5 : 0)`
- [ ] Test: LONG_TAIL with seed=6 produces workUnits=600 (when baseWorkUnits=100)
- [ ] Test: LONG_TAIL with seed=7 produces workUnits=100 (when baseWorkUnits=100)
- [ ] Test: Plan is deterministic across invocations

### 1.3 Extend DeterministicScenarioPlanner for MIXED_CPU_IO
- [ ] Implement formula: even index → `workUnits = baseWorkUnits * 3`, odd index → `workUnits = baseWorkUnits` with `plannedDelayMillis = baseWorkUnits * 2`
- [ ] Test: MIXED_CPU_IO alternates CPU and IO steps correctly
- [ ] Test: Plan is deterministic across invocations

### 1.4 Extend DeterministicScenarioPlanner for DOWNSTREAM_BLOCKED
- [ ] Implement formula: `workUnits = baseWorkUnits`, `plannedDelayMillis = baseWorkUnits * 10`
- [ ] Test: DOWNSTREAM_BLOCKED uses constant work units with high delay
- [ ] Test: Plan is deterministic across invocations

### 1.5 Verify seed reproducibility
- [ ] Test: Different seeds produce different LONG_TAIL plans when seed % 3 differs
- [ ] Test: Same seed produces identical plans for any new profile

---

## 2. Rollback-Aware Adjustment Adapter

### 2.1 Create RollbackAwareAdjustmentAdapter class
- [ ] Implement `ExecutorAdjustmentAdapter` interface
- [ ] Accept delegate `ExecutorAdjustmentAdapter` in constructor (null check)
- [ ] Accept `RuntimeAdjustmentSafetyGate` in constructor for rollback evaluation
- [ ] Accept `DegradationConfig` for configurable thresholds
- [ ] Test: Rejects null delegate with NullPointerException

### 2.2 Implement currentState delegation
- [ ] Delegate `currentState()` to wrapped adapter
- [ ] Test: currentState returns identical result to delegate

### 2.3 Implement pre-adjustment snapshot capture
- [ ] Capture `ExecutorStateSnapshot` before delegating `apply()`
- [ ] Test: Pre-adjustment snapshot is captured before delegate apply

### 2.4 Implement degradation detection and rollback
- [ ] Sample post-adjustment snapshot after delegate `apply()`
- [ ] Compare against degradation thresholds (queue depth, throughput, latency)
- [ ] Issue rollback `ScaleAdjustmentCommand` if degradation detected
- [ ] Test: Degradation detected triggers rollback
- [ ] Test: No degradation does not trigger rollback

### 2.5 Implement rollback bounding (max 1 per decision)
- [ ] Use `rollingBack` flag to prevent re-entry
- [ ] Test: Rollback does not recurse when rollback itself degrades

### 2.6 Implement safety gate integration for rollback
- [ ] Pass rollback command through `RuntimeAdjustmentSafetyGate`
- [ ] If gate rejects, return original result
- [ ] Test: Safety gate allows rollback
- [ ] Test: Safety gate rejects rollback

### 2.7 Implement rollback evidence recording
- [ ] Record all rollback actions (success, failure, skip, rejection) via `LoopEvidenceRecorder`
- [ ] Test: Successful rollback is recorded in evidence
- [ ] Test: Failed rollback is recorded in evidence

### 2.8 Verify configurable degradation threshold
- [ ] Test: Custom threshold controls rollback behavior correctly

---

## 3. Time-Based Cooldown Safety Gate

### 3.1 Create TimeBasedCooldownSafetyGate class
- [ ] Implement `RuntimeAdjustmentSafetyGate` interface
- [ ] Accept `Supplier<Instant>` for injectable time source
- [ ] Maintain `Map<String, Instant> lastAppliedInstant` per executor
- [ ] Test: Rejects null clock with NullPointerException

### 3.2 Implement time-based cooldown check
- [ ] Compare `Duration.between(lastAppliedInstant, clock.get())` against `cooldownDuration`
- [ ] Reject with `COOLDOWN_ACTIVE` if within cooldown window
- [ ] Test: Command rejected during cooldown window
- [ ] Test: Command allowed after cooldown expires

### 3.3 Implement emergency rollback bypass
- [ ] Skip cooldown check for emergency rollback commands
- [ ] Emergency bypass only applies to rollback (target == previous safe state)
- [ ] Test: Emergency rollback bypasses active cooldown
- [ ] Test: Non-emergency command is blocked during cooldown
- [ ] Test: Emergency flag on non-rollback command does not bypass cooldown
- [ ] Test: Emergency flag on rollback command bypasses cooldown

### 3.4 Preserve all other safety checks
- [ ] Implement readiness check (NOT_READY)
- [ ] Implement per-run limit check (RUN_LIMIT_EXCEEDED)
- [ ] Implement opposite direction check (OPPOSITE_DIRECTION)
- [ ] Implement no-op detection (NO_OP)
- [ ] Test: NOT_READY rejection still applies
- [ ] Test: Per-run limit rejection still applies
- [ ] Test: No-op detection still applies

### 3.5 Verify controllable clock testability
- [ ] Test: Use `AtomicReference<Instant>` to control time without sleep

---

## 4. Anti-Oscillation Guard

### 4.1 Add ANTI_OSCILLATION_ACTIVE to AdjustmentFailureCode
- [ ] Add new enum constant `ANTI_OSCILLATION_ACTIVE`
- [ ] Test: Constant exists and can be used in `SafetyGateDecision.rejected()`

### 4.2 Create AntiOscillationGuard class
- [ ] Accept `OscillationDetector` and `blockThreshold` in constructor
- [ ] Maintain `consecutiveOscillations` counter and `activated` flag
- [ ] Test: Threshold controls activation correctly

### 3.3 Implement guard evaluation logic
- [ ] Consult `OscillationDetector.wouldOscillate()` for each decision
- [ ] Increment counter on oscillation, reset on stable
- [ ] Activate guard when counter exceeds `blockThreshold`
- [ ] Test: Sustained oscillation blocks non-emergency adjustment
- [ ] Test: No oscillation allows adjustment
- [ ] Test: Threshold exceeded activates guard

### 4.4 Implement emergency rollback bypass
- [ ] Allow emergency rollback commands even when guard is activated
- [ ] Test: Emergency rollback bypasses active anti-oscillation guard
- [ ] Test: Non-emergency blocked when guard is active

### 4.5 Implement guard reset on stable adjustment
- [ ] Reset `consecutiveOscillations` to 0 when stable adjustment succeeds
- [ ] Test: Guard resets after stable adjustment
- [ ] Test: Guard remains active during continued oscillation

### 4.6 Implement evidence recording for blocked adjustments
- [ ] Record block reason via `LoopEvidenceRecorder` when guard blocks
- [ ] Test: Blocked adjustment is recorded in evidence

### 4.7 Integrate guard into AdjustmentLoop lifecycle
- [ ] Evaluate guard after `oscillationDetector.wouldOscillate()` and before `safetyGate.evaluate()`
- [ ] Test: Guard is consulted before safety gate

---

## 5. Complex Scenario Validation Report

### 5.1 Create ComplexScenarioReport record
- [ ] Fields: `reportId`, `scenarioId`, `seed`, `scenarioConfig`, `adjustmentCount`, `blockedCount`, `rollbackCount`, `rollbackSuccessRate`, `recoveryTimeMs`, `p95LatencyMs`, `p99LatencyMs`, `rejectionCount`, `queueDepthDelta`, `throughputDelta`, `decisionWindows`, `generatedAt`
- [ ] Test: Report is created with all identification fields
- [ ] Test: Counts reflect scenario execution
- [ ] Test: Rollback success rate computed correctly
- [ ] Test: Zero rollbacks yields zero success rate

### 5.2 Create ObservationWindow record
- [ ] Fields: `decisionIndex`, `preDecisionSnapshots`, `postDecisionSnapshots`, `decisionTimestamp`
- [ ] Test: Observation windows are populated from real data

### 5.3 Create DegradationConfig record
- [ ] Fields: `queueDepthThreshold`, `throughputDropThreshold`, `latencyIncreaseThreshold`
- [ ] Test: Config is immutable and accessible

### 5.4 Create ComplexScenarioReportGenerator class
- [ ] Read from `EvidenceRecorder`, `LoopEvidenceRecorder`, `AdjustmentHistory`
- [ ] Compute all metrics from real observation data (no synthetic proxies)
- [ ] Test: Generator produces report from real evidence
- [ ] Test: Generator rejects null evidence sources

### 5.5 Implement percentile latency computation
- [ ] Compute p95 and p99 from real observation arrays
- [ ] Test: Percentile latencies are computed from real snapshots

### 5.6 Implement delta computation
- [ ] Compute queue depth delta (final - initial)
- [ ] Compute throughput delta (final - initial)
- [ ] Test: Deltas reflect start-to-end change

---

## 6. v0.15 Risk Fixes

### 6.1 Fix ClosedLoopValidationRunner.computeSignificance()
- [ ] Replace proxy array generation (`mean + Math.random() * noise`) with real snapshot arrays from `InMemoryEvidenceRecorder.snapshots(runId)`
- [ ] Extract metric of interest per snapshot as paired samples
- [ ] Test: Significance computation uses real snapshot data
- [ ] Test: Significance computation rejects synthetic proxy arrays
- [ ] Test: Significance result reflects real data patterns

### 6.2 Fix GroupLoopOrchestrator.startAll() null registry
- [ ] Add null check for `ExecutorRegistry` parameter
- [ ] Construct valid `ExecutorRegistry` with proper `DeletionSafety` instance
- [ ] Test: startAll() with valid components succeeds
- [ ] Test: startAll() does not create ExecutorRegistry with null

### 6.3 Document EvidenceRecorder thread-safety contract
- [ ] Add Javadoc to `EvidenceRecorder` interface documenting concurrent safety
- [ ] Test: Thread-safety contract is documented

### 6.4 Document LoopEvidenceRecorder thread-safety contract
- [ ] Add Javadoc to `LoopEvidenceRecorder` interface documenting concurrent safety
- [ ] Test: Thread-safety contract is documented

### 6.5 Add InMemoryEvidenceRecorder contention tests
- [ ] Test: 4 threads × 50 writes via CyclicBarrier, verify 200 snapshots in order
- [ ] Test: Concurrent writes to different runIds are independent

### 6.6 Add FileBackedEvidenceRecorder contention tests
- [ ] Test: 4 threads × 50 writes, verify no data loss

### 6.7 Add CoordinatedAdjustmentAdapter behavior tests
- [ ] Test: Coordinator rejection returns REJECTED result
- [ ] Test: Coordinator capping delegates capped command
- [ ] Test: Coordinator approval delegates original command
- [ ] Test: currentState delegates to wrapped adapter

### 6.8 Add GroupLoopOrchestrator behavior tests
- [ ] Test: startAll() creates loops for all executors
- [ ] Test: emergencyStopAll() stops all running loops
- [ ] Test: getGroupHealth() reflects current loop states

---

## 7. Integration Tests

### 7.1 Complex scenario end-to-end tests
- [ ] Test: BURST profile end-to-end with report generation
- [ ] Test: LONG_TAIL with degradation triggers rollback
- [ ] Test: Cooldown prevents rapid adjustments in complex scenario
- [ ] Test: Anti-oscillation blocks sustained ping-pong in MIXED_CPU_IO

### 7.2 Regression verification
- [ ] Run full `mvn test` suite — all existing tests must pass
- [ ] Verify no changes to protected APIs, dependencies, or build config

---

## 8. Verification

- [ ] 8.1 Run focused and full configured quality gates
- [ ] 8.2 Verify protected API, dependency, scope, and traceability constraints
- [ ] 8.3 Verify all spec requirements have corresponding tests
- [ ] 8.4 Verify no synthetic statistical data in any computation
