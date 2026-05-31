## ADDED Requirements

### Requirement: Experiment run lifecycle foundation
The system MUST provide a minimal experiment run lifecycle that can create, start, stop, and finalize a run using a scenario identifier and a policy identifier.

#### Scenario: Start an experiment run
- **WHEN** a new experiment is created with a scenario identifier and a policy identifier
- **THEN** the system MUST create a run record and transition it into an active running state

#### Scenario: Stop an experiment run
- **WHEN** a running experiment is stopped
- **THEN** the system MUST transition the run into a stopped state and prevent further lifecycle transitions

---

### Requirement: Shared experiment contracts
The system MUST define explicit shared contracts for `ExperimentRun`, `LoadScenario`, `PressureSnapshot`, `ControlPolicy`, `ScaleDecision`, `AdjustmentEvent`, `ResultSeries`, and `AnalysisSummary`.

#### Scenario: Construct foundation objects
- **WHEN** the foundation creates a new experiment record
- **THEN** the resulting data MUST be representable through the shared contracts without relying on ad hoc fields

#### Scenario: Reuse contracts across future changes
- **WHEN** a later change consumes experiment data
- **THEN** it MUST be able to read the shared contracts without redefining them

---

### Requirement: Deterministic experiment identity
The system MUST assign each experiment run a deterministic identity and must retain the scenario and policy identity pair associated with that run.

#### Scenario: Repeat a scenario with the same identifiers
- **WHEN** the same scenario and policy identifiers are used for a repeated experiment
- **THEN** the run identity and associated metadata MUST remain traceable for comparison

#### Scenario: Inspect run provenance
- **WHEN** a completed run is inspected later
- **THEN** the stored scenario and policy identity MUST be available for replay and comparison

---

### Requirement: Orchestration without mutation
The system MUST provide an experiment coordinator that handles lifecycle orchestration but MUST NOT perform metrics sampling, policy evaluation, or executor mutation in this change.

#### Scenario: Initialize a run
- **WHEN** the coordinator initializes an experiment
- **THEN** it MUST prepare the lifecycle state without changing executor or queue state

#### Scenario: Finalize a run
- **WHEN** the coordinator finalizes an experiment
- **THEN** it MUST produce a summary reference without invoking sampling or scaling logic

---

### Requirement: Minimal summary output
The system MUST produce a minimal experiment summary that captures run duration, scenario identity, policy identity, and lifecycle outcome.

#### Scenario: Generate a summary after stop
- **WHEN** a run is stopped
- **THEN** the system MUST be able to emit a summary that captures the run metadata and lifecycle result

#### Scenario: Replay summary data
- **WHEN** a completed summary is read later
- **THEN** it MUST preserve enough metadata to identify the original experiment
