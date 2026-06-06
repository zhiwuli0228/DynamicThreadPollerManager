# Current State

## Authoritative Status

- Current stage: `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
- Current authorized work type: `NONE`
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.1.0`, `v0.2.0`, `v0.3.0`, `v0.4.0`, `v0.5.0`, and `v0.6.0` (pressure data acquisition) are `IMPLEMENTED`
- OpenSpec capability changes: `experiment-foundation`, `metrics-snapshot-and-recording`, `scenario-runner-and-baseline`, `adaptive-policy-and-control-gate`, `offline-replay-and-readiness-gate`, `executor-adapter-and-adjustment-evidence`, and `pressure-data-acquisition-and-baseline` have been archived; the v0.1.0 through v0.6.0 capability baselines are present on `claude_master` and verified behavior is synchronized to `openspec/specs/`
- Java implementation status: the experiment foundation package, the metrics observation layer (sampling, normalization, append-only recording, summary), the deterministic scenario runner with fixed baseline executor, the adaptive policy/control-gate package, the read-only `experiment.analysis` package (evidence validation, offline policy replay, summary aggregation, threshold sensitivity comparison, mutation readiness assessment, and controlled report artifacts), the bounded `experiment.adjustment` package (executor adjustment command, read-only state snapshot, safety gate, in-memory adjustable probe, adapter result/evidence records, and boundary tests), and the bounded `experiment.acquisition` package (run manifest, pressure/replay summaries, evidence index, readiness summary, data quality validator, readiness classifier, retention record, and report writer) are present on the main working branch
- Governance status: future capability work must follow `docs/02-harness/managed-change-standard.md`; reusable stage-package guidance is available at `docs/07-templates/managed-change-stage-package-template.md`

## Active Authorized Change

- Authorized OpenSpec change: none.
- The most recent archived bounded change, `pressure-data-acquisition-and-baseline`, was archived on 2026-06-06 to `openspec/changes/archive/2026-06-06-pressure-data-acquisition-and-baseline/` and its delta spec is mirrored at `openspec/specs/pressure-data-acquisition-and-baseline/spec.md`.

## Active Authorized Version Work

- `v0.6.0` pressure data acquisition change has been implemented and archived.
- Completed version design artifacts: `README.md`, `00-objectives-and-scope.md`, `10-ir.md`, `11-ir-review.md`, `12-ir-review-disposition.md`, `13-ir-closure-verification.md`, `15-experiment-data-acquisition-plan.md`, `20-sr.md`, `21-sr-review.md`, `22-sr-review-disposition.md`, `23-sr-closure-verification.md`, and `decision-log.md`.
- Authorized OpenSpec change: none.
- Current authorization does not allow any Java implementation or tests without a new authorized change.
- Current authorization does not allow queue resizing implementation, production `ThreadPoolExecutor` integration, closed-loop scheduler/controller, persistence, REST/API/UI, external dependencies, throughput improvement claims, or neighboring capability changes.

## What Is Allowed Now

- Inspect archived artifacts and synchronized specs as evidence.
- Maintain managed-change standards and templates.
- Keep the current-state record synchronized with the actual repository state.
- Verify archived `pressure-data-acquisition-and-baseline` and `executor-adapter-and-adjustment-evidence` artifacts and synchronized specs as evidence.
- Inspect `v0.4.0` offline replay and readiness artifacts as input evidence.
- Draft new version designs or request new change authorization.

## What Is Not Allowed Now

- No Java source or test change without a new authorized change.
- No additional OpenSpec change creation without new authorization.
- No unreviewed scope expansion.
- No branch-state mismatch between the workspace and the authoritative branch.
- No archive or finalize event without synchronized authority records.
- No new dependencies.
- No queue resizing, production executor integration, scenario behavior change, persistence, external API, or neighboring capability implementation without new authorization.

## Future Gate Sequence

1. `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED` ← current stage
2. version design draft
3. version design baseline
4. `READY_FOR_CHANGE_DECOMPOSITION`
5. `EXECUTION_AUTHORIZED`
6. capability change execution

## Current Reference

The repository currently preserves earlier bootstrap and framework history in `docs/99-archive/`. Those files are historical evidence only.
