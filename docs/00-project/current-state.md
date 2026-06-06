# Current State

## Authoritative Status

- Current stage: `VERSION_REQUIREMENT_DRAFT_AUTHORIZED`
- Current authorized work type: `VERSION_IR_DRAFT_ONLY`
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.1.0`, `v0.2.0`, `v0.3.0`, `v0.4.0`, and `v0.5.0` are `IMPLEMENTED`; `v0.6.0` pressure data acquisition IR draft is authorized
- OpenSpec capability changes: `experiment-foundation`, `metrics-snapshot-and-recording`, `scenario-runner-and-baseline`, `adaptive-policy-and-control-gate`, `offline-replay-and-readiness-gate`, and `executor-adapter-and-adjustment-evidence` have been archived; the v0.1.0 through v0.5.0 capability baselines are present on `claude_master` and verified behavior is synchronized to `openspec/specs/`
- Java implementation status: the experiment foundation package, the metrics observation layer (sampling, normalization, append-only recording, summary), the deterministic scenario runner with fixed baseline executor, the adaptive policy/control-gate package, the read-only `experiment.analysis` package (evidence validation, offline policy replay, summary aggregation, threshold sensitivity comparison, mutation readiness assessment, and controlled report artifacts), and the bounded `experiment.adjustment` package (executor adjustment command, read-only state snapshot, safety gate, in-memory adjustable probe, adapter result/evidence records, and boundary tests) are present on the main working branch
- Governance status: future capability work must follow `docs/02-harness/managed-change-standard.md`; reusable stage-package guidance is available at `docs/07-templates/managed-change-stage-package-template.md`

## Active Authorized Change

- None.
- The most recent archived bounded change, `executor-adapter-and-adjustment-evidence`, was archived on 2026-06-06 to `openspec/changes/archive/2026-06-06-executor-adapter-and-adjustment-evidence/` and its delta spec is mirrored at `openspec/specs/executor-adapter-and-adjustment-evidence/spec.md`.

## Active Authorized Version Work

- `v0.6.0` IR requirement draft and experiment data acquisition plan are authorized under `docs/04-development/versions/v0.6.0/`.
- Completed version design artifacts: `README.md`, `00-objectives-and-scope.md`, `10-ir.md`, `11-ir-review.md`, `12-ir-review-disposition.md`, `13-ir-closure-verification.md`, `20-sr.md`, `21-sr-review.md`, `22-sr-review-disposition.md`, `23-sr-closure-verification.md`, and `decision-log.md`.
- Authorized OpenSpec change: none.
- Current authorization allows v0.6.0 IR draft documents and experiment data acquisition planning only.
- Current authorization does not allow additional Java implementation or tests.
- Current authorization does not allow queue resizing implementation, production `ThreadPoolExecutor` integration, closed-loop scheduler/controller, persistence, REST/API/UI, external dependencies, throughput improvement claims, or neighboring capability changes.

## What Is Allowed Now

- Inspect archived artifacts and synchronized specs as evidence.
- Maintain managed-change standards and templates.
- Keep the current-state record synchronized with the actual repository state.
- Verify archived `executor-adapter-and-adjustment-evidence` artifacts and synchronized specs as evidence.
- Inspect `v0.4.0` offline replay and readiness artifacts as input evidence.
- Create and revise v0.6.0 IR draft, objectives/scope, experiment data acquisition plan, and decision log.

## What Is Not Allowed Now

- No Java source or test change without a newly authorized change.
- No OpenSpec change creation before v0.6.0 IR and SR closure.
- No actual pressure test execution before the data acquisition plan is reviewed and authorized.
- No unreviewed scope expansion.
- No branch-state mismatch between the workspace and the authoritative branch.
- No archive or finalize event without synchronized authority records.
- No new dependencies.
- No queue resizing, production executor integration, scenario behavior change, persistence, external API, or neighboring capability implementation under this authorization.

## Future Gate Sequence

1. `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
2. version design draft ← current stage
3. version design baseline
4. `READY_FOR_CHANGE_DECOMPOSITION`
5. `EXECUTION_AUTHORIZED`
6. capability change execution

## Current Reference

The repository currently preserves earlier bootstrap and framework history in `docs/99-archive/`. Those files are historical evidence only.
