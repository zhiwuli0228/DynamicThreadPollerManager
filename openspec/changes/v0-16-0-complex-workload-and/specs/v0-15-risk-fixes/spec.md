## MODIFIED Requirements

### Requirement: ClosedLoopValidationRunner.computeSignificance() SHALL use real paired observation arrays

The `computeSignificance()` method in `ClosedLoopValidationRunner` MUST replace the current proxy-array generation (`mean + Math.random() * noise`) with real snapshot arrays from `InMemoryEvidenceRecorder.snapshots(runId)`. The method MUST extract the metric of interest (e.g., latency) per snapshot as paired samples. The `StatisticalSignificanceCalculator.compare(double[], double[])` API remains unchanged.

#### Scenario: Significance computation uses real snapshot data

- **WHEN** `computeSignificance()` is called with `ValidationRunResult` instances that have associated `EvidenceRecorder` data
- **THEN** the paired arrays passed to `StatisticalSignificanceCalculator.compare()` contain real per-snapshot metric values, not synthetic proxies

#### Scenario: Significance computation rejects synthetic proxy arrays

- **WHEN** `computeSignificance()` is called
- **THEN** no array is constructed using `mean + Math.random() * noise` or any equivalent synthetic generation

#### Scenario: Significance result reflects real data patterns

- **WHEN** real snapshot arrays show no meaningful difference between closed-loop and baseline
- **THEN** the `StatisticalSignificance.isSignificant()` result reflects the actual (lack of) significance

### Requirement: GroupLoopOrchestrator.startAll() SHALL reject null ExecutorRegistry

The `startAll()` method in `GroupLoopOrchestrator` MUST NOT pass `null` to the `ExecutorRegistry` constructor. The method MUST either accept a non-null `ExecutorRegistry` parameter or construct a valid one with a proper `DeletionSafety` instance.

#### Scenario: startAll() with valid components succeeds

- **WHEN** `startAll()` is called with a valid `ExecutorGroup` and complete `LoopComponents` for each member
- **THEN** all loops are created and started successfully

#### Scenario: startAll() does not create ExecutorRegistry with null

- **WHEN** `startAll()` is called
- **THEN** the `ExecutorRegistry` used internally is constructed with a non-null argument

### Requirement: EvidenceRecorder interface SHALL document thread-safety contract

The `EvidenceRecorder` interface Javadoc MUST explicitly document its thread-safety contract, specifying that `record()` and `snapshots()` are safe for concurrent invocation and that insertion order is preserved per run.

#### Scenario: Thread-safety contract is documented

- **WHEN** the `EvidenceRecorder` interface source code is read
- **THEN** the Javadoc specifies concurrent safety guarantees for `record()` and `snapshots()`

### Requirement: LoopEvidenceRecorder interface SHALL document thread-safety contract

The `LoopEvidenceRecorder` interface Javadoc MUST explicitly document its thread-safety contract, specifying that `recordIteration()`, `recordSessionStart()`, `recordSessionEnd()`, and `getIterationEvidence()` are safe for concurrent invocation.

#### Scenario: Thread-safety contract is documented

- **WHEN** the `LoopEvidenceRecorder` interface source code is read
- **THEN** the Javadoc specifies concurrent safety guarantees for all methods

### Requirement: InMemoryEvidenceRecorder SHALL pass concurrent write contention tests

The `InMemoryEvidenceRecorder` MUST pass tests using `CyclicBarrier` with concurrent writers (e.g., 4 threads × 50 writes) verifying no data loss and correct snapshot ordering.

#### Scenario: Concurrent writes do not lose data

- **WHEN** 4 threads each write 50 snapshots to the same runId concurrently using `CyclicBarrier` synchronization
- **THEN** `snapshots(runId)` returns exactly 200 snapshots in insertion order

#### Scenario: Concurrent writes to different runIds are independent

- **WHEN** threads write to different runIds concurrently
- **THEN** each runId's snapshot list contains only its own snapshots

### Requirement: FileBackedEvidenceRecorder SHALL pass concurrent write contention tests

The `FileBackedEvidenceRecorder` MUST pass tests using `CyclicBarrier` with concurrent writers verifying no data loss and correct snapshot ordering.

#### Scenario: Concurrent writes do not lose data

- **WHEN** 4 threads each write 50 snapshots to the same runId concurrently
- **THEN** `snapshots(runId)` returns exactly 200 snapshots without data loss

### Requirement: CoordinatedAdjustmentAdapter SHALL have independent behavior tests

The `CoordinatedAdjustmentAdapter` MUST have independent tests covering:
1. **Rejection path**: when `GroupCoordinator` returns `REJECTED`, the adapter returns a `REJECTED` `AdjustmentResult` without calling the delegate.
2. **Capping path**: when `GroupCoordinator` returns `CAPPED`, the adapter delegates to the wrapped adapter with the capped command.
3. **Approval path**: when `GroupCoordinator` returns `APPROVED_AS_IS`, the adapter delegates to the wrapped adapter with the original command.
4. **Delegation path**: `currentState()` delegates to the wrapped adapter.

#### Scenario: Coordinator rejection returns REJECTED result

- **WHEN** `apply()` is called and `coordinator.coordinate()` returns `REJECTED`
- **THEN** the returned `AdjustmentResult` has status `REJECTED` and `failureCode = COORDINATION_REJECTED`
- **AND** `delegate.apply()` is NOT called

#### Scenario: Coordinator capping delegates capped command

- **WHEN** `apply()` is called with command targeting pool size 12 and `coordinator.coordinate()` returns `CAPPED` with approved command targeting pool size 8
- **THEN** `delegate.apply()` is called with the capped command (target = 8)

#### Scenario: Coordinator approval delegates original command

- **WHEN** `apply()` is called and `coordinator.coordinate()` returns `APPROVED_AS_IS`
- **THEN** `delegate.apply()` is called with the original command

#### Scenario: currentState delegates to wrapped adapter

- **WHEN** `currentState()` is called
- **THEN** the result is identical to `delegate.currentState()`

### Requirement: GroupLoopOrchestrator SHALL have independent behavior tests

The `GroupLoopOrchestrator` MUST have independent tests covering:
1. **startAll()**: verifies per-executor loops are created and started.
2. **CoordinatedAdjustmentAdapter wrapping**: verifies each executor's adapter is wrapped with coordination.
3. **emergencyStopAll()**: verifies emergency stop propagates to all loops.

#### Scenario: startAll() creates loops for all executors

- **WHEN** `startAll()` is called with a group containing 3 executors and complete components for each
- **THEN** 3 loops are created and 3 `LoopSession` instances are returned

#### Scenario: emergencyStopAll() stops all running loops

- **WHEN** `emergencyStopAll("test")` is called after `startAll()` starts 3 loops
- **THEN** all 3 loops transition to `EMERGENCY_STOPPED` state

#### Scenario: getGroupHealth() reflects current loop states

- **WHEN** `getGroupHealth()` is called after `startAll()` starts 3 loops
- **THEN** the returned `GroupHealth` shows 3 running loops
