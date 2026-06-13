## 1. LivePressureSamplerConfig

- [ ] 1.1 Create `LivePressureSamplerConfig` record (pollIntervalMs, autoStart, sessionId)
- [ ] 1.2 Implement constructor validation: pollIntervalMs >= 100
- [ ] 1.3 Implement `defaults(String sessionId)` static factory (pollIntervalMs=1000, autoStart=false)
- [ ] 1.4 Write unit test: valid config construction
- [ ] 1.5 Write unit test: pollIntervalMs < 100 throws IllegalArgumentException
- [ ] 1.6 Write unit test: defaults() returns expected values

## 2. LivePressureSampler

- [ ] 2.1 Implement constructor `(ManagedExecutor, EvidenceRecorder, SnapshotAssembler, LivePressureSamplerConfig)`
- [ ] 2.2 Implement convenience constructor `(ManagedExecutor, EvidenceRecorder, LivePressureSamplerConfig)` — uses DefaultSnapshotAssembler
- [ ] 2.3 Implement `start(String runId)` — AtomicBoolean.compareAndSet, scheduleWithFixedDelay, daemon thread
- [ ] 2.4 Implement sampling lambda: RuntimeObservation.fromExecutor → assembler.assemble → recorder.record; reset failure counter on success
- [ ] 2.5 Implement circuit breaker: AtomicInteger consecutiveFailures, MAX_CONSECUTIVE_FAILURES=10, auto-stop
- [ ] 2.6 Implement `stop()` — AtomicBoolean.compareAndSet, scheduler.shutdown, awaitTermination(10s), shutdownNow
- [ ] 2.7 Implement `isRunning()` — AtomicBoolean.get
- [ ] 2.8 Implement `sample(String, RuntimeObservation, Instant)` — withTimestamp(at), assemble, record
- [ ] 2.9 Write unit test: start → wait for samples → stop → verify snapshots recorded
- [ ] 2.10 Write unit test: double start throws IllegalStateException
- [ ] 2.11 Write unit test: double stop is idempotent (no exception)
- [ ] 2.12 Write unit test: manual sample() works while autonomous sampling is running
- [ ] 2.13 Write unit test: stop() correctly stops — no new samples after stop
- [ ] 2.14 Write unit test: circuit breaker triggers after 10 consecutive failures

## 3. ManagedExecutorScenarioRunner Integration

- [ ] 3.1 Add `LivePressureSampler` field (nullable) to ManagedExecutorScenarioRunner
- [ ] 3.2 Modify existing 5-arg constructor to delegate to new 6-arg constructor with liveSampler=null
- [ ] 3.3 Add new 6-arg constructor `(ExperimentCoordinator, ScenarioPlanner, PressureSampler, EvidenceRecorder, Supplier<Instant>, LivePressureSampler)`
- [ ] 3.4 In `run()` Phase 2: if liveSampler != null, call liveSampler.start(runId)
- [ ] 3.5 In `run()` Phase 3: if liveSampler != null, skip manual step-level sampling; else use existing manual sampling
- [ ] 3.6 In `run()` Phase 5: if liveSampler != null, call liveSampler.stop()
- [ ] 3.7 Write integration test: Runner with LivePressureSampler → verify outcome.evidenceCount > 0
- [ ] 3.8 Write regression test: existing 8 ManagedExecutorScenarioRunnerTest tests pass unmodified

## 4. End-to-End Integration Tests

- [ ] 4.1 E2E-1: record 10 snapshots → closeSession → new FileBackedEvidenceRecorder → snapshots() returns 10 → verify data integrity after round-trip
- [ ] 4.2 E2E-2: LivePressureSampler + FileBackedEvidenceRecorder, run 2s @ 200ms → verify snapshots >= 3 (relaxed assertion per IR F06)
- [ ] 4.3 E2E-3: ManagedExecutorScenarioRunner + LivePressureSampler injection → verify outcome.evidenceCount > 0 and evidence file readable
- [ ] 4.4 E2E-4: 4 threads concurrent record() same runId, 100 snapshots each → total 400, no corruption
- [ ] 4.5 E2E-5: start() → 500ms → stop() → wait 1s → verify count unchanged (stop correctly halts scheduling)

## 5. Test Suite Verification

- [ ] 5.1 Run `mvn test` — verify all 535 + change-1-new tests pass with zero failures
- [ ] 5.2 Verify no regression in ManagedExecutorScenarioRunnerTest (8 tests) — all pass with old constructor
- [ ] 5.3 Verify no regression in ManualPressureSampler tests
- [ ] 5.4 Verify no regression in FileBackedEvidenceRecorder tests (from change 1)
- [ ] 5.5 Verify all 5 end-to-end scenarios pass
