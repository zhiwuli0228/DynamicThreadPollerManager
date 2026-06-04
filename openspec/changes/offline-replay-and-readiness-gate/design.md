## Context

已交付能力：

- `experiment-foundation` 提供 run lifecycle 与基础模型。
- `metrics-snapshot-and-recording` 提供 `ObservedSnapshot`、`EvidenceRecorder`、`EvidenceSummary`。
- `scenario-runner-and-baseline` 提供 deterministic baseline scenario execution 与 `ScenarioRunOutcome`。
- `adaptive-policy-and-control-gate` 提供 `ThresholdPolicyConfig`、`PolicyEvaluationInput`、`PolicyDecision`、`ThresholdPolicyEvaluator` 和 `GateStatus`。

当前缺口不是“如何调 executor”，而是“如何证明现有策略在 baseline evidence 上表现稳定，并足以进入下一阶段 mutation 设计”。本 change 解决这个缺口，但不越权进入 runtime adaptive control。

## Goals / Non-Goals

**Goals:**

- 新增一个严格只读的 `experiment.analysis` 子层。
- 校验 baseline run evidence 最小可用性。
- 用固定三组阈值配置对 snapshots 做 offline replay。
- 生成逐 snapshot 的 `ReplayDecisionEvidence`。
- 聚合 run summary、scenario summary、sensitivity comparison。
- 根据固定 readiness 阈值输出 `READY`、`READY_WITH_RISK` 或 `NOT_READY`。
- 生成受控的本地 JSON/Markdown 摘要 artifact。
- 添加边界隔离测试，禁止分析层引用 mutation 或外部系统类型。

**Non-Goals:**

- 不做 executor adapter。
- 不做 queue resizing。
- 不做 `AdjustmentEvent` 生成。
- 不做 scheduler、REST API、数据库、外部压测平台、自动调参或 learned policy。
- 不修改 `scenario runner`、`metrics recorder` 或 `policy evaluator` 既有语义。
- 不提交大型运行时输出到版本库。

## Decisions

### 1. 包边界

新增包：

```text
com.zhiwu.dynamicthreadpollermanager.experiment.analysis
```

允许依赖：

- `experiment.metrics`
- `experiment.scenario`
- `experiment.policy`
- `experiment.model`

禁止依赖：

- `ThreadPoolExecutor`
- `ScheduledExecutorService`
- `AdjustmentEvent`
- 未来 `ExecutorAdapter` / `QueueCapacityController`
- 外部 IO client、database、REST controller

### 2. 固定 replay 配置集

三组配置必须固定且可复现：

| label | policyId | min | max | scaleUpActive | scaleUpQueue | scaleDownActive | scaleStep |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `default` | `default-adaptive` | 1 | 32 | 24 | 16 | 4 | 2 |
| `conservative` | `conservative-adaptive` | 1 | 32 | 28 | 20 | 2 | 1 |
| `aggressive` | `aggressive-adaptive` | 1 | 32 | 20 | 12 | 6 | 3 |

`default` 必须复用 `ThresholdPolicyConfig.defaultAdaptive()` 的当前实现值。其余两组配置由本 change 固定，不允许在实现阶段自由发挥。

### 3. 核心组件

建议文件：

- `ReplayEvidenceValidator.java`
- `ReplayEvidenceValidationResult.java`
- `ReplayValidationStatus.java`
- `ReplayRunInput.java`
- `OfflinePolicyReplayService.java`
- `ReplayDecisionEvidence.java`
- `ReplayRunSummary.java`
- `ReplayScenarioSummary.java`
- `ReplaySummaryBuilder.java`
- `SensitivityConfigSet.java`
- `SensitivityComparison.java`
- `ThresholdSensitivityAnalyzer.java`
- `ReadinessStatus.java`
- `ReadinessThresholds.java`
- `ReadinessAssessment.java`
- `MutationReadinessGate.java`
- `ReplayReportArtifact.java`
- `ReplayReportWriter.java`

### 4. ReplayEvidenceValidationResult

最小字段：

- `ReplayValidationStatus status`，仅允许 `VALID` / `INVALID`
- `List<String> failureCodes`
- `List<String> failureReasons`
- `int acceptedSnapshotCount`
- `int rejectedSnapshotCount`

失败代码至少覆盖：

- `MISSING_RUN_ID`
- `MISSING_SCENARIO_ID`
- `MISSING_SCENARIO_PROFILE`
- `EMPTY_SNAPSHOTS`
- `INSUFFICIENT_SNAPSHOTS`
- `RUN_ID_MISMATCH`
- `UNORDERED_TIMESTAMP`
- `MISSING_PRESSURE_FIELDS`

### 5. ReplayRunInput

字段固定为：

- `String runId`
- `String scenarioId`
- `ScenarioProfile scenarioProfile`
- `String baselinePolicyId`
- `List<ObservedSnapshot> snapshots`
- `EvidenceSummary evidenceSummary`
- `int completedStepCount`
- `int totalWorkUnits`

规则：

- `runId` 必须与所有 `ObservedSnapshot.runId()` 一致。
- `scenarioProfile` 不得为空。
- `snapshots.size() < 3` 不得进入 readiness 的 `READY` / `READY_WITH_RISK` 分支。

### 6. ReplayDecisionEvidence

字段固定为：

- `runId`
- `scenarioId`
- `scenarioProfile`
- `policyConfigLabel`
- `policyId`
- `snapshotIndex`
- `snapshotTimestamp`
- `decisionTimestamp`
- `action`
- `gateStatus`
- `currentPoolSize`
- `proposedPoolSize`
- `reason`
- `replayMode`

规则：

- `decisionTimestamp` 必须等于 `snapshotTimestamp`。
- `replayMode` 固定为 `offline_replay`。
- 不允许出现 `AdjustmentEvent`、mutation id 或 executor target id。

### 7. ReplayRunSummary 与 scenario 聚合

`ReplayRunSummary` 字段固定为：

- `runId`
- `scenarioId`
- `scenarioProfile`
- `policyConfigLabel`
- `evidenceCount`
- `decisionCount`
- `skippedCount`
- `scaleUpCount`
- `scaleDownCount`
- `holdCount`
- `acceptedCount`
- `cappedCount`
- `gateHoldCount`
- `rejectedCount`
- `directionFlipCount`
- `alternatingStreakMax`
- `holdRatio`
- `cappedRatio`

不变式：

- `decisionCount + skippedCount == evidenceCount`
- `decisionCount == 0` 时不得判定为 `READY`
- `directionFlipCount` 与 `alternatingStreakMax` 只基于非 `HOLD` action

`ReplayScenarioSummary` 字段固定为：

- `scenarioProfile`
- `policyConfigLabel`
- `runIds`
- `runSummaries`
- `totalEvidenceCount`
- `totalDecisionCount`
- `totalSkippedCount`
- `aggregateDirectionFlipCount`
- `aggregateAlternatingStreakMax`
- `aggregateHoldRatio`
- `aggregateCappedRatio`

### 8. 抖动计算

`directionFlipCount` 规则：

- 只遍历非 `HOLD` action 序列
- 相邻非 `HOLD` 从 `SCALE_UP -> SCALE_DOWN` 或 `SCALE_DOWN -> SCALE_UP` 计 1 次

`alternatingStreakMax` 规则：

- 只在非 `HOLD` action 序列上计算
- 如 `UP, DOWN, UP, DOWN` 的最长交替长度为 `4`

### 9. SensitivityComparison

比较对象固定为 `default`、`conservative`、`aggressive` 三组 summary。

最小输出应包含：

- 三组配置的 action counts
- 三组配置的 gate counts
- 三组配置的 `holdRatio`
- 三组配置的 `cappedRatio`
- 三组配置的 `directionFlipCount`
- 三组配置的 `alternatingStreakMax`
- 与 `default` 的差值摘要

### 10. ReadinessThresholds

固定阈值如下：

| 字段 | 值 |
| --- | ---: |
| `maxCappedRatioForReady` | `0.25` |
| `maxHoldRatioForReady` | `0.85` |
| `maxDirectionFlipCountForReady` | `2` |
| `maxAlternatingStreakForReady` | `2` |
| `maxCappedRatioForRisk` | `0.50` |
| `maxHoldRatioForRisk` | `0.95` |
| `maxDirectionFlipCountForRisk` | `4` |
| `maxAlternatingStreakForRisk` | `4` |

### 11. MutationReadinessGate

`ReadinessAssessment` 字段固定为：

- `status`
- `evaluatedScenarioProfiles`
- `missingScenarioProfiles`
- `blockingReasons`
- `riskReasons`
- `selectedConfigLabel`
- `inputRunIds`

判定规则固定为：

1. 缺失 `STEADY`、`RAMP`、`BURST` 中任一 profile，结果为 `NOT_READY`
2. 任一 run `evidenceCount < 3`，结果为 `NOT_READY`
3. 任一 summary `skippedCount > 0`，结果为 `NOT_READY`
4. 若 `cappedRatio`、`holdRatio`、`directionFlipCount`、`alternatingStreakMax` 全部不超过 `...ForReady` 阈值，结果为 `READY`
5. 若至少一项超过 `...ForReady` 但全部不超过 `...ForRisk` 阈值，结果为 `READY_WITH_RISK`
6. 若任一项超过 `...ForRisk` 阈值，结果为 `NOT_READY`

当前版本固定使用 `default` 配置进行 readiness 判定；`conservative` 与 `aggressive` 只作为 sensitivity 参考。

### 12. 报告 artifact

输出目录固定为：

```text
outputs/reports/v0.4.0/
```

建议文件名：

- `replay-run-summary-<runId>-<configLabel>.json`
- `replay-scenario-summary-<scenarioProfile>-<configLabel>.json`
- `replay-sensitivity-report-<runId>.json`
- `readiness-assessment-v0.4.0.json`
- `replay-report-v0.4.0.md`

规则：

- 不默认复制原始大 evidence
- 只输出最小必要字段
- 文档和验证记录只追踪路径、摘要和结果，不追踪大文件内容

## Risks / Trade-offs

- [Risk] readiness 阈值仍然是第一版经验规则。  
  [Mitigation] 本 change 把阈值显式固化，并要求 sensitivity 报告为后续版本提供修正依据。

- [Risk] 下游实现把 replay 结果误当作 runtime adjustment。  
  [Mitigation] `replayMode=offline_replay` 必须显式存在，并加入边界隔离测试。

- [Risk] 报告逻辑与判定逻辑耦合，导致测试困难。  
  [Mitigation] summary、sensitivity、readiness、writer 分离为独立组件。

- [Risk] 输入 evidence 不完整却继续计算。  
  [Mitigation] validation failure 必须阻断 readiness，并暴露 failure codes。

## Migration Plan

这是一个 additive change。

推荐实现顺序：

1. 新建 analysis 包与基础枚举/记录类型
2. 完成 evidence validation 与相关负例测试
3. 完成 offline replay 与逐条 evidence 生成
4. 完成 summary builder 与抖动指标
5. 完成 sensitivity analyzer
6. 完成 readiness gate 与固定阈值
7. 完成 report writer
8. 完成边界隔离测试、CLI 校验与文档闭环

回滚策略：

- 如果 report writer 阻塞实现，可先保留 JSON/Markdown artifact 模型，延后实际写盘逻辑，但必须在 apply/verify 中明确标识未实现。
- 如果 sensitivity 比较实现过重，不允许删除三组固定配置；只能缩减输出维度，不能缩减比较对象。

## Open Questions

- `ReplayReportWriter` 是否需要覆盖写保护。当前建议：默认覆盖同名输出，交由调用方控制执行目录。
- `failureCodes` 是否使用 enum。当前建议：实现阶段可用 enum 或常量字符串，但输出语义必须与本文一致。
