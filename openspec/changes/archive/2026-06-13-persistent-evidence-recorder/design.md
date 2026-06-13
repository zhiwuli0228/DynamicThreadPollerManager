## Context

v0.1.0 through v0.10.0 built in-memory metrics, executor management, dynamic configuration, and data acquisition — all backed by `InMemoryEvidenceRecorder`. Snapshots are lost on JVM restart. v0.11.0 SR (`docs/04-development/versions/v0.11.0/20-sr.md`) defines the functional design for persistence, serialization, and session lifecycle. This change (1/2) implements the persistence and serialization foundation.

## Goals / Non-Goals

**Goals:**
- Implement `FileBackedEvidenceRecorder` in `experiment.acquisition` (not `experiment.metrics` — avoids circular dependency)
- Add `toMap()`/`fromMap()` to `PressureSnapshot`, `RuntimeObservation`, `ObservedSnapshot`
- Add `RuntimeObservation.fromExecutor(ManagedExecutor, Instant)` static factory
- Add `AcquisitionJsonWriter.parse(String)` for JSON deserialization, promote class to `public`
- Add `AcquisitionReportPaths` methods for evidence and session file paths (direct concatenation, not `.replace()`)
- Implement `RecordingSession`, `RecordingSessionMetadata`, `SessionStatus` in `experiment.metrics`
- Delegate `ManagedExecutorScenarioRunner.buildObservation()` to `RuntimeObservation.fromExecutor()`
- All 535 existing tests pass with zero changes

**Non-Goals:**
- LivePressureSampler (change 2)
- ManagedExecutorScenarioRunner new constructor (change 2)
- End-to-end integration tests (change 2)
- CPU utilization real data source
- Cross-run aggregation, compression, retention enforcement

## Decisions

### D1: FileBackedEvidenceRecorder in `experiment.acquisition`

To avoid a circular dependency (acquisition → metrics already exists; metrics → acquisition would be new). `AcquisitionJsonWriter` is in acquisition; placing the recorder there leverages existing dependency direction.

### D2: toMap/fromMap on snapshot types, not in AcquisitionJsonWriter

Snapshot types know their own fields. `AcquisitionJsonWriter` handles only generic Map↔JSON conversion. This avoids coupling the JSON writer to snapshot internals.

### D3: hand-written recursive descent parser in AcquisitionJsonWriter.parse()

No external JSON library. Returns `Object` (Map/String/Number/Boolean/List/null). Number strategy: no decimal → Long, has decimal → Double. Callers cast to expected types via `Number.intValue()`/`.longValue()`/`.doubleValue()`.

### D4: JSON Lines format (.jsonl)

One complete JSON object per line. Supports streaming append via `Files.writeString(..., CREATE, APPEND)`. File naming: `{runId}-evidence.jsonl`, `{runId}-session.json` (consistent with existing `AcquisitionReportPaths` convention).

### D5: write-before-file error handling

`record()` writes to in-memory buffer first (`ConcurrentHashMap` + `CopyOnWriteArrayList`), then appends to file. If file I/O fails, throws `UncheckedIOException` — in-memory data remains available. Session counter incremented only when session is ACTIVE (SR F01 fix).

### D6: FileBackedEvidenceRecorder constructor accepts version tag

Constructor: `FileBackedEvidenceRecorder(Path outputRoot, String versionTag)`. Delegates to `AcquisitionReportPaths.forVersion(versionTag)` for directory resolution and file path construction (SR F02/F03 fix).

## Risks / Trade-offs

- **Risk**: JSON Lines append is not atomic — partial line written on crash. **Mitigation**: in-memory buffer is the authoritative source; file is a durability copy. Re-read on startup can truncate trailing partial line.
- **Risk**: `parse()` is a hand-written parser — potential bugs in edge cases. **Mitigation**: comprehensive round-trip tests for all JSON types.
- **Trade-off**: `RecordingSession` tracks snapshot count but does not enforce consistency with actual file contents. Acceptable — session metadata is advisory, not transactional.
