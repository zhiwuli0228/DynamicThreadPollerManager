## Why

v0.1.0 delivered in-memory evidence recording via `InMemoryEvidenceRecorder`, but recorded snapshots are lost on JVM restart. `PressureSnapshot`, `RuntimeObservation`, and `ObservedSnapshot` have no serialization format. `AcquisitionJsonWriter` can render JSON but cannot parse it back. There is no recording session lifecycle to bound data collection windows. This change adds persistence, serialization round-trip, and session management so evidence survives restarts and can be re-read by offline analysis tools.

## What Changes

**EvidenceRecorder — new file-backed implementation**
- From: only `InMemoryEvidenceRecorder` (ConcurrentHashMap-backed, data lost on restart)
- To: `FileBackedEvidenceRecorder` implements `EvidenceRecorder`, appends snapshots as JSON Lines to disk, maintains in-memory buffer for query
- Reason: durability requirement
- Impact: non-breaking — new implementation of existing interface; `InMemoryEvidenceRecorder` unchanged

**Snapshot types — Map serialization**
- From: `PressureSnapshot`, `RuntimeObservation`, `ObservedSnapshot` have no `toMap()`/`fromMap()` methods
- To: each snapshot type provides `toMap()` → `Map<String, Object>` and `static fromMap(Map<String, Object>)` factory
- Reason: enable JSON round-trip without coupling AcquisitionJsonWriter to snapshot internals
- Impact: non-breaking — additive methods only, existing constructors and accessors unchanged

**RuntimeObservation — executor observation factory**
- From: `buildObservation(ManagedExecutor, Instant)` is a private method in `ManagedExecutorScenarioRunner`
- To: `RuntimeObservation.fromExecutor(ManagedExecutor, Instant)` static factory, reusable by any caller
- Reason: LivePressureSampler needs the same logic without duplicating code
- Impact: `ManagedExecutorScenarioRunner.buildObservation()` delegates to this factory; behavior identical

**AcquisitionJsonWriter — JSON parser**
- From: write-only (`render(Object)`, `map()`), package-private class
- To: `public final class` with new `static Object parse(String)` method for JSON deserialization
- Reason: round-trip serialization requires both read and write
- Impact: non-breaking — existing `render()` and `map()` unchanged; visibility promoted to public

**AcquisitionReportPaths — evidence and session file paths**
- From: no evidence or session metadata file paths
- To: `evidenceFileName(String)`, `sessionMetadataFileName(String)`, `evidenceFile(Path, String)`, `sessionMetadataFile(Path, String)`
- Reason: centralized path management for evidence files
- Impact: non-breaking — additive methods only

**RecordingSession — session lifecycle**
- From: no session concept — recordings are unbounded
- To: `RecordingSession` (ACTIVE/CLOSED lifecycle, snapshot counter), `RecordingSessionMetadata` (immutable record), `SessionStatus` enum
- Reason: bound data collection windows and capture executor config at session start
- Impact: new types in `experiment.metrics`, no modification to existing types

## Capabilities

### New Capabilities

- `persistent-evidence-recorder`: file-backed evidence recording with JSON Lines persistence, snapshot JSON serialization round-trip via toMap/fromMap/parse, recording session lifecycle management

### Modified Capabilities

None — this change is purely additive.

## Impact

- New files: `FileBackedEvidenceRecorder.java` (in `experiment.acquisition`), `RecordingSession.java`, `RecordingSessionMetadata.java`, `SessionStatus.java` (in `experiment.metrics`)
- Modified files: `PressureSnapshot.java`, `RuntimeObservation.java`, `ObservedSnapshot.java` (add `toMap`/`fromMap`), `AcquisitionJsonWriter.java` (add `parse()`, promote to `public`), `AcquisitionReportPaths.java` (add 4 methods), `ManagedExecutorScenarioRunner.java` (delegate `buildObservation()` to `RuntimeObservation.fromExecutor()`)
- Zero regression: all 535 existing tests must pass unchanged
- No new external dependencies
