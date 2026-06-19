# adaptive-loop-core

## Purpose

Closed-loop adjustment controller that orchestrates the full pipeline: sample → classify → score → select → evaluate → safety-check → apply → observe. Provides loop lifecycle management, decision orchestration, pressure state transition tracking, and thread-safe adjustment history.

## Requirements

### Requirement: LoopState Enum

The system SHALL provide a `LoopState` enum with values IDLE, RUNNING, PAUSED, STOPPED, EMERGENCY_STOPPED.

#### Scenario: All enum values present
- **GIVEN** the LoopState enum exists
- **WHEN** LoopState.values() is called
- **THEN** it returns exactly [IDLE, RUNNING, PAUSED, STOPPED, EMERGENCY_STOPPED]

#### Scenario: Legal state transition
- **GIVEN** an AdjustmentLoop in IDLE state
- **WHEN** start() is called
- **THEN** state transitions to RUNNING and no exception is thrown

#### Scenario: Illegal state transition throws
- **GIVEN** an AdjustmentLoop in RUNNING state
- **WHEN** start() is called
- **THEN** IllegalStateException is thrown

### Requirement: LoopConfig Record

The system SHALL provide a `LoopConfig` record with 8 validated configuration fields and a `defaults()` factory method. Cooldown is delegated to `RuntimeAdjustmentSafetyGate`.

### Requirement: LoopSession Record

The system SHALL provide a `LoopSession` record with `Optional<Instant>` endTime, `started()` and `ended()` factory methods.

### Requirement: AdjustmentDecision Record

The system SHALL provide an `AdjustmentDecision` record with nullable `selectedScore` and `selectedPolicy` (null only for NO_OP decisions where `policyDecision.action() == HOLD`).

#### Scenario: NO_OP decision
- **GIVEN** a PolicyDecision with HOLD action
- **WHEN** AdjustmentDecision is constructed with null selectedScore and null selectedPolicy
- **THEN** construction succeeds (no NPE) and isNoOp() returns true

### Requirement: DecisionOrchestrator Class

The system SHALL provide an immutable `DecisionOrchestrator` executing classify→rank→find-config→construct-input→evaluate→assemble pipeline. Uses `lastSnapshot.timestamp()` for PolicyEvaluationInput (not wall-clock).

#### Scenario: OVERLOAD state selects aggressive policy
- **GIVEN** snapshots showing OVERLOAD conditions and 3 candidate policies (conservative/moderate/aggressive)
- **WHEN** decide() is called
- **THEN** the selected policy has the highest responsivenessScore

#### Scenario: Empty snapshots returns NO_OP
- **GIVEN** an empty snapshot list
- **WHEN** decide() is called
- **THEN** the returned AdjustmentDecision has isNoOp() == true

### Requirement: PressureStateMachine Class

The system SHALL provide `PressureStateMachine` with LEGAL/ANOMALOUS/ILLEGAL transition tables. "Any state → NORMAL" is always LEGAL (takes precedence over ANOMALOUS table entries).

#### Scenario: Legal transition
- **GIVEN** a PressureStateMachine
- **WHEN** isLegalTransition(NORMAL, QUEUE_BUILDUP) is called
- **THEN** returns LEGAL

#### Scenario: Any-to-NORMAL always legal
- **GIVEN** any PressureState
- **WHEN** isLegalTransition(state, NORMAL) is called
- **THEN** returns LEGAL (including OVERLOAD→NORMAL and REJECTION_ACTIVE→NORMAL)

#### Scenario: Illegal transition
- **GIVEN** a PressureStateMachine
- **WHEN** isLegalTransition(RECOVERY, OVERLOAD) is called
- **THEN** returns ILLEGAL

### Requirement: AdjustmentHistory Class

The system SHALL provide thread-safe `AdjustmentHistory` using `CopyOnWriteArrayList`. Success is determined by PressureState ordinal comparison (higher ordinal = lower pressure = improvement). NORMAL→NORMAL counts as success.

#### Scenario: Thread-safe concurrent record
- **GIVEN** 2 threads recording entries concurrently
- **WHEN** both threads complete
- **THEN** totalAdjustmentCount() equals the sum of both threads' entries

### Requirement: AdjustmentLoop Lifecycle

The system SHALL provide `AdjustmentLoop` with start/pause/resume/stop/emergencyStop/reset lifecycle methods.

#### Scenario: Full lifecycle
- **GIVEN** an AdjustmentLoop in IDLE
- **WHEN** start() → pause() → resume() → stop() are called in sequence
- **THEN** state transitions: IDLE→RUNNING→PAUSED→RUNNING→STOPPED with no exceptions

#### Scenario: Reset from STOPPED
- **GIVEN** an AdjustmentLoop in STOPPED state with entries in history
- **WHEN** reset() is called
- **THEN** state transitions to IDLE, history is empty, stateMachine is reset

#### Scenario: Emergency stop
- **GIVEN** an AdjustmentLoop in RUNNING state
- **WHEN** emergencyStop("reason") is called
- **THEN** state is EMERGENCY_STOPPED and session summary contains "reason"

### Requirement: SafetyGate Integration

The system SHALL construct a runtime `ReadinessAssessment` with status=READY and pass it to `SafetyGate.evaluate(command, executorState, readiness)` with the correct 3-argument signature. After successful adjustment, `safetyGate.recordApplied()` is called to maintain cooldown state.

### Requirement: Oscillation Detection and Feedback Calibration (Stubs)

Change 1 provides stub implementations: `OscillationDetector` always returns false (no oscillation), `FeedbackCalibrator` always returns the same scorer instance. Full implementations are provided in Change 2 (`oscillation-guard-and-loop-verification`).
