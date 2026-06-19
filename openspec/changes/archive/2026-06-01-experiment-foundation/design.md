## Context

`v0.1.0` has been defined as a research-oriented experimental platform, not a production thread-pool manager. The first change must establish the runtime foundation that later changes can reuse for observation, scenarios, policy evaluation, and executor mutation.

Current constraints:

- The repository is still in the documentation-framework stage.
- The codebase is minimal and the implementation model is weak, so the first change must be intentionally small.
- The project already has a version-level blueprint that defines the experiment loop and the cross-change contract model.

## Goals / Non-Goals

**Goals:**

- Create a minimal experiment runtime foundation.
- Define the shared domain contracts used by all future `v0.1.0` changes.
- Establish a deterministic lifecycle for experiment runs.
- Keep the base small enough to implement and test incrementally.
- Provide extension points for later metrics, scenarios, policy, and executor work.

**Non-Goals:**

- No JVM metrics sampling.
- No actual thread-pool resizing.
- No queue capacity adjustment.
- No visualization or reporting UI.
- No production rollout mechanics.
- No ADR or architecture-document rewrite unless implementation later reveals a boundary mismatch.

## Decisions

### 1. Shared contract first

The foundation will define immutable contracts for `ExperimentRun`, `LoadScenario`, `PressureSnapshot`, `ControlPolicy`, `ScaleDecision`, `AdjustmentEvent`, `ResultSeries`, and `AnalysisSummary`.

Why:
- Later changes need a stable handoff boundary.
- Shared contracts reduce the chance of incompatible models across change boundaries.

### 2. Small lifecycle coordinator

The foundation will include a small coordinator that controls run initialization, start, stop, and summary finalization.

Why:
- It gives the experiment a single entry point.
- It keeps orchestration explicit instead of relying on hidden framework behavior.

### 3. Deterministic run identity

Every experiment run will be tied to a run identifier and scenario/policy identity pair.

Why:
- Reproducibility matters more than presentation.
- Later comparison work needs stable identifiers.

### 4. No mutation responsibility in the foundation

The foundation will not mutate executors or queues.

Why:
- Mutation is high risk and belongs in a later dedicated change.
- Keeping the foundation read-oriented reduces the chance of early coupling.

### 5. No new ADR required at this stage

The design aligns with the existing living architecture and the version blueprint, so no separate ADR is required for the first change.

Why:
- This change implements the already-documented architecture direction rather than introducing a new boundary.
- If implementation later changes the architecture boundary, an ADR should be added then.

## Risks / Trade-offs

- [Risk] The foundation may be too small to anticipate every later need. → [Mitigation] Keep the contracts explicit and versioned, then extend them only when later changes prove a gap.
- [Risk] A shared model may become over-general too early. → [Mitigation] Limit the initial object set to the minimum needed for experiments.
- [Risk] Lifecycle coordination could accidentally absorb policy logic. → [Mitigation] Keep the coordinator orchestration-only and enforce a strict no-mutation rule.
- [Risk] Later changes may want different persistence or output formats. → [Mitigation] Keep the foundation output-neutral and let the recording change own formatting.

## Migration Plan

This is a greenfield foundation change, so there is no legacy migration path.

Deployment order for the future implementation:

1. Introduce the shared experiment contracts.
2. Add the lifecycle coordinator and run state transitions.
3. Wire the minimal summary output.
4. Add tests that exercise the run lifecycle and object boundaries.
5. Verify that later changes can depend on the foundation without modifying it.

Rollback strategy:

- If the foundation proves too narrow, expand the contracts in a backward-compatible way.
- If the lifecycle becomes too complex, split orchestration from state representation before adding later changes.

## Open Questions

- Should the foundation expose a scenario registry or only accept scenario references?
- Should the run summary be file-based only in the first implementation?
- What is the minimum state machine needed to keep the first version deterministic?
- Which package names best preserve a clean boundary with the later metrics and policy changes?
