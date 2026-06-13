# Current State

## Authoritative Status

- Current stage: `ARCHIVED` for `v0.9.0` — both changes implemented, verified, and archived
- Current authorized work type: `RETROSPECTIVE` — write v0.9.0 retrospective
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.1.0` through `v0.8.0` are `IMPLEMENTED`; `v0.9.0` is `IMPLEMENTED`
- OpenSpec capability changes: All 14 capability changes (experiment-foundation through queue-resize-end-to-end-verification) have been implemented; all capability baselines are present on `claude_master` and verified behavior is synchronized to `openspec/specs/`
- Java implementation status: all experiment packages (foundation, metrics, scenario, policy, analysis, adjustment, acquisition, executor) are present on the main working branch; 476 tests pass with 0 failures

## Active Authorized Changes

- No active changes pending — v0.9.0 implementation complete.

## v0.9.0 Change Summary

| Change | Name | Status | Tests |
|---|---|---|---|
| 1/2 | queue-resize-command-and-rebuild | ARCHIVED (2026-06-13) | 468 pass |
| 2/2 | queue-resize-end-to-end-verification | ARCHIVED (2026-06-13) | 476 pass |

## Archived Changes

- Change 1/2: `real-executor-data-acquisition` — **ARCHIVED** on 2026-06-13
  - Delivered: ManagedExecutorConfig, ManagedExecutorScenarioRunner (7-phase), SnapshotAssembler.fromExecutorState(), ManualPressureSampler.sampleFromExecutorState(), integration tests
- Change 2/2: `acquisition-paths-and-quality-gates` — **ARCHIVED** on 2026-06-13 (433 tests, 0 failures)
  - Delivered: AcquisitionReportPaths.forVersion(), AcquisitionReportWriter dual-arg constructor, RunSnapshot extension (G7-G9 fields), G7-G9 data quality gates, AcquisitionReportBridge, 9-run data acquisition test, RuntimeObservation extension (keepAliveTimeSeconds/largestPoolSize/taskCount)

## What Is Allowed Now

- Write v0.9.0 retrospective.
- Plan v0.10.0 scope.

## What Is Not Allowed Now

- No new Java implementation without version design authorization.
- No modification to ManagedExecutorAdjustmentAdapter, ScaleAdjustmentCommand, ManagedExecutor, ExecutorRegistry.

## Future Gate Sequence

1. ~~`CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`~~ (completed)
2. ~~`VERSION_DESIGN_DRAFT`~~ (completed — v0.8.0)
3. ~~`SR_DESIGN_AUTHORIZED`~~ (completed)
4. ~~`READY_FOR_CHANGE_DECOMPOSITION`~~ (completed)
5. ~~`CHANGE_DECOMPOSITION_COMPLETE`~~ (completed — 2 OpenSpec changes created)
6. ~~`EXECUTION_AUTHORIZED`~~ (completed)
7. ~~capability change execution~~ (completed — change 1/2 and 2/2 implemented)
8. ~~archive~~ (completed — 2026-06-13)
9. ~~v0.8.0 retrospective~~ (completed — 2026-06-13)

## v0.7.0 Change Summary

| Change | Name | Archive | Tests |
|---|---|---|---|
| 1/3 | establish-managed-executor-and-registry | `2026-06-12` | 394 pass |
| 2/3 | bridge-adjustment-to-real-executor | `2026-06-12` | 409 pass |
| 3/3 | closed-loop-experiment-verification | `2026-06-12` | 412 pass |

## v0.8.0 Plan

- Version design documents: `docs/04-development/versions/v0.8.0/`
- OpenSpec changes: `openspec/changes/real-executor-data-acquisition/` and `openspec/changes/acquisition-paths-and-quality-gates/`
- Changes: `real-executor-data-acquisition` (change 1/2) → `acquisition-paths-and-quality-gates` (change 2/2)
- Key input: `docs/04-development/versions/v0.7.0/15-experiment-data-acquisition-plan.md`
- Next gate: `ARCHIVE`

## v0.8.0 Change Summary

| Change | Name | Status | Tests |
|---|---|---|---|
| 1/2 | real-executor-data-acquisition | ARCHIVED (2026-06-13) | 432 pass |
| 2/2 | acquisition-paths-and-quality-gates | ARCHIVED (2026-06-13) | 433 pass |

## v0.9.0 Plan

- Version design documents: `docs/04-development/versions/v0.9.0/`
- Requirement theme: runtime queue capacity resizing with executor rebuild strategy
- Key challenge: ThreadPoolExecutor does not support work-queue replacement — requires decommission/commission cycle
- Surrounding infrastructure: ManagedExecutor, ExecutorRegistry, SafetyGate, AdjustmentAdapter, EvidenceRecorder all in place
- OpenSpec changes: `openspec/changes/queue-resize-command-and-rebuild/` and `openspec/changes/queue-resize-end-to-end-verification/`
- Changes: `queue-resize-command-and-rebuild` (change 1/2) → `queue-resize-end-to-end-verification` (change 2/2)
- Status: `ARCHIVED` — 476 tests passing, 0 failures
