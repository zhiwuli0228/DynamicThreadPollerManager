# Current State

## Authoritative Status

- Current stage: `SR_FUNCTIONAL_DESIGN_DRAFT` for `v0.9.0` — SR functional design document created (5 core components)
- Current authorized work type: `SR_REVIEW` — independent review of v0.9.0 SR
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.1.0` through `v0.8.0` are `IMPLEMENTED`; `v0.9.0` is `VERSION_DESIGN_DRAFT`
- OpenSpec capability changes: All 12 capability changes (experiment-foundation through acquisition-paths-and-quality-gates) have been implemented and archived; all capability baselines are present on `claude_master` and verified behavior is synchronized to `openspec/specs/`
- Java implementation status: all experiment packages (foundation, metrics, scenario, policy, analysis, adjustment, acquisition, executor) are present on the main working branch; 433 tests pass with 0 failures

## Active Authorized Change

- Change 1/2: `real-executor-data-acquisition` — **ARCHIVED** on 2026-06-13
  - Delivered: ManagedExecutorConfig, ManagedExecutorScenarioRunner (7-phase), SnapshotAssembler.fromExecutorState(), ManualPressureSampler.sampleFromExecutorState(), integration tests
- Change 2/2: `acquisition-paths-and-quality-gates` — **ARCHIVED** on 2026-06-13 (433 tests, 0 failures)
  - Delivered: AcquisitionReportPaths.forVersion(), AcquisitionReportWriter dual-arg constructor, RunSnapshot extension (G7-G9 fields), G7-G9 data quality gates, AcquisitionReportBridge, 9-run data acquisition test, RuntimeObservation extension (keepAliveTimeSeconds/largestPoolSize/taskCount)
- No active changes pending.

## What Is Allowed Now

- Proceed with v0.9.0 IR (requirements analysis) phase.
- Create v0.9.0 design documents (IR, SR).
- Plan v0.9.0 OpenSpec change decomposition.
- No Java source or test changes until EXECUTION_AUTHORIZED.

## What Is Not Allowed Now

- No Java source or test changes without EXECUTION_AUTHORIZED.
- No new dependencies, persistence, REST/API/UI without explicit authorization.

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
- Next gate: `IR` — requirements analysis
