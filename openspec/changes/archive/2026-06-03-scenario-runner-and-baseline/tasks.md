## 1. Scenario Models

- [x] 1.1 Create the `experiment/scenario` package.
- [x] 1.2 Add `ScenarioProfile` with `STEADY`, `RAMP`, and `BURST`.
- [x] 1.3 Add immutable `ScenarioDefinition` with validation for id, profile, step count, and base work units.
- [x] 1.4 Add immutable `ScenarioStep` and `ScenarioPlan` with ordered steps and total work calculation.
- [x] 1.5 Add unit tests for valid and invalid scenario model creation.

## 2. Deterministic Planning

- [x] 2.1 Add `ScenarioPlanner` interface.
- [x] 2.2 Implement `DeterministicScenarioPlanner` for steady profile.
- [x] 2.3 Implement deterministic ramp profile.
- [x] 2.4 Implement deterministic burst profile.
- [x] 2.5 Add tests proving identical definitions produce identical plans and expected work units.

## 3. Fixed Baseline Execution

- [x] 3.1 Add immutable `BaselineExecutorPreset` with baseline policy id and fixed sizing values.
- [x] 3.2 Add validation for invalid core, max, and queue capacity values.
- [x] 3.3 Add `BaselineWorkloadExecutor` that executes planned steps without resizing.
- [x] 3.4 Add tests for completed step count, total completed work units, and fixed preset behavior.

## 4. Scenario Runner Orchestration

- [x] 4.1 Add `ScenarioRunOutcome` with run id, scenario id, policy id, completed step count, total work units, evidence count, and finalized state.
- [x] 4.2 Implement `ScenarioExperimentRunner` using `ExperimentCoordinator`, `ScenarioPlanner`, `BaselineWorkloadExecutor`, `PressureSampler`, and `EvidenceRecorder`.
- [x] 4.3 Map baseline executor state into `RuntimeObservation` values for active threads, pool size, queue size, completed task count, and absent CPU utilization.
- [x] 4.4 Record observed snapshots through `EvidenceRecorder` during execution.
- [x] 4.5 Add runner tests proving lifecycle finalization, evidence recording, and outcome fields.

## 5. Boundary and Verification

- [x] 5.1 Add a boundary test that the scenario package does not reference adaptive policy or mutation types.
- [x] 5.2 Ensure no new dependencies are added.
- [x] 5.3 Run `openspec.cmd validate --all --json`.
- [x] 5.4 Run `.\mvnw.cmd test`.
- [x] 5.5 Confirm `git status --short` before handoff.
