# Current State

## Authoritative Status

- Current stage: `READY_FOR_CHANGE_DECOMPOSITION`
- Current authorized work type: `CHANGE_DECOMPOSITION_ONLY`
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.1.0`, `v0.2.0`, `v0.3.0`, and `v0.4.0` are `IMPLEMENTED`; `v0.5.0` IR and SR are closed and authorized for bounded OpenSpec change decomposition only
- OpenSpec capability changes: `experiment-foundation`, `metrics-snapshot-and-recording`, `scenario-runner-and-baseline`, `adaptive-policy-and-control-gate`, and `offline-replay-and-readiness-gate` have been archived; the v0.1.0, v0.2.0, v0.3.0, and v0.4.0 capability baselines are present on `claude_master` and verified behavior is synchronized to `openspec/specs/`
- Java implementation status: the experiment foundation package, the metrics observation layer (sampling, normalization, append-only recording, summary), the deterministic scenario runner with fixed baseline executor, the adaptive policy/control-gate package, and the read-only `experiment.analysis` package (evidence validation, offline policy replay, summary aggregation, threshold sensitivity comparison, mutation readiness assessment, and controlled report artifacts) are present on the main working branch
- Governance status: future capability work must follow `docs/02-harness/managed-change-standard.md`; reusable stage-package guidance is available at `docs/07-templates/managed-change-stage-package-template.md`

## Active Authorized Change

- None. The most recent bounded change, `offline-replay-and-readiness-gate`, was archived on 2026-06-05 to `openspec/changes/archive/2026-06-05-offline-replay-and-readiness-gate/` and its delta spec is mirrored at `openspec/specs/offline-replay-and-readiness-gate/spec.md`.

## Active Authorized Version Work

- `v0.5.0` change decomposition is authorized under `openspec/changes/`.
- Completed version design artifacts: `README.md`, `00-objectives-and-scope.md`, `10-ir.md`, `11-ir-review.md`, `12-ir-review-disposition.md`, `13-ir-closure-verification.md`, `20-sr.md`, `21-sr-review.md`, `22-sr-review-disposition.md`, `23-sr-closure-verification.md`, and `decision-log.md`.
- Authorized next OpenSpec change: `executor-adapter-and-adjustment-evidence`.
- Current authorization allows OpenSpec proposal/design/spec/tasks/plan creation only; it does not allow Java implementation, executor mutation, or queue resizing implementation.

## What Is Allowed Now

- Inspect archived artifacts and synchronized specs as evidence.
- Maintain managed-change standards and templates.
- Keep the current-state record synchronized with the actual repository state.
- Create and maintain the `executor-adapter-and-adjustment-evidence` OpenSpec change decomposition artifacts.
- Inspect `v0.4.0` offline replay and readiness artifacts as input evidence.

## What Is Not Allowed Now

- No Java source or test change during `v0.5.0` change decomposition authorization.
- No unreviewed scope expansion.
- No branch-state mismatch between the workspace and the authoritative branch.
- No archive or finalize event without synchronized authority records.
- No new dependencies.
- No Java source or test change under this authorization.
- No executor mutation, queue resizing, scenario behavior change, persistence, external API, or neighboring capability implementation under this authorization.

## Future Gate Sequence

1. `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
2. version design draft
3. version design baseline
4. `READY_FOR_CHANGE_DECOMPOSITION` ← current stage
5. `EXECUTION_AUTHORIZED`
6. capability change execution

## Current Reference

The repository currently preserves earlier bootstrap and framework history in `docs/99-archive/`. Those files are historical evidence only.
