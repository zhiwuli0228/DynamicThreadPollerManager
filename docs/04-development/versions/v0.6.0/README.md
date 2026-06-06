# v0.6.0 压测数据获取需求设计

## Header

- Version name: `v0.6.0`
- Authoring date: `2026-06-06`
- Status: `SR_CLOSED`
- Current phase: `READY_FOR_CHANGE_DECOMPOSITION`
- Requirement theme: pressure data acquisition and reproducible baseline evidence
- Current conclusion: IR review、disposition、closure verification、SR review、disposition 和 closure verification 已完成；v0.6.0 现已授权进入 OpenSpec change decomposition，但仍不授权 Java 实现
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
- [decision-log.md](./decision-log.md)

Pending:

- OpenSpec change decomposition package

## Current Scope Boundary

Allowed now:

- 设计 v0.6.0 OpenSpec change decomposition。
- 读取已归档 capability spec、v0.6.0 IR / SR 闭环文档和当前主 spec 作为输入证据。
- 定义后续 OpenSpec change 的 proposal、design、spec、tasks 和 plan 草案边界。
- 明确后续是否需要新 OpenSpec change，但不得创建未授权实现。

Not allowed now:

- 不修改 Java 源码或测试。
- 不执行 OpenSpec apply/verify/archive。
- 不创建 `openspec/changes/**`。
- 不实现新的压测 runner、CLI、报告 writer 或持久化。
- 不执行 queue resizing、生产 executor mutation、closed-loop controller、scheduler、REST/API/UI 或外部依赖。
- 不声明性能提升结论；只能定义未来如何采集和判定数据。
