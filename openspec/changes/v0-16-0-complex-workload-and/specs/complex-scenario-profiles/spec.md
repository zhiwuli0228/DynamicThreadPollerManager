## ADDED Requirements

### Requirement: ScenarioProfile enum SHALL include LONG_TAIL, MIXED_CPU_IO, and DOWNSTREAM_BLOCKED values

The `ScenarioProfile` enum MUST contain three new constant values — `LONG_TAIL`, `MIXED_CPU_IO`, and `DOWNSTREAM_BLOCKED` — in addition to the existing `STEADY`, `RAMP`, and `BURST` values. The new values MUST be usable as inputs to `ScenarioDefinition` and `DeterministicScenarioPlanner`.

#### Scenario: Construct ScenarioDefinition with LONG_TAIL profile

- **WHEN** a `ScenarioDefinition` is constructed with `ScenarioProfile.LONG_TAIL`, seed `42`, stepCount `10`, and baseWorkUnits `100`
- **THEN** the definition is created successfully and `definition.profile()` returns `LONG_TAIL`

#### Scenario: Construct ScenarioDefinition with MIXED_CPU_IO profile

- **WHEN** a `ScenarioDefinition` is constructed with `ScenarioProfile.MIXED_CPU_IO`, seed `7`, stepCount `8`, and baseWorkUnits `50`
- **THEN** the definition is created successfully and `definition.profile()` returns `MIXED_CPU_IO`

#### Scenario: Construct ScenarioDefinition with DOWNSTREAM_BLOCKED profile

- **WHEN** a `ScenarioDefinition` is constructed with `ScenarioProfile.DOWNSTREAM_BLOCKED`, seed `0`, stepCount `6`, and baseWorkUnits `200`
- **THEN** the definition is created successfully and `definition.profile()` returns `DOWNSTREAM_BLOCKED`

### Requirement: DeterministicScenarioPlanner SHALL produce deterministic LONG_TAIL steps using seed

The `DeterministicScenarioPlanner.plan()` method MUST produce a `ScenarioPlan` for `LONG_TAIL` profiles where step work units are computed as `baseWorkUnits + (seed % 3 == 0 ? baseWorkUnits * 5 : 0)`. Given the same `ScenarioDefinition` (including seed), the planner MUST always produce the identical plan.

#### Scenario: LONG_TAIL plan with seed divisible by 3 produces spike steps

- **WHEN** `plan()` is called with a `LONG_TAIL` definition where `seed = 6` (6 % 3 == 0), `baseWorkUnits = 100`, and `stepCount = 4`
- **THEN** every step in the returned plan has `workUnits = 600` (100 + 100 * 5)

#### Scenario: LONG_TAIL plan with seed not divisible by 3 produces base steps

- **WHEN** `plan()` is called with a `LONG_TAIL` definition where `seed = 7` (7 % 3 == 1), `baseWorkUnits = 100`, and `stepCount = 4`
- **THEN** every step in the returned plan has `workUnits = 100`

#### Scenario: LONG_TAIL plan is deterministic across invocations

- **WHEN** `plan()` is called twice with the same `LONG_TAIL` `ScenarioDefinition`
- **THEN** both returned `ScenarioPlan` instances are equal

### Requirement: DeterministicScenarioPlanner SHALL produce deterministic MIXED_CPU_IO steps

The `DeterministicScenarioPlanner.plan()` method MUST produce a `ScenarioPlan` for `MIXED_CPU_IO` profiles where even-indexed steps are CPU-bound (high work units) and odd-indexed steps are IO-bound (low work units with increased delay). The formula MUST be: even index → `workUnits = baseWorkUnits * 3`, odd index → `workUnits = baseWorkUnits` with `plannedDelayMillis = baseWorkUnits * 2`.

#### Scenario: MIXED_CPU_IO alternates CPU and IO steps

- **WHEN** `plan()` is called with a `MIXED_CPU_IO` definition where `baseWorkUnits = 50` and `stepCount = 4`
- **THEN** step 0 has `workUnits = 150` and `plannedDelayMillis = 0`
- **AND** step 1 has `workUnits = 50` and `plannedDelayMillis = 100`
- **AND** step 2 has `workUnits = 150` and `plannedDelayMillis = 0`
- **AND** step 3 has `workUnits = 50` and `plannedDelayMillis = 100`

#### Scenario: MIXED_CPU_IO plan is deterministic across invocations

- **WHEN** `plan()` is called twice with the same `MIXED_CPU_IO` `ScenarioDefinition`
- **THEN** both returned `ScenarioPlan` instances are equal

### Requirement: DeterministicScenarioPlanner SHALL produce deterministic DOWNSTREAM_BLOCKED steps

The `DeterministicScenarioPlanner.plan()` method MUST produce a `ScenarioPlan` for `DOWNSTREAM_BLOCKED` profiles where every step uses `workUnits = baseWorkUnits` and `plannedDelayMillis = baseWorkUnits * 10` to simulate downstream backpressure.

#### Scenario: DOWNSTREAM_BLOCKED plan uses constant work units with high delay

- **WHEN** `plan()` is called with a `DOWNSTREAM_BLOCKED` definition where `baseWorkUnits = 200` and `stepCount = 3`
- **THEN** every step has `workUnits = 200` and `plannedDelayMillis = 2000`

#### Scenario: DOWNSTREAM_BLOCKED plan is deterministic across invocations

- **WHEN** `plan()` is called twice with the same `DOWNSTREAM_BLOCKED` `ScenarioDefinition`
- **THEN** both returned `ScenarioPlan` instances are equal

### Requirement: Seed SHALL be used for reproducibility in all new profiles

For `LONG_TAIL`, `MIXED_CPU_IO`, and `DOWNSTREAM_BLOCKED` profiles, the `ScenarioDefinition.seed()` field MUST influence the plan output (either directly in formulas or as a tiebreaker for ambiguity). Two definitions differing only in seed MUST produce distinguishable plans when the seed-dependent formula yields different results.

#### Scenario: Different seeds produce different LONG_TAIL plans when seed % 3 differs

- **WHEN** `plan()` is called with `LONG_TAIL` definition A where `seed = 3` and definition B where `seed = 4`, both with `baseWorkUnits = 100`
- **THEN** plan A has all steps at `workUnits = 600` and plan B has all steps at `workUnits = 100`

#### Scenario: Same seed produces identical plans for any new profile

- **WHEN** `plan()` is called twice with `DOWNSTREAM_BLOCKED` definitions both having `seed = 99`
- **THEN** the two plans are identical
