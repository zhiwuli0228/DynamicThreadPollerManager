# v0.14.0 IR Review

## Header

- Document type: IR independent review
- Version name: `v0.14.0`
- Review date: `2026-06-14`
- Reviewer: Independent IR review agent
- Status: `READY_FOR_DISPOSITION`
- Source IR: `10-ir.md` (v0.14.0 IR 需求分析草案)

## Scope

对 v0.14.0 IR 草案执行独立评审，重点检查：
1. IR 是否只做需求，不隐含实现授权
2. 需求中的 API 引用是否与实际源码签名一致
3. 组件边界和依赖方向是否明确
4. 遗漏的边界情况和交互契约
5. 范围和风险覆盖完整性

评审对照实际源码验证了所有关键 API 签名。

## Findings

### P0 — 阻断性

#### F01: PolicyEvaluationInput 构造需要 PressureSnapshot，IR 描述的数据提取路径不完整

**位置**: IR-v0.14-003 步骤 8 + 注

**问题**: IR 说"从 snapshots 列表取最后一个快照的瞬时 activeThreads 和 queueSize 作为 PolicyEvaluationInput 的当前值"，暗示构造 `PolicyEvaluationInput(runId, activeThreads, queueSize, evaluatedAt)`。但实际签名是：

```java
// PolicyEvaluationInput.java:21
public PolicyEvaluationInput(String runId, PressureSnapshot snapshot, Instant evaluatedAt)
```

构造器需要 `PressureSnapshot` 对象，不是单独的 int 值。

**实际可行路径**: `ObservedSnapshot.snapshot()` 返回 `PressureSnapshot`，包含全部 6 个字段（timestamp, activeThreads, poolSize, queueSize, completedTaskCount, cpuUtilization）。最后一个 `ObservedSnapshot` 的 `snapshot()` 可直接传入 `PolicyEvaluationInput`。

**建议**: 修正 IR-v0.14-003 中 `decide()` 步骤 8 的描述：从 `snapshots.get(snapshots.size()-1).snapshot()` 获取最后一个 `PressureSnapshot`，直接传入 `PolicyEvaluationInput`。

**严重级别**: P0（API 签名不匹配会导致编译失败）

---

#### F02: RuntimeAdjustmentSafetyGate.evaluate() 签名为 3 参数，IR 使用 2 参数

**位置**: IR-v0.14-002 主循环步骤 8

**问题**: IR 主循环伪代码写为：
```java
safetyGate.evaluate(decision.toCommand(), executor)
```
实际签名为：
```java
// RuntimeAdjustmentSafetyGate.java:17-19
SafetyGateDecision evaluate(ScaleAdjustmentCommand command,
                            ExecutorStateSnapshot currentState,
                            ReadinessAssessment readiness);
```

需要 3 个参数：`command`、`ExecutorStateSnapshot`、`ReadinessAssessment`。`AdjustmentLoop` 必须能提供后两者。

**影响分析**:
- `ExecutorStateSnapshot` 可从 `adapter.currentState()` 获取（`ExecutorAdjustmentAdapter` 接口已有此方法）
- `ReadinessAssessment` 来自 v0.5.0 离线回放模块（`MutationReadinessGate`），它依赖 `ScenarioProfile` 列表和 `ReadinessStatus`。**闭环运行是实时的，没有离线回放场景**，因此不存在自然的 `ReadinessAssessment` 数据源

**建议**: 
1. IR 中明确 `ExecutorStateSnapshot` 来源：`adapter.currentState()`
2. IR 必须解决 `ReadinessAssessment` 缺口。两个方向：
   - A: 创建 `ReadinessAssessment.runtimeReady()` 静态工厂方法（总是 READY），因为闭环运行不需要离线回放就绪判定
   - B: `AdjustmentLoop` 使用 `SafetyGateConfig` 直接构造 gate 实例，gate 在 readiness=READY 且 allowReadyWithRisk=true 时放行（`SafetyGateConfig.defaults().allowReadyWithRisk()` 当前默认值需验证）
3. 修正主循环伪代码

**严重级别**: P0（缺少参数会导致编译失败；ReadinessAssessment 来源缺失是设计缺口）

---

#### F03: ThresholdPolicyScorer 权重为 private final，FeedbackCalibrator 无法读写

**位置**: IR-v0.14-007

**问题**: IR 描述 FeedbackCalibrator 要"更新 scorer 的内部权重配置"。但 `ThresholdPolicyScorer` 的实现是：

```java
// ThresholdPolicyScorer.java:15-18
private final double wResponsiveness;
private final double wSafety;
private final double wStability;
private final double wEfficiency;
```

权重字段是 `private final` — 既没有公共 getter 也没有 setter。FeedbackCalibrator 既不能读取当前权重（无 getter），也不能写入新权重（无 setter，字段 final）。

**建议**: 两个互补方案：
1. 在 `ThresholdPolicyScorer` 上添加包级可见的权重 getter（如 `double wResponsiveness()`），使 `FeedbackCalibrator` 可读取当前权重
2. `FeedbackCalibrator.calibrate()` 返回**新的** `ThresholdPolicyScorer` 实例（而不是修改现有实例），`AdjustmentLoop` 使用新 scorer 替换 DecisionOrchestrator 中的旧 scorer

**严重级别**: P0（FeedbackCalibrator 在当前 scorer 设计下无法实现）

---

#### F04: SafetyGateDecision 使用 Outcome 枚举，IR 中 rejected() 语义不准确

**位置**: IR-v0.14-002 主循环步骤 8-9

**问题**: IR 伪代码写为：
```java
if safetyGate rejected → record rejection, continue
```

实际 `SafetyGateDecision` 使用 `Outcome` 枚举（ALLOW, REJECTED, NO_OP），不提供 `rejected()` boolean 方法。判断逻辑应为：
```java
SafetyGateDecision decision = safetyGate.evaluate(...);
if (decision.outcome() == SafetyGateDecision.Outcome.REJECTED) { ... }
```

同时注意 `NO_OP` 也是非 ALLOW 的 outcome，需与 REJECTED 区分处理。

**建议**: 修正主循环伪代码，明确使用 `outcome()` 方法判断，区分 REJECTED 和 NO_OP 两种非 ALLOW 情况。

**严重级别**: P0（伪代码与实际 API 不一致，会导致 SR 设计错误）

---

#### F05: SafetyGate 内部已有 cooldown 机制，与 LoopConfig.cooldownPeriodMs 职责重叠

**位置**: IR-v0.14-002 主循环步骤 2 + IR-v0.14-009 冷却期约束

**问题**: `DefaultRuntimeAdjustmentSafetyGate` 内部已有 cooldown 机制：
- `cooldownRemaining` 计数器
- `config.cooldownDecisionIntervals()` 配置冷却间隔
- `recordApplied()` 更新冷却

IR 在 `AdjustmentLoop` 主循环中又设计了一个独立的冷却期（`LoopConfig.cooldownPeriodMs`），基于 wall-clock 时间。两个冷却机制同时存在会产生冲突：

1. SafetyGate 的 cooldown 是基于"decision intervals"（次数），不是 wall-clock 时间
2. AdjustmentLoop 的 cooldown 是基于 wall-clock 毫秒
3. 如果两者不一致，可能一个放行而另一个阻塞

**建议**: 明确两个冷却机制的关系和优先级：
- A: 移除 `LoopConfig.cooldownPeriodMs`，完全依赖 SafetyGate 的 cooldown（通过 `SafetyGateConfig.cooldownDecisionIntervals()` 配置）
- B: 移除 SafetyGate 的 cooldown 对闭环的影响（闭环使用独立的 wall-clock cooldown），SafetyGate 仅校验 readiness/bounds/direction
- C: 双重保护——两者都生效，任一触发则跳过调整

推荐 A，因为 SafetyGate 已经是调整执行的标准安全层，闭环不应重复实现。

**严重级别**: P0（两个独立的冷却机制会导致行为不确定）

---

### P1 — 关键

#### F06: LivePressureSampler 缺少直接获取快照列表的方法

**位置**: IR-v0.14-002 主循环步骤 3 + IR 风险表

**问题**: IR 主循环伪代码使用 `sampler.recentSnapshots(config.snapshotWindowSize)`，但 `LivePressureSampler` 不提供此方法。快照通过 `EvidenceRecorder.record()` 写入。获取快照的唯一路径是 `EvidenceRecorder.snapshots(runId)`。

IR 风险表中已标注此风险（P2），但主循环伪代码未反映实际 API。`AdjustmentLoop` 需要持有 `EvidenceRecorder` 引用（而非或外加 `LivePressureSampler`）。

**建议**: 
1. 修正主循环步骤 3：从 `evidenceRecorder.snapshots(runId)` 获取快照列表，取最近 `snapshotWindowSize` 个
2. 明确 `AdjustmentLoop` 需要 `EvidenceRecorder` 依赖（构造参数或通过 sampler 间接获取）
3. 解决并发问题：sampler 写入和 loop 读取共享同一 runId 的 snapshots 列表。`InMemoryEvidenceRecorder` 使用 `ArrayList`（非线程安全），`FileBackedEvidenceRecorder` 使用文件。SR 必须设计线程安全的数据交换

**严重级别**: P1（设计缺口——获取快照的 API 路径不明确）

---

#### F07: DecisionOrchestrator 与 PolicyRanker 的关系冗余

**位置**: IR-v0.14-003 构造参数

**问题**: IR 说 DecisionOrchestrator 同时持有 `PolicyScorer` 和 `PolicyRanker`。但 `PolicyRanker` 构造时已持有 `PolicyScorer`：

```java
// PolicyRanker.java:16-17
public PolicyRanker(PolicyScorer scorer) {
    this.scorer = Objects.requireNonNull(scorer, "scorer must not be null");
}
```

`DecisionOrchestrator.decide()` 步骤 4 说"对每个 candidate 调用 scorer.score()"，步骤 5 说"调用 ranker.rank()"。实际上步骤 4 是多余的——`ranker.rank()` 内部已经调用 `scorer.score()`。

**建议**: `DecisionOrchestrator` 只持有 `PolicyRanker`（它内部已有 scorer），去掉独立的 `PolicyScorer` 字段。如果 FeedbackCalibrator 需要 scorer 引用，通过独立的注入路径提供。

**严重级别**: P1（设计冗余——两个字段指向同一职责，增加了不一致风险）

---

#### F08: AdjustmentLoop 缺少 reset() 方法但状态转换需要

**位置**: IR-v0.14-001 合法转换 + IR-v0.14-002

**问题**: IR-v0.14-001 合法转换定义中包含：
- `EMERGENCY_STOPPED → IDLE`（reset）
- `STOPPED → IDLE`（reset）

但 IR-v0.14-002 的 `AdjustmentLoop` 方法列表中只有 `start()`, `pause()`, `resume()`, `stop()`, `emergencyStop()`，没有 `reset()` 方法。没有 `reset()`，闭环无法从 STOPPED/EMERGENCY_STOPPED 回到 IDLE 以重新启动。

**建议**: 添加 `reset()` 方法到 IR-v0.14-002，该方法：
- 状态必须为 STOPPED 或 EMERGENCY_STOPPED
- 清除 AdjustmentHistory
- 重置 PressureStateMachine
- 状态 → IDLE

**严重级别**: P1（功能缺口——状态机不可达）

---

#### F09: AdjustmentDecision.toCommand() 需要 runId 但闭环中 runId 的来源不明确

**位置**: IR-v0.14-003 AdjustmentDecision.toCommand()

**问题**: `ScaleAdjustmentCommand.create()` 和 `ScaleAdjustmentCommand.noOp()` 都需要 `runId` 参数。闭环中的 runId 是什么？
- 使用 `LoopSession.sessionId`？
- 使用独立的 `runId`（与 `ExperimentCoordinator.createRun()` 产生的 runId 关联）？
- 如果使用 sessionId，语义上 sessionId 代表闭环会话，runId 代表单次实验运行——两者粒度不同

**建议**: 明确闭环中 runId 的来源。推荐使用 `LoopSession.sessionId` 作为 runId（闭环本身就是一个连续的实验运行）。或者通过 `ExperimentCoordinator.createRun("loop", "auto")` 创建独立 run。

**严重级别**: P1（接口契约不明确）

---

### P2 — 次要

#### F10: PressureStateMachine 的 TRANSITION_LEGALITY 命名过长

**位置**: IR-v0.14-004 TransitionLegality enum

**问题**: `TransitionLegality` 枚举值命名清晰，但枚举名称可以作为内部类放在 `PressureStateMachine` 中（`PressureStateMachine.TransitionLegality`）而非独立的顶级类型。

**建议**: SR 阶段确定命名空间放置。非阻塞。

**严重级别**: P2（命名建议）

---

#### F11: IR 中 LoopSession 的 adjustmentCount vs iterationCount 区分不够清晰

**位置**: IR-v0.14-008

**问题**: IR 定义了 `adjustmentCount`（不含 NO_OP）和 `iterationCount`（含 NO_OP），但未定义"iteration"的精确含义。如果冷却期跳过的周期算不算 iteration？sampling-only 周期（cooldown 内）算不算？

**建议**: SR 阶段明确：
- iteration = 每次主循环迭代（包括 sampling-only、NO_OP、cooldown 跳过）
- adjustment = 实际产生非 NO_OP 决策并成功通过 SafetyGate 的迭代

**严重级别**: P2（需要 SR 澄清）

---

#### F12: 风险表中 LivePressureSampler 并发风险的严重级别偏低

**位置**: IR 风险表

**问题**: IR 风险表将 `LivePressureSampler` 快照获取的并发风险标为 P2。考虑到：
1. `InMemoryEvidenceRecorder` 使用 `ArrayList`（非线程安全）
2. sampler 写入和 loop 读取可能同时发生
3. 并发修改 `ArrayList` 会导致 `ConcurrentModificationException` 或数据损坏

这应该是 P1 风险而非 P2。

**建议**: 提升此风险至 P1。SR 必须设计线程安全的快照交换机制（如 `CopyOnWriteArrayList`、显式锁、或使用 `FileBackedEvidenceRecorder` 的原子文件写入）。

**严重级别**: P2→P1（风险级别低估）

---

#### F13: 端到端测试的 candidate 策略配置过于模糊

**位置**: IR-v0.14-010 测试 7

**问题**: 测试 7 只说"3 个候选策略"，未指定具体参数。为了验证闭环确实选择了正确的策略（OVERLOAD → 激进策略），候选策略需要有明确的差异化参数。

**建议**: 在测试验收条件中明确候选策略的具体参数差异：
- 保守策略：scaleUpThreshold=高, scaleStep=1
- 激进策略：scaleUpThreshold=低, scaleStep=4
- 适中策略：scaleUpThreshold=中, scaleStep=2

**严重级别**: P2（测试设计细节）

---

## 综述

### 需求完整性

IR 覆盖了闭环调整的 10 个需求方向，从生命周期管理到端到端验证。需求与 v0.13.0 既有组件的依赖关系基本正确，但存在 5 个 P0 级别的 API 不匹配问题需要优先处置。

### 关键缺口

1. **P0 — ReadinessAssessment 运行时来源缺失**（F02, F05）：闭环是运行时组件，`RuntimeAdjustmentSafetyGate` 需要 `ReadinessAssessment`（离线回放概念）。需要设计运行时 readiness 策略。

2. **P0 — ThresholdPolicyScorer 不可变性**（F03）：`FeedbackCalibrator` 的设计假设 scorer 权重可读写，但实际是 `private final`。需要重新设计权重更新路径。

3. **P0 — 双重冷却机制**（F05）：`SafetyGate` 内部和 `AdjustmentLoop` 各自实现冷却，职责重叠可能导致行为不确定。

4. **P1 — 快照获取路径**（F06）：`LivePressureSampler` 不提供直接快照查询，闭环必须通过 `EvidenceRecorder` 获取，存在并发安全问题。

### 架构一致性

- 闭环控制器作为编排层（不引入新 executor mutation）的设计原则与架构一致
- `experiment.loop` 包的依赖方向正确（向下依赖已有包，不反向）
- 与 v0.13.0 复盘建议一致（重载扩展模式避免行为变更）

### 范围一致性

IR 范围与 `00-objectives-and-scope.md` 和 `decision-log.md` 一致。无范围蠕变。v0.13.0 DFR-01/02 正确承接。

## Findings Summary

| ID | 严重级别 | 位置 | 简要描述 |
|---|---|---|---|
| F01 | P0 | IR-v0.14-003 | PolicyEvaluationInput 构造需要 PressureSnapshot，不是单独字段 |
| F02 | P0 | IR-v0.14-002 步骤 8 | SafetyGate.evaluate() 签名为 3 参数（含 ReadinessAssessment），IR 使用 2 参数 |
| F03 | P0 | IR-v0.14-007 | ThresholdPolicyScorer 权重为 private final，FeedbackCalibrator 无法读写 |
| F04 | P0 | IR-v0.14-002 步骤 9 | SafetyGateDecision 使用 Outcome 枚举，不是 rejected() boolean |
| F05 | P0 | IR-v0.14-002/009 | SafetyGate 内部 cooldown 与 LoopConfig.cooldown 双重冷却机制重叠 |
| F06 | P1 | IR-v0.14-002 步骤 3 | LivePressureSampler 缺少 recentSnapshots()，需通过 EvidenceRecorder 获取 |
| F07 | P1 | IR-v0.14-003 | DecisionOrchestrator 的 Scorer 和 Ranker 字段冗余（Ranker 已含 Scorer） |
| F08 | P1 | IR-v0.14-001/002 | 缺少 reset() 方法，EMERGENCY_STOPPED/STOPPED → IDLE 不可达 |
| F09 | P1 | IR-v0.14-003 | AdjustmentDecision.toCommand() 需要 runId，闭环中 runId 来源不明确 |
| F10 | P2 | IR-v0.14-004 | TransitionLegality 枚举命名空间建议 |
| F11 | P2 | IR-v0.14-008 | adjustmentCount 和 iterationCount 的精确边界不清 |
| F12 | P2→P1 | IR 风险表 | LivePressureSampler 快照并发风险级别低估 |
| F13 | P2 | IR-v0.14-010 | 端到端测试 candidate 策略参数未指定 |

**总结**: 5 P0, 4 P1, 4 P2。所有 P0 均为 API 签名不匹配或设计缺口。所有 P1 均为设计细节缺失或风险低估。P0/P1 必须在 IR disposition 中逐项处置并闭环验证后方可进入 SR。
