## ADDED Requirements

### Requirement: LivePressureSampler MUST autonomously sample at fixed delay

The system MUST provide a `LivePressureSampler` implementing `PressureSampler` that autonomously polls a `ManagedExecutor` at a configurable fixed delay using a daemon single-thread scheduled executor.

#### Scenario: Start autonomous sampling
- **WHEN** `start(runId)` is called
- **THEN** the sampler SHALL schedule periodic sampling at the configured poll interval
- **AND** each sample SHALL read all available metrics from the ManagedExecutor via `RuntimeObservation.fromExecutor()`
- **AND** each sample SHALL be recorded via `EvidenceRecorder.record()`

#### Scenario: Fixed delay scheduling
- **WHEN** a sample takes longer than the poll interval to complete
- **THEN** the next sample SHALL be scheduled after the current sample completes (not overlapping)

#### Scenario: Stop sampling
- **WHEN** `stop()` is called
- **THEN** the scheduler SHALL be shut down
- **AND** `awaitTermination(10, SECONDS)` SHALL be called
- **AND** `shutdownNow()` SHALL be called if termination times out
- **AND** subsequent `start()` calls SHALL throw `IllegalStateException`

#### Scenario: Double start throws
- **WHEN** `start()` is called while the sampler is already running
- **THEN** an `IllegalStateException` SHALL be thrown

#### Scenario: Double stop is idempotent
- **WHEN** `stop()` is called after the sampler is already stopped
- **THEN** no exception SHALL be thrown and the call SHALL be a no-op

#### Scenario: Manual sample method
- **WHEN** `sample(runId, observation, at)` is called
- **THEN** the observation SHALL be timestamped with `at` via `observation.withTimestamp(at)`
- **AND** the snapshot SHALL be assembled and recorded
- **AND** the method SHALL work regardless of whether autonomous sampling is running

#### Scenario: Circuit breaker on consecutive failures
- **WHEN** `MAX_CONSECUTIVE_FAILURES` (10) consecutive samples throw RuntimeException
- **THEN** the sampler SHALL automatically call `stop()`
- **AND** the scheduler SHALL be shut down

#### Scenario: Failure counter resets on success
- **WHEN** a sample succeeds after one or more failures
- **THEN** the consecutive failure counter SHALL reset to 0

---

### Requirement: LivePressureSamplerConfig MUST validate parameters

The system MUST provide a `LivePressureSamplerConfig` record with validation.

#### Scenario: Minimum poll interval
- **WHEN** `LivePressureSamplerConfig` is created with pollIntervalMs < 100
- **THEN** an `IllegalArgumentException` SHALL be thrown

#### Scenario: Default configuration
- **WHEN** `LivePressureSamplerConfig.defaults(sessionId)` is called
- **THEN** the config SHALL have pollIntervalMs=1000 and autoStart=false

---

## MODIFIED Requirements

None in this change — the spec modifications are in change 1's domain. This change is purely additive on top of change 1.

---

### Requirement: ManagedExecutorScenarioRunner MUST support LivePressureSampler injection

The system MUST extend `ManagedExecutorScenarioRunner` with a new constructor accepting an optional `LivePressureSampler`. When injected, autonomous sampling replaces step-level manual sampling.

#### Scenario: Backward compatible construction
- **WHEN** `ManagedExecutorScenarioRunner` is constructed with the existing 5-arg constructor
- **THEN** behavior SHALL be identical to before this change
- **AND** step-level manual sampling SHALL be used

#### Scenario: LivePressureSampler injection
- **WHEN** `ManagedExecutorScenarioRunner` is constructed with the new 6-arg constructor and a non-null `LivePressureSampler`
- **THEN** the sampler SHALL be started in Phase 2 of `run()`
- **AND** step-level manual sampling SHALL be skipped in Phase 3
- **AND** the sampler SHALL be stopped in Phase 5

#### Scenario: End-to-end live sampling produces evidence
- **WHEN** a scenario is run with a LivePressureSampler injected and a FileBackedEvidenceRecorder
- **THEN** the scenario outcome SHALL have `evidenceCount > 0`
- **AND** recorded snapshots SHALL be readable from the evidence file after completion
