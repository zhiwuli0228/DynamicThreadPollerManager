# Scenario Runner and Baseline Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development
> to implement this plan task-by-task.

**Goal:** Add a deterministic scenario runner and fixed baseline execution path that records metrics evidence without adaptive policy or executor mutation.

**Architecture:** Implement a small `experiment.scenario` package. Build immutable scenario models first, then a deterministic planner, then fixed baseline execution, then a runner that connects existing foundation and metrics components.

**Tech Stack:** Java 21, existing Spring Boot project, JUnit 5, Maven. No new dependencies.

---

## Task 1: Scenario Models

- [ ] **Step 1:** Create `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/scenario/`.
- [ ] **Step 2:** Add `ScenarioProfile` enum with `STEADY`, `RAMP`, and `BURST`.
- [ ] **Step 3:** Write failing tests for valid `ScenarioDefinition` construction.
- [ ] **Step 4:** Implement `ScenarioDefinition` with fields `scenarioId`, `profile`, `seed`, `stepCount`, `baseWorkUnits`, and `description`.
- [ ] **Step 5:** Write failing tests for blank id, null profile, non-positive step count, and non-positive base work units.
- [ ] **Step 6:** Implement validation.
- [ ] **Step 7:** Add `ScenarioStep` with `index`, `workUnits`, and `plannedDelayMillis`.
- [ ] **Step 8:** Add `ScenarioPlan` with scenario id, ordered steps, and total work calculation.
- [ ] **Step 9:** Run model tests and commit.

## Task 2: Deterministic Planning

- [ ] **Step 1:** Add `ScenarioPlanner` interface with `ScenarioPlan plan(ScenarioDefinition definition)`.
- [ ] **Step 2:** Write a failing test that planning the same `STEADY` definition twice returns equal step work units.
- [ ] **Step 3:** Implement `DeterministicScenarioPlanner` steady profile: every step uses `baseWorkUnits`.
- [ ] **Step 4:** Write a failing test for `RAMP`: step `i` uses `baseWorkUnits + i`.
- [ ] **Step 5:** Implement ramp profile.
- [ ] **Step 6:** Write a failing test for `BURST`: every third step uses `baseWorkUnits * 3`, otherwise `baseWorkUnits`.
- [ ] **Step 7:** Implement burst profile.
- [ ] **Step 8:** Add a total work units test for each profile.
- [ ] **Step 9:** Run planner tests and commit.

## Task 3: Fixed Baseline Execution

- [ ] **Step 1:** Add `BaselineExecutorPreset` with `policyId`, `corePoolSize`, `maximumPoolSize`, and `queueCapacity`.
- [ ] **Step 2:** Write failing validation tests for invalid core/max/queue values.
- [ ] **Step 3:** Implement preset validation and a static factory such as `fixedSmall()` if useful.
- [ ] **Step 4:** Add `BaselineWorkloadExecutor` that synchronously executes a `ScenarioStep` and tracks completed step count and completed work units.
- [ ] **Step 5:** Write tests that executing a plan completes all steps and sums work units.
- [ ] **Step 6:** Add read methods for active threads, pool size, queue size, and completed task count for metrics mapping. In a synchronous executor, active threads and queue size may be `0`, pool size may be preset core size, and completed task count should reflect completed steps.
- [ ] **Step 7:** Run baseline execution tests and commit.

## Task 4: Scenario Runner Orchestration

- [ ] **Step 1:** Add `ScenarioRunOutcome` with run id, scenario id, policy id, completed step count, total work units, evidence count, and final state.
- [ ] **Step 2:** Write a failing runner test using `ExperimentCoordinator`, `DeterministicScenarioPlanner`, `BaselineWorkloadExecutor`, `ManualPressureSampler`, and `InMemoryEvidenceRecorder`.
- [ ] **Step 3:** Implement `ScenarioExperimentRunner` constructor dependencies.
- [ ] **Step 4:** Implement run flow: plan, create run, start run, execute each step, sample observation, record evidence, stop run, finalize run, return outcome.
- [ ] **Step 5:** Build `RuntimeObservation` after each executed step with active threads, pool size, queue size, completed task count, and absent CPU utilization.
- [ ] **Step 6:** Use deterministic timestamps in tests. Do not use `Thread.sleep`.
- [ ] **Step 7:** Assert evidence snapshots are associated with the returned run id.
- [ ] **Step 8:** Assert outcome uses the baseline preset policy id and finalized state.
- [ ] **Step 9:** Run runner tests and commit.

## Task 5: Boundary and Final Verification

- [ ] **Step 1:** Add `ScenarioBoundaryIsolationTest` that scans `src/main/java/.../experiment/scenario` for forbidden strings: `ControlPolicy`, `ScaleDecision`, `AdjustmentEvent`, `.policy.`, `adaptive`, `ExecutorAdapter`, `QueueCapacityController`, `MutationValidator`.
- [ ] **Step 2:** Confirm `pom.xml` has no new dependencies.
- [ ] **Step 3:** Run `openspec.cmd validate --all --json`.
- [ ] **Step 4:** Run `.\mvnw.cmd test`.
- [ ] **Step 5:** Update `tasks.md` checkboxes only after tests pass.
- [ ] **Step 6:** Prepare `apply.md` after implementation with commit range, task counts, and test results.
