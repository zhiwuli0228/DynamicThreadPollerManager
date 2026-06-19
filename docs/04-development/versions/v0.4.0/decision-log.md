# v0.4.0 Decision Log

## Decision 1: 先做 evidence/replay，不直接做 executor mutation

- Decision: `v0.4.0` 需求草案聚焦 baseline pressure evidence、offline policy replay 和 decision evidence。
- Rationale: 当前 policy 仍是非突变决策层，直接接 executor mutation 会放大阈值不合理和抖动风险。
- Consequence: executor adapter、queue resizing 和闭环 adaptive control 继续延期。

## Decision 2: 使用管理变更标准作为阶段门禁

- Decision: `v0.4.0` 必须先完成 IR review、disposition 和 closure verification，才能进入 SR。
- Rationale: 后续实现可能交给能力较弱的 agent，必须用阶段门禁降低需求遗漏和范围漂移。
- Consequence: 当前不创建 OpenSpec change，不写 Java 实现。

## Decision 3: 压测先定义为轻量实验数据需求

- Decision: 当前阶段不引入完整压测平台，只定义 baseline scenario evidence 和 replay summary 需求。
- Rationale: 现有 deterministic scenario runner 已能提供第一批实验输入，足以支撑需求分析和后续 SR。
- Consequence: 真实压测、生产负载和外部观测系统作为后续版本候选。
