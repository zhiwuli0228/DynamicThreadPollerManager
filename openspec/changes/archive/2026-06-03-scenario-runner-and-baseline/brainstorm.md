## Design Summary

The third capability should introduce deterministic scenario execution and a fixed baseline runner. The current repository can create experiment runs and record pressure snapshots, but it cannot yet drive a repeatable workload. Without repeatable scenarios, later adaptive policies would have no reliable baseline for comparison.

The chosen design is a small synchronous scenario runner. It defines immutable scenario inputs, generates ordered scenario steps deterministically, executes those steps against a fixed baseline workload executor, samples runtime pressure through the existing metrics layer, records evidence, and finalizes the experiment run. It deliberately avoids adaptive policy evaluation, scale decisions, executor resizing, queue mutation, background scheduling, external APIs, and new dependencies.

## Alternatives Considered

### Alternative A: Real scheduled thread-pool benchmark

- **Approach**: Create a real scheduled workload driver with timed delays and real thread-pool queue pressure.
- **Pros**: Closer to runtime behavior; could expose richer executor metrics.
- **Cons**: Flaky tests, timing dependence, larger implementation surface, harder for weak downstream agents.
- **Why not chosen**: This change needs deterministic repeatability more than runtime realism.

### Alternative B: Combine scenario runner with baseline policy

- **Approach**: Add scenario playback and a baseline policy evaluator in one change.
- **Pros**: Faster path to comparing policy output.
- **Cons**: Blurs scenario and policy boundaries; risks introducing decisions before baseline evidence is stable.
- **Why not chosen**: Policy abstraction is explicitly deferred to a later change.

### Alternative C: Deterministic synchronous scenario runner

- **Approach**: Use plain Java scenario models, deterministic planning, a fixed baseline executor abstraction, and explicit evidence recording.
- **Pros**: Stable tests; small implementation; clean handoff to later policy work; validates foundation and metrics integration.
- **Cons**: Not a production benchmark yet; no wall-clock load realism.
- **Why chosen**: Best fit for current implementation capability and the version boundary.

## Agreed Approach

Use Alternative C. Implement `scenario-runner-and-baseline` as a narrow internal Java capability under the experiment boundary. The downstream agent should create small model and orchestration classes, not a large framework.

## Key Decisions

- The capability name is `scenario-runner-and-baseline`.
- Supported initial scenario profiles are `STEADY`, `RAMP`, and `BURST`.
- Scenario planning must be deterministic for the same definition and seed.
- Baseline execution must be fixed and non-adaptive.
- Runner orchestration must record evidence through `EvidenceRecorder`.
- Unit tests must not depend on `Thread.sleep` or wall-clock scheduling.
- Boundary tests must ban policy and mutation references.

## Open Questions

- Whether `TIDE` should be added now or reserved for a later version.
- Whether the first baseline executor should use a real `ThreadPoolExecutor` or a synchronous work counter.
- Whether `ScenarioRunOutcome` should include raw snapshots or only evidence count.

Resolution for this change: defer `TIDE`; prefer synchronous work counter unless a small fixed executor is straightforward; outcome should include counts and identifiers, not raw snapshot lists.
