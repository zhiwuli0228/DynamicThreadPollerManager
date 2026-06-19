# v0.2.0 Requirements and Use Cases

## Header

- Version name: `v0.2.0`
- Status: `EXECUTION_AUTHORIZED`
- Authorized change: `scenario-runner-and-baseline`

## 1. Primary Use Case

A developer wants to run a fixed baseline experiment with a deterministic workload profile, then inspect the recorded evidence stream to compare future adaptive behavior against the baseline.

## 2. Actors

- Experiment developer: uses the runner in tests or local experiments.
- Future policy implementer: consumes baseline result evidence later.
- Verification agent: checks scenario repeatability and boundary isolation.

## 3. Functional Requirements

### 3.1 Scenario definition

The system must model a scenario with:

- scenario id,
- profile type,
- deterministic seed,
- total steps or duration proxy,
- per-step workload size,
- optional description.

### 3.2 Scenario planning

The system must convert a scenario definition into ordered workload steps. The same definition must produce the same ordered steps.

### 3.3 Baseline executor

The system must provide a fixed baseline executor preset. The preset must expose fixed core size, maximum size, and queue capacity values without adaptive resizing.

### 3.4 Runner orchestration

The runner must:

1. create an experiment run,
2. start the run,
3. play scenario steps against the baseline executor abstraction,
4. sample and record runtime pressure evidence,
5. stop and finalize the run,
6. return a run outcome with run id, scenario id, policy id, completed step count, and evidence count.

### 3.5 Boundary isolation

The scenario runner must not:

- evaluate adaptive policies,
- create `ScaleDecision`,
- create `AdjustmentEvent`,
- resize executors,
- mutate queue capacity.

## 4. Non-Functional Requirements

- Deterministic tests must not rely on wall-clock sleeps.
- Implementation must use existing foundation and metrics contracts when practical.
- The first implementation should favor plain Java classes over framework wiring.
- No new dependency is allowed.

## 5. Acceptance Use Cases

### Use Case A: Repeat a steady scenario

- Given a steady scenario with fixed seed and step count.
- When the planner builds the plan twice.
- Then both plans contain identical ordered steps.

### Use Case B: Execute a baseline run

- Given a baseline runner and a small scenario.
- When the runner executes the scenario.
- Then a finalized run outcome is returned and evidence snapshots are recorded.

### Use Case C: Preserve non-adaptive boundary

- Given the scenario runner package.
- When verification scans its source.
- Then it does not reference adaptive policy or executor mutation types.
