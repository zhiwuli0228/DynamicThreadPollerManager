# v0.14.0 IR Review Disposition

## Header

- Document type: IR review disposition
- Version name: `v0.14.0`
- Reviewed artifact: `docs/04-development/versions/v0.14.0/11-ir-review.md`
- Disposition date: `2026-06-14`
- Disposition basis: `docs/02-harness/managed-change-standard.md` §2（出口条件：P0/P1 findings 已处置并通过闭环验证）

## Disposition Summary

| ID | 级别 | 处置 | 理由 |
|---|---|---|---|
| F01 | P0 | **FIX** | 修正 PolicyEvaluationInput 构造路径 |
| F02 | P0 | **FIX** | 修正 SafetyGate.evaluate() 签名，解决 ReadinessAssessment 来源 |
| F03 | P0 | **FIX** | ThresholdPolicyScorer 添加权重 getter，calibrate() 返回新实例 |
| F04 | P0 | **FIX** | 修正 SafetyGateDecision outcome 判断方式 |
| F05 | P0 | **FIX** | 移除 LoopConfig.cooldownPeriodMs，统一使用 SafetyGate cooldown |
| F06 | P1 | **FIX** | 修正快照获取路径，升级并发风险 |
| F07 | P1 | **FIX** | 移除 DecisionOrchestrator 冗余 PolicyScorer 字段 |
| F08 | P1 | **FIX** | 添加 reset() 方法 |
| F09 | P1 | **FIX** | 明确 runId = sessionId |
| F10 | P2 | **DEFER_TO_SR** | 命名空间决策属于 SR 设计细节 |
| F11 | P2 | **DEFER_TO_SR** | SR 阶段精确化 iteration 语义 |
| F12 | P2→P1 | **FIX** | 提升风险级别至 P1，SR 设计线程安全快照交换 |
| F13 | P2 | **DEFER_TO_SR** | SR 测试设计时指定候选策略具体参数 |

## Detailed Disposition

### F01 [P0 → FIX] PolicyEvaluationInput 构造需要 PressureSnapshot

**处置**: FIX。修正 IR-v0.14-003 `decide()` 步骤 8 的描述。

修改 `10-ir.md` IR-v0.14-003：

将步骤 7-8 从：
> 7. 用该 score 对应的 `ThresholdPolicyConfig`，构造 `PolicyEvaluationInput`（从 classification 提供当前状态数据）
> 8. 调用 `evaluator.evaluate(input, selectedConfig)` → `PolicyDecision`

改为：
> 7. 从 `snapshots.get(snapshots.size()-1).snapshot()` 获取最后一个 `PressureSnapshot`
> 8. 构造 `PolicyEvaluationInput`：`new PolicyEvaluationInput(runId, lastSnapshot, Instant.now())`
> 9. 调用 `evaluator.evaluate(input, selectedConfig)` → `PolicyDecision`

同时删除 IR-v0.14-003 末尾的注（`PolicyEvaluationInput` 构造需要确认构造器签名），因为签名已验证。

数据流验证：
- `ObservedSnapshot.snapshot()` → `PressureSnapshot`（含 activeThreads, poolSize, queueSize, completedTaskCount, cpuUtilization, timestamp）
- `PolicyEvaluationInput` 构造器签名：`(String runId, PressureSnapshot snapshot, Instant evaluatedAt)` — 已通过源码验证
- `ThresholdPolicyEvaluator.evaluate()` 通过 `input.snapshot().activeThreads()` 和 `input.snapshot().queueSize()` 访问当前瞬时值

---

### F02 [P0 → FIX] SafetyGate.evaluate() 签名为 3 参数，ReadinessAssessment 运行时来源

**处置**: FIX。修正 IR 伪代码中的 API 调用，设计运行时 `ReadinessAssessment` 来源。

修改 `10-ir.md` IR-v0.14-002 主循环步骤 8：

从：
```java
safetyGate.evaluate(decision.toCommand(), executor)
```

改为：
```java
ExecutorStateSnapshot executorState = adapter.currentState();
SafetyGateDecision gateDecision = safetyGate.evaluate(
    decision.toCommand(), executorState, loopReadiness);
```

`ReadinessAssessment` 来源设计：

闭环运行时不存在离线回放场景，因此不经过 `MutationReadinessGate`。`AdjustmentLoop` 在 `start()` 时构造一个运行时 readiness assessment：

```java
ReadinessAssessment loopReadiness = new ReadinessAssessment(
    ReadinessStatus.READY,           // 运行时始终为 READY
    List.of(),                       // 无离线 scenario profiles
    List.of(),                       // 无缺失 profiles
    List.of(),                       // 无阻塞原因
    List.of(),                       // 无风险原因
    "runtime-loop",                  // config label
    List.of(session.sessionId())     // input run IDs
);
```

理由：
- 运行时闭环不依赖离线回放数据，不存在 NOT_READY 场景
- `ReadinessStatus.READY` 通过 SafetyGate 的 readiness 检查（NOT_READY 和 READY_WITH_RISK 均不放行）
- 如果未来需要运行时 readiness 判定（如"采样数据不足不调整"），可在 SR 中添加 `RuntimeReadinessGate` 独立组件
- `ReadinessAssessment` 构造器已验证：所有 List 字段可为空列表，selectedConfigLabel 非 blank 即可

同步修正 IR-v0.14-009 中 SafetyGate 前置描述：
> 每次调整前必须通过 `RuntimeAdjustmentSafetyGate.evaluate()`。`AdjustmentLoop` 通过 `adapter.currentState()` 获取 `ExecutorStateSnapshot`，使用运行时 `ReadinessAssessment`（始终 READY）。

并添加主循环步骤 8b：
> 8b. 如果 gateDecision.outcome() == ALLOW → 调用 `adapter.apply(command)` → 调用 `safetyGate.recordApplied(gateDecision)`

---

### F03 [P0 → FIX] ThresholdPolicyScorer 权重不可读写

**处置**: FIX。两个互补修改：

**修改 A**: 在 `ThresholdPolicyScorer` 上添加包级可见的权重 getter（IR 阶段记录需求，实现属于 v0.14.0 Change 2）：

```java
// 包级可见 getter（experiment.classification 包内可访问）
double wResponsiveness() { return wResponsiveness; }
double wSafety() { return wSafety; }
double wStability() { return wStability; }
double wEfficiency() { return wEfficiency; }
```

**修改 B**: 修改 IR-v0.14-007 `FeedbackCalibrator.calibrate()` 签名和语义：

从：
> 更新 scorer 的内部权重配置

改为：
```java
public ThresholdPolicyScorer calibrate(AdjustmentHistory history, ThresholdPolicyScorer currentScorer);
```
- 方法读取 `currentScorer` 的当前权重，计算新权重
- 返回**新的** `ThresholdPolicyScorer` 实例（使用新权重）
- 不修改传入的 `currentScorer`（不可变模式）
- `AdjustmentLoop` 在收到新 scorer 后替换 `DecisionOrchestrator` 持有的 scorer 引用

修改 IR-v0.14-002 主循环步骤 14：
```java
if (history.totalAdjustmentCount() % config.feedbackCalibrationWindow == 0) {
    ThresholdPolicyScorer newScorer = calibrator.calibrate(history, currentScorer);
    orchestrator.updateScorer(newScorer);  // or: recreate ranker with new scorer
    currentScorer = newScorer;
}
```

`DecisionOrchestrator` 需要添加 `updateScorer()` 方法或通过重新创建 `PolicyRanker` 来更新 scorer。

---

### F04 [P0 → FIX] SafetyGateDecision 使用 Outcome 枚举

**处置**: FIX。修正 IR-v0.14-002 主循环步骤 9 的判断方式。

修改 `10-ir.md` IR-v0.14-002 主循环步骤 8-9：

从：
```
8. safetyResult = safetyGate.evaluate(...)
9. if safetyResult rejected → record rejection, continue
```

改为：
```
8. gateDecision = safetyGate.evaluate(command, executorState, loopReadiness)
9. if gateDecision.outcome() == Outcome.REJECTED:
       history.recordRejection(decision, gateDecision); continue
9a. if gateDecision.outcome() == Outcome.NO_OP:
       continue  // target equals current state, no adjustment needed
9b. if gateDecision.outcome() == Outcome.ALLOW:
       proceed to step 10
```

同时修正 IR 验收条件 AC-v0.14-031：
> SafetyGate REJECTED → 记录拒绝，不停止闭环。SafetyGate NO_OP → 跳过调整，不停止闭环

---

### F05 [P0 → FIX] 双重冷却机制冲突

**处置**: FIX。移除 `LoopConfig.cooldownPeriodMs`，统一使用 `SafetyGate` 内置 cooldown 机制。

**分析**:
- `DefaultRuntimeAdjustmentSafetyGate` 已实现完整的 cooldown 机制：`cooldownRemaining` 计数器在 `evaluate()` 中递减，`recordApplied()` 中重置
- SafetyGate cooldown 基于"decision intervals"（`evaluate()` 调用次数），与闭环的迭代周期自然同步
- wall-clock cooldown（`cooldownPeriodMs`）在单线程轮询模型中与 decision-interval cooldown 等价：冷却 N 个 interval × 采样间隔 M ms = N×M ms

**修改**:

1. 从 `LoopConfig` 移除 `cooldownPeriodMs` 字段
2. 闭环的调整频率由 `SafetyGateConfig.cooldownDecisionIntervals()` 控制（通过 `LoopConfig` 传递或在 SR 中明确)
3. 在 IR-v0.14-009 冷却期约束中明确：

> **冷却期约束**: 调整冷却由 `RuntimeAdjustmentSafetyGate` 内置 cooldown 机制保证。`AdjustmentLoop` 每次迭代调用 `safetyGate.evaluate()`；SafetyGate 在 cooldown 活跃时返回 `REJECTED`（failureCode=COOLDOWN_ACTIVE），此时闭环记录拒绝并 continue。调整成功应用后，闭环调用 `safetyGate.recordApplied(decision)` 重置 cooldown 计数器。

4. 在 `SafetyGateConfig` 中配置 `cooldownDecisionIntervals`（闭环通过 `LoopConfig` 或直接使用 `SafetyGateConfig.defaults()`）

5. `LoopConfig` 更新后的字段列表（移除 `cooldownPeriodMs`）：

```
samplingIntervalMs, maxIterations, snapshotWindowSize,
oscillationWindowSize, oscillationPatternThreshold,
feedbackCalibrationWindow, emergencyStopThreshold, candidatePolicies
```

---



### F06 [P1 → FIX] 快照获取路径和并发安全

**处置**: FIX。明确快照获取 API 路径，升级并发风险级别。

**修改 A**: 修正 IR-v0.14-002 主循环步骤 3：

从：
```
从 sampler 或 recorder 获取最近 config.snapshotWindowSize 个快照
```

改为：
```
List<ObservedSnapshot> allSnapshots = evidenceRecorder.snapshots(runId);
int fromIndex = Math.max(0, allSnapshots.size() - config.snapshotWindowSize());
List<ObservedSnapshot> recentSnapshots = allSnapshots.subList(fromIndex, allSnapshots.size());
if (recentSnapshots.isEmpty()) continue; // 数据不足，等待下一周期
```

**修改 B**: `AdjustmentLoop` 构造参数需要 `EvidenceRecorder`（从 `LivePressureSampler` 获取或独立注入）。`LivePressureSampler` 内部已持有 `EvidenceRecorder`，可通过 getter 暴露或 loop 持有相同的 recorder 引用。

**修改 C**: 将 IR 风险表中"LivePressureSampler 快照获取的并发风险"从 P2 提升至 P1：
> **P1 风险**: `InMemoryEvidenceRecorder` 内部使用 `ArrayList`（非线程安全）。`LivePressureSampler` 的 `ScheduledExecutorService` 线程写入，`AdjustmentLoop` 的循环线程读取——存在并发修改风险。SR 必须设计线程安全的快照交换机制：使用 `CopyOnWriteArrayList`、显式读写锁、或通过 `LivePressureSampler` 提供 `recentSnapshots(int count)` 方法进行线程安全读取。

**修改 D**: `AdjustmentLoop` 构造参数添加 `EvidenceRecorder` 依赖。

---

### F07 [P1 → FIX] DecisionOrchestrator Scorer/Ranker 字段冗余

**处置**: FIX。移除冗余字段。

修改 IR-v0.14-003 `DecisionOrchestrator` 构造参数：

从：
- `PressureClassifier classifier`
- `PolicyScorer scorer`
- `PolicyRanker ranker`
- `PolicyEvaluator evaluator`

改为：
- `PressureClassifier classifier`
- `PolicyRanker ranker`（内部已持有 PolicyScorer）
- `PolicyEvaluator evaluator`

`decide()` 步骤 4-5 从：
```
4. 对每个 candidate 调用 scorer.score(classification, config) → 列表
5. 调用 ranker.rank(classification, candidates) → 排序后的 List<PolicyScore>
```

改为：
```
4. 调用 ranker.rank(classification, candidates) → 排序后的 List<PolicyScore>
5. 取最高分 → PolicyScore
```

`PolicyRanker.rank()` 内部已调用 `scorer.score()` — 无需 DecisionOrchestrator 重复调用。

**FeedbackCalibrator 访问 scorer 的路径**: `DecisionOrchestrator` 提供 `updateScorer(PolicyScorer newScorer)` 方法（或通过重新创建 `PolicyRanker` 实现），供 `AdjustmentLoop` 在校准权重后更新。

---

### F08 [P1 → FIX] 缺少 reset() 方法

**处置**: FIX。在 IR-v0.14-002 中添加 `reset()` 方法。

修改 `10-ir.md` IR-v0.14-002，在方法列表中添加：

- `reset()` 方法：
  - 状态必须为 STOPPED 或 EMERGENCY_STOPPED → 转换为 IDLE
  - 清除 `AdjustmentHistory`（创建新实例或清空）
  - 重置 `PressureStateMachine`（清除转换历史）
  - 重置 `OscillationDetector`（如果有内部状态）
  - 当前 `LoopSession` 置为 null
  - 如果状态不是 STOPPED 或 EMERGENCY_STOPPED → `IllegalStateException`

同时添加验收条件：
> AC-v0.14-035: reset() 从 STOPPED → IDLE，历史已清除，可重新 start()

---

### F09 [P1 → FIX] runId 来源不明确

**处置**: FIX。明确闭环使用 `LoopSession.sessionId` 作为 `ScaleAdjustmentCommand` 的 `runId`。

修改 IR-v0.14-003 `AdjustmentDecision.toCommand()`：

> `toCommand(ManagedExecutor executor, String runId, Supplier<Instant> clock)`: 从 `policyDecision` 和 executor 当前状态构造 `ScaleAdjustmentCommand`。`runId` 由 `AdjustmentLoop` 从当前 `LoopSession.sessionId` 提供。

在 IR-v0.14-002 主循环步骤 10 前添加：
> 从 `policyDecision` 构造 `ScaleAdjustmentCommand`：`decision.toCommand(executor, session.sessionId(), Instant::now)`

理由：
- 整个闭环会话是一个连续的实验运行，sessionId 语义上等同于 runId
- 不需要通过 `ExperimentCoordinator.createRun()` 创建独立的 run（闭环不是单次 scenario 实验）
- 如果未来需要将闭环的每次迭代映射到 `ExperimentCoordinator` run，可通过 sessionId→runId 映射实现（SR 可添加）

---

### F10 [P2 → DEFER_TO_SR] TransitionLegality 枚举命名空间

**处置**: DEFER_TO_SR。`TransitionLegality` 可以作为 `PressureStateMachine` 的内部枚举或顶级类型。SR 阶段根据包内引用模式确定。IR 无阻塞。

---

### F11 [P2 → DEFER_TO_SR] adjustmentCount vs iterationCount 边界

**处置**: DEFER_TO_SR。SR 阶段精确化语义：

- `iterationCount`: 主循环 `while` 体的执行次数（包括 sampling-only 周期、NO_OP、cooldown 跳过）
- `adjustmentCount`: 实际产生非 NO_OP 决策、通过 SafetyGate ALLOW、成功执行 adapter.apply() 的迭代次数

IR 验收条件中的计数断言使用区间而非精确值。非阻塞。

---

### F12 [P2→P1 → FIX] 快照并发风险级别提升

**处置**: FIX。此 finding 的处理已合并到 F06 的修改 C 中——风险级别从 P2 提升至 P1，并在 IR 风险表中更新描述。

更新 `10-ir.md` 风险表中对应条目：
> ~~P2 风险~~ → **P1 风险**: `InMemoryEvidenceRecorder` 使用 `ArrayList`（非线程安全）。sampler 线程写入、loop 线程读取存在并发修改风险。SR 必须设计线程安全的数据交换。

---

### F13 [P2 → DEFER_TO_SR] 端到端测试 candidate 策略参数

**处置**: DEFER_TO_SR。SR 测试设计阶段指定候选策略的具体参数差异。IR 阶段仅需声明验证意图（"OVERLOAD 状态下选激进策略"）。非阻塞。

---

## Post-Disposition IR 修改清单

以下 IR 文件需同步更新（进入 `13-ir-closure-verification.md` 前完成）：

| 修改项 | 文件 | 涉及 IR 条目 |
|---|---|---|
| PolicyEvaluationInput 构造路径修正 | `10-ir.md` | IR-v0.14-003 步骤 7-8 |
| SafetyGate.evaluate() 签名修正 + ReadinessAssessment 来源 | `10-ir.md` | IR-v0.14-002 步骤 8, IR-v0.14-009 |
| FeedbackCalibrator 签名变更（返回新 scorer） | `10-ir.md` | IR-v0.14-007 |
| SafetyGateDecision outcome 判断修正 | `10-ir.md` | IR-v0.14-002 步骤 8-9 |
| 移除 LoopConfig.cooldownPeriodMs | `10-ir.md` | IR-v0.14-001, IR-v0.14-009 |
| 快照获取路径修正 + 并发风险升级 | `10-ir.md` | IR-v0.14-002 步骤 3, 风险表 |
| DecisionOrchestrator 移除冗余 scorer | `10-ir.md` | IR-v0.14-003 构造参数 |
| 添加 reset() 方法 | `10-ir.md` | IR-v0.14-002 |
| runId = sessionId 明确 | `10-ir.md` | IR-v0.14-003 |
| 验收条件 AC-v0.14-035 添加 (reset) | `10-ir.md` | IR-v0.14-010 |

## 出口状态

- 所有 P0 (5) → FIX，处置方案已明确
- 所有 P1 (4) → FIX，处置方案已明确
- 所有 P2 (4) → 3 DEFER_TO_SR + 1 FIX（风险升级到 P1）
- 进入 `13-ir-closure-verification.md` 的条件：上述 10 项修改已应用到 `10-ir.md`
