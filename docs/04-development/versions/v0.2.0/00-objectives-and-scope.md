# v0.2.0 Objectives and Scope

## Header

- Version name: `v0.2.0`
- Status: `EXECUTION_AUTHORIZED`
- Authorized change: `scenario-runner-and-baseline`

## 1. Purpose

The project already has a minimal experiment foundation and a metrics observation layer. It still lacks a repeatable way to drive workload and compare a fixed baseline run. `v0.2.0` closes that gap by authorizing a deterministic scenario runner and baseline executor capability.

## 2. Objectives

- Define deterministic scenario profiles that can be replayed with the same seed and parameters.
- Provide workload steps that are small, explicit, and testable without sleeping in unit tests.
- Add a baseline executor preset that does not resize or adapt during a run.
- Add a runner that creates a run, plays a scenario, records pressure snapshots, stops the run, and returns a minimal outcome.
- Preserve the separation between scenario playback, metrics observation, policy evaluation, and executor mutation.

## 3. In Scope

- Scenario definition model.
- Scenario step model.
- Deterministic scenario planner/player.
- Baseline executor configuration/preset.
- Experiment runner orchestration that uses existing foundation and metrics components.
- Unit and integration-style tests using deterministic clocks and direct execution.

## 4. Out of Scope

- Adaptive policies.
- Scaling decisions.
- Executor mutation after runner creation.
- Queue capacity resizing.
- Thread scheduling accuracy benchmarking.
- External persistence, REST API, UI, or dashboard.
- New dependencies.

## 5. Success Criteria

- Given the same scenario definition and seed, the scenario plan is identical across runs.
- A baseline run can be executed without adaptive control.
- The runner records at least one evidence snapshot for a completed run.
- The implementation does not introduce policy or mutation dependencies into the scenario package.
- OpenSpec validation and the Maven test suite pass after implementation.
