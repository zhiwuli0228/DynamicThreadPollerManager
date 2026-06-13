## Context

Change 1 (`persistent-evidence-recorder`) delivers `FileBackedEvidenceRecorder` and snapshot serialization. Change 2 adds autonomous scheduled sampling and integrates it into the scenario runner, completing the v0.11.0 pipeline from live data collection through persistent recording.

## Goals / Non-Goals

**Goals:**
- Implement `LivePressureSampler` implementing `PressureSampler` in `experiment.metrics`
- Implement `LivePressureSamplerConfig` record with validation
- Add new `ManagedExecutorScenarioRunner` constructor with nullable `LivePressureSampler` injection
- When LivePressureSampler is injected, autonomous sampling runs in background; step-level manual sampling is skipped
- 5 end-to-end integration test scenarios
- Full regression: all 535 + change-1-new tests pass

**Non-Goals:**
- Modifying `PressureSampler` interface
- Modifying `ManualPressureSampler`
- Modifying `ManagedExecutor` or `ManagedExecutorConfig`
- CPU utilization real data source
- Dynamic interval adjustment during sampling

## Decisions

### D1: scheduleWithFixedDelay (not scheduleAtFixedRate)

Fixed delay prevents sampling backlog — if one sample is slow, subsequent samples are deferred rather than stacking up. This matches the decision in the v0.11.0 decision log (D3).

### D2: daemon single-thread executor

Daemon thread prevents JVM shutdown blockage. Single-thread ensures sampling order and avoids concurrent access to the ManagedExecutor (all getters are thread-safe but single-thread access is simpler to reason about).

### D3: AtomicBoolean for running state

`compareAndSet(false, true)` in `start()` prevents TOCTOU race. `compareAndSet(true, false)` in `stop()` makes stop idempotent (SR F04 fix).

### D4: circuit breaker on consecutive failures

`AtomicInteger consecutiveFailures` counter. Reset to 0 on successful sample. Increment on `RuntimeException`. At `MAX_CONSECUTIVE_FAILURES = 10`, auto-call `stop()` to prevent silent infinite error loops (SR F05 fix).

### D5: nullable LivePressureSampler in ManagedExecutorScenarioRunner

Existing 5-arg constructor delegates to new 6-arg constructor with `null` liveSampler. When `liveSampler != null`, Phase 2 starts the sampler, Phase 3 skips manual step-level sampling, Phase 5 stops the sampler. This preserves full backward compatibility.

### D6: sample() manual method still available

`LivePressureSampler.sample(String, RuntimeObservation, Instant)` directly calls assembler and recorder — usable alongside or instead of autonomous scheduling. Timestamp behavior is consistent with `ManualPressureSampler.sample()` (both use `observation.withTimestamp(at)`).

## Risks / Trade-offs

- **Risk**: End-to-end test timing assertions may flake on slow CI. **Mitigation**: use relaxed assertions (`snapshots >= 3` not `== 5`) as decided in IR F06 disposition.
- **Trade-off**: `LivePressureSampler` does not persist its config or running state. Acceptable — it is a runtime component, not a persistent service.
- **Trade-off**: Circuit breaker threshold (10) is hardcoded, not configurable. Acceptable for initial implementation; can be extracted to config if needed.
