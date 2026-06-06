# v0.6.0 压测数据获取需求设计

## Header

- Version name: `v0.6.0`
- Authoring date: `2026-06-06`
- Status: `IMPLEMENTED`
- Current phase: `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
- Requirement theme: pressure data acquisition and reproducible baseline evidence
- Current conclusion: IR review、disposition、closure verification、SR review、disposition、closure verification、OpenSpec implementation、verification、finalize、archive and spec synchronization are complete for `pressure-data-acquisition-and-baseline`
- Authoritative branch: `claude_master`

## Purpose

`v0.6.0` 用于把“是否可以进入 executor mutation、queue resizing 或更强控制策略设计”的判断建立在可复现的压力数据上。

本版本不直接实现新的业务能力，不修改现有 Java 源码，不执行 queue resizing，不接入生产 `ThreadPoolExecutor`。当前阶段只设计数据获取需求和实验计划，确保后续压测不是临时手工操作，而是受管理、可复核、可追踪的证据生产过程。

## Managed Change Gate

本版本必须遵守 [管理变更标准](../../../../docs/02-harness/managed-change-standard.md)：

1. 完成 IR 需求草案。
2. 完成独立 IR review。
3. 完成 IR review disposition。
4. 完成 IR closure verification。
5. 只有 IR 闭环通过后，才能进入 SR 功能设计。
6. 只有 SR 闭环通过后，才能创建基于 `superspec` 的 OpenSpec change。
7. 每个完成的需求、版本或 bounded change 必须完成 retrospective。

## Document Set

Current:

- [00-objectives-and-scope.md](./00-objectives-and-scope.md)
- [10-ir.md](./10-ir.md)
- [15-experiment-data-acquisition-plan.md](./15-experiment-data-acquisition-plan.md)
- [20-sr.md](./20-sr.md)
- [21-sr-review.md](./21-sr-review.md)
- [22-sr-review-disposition.md](./22-sr-review-disposition.md)
- [23-sr-closure-verification.md](./23-sr-closure-verification.md)
- [24-completion-assessment.md](./24-completion-assessment.md)
- [decision-log.md](./decision-log.md)

Archived OpenSpec artifacts:

- `openspec/changes/archive/2026-06-06-pressure-data-acquisition-and-baseline/apply.md`
- `openspec/changes/archive/2026-06-06-pressure-data-acquisition-and-baseline/verify.md`
- `openspec/changes/archive/2026-06-06-pressure-data-acquisition-and-baseline/finalize.md`
- `openspec/specs/pressure-data-acquisition-and-baseline/spec.md`

Remediation package:

- [remediation/README.md](./remediation/README.md)
- `remediation/` contains a separate, full-process design for the real-data gap remediation path and does not rewrite the original `v0.6.0` design docs.

## Current Scope Boundary

Allowed now:

- Inspect archived `pressure-data-acquisition-and-baseline` artifacts and synchronized main spec as evidence.
- Use the remediation package as an independent design reference when explicitly authorized.
- Maintain governance, verification, and retrospective documents when explicitly authorized.

Not allowed now:

- No Java source or test change without a new authorized change.
- No additional OpenSpec change creation without new authorization.
- No queue resizing implementation.
- No production `ThreadPoolExecutor` integration.
- No scheduler, scenario, persistence, REST, UI, or external dependency change.
