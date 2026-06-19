# v0.2.0 Change Decomposition Plan

## Header

- Version name: `v0.2.0`
- Status: `BASELINE_DELIVERED`
- Authorization status: all authorized work delivered and archived

## 1. Delivered Change

### Change: `scenario-runner-and-baseline` → delivered and archived 2026-06-03

Core responsibility:

- define deterministic scenario profiles,
- generate repeatable scenario steps,
- provide a fixed baseline executor preset,
- execute a scenario through the foundation lifecycle,
- record metrics snapshots through the existing observation layer.

Key boundary:

- no adaptive policy,
- no scale decision,
- no executor resizing,
- no queue mutation,
- no external API.

## 2. Deferred Work

The following remain deferred and are not authorized by this version:

- `adaptive-policy-and-control-gate`,
- `executor-adapter-and-queue-resizing`,
- result persistence beyond current evidence recorder,
- analysis UI or reporting surface,
- production thread-pool management.

## 3. Dependency Basis

This change depended on delivered capabilities:

- `experiment-foundation`,
- `metrics-snapshot-and-recording`.

## 4. Execution Closure

`scenario-runner-and-baseline` is delivered: all 9 superspec artifacts are
complete, 93/93 Maven tests pass, verified behavior is synchronized to
`openspec/specs/scenario-runner-and-baseline/spec.md`, and the change
directory has been moved to `openspec/changes/archive/2026-06-03-scenario-runner-and-baseline/`.
A successor version (e.g., `v0.3.0`) is required before any new
`openspec/changes/**` work begins.
