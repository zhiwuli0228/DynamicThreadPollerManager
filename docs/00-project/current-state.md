# Current State

## Authoritative Status

- Current stage: `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
- Current authorized work type: `NONE` (no active bounded change)
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.1.0`, `v0.2.0`, `v0.3.0`, and `v0.4.0` are `IMPLEMENTED`
- OpenSpec capability changes: `experiment-foundation`, `metrics-snapshot-and-recording`, `scenario-runner-and-baseline`, `adaptive-policy-and-control-gate`, and `offline-replay-and-readiness-gate` have been archived; the v0.1.0, v0.2.0, v0.3.0, and v0.4.0 capability baselines are present on `claude_master` and verified behavior is synchronized to `openspec/specs/`
- Java implementation status: the experiment foundation package, the metrics observation layer (sampling, normalization, append-only recording, summary), the deterministic scenario runner with fixed baseline executor, the adaptive policy/control-gate package, and the read-only `experiment.analysis` package (evidence validation, offline policy replay, summary aggregation, threshold sensitivity comparison, mutation readiness assessment, and controlled report artifacts) are present on the main working branch
- Governance status: future capability work must follow `docs/02-harness/managed-change-standard.md`; reusable stage-package guidance is available at `docs/07-templates/managed-change-stage-package-template.md`

## Active Authorized Change

- None. The most recent bounded change, `offline-replay-and-readiness-gate`, was archived on 2026-06-05 to `openspec/changes/archive/2026-06-05-offline-replay-and-readiness-gate/` and its delta spec is mirrored at `openspec/specs/offline-replay-and-readiness-gate/spec.md`.

## Active Authorized Version Work

- None. v0.4.0 is fully implemented and archived; a successor version requires a new version design under `docs/04-development/versions/<next-version>/` with explicit authorization in this file before any new openspec change may be created.

## What Is Allowed Now

- Inspect archived artifacts and synchronized specs as evidence.
- Maintain managed-change standards and templates.
- Keep the current-state record synchronized with the actual repository state.
- Re-authorize a successor version by creating a new version design under `docs/04-development/versions/<next-version>/` and updating this file.

## What Is Not Allowed Now

- No new OpenSpec change creation without a successor version design and explicit authorization in this file.
- No unreviewed scope expansion.
- No branch-state mismatch between the workspace and the authoritative branch.
- No archive or finalize event without synchronized authority records.
- No new dependencies.
- No Java source or test change outside the archived `offline-replay-and-readiness-gate` scope unless a successor version is authorized.
- No executor mutation, queue resizing, scenario behavior change, persistence, external API, or neighboring capability implementation under this authorization.

## Future Gate Sequence

1. `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED` ← current stage
2. version design draft
3. version design baseline
4. `READY_FOR_CHANGE_DECOMPOSITION`
5. `EXECUTION_AUTHORIZED`
6. capability change execution

## Current Reference

The repository currently preserves earlier bootstrap and framework history in `docs/99-archive/`. Those files are historical evidence only.
