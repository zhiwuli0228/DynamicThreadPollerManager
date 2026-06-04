## 1. Analysis Contracts

- [ ] 1.1 创建 `experiment/analysis` 包及其对应测试包。
- [ ] 1.2 添加 replay validation 状态与结果类型，覆盖 `VALID` / `INVALID`、failure code/reason、accepted/rejected 计数。
- [ ] 1.3 添加 `ReplayRunInput`，固定 run、scenario、baseline policy、snapshots、evidence summary、completedStepCount、totalWorkUnits 字段。
- [ ] 1.4 添加 `ReplayDecisionEvidence`，固定 `offline_replay` 模式和逐 snapshot 决策字段。
- [ ] 1.5 添加 `ReplayRunSummary`、`ReplayScenarioSummary`、`SensitivityComparison`、`ReadinessThresholds`、`ReadinessAssessment` 等聚合契约。

## 2. Evidence Validation and Offline Replay

- [ ] 2.1 实现 `ReplayEvidenceValidator`，校验 metadata、最小 snapshot 数、runId 一致性、时间戳顺序和最小 pressure 字段可读性。
- [ ] 2.2 为 validation failure 增加负例测试，覆盖缺 metadata、evidence 不足、runId mismatch、timestamp disorder。
- [ ] 2.3 实现 `OfflinePolicyReplayService`，对有效输入分别使用 `default`、`conservative`、`aggressive` 三组固定配置进行 replay。
- [ ] 2.4 确保 `decisionTimestamp` 等于源 snapshot timestamp，不允许任何 wall-clock 时间生成。
- [ ] 2.5 增加 replay evidence 字段测试，验证 `policyConfigLabel`、`policyId`、`snapshotIndex`、`replayMode` 和 reason 暴露正确。

## 3. Summary, Sensitivity, and Readiness

- [ ] 3.1 实现 `ReplaySummaryBuilder`，计算 action/gate counts、`holdRatio`、`cappedRatio`、`directionFlipCount`、`alternatingStreakMax`。
- [ ] 3.2 为 summary builder 增加计数守恒和非 `HOLD` 抖动计算测试。
- [ ] 3.3 实现 `ThresholdSensitivityAnalyzer`，比较三组固定配置并输出相对于 `default` 的差值摘要。
- [ ] 3.4 实现 `MutationReadinessGate`，固定 `STEADY` / `RAMP` / `BURST` 完整性规则与 readiness/risk 阈值。
- [ ] 3.5 增加 readiness 测试，覆盖 `READY`、`READY_WITH_RISK`、`NOT_READY`、缺 profile、evidence 不足、skipped evidence 和风险阈值越界场景。

## 4. Report Artifact and Boundary Verification

- [ ] 4.1 实现 `ReplayReportWriter`，只允许输出到 `outputs/reports/v0.4.0/`，并采用约定命名模式。
- [ ] 4.2 增加 artifact 测试，验证文件命名、最小字段输出和不复制原始大 evidence。
- [ ] 4.3 增加 analysis boundary isolation test，扫描 analysis 包中的 forbidden references：`.scenario.` 之外的 mutation 类型、`AdjustmentEvent`、`ThreadPoolExecutor`、`ScheduledExecutorService`、`ExecutorAdapter`、`QueueCapacityController`、外部 IO client。
- [ ] 4.4 确认 `pom.xml` 无新依赖。
- [ ] 4.5 运行 `openspec.cmd validate --all --json`。
- [ ] 4.6 运行 `.\mvnw.cmd test`。
- [ ] 4.7 仅在对应实现、测试、apply、verify 完成后更新本文件复选框。
