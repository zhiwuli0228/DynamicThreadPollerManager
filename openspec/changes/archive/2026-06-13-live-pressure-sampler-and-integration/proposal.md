## Why

v0.10.0 completed the three dynamic configuration dimensions (thread count, queue capacity, rejection policy). Sampling still requires manual calls to `PressureSampler.sample()` — there is no autonomous polling. `ManagedExecutorScenarioRunner` has no integration point for a background sampler, so live pressure data collection during scenario runs requires external orchestration. This change adds a scheduled live sampler and integrates it into the scenario runner so data collection becomes autonomous.

## What Changes

**LivePressureSampler — autonomous scheduled polling**
- From: only `ManualPressureSampler` exists, requiring explicit caller invocation
- To: `LivePressureSampler` implements `PressureSampler`, uses `ScheduledExecutorService` with `scheduleWithFixedDelay`, daemon thread, autonomous polling of live `ManagedExecutor`
- Reason: autonomous data collection without caller orchestration
- Impact: non-breaking — new implementation of existing `PressureSampler` interface

**LivePressureSamplerConfig — sampling configuration**
- From: no configuration record for sampling parameters
- To: `LivePressureSamplerConfig` record with `pollIntervalMs`, `autoStart`, `sessionId`; minimum interval 100ms
- Reason: parameterize sampling behavior
- Impact: new record type in `experiment.metrics`

**ManagedExecutorScenarioRunner — LivePressureSampler integration**
- From: single 5-arg constructor, manual sampling only within scenario steps
- To: new 6-arg constructor with nullable `LivePressureSampler` parameter; when injected, autonomous sampling runs in background and step-level manual sampling is skipped
- Reason: enable autonomous data collection during scenario runs without breaking existing callers
- Impact: backward compatible — existing constructor unchanged, delegates to new constructor with `null` liveSampler

**End-to-end persistent recording verification**
- From: no integration tests combining file-backed recording with live sampling
- To: 5 end-to-end test scenarios covering: record→close→re-read round-trip, LivePressureSampler+FileBackedEvidenceRecorder integration, Runner+LivePressureSampler integration, concurrent writes, sampler stop correctness
- Reason: verify the full v0.11.0 pipeline from sampling through persistence to re-read
- Impact: new integration tests only, no production code changes beyond those listed above

## Capabilities

### New Capabilities

- `live-pressure-sampler-and-integration`: scheduled autonomous pressure sampling with LivePressureSampler, scenario runner integration, and end-to-end persistent recording verification

### Modified Capabilities

- `scenario-runner-and-baseline`: ManagedExecutorScenarioRunner gains optional LivePressureSampler injection point; when injected, autonomous sampling replaces step-level manual sampling

## Impact

- New files: `LivePressureSampler.java`, `LivePressureSamplerConfig.java` (in `experiment.metrics`)
- Modified files: `ManagedExecutorScenarioRunner.java` (new constructor, delegate buildObservation)
- New integration tests: 5 end-to-end scenarios + LivePressureSampler unit tests
- Dependency: change 1 (`persistent-evidence-recorder`) must be completed first — FileBackedEvidenceRecorder is used as the EvidenceRecorder in end-to-end tests
- Zero regression: all 535 + change-1-new tests must pass
- No new external dependencies
