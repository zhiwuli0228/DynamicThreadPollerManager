# adjustment-history

## Purpose

Thread-safe storage and query of adjustment records with success/failure classification.

## ADDED Requirements

### Requirement: HistoryEntry Record

The system SHALL provide `HistoryEntry` record with decision, result, beforeClassification, afterClassification, recordedAt fields.

### Requirement: AdjustmentHistory Class

The system SHALL provide thread-safe `AdjustmentHistory` with record/query/success-count operations.

#### Scenario: Record and retrieve recent entries
- **GIVEN** an empty AdjustmentHistory
- **WHEN** 5 HistoryEntries are recorded
- **THEN** recent(3) returns the 3 most recent entries in order
- **AND** totalAdjustmentCount() returns 5

#### Scenario: Successful adjustment counting
- **GIVEN** entries with before=OVERLOAD→after=RECOVERY (improvement), before=QUEUE_BUILDUP→after=NORMAL (improvement), before=NORMAL→after=QUEUE_BUILDUP (degradation)
- **WHEN** successfulAdjustmentCount() is called
- **THEN** returns 2 (the two improvements)

#### Scenario: NORMAL→NORMAL counts as success
- **GIVEN** an entry with before=NORMAL→after=NORMAL
- **WHEN** successfulAdjustmentCount() is called
- **THEN** this entry is counted as success (maintaining steady state)

#### Scenario: Thread-safe concurrent record
- **GIVEN** 2 threads recording entries concurrently
- **WHEN** both threads complete
- **THEN** totalAdjustmentCount() equals the sum of both threads' entries (no lost updates)

#### Scenario: Clear empties history
- **GIVEN** entries recorded
- **WHEN** clear() is called
- **THEN** totalAdjustmentCount() returns 0 and isEmpty() returns true
