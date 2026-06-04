# v0.4.0 SR 独立评审记录

## 基本信息

| 项 | 内容 |
| --- | --- |
| Review type | independent SR review |
| Review target | [20-sr.md](./20-sr.md) |
| Reviewer mode | no-context independent read-only review |
| Review date | 2026-06-04 |
| Review conclusion | Ready for disposition; 2 个 P1，2 个 P2 |

## 输入包

- [README.md](./README.md)
- [00-objectives-and-scope.md](./00-objectives-and-scope.md)
- [10-ir.md](./10-ir.md)
- [11-ir-review.md](./11-ir-review.md)
- [12-ir-review-disposition.md](./12-ir-review-disposition.md)
- [13-ir-closure-verification.md](./13-ir-closure-verification.md)
- [20-sr.md](./20-sr.md)
- [current-state.md](/E:/009workspace/claudecode/DynamicThreadPollerManager/docs/00-project/current-state.md)
- `openspec/specs/metrics-snapshot-and-recording/spec.md`
- `openspec/specs/scenario-runner-and-baseline/spec.md`
- `openspec/specs/adaptive-policy-and-control-gate/spec.md`
- 现有 `experiment.metrics`、`experiment.scenario`、`experiment.policy` 包源码

## Findings

### P1

| ID | Finding | 问题 | 影响 | 建议 |
| --- | --- | --- | --- | --- |
| SR-V040-001 | validation result 缺少最小状态语义 | `ReplayEvidenceValidationResult` 只被命名，没有定义最小状态和 failure 粒度。 | 后续实现可能把所有失败压成布尔值，导致 readiness gate 无法解释阻塞原因。 | 定义最小状态：`VALID` / `INVALID`，并要求保留 failure codes 或 reason 列表。 |
| SR-V040-002 | readiness gate 阈值结构未固化 | `READY_WITH_RISK` / `NOT_READY` 的规则结构有了，但没有最小阈值配置入口。 | 后续实现可能硬编码阈值，或不同测试使用不同常量。 | 引入独立 `ReadinessThresholds` 契约，承载 capped/hold/flip/streak 的阈值配置。 |

### P2

| ID | Finding | 问题 | 影响 | 建议 |
| --- | --- | --- | --- | --- |
| SR-V040-003 | summary 缺少 scenario-level 聚合对象 | 当前只有 `ReplayRunSummary`，没有明确多 run / 多 profile 的汇总模型。 | readiness assessment 和报告可能直接拼接 run 级 summary，结构不稳定。 | 增加 `ReplayScenarioSummary` 或等价聚合对象。 |
| SR-V040-004 | 输出 artifact 文件名过于示例化 | `replay-run-summary.json` 等文件名像单一固定文件，未说明多 run 或多 config 时的命名规则。 | 后续实现可能覆盖输出，或在测试中引入不稳定命名。 | 定义稳定命名模式，至少包含 `runId` 或 `configLabel`。 |

## 正向观察

- 设计已清晰复用现有 metrics/scenario/policy 类型。
- 边界隔离约束明确，没有越权到 executor mutation。
- 数据契约、算法步骤和测试映射已足够接近实现输入。

## 结论

`20-sr.md` 已达到 disposition 入口，但在 P1 关闭前不得进入 OpenSpec change decomposition。

