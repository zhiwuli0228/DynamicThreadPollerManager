# v0.4.0 版本需求草案

## Header

- Version name: `v0.4.0`
- Authoring date: `2026-06-05`
- Status: `EXECUTION_AUTHORIZED`
- Current phase: `BOUNDED_CHANGE_IMPLEMENTATION_AUTHORIZED`
- Current conclusion: the `offline-replay-and-readiness-gate` change decomposition package has been approved for bounded implementation
- Authoritative branch: `claude_master`

## Purpose

`v0.4.0` 用于定义下一阶段需求：在进入 executor mutation、queue resizing 或闭环 adaptive control 之前，先获取和固化 baseline pressure evidence、offline policy replay、decision evidence 和最小实验报告输入。

本版本已授权 `offline-replay-and-readiness-gate` 进入受控实现阶段，但只允许在该 change 边界内修改 Java 源码、测试和 OpenSpec 执行证据。

## Managed Change Gate

本版本必须遵守 [管理变更标准](../../../../02-harness/managed-change-standard.md)：

1. 完成 IR 需求分析。
2. 完成独立 IR review。
3. 完成 IR review disposition。
4. 完成 IR closure verification。
5. 只有 IR 闭环通过后，才能进入 SR 功能设计。
6. 只有 SR 闭环通过后，才能创建 OpenSpec change。

## Document Set

Current:

- [00-objectives-and-scope.md](./00-objectives-and-scope.md)
- [10-ir.md](./10-ir.md)
- [11-ir-review.md](./11-ir-review.md)
- [12-ir-review-disposition.md](./12-ir-review-disposition.md)
- [13-ir-closure-verification.md](./13-ir-closure-verification.md)
- [20-sr.md](./20-sr.md)
- [21-sr-review.md](./21-sr-review.md)
- [22-sr-review-disposition.md](./22-sr-review-disposition.md)
- [23-sr-closure-verification.md](./23-sr-closure-verification.md)
- [decision-log.md](./decision-log.md)

Next gated artifacts:

- `apply.md`
- `verify.md`
- `finalize.md`

## Current IR Summary

The current IR defines eight requirement items:

- `IR-v0.4-001`: baseline pressure evidence.
- `IR-v0.4-002`: offline policy replay.
- `IR-v0.4-003`: decision evidence.
- `IR-v0.4-004`: replay summary.
- `IR-v0.4-005`: threshold sensitivity.
- `IR-v0.4-006`: executor mutation readiness gate.
- `IR-v0.4-007`: evidence hygiene and boundary isolation.
- `IR-v0.4-008`: reviewable experiment readiness criteria.

The IR and SR closure sets are complete, the change decomposition package for `offline-replay-and-readiness-gate` exists under `openspec/changes/`, and `docs/00-project/current-state.md` now authorizes bounded Java implementation for that change only.

## Current Non-Scope

- No executor mutation.
- No queue capacity resizing.
- No scheduler or scenario execution behavior change.
- No persistence, REST API, UI, or production integration.
- No neighboring capability implementation outside `offline-replay-and-readiness-gate`.
