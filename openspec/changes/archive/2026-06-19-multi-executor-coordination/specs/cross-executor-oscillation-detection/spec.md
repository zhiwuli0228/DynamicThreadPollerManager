# Cross-Executor Oscillation Detection

## Overview
Detects oscillation patterns spanning multiple executors. Advisory only — does not block adjustments.

## ADDED Requirements

### Requirement: Lockstep Counter-Adjustment Detection
CrossExecutorOscillationDetector SHALL detect when two executors repeatedly adjust in opposite directions.

#### Scenario: Lockstep pattern detected
- GIVEN history: exec-A scale-up, exec-B scale-down, exec-A scale-up, exec-B scale-down
- WHEN wouldCrossOscillate(pending scale-up for exec-A) is called
- THEN returns true

#### Scenario: Normal alternating workload not flagged
- GIVEN history: exec-A scale-up, exec-A scale-down (normal single-executor pattern)
- WHEN wouldCrossOscillate(pending) is called
- THEN returns false

### Requirement: Resource Ping-Pong Detection
CrossExecutorOscillationDetector SHALL detect when the same resource units cycle between two executors.

#### Scenario: Ping-pong detected
- GIVEN history: exec-A claims 3, exec-A releases 3, exec-B claims 3, exec-B releases 3, exec-A claims 3 (repeated >= 3 times)
- WHEN wouldCrossOscillate() is called
- THEN returns true

### Requirement: Priority Thrashing Detection
CrossExecutorOscillationDetector SHALL detect when the same executor is repeatedly preempted.

#### Scenario: Thrashing detected
- GIVEN history: exec-A (HIGH) preempts exec-B (LOW) >= 3 times in window
- WHEN wouldCrossOscillate() is called
- THEN returns true

### Requirement: Empty History Safe
CrossExecutorOscillationDetector SHALL return false for empty or insufficient history.

#### Scenario: Empty history
- GIVEN empty GroupCoordinationHistory
- WHEN wouldCrossOscillate() is called
- THEN returns false
