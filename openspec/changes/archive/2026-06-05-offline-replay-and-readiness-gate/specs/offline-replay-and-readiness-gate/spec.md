## ADDED Requirements

### Requirement: Replay evidence validation
系统 MUST 在离线 replay 前校验 baseline run evidence 的最小完整性，包括 run metadata、snapshot 数量、runId 一致性和时间戳顺序。

#### Scenario: 接受可用 replay 输入
- **WHEN** 输入包含非空 `runId`、`scenarioId`、`scenarioProfile`，且 snapshots 数量不少于 3、所有 snapshot 的 `runId` 一致、时间戳按非降序排列
- **THEN** 系统 MUST 将该输入标记为 `VALID`，并暴露 accepted/rejected snapshot 计数

#### Scenario: 阻断无效 replay 输入
- **WHEN** 输入缺少关键 metadata、snapshots 为空或少于 3、runId 不一致、或时间戳无序
- **THEN** 系统 MUST 将该输入标记为 `INVALID`，并暴露可用于 readiness blocking 的 failure code 和 failure reason

---

### Requirement: Offline policy replay evidence
系统 MUST 使用固定三组阈值配置对 baseline snapshots 做只读 offline replay，并为每个 snapshot 生成结构化 decision evidence。

#### Scenario: 为单个 snapshot 生成 replay decision evidence
- **WHEN** 一个有效 snapshot 被 replay 服务处理
- **THEN** 系统 MUST 调用既有 policy evaluator，并生成包含 `policyConfigLabel`、`policyId`、`snapshotIndex`、`snapshotTimestamp`、`decisionTimestamp`、`action`、`gateStatus`、`currentPoolSize`、`proposedPoolSize`、`reason` 与 `replayMode=offline_replay` 的 evidence

#### Scenario: 保持 replay 时间确定性
- **WHEN** replay 服务为 snapshot 生成 decision evidence
- **THEN** `decisionTimestamp` MUST 等于源 snapshot timestamp，而不是在 replay 过程中重新生成 wall-clock 时间

#### Scenario: 禁止 runtime mutation 语义泄漏
- **WHEN** analysis 包源码被检查
- **THEN** 它 MUST NOT 引用 executor mutation、queue resizing、`AdjustmentEvent`、`ThreadPoolExecutor` 或 `ScheduledExecutorService`

---

### Requirement: Replay summary and oscillation signals
系统 MUST 从 replay decision evidence 聚合单 run summary，并暴露 action/gate 分布、skipped 计数和最小抖动信号。

#### Scenario: 生成单 run summary
- **WHEN** 一个 run 的 replay decision evidence 被聚合
- **THEN** 系统 MUST 输出 `evidenceCount`、`decisionCount`、`skippedCount`、`scaleUpCount`、`scaleDownCount`、`holdCount`、`acceptedCount`、`cappedCount`、`gateHoldCount`、`rejectedCount`、`holdRatio` 与 `cappedRatio`

#### Scenario: 计算方向翻转和交替长度
- **WHEN** 一个 run 的非 `HOLD` action 序列被分析
- **THEN** 系统 MUST 输出 `directionFlipCount` 和 `alternatingStreakMax`，且二者只基于非 `HOLD` action 计算

#### Scenario: 保持 summary 计数守恒
- **WHEN** 一个 run summary 被构造
- **THEN** `decisionCount + skippedCount` MUST 等于 `evidenceCount`

---

### Requirement: Threshold sensitivity comparison
系统 MUST 在同一份 baseline evidence 上比较 `default`、`conservative` 与 `aggressive` 三组配置的 replay 结果。

#### Scenario: 比较三组固定配置
- **WHEN** sensitivity analyzer 对一个 run 或 scenario 聚合结果执行比较
- **THEN** 系统 MUST 同时输出三组配置的 action counts、gate counts、`holdRatio`、`cappedRatio`、`directionFlipCount` 和 `alternatingStreakMax`

#### Scenario: 以 default 作为比较基线
- **WHEN** sensitivity comparison 被生成
- **THEN** 系统 MUST 提供相对于 `default` 的差值摘要，供后续 readiness 审阅使用

---

### Requirement: Mutation readiness assessment
系统 MUST 基于 `default` 配置的 replay 结果，按固定 profile 完整性和风险阈值输出 mutation readiness 结论。

#### Scenario: 判定 READY
- **WHEN** `STEADY`、`RAMP`、`BURST` 三类 profile 齐全，所有 run `evidenceCount >= 3`、所有 summary `skippedCount == 0`，且 `cappedRatio`、`holdRatio`、`directionFlipCount`、`alternatingStreakMax` 均不超过 `READY` 阈值
- **THEN** 系统 MUST 输出 `ReadinessStatus.READY`

#### Scenario: 判定 READY_WITH_RISK
- **WHEN** profile 完整且无 blocking condition，但至少一项风险指标超过 `READY` 阈值且全部未超过 `RISK` 阈值
- **THEN** 系统 MUST 输出 `ReadinessStatus.READY_WITH_RISK`，并记录风险原因

#### Scenario: 判定 NOT_READY
- **WHEN** 缺失任一必需 profile、任一 run evidence 不足、任一 summary 存在 skipped evidence、或任一风险指标超过 `RISK` 阈值
- **THEN** 系统 MUST 输出 `ReadinessStatus.NOT_READY`，并记录 blocking reason

---

### Requirement: Controlled replay report artifacts
系统 MUST 以受控方式输出本地 replay 摘要 artifact，并避免把原始大 evidence 误纳入版本控制。

#### Scenario: 写出受控摘要文件
- **WHEN** report writer 生成 replay artifact
- **THEN** 系统 MUST 将 JSON/Markdown 摘要写入 `outputs/reports/v0.4.0/`，并使用约定命名模式标识 run、scenario profile 和 config label

#### Scenario: 保持 evidence hygiene
- **WHEN** 报告 artifact 被生成
- **THEN** 系统 MUST 只包含最小必要字段和审阅摘要，且 MUST NOT 默认复制原始大 snapshot evidence 到报告目录
