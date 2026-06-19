## Context

`experiment-foundation` delivered lifecycle and shared model contracts. `metrics-snapshot-and-recording` delivered read-only pressure observation and append-only evidence recording. The next required layer is repeatable workload execution with a fixed baseline.

This design is intentionally explicit because downstream implementation agents may be weak. The implementation should be small, synchronous, and test-first.

## Goals / Non-Goals

**Goals:**

- Define deterministic scenario inputs.
- Generate ordered scenario steps for `STEADY`, `RAMP`, and `BURST`.
- Provide fixed baseline executor configuration.
- Execute a scenario without adaptive control.
- Record metrics evidence during the run.
- Return a minimal `ScenarioRunOutcome`.
- Add tests that map directly to every spec scenario.

**Non-Goals:**

- No adaptive policy.
- No scale decisions.
- No executor resizing.
- No queue capacity mutation.
- No external API.
- No persistence.
- No production benchmark realism.
- No new dependencies.

## Decisions

### 1. Scenario package boundary

Create:

```text
com.zhiwu.dynamicthreadpollermanager.experiment.scenario
```

Recommended files:

- `ScenarioProfile.java`
- `ScenarioDefinition.java`
- `ScenarioStep.java`
- `ScenarioPlan.java`
- `ScenarioPlanner.java`
- `DeterministicScenarioPlanner.java`
- `BaselineExecutorPreset.java`
- `BaselineWorkloadExecutor.java`
- `ScenarioRunOutcome.java`
- `ScenarioExperimentRunner.java`

### 2. Deterministic scenario models

`ScenarioDefinition` should be immutable and validate:

- `scenarioId` non-null and non-blank,
- `profile` non-null,
- `stepCount > 0`,
- `baseWorkUnits > 0`.

Minimum constructor:

```java
new ScenarioDefinition(
    "steady-small",
    ScenarioProfile.STEADY,
    42L,
    5,
    10,
    "Small steady workload"
)
```

### 3. Scenario profiles

Initial enum:

```java
STEADY, RAMP, BURST
```

Expected behavior:

- `STEADY`: all steps use `baseWorkUnits`.
- `RAMP`: step `i` uses `baseWorkUnits + i`.
- `BURST`: deterministic spike every third step; suggested formula: if `i % 3 == 0`, `baseWorkUnits * 3`, otherwise `baseWorkUnits`.

Use zero-based indexes internally or one-based indexes consistently. Tests must make the chosen convention explicit.

### 4. Baseline executor remains fixed

`BaselineExecutorPreset` validates fixed sizing:

- `policyId`, suggested default: `baseline-fixed`,
- `corePoolSize > 0`,
- `maximumPoolSize >= corePoolSize`,
- `queueCapacity >= 0`.

`BaselineWorkloadExecutor` should not resize. For the first implementation, it can be synchronous and count completed work units. If it uses a real executor, it must expose deterministic shutdown and avoid sleep-based tests.

### 5. Runner orchestration

`ScenarioExperimentRunner` should depend on:

- `ExperimentCoordinator`,
- `ScenarioPlanner`,
- `BaselineWorkloadExecutor`,
- `PressureSampler`,
- `EvidenceRecorder`,
- a deterministic timestamp supplier or clock-like function.

Suggested run flow:

1. Build plan from definition.
2. Create run using `scenarioId` and `baselinePreset.policyId`.
3. Start run.
4. For each scenario step:
   - execute step against baseline executor,
   - build `RuntimeObservation` from executor state,
   - sample via `PressureSampler`,
   - record via `EvidenceRecorder`.
5. Stop run.
6. Finalize run.
7. Return `ScenarioRunOutcome`.

### 6. RuntimeObservation mapping

The runner should populate:

- active threads,
- pool size,
- queue size,
- completed task count,
- CPU utilization absent unless safely available.

### 7. Boundary isolation

Scenario package must not reference:

- `ControlPolicy`,
- `ScaleDecision`,
- `AdjustmentEvent`,
- `.policy.`,
- `adaptive`,
- executor mutation adapter names.

Using `policyId` as a string for the fixed baseline is allowed because `ExperimentRun` requires a policy identity.

## Risks / Trade-offs

- [Risk] A synchronous executor is less realistic. -> [Mitigation] This change optimizes repeatability; real executor realism can be added later.
- [Risk] Runner may accidentally become policy-aware. -> [Mitigation] boundary tests ban policy and mutation references.
- [Risk] Scenario profile logic may become too clever. -> [Mitigation] use simple formulas and explicit tests.
- [Risk] Tests may use real time. -> [Mitigation] use deterministic timestamps supplied by tests.

## Migration Plan

This is additive.

Implementation order:

1. Scenario models and validation.
2. Deterministic planner.
3. Baseline preset and workload executor.
4. Runner orchestration.
5. Boundary tests and full Maven suite.

Rollback strategy:

- If runner orchestration grows too complex, keep model/planner and defer execution integration.
- If real executor behavior is unstable, replace it with a synchronous work counter.

## Open Questions

- Should `ScenarioRunOutcome` include final `RunState` or a boolean `finalized`? Either is acceptable if tests assert finalized outcome.
- Should burst profile use every third step or seed-derived spikes? Prefer every third step for first implementation.
