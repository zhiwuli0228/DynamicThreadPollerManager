# oscillation-detection

## Purpose

Detect configuration oscillation patterns (ping-pong, over-adjustment, policy switching) from adjustment history to prevent destructive loop behavior.

## ADDED Requirements

### Requirement: OscillationDetector Class

The system SHALL provide a stateless `OscillationDetector` that checks whether a pending decision would create an oscillation pattern.

#### Scenario: Ping-pong oscillation detected
- **GIVEN** adjustment history with target pool sizes [10, 20, 10, 20] (alternating directions)
- **WHEN** wouldOscillate(pending with target=10, history) is called
- **THEN** returns true (ping-pong pattern detected)

#### Scenario: Over-adjustment detected
- **GIVEN** adjustment history with target pool sizes [10, 15, 20] (3 consecutive scale-ups)
- **WHEN** wouldOscillate(pending with target=25, history) is called
- **THEN** returns true (over-adjustment pattern detected)

#### Scenario: Policy switching oscillation detected
- **GIVEN** adjustment history with selected policies [A, B, A, C]
- **WHEN** wouldOscillate(pending with policy=A, history) is called
- **THEN** returns true (policy switching pattern detected)

#### Scenario: Normal history not detected
- **GIVEN** adjustment history with stable adjustments [10, 15, 15, 15]
- **WHEN** wouldOscillate(pending with target=15, history) is called
- **THEN** returns false

#### Scenario: Empty history
- **GIVEN** an empty AdjustmentHistory
- **WHEN** wouldOscillate(anyDecision, history) is called
- **THEN** returns false

#### Scenario: Insufficient data
- **GIVEN** adjustment history with fewer than windowSize entries
- **WHEN** wouldOscillate(anyDecision, history) is called
- **THEN** returns false (not enough data to detect patterns)

### Requirement: Emergency Stop on Oscillation

The system SHALL trigger EMERGENCY_STOPPED when oscillation is detected emergencyStopThreshold consecutive times.

#### Scenario: Emergency stop after threshold
- **GIVEN** an OscillationDetector that returns true for all decisions
- **AND** emergencyStopThreshold=2
- **WHEN** the loop runs and encounters 2 consecutive oscillation detections
- **THEN** loop state becomes EMERGENCY_STOPPED
- **AND** the session summary contains "oscillation detected"
