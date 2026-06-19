# loop-evidence-recording

## Purpose

Record loop iteration evidence (decision-result-observation triples) for audit and debugging.

## Requirements

### Requirement: InMemoryLoopEvidenceRecorder

The system SHALL provide `InMemoryLoopEvidenceRecorder` implementing `LoopEvidenceRecorder` with thread-safe in-memory storage.

#### Scenario: Record iteration and retrieve
- **GIVEN** an InMemoryLoopEvidenceRecorder
- **WHEN** recordIteration() is called 3 times
- **THEN** getIterationEvidence(sessionId) returns 3 entries

#### Scenario: Record session lifecycle
- **GIVEN** an InMemoryLoopEvidenceRecorder
- **WHEN** recordSessionStart() and recordSessionEnd() are called
- **THEN** both calls succeed without exception

#### Scenario: Thread-safe recording
- **GIVEN** 2 threads recording iterations concurrently
- **WHEN** both complete
- **THEN** all iterations are preserved (no lost data, no ConcurrentModificationException)
