# v0.4.0 IR 独立评审记录

## 基本信息

| 项 | 内容 |
| --- | --- |
| Review type | independent IR review |
| Review target | [10-ir.md](./10-ir.md) |
| Reviewer mode | no-context independent read-only review |
| Review date | 2026-06-04 |
| Review conclusion | Ready for disposition; 2 个 P1，2 个 P2 |

## 输入包

- [README.md](./README.md)
- [00-objectives-and-scope.md](./00-objectives-and-scope.md)
- [10-ir.md](./10-ir.md)
- [decision-log.md](./decision-log.md)
- [current-state.md](/E:/009workspace/claudecode/DynamicThreadPollerManager/docs/00-project/current-state.md)
- [managed-change-standard.md](/E:/009workspace/claudecode/DynamicThreadPollerManager/docs/02-harness/managed-change-standard.md)
- [metrics snapshot spec](/E:/009workspace/claudecode/DynamicThreadPollerManager/openspec/specs/metrics-snapshot-and-recording/spec.md)
- [scenario runner spec](/E:/009workspace/claudecode/DynamicThreadPollerManager/openspec/specs/scenario-runner-and-baseline/spec.md)
- [adaptive policy spec](/E:/009workspace/claudecode/DynamicThreadPollerManager/openspec/specs/adaptive-policy-and-control-gate/spec.md)

## Findings

### P1

| ID | Finding | 问题 | 影响 | 建议 |
| --- | --- | --- | --- | --- |
| IR-V040-001 | 抖动风险信号定义不够可执行 | `IR-v0.4-004` 只写“连续 scale-up/scale-down 或来回抖动”，缺最小统计口径。 | 后续 SR 和实现可能各自定义不同抖动规则，导致 readiness gate 不一致。 | 至少定义 direction flip count、alternating streak、hold ratio、capped ratio。 |
| IR-V040-002 | readiness gate 缺少最低判定门槛 | `IR-v0.4-008` 提到 scenario 类型和 evidence count，但没有最小可用门槛或缺失场景时的强制输出。 | 后续可能在 evidence 很弱时仍宣称 `ready`。 | 定义每类 scenario 的最小 run/snapshot 门槛，并规定缺失场景或 skipped count 非零时不得为 `ready`。 |

### P2

| ID | Finding | 问题 | 影响 | 建议 |
| --- | --- | --- | --- | --- |
| IR-V040-003 | evidence 输出治理不够具体 | 只说可脱敏、可排除大型输出，没有受控输出根路径。 | 后续 evidence 可能散落在任意目录。 | 定义默认受控输出目录，例如 `outputs/reports/v0.4.0/`。 |
| IR-V040-004 | threshold sensitivity 比较集不明确 | 只说多组 config，可比性没有最低要求。 | 后续可能只比两组近似配置，信息量不足。 | 至少要求默认、保守、激进三组对比。 |

## 正向观察

- IR 已明确阻止直接进入 executor mutation。
- 范围内/范围外边界清晰。
- AC 和追踪矩阵已经具备进入 disposition 的基础。
- 已明确当前阶段不允许 OpenSpec change 和 Java 实现。

## 结论

`10-ir.md` 已达到 disposition 入口，但在 P1 关闭前不得作为 SR 设计输入。
