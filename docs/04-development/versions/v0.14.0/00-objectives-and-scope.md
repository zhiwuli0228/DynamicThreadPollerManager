# v0.14.0 目标与范围

## Header

- Version name: `v0.14.0`
- Status: `VERSION_DESIGN_DRAFT`
- Current phase: `VERSION_BASELINE`
- Requirement theme: autonomous closed-loop adjustment, oscillation detection, state transition model, adjustment decision orchestration, feedback-driven weight calibration

## 1. 背景

### 1.1 已完成的能力基线

| 版本 | 能力 | 状态 |
|---|---|---|
| v0.1.0 | 实验基础模型（ExperimentRun, LoadScenario, ControlPolicy） | IMPLEMENTED |
| v0.2.0 | 指标采集与记录（ObservedSnapshot, EvidenceRecorder, EvidenceSummary） | IMPLEMENTED |
| v0.3.0 | 场景运行器（ScenarioPlanner, BaselineWorkloadExecutor, ScenarioExperimentRunner） | IMPLEMENTED |
| v0.4.0 | 自适应策略与控制门（ControlGate, ThresholdPolicyEvaluator, PolicyDecision） | IMPLEMENTED |
| v0.5.0 | 离线回放与就绪判定（OfflinePolicyReplay, MutationReadinessGate, AdjustmentResult） | IMPLEMENTED |
| v0.6.0 | 压测数据获取基线（RunManifest, DataQualityValidator, ReadinessClassifier, ReportWriter） | IMPLEMENTED |
| v0.7.0 | ManagedExecutor 域与闭环实验（ManagedExecutor, ExecutorRegistry, AdjustmentAdapter, ScaleAdjustmentCommand） | IMPLEMENTED |
| v0.8.0 | 真实数据获取与 metrics 管道集成（ManagedExecutorScenarioRunner, G7-G9 门禁） | IMPLEMENTED |
| v0.9.0 | 队列容量动态调整（QueueResizeCommand, ExecutorRebuildStrategy, QueueResizeAdjustmentAdapter） | IMPLEMENTED |
| v0.10.0 | 拒绝策略动态替换（RejectionPolicyCommand, RejectionPolicyAdjustmentAdapter, PolicyReplacementEvidence） | IMPLEMENTED |
| v0.11.0 | 持久化证据录制与自主采样（FileBackedEvidenceRecorder, RecordingSession, LivePressureSampler） | IMPLEMENTED |
| v0.12.0 | 基线比较实验框架（BaselineExecutorCatalog, ComparableScenarioRunner, NormalizedComparisonMetrics, ComparisonReportArtifact） | IMPLEMENTED |
| v0.13.0 | 压力分类与策略评分（PressureState, SnapshotPressureClassifier, NormalizedPressureMetrics, PolicyScore, ThresholdPolicyScorer, PolicyRanker, SystemCpuProbe） | IMPLEMENTED |

### 1.2 当前缺口

系统已具备完整的"观察→诊断"能力链，但缺少"诊断→行动"的闭环执行器。具体缺口：

1. **无自主循环控制器** — `LivePressureSampler` 可以周期性采样，但采样结果需要外部消费。`ThresholdPolicyEvaluator` 可以做出决策，但决策需要外部触发。目前没有任何组件将采样→分类→评分→决策→执行串联为一个自主运行的闭环。缺失的是 `AdjustmentLoop` — 一个管理此循环生命周期的顶层控制器。

2. **无振荡防护** — 当系统连续做出方向相反的调整决策时（如 scale-up 后立即 scale-down），会产生配置振荡，导致 executor 反复重建（queue resize）或线程反复创建/销毁（pool resize）。现有的 `SafetyGate` 仅校验单次调整的边界合法性（min/max），不检测跨调整的模式（振荡、过度调整）。

3. **无调整历史与结果追踪** — 系统可以记录调整（`AdjustmentEvidence`），但没有结构化的调整历史来支持反馈。缺失的是 `AdjustmentHistory` —— 能回答"过去 5 次调整中，哪几次改善了压力状态？"、"`policy-A` 在 OVERLOAD 状态下的成功率是多少？"

4. **无状态转换模型** — v0.13.0 的 `SnapshotPressureClassifier` 对每次 `classify()` 调用独立分类，不跟踪状态转换。`PressureState` 之间没有显式的合法转换定义。v0.13.0 DFR-02 将此延后到 v0.14.0。缺失的是 `PressureStateTransition` 模型 —— 定义哪些状态转换是合法的/预期的（如 NORMAL→QUEUE_BUILDUP 合法，UNDER_UTILIZED→RECOVERY 非法），并追踪转换历史。

5. **无反馈驱动的权重校准** — v0.13.0 `ThresholdPolicyScorer` 的 4 维权重（0.35/0.30/0.20/0.15）是静态的。v0.13.0 DFR-01 明确说明需要闭环调整的实际运行数据来校准这些权重。缺失的是反馈回路 —— 调整后观察结果，根据结果调整评分权重。

### 1.3 JDK API 可行性评估

v0.14.0 不引入新的 `ThreadPoolExecutor` 属性变更：

| 问题 | 答案 |
|---|---|
| 是否需要新 `ThreadPoolExecutor` 属性变更？ | 否 — 闭环调整通过已有 AdjustmentAdapter 接口执行变更（pool size, queue capacity, rejection policy 均已实现） |
| 是否需要新的外部依赖？ | 否 — 所有数据源（PressureSampler, PressureClassifier, PolicyScorer, SystemCpuProbe）均已在既有版本中实现 |
| 是否需要修改 executor 行为？ | 否 — 闭环控制器仅编排已有组件 |

### 1.4 与既有基础设施的关系

闭环调整是既有组件的编排层，不引入新的 executor mutation：

| 既有组件 | v0.14.0 中的角色 |
|---|---|
| `LivePressureSampler` (v0.11.0) | 闭环的采样输入源。`AdjustmentLoop` 消费其采样的 `ObservedSnapshot` 序列 |
| `SnapshotPressureClassifier` (v0.13.0) | 闭环的诊断层。将快照序列分类为 `PressureClassification` |
| `PolicyScorer` + `PolicyRanker` (v0.13.0) | 闭环的策略选择层。对候选策略评分排序，选择最优策略 |
| `ThresholdPolicyEvaluator` (v0.4.0) | 闭环的决策生成层。从选定策略 + 当前快照生成 `PolicyDecision` |
| `SafetyGate` (v0.7.0) | 闭环的安全校验层。在应用调整前验证边界 |
| `AdjustmentAdapter` (v0.7.0-v0.10.0) | 闭环的执行层。将 `ScaleAdjustmentCommand` 应用到 `ManagedExecutor` |
| `EvidenceRecorder` (v0.11.0) | 闭环的证据记录层。记录每次调整的证据 |
| `ExperimentCoordinator` (v0.7.0) | 闭环的 run 管理层。管理闭环 run 的生命周期 |

关键：**闭环控制器不是新能力，而是对既有能力的编排**。它通过组合已有组件实现自主调整，本身不修改任何 executor 属性。

### 1.5 为什么是现在

- v0.13.0 完成了诊断层 — 系统现在能回答"当前是什么状态"和"哪个策略最适合"
- 闭环调整是 roadmap 中诊断层的直接后继 — v0.13.0 的 `00-objectives-and-scope.md` 第 1.2 节将闭环调整列为 v0.14.0 候选
- v0.13.0 复盘确认了诊断层的成熟度（774 测试，IR/SR pipeline 已验证）
- 没有闭环执行器，分类器和评分器的价值停留在"纸上诊断" — 闭环使诊断产生行动
- v0.13.0 DFR-01（权重自动校准）和 DFR-02（状态转换模型）都需要闭环基础设施来承载
- 振荡防护是实现"安全自主调整"的必要条件 — 没有防护的闭环是不可部署的

## 2. 目标

`v0.14.0` 聚焦以下目标：

1. **AdjustmentLoop 闭环控制器**：管理闭环生命周期（IDLE → RUNNING → PAUSED → STOPPED），周期性地执行采样→分类→评分→选择→决策→执行→观察循环
2. **DecisionOrchestrator 决策编排器**：将分类结果、策略排序和策略评估串联为单一的 `AdjustmentDecision`，作为执行层的输入
3. **PressureStateTransition 状态转换模型**：定义 6 种压力状态之间的合法转换，追踪转换历史，支持"当前状态 + 历史路径"的上下文感知分类
4. **OscillationDetector 振荡检测器**：从调整历史中检测配置振荡模式（如 A→B→A→B），在检测到振荡时阻止新调整并触发冷却
5. **AdjustmentHistory 调整历史**：记录每次调整的完整信息（决策→命令→结果→后续观察），支持查询和统计
6. **反馈驱动的权重校准**：基于调整后的实际结果（压力状态是否改善），调整 `ThresholdPolicyScorer` 的 4 维评分权重
7. **端到端闭环验证**：至少一个场景中闭环自主运行 ≥ 5 个周期，包含状态转换检测和至少一次策略切换

## 3. 范围内

- `LoopState` enum（IDLE, RUNNING, PAUSED, STOPPED, EMERGENCY_STOPPED）
- `LoopConfig` record（samplingInterval, cooldownPeriod, maxIterations, oscillationWindowSize, emergencyStopThreshold）
- `AdjustmentLoop` 类（闭环生命周期管理 + 主循环逻辑）
- `DecisionOrchestrator` 类（编排 classification → scoring → ranking → evaluation → decision）
- `AdjustmentDecision` record（selectedPolicy, pressureClassification, policyScore, policyDecision, decisionRationale）
- `PressureStateTransition` record（from, to, timestamp, trigger, legal）
- `PressureStateMachine` 类（合法转换定义 + 转换验证 + 转换历史）
- `OscillationDetector` 类（滑动窗口振荡模式检测）
- `AdjustmentHistory` 类（调整记录存储 + 查询）
- `HistoryWindow` record（窗口大小 + 调整记录列表）
- `LoopSession` record（sessionId, loopConfig, startTime, endTime, adjustmentCount, finalState, summary）
- `FeedbackCalibrator` 类（基于调整结果校准评分权重）
- `experiment.loop` 新包
- 现有 774 测试零回归

## 4. 范围外

- 多 executor 并行闭环协调（单 executor 闭环是 v0.14.0 范围，多 executor 协调是 v0.15.0+ 候选）
- 跨进程/分布式闭环调整（当前所有组件在单 JVM 内）
- 策略自动生成或参数自动调优（仅使用预定义的 `ThresholdPolicyConfig` 候选列表 + 权重校准）
- 闭环性能优化（吞吐量、延迟基准等 — v0.13.0 DFR-04）
- 闭环与外部监控系统集成
- CLI entry
- 新 executor mutation 或动态配置维度

## 5. 架构对齐

| 架构文档 | 本版本对应内容 |
|---|---|
| `observability-and-experiment-strategy.md` | "观察行为后再优化" — 闭环调整是此策略的最终实现：观察→诊断→行动→再观察 |
| `operational-and-evolution-boundaries.md` | 不引入新 executor mutation、不引入外部依赖、不扩展 Redis/Kafka/数据库边界；闭环调整通过已有 SafetyGate 保证操作安全 |
| `managed-executor-domain-model.md` | 扩展编排层（adjustment loop），不修改 ManagedExecutor 自身；遵循"每个可变操作必须显式且可追溯"原则 |
| `scheduling-reconfiguration-and-recovery-model.md` | 闭环调整引入冷却期（cooldown）和紧急停止（emergency stop）作为恢复机制 |

## 6. 模块边界

| 模块 | 变更类型 | 说明 |
|---|---|---|
| `experiment.loop` | **新增** `LoopState` | 闭环生命周期状态枚举 |
| `experiment.loop` | **新增** `LoopConfig` | 闭环配置 record |
| `experiment.loop` | **新增** `LoopSession` | 闭环会话 record |
| `experiment.loop` | **新增** `AdjustmentLoop` | 主闭环控制器 |
| `experiment.loop` | **新增** `DecisionOrchestrator` | 决策编排器 |
| `experiment.loop` | **新增** `AdjustmentDecision` | 调整决策 record |
| `experiment.loop` | **新增** `PressureStateTransition` | 状态转换 record |
| `experiment.loop` | **新增** `PressureStateMachine` | 状态机（合法转换 + 历史） |
| `experiment.loop` | **新增** `OscillationDetector` | 振荡检测器 |
| `experiment.loop` | **新增** `AdjustmentHistory` | 调整历史 |
| `experiment.loop` | **新增** `HistoryWindow` | 历史窗口 record |
| `experiment.loop` | **新增** `FeedbackCalibrator` | 权重校准器 |
| `experiment.loop` | **新增** `LoopEvidenceRecorder` | 闭环证据记录器 |
| `experiment.classification` | 不变 | 分类器、评分器、排序器作为闭环的诊断输入 |
| `experiment.policy` | 不变 | PolicyEvaluator 和 ThresholdPolicyConfig 作为闭环的决策生成输入 |
| `experiment.adjustment` | 不变 | AdjustmentAdapter, SafetyGate 作为闭环的执行层 |
| `experiment.metrics` | 不变 | RuntimeObservation, PressureSnapshot 作为闭环的数据源 |
| `experiment.executor` | 不变 | ManagedExecutor 作为闭环的调整目标 |

### 依赖方向

```text
experiment.loop (AdjustmentLoop, DecisionOrchestrator, AdjustmentDecision,
                 PressureStateMachine, PressureStateTransition,
                 OscillationDetector, AdjustmentHistory, HistoryWindow,
                 FeedbackCalibrator, LoopSession, LoopConfig, LoopState,
                 LoopEvidenceRecorder)
    ├── experiment.classification (PressureClassifier, PolicyScorer, PolicyRanker,
    │                              PressureClassification, PolicyScore — 读取)
    ├── experiment.policy (PolicyEvaluator, ThresholdPolicyConfig, PolicyDecision — 读取)
    ├── experiment.adjustment (AdjustmentAdapter, SafetyGate, ScaleAdjustmentCommand — 调用)
    ├── experiment.metrics (RuntimeObservation, PressureSnapshot — 读取)
    ├── experiment.executor (ManagedExecutor, ExecutorRegistry — 读取/调用)
    └── experiment.acquisition (EvidenceRecorder — 调用)
```

## 7. 核心技术设计

### 7.1 LoopState 与闭环生命周期

```java
public enum LoopState {
    IDLE,              // 初始状态，未启动
    RUNNING,           // 闭环运行中
    PAUSED,            // 暂停（保持状态，可恢复）
    STOPPED,           // 正常停止
    EMERGENCY_STOPPED  // 紧急停止（检测到异常模式）
}
```

合法转换：
```
IDLE → RUNNING
RUNNING → PAUSED
PAUSED → RUNNING
RUNNING → STOPPED
PAUSED → STOPPED
RUNNING → EMERGENCY_STOPPED  (振荡检测触发)
PAUSED → EMERGENCY_STOPPED   (振荡检测触发)
EMERGENCY_STOPPED → IDLE     (reset)
STOPPED → IDLE               (reset)
```

### 7.2 AdjustmentLoop 主循环

```java
public final class AdjustmentLoop {
    private final LoopConfig config;
    private final DecisionOrchestrator orchestrator;
    private final AdjustmentAdapter adapter;
    private final SafetyGate safetyGate;
    private final OscillationDetector oscillationDetector;
    private final AdjustmentHistory history;
    private final LoopEvidenceRecorder evidenceRecorder;
    private final PressureStateMachine stateMachine;

    private LoopState state = LoopState.IDLE;
    private LoopSession currentSession;

    public LoopSession start(ManagedExecutor executor, List<ThresholdPolicyConfig> candidates);
    public void pause();
    public void resume();
    public LoopSession stop();
    public void emergencyStop(String reason);

    // 主循环（在 start() 中启动，在 stop()/emergencyStop() 中终止）
    private void runLoop(ManagedExecutor executor, List<ThresholdPolicyConfig> candidates);
}
```

主循环伪代码：

```
while state == RUNNING:
    1. sleep(config.samplingInterval)
    2. snapshots = liveSampler.recentSnapshots(config.snapshotWindowSize)
    3. decision = orchestrator.decide(snapshots, candidates, executor)
    4. if decision.action == NO_OP: continue
    5. if oscillationDetector.wouldOscillate(decision, history):
           emergencyStop("oscillation detected")
           break
    6. safetyResult = safetyGate.evaluate(decision.toCommand(), executor)
    7. if safetyResult.rejected(): record rejection; continue
    8. result = adapter.apply(decision.toCommand())
    9. history.record(decision, result)
    10. evidenceRecorder.recordIteration(decision, result, session)
    11. stateMachine.recordTransition(previousClassification, decision.classification)
    12. if history.recentAdjustmentCount() >= config.feedbackCalibrationWindow:
            calibrator.calibrate(history, scorer)
    13. checkStopConditions()
```

**关键设计决策**：闭环使用单线程轮询模型（`ScheduledExecutorService` 或简单的 `while` + `sleep`），而非事件驱动模型。理由见 `decision-log.md` D1。

### 7.3 DecisionOrchestrator 决策编排

```java
public final class DecisionOrchestrator {
    private final PressureClassifier classifier;
    private final PolicyScorer scorer;
    private final PolicyRanker ranker;
    private final PolicyEvaluator evaluator;

    public AdjustmentDecision decide(
        List<ObservedSnapshot> snapshots,
        List<ThresholdPolicyConfig> candidates,
        ManagedExecutor executor
    );
}
```

`decide()` 流程：

1. 调用 `classifier.classify(snapshots, classifierConfig)` → `PressureClassification`
2. 对每个候选策略调用 `scorer.score(classification, config)` → `List<PolicyScore>`
3. 调用 `ranker.rank(classification, candidates)` → 排序后的 `List<PolicyScore>`
4. 取 best score 的 `ThresholdPolicyConfig`，调用 `evaluator.evaluate(input, config)` → `PolicyDecision`
5. 组装 `AdjustmentDecision`

```java
public record AdjustmentDecision(
    PressureClassification classification,
    PolicyScore selectedScore,
    ThresholdPolicyConfig selectedPolicy,
    PolicyDecision policyDecision,
    String rationale,
    Instant decidedAt
) {
    public ScaleAdjustmentCommand toCommand(ManagedExecutor executor) { ... }
    public boolean isNoOp() { return policyDecision.action() == PolicyAction.HOLD; }
}
```

### 7.4 PressureStateMachine 状态转换模型

v0.13.0 的分类器是**无状态**的（每次 `classify()` 独立执行）。v0.14.0 引入**有状态**的转换模型作为分类器的补充（不修改分类器自身）：

```java
public final class PressureStateMachine {
    private final List<PressureStateTransition> transitionHistory;

    public boolean isLegalTransition(PressureState from, PressureState to);
    public void recordTransition(PressureState from, PressureState to, Instant timestamp);
    public List<PressureStateTransition> recentTransitions(int count);
    public Optional<PressureState> currentState();
}
```

合法转换定义：

| 从 | 到 | 合法？ | 说明 |
|---|---|---|---|
| 任意 | NORMAL | ✅ | 总是可以回到稳态 |
| NORMAL | QUEUE_BUILDUP | ✅ | 压力开始积累 |
| NORMAL | UNDER_UTILIZED | ✅ | 压力降低 |
| NORMAL | OVERLOAD | ✅ | 突然的过载（如流量尖峰） |
| QUEUE_BUILDUP | OVERLOAD | ✅ | 队列增长导致线程饱和 |
| QUEUE_BUILDUP | NORMAL | ✅ | 干预成功，压力缓解 |
| OVERLOAD | RECOVERY | ✅ | 干预后开始恢复 |
| OVERLOAD | REJECTION_ACTIVE | ✅ | 过载恶化到拒绝 |
| REJECTION_ACTIVE | RECOVERY | ✅ | 干预后停止拒绝 |
| RECOVERY | NORMAL | ✅ | 恢复完成 |
| RECOVERY | UNDER_UTILIZED | ✅ | 恢复过度，线程过剩 |
| UNDER_UTILIZED | NORMAL | ✅ | 负载回升 |
| OVERLOAD | NORMAL | ⚠️ 异常 | 跳过 RECOVERY — 可能表示分类器误判 |
| RECOVERY | OVERLOAD | ❌ | 恢复中不应直接回到过载 |
| UNDER_UTILIZED | OVERLOAD | ⚠️ 异常 | 应经过 NORMAL/QUEUE_BUILDUP |
| REJECTION_ACTIVE | NORMAL | ⚠️ 异常 | 应经过 RECOVERY |

异常转换（⚠️）触发置信度降低但不阻塞闭环。非法转换（❌）触发警告并检查分类器配置。

### 7.5 OscillationDetector 振荡检测

```java
public final class OscillationDetector {
    private final int windowSize;       // 检测窗口大小（调整次数），默认 6
    private final int patternThreshold; // 触发振荡的最小重复次数，默认 2

    public boolean wouldOscillate(AdjustmentDecision pending, AdjustmentHistory history);
    public Optional<String> detectedPattern(AdjustmentHistory history);
}
```

检测的振荡模式：

1. **乒乓振荡**：A→B→A→B（连续相反的调整方向）。检测方法：检查最近 N 次调整的目标 poolSize 是否形成交替模式
2. **过度调整**：连续 ≥ 3 次同方向调整（如连续 scale-up 3 次）。检测方法：检查最近 N 次调整是否全部同方向
3. **策略切换振荡**：在多个策略间反复切换（policy-A → policy-B → policy-A → policy-C → policy-A）。检测方法：检查最近 N 次决策的 selectedPolicy 是否频繁切换

检测到振荡后的行为：
- 设置 `LoopState = EMERGENCY_STOPPED`
- 记录振荡模式到 `LoopSession.summary`
- 触发 `cooldownPeriod`（冷却期，默认 60s）
- 冷却期后 loop 可被手动 reset 到 IDLE

### 7.6 AdjustmentHistory 调整历史

```java
public final class AdjustmentHistory {
    private final List<HistoryEntry> entries;

    public void record(AdjustmentDecision decision, AdjustmentResult result);
    public List<HistoryEntry> recent(int count);
    public List<HistoryEntry> since(Instant timestamp);
    public int totalAdjustmentCount();
    public int successfulAdjustmentCount(); // 调整后压力状态改善的次数

    public record HistoryEntry(
        AdjustmentDecision decision,
        AdjustmentResult result,
        PressureClassification beforeClassification,
        PressureClassification afterClassification, // 调整后下一个采样周期的分类
        Instant recordedAt
    ) {}
}
```

"调整成功"的定义（供 `FeedbackCalibrator` 使用）：
- 调整后压力状态向"更好"方向移动：OVERLOAD→RECOVERY(NORMAL), QUEUE_BUILDUP→NORMAL, UNDER_UTILIZED→NORMAL
- 或者压力状态未恶化：NORMAL→NORMAL（维持稳态也是一种成功）

### 7.7 FeedbackCalibrator 权重校准

```java
public final class FeedbackCalibrator {
    public void calibrate(AdjustmentHistory history, ThresholdPolicyScorer scorer);
}
```

校准逻辑（DFR-01 实现）：

1. 从 `AdjustmentHistory` 中提取最近 N 次（默认 10 次）调整的结果
2. 对每次调整，计算"预期评分"与"实际结果"的偏差
3. 如果某个维度的评分与结果正相关（高分 → 成功），增加该维度权重
4. 如果某个维度的评分与结果负相关或无关，减少该维度权重
5. 权重调整幅度有限（每次最多 ±0.05），避免单次异常数据导致剧烈变化

权重约束：
- 所有权重之和始终为 1.0
- 每个维度权重在 [0.10, 0.50] 范围内
- 初始权重 = v0.13.0 静态默认值（0.35/0.30/0.20/0.15）

### 7.8 LoopConfig 配置

```java
public record LoopConfig(
    long samplingIntervalMs,            // 采样间隔（毫秒），默认 2000
    long cooldownPeriodMs,              // 调整冷却期（毫秒），默认 10000
    int maxIterations,                  // 最大迭代次数，默认 100（0 = 无限）
    int snapshotWindowSize,             // 分类器输入的快照窗口大小，默认 20
    int oscillationWindowSize,          // 振荡检测窗口（调整次数），默认 6
    int oscillationPatternThreshold,    // 触发振荡的最小重复次数，默认 2
    int feedbackCalibrationWindow,      // 权重校准窗口（调整次数），默认 10
    int emergencyStopThreshold,         // 紧急停止前允许的连续振荡次数，默认 2
    List<ThresholdPolicyConfig> candidatePolicies // 候选策略列表
) {
    public static LoopConfig defaults(List<ThresholdPolicyConfig> candidates) { ... }
}
```

### 7.9 与 LivePressureSampler 的集成模式

`AdjustmentLoop` 不直接实现采样逻辑。它通过以下两种方式之一消费快照：

**模式 A：直接集成** — `AdjustmentLoop` 持有 `LivePressureSampler` 引用，在每次迭代中调用 `sampler.recentSnapshots(windowSize)`。

**模式 B：回调/监听器** — `LivePressureSampler` 不变，`AdjustmentLoop` 使用独立的轮询线程消费采样结果。

**设计建议（IR 阶段确认）**：选择模式 A — `AdjustmentLoop` 直接调用 `LivePressureSampler`。模式 B 引入不必要的间接层，且 `LivePressureSampler` 当前不支持监听器注册。

### 7.10 闭环安全性约束

1. **冷却期**：两次调整之间最小间隔 = `cooldownPeriodMs`（默认 10s）。在冷却期内的新调整被跳过，记录为 NO_OP
2. **最大迭代**：`maxIterations` 限制总迭代次数（0 = 无限）。达到上限 → STOPPED
3. **振荡紧急停止**：连续检测到 `emergencyStopThreshold` 次振荡 → EMERGENCY_STOPPED
4. **SafetyGate 前置**：每次调整前必须通过 SafetyGate 验证
5. **边界硬限制**：`LoopConfig.candidatePolicies` 中的 maxPoolSize/minPoolSize 定义绝对边界，闭环不能超出

## 8. 成功标准草案

- `AdjustmentLoop` 正确执行全部 5 种生命周期状态转换
- `DecisionOrchestrator.decide()` 产出包含完整诊断链的 `AdjustmentDecision`（分类→评分→排序→评估→决策）
- `PressureStateMachine` 正确定义并验证所有合法/异常/非法状态转换
- `PressureStateMachine` 在生产环境中追踪 ≥ 3 次连续状态转换
- `OscillationDetector` 正确检测乒乓振荡模式（A→B→A→B）
- `OscillationDetector` 正确检测过度调整模式（连续 ≥ 3 次同方向调整）
- `OscillationDetector` 检测到振荡后触发 EMERGENCY_STOPPED
- `AdjustmentHistory` 记录每次调整的完整 before/after 信息
- `FeedbackCalibrator` 在 ≥ 10 次调整后成功校准权重（权重值偏离初始默认值）
- 至少 1 个端到端场景：闭环自主运行 ≥ 5 个周期（包括 ≥ 1 次实际调整），压力状态从 QUEUE_BUILDUP 或 OVERLOAD 改善到 NORMAL
- 闭环在检测到振荡时正确触发紧急停止，不执行第 3 次同方向调整
- 现有 774 测试零回归

## 9. 候选 Change Decomposition

IR/SR 阶段确认后：

| # | Change name | 范围 | 依赖 |
|---|---|---|---|
| 1/2 | `adaptive-loop-core` | LoopState, LoopConfig, LoopSession, AdjustmentLoop, DecisionOrchestrator, AdjustmentDecision, PressureStateTransition, PressureStateMachine, 单元测试 | PressureClassifier, PolicyScorer, PolicyRanker, PolicyEvaluator, LivePressureSampler, AdjustmentAdapter, SafetyGate |
| 2/2 | `oscillation-guard-and-loop-verification` | OscillationDetector, AdjustmentHistory, HistoryWindow, FeedbackCalibrator, LoopEvidenceRecorder, 端到端闭环验证 | Change 1（AdjustmentLoop, DecisionOrchestrator, AdjustmentHistory 接口）, AdjustmentResult |

### 独立可验证性检查（预检）

- Change 1 可独立编译和测试：`AdjustmentLoop` 的生命周期、`DecisionOrchestrator` 的决策编排、`PressureStateMachine` 的状态转换验证均可独立测试。振荡检测和权重校准可 mock/stub
- Change 2 依赖 Change 1 的 `AdjustmentLoop`、`AdjustmentHistory` 和 `AdjustmentDecision` 类型，但振荡检测和权重校准的算法逻辑独立于闭环主循环
- 两个 change 均可独立运行 `mvn test` 并通过各自范围内的测试

## 10. 当前阶段出口

进入 IR 前必须完成：

1. `README.md` 版本索引
2. `00-objectives-and-scope.md`（本文档）
3. `decision-log.md` 记录关键设计判断
4. `docs/00-project/current-state.md` 反映 v0.14.0 版本设计草稿状态
