# v0.5.0 IR Review

## Header

- Document type: IR review
- Version name: `v0.5.0`
- Review date: `2026-06-05`
- Review scope: `README.md`, `00-objectives-and-scope.md`, `10-ir.md`, `decision-log.md`, synced `v0.4.0` and policy specs
- Review mode: independent documentation review
- Conclusion: `ready for disposition`

## 输入包

- `docs/00-project/current-state.md`
- `docs/02-harness/managed-change-standard.md`
- `docs/04-development/versions/v0.5.0/README.md`
- `docs/04-development/versions/v0.5.0/00-objectives-and-scope.md`
- `docs/04-development/versions/v0.5.0/10-ir.md`
- `docs/04-development/versions/v0.5.0/decision-log.md`
- `openspec/specs/offline-replay-and-readiness-gate/spec.md`
- `openspec/specs/adaptive-policy-and-control-gate/spec.md`

## Findings

| ID | Priority | Finding | Impact | Recommended correction |
| --- | --- | --- | --- | --- |
| IR-V050-001 | P1 | `v0.4.0` readiness output 没有被足够具体地映射为 `v0.5.0` SR/implementation gate。 | 后续 agent 可能把 `READY` 误读为实现授权，或在 `READY_WITH_RISK` 时跳过风险接受条件。 | 明确 `READY`、`READY_WITH_RISK`、`NOT_READY` 分别允许或阻止哪些后续阶段。 |
| IR-V050-002 | P1 | adapter 输入契约还停留在 accepted/capped decision，缺少 `PolicyDecision` / `ScaleDecision` 到 runtime command 的映射要求。 | 后续实现可能直接消费 replay evidence 或原始 policy decision，导致 `HOLD`、no-op capped 或不可执行 decision 泄漏到 runtime mutation。 | 要求 SR 定义可执行 command 输入，并明确拒绝不可转换 decision。 |
| IR-V050-003 | P1 | queue resizing 只写“单独判定”，但没有结合当前代码状态给出默认判断。 | queue resizing 可能被混入 pool size adjustment，扩大运行时突变风险。 | 基于当前没有 queue controller 的事实，默认延期 queue resizing，除非 SR 先证明安全 abstraction。 |
| IR-V050-004 | P2 | runtime safety gate 缺少最小抖动防护要求。 | SR 可能只写泛化 safety gate，而没有 cooldown、方向翻转或单 run adjustment 上限。 | 要求 SR 至少定义 cooldown、连续相反方向 adjustment 阻断和单 run adjustment 上限。 |

## 非 Findings

- IR 已明确禁止 Java 实现、OpenSpec change、executor mutation 和 queue resizing implementation。
- IR 已把 adjustment evidence 作为 P0 需求，方向正确。
- IR 已把 queue resizing 与 pool size adjustment 拆分，方向正确。

## 结论

`v0.5.0` IR 可以进入 disposition，但 P1 findings 必须修订并闭环后才能进入 SR 功能设计。
