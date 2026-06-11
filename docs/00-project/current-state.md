# Current State

## Authoritative Status

- Current stage: `VERSION_DESIGN_DRAFT` for `v0.7.0` change 2/3
- Current authorized work type: `DESIGN_ONLY` — no implementation authorized until next change is approved
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.1.0`, `v0.2.0`, `v0.3.0`, `v0.4.0`, `v0.5.0`, and `v0.6.0` (pressure data acquisition) are `IMPLEMENTED`; `v0.7.0` (managed executor domain) change 1/3 (`establish-managed-executor-and-registry`) is `ARCHIVED`; changes 2/3 and 3/3 are pending
- OpenSpec capability changes: `experiment-foundation`, `metrics-snapshot-and-recording`, `scenario-runner-and-baseline`, `adaptive-policy-and-control-gate`, `offline-replay-and-readiness-gate`, `executor-adapter-and-adjustment-evidence`, `pressure-data-acquisition-and-baseline`, and `establish-managed-executor-and-registry` have been archived; the v0.1.0 through v0.7.0-change-1 capability baselines are present on `claude_master` and verified behavior is synchronized to `openspec/specs/`
- Java implementation status: the experiment foundation package, the metrics observation layer (sampling, normalization, append-only recording, summary), the deterministic scenario runner with fixed baseline executor, the adaptive policy/control-gate package, the read-only `experiment.analysis` package (evidence validation, offline policy replay, summary aggregation, threshold sensitivity comparison, mutation readiness assessment, and controlled report artifacts), the bounded `experiment.adjustment` package (executor adjustment command, read-only state snapshot, safety gate, in-memory adjustable probe, adapter result/evidence records, and boundary tests), the bounded `experiment.acquisition` package (run manifest, pressure/replay summaries, evidence index, readiness summary, data quality validator, readiness classifier, retention record, and report writer), and the bounded `experiment.executor` package (ManagedExecutor wrapping ThreadPoolExecutor, ExecutorRegistry with deletion safety, RuntimeSetting bounds and parameter classification, AtomicDeletionSafety, and ExecutorStateSnapshot extension) are present on the main working branch
- Governance status: future capability work must follow `docs/02-harness/managed-change-standard.md`; reusable stage-package guidance is available at `docs/07-templates/managed-change-stage-package-template.md`

## Active Authorized Change

- No active authorized change. Implementation authorization requires an approved OpenSpec change.
- The most recent archived change, `establish-managed-executor-and-registry`, was archived on 2026-06-12 to `openspec/changes/archive/2026-06-12-establish-managed-executor-and-registry/` and its delta spec is mirrored at `openspec/specs/establish-managed-executor-and-registry/spec.md`.
- Next pending change: `bridge-adjustment-to-real-executor` (change 2/3 for v0.7.0).

## Active Authorized Version Work

- `v0.7.0` managed executor domain design is in progress (IR + SR closed; change 1/3 archived).
- Completed version design artifacts: `README.md`, `00-objectives-and-scope.md`, `10-ir.md`, `11-ir-review.md`, `12-ir-review-disposition.md`, `13-ir-closure-verification.md`, `20-sr.md`, `21-sr-review.md`, `22-sr-review-disposition.md`, `23-sr-closure-verification.md`, and `decision-log.md`.
- Current authorization does not allow any Java implementation or tests without a new authorized change.
- Current authorization does not allow queue resizing implementation, production `ThreadPoolExecutor` integration, closed-loop scheduler/controller, persistence, REST/API/UI, external dependencies, throughput improvement claims, or neighboring capability changes.

## What Is Allowed Now

- Inspect archived artifacts and synchronized specs as evidence.
- Maintain managed-change standards and templates.
- Keep the current-state record synchronized with the actual repository state.
- Verify archived capability artifacts and synchronized specs as evidence.
- Draft new OpenSpec changes for the next v0.7.0 capability (`bridge-adjustment-to-real-executor`).
- Draft new version designs or request new change authorization.
- Review v0.7.0 IR and SR as input for change 2/3 design.

## What Is Not Allowed Now

- No Java source or test changes without an authorized OpenSpec change in `EXECUTION_AUTHORIZED` state.
- No unreviewed scope expansion.
- No branch-state mismatch between the workspace and the authoritative branch.
- No archive or finalize event without synchronized authority records.
- No new dependencies.
- No queue resizing, production executor integration, scenario behavior change, persistence, external API, or neighboring capability implementation without new authorization.

## Future Gate Sequence

1. ~~`CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`~~ (completed)
2. ~~`VERSION_DESIGN_DRAFT`~~ (completed)
3. ~~`SR_DESIGN_AUTHORIZED`~~ (completed)
4. ~~`READY_FOR_CHANGE_DECOMPOSITION`~~ (completed)
5. ~~`EXECUTION_AUTHORIZED`~~ (completed for change 1/3 — `establish-managed-executor-and-registry` archived)
6. ~~capability change execution~~ (completed for change 1/3)
7. change 2/3 OpenSpec drafting ← next stage
8. change 2/3 `EXECUTION_AUTHORIZED`
9. change 2/3 implementation

## Current Reference

The repository currently preserves earlier bootstrap and framework history in `docs/99-archive/`. Those files are historical evidence only.
