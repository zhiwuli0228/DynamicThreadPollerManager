# v0.1.0 Solution Design

## Header

- Version name: `v0.1.0`
- Authoring date: `2026-06-01`
- Status: `DRAFT`
- Authoritative branch: `claude_master`

## 1. Purpose

This version defines the first exploratory runtime for the project: a thread-pool dynamic control research platform.

The goal is not to ship a production-ready thread-pool manager. The goal is to build a reproducible experiment system that can answer whether dynamic scaling is useful under tide-like traffic and JVM pressure, and which control signals actually matter.

The design is based on the earlier thread-pool auto-scaling ideas from the reference documents, but the objective has been reframed from "production rollout" to "controlled experimentation."

## 2. In scope

v0.1.0 includes the minimum end-to-end research loop:

- Generate repeatable load patterns.
- Sample JVM and executor pressure signals.
- Evaluate multiple scaling policies in one runtime model.
- Apply thread-pool and queue adjustments through a controlled executor abstraction.
- Record every decision and state transition for later analysis.
- Produce experiment output that can be replayed and compared.

The first version is intentionally focused on capability validation. It should make it possible to ask and answer:

- Does dynamic scaling improve latency under tide-like load?
- Which JVM pressure indicators are useful as control inputs?
- Do thread scaling and queue scaling behave differently?
- How much do cooldown, deadband, and step limits reduce oscillation?
- What tradeoff is observed between throughput, latency, and stability?

## 3. Out of scope

v0.1.0 does not include:

- A production rollout path.
- A UI dashboard or polished demo presentation layer.
- Kubernetes or service-discovery integration.
- Automatic model training or online ML inference.
- A change decomposition into OpenSpec implementation tasks.
- A claim that the strategy is production-safe without more evidence.

## 4. Technical decisions

### 4.1 Research-first architecture

The system is organized as a closed experiment loop:

```text
Load Scenario -> Pressure Sampling -> Policy Evaluation -> Executor Action -> Result Recording -> Analysis
```

This keeps the experimental path deterministic and repeatable. The load generator and the policy engine must both be configurable so that the same scenario can be replayed across strategies.

### 4.2 Strategy layering

The earlier design documents are kept as the conceptual base, but their production wording is translated into an experiment framework.

The preferred layering is:

- Baseline layer: fixed thread pool and fixed queue.
- Rule-based layer: threshold-driven adjustments.
- Hybrid layer: budget bounds plus online control.
- Optional future layers: trend-based and model-based strategies.

The first version does not need all strategies implemented on day one, but the model must allow them to coexist behind the same interface.

### 4.3 Safety gating still matters

Although v0.1.0 is experimental, the control path still needs safety constraints because unsafe experiments are not useful experiments.

The following constraints are retained from the previous design thinking:

- Cooldown after each adjustment.
- Deadband to suppress tiny target changes.
- Step limits to prevent large jumps.
- Continuous-window triggering instead of single-sample triggering.
- Heap and GC protection thresholds that suppress aggressive scale-up.

### 4.4 Queue resizing is treated as a first-class capability

The queue is not a passive implementation detail. It is a second adjustable resource with its own risk profile.

The design treats queue scaling as:

- a direct capability when the queue implementation supports it;
- an abstraction boundary when the queue implementation does not support safe in-place resizing;
- a recorded decision event either way.

This avoids conflating "queue can change" with "queue must always change in-place."

### 4.5 JVM pressure is sampled from runtime signals

The platform should derive pressure from local JVM/runtime observations first, not from external orchestration assumptions.

Initial pressure inputs should include:

- heap usage ratio
- GC pause time and frequency
- thread count and executor activity
- queue depth and enqueue wait behavior
- task execution latency
- rejection count or rejection rate
- CPU utilization when available

The control policy should treat these signals as evidence, not as absolute truth.

### 4.6 Observability is event-based, not visual-first

v0.1.0 is not a UI-first project. The primary output is a structured experiment record.

Preferred outputs:

- JSON snapshots
- append-only event logs
- CSV or tabular experiment summaries
- reproducible scenario identifiers

This keeps the first version lightweight and analysis-friendly.

### 4.7 Version boundary

This version is a baseline for future decomposition, not an execution authorization.

It should remain small enough to answer the first research questions without becoming a general-purpose framework.

## 5. Domain model

The core domain objects for v0.1.0 are:

- `ExperimentRun`: one replayable experiment instance.
- `LoadScenario`: a declared traffic shape such as ramp-up, burst, or tide.
- `PressureSnapshot`: one sampled view of JVM and executor state.
- `ControlPolicy`: a pluggable strategy that maps snapshot to target state.
- `ScaleDecision`: the recommended target thread and queue values.
- `AdjustmentEvent`: the actual change applied to the executor.
- `ResultSeries`: the time-ordered record of samples and decisions.
- `AnalysisSummary`: the aggregated experiment result.

The main relationship is:

```text
ExperimentRun
  -> LoadScenario
  -> PressureSnapshot[]
  -> ControlPolicy
  -> ScaleDecision[]
  -> AdjustmentEvent[]
  -> AnalysisSummary
```

## 6. API and observability surfaces

v0.1.0 should expose only the surfaces needed for experimentation.

### 6.1 Internal control surfaces

- Start experiment run.
- Stop experiment run.
- Switch active policy.
- Apply a new target core size.
- Apply a new queue capacity target.
- Read current snapshot.
- Read recent adjustment history.

### 6.2 Data surfaces

Recommended experiment outputs:

- a run summary file per experiment
- a timeline of sampling snapshots
- a timeline of control decisions
- a timeline of applied adjustment events

### 6.3 Optional runtime endpoints

If HTTP surfaces are added later, they should be treated as read-mostly operational surfaces, not as the primary control plane.

Examples:

- current run status
- recent metrics
- current policy
- last decision reason

## 7. Testing and acceptance strategy

v0.1.0 should be judged by reproducibility and comparative signal quality, not by aesthetics.

### 7.1 Test dimensions

- Scenario repeatability
- Policy determinism
- Boundary safety
- Oscillation resistance
- Metrics completeness
- Result comparability

### 7.2 Required scenario set

At minimum, the first version should support:

- steady low load
- step up and step down load
- tide-like periodic load
- short burst load
- long high-pressure load

### 7.3 Acceptance criteria

The version is acceptable when it can:

- run the same scenario multiple times with consistent behavior boundaries;
- compare at least one fixed baseline and one adaptive policy;
- record adjustment events together with their triggering snapshots;
- show whether dynamic control improves or worsens latency, throughput, and stability in a given scenario;
- preserve enough data to replay or summarize the run afterward.

### 7.4 Non-goals for testing

The first version does not need production-grade soak validation or external platform integration tests.

## 8. Change decomposition

If this version later reaches a point where implementation is authorized, it can be decomposed into the following bounded work packages:

1. Experiment harness and scenario generator.
2. JVM and executor metrics sampler.
3. Policy abstraction and baseline policy.
4. Rule-based adaptive policy.
5. Queue resizing abstraction and executor adapter.
6. Result recording and summary generation.
7. Optional analysis or visualization surface.

The decomposition order should follow dependency order, not presentation order.

## 9. Execution authorization

This v0.1.0 design does not authorize implementation by itself.

Status at the design level:

- design baseline: `DRAFT`
- decomposition authorization: `NOT AUTHORIZED`
- execution authorization: `NOT AUTHORIZED`

The next step, if desired, is to refine this design into a change decomposition plan or a more detailed requirement package.
