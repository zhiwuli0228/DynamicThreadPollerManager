# Current State

## Authoritative Status

- Current stage: `EXECUTION_AUTHORIZED`
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.3.0` is `EXECUTION_AUTHORIZED`
- OpenSpec capability changes: `experiment-foundation`, `metrics-snapshot-and-recording`, and `scenario-runner-and-baseline` have been archived; the v0.1.0 and v0.2.0 capability baselines are present on `claude_master` and verified behavior is synchronized to `openspec/specs/`; `adaptive-policy-and-control-gate` has complete pre-implementation artifacts and is authorized for implementation
- Java implementation status: the experiment foundation package, the metrics observation layer (sampling, normalization, append-only recording, summary), and the deterministic scenario runner with fixed baseline executor are present on the main working branch

## Active Authorized Change

- Change name: `adaptive-policy-and-control-gate`
- Bounded by: `docs/04-development/versions/v0.3.0/`
- Schema: `superspec`
- Scope: implement `adaptive-policy-and-control-gate` according to `openspec/changes/adaptive-policy-and-control-gate/`
- Non-scope: executor mutation, queue resizing, scenario execution changes, persistence, external API, new dependencies, neighboring capability changes

## What Is Allowed Now

- Maintain the documentation framework.
- Update cross-links so future work can discover the correct authority sequence quickly.
- Implement `openspec/changes/adaptive-policy-and-control-gate/` using the existing superspec artifacts.
- Run `/opsx:apply adaptive-policy-and-control-gate` or equivalent bounded implementation workflow.
- Update `tasks.md` only as implementation tasks are actually completed.
- Keep the current-state record synchronized with the actual repository state.

## What Is Not Allowed Now

- No new OpenSpec change creation outside `adaptive-policy-and-control-gate`.
- No unreviewed scope expansion.
- No branch-state mismatch between the workspace and the authoritative branch.
- No archive or finalize event without synchronized authority records.
- No new dependencies.
- No Java source or test change outside `adaptive-policy-and-control-gate`.

## Future Gate Sequence

1. `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
2. version design draft
3. version design baseline
4. `READY_FOR_CHANGE_DECOMPOSITION`
5. `EXECUTION_AUTHORIZED` ← current stage
6. capability change execution

## Current Reference

The repository currently preserves earlier bootstrap and framework history in `docs/99-archive/`. Those files are historical evidence only.
