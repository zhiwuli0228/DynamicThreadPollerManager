# v0.6.0 SR Review

## Header

- Document type: SR review
- Version name: `v0.6.0`
- Review date: `2026-06-06`
- Review scope: `20-sr.md` and closed IR artifacts
- Review mode: independent functional design review
- Conclusion: `ready for disposition`

## 输入包

- `docs/00-project/current-state.md`
- `docs/02-harness/managed-change-standard.md`
- `docs/04-development/versions/v0.6.0/10-ir.md`
- `docs/04-development/versions/v0.6.0/13-ir-closure-verification.md`
- `docs/04-development/versions/v0.6.0/15-experiment-data-acquisition-plan.md`
- `docs/04-development/versions/v0.6.0/20-sr.md`
- `docs/04-development/versions/v0.6.0/decision-log.md`
- `openspec/specs/scenario-runner-and-baseline/spec.md`
- `openspec/specs/metrics-snapshot-and-recording/spec.md`
- `openspec/specs/adaptive-policy-and-control-gate/spec.md`
- `openspec/specs/offline-replay-and-readiness-gate/spec.md`
- `openspec/specs/executor-adapter-and-adjustment-evidence/spec.md`

## Findings

| ID | Priority | Finding | Impact | Recommended correction |
| --- | --- | --- | --- | --- |
| SR-V060-001 | P1 | `experiment.acquisition` 作为候选模块名存在，但 `20-sr.md` 没有明确它只是候选实现层而不是当前授权模块。 | 弱实现 agent 可能误以为该包已经可实现，进而把 SR 误读为实现授权。 | 进一步强调该模块只是后续 OpenSpec change 候选，当前仍不授权代码。 |
| SR-V060-002 | P1 | `RunManifest` 的环境指纹只列出了字段草案，未明确它们是执行前必须记录还是尽力而为。 | 后续实现可能丢失可比性必要字段，导致 pressure data 不可审计。 | 规定环境指纹字段至少要有核心必填集合，缺失时必须标记 `unavailable`。 |
| SR-V060-003 | P2 | `ReadinessSummary` 的 `recommendedNextStep` 语义没有与 `READY` / `READY_WITH_RISK` / `NOT_READY` 直接绑定。 | 设计阅读者可能不清楚不同 readiness 结果对应的后续动作。 | 明确推荐下一步与三种 readiness 状态的对应关系。 |
| SR-V060-004 | P2 | `current-state.md` 仍未说明 SR 闭环后应进入哪个版本授权状态。 | SR 完成后若不更新 current-state，后续无法合法推进到 OpenSpec change decomposition。 | SR closure 后同步 current-state 到 `READY_FOR_CHANGE_DECOMPOSITION`。 |

## 非 Findings

- `20-sr.md` 清晰禁止 Java 实现、OpenSpec change 创建和实际压测执行。
- `20-sr.md` 将 pressure acquisition、replay、readiness 和 report hygiene 放在同一条数据链上，结构正确。
- `20-sr.md` 明确候选 change 名称仅是后续可能路径，不是当前授权。
- `20-sr.md` 没有把 queue resizing 或 executor mutation 纳入本 SR 的授权范围。

## 结论

`v0.6.0` SR 可以进入 disposition，但 P1 findings 必须修订并闭环后才能进入后续 OpenSpec change decomposition。
