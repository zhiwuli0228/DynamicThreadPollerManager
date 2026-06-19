# v0.5.0 IR Review Disposition

## Header

- Document type: IR review disposition
- Version name: `v0.5.0`
- Disposition date: `2026-06-05`
- Input review: `11-ir-review.md`
- Conclusion: `disposition completed`

## Disposition Table

| Finding ID | Decision | 修订内容 | 验证 | 残余风险 |
| --- | --- | --- | --- | --- |
| IR-V050-001 | Accepted | `10-ir.md` 已补充 `READY`、`READY_WITH_RISK`、`NOT_READY` 对 SR/implementation 的门禁语义。 | `10-ir.md` 的需求判断和评审重点已包含 readiness consumption rule。 | `READY` 仍不证明 runtime mutation 收益，作为后续 SR 残余风险。 |
| IR-V050-002 | Accepted | `10-ir.md` 已补充 adapter 输入应定义为 `ScaleDecision` 或等价 command，并拒绝 `HOLD`、`REJECTED`、无 target 或 capped no-op。 | `IR-v0.5-001`、`AC-v0.5-007` 和追踪矩阵已更新。 | 具体 command 模型留给 SR。 |
| IR-V050-003 | Accepted | `10-ir.md` 已补充当前代码缺少 runtime queue controller，并默认 queue resizing 不进入第一批实现候选。 | `IR-v0.5-003` 和风险表已更新。 | 若 SR 能证明安全 abstraction，可重新评审纳入。 |
| IR-V050-004 | Accepted | `10-ir.md` 已补充 SR 至少定义 cooldown、连续相反方向 adjustment 阻断和单 run adjustment 上限。 | `IR-v0.5-004`、`AC-v0.5-008` 和追踪矩阵已更新。 | 具体阈值和测试映射留给 SR。 |

## 修订文件

| 文件 | 修订摘要 |
| --- | --- |
| `10-ir.md` | 补充 readiness gate 消费语义、adapter command 映射、queue resizing 默认延期判断、runtime safety 最小要求和新增 AC。 |

## 结论

所有 IR review findings 已接受并处置。可以进入独立 IR closure verification。
