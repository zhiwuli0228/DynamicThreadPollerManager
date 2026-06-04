# Current State

## Authoritative Status

- Current stage: `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
- Current authorized work type: `DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY`
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.3.0` is `IMPLEMENTED`
- OpenSpec capability changes: `experiment-foundation`, `metrics-snapshot-and-recording`, `scenario-runner-and-baseline`, and `adaptive-policy-and-control-gate` have been archived; the v0.1.0, v0.2.0, and v0.3.0 capability baselines are present on `claude_master` and verified behavior is synchronized to `openspec/specs/`
- Java implementation status: the experiment foundation package, the metrics observation layer (sampling, normalization, append-only recording, summary), the deterministic scenario runner with fixed baseline executor, and the adaptive policy/control-gate package are present on the main working branch
- Governance status: future capability work must follow `docs/02-harness/managed-change-standard.md`; reusable stage-package guidance is available at `docs/07-templates/managed-change-stage-package-template.md`

## Active Authorized Change

- None.
- No OpenSpec change or Java implementation scope is active until this file explicitly authorizes a successor version design and change.

## What Is Allowed Now

- Maintain the documentation framework.
- Update cross-links so future work can discover the correct authority sequence quickly.
- Keep the current-state record synchronized with the actual repository state.
- Inspect archived artifacts and synchronized specs as evidence.
- Maintain managed-change standards and templates without authorizing new Java implementation.

## What Is Not Allowed Now

- No new OpenSpec change creation without a successor version design and explicit authorization in this file.
- No unreviewed scope expansion.
- No branch-state mismatch between the workspace and the authoritative branch.
- No archive or finalize event without synchronized authority records.
- No new dependencies.
- No Java source or test change unless a later explicit task updates this file to authorize bounded implementation.

## Future Gate Sequence

1. `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED` ← current stage
2. version design draft
3. version design baseline
4. `READY_FOR_CHANGE_DECOMPOSITION`
5. `EXECUTION_AUTHORIZED`
6. capability change execution

## Current Reference

The repository currently preserves earlier bootstrap and framework history in `docs/99-archive/`. Those files are historical evidence only.
