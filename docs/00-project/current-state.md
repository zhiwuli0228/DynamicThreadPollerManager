# Current State

## Authoritative Status

- Current stage: `ARCHIVED` — v0.12.0 both changes archived
- Current authorized work type: `NONE` — no active implementation
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.1.0` through `v0.12.0` are `IMPLEMENTED` / `ARCHIVED`
- OpenSpec capability changes: 20 implemented + archived
- Java implementation status: all experiment packages (foundation, metrics, scenario, policy, analysis, adjustment, acquisition, executor) are present; 708 tests pass with 0 failures (1 pre-existing flaky)

## Active Authorized Changes

- (none — v0.12.0 changes archived)

## v0.12.0 Change Summary

| Change | Name | Status | Tests |
|---|---|---|---|
| 1/2 | baseline-catalog-and-comparison-runner | ARCHIVED (2026-06-14) | 697 pass |
| 2/2 | comparison-report-and-end-to-end-verification | ARCHIVED (2026-06-14) | 707 pass |

## v0.10.0 Change Summary

| Change | Name | Status | Tests |
|---|---|---|---|
| 1/2 | rejection-policy-command-and-adapter | ARCHIVED (2026-06-13) | 526 pass |
| 2/2 | rejection-policy-end-to-end-verification | ARCHIVED (2026-06-13) | 534 pass |

Retrospective: `docs/08-retrospectives/2026-06-13-v0.10.0-rejection-policy-retrospective.md` — all three dynamic configuration dimensions delivered.

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

## v0.11.0 Change Summary

| Change | Name | Status | Tests |
|---|---|---|---|
| 1/2 | persistent-evidence-recorder | ARCHIVED (2026-06-13) | 594 pass |
| 2/2 | live-pressure-sampler-and-integration | ARCHIVED (2026-06-13) | 622 pass |

## v0.11.0 Plan

- Version design documents: `docs/04-development/versions/v0.11.0/`
- Requirement theme: persistent evidence recording, snapshot serialization, recording session lifecycle, live pressure sampling
- Key insight: v0.1.0 delivered in-memory-only metrics foundation; v0.11.0 adds durability and autonomous data collection
- Predecessor: v0.10.0 (rejection policy — completed dynamic config baseline)
- OpenSpec changes: `openspec/changes/persistent-evidence-recorder/` and `openspec/changes/live-pressure-sampler-and-integration/`
- Changes: `persistent-evidence-recorder` (change 1/2) → `live-pressure-sampler-and-integration` (change 2/2)
- Status: `ARCHIVED` — 622 tests passing, 0 failures

## parallel-test-execution Change Summary

| Change | Name | Status | Tests |
|---|---|---|---|
| 1/1 | parallel-test-execution | ARCHIVED (2026-06-14) | 646 pass |

- Change: `parallel-test-execution`
- Status: `ARCHIVED` — 646 tests passing, 0 failures
- Performance: sequential 4:46 → parallel 2:37 (~1.8x speedup)
- OpenSpec change: `openspec/changes/archive/2026-06-14-parallel-test-execution/`
- Main spec synced: `openspec/specs/parallel-test-execution-config/spec.md`

## What Is Allowed Now

- Documentation and retrospectives refinement.
- Planning for v0.13.0 or subsequent versions (requires version design first).

## What Is Not Allowed Now

- No new production code or tests without a new version design and EXECUTION_AUTHORIZED status.
- No modification to existing adapters or commands without design revision.

## v0.12.0 Plan

- Version design documents: `docs/04-development/versions/v0.12.0/`
- Requirement theme: baseline comparison experiment framework — catalog, comparable runner, normalized metrics, comparison report
- Key insight: the system can already run baseline and managed executors independently; v0.12.0 adds the comparison layer that runs the same workload against both and produces side-by-side delta reports
- Predecessor: v0.11.0 (persistent evidence recording and live sampling)
- OpenSpec changes: archived to `openspec/changes/archive/2026-06-14-baseline-catalog-and-comparison-runner/` and `openspec/changes/archive/2026-06-14-comparison-report-and-end-to-end-verification/`
- Changes: `baseline-catalog-and-comparison-runner` (change 1/2) → `comparison-report-and-end-to-end-verification` (change 2/2)
- Status: `ARCHIVED` — 708 tests passing, 0 new failures
- Main specs synced: 5 new specs (baseline-executor-catalog, normalized-comparison-metrics, comparable-scenario-runner, managed-executor-rejection-counting, comparison-report-artifact) + 3 modified specs
- Retrospective: pending

## Future Gate Sequence

1. ~~`CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`~~ (completed)
2. ~~`VERSION_DESIGN_DRAFT`~~ (completed — v0.8.0)
3. ~~`SR_DESIGN_AUTHORIZED`~~ (completed)
4. ~~`READY_FOR_CHANGE_DECOMPOSITION`~~ (completed)
5. ~~`CHANGE_DECOMPOSITION_COMPLETE`~~ (completed — 2 OpenSpec changes created)
6. ~~`EXECUTION_AUTHORIZED`~~ (completed)
7. ~~capability change execution~~ (completed — change 1/2 and 2/2 implemented)
8. ~~archive~~ (completed — 2026-06-13)
9. ~~v0.9.0 retrospective~~ (completed — 2026-06-13)
10. ~~v0.10.0 scope objectives and decision log created~~ (completed — 2026-06-13)
11. ~~v0.10.0 IR phase~~ (completed — 2026-06-13, IR closure verified)
12. ~~v0.10.0 SR phase~~ (completed — 2026-06-13, SR closure verified)
13. ~~v0.10.0 READY_FOR_CHANGE_DECOMPOSITION~~ (completed — 2026-06-13)
14. ~~v0.10.0 EXECUTION_AUTHORIZED~~ (completed — 2026-06-13)
15. ~~v0.10.0 capability change execution~~ (completed — 2026-06-13, both changes implemented)
16. ~~v0.10.0 archive~~ (completed — 2026-06-13, both changes archived)
17. ~~v0.10.0 retrospective~~ (completed — 2026-06-13)

## v0.10.0 Plan

- Version design documents: `docs/04-development/versions/v0.10.0/`
- Requirement theme: runtime rejection-policy replacement with rebuild policy preservation
- Key insight: `ThreadPoolExecutor.setRejectedExecutionHandler()` is a public JDK API — no executor rebuild needed (unlike v0.9.0 queue resize)
- Associated fix: `ExecutorRebuildStrategy` hardcodes `AbortPolicy()` — must preserve original rejection policy during rebuild
- OpenSpec changes: TBD after change decomposition
- Candidate changes: `rejection-policy-command-and-adapter` (change 1/2) → `rejection-policy-end-to-end-verification` (change 2/2)
- OpenSpec changes: `openspec/changes/rejection-policy-command-and-adapter/` and `openspec/changes/rejection-policy-end-to-end-verification/`
- Changes: `rejection-policy-command-and-adapter` (change 1/2) → `rejection-policy-end-to-end-verification` (change 2/2)
- Status: `ARCHIVED` — 534 tests passing, 0 failures, both changes archived, retrospective completed

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
