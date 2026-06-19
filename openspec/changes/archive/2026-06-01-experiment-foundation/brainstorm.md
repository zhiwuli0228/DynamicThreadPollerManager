## Design Summary

The first change should establish the experiment foundation for the research platform: a small runtime core, a shared domain model, and a deterministic lifecycle that later changes can build on without reworking the base contracts.

The safest path is an explicit experiment coordinator with immutable value objects for run, scenario, snapshot, decision, event, and summary data. This keeps the first change small enough for the current implementation model while still creating a real foundation for later metrics, scenarios, policy, and executor work.

## Alternatives Considered

### Alternative A: Monolithic experiment runner
- **Approach**: Put lifecycle, scenario tracking, summary creation, and future extension hooks into one runner class.
- **Pros**: Fastest to code; fewer initial files.
- **Cons**: High coupling; hard to test; difficult to extend with later metrics and policy changes.
- **Why not chosen**: It would make later change boundaries fuzzy and increase rewrite risk.

### Alternative B: Generic framework with mutable registry
- **Approach**: Create a flexible registry and let components mutate shared state directly.
- **Pros**: Easy to attach new behaviors.
- **Cons**: State is harder to reason about; replayability suffers; bugs become harder to isolate.
- **Why not chosen**: The current implementation model is not strong enough to absorb that level of implicit coupling.

### Alternative C: Explicit experiment foundation with immutable contracts
- **Approach**: Define a small experiment coordinator and a stable set of immutable domain objects that all later changes reuse.
- **Pros**: Clear boundaries; easier testing; good replayability; low coupling.
- **Cons**: Slightly more upfront modeling work.
- **Why chosen**: Best balance of safety, clarity, and future extensibility.

## Agreed Approach

Use Alternative C. Create a minimal experiment runtime foundation that owns lifecycle, shared contracts, and extension points, but does not perform sampling or executor mutation yet. This gives the rest of the version a stable substrate for metrics, scenarios, policy, and execution.

## Key Decisions

- The first capability is a single foundation capability named `experiment-foundation`.
- The foundation must expose a deterministic experiment lifecycle and run identity.
- Core domain objects must be modeled explicitly and kept as the stable handoff boundary for later changes.
- The foundation must not include real metrics sampling, scaling decisions, or executor mutation.
- The design should remain aligned with the existing architecture and blueprint documents, so no new ADR is required at this stage unless implementation later deviates.

## Open Questions

- What is the smallest runtime lifecycle that still supports replayable experiments?
- Should the first version persist run records to files only, or also expose read-only runtime endpoints later?
- Which package layout will best keep the foundation small without blocking the later metrics and policy changes?
- Should the foundation own scenario identity only, or also provide a small registry for scenario definitions?
