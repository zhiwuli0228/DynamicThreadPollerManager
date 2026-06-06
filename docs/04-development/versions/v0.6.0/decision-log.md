# v0.6.0 Decision Log

## Decision 1: 先设计压测数据获取，不直接进入 mutation

- Decision: `v0.6.0` 从 pressure data acquisition IR 开始。
- Rationale: `v0.5.0` 已提供 runtime adjustment boundary，但还没有足够运行数据证明下一阶段 mutation 或 queue resizing 设计的必要性和安全性。
- Consequence: 当前不创建 OpenSpec change，不写 Java 实现，不执行实际压测。

## Decision 2: 压测数据必须覆盖 steady/ramp/burst

- Decision: pressure dataset 至少覆盖 `STEADY`、`RAMP`、`BURST` 三类 profile。
- Rationale: `offline-replay-and-readiness-gate` 的 readiness 语义依赖 profile 完整性；单一 profile 不足以支持总体 readiness 判断。
- Consequence: 缺失任一 required profile 时，不得进入 mutation readiness 结论或后续 mutation SR。

## Decision 3: 每个 profile 至少 3 个有效 run

- Decision: IR 草案建议每个 profile 至少 `3` 个有效 run。
- Rationale: 单次 run 容易受偶然因素影响，不能稳定反映 pressure pattern。
- Consequence: SR 可提高该阈值，但不得降低到单 run 即可通过。

## Decision 4: raw evidence 默认不纳入版本控制

- Decision: 默认只版本化 manifest、summary 和 readiness report，不提交大型 raw snapshot evidence。
- Rationale: 原始压测数据可能体积大且噪声高；仓库应保留可审阅摘要和复现实验参数。
- Consequence: 若确需保留 raw evidence，必须在 SR 或 OpenSpec change 中定义清理、压缩和纳入规则。

## Decision 5: 后续 change 必须使用 superspec

- Decision: 若后续需要自动化数据获取或报告能力，必须创建 `schema: superspec` 的 OpenSpec change。
- Rationale: 项目已禁止新 capability change 使用 `spec-driven`，避免再次出现 apply/verify/finalize 流程不可追踪。
- Consequence: 当前只保留候选 change 名称 `pressure-data-acquisition-and-baseline`，不创建 change。
