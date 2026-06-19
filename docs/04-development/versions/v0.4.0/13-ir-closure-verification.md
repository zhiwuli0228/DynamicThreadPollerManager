# v0.4.0 IR 闭环验证记录

## 基本信息

| 项 | 内容 |
| --- | --- |
| Verification type | IR closure verification |
| Verification mode | independent read-only verification |
| Verification date | 2026-06-04 |
| Inputs | [11-ir-review.md](./11-ir-review.md), [12-ir-review-disposition.md](./12-ir-review-disposition.md), [10-ir.md](./10-ir.md) |
| Final conclusion | Closed with recorded residual risk |

## Finding 闭环表

| Finding | 原优先级 | 闭环结论 | 核验证据 | 残余风险 |
| --- | --- | --- | --- | --- |
| IR-V040-001 | P1 | Closed | `IR-v0.4-004` 已定义 direction flip count、alternating streak、hold ratio、capped ratio；`AC-v0.4-007` 同步收紧。 | 阈值数值待 SR。 |
| IR-V040-002 | P1 | Closed | `IR-v0.4-008` 已定义每类 scenario 至少一个 completed run、每 run 至少 3 个有序 snapshots，且缺失场景或 skipped count 非零时不得为 `ready`。 | 样本充足性细化待 SR。 |
| IR-V040-003 | P2 | Closed | `IR-v0.4-007` 已定义受控输出目录和默认脱敏/排除大输出策略。 | 具体路径命名和 ignore 细节待 SR。 |
| IR-V040-004 | P2 | Closed | `IR-v0.4-005` 已定义默认、保守、激进三组最小比较集。 | 参数值待 SR。 |

## 文档卫生

- 未发现将 IR 处置误写为 OpenSpec 授权或 Java 实现授权。
- `README.md` 和 `00-objectives-and-scope.md` 仍表达为 requirement-stage 文档。
- 当前仓库不存在 active OpenSpec change，符合 `v0.4.0` 当前边界。

## 残余风险

| 风险 | 非阻塞理由 | 后续要求 |
| --- | --- | --- |
| 抖动阈值仍未数值化 | IR 阶段先定义必需指标已足够进入 SR。 | `20-sr.md` 必须定义阈值/规则。 |
| 3 个 snapshot 只是最低门槛 | 当前用于阻止空 evidence 或过弱 evidence 宣称 `ready`。 | SR 必须补充分场景样本充分性解释。 |
| 输出目录只有模式约定 | 需求阶段不需要具体文件名和 ignore 规则。 | SR 必须定义具体 artifact contract。 |

## 结论

`v0.4.0` IR 需求阶段闭环验证通过，结论为 `closed with recorded residual risk`。当前可进入 `v0.4.0` SR 功能设计阶段，但仍不授权 OpenSpec change 或 Java 实现。
