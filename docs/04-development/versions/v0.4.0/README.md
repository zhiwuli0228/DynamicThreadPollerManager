# v0.4.0 版本需求草案

## Header

- Version name: `v0.4.0`
- Authoring date: `2026-06-04`
- Status: `DRAFT`
- Current phase: `IR_REQUIREMENT_DRAFT`
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

## Document Set

Current:

- [00-objectives-and-scope.md](./00-objectives-and-scope.md)
- [10-ir.md](./10-ir.md)
- [decision-log.md](./decision-log.md)

Future gated artifacts:

- `11-ir-review.md`
- `12-ir-review-disposition.md`
- `13-ir-closure-verification.md`
- `20-sr.md`
- `21-sr-review.md`
- `22-sr-review-disposition.md`
- `23-sr-closure-verification.md`

## Current Non-Scope

- No OpenSpec change.
- No Java source or test implementation.
- No executor mutation.
- No queue capacity resizing.
- No scheduler or scenario execution behavior change.
- No persistence, REST API, UI, or production integration.
