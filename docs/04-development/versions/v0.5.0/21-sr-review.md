# v0.5.0 SR Review

## Header

- Document type: SR review
- Version name: `v0.5.0`
- Review date: `2026-06-05`
- Review scope: `20-sr.md` and closed IR artifacts
- Review mode: independent functional design review
- Conclusion: `ready for disposition`

## 输入包

- `docs/00-project/current-state.md`
- `docs/02-harness/managed-change-standard.md`
- `docs/04-development/versions/v0.5.0/10-ir.md`
- `docs/04-development/versions/v0.5.0/13-ir-closure-verification.md`
- `docs/04-development/versions/v0.5.0/20-sr.md`
- `openspec/specs/offline-replay-and-readiness-gate/spec.md`
- `openspec/specs/adaptive-policy-and-control-gate/spec.md`

## Findings

| ID | Priority | Finding | Impact | Recommended correction |
| --- | --- | --- | --- | --- |
| SR-V050-001 | P1 | `ScaleAdjustmentCommand.commandId` 没有 deterministic 派生规则。 | 后续 evidence 和测试可能无法稳定追踪 command。 | 指定默认 deterministic command id 格式，或要求由 run/timestamp/current/target 稳定派生。 |
| SR-V050-002 | P1 | 第一批 bounded change 是否允许真实 `ThreadPoolExecutor` 集成不够清楚。 | 弱实现 agent 可能直接接生产线程池，扩大风险。 | 明确第一批只做 adapter contract、in-memory adjustable executor probe 和 evidence，不做真实生产 executor integration。 |
| SR-V050-003 | P1 | runtime safety gate 的默认值留给 OpenSpec，但缺少最小建议值。 | 后续 OpenSpec 可能写出不可验证或过于宽松的 safety gate。 | 给出 cooldown、单 run adjustment 上限、反向调整阻断和 `READY_WITH_RISK` 默认处理建议。 |
| SR-V050-004 | P2 | `current-state.md` 仍只授权 SR 设计，未说明 SR 闭环后的 decomposition 状态。 | SR 完成后若不更新状态，后续无法合法创建 OpenSpec change。 | SR closure 后同步 current-state 到 `READY_FOR_CHANGE_DECOMPOSITION`。 |

## 非 Findings

- SR 明确排除了 queue resizing 第一批实现。
- SR 明确 offline replay evidence 不能直接驱动 runtime mutation。
- SR 明确了 adjustment evidence 与 replay evidence 的区分。

## 结论

`v0.5.0` SR 可以进入 disposition。P1 findings 必须修订并闭环后，才允许进入 OpenSpec change decomposition。
