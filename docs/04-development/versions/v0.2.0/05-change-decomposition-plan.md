# v0.2.0 Change Decomposition Plan

## Header

- Version name: `v0.2.0`
- Status: `EXECUTION_AUTHORIZED`
- Authorization status: one OpenSpec change authorized

## 1. Authorized Change

### Change: `scenario-runner-and-baseline`

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

This change depends on delivered capabilities:

- `experiment-foundation`,
- `metrics-snapshot-and-recording`.

## 4. Execution Authorization

`scenario-runner-and-baseline` is authorized to be created under `openspec/changes/` using the `superspec` schema and implemented after its full artifact set is produced.
