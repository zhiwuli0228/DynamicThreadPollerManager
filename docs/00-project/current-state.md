# Current State

## Authoritative Status

- Current stage: `EXECUTION_AUTHORIZED`
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.2.0` is `EXECUTION_AUTHORIZED` for `scenario-runner-and-baseline`
- OpenSpec capability changes: `experiment-foundation` and `metrics-snapshot-and-recording` have been archived; `scenario-runner-and-baseline` is authorized for superspec artifact creation and later implementation
- Java implementation status: the experiment foundation package and the metrics observation layer (sampling, normalization, append-only recording, summary) are present on the main working branch; scenario runner implementation is not present yet

## Active Authorized Change

- Change name: `scenario-runner-and-baseline`
- Bounded by: `docs/04-development/versions/v0.2.0/`
- Schema: `superspec`
- Scope: create and execute the third superspec change for deterministic scenario running and fixed baseline execution
- Non-scope: adaptive policy, executor resizing, queue mutation, external observability, new dependencies

## What Is Allowed Now

- Maintain the documentation framework.
- Update cross-links so future work can discover the correct authority sequence quickly.
- Create and maintain `openspec/changes/scenario-runner-and-baseline/` using the `superspec` schema.
- Implement `scenario-runner-and-baseline` after its full artifact set is complete.
- Keep the current-state record synchronized with the actual repository state.

## What Is Not Allowed Now

- No OpenSpec change creation outside `scenario-runner-and-baseline`.
- No unreviewed scope expansion.
- No branch-state mismatch between the workspace and the authoritative branch.
- No archive or finalize event without synchronized authority records.
- No new dependencies.
- No Java source or test change outside `scenario-runner-and-baseline`.

## Future Gate Sequence

1. `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
2. version design draft
3. version design baseline
4. `READY_FOR_CHANGE_DECOMPOSITION`
5. `EXECUTION_AUTHORIZED` ← current stage
6. capability change execution

## Current Reference

The repository currently preserves earlier bootstrap and framework history in `docs/99-archive/`. Those files are historical evidence only.
