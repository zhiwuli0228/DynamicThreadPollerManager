# closed-loop-controller

## Purpose

Autonomous adjustment loop that orchestrates sampling→classification→scoring→selection→evaluation→safety→application→recording in a continuous cycle.

## ADDED Requirements

### Requirement: AdjustmentLoop Lifecycle

The system SHALL provide `AdjustmentLoop` with start/pause/resume/stop/emergencyStop/reset lifecycle methods.

#### Scenario: Full lifecycle
- **GIVEN** an AdjustmentLoop in IDLE
- **WHEN** start() → pause() → resume() → stop() are called in sequence
- **THEN** state transitions: IDLE→RUNNING→PAUSED→RUNNING→STOPPED with no exceptions

#### Scenario: Illegal start from RUNNING
- **GIVEN** an AdjustmentLoop in RUNNING state
- **WHEN** start() is called
- **THEN** IllegalStateException is thrown

#### Scenario: Reset from STOPPED
- **GIVEN** an AdjustmentLoop in STOPPED state with entries in history
- **WHEN** reset() is called
- **THEN** state transitions to IDLE, history is empty, stateMachine is reset

### Requirement: AdjustmentLoop Main Loop

The system SHALL execute a 16-step main loop per SR §4.10 when in RUNNING state.

#### Scenario: NO_OP decision skips adjustment
- **GIVEN** a mock DecisionOrchestrator that returns NO_OP decisions
- **WHEN** the loop runs for 3 iterations
- **THEN** adjustment history has 0 entries (all were NO_OP, no adapter.apply() called)

#### Scenario: SafetyGate REJECTED continues loop
- **GIVEN** a mock SafetyGate that returns REJECTED
- **AND** a mock orchestrator that returns non-NO_OP decisions
- **WHEN** the loop runs for 3 iterations
- **THEN** loop continues running (not stopped), adapter.apply() is never called

#### Scenario: Max iterations reached
- **GIVEN** LoopConfig with maxIterations=3
- **WHEN** the loop runs
- **THEN** after 3 iterations, state is STOPPED

#### Scenario: Runtime exception per iteration does not crash loop
- **GIVEN** a mock orchestrator that throws RuntimeException on iteration 2
- **WHEN** the loop runs for 3 iterations
- **THEN** loop completes normally, iteration 2 is recorded as failed, iterations 1 and 3 proceed

### Requirement: SafetyGate Integration

The system SHALL construct a runtime ReadinessAssessment and pass it to SafetyGate.evaluate() with 3-arg signature.

#### Scenario: Runtime readiness assessment
- **GIVEN** an AdjustmentLoop starting
- **WHEN** start() is called
- **THEN** a ReadinessAssessment with status=READY and configLabel="runtime-loop" is constructed
- **AND** it is passed to safetyGate.evaluate() on each iteration

#### Scenario: recordApplied after successful adjustment
- **GIVEN** a SafetyGate that returns ALLOW
- **WHEN** adapter.apply() succeeds
- **THEN** safetyGate.recordApplied() is called with the gate decision

### Requirement: AfterClassification Tracking

The system SHALL correctly track before/after pressure classifications across iterations.

#### Scenario: Before/after classification recording
- **GIVEN** loop running with real classifier
- **WHEN** 3 iterations produce classifications C1, C2, C3
- **THEN** HistoryEntry[0]: before=C1, after=C2
- **AND** HistoryEntry[1]: before=C2, after=C3

### Requirement: DecisionOrchestrator Rebuild on Calibration

The system SHALL rebuild the DecisionOrchestrator when FeedbackCalibrator produces new weights.

#### Scenario: Orchestrator replaced after calibration
- **GIVEN** a calibrator that returns a different scorer
- **WHEN** calibration is triggered (adjustment count % window == 0)
- **THEN** AdjustLoop's orchestrator is replaced with a new instance using the new scorer
