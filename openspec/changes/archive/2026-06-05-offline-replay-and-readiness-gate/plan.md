# Offline Replay and Readiness Gate Implementation Plan

> **For agentic workers:** 使用 `opsx:apply` 或等价实现代理时，严格按本计划顺序执行，每完成一组任务就补充测试，再推进下一组；不要跨任务并行扩展范围。

**Goal:** 在不引入任何 runtime mutation 的前提下，新增 baseline evidence validation、offline policy replay、summary aggregation、threshold sensitivity 和 mutation readiness assessment。

**Architecture:** 实现一个新的 `experiment.analysis` 只读子层。该子层消费既有 `ObservedSnapshot`、`EvidenceSummary`、`ScenarioRunOutcome` 元数据和 `ThresholdPolicyEvaluator`，输出 `ReplayDecisionEvidence`、`ReplayRunSummary`、`SensitivityComparison`、`ReadinessAssessment` 与受控报告 artifact。它不得依赖 executor mutation 或外部系统。

**Tech Stack:** Java 21、现有 Spring Boot 项目、JUnit 5、Maven。No new dependencies.

---

## Task 1: Analysis Contracts

- [ ] **Step 1:** 创建 `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/analysis/` 与对应测试目录。
- [ ] **Step 2:** 先写失败测试，覆盖 `ReplayValidationStatus` 与 `ReplayEvidenceValidationResult` 的字段暴露和最小校验语义。
- [ ] **Step 3:** 实现 validation 状态与结果类型，至少包含 `status`、`failureCodes`、`failureReasons`、`acceptedSnapshotCount`、`rejectedSnapshotCount`。
- [ ] **Step 4:** 先写失败测试，覆盖 `ReplayRunInput` 的有效构造与 `runId` / `scenarioProfile` / `snapshots` 约束。
- [ ] **Step 5:** 实现 `ReplayRunInput`。
- [ ] **Step 6:** 先写失败测试，覆盖 `ReplayDecisionEvidence` 必填字段、`decisionTimestamp == snapshotTimestamp`、`replayMode == offline_replay`。
- [ ] **Step 7:** 实现 `ReplayDecisionEvidence`。
- [ ] **Step 8:** 实现 `ReplayRunSummary`、`ReplayScenarioSummary`、`SensitivityComparison`、`ReadinessThresholds`、`ReadinessAssessment` 等剩余契约。
- [ ] **Step 9:** 运行契约测试并提交。

## Task 2: Evidence Validation

- [ ] **Step 1:** 先写失败测试，覆盖缺 `runId`、缺 `scenarioId`、缺 `scenarioProfile`、空 snapshots、少于 3 条 snapshots。
- [ ] **Step 2:** 先写失败测试，覆盖 snapshot `runId` 不一致与 timestamp 无序。
- [ ] **Step 3:** 实现 `ReplayEvidenceValidator` 和 failure code 映射。
- [ ] **Step 4:** 确保 validation failure 不会进入 replay 主流程。
- [ ] **Step 5:** 运行 validation 测试并提交。

## Task 3: Offline Replay

- [ ] **Step 1:** 实现 `SensitivityConfigSet`，固定三组配置：
  - `default`: 复用 `ThresholdPolicyConfig.defaultAdaptive()`
  - `conservative`: `min=1,max=32,upActive=28,upQueue=20,downActive=2,step=1`
  - `aggressive`: `min=1,max=32,upActive=20,upQueue=12,downActive=6,step=3`
- [ ] **Step 2:** 先写失败测试，覆盖单个 snapshot replay 生成 `ReplayDecisionEvidence` 的字段和值。
- [ ] **Step 3:** 先写失败测试，覆盖三组配置 replay 都会调用既有 `ThresholdPolicyEvaluator`。
- [ ] **Step 4:** 实现 `OfflinePolicyReplayService`。
- [ ] **Step 5:** 确保 replay 使用 snapshot timestamp 构造 `PolicyEvaluationInput`，不得调用 `Instant.now()`。
- [ ] **Step 6:** 运行 replay 测试并提交。

## Task 4: Summary and Sensitivity

- [ ] **Step 1:** 先写失败测试，覆盖 `decisionCount + skippedCount == evidenceCount`。
- [ ] **Step 2:** 先写失败测试，覆盖 `directionFlipCount` 只计算相邻非 `HOLD` 方向切换。
- [ ] **Step 3:** 先写失败测试，覆盖 `alternatingStreakMax` 对 `UP,DOWN,UP,DOWN` 产生 `4`。
- [ ] **Step 4:** 实现 `ReplaySummaryBuilder`。
- [ ] **Step 5:** 先写失败测试，覆盖 sensitivity comparison 同时输出 `default`、`conservative`、`aggressive` 三组结果以及相对 `default` 的差值。
- [ ] **Step 6:** 实现 `ThresholdSensitivityAnalyzer`。
- [ ] **Step 7:** 运行 summary/sensitivity 测试并提交。

## Task 5: Readiness Gate

- [ ] **Step 1:** 固化默认 `ReadinessThresholds`：
  - `maxCappedRatioForReady=0.25`
  - `maxHoldRatioForReady=0.85`
  - `maxDirectionFlipCountForReady=2`
  - `maxAlternatingStreakForReady=2`
  - `maxCappedRatioForRisk=0.50`
  - `maxHoldRatioForRisk=0.95`
  - `maxDirectionFlipCountForRisk=4`
  - `maxAlternatingStreakForRisk=4`
- [ ] **Step 2:** 先写失败测试，覆盖缺少 `STEADY` / `RAMP` / `BURST` 任一 profile 返回 `NOT_READY`。
- [ ] **Step 3:** 先写失败测试，覆盖任一 run `evidenceCount < 3` 或 `skippedCount > 0` 返回 `NOT_READY`。
- [ ] **Step 4:** 先写失败测试，覆盖指标均在 ready 阈值内返回 `READY`。
- [ ] **Step 5:** 先写失败测试，覆盖指标超过 ready 阈值但未超过 risk 阈值返回 `READY_WITH_RISK`。
- [ ] **Step 6:** 先写失败测试，覆盖任一指标超过 risk 阈值返回 `NOT_READY`。
- [ ] **Step 7:** 实现 `MutationReadinessGate`，并固定只使用 `default` 配置 summary 做 readiness 判定。
- [ ] **Step 8:** 运行 readiness 测试并提交。

## Task 6: Report Artifact and Boundary Verification

- [ ] **Step 1:** 先写失败测试，覆盖输出目录强制为 `outputs/reports/v0.4.0/`。
- [ ] **Step 2:** 先写失败测试，覆盖 `replay-run-summary-<runId>-<configLabel>.json` 等命名模式。
- [ ] **Step 3:** 实现 `ReplayReportWriter` 与对应 artifact 模型。
- [ ] **Step 4:** 增加 analysis boundary isolation test，扫描 analysis 包禁止引用：
  - `AdjustmentEvent`
  - `ThreadPoolExecutor`
  - `ScheduledExecutorService`
  - `ExecutorAdapter`
  - `QueueCapacityController`
  - `MutationValidator`
  - `Instant.now(`
  - 外部 REST / database / client 相关引用
- [ ] **Step 5:** 确认 `pom.xml` 无新依赖。
- [ ] **Step 6:** 运行 `openspec.cmd validate --all --json`。
- [ ] **Step 7:** 运行 `.\mvnw.cmd test`。
- [ ] **Step 8:** 仅在实现完成后更新 `tasks.md` 复选框，并生成 `apply.md`。

## Implementation Notes

- 使用 `PressureSnapshot.poolSize()` 作为 `currentPoolSize`。
- 使用 snapshot 原始 timestamp 作为 replay 的 `evaluatedAt` 和 `decisionTimestamp`。
- `completedTaskCount()` 不参与 readiness 规则，只用于保留 evidence 背景。
- `ReadinessAssessment.selectedConfigLabel` 在本版本固定为 `default`。
- 如果某个 summary 需要跳过某条 evidence，必须把 skipped 原因显式记录；静默丢弃 evidence 视为缺陷。
- 不允许修改既有 `ThresholdPolicyEvaluator`、`ScenarioExperimentRunner` 或 metrics recorder 语义，除非测试证明存在设计文档与实现冲突，并先记录在 `apply.md`/`verify.md`。
