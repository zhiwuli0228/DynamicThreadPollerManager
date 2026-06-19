# adaptive-loop-lifecycle

## Purpose

Define the closed-loop lifecycle state machine with 5 states and controlled transitions, plus loop configuration and session tracking.

## ADDED Requirements

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

The system SHALL provide a `LoopConfig` record with 8 validated configuration fields and a `defaults()` factory method.

#### Scenario: Valid construction
- **GIVEN** all fields within valid ranges
- **WHEN** LoopConfig is constructed
- **THEN** no exception is thrown and all field values are preserved

#### Scenario: Invalid sampling interval
- **GIVEN** samplingIntervalMs < 100
- **WHEN** LoopConfig is constructed
- **THEN** IllegalArgumentException is thrown

#### Scenario: Empty candidate policies
- **GIVEN** an empty candidatePolicies list
- **WHEN** LoopConfig is constructed
- **THEN** IllegalArgumentException is thrown

### Requirement: LoopSession Record

The system SHALL provide a `LoopSession` record tracking a loop run's complete information from start to end.

#### Scenario: Started session
- **GIVEN** a LoopConfig
- **WHEN** LoopSession.started(config) is called
- **THEN** sessionId is a non-blank UUID, endTime is Optional.empty(), adjustmentCount=0, iterationCount=0, finalState=RUNNING

#### Scenario: Ended session
- **GIVEN** a running LoopSession
- **WHEN** session.ended(STOPPED, 3, 10, "summary") is called
- **THEN** endTime is Optional.of(...), adjustmentCount=3, iterationCount=10, finalState=STOPPED
