# v0.2.0 Solution Design

## Header

- Version name: `v0.2.0`
- Status: `EXECUTION_AUTHORIZED`
- Authorized change: `scenario-runner-and-baseline`

## 1. Context

The current codebase has:

- foundation model and lifecycle coordination,
- metrics sampling, normalization, evidence recording, and summary generation,
- no repeatable scenario runner.

The next safe capability is therefore a deterministic runner that connects existing contracts without introducing control logic.

## 2. Design Principles

- Keep workload generation deterministic.
- Keep baseline execution fixed.
- Keep scenario code independent from policy and mutation.
- Prefer synchronous testable orchestration over background scheduling.
- Make every output explicit.

## 3. Proposed Package Shape

Recommended package:

```text
src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/scenario/
```

Recommended classes/interfaces:

- `ScenarioProfile`
- `ScenarioDefinition`
- `ScenarioStep`
- `ScenarioPlan`
- `ScenarioPlanner`
- `DeterministicScenarioPlanner`
- `BaselineExecutorPreset`
- `BaselineWorkloadExecutor`
- `ScenarioRunOutcome`
- `ScenarioExperimentRunner`

## 4. Component Responsibilities

### 4.1 ScenarioDefinition

Immutable input model for one scenario.

Minimum fields:

- `scenarioId`
- `ScenarioProfile profile`
- `long seed`
- `int stepCount`
- `int baseWorkUnits`
- `String description`

Validation:

- `scenarioId` non-null and non-blank.
- `stepCount > 0`.
- `baseWorkUnits > 0`.

### 4.2 ScenarioStep

Immutable ordered unit of workload.

Minimum fields:

- `int index`
- `int workUnits`
- `long plannedDelayMillis`

For unit tests, `plannedDelayMillis` is data only. Unit tests must not sleep.

### 4.3 ScenarioPlanner

Builds a `ScenarioPlan` from `ScenarioDefinition`.

Supported profiles for first implementation:

- `STEADY`: every step has `baseWorkUnits`.
- `RAMP`: work units increase by index.
- `BURST`: deterministic periodic spikes.

`TIDE` can be reserved only if implementation remains small; otherwise defer it.

### 4.4 BaselineExecutorPreset

Immutable fixed executor configuration.

Minimum fields:

- `policyId`, fixed value such as `baseline-fixed`.
- `corePoolSize`.
- `maximumPoolSize`.
- `queueCapacity`.

Validation:

- core size > 0.
- maximum size >= core size.
- queue capacity >= 0.

### 4.5 BaselineWorkloadExecutor

Executes scenario steps in a fixed baseline mode.

For the first implementation, it may be a synchronous abstraction that records completed work units rather than a complex real thread-pool benchmark. If a real `ThreadPoolExecutor` is used, it must be fixed-size and must be shut down deterministically by tests.

### 4.6 ScenarioExperimentRunner

Coordinates:

```text
ScenarioDefinition
  -> ScenarioPlan
  -> ExperimentCoordinator.create/start
  -> BaselineWorkloadExecutor.execute(step)
  -> PressureSampler.sample(...)
  -> EvidenceRecorder.record(...)
  -> ExperimentCoordinator.stop/finalize
  -> ScenarioRunOutcome
```

The runner must not contain adaptive branching.

## 5. Integration With Existing Metrics

The runner should create `RuntimeObservation` objects from baseline executor state. At minimum:

- active threads,
- pool size,
- queue size,
- completed task count,
- CPU utilization absent unless safely available.

The runner should pass those observations to `PressureSampler` and record returned `ObservedSnapshot` values through `EvidenceRecorder`.

## 6. Risks and Mitigations

- Risk: tests become flaky if real sleeps are used. Mitigation: unit tests use deterministic steps and controlled timestamps.
- Risk: runner grows policy logic. Mitigation: boundary test bans `ControlPolicy`, `ScaleDecision`, `AdjustmentEvent`, and adaptive package references.
- Risk: baseline executor leaks threads. Mitigation: if a real executor is used, tests must assert shutdown.
- Risk: scenario profiles become too broad. Mitigation: implement only steady/ramp/burst unless tide remains trivial.

## 7. Implementation Guidance for Weak Agents

Use small files. Do not write a large runner first.

Recommended order:

1. model objects,
2. planner,
3. baseline preset,
4. simple workload executor,
5. runner orchestration,
6. boundary tests.

Avoid:

- background schedulers,
- real delays in unit tests,
- adaptive decisions,
- new dependencies,
- changing existing metrics contracts unless a test proves it is required.
