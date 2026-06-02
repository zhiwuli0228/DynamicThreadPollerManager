## 1. Scenario Models

- [ ] 1.1 Create the `experiment/scenario` package.
- [ ] 1.2 Add `ScenarioProfile` with `STEADY`, `RAMP`, and `BURST`.
- [ ] 1.3 Add immutable `ScenarioDefinition` with validation for id, profile, step count, and base work units.
- [ ] 1.4 Add immutable `ScenarioStep` and `ScenarioPlan` with ordered steps and total work calculation.
- [ ] 1.5 Add unit tests for valid and invalid scenario model creation.

## 2. Deterministic Planning

- [ ] 2.1 Add `ScenarioPlanner` interface.
- [ ] 2.2 Implement `DeterministicScenarioPlanner` for steady profile.
- [ ] 2.3 Implement deterministic ramp profile.
- [ ] 2.4 Implement deterministic burst profile.
- [ ] 2.5 Add tests proving identical definitions produce identical plans and expected work units.

## 3. Fixed Baseline Execution

- [ ] 3.1 Add immutable `BaselineExecutorPreset` with baseline policy id and fixed sizing values.
- [ ] 3.2 Add validation for invalid core, max, and queue capacity values.
- [ ] 3.3 Add `BaselineWorkloadExecutor` that executes planned steps without resizing.
- [ ] 3.4 Add tests for completed step count, total completed work units, and fixed preset behavior.

## 4. Scenario Runner Orchestration

- [ ] 4.1 Add `ScenarioRunOutcome` with run id, scenario id, policy id, completed step count, total work units, evidence count, and finalized state.
- [ ] 4.2 Implement `ScenarioExperimentRunner` using `ExperimentCoordinator`, `ScenarioPlanner`, `BaselineWorkloadExecutor`, `PressureSampler`, and `EvidenceRecorder`.
- [ ] 4.3 Map baseline executor state into `RuntimeObservation` values for active threads, pool size, queue size, completed task count, and absent CPU utilization.
- [ ] 4.4 Record observed snapshots through `EvidenceRecorder` during execution.
- [ ] 4.5 Add runner tests proving lifecycle finalization, evidence recording, and outcome fields.

## 5. Boundary and Verification

- [ ] 5.1 Add a boundary test that the scenario package does not reference adaptive policy or mutation types.
- [ ] 5.2 Ensure no new dependencies are added.
- [ ] 5.3 Run `openspec.cmd validate --all --json`.
- [ ] 5.4 Run `.\mvnw.cmd test`.
- [ ] 5.5 Confirm `git status --short` before handoff.
