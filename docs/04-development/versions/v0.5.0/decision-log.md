# v0.5.0 Decision Log

## Decision 1: 先定义 executor adapter 需求，不直接实现 mutation

- Decision: `v0.5.0` 先进入 executor adapter 与 queue resizing 的 IR 需求阶段。
- Rationale: `v0.4.0` 提供的是 offline readiness input，不是 runtime mutation 的安全证明。
- Consequence: 当前不创建 OpenSpec change，不写 Java 实现。

## Decision 2: queue resizing 必须单独判定

- Decision: queue resizing 不能默认和 pool size adjustment 合并为一个能力。
- Rationale: queue capacity 的可调整性、并发安全、失败语义和回滚成本都不同于 pool size adjustment。
- Consequence: 若 IR/SR 不能证明 queue resizing 可安全实现，应延期到后续版本。

## Decision 3: adjustment evidence 是实现前置条件

- Decision: 任何 runtime adjustment 实现前，必须先定义 adjustment evidence contract。
- Rationale: 没有 before/requested/applied/failure evidence，就无法审计实际 executor mutation。
- Consequence: 后续 SR 必须给出状态枚举、字段定义和测试映射。

## Decision 4: `v0.4.0` readiness 只授权设计判断

- Decision: `READY` 最多支持进入 SR 设计；`READY_WITH_RISK` 必须记录风险接受条件；`NOT_READY` 阻止 mutation implementation。
- Rationale: offline replay 能证明 decision 分布和风险信号，不能证明 runtime mutation 后收益。
- Consequence: 后续 SR 必须把 readiness status 映射到 runtime safety gate 和测试计划。

## Decision 5: queue resizing 默认延期到安全 abstraction 明确之后

- Decision: `v0.5.0` 默认不把 queue resizing 放入第一批实现候选。
- Rationale: 当前代码没有 runtime queue capacity controller，baseline executor 只提供固定 queue capacity。
- Consequence: 若 SR 要纳入 queue resizing，必须先定义单独 capability、failure semantics、rollback semantics 和测试策略。
