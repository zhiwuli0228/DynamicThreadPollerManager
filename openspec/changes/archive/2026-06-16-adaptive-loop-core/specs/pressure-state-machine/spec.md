# pressure-state-machine

## Purpose

Define legal/anomalous/illegal transitions between PressureState values and track transition history.

## ADDED Requirements

### Requirement: TransitionLegality Enum

The system SHALL provide `TransitionLegality` enum with LEGAL, ANOMALOUS, ILLEGAL values.

### Requirement: PressureStateTransition Record

The system SHALL provide `PressureStateTransition` record with from, to, timestamp, trigger, legality fields.

### Requirement: PressureStateMachine Class

The system SHALL provide `PressureStateMachine` with transition table lookup and history tracking.

#### Scenario: Legal transition
- **GIVEN** a PressureStateMachine
- **WHEN** isLegalTransition(NORMAL, QUEUE_BUILDUP) is called
- **THEN** returns LEGAL

#### Scenario: Anomalous transition
- **GIVEN** a PressureStateMachine
- **WHEN** isLegalTransition(OVERLOAD, NORMAL) is called
- **THEN** returns ANOMALOUS (skipped RECOVERY)

#### Scenario: Illegal transition
- **GIVEN** a PressureStateMachine
- **WHEN** isLegalTransition(RECOVERY, OVERLOAD) is called
- **THEN** returns ILLEGAL

#### Scenario: Any-to-NORMAL always legal
- **GIVEN** a PressureStateMachine
- **WHEN** isLegalTransition(anyState, NORMAL) is called for any PressureState
- **THEN** returns LEGAL

#### Scenario: Transition history tracking
- **GIVEN** transitions NORMAL→QUEUE_BUILDUP→OVERLOAD recorded
- **WHEN** currentState() is called
- **THEN** returns OVERLOAD
- **AND** recentTransitions(2) returns [QUEUE_BUILDUP→OVERLOAD, NORMAL→QUEUE_BUILDUP] in order

#### Scenario: Reset clears history
- **GIVEN** 3 transitions recorded
- **WHEN** reset() is called
- **THEN** transitionCount() returns 0 and currentState() returns Optional.empty()
