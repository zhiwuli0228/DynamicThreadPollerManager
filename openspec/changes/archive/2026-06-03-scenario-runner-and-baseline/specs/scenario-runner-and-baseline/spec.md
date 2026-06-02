## ADDED Requirements

### Requirement: Deterministic scenario definition
The system MUST define scenario inputs that include a scenario identifier, profile, seed, step count, base work units, and description.

#### Scenario: Create a valid scenario definition
- **WHEN** a scenario is created with a non-blank identifier, profile, seed, positive step count, and positive base work units
- **THEN** the system MUST retain those values for deterministic planning

#### Scenario: Reject invalid scenario definition
- **WHEN** a scenario is created with a blank identifier, missing profile, non-positive step count, or non-positive base work units
- **THEN** the system MUST reject the definition before planning

---

### Requirement: Repeatable scenario planning
The system MUST convert a scenario definition into an ordered scenario plan that is identical for the same definition.

#### Scenario: Plan the same steady scenario twice
- **WHEN** the same steady scenario definition is planned twice
- **THEN** both plans MUST contain the same ordered steps and total work units

#### Scenario: Plan supported profiles
- **WHEN** steady, ramp, or burst scenario definitions are planned
- **THEN** each plan MUST produce deterministic step work units according to its profile rule

---

### Requirement: Fixed baseline executor preset
The system MUST provide a fixed baseline executor preset with a baseline policy identity, core pool size, maximum pool size, and queue capacity.

#### Scenario: Create a fixed baseline preset
- **WHEN** a baseline preset is created with valid fixed sizing values
- **THEN** the system MUST expose the preset values without adaptive behavior

#### Scenario: Reject invalid baseline preset
- **WHEN** a baseline preset is created with non-positive core size, maximum size lower than core size, or negative queue capacity
- **THEN** the system MUST reject the preset

---

### Requirement: Baseline scenario execution
The system MUST execute scenario steps against a fixed baseline workload executor without resizing or adaptive policy evaluation.

#### Scenario: Execute all planned steps
- **WHEN** a scenario plan is executed by the baseline workload executor
- **THEN** the executor MUST report the completed step count and total completed work units

#### Scenario: Preserve non-adaptive execution
- **WHEN** the baseline executor runs scenario steps
- **THEN** it MUST NOT create scale decisions, adjustment events, or executor resizing operations

---

### Requirement: Scenario experiment runner
The system MUST provide a runner that creates, starts, executes, stops, finalizes, samples, and records a baseline scenario run.

#### Scenario: Complete a baseline scenario run
- **WHEN** the runner executes a valid scenario definition with a fixed baseline preset
- **THEN** it MUST return an outcome containing run id, scenario id, baseline policy id, completed step count, total work units, evidence count, and finalized state

#### Scenario: Record evidence during run
- **WHEN** the runner executes scenario steps
- **THEN** it MUST record at least one observed snapshot associated with the run id

---

### Requirement: Scenario boundary isolation
The system MUST keep scenario runner code independent from adaptive policy and executor mutation capabilities.

#### Scenario: Verify forbidden dependencies
- **WHEN** the scenario package source is inspected
- **THEN** it MUST NOT reference adaptive policy types, scale decision creation, adjustment event creation, or executor mutation adapter types
