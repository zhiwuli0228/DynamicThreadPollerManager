# v0.1.0 Change Architecture Blueprint

## Header

- Version name: `v0.1.0`
- Document purpose: provide the shared implementation blueprint for the five planned changes
- Status: `BASELINED`
- Authoritative branch: `claude_master`

## 1. Blueprint intent

This document defines the implementation architecture that all future `v0.1.0` change designs must respect.

It is not a spec, not a task list, and not an execution authorization. It is the shared design basis for later change-level spec and design work.

The blueprint exists because the current implementation capacity is limited. The system therefore needs a small, explicit, low-coupling structure that can be implemented incrementally without forcing large rewrites.

## 2. Global design principles

### 2.1 Keep the experiment loop intact

The entire version must remain centered around a closed loop:

```text
Scenario -> Snapshot -> Decision -> Adjustment -> Recording -> Analysis
```

Any change that breaks this loop should be rejected or redesigned.

### 2.2 Separate concerns aggressively

The system should be separated into these concerns:

- scenario generation
- runtime observation
- policy evaluation
- executor mutation
- result recording
- summary and analysis

No single change should own more than one of the high-risk concerns.

### 2.3 Prefer explicit contracts

The architecture should avoid implicit coupling. Every important transition should be expressed through a small set of shared contracts:

- `ExperimentRun`
- `LoadScenario`
- `PressureSnapshot`
- `ControlPolicy`
- `ScaleDecision`
- `AdjustmentEvent`
- `ResultSeries`
- `AnalysisSummary`

These objects form the stable API between changes.

### 2.4 Make failure visible

The system must never silently ignore a failed decision or failed adjustment.

Every failure path should produce at least:

- a failure reason
- a timestamp
- the triggering snapshot or decision reference

### 2.5 Keep the first implementation small

The first implementation should prefer the simplest shape that can validate the research hypotheses.

That means:

- one baseline policy
- one adaptive policy
- a small set of scenarios
- a simple snapshot model
- one executor mutation path at a time

## 3. Cross-change contract model

### 3.1 ExperimentRun

Represents one replayable experiment execution.

Required semantics:

- identifies the scenario and policy used
- records start and end boundaries
- owns the final summary reference

### 3.2 LoadScenario

Represents a bounded workload profile.

Required semantics:

- deterministic given the same seed and parameters
- capable of producing tide, burst, ramp, and steady patterns

### 3.3 PressureSnapshot

Represents the runtime state observed at one sampling point.

Required semantics:

- must be timestamped
- must include both JVM and executor state
- must be serializable into experiment output

### 3.4 ControlPolicy

Represents a strategy that maps a snapshot to a decision.

Required semantics:

- must not directly mutate the executor
- must emit a reasoned recommendation
- must be deterministic for the same inputs

### 3.5 ScaleDecision

Represents the target state proposed by a policy.

Required semantics:

- includes target core size
- includes target queue capacity when supported
- includes a decision reason
- includes the snapshot that triggered it
- includes gate status

### 3.6 AdjustmentEvent

Represents an actual mutation applied to runtime state.

Required semantics:

- records before and after state
- records success or failure
- references the originating decision

### 3.7 ResultSeries

Represents the chronological experiment record.

Required semantics:

- stores snapshots
- stores decisions
- stores applied events
- can be summarized after the run

### 3.8 AnalysisSummary

Represents the end-of-run aggregate view.

Required semantics:

- includes runtime duration
- includes throughput and latency summaries
- includes adjustment counts
- includes rejection and safety signals when available

## 4. Five-change implementation map

### 4.1 Change 1: `experiment-foundation`

Core responsibility:

- define the experiment runtime skeleton
- define the shared domain model
- define lifecycle state
- define base configuration and extension points

Key boundary:

- no real metrics collection
- no mutation execution
- no adaptive logic

Implementation shape:

- small set of immutable value objects
- small experiment coordinator
- lifecycle state machine

### 4.2 Change 2: `metrics-snapshot-and-recording`

Core responsibility:

- collect runtime evidence
- normalize observation data
- store the experiment evidence stream

Key boundary:

- no policy evaluation
- no executor mutation
- no scenario scheduling

Implementation shape:

- polling sampler or scheduled sampler
- snapshot mapper
- append-only recording sink
- run summary generator

### 4.3 Change 3: `scenario-runner-and-baseline`

Core responsibility:

- drive a repeatable workload
- provide a fixed baseline executor
- produce comparable experiment runs

Key boundary:

- no adaptive control
- no executor resizing
- no complex analysis

Implementation shape:

- deterministic scenario generator
- experiment runner
- baseline executor preset
- repeatability controls

### 4.4 Change 4: `adaptive-policy-and-control-gate`

Core responsibility:

- evaluate runtime evidence
- apply safety gates
- generate a scale decision

Key boundary:

- no direct executor mutation
- no scenario generation
- no output formatting concerns

Implementation shape:

- policy interface
- rule-based policy
- gate evaluator
- decision builder

### 4.5 Change 5: `executor-adapter-and-queue-resizing`

Core responsibility:

- apply decisions to executor and queue state
- guard against unsafe mutation
- record change outcomes

Key boundary:

- no policy calculation
- no scenario scheduling
- no metrics sampling

Implementation shape:

- executor adapter
- queue capacity controller
- mutation validator
- adjustment event recorder

## 5. Integration rules

### 5.1 Direction of dependency

The dependency direction must be:

```text
Scenario Runner
  -> Snapshot Recording
  -> Policy Evaluation
  -> Executor Adapter
  -> Result Summary
```

Policy code must not depend on executor implementation details.  
Executor code may depend on decision contracts, but not on scenario internals.

### 5.2 Immutable inputs, explicit outputs

Where possible, the following should be treated as immutable inputs:

- scenario definitions
- pressure snapshot records
- policy inputs

The following should always be explicit outputs:

- decision objects
- adjustment events
- summary records

### 5.3 Compatibility with weak implementation capability

Because the current implementation model is not strong, the architecture should bias toward:

- small classes
- fewer cross-cutting abstractions
- stable interfaces over clever inheritance
- explicit orchestration instead of hidden framework magic

This keeps the later code generator and implementation work less error-prone.

## 6. Minimum implementation shape by layer

### 6.1 Scenario layer

Recommended components:

- `ScenarioDefinition`
- `ScenarioPlayer`
- `ScenarioScheduler`

### 6.2 Observation layer

Recommended components:

- `PressureSampler`
- `SnapshotAssembler`
- `EvidenceRecorder`

### 6.3 Decision layer

Recommended components:

- `PolicyEvaluator`
- `GateEvaluator`
- `DecisionBuilder`

### 6.4 Execution layer

Recommended components:

- `ExecutorAdapter`
- `QueueCapacityController`
- `MutationValidator`

### 6.5 Reporting layer

Recommended components:

- `RunSummaryBuilder`
- `ResultSeriesWriter`
- `AnalysisSummaryFormatter`

## 7. Future design handoff rule

When the user confirms that implementation should proceed, the next design documents should be created in this order:

1. change-specific scope
2. change-specific design
3. change-specific tasks
4. change-specific verification notes

This blueprint should be treated as the parent reference for all later design artifacts in `v0.1.0`.

## 8. Open questions

The blueprint intentionally leaves several points open for later confirmation:

- whether the first implementation uses direct executor mutation or an adapter over a managed executor wrapper
- whether queue capacity resizing is supported in-place or via a controlled replacement strategy
- whether the first adaptive policy uses only threshold rules or also simple trend detection
- whether observability is file-only or exposed through optional runtime read endpoints

These questions must be resolved in later change design, not in this blueprint.
