# v0.5.0 版本需求草案

## Header

- Version name: `v0.5.0`
- Authoring date: `2026-06-05`
- Status: `READY_FOR_CHANGE_DECOMPOSITION`
- Current phase: `SR_CLOSED`
- Requirement theme: executor adapter and queue resizing design readiness
- Current conclusion: IR and SR review, disposition, and closure are complete; bounded OpenSpec change decomposition may begin after current-state authorization
- Authoritative branch: `claude_master`

## Purpose

`v0.5.0` 用于定义进入 executor adapter、queue resizing 和运行时调整设计前的需求边界。它承接 `v0.4.0` 的 offline replay、threshold sensitivity 和 readiness gate 输出，但当前阶段只允许需求分析。

本版本的核心问题不是“立即实现 executor mutation”，而是明确什么数据、边界、失败语义和保护条件足以支撑后续 SR 设计。

## Managed Change Gate

本版本必须遵守 [管理变更标准](../../../../02-harness/managed-change-standard.md)：

1. 完成 IR 需求分析。
2. 完成独立 IR review。
3. 完成 IR review disposition。
4. 完成 IR closure verification。
5. 只有 IR 闭环通过后，才能进入 SR 功能设计。
6. 只有 SR 闭环通过后，才能创建 OpenSpec change。
7. 每个完成的需求或 bounded change 必须完成 retrospective。

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

- [OpenSpec change decomposition](../../../../openspec/changes/executor-adapter-and-adjustment-evidence/)

## Current Scope Boundary

Allowed after current-state authorization:

- Create bounded OpenSpec proposal/design/spec/tasks/plan artifacts for `executor-adapter-and-adjustment-evidence`.
- Use archived `v0.4.0` artifacts and synced specs as input evidence.
- Translate closed SR requirements into OpenSpec change artifacts.

Not allowed now:

- No OpenSpec change.
- No Java source or test change.
- No executor mutation.
- No queue resizing implementation.
- No scheduler, scenario, persistence, REST, UI, or external dependency change.
