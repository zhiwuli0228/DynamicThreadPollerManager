# v0.4.0 SR 评审处置记录

## 基本信息

| 项 | 内容 |
| --- | --- |
| Disposition target | [21-sr-review.md](./21-sr-review.md) |
| Disposition date | 2026-06-04 |
| Disposition owner | Codex |
| Current conclusion | 所有 findings 已接受并完成 SR 文档处置，待独立闭环验证 |

## 处置总览

| ID | 优先级 | Decision | 处置内容 | 残余风险 |
| --- | --- | --- | --- | --- |
| SR-V040-001 | P1 | Accepted | 为 `ReplayEvidenceValidationResult` 定义最小状态语义：`VALID` / `INVALID`，并要求保留 `failureCodes` 与 `failureReasons`。 | failure code 具体枚举值留到实现阶段按 SR 固化。 |
| SR-V040-002 | P1 | Accepted | 引入 `ReadinessThresholds` 契约，承载 `cappedRatio`、`holdRatio`、`directionFlipCount`、`alternatingStreakMax` 的阈值入口。 | 默认阈值数值留到 change decomposition 或实现前固定。 |
| SR-V040-003 | P2 | Accepted | 增加 `ReplayScenarioSummary` 聚合对象，汇总同一 scenario profile 下多个 run / config 的 run summaries。 | 跨 scenario 最终聚合仍由 readiness assessment 承担。 |
| SR-V040-004 | P2 | Accepted | 将 artifact 设计从固定文件名改为稳定命名模式：包含 `runId`、`scenarioProfile` 或 `configLabel`。 | 最终文件后缀与精确命名在实现阶段可再细化。 |

## 修改摘要

| 文件 | 修改 |
| --- | --- |
| [20-sr.md](./20-sr.md) | 补充 validation result 状态语义、`ReadinessThresholds`、`ReplayScenarioSummary` 和 artifact 命名模式。 |
| [21-sr-review.md](./21-sr-review.md) | 固化独立 SR review findings。 |
| [22-sr-review-disposition.md](./22-sr-review-disposition.md) | 记录本次处置。 |

## 验证

- 已复核 `20-sr.md` 中新增契约和命名规则存在且与处置声明一致。
- 未创建 OpenSpec change 或 Java 实现，符合当前授权边界。

## 结论

SR 评审处置完成。下一步必须执行独立闭环验证；在闭环完成前，`v0.4.0` 仍不允许进入 OpenSpec change decomposition。

