# Current State

## Authoritative Status

- Current stage: `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.1.0` is `BASELINE_DELIVERED`
- OpenSpec capability changes: `experiment-foundation` and `metrics-snapshot-and-recording` have been archived; the v0.1.0 capability baseline is present on `claude_master` and verified behavior is synchronized to `openspec/specs/`
- Java implementation status: the experiment foundation package and the metrics observation layer (sampling, normalization, append-only recording, summary) are present on the main working branch

## Active Authorized Change

- Change name: `none` — v0.1.0 is delivered; the next change requires a new version design
- Bounded by: `v0.1.0` is the most recent baseline; v0.2.0 (or any successor) must be authored and reach `READY_FOR_CHANGE_DECOMPOSITION` before new `openspec/changes/**` work begins
- Schema: `superspec`
- Scope: post-delivery maintenance, documentation hygiene, and preparation of the next version design package
- Non-scope: no new capability implementation until a successor version design is created and authorized

## What Is Allowed Now

- Maintain the documentation framework.
- Update cross-links so future work can discover the correct authority sequence quickly.
- Author a successor version design (e.g., `v0.2.0`) under `docs/04-development/versions/`.
- Keep the current-state record synchronized with the actual repository state.

## What Is Not Allowed Now

- No new OpenSpec change creation until a successor version design is `READY_FOR_CHANGE_DECOMPOSITION` or `EXECUTION_AUTHORIZED`.
- No unreviewed scope expansion.
- No branch-state mismatch between the workspace and the authoritative branch.
- No archive or finalize event without synchronized authority records.
- No new dependencies.
- No Java source or test change outside an authorized change.

## Future Gate Sequence

1. `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED` ← current stage
2. version design draft
3. version design baseline
4. `READY_FOR_CHANGE_DECOMPOSITION`
5. `EXECUTION_AUTHORIZED`
6. capability change execution

## Current Reference

The repository currently preserves earlier bootstrap and framework history in `docs/99-archive/`. Those files are historical evidence only.
