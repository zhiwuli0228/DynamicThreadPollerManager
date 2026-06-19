## 1. PressureSnapshot Serialization

- [ ] 1.1 Add `toMap()` instance method — serialize 6 fields with ISO-8601 timestamp
- [ ] 1.2 Add `static fromMap(Map<String, Object>)` factory — Number base class with `.intValue()`/`.longValue()`/`.doubleValue()` for precise type conversion
- [ ] 1.3 Write round-trip unit test: `assertEquals(original, fromMap(toMap(original)))`
- [ ] 1.4 Write 4-field constructor variant round-trip test

## 2. RuntimeObservation Serialization and Factory

- [ ] 2.1 Add `toMap()` instance method — each MetricValue as `{"status": "PRESENT"/"ABSENT", "value": ...}`
- [ ] 2.2 Add `static fromMap(Map<String, Object>)` factory — targetType-driven Number conversion
- [ ] 2.3 Add `static fromExecutor(ManagedExecutor, Instant)` static factory — read 7 metrics, cpuUtilization=absent
- [ ] 2.4 Write round-trip unit test for full 8-metric RuntimeObservation
- [ ] 2.5 Write round-trip unit test for RuntimeObservation with some absent metrics
- [ ] 2.6 Write unit test for `fromExecutor()`: verify all 7 PRESENT fields and cpuUtilization ABSENT
- [ ] 2.7 Delegate `ManagedExecutorScenarioRunner.buildObservation()` to `RuntimeObservation.fromExecutor()`

## 3. ObservedSnapshot Serialization

- [ ] 3.1 Add `toMap()` instance method — nested snapshot and observation maps
- [ ] 3.2 Add `static fromMap(Map<String, Object>)` factory — delegates to PressureSnapshot.fromMap and RuntimeObservation.fromMap
- [ ] 3.3 Write round-trip unit test for ObservedSnapshot with nested components

## 4. AcquisitionJsonWriter.parse()

- [ ] 4.1 Promote AcquisitionJsonWriter from `final class` to `public final class`
- [ ] 4.2 Implement `static Object parse(String json)` — hand-written recursive descent parser
- [ ] 4.3 Write round-trip test: `assertEquals(map, parse(render(map)))` for all JSON types
- [ ] 4.4 Write test: parse empty object `{}`
- [ ] 4.5 Write test: parse nested structures (object in object, array in object, mixed)
- [ ] 4.6 Write test: invalid JSON throws exception (unclosed brace, trailing comma, etc.)
- [ ] 4.7 Write test: parse JSON null, true, false

## 5. AcquisitionReportPaths Extension

- [ ] 5.1 Add `evidenceFileName(String runId)` — returns `"{runId}-evidence.jsonl"` via direct concatenation
- [ ] 5.2 Add `sessionMetadataFileName(String runId)` — returns `"{runId}-session.json"` via direct concatenation
- [ ] 5.3 Add `evidenceFile(Path outputRoot, String runId)` — resolves outputRoot/outputs/reports/v0.11.0/ + evidenceFileName
- [ ] 5.4 Add `sessionMetadataFile(Path outputRoot, String runId)` — resolves outputRoot/outputs/reports/v0.11.0/ + sessionMetadataFileName
- [ ] 5.5 Write unit tests for all 4 new methods

## 6. RecordingSession and RecordingSessionMetadata

- [ ] 6.1 Create `SessionStatus` enum (ACTIVE, CLOSED)
- [ ] 6.2 Implement `RecordingSession` class with sessionId, runId, executorConfig, startedAt, closedAt, snapshotCount, status
- [ ] 6.3 Implement `incrementSnapshotCount()` — throws IllegalStateException if not ACTIVE
- [ ] 6.4 Implement `close()` — returns `RecordingSessionMetadata`, throws if already CLOSED
- [ ] 6.5 Create `RecordingSessionMetadata` record (sessionId, runId, executorConfig, startedAt, closedAt, snapshotCount, status)
- [ ] 6.6 Write unit test: start → increment → close → verify metadata
- [ ] 6.7 Write unit test: double close throws IllegalStateException
- [ ] 6.8 Write unit test: increment on CLOSED session throws IllegalStateException

## 7. FileBackedEvidenceRecorder

- [ ] 7.1 Implement `FileBackedEvidenceRecorder(Path outputRoot, String versionTag)` constructor — delegates to AcquisitionReportPaths.forVersion
- [ ] 7.2 Implement `record(ObservedSnapshot)` — write to memory buffer first, then append JSON Lines to file; guard session increment with ACTIVE check
- [ ] 7.3 Implement `snapshots(String runId)` — return unmodifiable list from buffer
- [ ] 7.4 Implement `runIds()` — return unmodifiable set of keys
- [ ] 7.5 Implement `startSession(String runId, ManagedExecutorConfig)` — create RecordingSession with UUID
- [ ] 7.6 Implement `closeSession(String runId)` — close session, write metadata JSON file, return metadata
- [ ] 7.7 Implement `flush(String runId)` — no-op (files written immediately); reserved for future batching
- [ ] 7.8 Implement private `renderSnapshot(ObservedSnapshot)` — toMap → AcquisitionJsonWriter.render
- [ ] 7.9 Implement private `writeSessionMetadata(RecordingSessionMetadata)` — serialize to JSON, write to file
- [ ] 7.10 Write unit test: record single snapshot, verify snapshots() returns it
- [ ] 7.11 Write unit test: record multiple snapshots across different runIds
- [ ] 7.12 Write unit test: concurrent writes from 4 threads, total count correct
- [ ] 7.13 Write unit test: startSession → record → closeSession → verify metadata file
- [ ] 7.14 Write unit test: session metadata JSON round-trip (read file, verify fields)
- [ ] 7.15 Write unit test: duplicate active session throws IllegalStateException
- [ ] 7.16 Write unit test: close non-existent session throws IllegalStateException
- [ ] 7.17 Write unit test: record after session closed does NOT throw (session count not incremented)
- [ ] 7.18 Write unit test: I/O error during file write throws UncheckedIOException, buffer still accessible
- [ ] 7.19 Write unit test: constructor creates output directory

## 8. Test Suite Verification

- [ ] 8.1 Run `mvn test` — verify all 535 existing tests pass with zero modifications
- [ ] 8.2 Verify no regression in InMemoryEvidenceRecorder tests
- [ ] 8.3 Verify no regression in ManagedExecutorScenarioRunnerTest (8 tests)
- [ ] 8.4 Verify no regression in ManualPressureSampler tests
- [ ] 8.5 Verify no regression in AcquisitionReportWriterTest
- [ ] 8.6 Verify no regression in AcquisitionJsonWriter existing tests
