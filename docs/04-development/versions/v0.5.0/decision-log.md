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
