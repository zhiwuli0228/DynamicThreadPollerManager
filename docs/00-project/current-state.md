# Current State

## Authoritative Status

- Current stage: `VERSION_DESIGN_DRAFT` for `v0.7.0` change 3/3
- Current authorized work type: `DESIGN_ONLY` — no implementation authorized until next change is approved
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.1.0`, `v0.2.0`, `v0.3.0`, `v0.4.0`, `v0.5.0`, and `v0.6.0` (pressure data acquisition) are `IMPLEMENTED`; `v0.7.0` (managed executor domain) changes 1/3 and 2/3 are `ARCHIVED`; change 3/3 is pending
- OpenSpec capability changes: `experiment-foundation`, `metrics-snapshot-and-recording`, `scenario-runner-and-baseline`, `adaptive-policy-and-control-gate`, `offline-replay-and-readiness-gate`, `executor-adapter-and-adjustment-evidence`, `pressure-data-acquisition-and-baseline`, `establish-managed-executor-and-registry`, and `bridge-adjustment-to-real-executor` have been archived; the v0.1.0 through v0.7.0-change-2 capability baselines are present on `claude_master` and verified behavior is synchronized to `openspec/specs/`
- Java implementation status: the experiment foundation package, the metrics observation layer, the deterministic scenario runner with fixed baseline executor, the adaptive policy/control-gate package, the read-only `experiment.analysis` package, the bounded `experiment.adjustment` package, the bounded `experiment.acquisition` package, and the bounded `experiment.executor` package (ManagedExecutor, ExecutorRegistry, RuntimeSetting, DeletionSafety, ManagedExecutorAdjustmentAdapter, ExecutorStateSnapshot extension) are present on the main working branch
- Governance status: future capability work must follow `docs/02-harness/managed-change-standard.md`; reusable stage-package guidance is available at `docs/07-templates/managed-change-stage-package-template.md`

## Active Authorized Change

- No active authorized change. Implementation authorization requires an approved OpenSpec change.
- The most recent archived change, `bridge-adjustment-to-real-executor`, was archived on 2026-06-12 to `openspec/changes/archive/2026-06-12-bridge-adjustment-to-real-executor/` and its delta spec is mirrored at `openspec/specs/bridge-adjustment-to-real-executor/spec.md`.
- Next pending change: `closed-loop-experiment-verification` (change 3/3 for v0.7.0).

## Active Authorized Version Work

- `v0.7.0` managed executor domain: changes 1/3 and 2/3 archived. Change 3/3 (`closed-loop-experiment-verification`) is the final change.
- Completed version design artifacts: `README.md`, `00-objectives-and-scope.md`, `10-ir.md`, `11-ir-review.md`, `12-ir-review-disposition.md`, `13-ir-closure-verification.md`, `20-sr.md`, `21-sr-review.md`, `22-sr-review-disposition.md`, `23-sr-closure-verification.md`, and `decision-log.md`.
- Current authorization does not allow any Java implementation or tests without a new authorized change.

## What Is Allowed Now

- Inspect archived artifacts and synchronized specs as evidence.
- Maintain managed-change standards and templates.
- Draft the final v0.7.0 OpenSpec change (`closed-loop-experiment-verification`).
- Review v0.7.0 IR and SR as input for change 3/3 design.

## What Is Not Allowed Now

- No Java source or test changes without an authorized OpenSpec change in `EXECUTION_AUTHORIZED` state.
- No unreviewed scope expansion.
- No new dependencies.
- No queue resizing, production executor integration beyond adapter, persistence, external API, or neighboring capability implementation without new authorization.

## Future Gate Sequence

1. ~~`CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`~~ (completed)
2. ~~`VERSION_DESIGN_DRAFT`~~ (completed)
3. ~~`SR_DESIGN_AUTHORIZED`~~ (completed)
4. ~~`READY_FOR_CHANGE_DECOMPOSITION`~~ (completed)
5. ~~`EXECUTION_AUTHORIZED`~~ (completed for changes 1/3 and 2/3)
6. ~~capability change execution~~ (completed for changes 1/3 and 2/3)
7. change 3/3 OpenSpec drafting ← next stage
8. change 3/3 `EXECUTION_AUTHORIZED`
9. change 3/3 implementation
10. v0.7.0 retrospective

## Current Reference

The repository currently preserves earlier bootstrap and framework history in `docs/99-archive/`. Those files are historical evidence only.
