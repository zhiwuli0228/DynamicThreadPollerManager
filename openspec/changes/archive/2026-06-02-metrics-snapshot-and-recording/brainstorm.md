## Design Summary

The second `v0.1.0` change should add the observation and evidence layer on top of the delivered experiment foundation. The system needs a small sampler that can collect runtime state at controlled intervals, normalize that state into `PressureSnapshot` records, append snapshots into a `ResultSeries`, and produce a minimal summary from the recorded evidence.

The safest path is to keep this change read-oriented. It should observe JVM and executor state, but it must not evaluate policies, schedule workload scenarios, or mutate executor configuration. This keeps the observation boundary reusable by later scenario, adaptive policy, and executor adapter changes.

## Alternatives Considered

### Alternative A: Policy-aware metrics collector

- **Approach**: Combine metric sampling with early threshold evaluation and decision hints.
- **Pros**: Faster path to adaptive behavior; fewer handoff objects in the short term.
- **Cons**: Blurs observation and policy boundaries; makes later policy changes depend on collector internals; increases test complexity.
- **Why not chosen**: The version blueprint explicitly separates observation from control, and this would violate that boundary.

### Alternative B: External metrics stack integration

- **Approach**: Use Micrometer, Actuator, or an external monitoring sink as the first-class evidence store.
- **Pros**: Closer to production observability patterns; richer metrics ecosystem.
- **Cons**: Adds dependencies and operational surface before the experiment loop is stable; harder to keep deterministic.
- **Why not chosen**: The current version is a research experiment platform, not a production observability rollout.

### Alternative C: Internal append-only snapshot recorder

- **Approach**: Add a small sampler, snapshot assembler, append-only recorder, and summary builder using existing foundation contracts.
- **Pros**: Clear capability boundary; deterministic tests; no new dependencies; easy for later changes to consume.
- **Cons**: Less feature-rich than a real monitoring stack; summary output starts intentionally minimal.
- **Why chosen**: Best balance of traceability, low coupling, and incremental delivery.

## Agreed Approach

Use Alternative C. Implement a lightweight metrics snapshot and recording capability that periodically or manually captures runtime evidence, writes normalized snapshots to an append-only in-memory series, and builds a minimal summary from that evidence. The implementation should reuse foundation types where possible and introduce only observation-specific interfaces where the current foundation is too abstract.

## Key Decisions

- The second capability is named `metrics-snapshot-and-recording`.
- Observation is read-only: it records snapshots and summaries but does not create scale decisions or adjustment events.
- Snapshot records must be timestamped and associated with an experiment run.
- The first recorder can be in-memory and append-only; durable file output remains a future concern unless needed by tests.
- The sampler should support deterministic manual sampling for unit tests and a bounded scheduled sampling adapter for runtime use.
- Summary generation should use recorded evidence only and should remain independent of policy and executor mutation.

## Open Questions

- Should the first scheduled sampler use a JDK scheduler directly or a Spring-managed scheduling abstraction?
- Which executor metrics are available without introducing a custom managed executor wrapper?
- Should queue capacity be recorded only when the executor exposes it safely?
- Does the summary need percentile-style latency fields now, or should it only reserve the shape for later scenario-driven timing data?
