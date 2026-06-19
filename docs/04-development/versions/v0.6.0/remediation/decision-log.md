# v0.6.0 补救措施 Decision Log

## Decision 1: 不修改原始 v0.6.0 设计正文

- Decision: 原始 `v0.6.0` IR / SR / 计划文档保持不动。
- Rationale: 原始设计已经闭环归档，补救问题应通过独立设计承载，而不是回写历史正文。
- Consequence: 补救逻辑单独放入 `remediation/` 子目录。

## Decision 2: 补救方案只解决真实数据缺口

- Decision: 补救包只聚焦“真实实验数据没有落盘”的问题。
- Rationale: 当前缺陷不是能力缺失，而是证据缺口。
- Consequence: 不扩展到 queue resizing、生产 executor 集成或控制器设计。

## Decision 3: 输出目录独立

- Decision: 补救设计建议使用独立输出目录 `outputs/reports/v0.6.0-remediation/`。
- Rationale: 避免与原始 `v0.6.0` 归档产物混淆。
- Consequence: 后续执行时能更清楚地区分原始能力产物与补救采集产物。

## Decision 4: 补救流程必须完整

- Decision: 补救包必须包含 IR、IR review、IR disposition、IR closure verification、SR、SR review、SR disposition、SR closure verification 和 decision log。
- Rationale: 只有完整流程，弱 agent 才不会再次停在“解释性文本”而非“可执行设计”上。
- Consequence: 补救包可以作为独立设计资产交给后续 agent。

