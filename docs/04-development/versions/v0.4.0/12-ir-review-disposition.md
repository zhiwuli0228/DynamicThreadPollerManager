# v0.4.0 IR 评审处置记录

## 基本信息

| 项 | 内容 |
| --- | --- |
| Disposition target | [11-ir-review.md](./11-ir-review.md) |
| Disposition date | 2026-06-04 |
| Disposition owner | Codex |
| Current conclusion | 所有 findings 已接受并完成 IR 文档处置，待独立闭环验证 |

## 处置总览

| ID | 优先级 | Decision | 处置内容 | 残余风险 |
| --- | --- | --- | --- | --- |
| IR-V040-001 | P1 | Accepted | 在 `IR-v0.4-004` 中新增最小抖动风险口径：direction flip count、alternating streak、hold ratio、capped ratio；同步收紧 `AC-v0.4-007`。 | 具体阈值数值留到 SR；本 IR 只定义最低必需指标。 |
| IR-V040-002 | P1 | Accepted | 在 `IR-v0.4-008` 中新增最小门槛：每类 scenario 至少一个 completed run、每 run 至少 3 个有序 snapshots；缺失 scenario 或 skipped count 非零时不得为 `ready`；同步收紧 `AC-v0.4-012`。 | 3 个 snapshot 是最低需求门槛，不代表最终实验充分性。 |
| IR-V040-003 | P2 | Accepted | 在 `IR-v0.4-007` 中新增受控输出目录约定：`outputs/reports/v0.4.0/` 或等价目录；默认脱敏并排除大输出入库。 | SR 仍需定义具体文件名、摘要字段和 `.gitignore` 策略。 |
| IR-V040-004 | P2 | Accepted | 在 `IR-v0.4-005` 中新增最小比较集：默认、保守、激进三组 threshold config。 | 具体参数值留到 SR。 |

## 修改摘要

| 文件 | 修改 |
| --- | --- |
| [10-ir.md](./10-ir.md) | 收紧抖动风险信号、readiness gate 门槛、evidence 输出目录和 threshold sensitivity 最小比较集；同步 AC 文案。 |
| [11-ir-review.md](./11-ir-review.md) | 固化独立 IR review findings。 |
| [12-ir-review-disposition.md](./12-ir-review-disposition.md) | 记录本次处置。 |

## 验证

- 已复核 `10-ir.md` 中相关 IR/AC 条目文本存在且与处置声明一致。
- 未创建 SR、OpenSpec change 或 Java 实现，符合当前授权边界。

## 结论

IR 评审处置完成。下一步必须执行独立闭环验证；在闭环验证完成前，`v0.4.0` 仍不得进入 SR 设计。
