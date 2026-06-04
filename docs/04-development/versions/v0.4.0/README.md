# v0.4.0 版本需求草案

## Header

- Version name: `v0.4.0`
- Authoring date: `2026-06-04`
- Status: `BASELINED`
- Current phase: `SR_FUNCTIONAL_DESIGN_AUTHORIZED`
- Current conclusion: IR closure verified; SR functional design may start
- Authoritative branch: `claude_master`

## Purpose

`v0.4.0` 用于定义下一阶段需求：在进入 executor mutation、queue resizing 或闭环 adaptive control 之前，先获取和固化 baseline pressure evidence、offline policy replay、decision evidence 和最小实验报告输入。

本版本当前只处于需求草案阶段，不授权 OpenSpec change，也不授权 Java 实现。

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
- [decision-log.md](./decision-log.md)

Next gated artifacts:

- `20-sr.md`
- `21-sr-review.md`
- `22-sr-review-disposition.md`
- `23-sr-closure-verification.md`

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

The IR defines `AC-v0.4-001` through `AC-v0.4-012`, and the IR review/closure set is now complete. These ACs now authorize SR functional design input, but they still do not authorize OpenSpec or implementation work.

## Current Non-Scope

- No OpenSpec change.
- No Java source or test implementation.
- No executor mutation.
- No queue capacity resizing.
- No scheduler or scenario execution behavior change.
- No persistence, REST API, UI, or production integration.
