# Current State

## Authoritative Status

- Current stage: `READY_FOR_CHANGE_DECOMPOSITION`
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.3.0` is `READY_FOR_CHANGE_DECOMPOSITION`
- OpenSpec capability changes: `experiment-foundation`, `metrics-snapshot-and-recording`, and `scenario-runner-and-baseline` have been archived; the v0.1.0 and v0.2.0 capability baselines are present on `claude_master` and verified behavior is synchronized to `openspec/specs/`; the v0.3.0 design authorizes decomposition for `adaptive-policy-and-control-gate`
- Java implementation status: the experiment foundation package, the metrics observation layer (sampling, normalization, append-only recording, summary), and the deterministic scenario runner with fixed baseline executor are present on the main working branch

## Active Authorized Change

- Change name: `none` — no active OpenSpec change exists yet
- Bounded by: `docs/04-development/versions/v0.3.0/`
- Schema: `superspec`
- Scope: create `openspec/changes/adaptive-policy-and-control-gate/` using `superspec` and produce full change artifacts
- Non-scope: no Java implementation until the OpenSpec change artifacts are complete and execution is explicitly authorized

## What Is Allowed Now

- Maintain the documentation framework.
- Update cross-links so future work can discover the correct authority sequence quickly.
- Create and maintain `openspec/changes/adaptive-policy-and-control-gate/` using the `superspec` schema.
- Produce full change artifacts before implementation.
- Keep the current-state record synchronized with the actual repository state.

## What Is Not Allowed Now

- No new OpenSpec change creation outside `adaptive-policy-and-control-gate`.
- No unreviewed scope expansion.
- No branch-state mismatch between the workspace and the authoritative branch.
- No archive or finalize event without synchronized authority records.
- No new dependencies.
- No Java source or test change until `adaptive-policy-and-control-gate` artifacts are complete and execution is authorized.

## Future Gate Sequence

1. `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
2. version design draft
3. version design baseline
4. `READY_FOR_CHANGE_DECOMPOSITION` ← current stage
5. `EXECUTION_AUTHORIZED`
6. capability change execution

## Current Reference

The repository currently preserves earlier bootstrap and framework history in `docs/99-archive/`. Those files are historical evidence only.
