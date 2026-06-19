# Brainstorm: baseline-catalog-and-comparison-runner

## Design Summary

This change delivers the foundational building blocks of the v0.12.0 baseline comparison experiment framework. The design was explored and validated through a formal IR (requirements analysis) and SR (functional design) process documented in `docs/04-development/versions/v0.12.0/`.

The problem: the system can run baseline executor scenarios and managed executor scenarios independently, but has no framework to (1) catalog common executor presets, (2) run the same workload against two executor types, (3) normalize metrics across them, or (4) compute side-by-side comparison deltas. This change addresses gaps (1)-(3), while gap (4) is deferred to change 2/2.

The validated design introduces 5 new components in `experiment.scenario` (CommonExecutorPreset, BaselineExecutorCatalog, NormalizedComparisonMetrics, MetricDelta, ComparisonResult, ComparableScenarioRunner) and 2 modifications to existing components (ManagedExecutor rejection counting, ScenarioRunOutcome extension).

## Alternatives Considered

### Alternative A: Extend BaselineExecutorPreset inline (no catalog)
- **Approach**: Add new preset factory methods to `BaselineExecutorPreset` without a catalog abstraction
- **Pros**: Minimal new classes, no new abstraction
- **Cons**: No lookup by ID, no centralized preset management, cannot support external preset registration
- **Why not chosen**: The roadmap requires systematic comparison of multiple executor presets. A catalog is the natural abstraction for "registry of configurations"

### Alternative B: Use Spring Bean configuration for presets
- **Approach**: Define presets as Spring `@ConfigurationProperties` or `@Bean`
- **Pros**: Leverages Spring DI, external configuration via application.yml
- **Cons**: Introduces Spring dependency to experiment model; catalog should be a pure Java abstraction
- **Why not chosen**: The project's architecture keeps experiment packages free of Spring coupling. A plain Java catalog aligns with existing patterns (ExecutorRegistry, ExperimentCoordinator)

### Alternative C: Single change for all v0.12.0
- **Approach**: Combine catalog + runner + report into one change
- **Pros**: Single deliverable, no inter-change dependency
- **Cons**: Violates managed-change-standard independent verifiability rule; catalog+runner can be tested without report serialization
- **Why not chosen**: SR §5 decomposes into 2 independently verifiable changes

## Agreed Approach

6 new components all in `experiment.scenario` package + 2 existing code modifications:
1. `CommonExecutorPreset` (record) — standardized preset with executor type classification
2. `BaselineExecutorCatalog` (class + Builder) — immutable registry with 6 JDK defaults
3. `NormalizedComparisonMetrics` (record) — 9 cross-executor metrics + `fromSnapshots()` factory
4. `MetricDelta` (record) — per-metric comparison delta with IMPROVED/REGRESSED/NEUTRAL direction
5. `ComparisonResult` (record) — complete comparison outcome (2x outcomes + 2x metrics + 9 deltas)
6. `ComparableScenarioRunner` (class) — accepts scenario + preset ID + managed config, sequentially runs baseline then managed, returns ComparisonResult
7. `ManagedExecutor` extension — `getRejectedTaskCount()` via AtomicLong + handler wrapper
8. `ScenarioRunOutcome` extension — new `rejectedTaskCount` field (backward-compatible)

## Key Decisions

See `docs/04-development/versions/v0.12.0/decision-log.md`:
- D1: 6 default presets (fixed-2/4/8, cached, single, fixed-2-bounded)
- D2: Sequential execution (baseline before managed)
- D3: 9 normalized metrics
- D5: Two-change decomposition
- D6: Reuse ScenarioDefinition (no new WorkloadDefinition)

## Open Questions

None — all design questions resolved through IR/SR process. Implementation details deferred to tasks.md.
