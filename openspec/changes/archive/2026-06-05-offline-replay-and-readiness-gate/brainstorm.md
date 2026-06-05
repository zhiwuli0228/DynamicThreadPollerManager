## Design Summary

`v0.4.0` 的目标不是把 adaptive control 接到真实 executor，而是在既有 baseline scenario evidence 与已实现 policy evaluator 之间增加一个严格只读的离线分析层。这个 change 将把单次 baseline run 的 snapshots 校验、离线 replay、decision evidence、summary 聚合、threshold sensitivity 和 readiness gate 串成一个完整能力包，并把输出限制在受控本地报告目录下。

该设计要求下游实现 agent 遵守三条硬边界：

1. 只允许依赖 `experiment.metrics`、`experiment.scenario`、`experiment.policy`、`experiment.model`。
2. 不得调用 executor mutation、queue resizing、scheduler、wall clock 重新取时、外部服务或数据库。
3. 只产出离线分析结果，不得伪装成 runtime adjustment 或自适应控制闭环。

## Alternatives Considered

### Alternative A: 直接进入 executor mutation 闭环
- **Approach**: 在现有 policy evaluator 基础上直接增加 executor adapter、queue controller 和 runtime 调整逻辑。
- **Pros**:
  - 交付表面上更“完整”
  - 能更快看到线程池实际变化
- **Cons**:
  - 当前缺 baseline replay 数据，无法证明阈值配置合理
  - 一旦策略抖动，会把文档阶段的不确定性直接变成运行时风险
  - 越过了 `v0.4.0` 已批准范围
- **Why not chosen**: 这会把“策略是否合理”和“突变是否安全”两个问题耦合，风险过高，不符合当前授权边界。

### Alternative B: 纯报告导出，不建立结构化 replay 契约
- **Approach**: 只写一个脚本把 snapshots 导出为 Markdown/JSON，不定义独立数据模型、summary 或 readiness 规则。
- **Pros**:
  - 上手快
  - 代码量最少
- **Cons**:
  - 无法形成稳定的测试边界
  - 下游 agent 容易把报告逻辑和判定逻辑混写
  - 后续无法可靠衔接 executor mutation gate
- **Why not chosen**: 只导出报告不足以支撑下一阶段决策，需要先定义清晰契约。

### Alternative C: 独立离线 replay 与 readiness gate 子层
- **Approach**: 在 `experiment.analysis` 下建立 evidence validation、offline replay、summary、sensitivity、readiness、report writer 六个组件，全部只读。
- **Pros**:
  - 与已实现 `v0.2.0`、`v0.3.0` 能力边界清晰衔接
  - 可以先验证策略行为，再决定是否授权 mutation
  - 容易用单元测试和隔离测试覆盖
- **Cons**:
  - 当前版本不会产生真实 runtime 调整效果
  - 需要额外维护离线报告与 readiness 判定契约
- **Why not chosen**: 已选方案。

## Agreed Approach

采用 **Alternative C**。本 change 只实现离线 replay 与 readiness gate：

- 输入是已完成 baseline run 的 `ScenarioRunOutcome` 元数据、`EvidenceSummary` 和 `ObservedSnapshot` 列表。
- `ReplayEvidenceValidator` 先检查 run 元信息完整性、snapshot 最小数量、runId 一致性与时间戳顺序。
- `OfflinePolicyReplayService` 对每个 snapshot 使用三组固定 `ThresholdPolicyConfig` 进行 replay：
  - `default`: 复用 `ThresholdPolicyConfig.defaultAdaptive()`，即 `policyId=default-adaptive`、`min=1`、`max=32`、`scaleUpActive=24`、`scaleUpQueue=16`、`scaleDownActive=4`、`scaleStep=2`
  - `conservative`: `policyId=conservative-adaptive`、`min=1`、`max=32`、`scaleUpActive=28`、`scaleUpQueue=20`、`scaleDownActive=2`、`scaleStep=1`
  - `aggressive`: `policyId=aggressive-adaptive`、`min=1`、`max=32`、`scaleUpActive=20`、`scaleUpQueue=12`、`scaleDownActive=6`、`scaleStep=3`
- `ReplaySummaryBuilder` 汇总单 run 的 action/gate 分布、skipped 数、flip 数和 alternating streak。
- `ThresholdSensitivityAnalyzer` 比较三组配置的 summary 差异。
- `MutationReadinessGate` 只基于 `default` 配置的三类 profile 聚合结果做 readiness 判定，并记录 sensitivity 结果作为风险上下文。
- `ReplayReportWriter` 只允许把摘要写到 `outputs/reports/v0.4.0/`。

## Key Decisions

- readiness 不是 runtime 安全承诺，只是“是否值得进入下一阶段 mutation 设计”的治理信号。
- readiness 判定的必需 profile 固定为 `STEADY`、`RAMP`、`BURST`。
- 任何 run `evidenceCount < 3`、`skippedCount > 0`、profile 缺失，都必须得到 `NOT_READY`。
- `decisionTimestamp` 必须等于源 snapshot timestamp；任何 `Instant.now()` 都视为越界。
- `ReplayDecisionEvidence.replayMode` 固定为 `offline_replay`，防止下游将其误解为已执行的调整事件。
- 本 change 需要额外携带 `delivery-checklist.md`，后续 archive 前必须逐项勾选。

## Open Questions

- `readiness` 阈值是否需要在 `default` 之外再支持按 config label 切换判断。当前决定：`v0.4.0` 固定只用 `default` 判断，其他 config 仅用于 sensitivity。
- 输出 artifact 是否需要纳入版本控制。当前决定：仅追踪文档和路径约束，不提交大体量运行输出。
