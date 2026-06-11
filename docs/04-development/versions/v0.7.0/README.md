# v0.7.0 ManagedExecutor 受控执行器域设计

## Header

- Version name: `v0.7.0`
- Authoring date: `2026-06-11`
- Status: `CHANGE_1_ARCHIVED` — change 1/3 `establish-managed-executor-and-registry` archived; changes 2/3 and 3/3 pending
- Current phase: `DESIGN_ONLY` — preparing change 2/3 `bridge-adjustment-to-real-executor`
- Requirement theme: managed executor domain, real ThreadPoolExecutor bridging, first closed-loop experiment
- Authoritative branch: `claude_master`

## Purpose

`v0.7.0` 是项目从"实验基础设施构建"转向"真实执行器管理"的第一个版本。

v0.1.0 到 v0.6.0 围绕 `InMemoryAdjustableExecutorProbe` 建立了完整的实验、指标、策略、分析和数据获取能力。但这些能力从未作用在一个真实的 `ThreadPoolExecutor` 上 — 项目名字是 `DynamicThreadPollerManager`，却还没有真正管理过任何线程池。

v0.7.0 不引入 queue resizing、不引入闭环调度器、不进入生产环境。它的唯一目标是：**在一个受控的 `ManagedExecutor`（包装真实 `ThreadPoolExecutor`）上，完成首次"场景→采集→策略→调整→验证"闭环实验**。

## Managed Change Gate

本版本必须遵守 [管理变更标准](../../../../docs/02-harness/managed-change-standard.md)：

1. 完成 Version Baseline（本文档 + objectives-and-scope + decision-log）。
2. 完成 IR 需求草案 → review → disposition → closure verification。
3. IR 闭环通过后，进入 SR 功能设计 → review → disposition → closure verification。
4. SR 闭环通过后，创建基于 `superspec` 的 OpenSpec change。
5. 实现 → Implementation Review → Test Design/Review → Acceptance Precheck → Archive。
6. 完成后进入 retrospective 复盘。

## Document Set

### Completed

- `README.md`（本文档）
- `00-objectives-and-scope.md`
- `decision-log.md`
- `10-ir.md`
- `11-ir-review.md`
- `12-ir-review-disposition.md`
- `13-ir-closure-verification.md`
- `20-sr.md`
- `21-sr-review.md`
- `22-sr-review-disposition.md`
- `23-sr-closure-verification.md`

### Change 1/3: establish-managed-executor-and-registry — ARCHIVED

- Archive location: `openspec/changes/archive/2026-06-12-establish-managed-executor-and-registry/`
- Main spec: `openspec/specs/establish-managed-executor-and-registry/spec.md`
- Status: 394 tests pass, all 29 tasks complete, post-archive guard verified
- Delivers: `ManagedExecutor`, `ExecutorRegistry`, `RuntimeSetting`, `DeletionSafety`, `ExecutorStateSnapshot` extension

### Change 2/3: bridge-adjustment-to-real-executor — PENDING

- Status: not yet drafted
- Scope: `ManagedExecutorAdjustmentAdapter`, safety gate integration, adjustment bridge

### Change 3/3: closed-loop-experiment-verification — PENDING

- Status: not yet drafted
- Scope: closed-loop experiment execution and verification

## Current Scope Boundary

Allowed now:

- Draft next OpenSpec change for v0.7.0 change 2/3.
- Inspect v0.1.0-v0.7.0-change-1 archived artifacts and synchronized specs as design input.
- Reference living architecture documents as target boundary.

Not allowed now:

- Java source or test change (requires new `EXECUTION_AUTHORIZED`).
- Queue resizing, production `ThreadPoolExecutor` integration beyond what change 2/3 authorizes, closed-loop scheduler/controller, persistence, REST/API/UI, external dependencies.
