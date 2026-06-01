# Current State

## Authoritative Status

- Current stage: `EXECUTION_AUTHORIZED`
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.1.0` is `EXECUTION_AUTHORIZED`
- OpenSpec capability changes: `experiment-foundation` has been archived; `metrics-snapshot-and-recording` is the active authorized change
- Java implementation status: the experiment foundation package and tests are present on the main working branch; the metrics observation layer is the next authorized increment

## Active Authorized Change

- Change name: `metrics-snapshot-and-recording`
- Bounded by: `v0.1.0` version design, work package 2 (Runtime snapshot collector) and supporting evidence/recording/summary work
- Schema: `superspec`
- Scope: add a read-only observation layer (`src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/metrics/`) that samples, normalizes, append-only records, and summarizes pressure snapshots for experiment runs
- Non-scope: no policy evaluation, no scale decisions, no executor or queue mutation, no scenario scheduling, no new dependencies

## What Is Allowed Now

- Implement the `metrics-snapshot-and-recording` change in line with its `tasks.md` and `plan.md`.
- Maintain the documentation framework.
- Update cross-links so future work can discover the correct authority sequence quickly.
- Keep the current-state record synchronized with the actual repository state.

## What Is Not Allowed Now

- No unreviewed scope expansion beyond `metrics-snapshot-and-recording`.
- No branch-state mismatch between the workspace and the authoritative branch.
- No archive or finalize event without synchronized authority records.
- No new capability change outside the approved version-design path.
- No new dependencies.

## Future Gate Sequence

1. `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
2. version design draft
3. version design baseline
4. `READY_FOR_CHANGE_DECOMPOSITION`
5. `EXECUTION_AUTHORIZED` ← current stage
6. capability change execution

## Current Reference

The repository currently preserves earlier bootstrap and framework history in `docs/99-archive/`. Those files are historical evidence only.
