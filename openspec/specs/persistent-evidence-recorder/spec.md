## ADDED Requirements

### Requirement: FileBackedEvidenceRecorder MUST persist ObservedSnapshot as JSON Lines

The system MUST provide a file-backed implementation of `EvidenceRecorder` that appends each recorded `ObservedSnapshot` as a single JSON Lines entry to a per-runId evidence file, while maintaining an in-memory buffer for query access.

#### Scenario: Record single snapshot
- **WHEN** `FileBackedEvidenceRecorder.record(snapshot)` is called with a valid `ObservedSnapshot`
- **THEN** the snapshot SHALL be added to the in-memory buffer for the snapshot's runId
- **AND** the snapshot SHALL be appended as a JSON Lines entry to the evidence file for that runId
- **AND** if a session is ACTIVE for the runId, its snapshot count SHALL be incremented
- **AND** if no session exists or the session is not ACTIVE, recording SHALL still succeed

#### Scenario: Query recorded snapshots
- **WHEN** `snapshots(runId)` is called
- **THEN** the system SHALL return an unmodifiable list of all snapshots recorded for that runId
- **AND** return an empty list if no snapshots exist for the runId

#### Scenario: List run IDs
- **WHEN** `runIds()` is called
- **THEN** the system SHALL return an unmodifiable set of all runId values with recorded snapshots

#### Scenario: Concurrent writes
- **WHEN** multiple threads concurrently call `record()` for the same runId
- **THEN** all snapshots SHALL be present in the buffer (no lost entries)
- **AND** no data corruption SHALL occur in the evidence file

#### Scenario: I/O error during file write
- **WHEN** `record()` fails to write to the evidence file due to an IOException
- **THEN** an `UncheckedIOException` SHALL be thrown
- **AND** the snapshot SHALL still be available in the in-memory buffer

#### Scenario: Constructor creates output directory
- **WHEN** `FileBackedEvidenceRecorder` is constructed with an output root and version tag
- **THEN** the versioned output directory SHALL be created if it does not exist
- **AND** an `UncheckedIOException` SHALL be thrown if directory creation fails

---

### Requirement: Snapshot types MUST support Map serialization round-trip

The system MUST provide `toMap()` and `static fromMap(Map<String, Object>)` methods on `PressureSnapshot`, `RuntimeObservation`, and `ObservedSnapshot` such that `fromMap(toMap(obj)).equals(obj)` holds for valid snapshots.

#### Scenario: PressureSnapshot round-trip
- **WHEN** `PressureSnapshot.toMap()` is called on a valid 6-field snapshot
- **THEN** the resulting map SHALL contain all 6 fields with correct types
- **AND** `PressureSnapshot.fromMap(map)` SHALL reconstruct an equal snapshot
- **AND** timestamp SHALL be serialized as ISO-8601 string, deserialized via `Instant.parse()`
- **AND** numeric fields SHALL be read via `Number` base class with `.intValue()`/`.longValue()`/`.doubleValue()` for precise conversion

#### Scenario: RuntimeObservation round-trip
- **WHEN** `RuntimeObservation.toMap()` is called on a valid observation
- **THEN** each `MetricValue` SHALL be represented as `{"status": "PRESENT"/"ABSENT", "value": ...}`
- **AND** `RuntimeObservation.fromMap(map)` SHALL reconstruct an equal observation
- **AND** PRESENT values SHALL be converted to the correct target type (Integer/Long/Double) from JSON Number

#### Scenario: ObservedSnapshot round-trip
- **WHEN** `ObservedSnapshot.toMap()` is called
- **THEN** the map SHALL contain nested `snapshot` and `observation` maps
- **AND** `ObservedSnapshot.fromMap(map)` SHALL reconstruct an equal snapshot with correct nested PressureSnapshot and RuntimeObservation

---

### Requirement: RuntimeObservation MUST provide executor observation factory

The system MUST provide a static factory method `RuntimeObservation.fromExecutor(ManagedExecutor, Instant)` that reads all available metrics from a live executor and returns a fully-populated RuntimeObservation.

#### Scenario: Read all available executor metrics
- **WHEN** `RuntimeObservation.fromExecutor(executor, timestamp)` is called with a valid ManagedExecutor
- **THEN** the returned observation SHALL have PRESENT values for: activeThreads, poolSize, queueSize, completedTaskCount, keepAliveTimeSeconds, largestPoolSize, taskCount
- **AND** cpuUtilization SHALL be ABSENT
- **AND** the observation timestamp SHALL equal the provided timestamp

#### Scenario: ManagedExecutorScenarioRunner delegates to factory
- **WHEN** `ManagedExecutorScenarioRunner.buildObservation(executor, now)` is called
- **THEN** it SHALL delegate to `RuntimeObservation.fromExecutor(executor, now)` and return the result

---

### Requirement: AcquisitionJsonWriter MUST support JSON deserialization

The system MUST extend `AcquisitionJsonWriter` (promoted to `public final class`) with a `static Object parse(String)` method that parses a JSON string into a Java object graph of Map, List, String, Number, Boolean, and null.

#### Scenario: Parse all JSON types
- **WHEN** `parse(json)` is called with a valid JSON string
- **THEN** the method SHALL return the corresponding Java object graph
- **AND** JSON objects SHALL become `Map<String, Object>`
- **AND** JSON arrays SHALL become `List<Object>`
- **AND** JSON numbers without decimal point SHALL become `Long`
- **AND** JSON numbers with decimal point SHALL become `Double`
- **AND** JSON strings SHALL become `String`
- **AND** JSON true/false SHALL become `Boolean`
- **AND** JSON null SHALL become Java `null`

#### Scenario: Render-parse round-trip
- **WHEN** a Map is rendered via `render(map)` and the resulting JSON is parsed via `parse(json)`
- **THEN** the result SHALL equal the original map

#### Scenario: Invalid JSON
- **WHEN** `parse(json)` is called with malformed JSON
- **THEN** the method SHALL throw an exception

---

### Requirement: AcquisitionReportPaths MUST provide evidence and session file paths

The system MUST extend `AcquisitionReportPaths` with static methods for evidence file and session metadata file naming and path resolution, using direct concatenation for file naming.

#### Scenario: Evidence file name
- **WHEN** `evidenceFileName("run-001")` is called
- **THEN** the result SHALL be `"run-001-evidence.jsonl"`

#### Scenario: Session metadata file name
- **WHEN** `sessionMetadataFileName("run-001")` is called
- **THEN** the result SHALL be `"run-001-session.json"`

#### Scenario: Evidence file path
- **WHEN** `evidenceFile(outputRoot, "run-001")` is called
- **THEN** the result SHALL be `outputRoot/outputs/reports/v0.11.0/run-001-evidence.jsonl`

---

### Requirement: RecordingSession MUST manage recording session lifecycle

The system MUST provide `RecordingSession` with ACTIVE/CLOSED lifecycle, snapshot counting, and immutable `RecordingSessionMetadata` produced on close.

#### Scenario: Start session
- **WHEN** a `RecordingSession` is created with sessionId, runId, and executorConfig
- **THEN** its status SHALL be ACTIVE
- **AND** its snapshotCount SHALL be 0
- **AND** its startedAt SHALL be set to the current time

#### Scenario: Increment snapshot count
- **WHEN** `incrementSnapshotCount()` is called on an ACTIVE session
- **THEN** snapshotCount SHALL increase by 1

#### Scenario: Close session
- **WHEN** `close()` is called on an ACTIVE session
- **THEN** status SHALL become CLOSED
- **AND** closedAt SHALL be set to the current time
- **AND** a `RecordingSessionMetadata` record SHALL be returned with all session fields

#### Scenario: Double close throws
- **WHEN** `close()` is called on an already CLOSED session
- **THEN** an `IllegalStateException` SHALL be thrown

#### Scenario: Increment on closed session throws
- **WHEN** `incrementSnapshotCount()` is called on a CLOSED session
- **THEN** an `IllegalStateException` SHALL be thrown

---

### Requirement: FileBackedEvidenceRecorder MUST manage sessions

The system MUST provide `startSession(runId, config)` and `closeSession(runId)` for recording session lifecycle management.

#### Scenario: Start session
- **WHEN** `startSession(runId, config)` is called for a runId with no existing ACTIVE session
- **THEN** a new `RecordingSession` SHALL be created with a UUID sessionId
- **AND** subsequent `record()` calls for that runId SHALL increment the session snapshot count

#### Scenario: Duplicate active session throws
- **WHEN** `startSession(runId, config)` is called for a runId that already has an ACTIVE session
- **THEN** an `IllegalStateException` SHALL be thrown

#### Scenario: Close session
- **WHEN** `closeSession(runId)` is called for a runId with an ACTIVE session
- **THEN** the session SHALL be closed
- **AND** a session metadata JSON file SHALL be written
- **AND** the returned `RecordingSessionMetadata` SHALL reflect the final state

#### Scenario: Close non-existent session throws
- **WHEN** `closeSession(runId)` is called for a runId with no session
- **THEN** an `IllegalStateException` SHALL be thrown
