# v0.8.0 真实数据获取与 Metrics 管道集成

## Header

- Version name: `v0.8.0`
- Authoring date: `2026-06-12`
- Status: `IMPLEMENTED`
- Current phase: `IMPLEMENTED`
- Authoritative branch: `claude_master`

## Purpose

v0.8.0 在 v0.7.0 的 ManagedExecutor 域基础上，实现真实线程池上的多场景数据获取（STEADY/RAMP/BURST），集成 ExecutorStateSnapshot 到标准 metrics 管道，并版本化 acquisition report 输出路径。

## Scope Summary

| # | Change | Scope | Status |
|---|---|---|---|
| 1/2 | `real-executor-data-acquisition` | ManagedExecutorConfig, ManagedExecutorScenarioRunner, SnapshotAssembler.fromExecutorState(), RunnerTest | COMPLETE |
| 2/2 | `acquisition-paths-and-quality-gates` | AcquisitionReportPaths.forVersion(), RunSnapshot extension, G7-G9 gates, AcquisitionReportBridge, 9-run acquisition test, RuntimeObservation extension | COMPLETE |

## Verification

- `mvn test`: 433 tests, 0 failures, 0 errors
- 9-run data acquisition test passes all G1-G9 gates with real executor

## Key Decisions

See `decision-log.md` for the full decision record.

- D1: New `ManagedExecutorScenarioRunner`, not modify `ScenarioExperimentRunner`
- D2: Extend `SnapshotAssembler` with `fromExecutorState()` default method
- D3: Add `forVersion(String)` factory; keep backward-compatible default
- D4: Extend validator with G7-G9; add `extendedFieldPresence` to `RunSnapshot`
- D5: 3 changes serial (1 → 2 | 3 parallel)

## Predecessor

- v0.7.0 ManagedExecutor 域与闭环实验（IMPLEMENTED）
- v0.7.0 数据获取方案 `15-experiment-data-acquisition-plan.md`

## Document Set

- `README.md`
- `00-objectives-and-scope.md`
- `decision-log.md`
- `10-ir.md` — 需求分析草案（IR review 已闭环）
- `11-ir-review.md` — 独立 IR 评审报告（10 findings）
- `12-ir-review-disposition.md` — IR 评审处置（6 FIX + 4 DEFER_TO_SR）
- `13-ir-closure-verification.md` — IR 闭环验证（全部 CLOSED）
- `20-sr.md` — 功能设计（SR review 已闭环）
- `21-sr-review.md` — 独立 SR 评审报告（6 findings）
- `22-sr-review-disposition.md` — SR 评审处置（5 FIX + 1 ACCEPT）
- `23-sr-closure-verification.md` — SR 闭环验证（全部 CLOSED）

## OpenSpec Changes

- `openspec/changes/real-executor-data-acquisition/` — change 1/2
- `openspec/changes/acquisition-paths-and-quality-gates/` — change 2/2

## Next Step

`ARCHIVE` — both changes implemented and verified. Proceed to archive changes and then v0.8.0 retrospective.
