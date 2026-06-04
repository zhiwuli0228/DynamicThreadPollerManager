# Current State

## Authoritative Status

- Current stage: `EXECUTION_AUTHORIZED`
- Current authorized work type: `BOUNDED_CHANGE_IMPLEMENTATION`
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.3.0` is `IMPLEMENTED`; `v0.4.0` is `EXECUTION_AUTHORIZED`
- OpenSpec capability changes: `experiment-foundation`, `metrics-snapshot-and-recording`, `scenario-runner-and-baseline`, and `adaptive-policy-and-control-gate` have been archived; the v0.1.0, v0.2.0, and v0.3.0 capability baselines are present on `claude_master` and verified behavior is synchronized to `openspec/specs/`
- Java implementation status: the experiment foundation package, the metrics observation layer (sampling, normalization, append-only recording, summary), the deterministic scenario runner with fixed baseline executor, and the adaptive policy/control-gate package are present on the main working branch
- Governance status: future capability work must follow `docs/02-harness/managed-change-standard.md`; reusable stage-package guidance is available at `docs/07-templates/managed-change-stage-package-template.md`

## Active Authorized Change

- Change name: `offline-replay-and-readiness-gate`
- Authorized scope: Java implementation for `experiment.analysis` and its bounded tests, plus OpenSpec apply/verify/finalize evidence for this change only
- Execution status: Java implementation is authorized within the bounded change scope
- Authority source: `docs/04-development/versions/v0.4.0/23-sr-closure-verification.md`

## Active Authorized Version Work

- Version name: `v0.4.0`
- Work type: bounded change implementation
- Bounded by: `docs/04-development/versions/v0.4.0/`
- Scope: implement `offline-replay-and-readiness-gate`, including baseline evidence validation, offline policy replay, replay decision evidence, summary aggregation, sensitivity comparison, readiness gate, controlled report artifacts, and bounded tests
- Non-scope: executor mutation, queue resizing, scheduler changes, persistence, external API, new dependencies, production adaptive control, neighboring changes outside `offline-replay-and-readiness-gate`

## What Is Allowed Now

- Implement Java source and tests only for `offline-replay-and-readiness-gate`.
- Update `tasks.md`, `apply.md`, `verify.md`, and `finalize.md` only as implementation evidence for this change.
- Keep the current-state record synchronized with the actual repository state.
- Inspect archived artifacts and synchronized specs as evidence.
- Maintain managed-change standards and templates while executing the bounded change.

## What Is Not Allowed Now

- No new OpenSpec change creation beyond `offline-replay-and-readiness-gate` without a successor version design and explicit authorization in this file.
- No unreviewed scope expansion.
- No branch-state mismatch between the workspace and the authoritative branch.
- No archive or finalize event without synchronized authority records.
- No new dependencies.
- No Java source or test change outside the bounded `offline-replay-and-readiness-gate` scope.
- No executor mutation, queue resizing, scenario behavior change, persistence, external API, or neighboring capability implementation under this authorization.

## Future Gate Sequence

1. `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
2. version design draft
3. version design baseline
4. `READY_FOR_CHANGE_DECOMPOSITION`
5. `EXECUTION_AUTHORIZED` ← current stage
6. capability change execution

## Current Reference

The repository currently preserves earlier bootstrap and framework history in `docs/99-archive/`. Those files are historical evidence only.
