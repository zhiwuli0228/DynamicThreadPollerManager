# v0.2.0 Decision Log

## Decision 1: Authorize only the scenario runner capability

- Date: `2026-06-02`
- Decision: `v0.2.0` authorizes only `scenario-runner-and-baseline`.
- Rationale: the project needs repeatable baseline runs before adaptive policy or executor mutation can be meaningful.
- Consequence: policy and mutation work remain deferred.

## Decision 2: Deterministic tests over timing realism

- Date: `2026-06-02`
- Decision: unit tests must avoid real sleeps and wall-clock scheduling assumptions.
- Rationale: weak downstream implementation agents are more likely to produce stable code with deterministic inputs.
- Consequence: scenario step delays are model data unless a later change authorizes runtime scheduling.

## Decision 3: Baseline executor must remain fixed

- Date: `2026-06-02`
- Decision: the baseline preset and executor must not resize during a run.
- Rationale: baseline comparison is only useful if the baseline does not adapt.
- Consequence: any resizing behavior belongs to a later executor adapter change.

## Decision 4: Use existing metrics evidence

- Date: `2026-06-02`
- Decision: the runner records evidence through the delivered metrics layer.
- Rationale: avoids duplicating recording logic and validates the previous change in a real orchestration path.
- Consequence: no external observability or persistence is authorized.
