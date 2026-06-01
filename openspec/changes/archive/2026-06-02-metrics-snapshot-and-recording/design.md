## Context

`experiment-foundation` has delivered the shared experiment model and lifecycle coordinator. The next architectural layer is observation: collecting runtime evidence while keeping policy and mutation responsibilities out of scope.

Current constraints:

- `v0.1.0` remains a research-oriented experiment platform.
- The version blueprint requires separation between observation, policy evaluation, executor mutation, and reporting.
- The implementation should remain small and deterministic because later changes depend on this boundary.
- No new dependencies should be introduced for the first metrics layer.

## Goals / Non-Goals

**Goals:**

- Capture timestamped pressure snapshots for an experiment run.
- Normalize observable JVM and executor state into stable snapshot records.
- Store snapshots in append-only order for later summary and analysis.
- Generate a minimal summary from the recorded evidence.
- Keep deterministic manual sampling paths for unit tests.

**Non-Goals:**

- No adaptive policy evaluation.
- No scale decision creation.
- No executor or queue mutation.
- No scenario scheduling or workload generation.
- No external monitoring, metrics registry, database, or UI integration.
- No durable file format unless required by implementation-level tests.

## Decisions

### 1. Read-only observation boundary

The metrics layer will collect and record evidence only. It will not decide whether a pool should scale and will not apply any adjustment.

Why:
- The blueprint separates observation from control.
- Later policy and adapter changes need a neutral evidence source.

### 2. Snapshot assembler over direct ad hoc records

The implementation should use a small assembler that maps runtime inputs into `PressureSnapshot` records.

Why:
- Mapping logic stays testable.
- Future executor-specific observation can be isolated behind the assembler instead of leaking into the recorder.

### 3. Append-only recorder

The recorder will accept snapshots for a run and preserve insertion order.

Why:
- Experiment analysis depends on chronology.
- Append-only behavior avoids hidden mutation and makes tests straightforward.

### 4. Deterministic manual sampling plus optional scheduled adapter

The core sampler should support explicit sample calls for tests. A scheduled adapter may wrap it for runtime loops, but scheduling must not become the primary contract.

Why:
- Unit tests should not depend on timing races.
- Runtime sampling still needs a clear path for repeated observation.

### 5. Minimal summary builder

The summary builder will derive basic counts and bounds from recorded snapshots, such as sample count, first and last timestamp, and observed pressure ranges when available.

Why:
- Later comparison and analysis work need an evidence-derived summary.
- Rich statistics can wait until scenario runner and analysis requirements are concrete.

## Risks / Trade-offs

- [Risk] Available executor metrics may be limited without a managed executor wrapper. -> [Mitigation] Record optional or unknown fields explicitly and keep the first assembler defensive.
- [Risk] Scheduled sampling tests can become flaky. -> [Mitigation] Make manual sampling the core contract and test scheduled behavior only with controlled clocks or bounded waits.
- [Risk] Summary output could grow into analysis logic too early. -> [Mitigation] Limit summaries to direct aggregates from recorded snapshots.
- [Risk] Result storage may need persistence later. -> [Mitigation] Keep the recorder interface small so a file-backed implementation can replace the in-memory recorder later.

## Migration Plan

This is an additive internal capability.

Implementation order:

1. Add observation contracts for sampling, assembling, recording, and summary building.
2. Implement deterministic manual snapshot assembly and append-only recording.
3. Add optional scheduled sampling orchestration if it can stay small.
4. Add summary generation from the recorded snapshot stream.
5. Add unit tests for ordering, normalization, summary values, and boundary isolation.

Rollback strategy:

- If scheduled sampling introduces instability, keep the manual sampler and defer scheduling to the scenario runner change.
- If executor metrics cannot be captured safely, retain JVM and run-level snapshot fields and reserve executor fields as absent values.

## Open Questions

- Should scheduled sampling live in this change or be deferred until the scenario runner owns the run loop?
- Which metrics should be mandatory in `PressureSnapshot` versus optional because executor implementations differ?
- Should summary generation update the existing `AnalysisSummary` shape or create an observation-specific summary object that is later folded into `AnalysisSummary`?
