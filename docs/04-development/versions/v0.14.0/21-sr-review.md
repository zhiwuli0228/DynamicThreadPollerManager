# v0.14.0 SR Review

## Header

- Document type: SR independent review
- Version name: `v0.14.0`
- Review date: `2026-06-14`
- Reviewer: Independent SR review agent
- Status: `READY_FOR_DISPOSITION`
- Source SR: `20-sr.md` (v0.14.0 SR 功能设计)
- Review basis: `docs/02-harness/managed-change-standard.md` §3（SR 伪代码强制验证规则）

## Review Method

1. 对照 `managed-change-standard.md` §3 的 SR 伪代码验证规则：
   - 随机抽取 3 个 API 调用点，读取实际源码验证签名匹配
   - 序列化方向检查（本 SR 无序列化代码 — N/A）
   - Record 反序列化 null 兼容性检查（本 SR 新 record 无 fromMap — N/A）
   - 跨类型转换边界值检查（本 SR 无跨类型转换 — N/A）
2. 检查组件间依赖关系的类型一致性
3. 检查并发、边界、失败语义的完整性
4. 对照 IR 验证需求覆盖率

### Extract 1: AdjustmentDecision NO_OP path

`20-sr.md` §4.9 `DecisionOrchestrator.createNoOpDecision()`:
```java
return new AdjustmentDecision(
    new PressureClassification(...),
    null, // selectedScore — null for NO_OP
    null, // selectedPolicy
    new PolicyDecision(...),
    reason, Instant.now());
```

对照 `20-sr.md` §4.4 `AdjustmentDecision` compact constructor:
```java
Objects.requireNonNull(selectedScore, "selectedScore must not be null");
Objects.requireNonNull(selectedPolicy, "selectedPolicy must not be null");
```

→ **P0: null 传入 requireNonNull — NPE at construction**

### Extract 2: AdjustmentLoop duplicate field

`20-sr.md` §4.10 line 844-846:
```java
private final LoopEvidenceRecorder evidenceRecorder;
private final EvidenceRecorder evidenceRecorder;  // for snapshot queries
```

→ **P0: 两个字段同名 evidenceRecorder — 编译错误**

### Extract 3: PolicyDecision NO_OP constructor

`20-sr.md` §4.9:
```java
new PolicyDecision(runId, "no-op", Instant.now(), PolicyAction.HOLD,
    GateStatus.HOLD, 0, 0, reason)
```

对照实际源码 `PolicyDecision.java:30-61`:
```java
public PolicyDecision(String runId, String policyId, Instant timestamp,
                      PolicyAction action, GateStatus gateStatus,
                      int currentPoolSize, int proposedPoolSize, String reason)
```

→ ✓ 签名匹配（8 参数顺序正确）

---

## Findings

### P0 — 阻断性

#### F01: AdjustmentLoop 两个 evidenceRecorder 字段同名

**位置**: §4.10 AdjustmentLoop 字段声明 + 构造函数

**问题**: 
```java
private final LoopEvidenceRecorder evidenceRecorder;   // line 844
private final EvidenceRecorder evidenceRecorder;        // line 846 — SAME NAME
```
以及构造器中：
```java
this.evidenceRecorder = Objects.requireNonNull(evidenceRecorder, ...); // assigns LoopEvidenceRecorder
this.evidenceRecorder = Objects.requireNonNull(evidenceRecorder, ...); // OVERWRITES with EvidenceRecorder
```
第二个赋值覆盖第一个，`LoopEvidenceRecorder` 字段永远为 null。

**影响**: 编译错误。`LoopEvidenceRecorder.recordIteration()` 等调用全部落在 `EvidenceRecorder` 上（类型不匹配 — `EvidenceRecorder` 没有 `recordIteration` 方法）。

**建议**: 重命名 `LoopEvidenceRecorder` 字段为 `loopEvidenceRecorder` 或 `iterationRecorder`。保留 `evidenceRecorder` 用于 snapshot 查询。或者将两者合并：让 `LoopEvidenceRecorder` 扩展 `EvidenceRecorder` 接口。最简单的修复：重命名区分两个字段。

**严重级别**: P0（编译错误）

---

#### F02: AdjustmentDecision NO_OP 路径 null 违反 compact constructor

**位置**: §4.4 AdjustmentDecision compact constructor + §4.9 createNoOpDecision()

**问题**: `AdjustmentDecision` compact constructor 对 `selectedScore` 和 `selectedPolicy` 调用 `requireNonNull`。但 `createNoOpDecision()` 传入 `null` 给这两个字段。

```java
// §4.4 compact constructor:
Objects.requireNonNull(selectedScore, "selectedScore must not be null");
Objects.requireNonNull(selectedPolicy, "selectedPolicy must not be null");

// §4.9 createNoOpDecision():
null, // selectedScore
null, // selectedPolicy
```

NO_OP 决策在语义上不需要 `selectedScore` 和 `selectedPolicy`（没有策略被选中），但 compact constructor 阻止了这种表达。

**建议**: 两个方案：
- A: 从 compact constructor 中移除 `selectedScore` 和 `selectedPolicy` 的 `requireNonNull`。`isNoOp()` 通过 `policyDecision.action() == HOLD` 判断，不依赖这两个字段。注释说明 `null` 仅当 NO_OP 时
- B: 使用 sentinel 值 — 创建 `PolicyScore.NO_OP` 和 `ThresholdPolicyConfig` 的 sentinel 实例。但 sentinel 模式对 record 类型不自然

推荐 A — 更简洁，语义准确。

**严重级别**: P0（构造时 NPE — NO_OP 路径不可达）

---

#### F03: DecisionOrchestrator.updateScorer() 设计未完成

**位置**: §4.9 DecisionOrchestrator.updateScorer()

**问题**: 方法体注释为：
```java
// Design choice: SR defers to implementation — either make ranker field
// non-final with synchronized update, or have AdjustmentLoop recreate orchestrator
```

SR 阶段不应将设计决策推迟到 implementation。`managed-change-standard.md` §3 要求 SR "对弱实现 agent 足够明确的任务切分"。未解决的设计选择会导致实现阶段的不确定性和潜在的不一致。

**建议**: 在 SR 中做出明确设计决策。推荐方案：`AdjustmentLoop` 在校准权重后直接调用 `new DecisionOrchestrator(classifier, new PolicyRanker(newScorer), evaluator, classifierConfig)` 替换整个 orchestrator。`DecisionOrchestrator` 保持不可变。`AdjustmentLoop` 的 orchestrator 字段改为 non-final volatile。

**严重级别**: P0（SR 出口条件不满足 — 设计不完整）

---

### P1 — 关键

#### F04: LoopSession.endTime 类型应该是 Optional<Instant> 或显式 nullable

**位置**: §4.3 LoopSession

**问题**: `endTime` 字段声明为 `Instant`，但 `started()` 静态工厂传入 `null`（表示"会话仍在运行"）。compact constructor 不对 `endTime` 做 null 检查。getter `endTime()` 返回 `Instant`（可能为 null），调用方需要 null 检查但没有类型提示。

**建议**: 将 `endTime` 类型改为 `Optional<Instant>`，或者显式在 Javadoc 标注 `@nullable`。`started()` 传入 `Optional.empty()`。更推荐 `Optional<Instant>` — 它强制调用方处理"可能未结束"的情况。

**严重级别**: P1（类型安全性差 — 可能导致 NPE）

---

#### F05: FeedbackCalibrator 触发时机设计不完整

**位置**: §4.10 AdjustmentLoop.runLoop() 步骤 15

**问题**: 
```java
// Step 15: feedback calibration
if (history.totalAdjustmentCount() > 0
        && history.totalAdjustmentCount() % config.feedbackCalibrationWindow() == 0) {
    // calibration trigger — handled by caller or via callback
}
```

方法体为空。`FeedbackCalibrator.calibrate()` 未被调用。设计意图明确（条件触发），但缺少调用代码。

**建议**: 在 if 块中补充完整调用：
```java
ThresholdPolicyScorer currentScorer = ...; // need access to current scorer
ThresholdPolicyScorer newScorer = calibrator.calibrate(history, currentScorer, config.feedbackCalibrationWindow());
if (newScorer != currentScorer) {
    orchestrator = new DecisionOrchestrator(classifier, new PolicyRanker(newScorer), evaluator, classifierConfig);
}
```
`AdjustmentLoop` 需要持有 `FeedbackCalibrator` 引用（当前构造参数中未包含 — 另一个缺口）。

**严重级别**: P1（未实现的设计意图）

---

#### F06: AdjustmentLoop 缺少 FeedbackCalibrator 依赖

**位置**: §4.10 AdjustmentLoop 构造参数

**问题**: 构造参数列表中没有 `FeedbackCalibrator`，但主循环步骤 15 需要调用它。IR 明确要求 `FeedbackCalibrator` 是闭环的一部分。

**建议**: 在 `AdjustmentLoop` 构造参数中添加 `FeedbackCalibrator`。

**严重级别**: P1（组件缺失）

---

#### F07: afterClassification 获取机制不明确

**位置**: §4.10 AdjustmentLoop.runLoop() 步骤 12

**问题**: SR 承认 `afterClassification` 只能在下次迭代获得：
```
// Note: true afterClassification would come from NEXT iteration's classify()
// Placeholder: use same classification as "before" for now.
```

这导致 `AdjustmentHistory.successfulAdjustmentCount()` 和 `FeedbackCalibrator.calibrate()` 使用的 `beforeClassification`/`afterClassification` 数据不正确。

**建议**: 明确延迟记录机制：
1. 每次迭代在 classify 之后（步骤 3）、record 之前，检查是否有"上一条未完成的 HistoryEntry"
2. 如果有，用当前 classification 作为其 `afterClassification`，更新该 entry
3. 当前迭代的 HistoryEntry 先以占位 `afterClassification = beforeClassification` 记录，标记为"待更新"
4. 或者简化：维护 `previousClassification` 字段。每次 record 时，`beforeClassification = previousClassification`, `afterClassification = currentClassification`。然后 `previousClassification = currentClassification`

推荐方案 4 — 最简单。

**严重级别**: P1（历史数据不正确影响 calibrator 精度）

---

#### F08: AdjustmentLoop 主循环中的 PolicyEvaluationInput 使用 Instant.now()

**位置**: §4.9 DecisionOrchestrator.decide() 步骤 5

**问题**: 
```java
PolicyEvaluationInput input = new PolicyEvaluationInput(
    runId, lastSnapshot, Instant.now());
```

`PolicyEvaluator` 接口要求实现不调用 wall-clock API（"MUST derive any timestamp used in the result from the input"）。但 `AdjustmentLoop` 在构造 `PolicyEvaluationInput` 时使用了 `Instant.now()` 作为 `evaluatedAt`。

虽然 `ThresholdPolicyEvaluator` 当前不使用 `evaluatedAt`，但违反了接口契约的精神（`PolicyEvaluator` Javadoc: "Implementations MUST derive any timestamp used in the result from the input, MUST NOT call wall-clock APIs"）。这里的 wall-clock 调用在 evaluator 外部（构造 input 时），技术上不违反接口契约，但破坏了时间戳的可重现性。

**建议**: 使用 `lastSnapshot.timestamp()` 代替 `Instant.now()`，使整个决策链的时间戳可重现。或者使 `evaluatedAt` 参数可注入（通过 `Supplier<Instant>`）。

**严重级别**: P1（可重现性受损 — 测试无法精确验证 decision 时间戳）

---

### P2 — 次要

#### F09: finalizeSession 的 iteration 计数器无法从 stop()/emergencyStop() 线程访问

**位置**: §4.10 AdjustmentLoop.finalizeSession() + runLoop()

**问题**: `finalizeSession` 中 `int iterations = 0; // track iteration counter`。`iteration` 是 `runLoop()` 的局部变量，`finalizeSession` 通过 `stop()`/`emergencyStop()` 从另一个线程调用时无法访问。

**建议**: 将 `iteration` 提升为 `AdjustmentLoop` 的实例字段（`private volatile int iterationCount`），在 runLoop 中递增。

**严重级别**: P2（实现细节 — 实现阶段自然解决）

---

#### F10: OscillationDetector.detectPolicySwitching 对子列表操作脆弱

**位置**: §4.8 OscillationDetector

**问题**:
```java
if (recent.size() >= 4 && detectPolicySwitching(recent.subList(0, recent.size() - 1),
        recent.get(recent.size() - 1).decision()))
```
`subList` 的语义依赖于底层列表是否被修改。实现阶段如果用 `ArrayList.subList()` 后修改原列表 → `ConcurrentModificationException`。

**建议**: SR 伪代码可接受。实现阶段使用 `new ArrayList<>(recent.subList(...))` 创建独立副本。

**严重级别**: P2（实现注意事项）

---

## 伪代码 API 验证（随机 3 点抽查）

### Check 1: PolicyEvaluationInput 构造器

**SR**: `new PolicyEvaluationInput(runId, lastSnapshot, Instant.now())`
**Source**: `PolicyEvaluationInput(String runId, PressureSnapshot snapshot, Instant evaluatedAt)` ✓

### Check 2: SafetyGateDecision.outcome()

**SR**: `gateDecision.outcome() == SafetyGateDecision.Outcome.REJECTED`
**Source**: `SafetyGateDecision.outcome()` → `Outcome`, enum `Outcome { ALLOW, REJECTED, NO_OP }` ✓

### Check 3: ReadinessAssessment 构造器

**SR**: `new ReadinessAssessment(ReadinessStatus.READY, List.of(), List.of(), List.of(), List.of(), "runtime-loop", List.of(sessionId))`
**Source**: `ReadinessAssessment(ReadinessStatus, List<ScenarioProfile>, List<ScenarioProfile>, List<String>, List<String>, String, List<String>)` ✓

**验证结果**: 3/3 API 签名一致 ✓

---

## 并发设计验证

| 检查项 | 结果 |
|---|---|
| `AdjustmentHistory.entries` 使用 `CopyOnWriteArrayList` | ✓ 线程安全 |
| `PressureStateMachine.transitions` 单线程 | ✓ loop 线程独占 |
| `AdjustmentLoop.state` volatile | 未标注 volatile — P2 |
| 快照数据 EvidenceRecorder 并发 | ✓ 风险已记录（IR F06/F12 处置） |

---

## IR 需求覆盖率

| IR 条目 | SR 组件 | 覆盖 |
|---|---|---|
| IR-v0.14-001 LoopState+LoopConfig | §4.1, §4.2 | ✓ |
| IR-v0.14-002 AdjustmentLoop | §4.10 | ✓ |
| IR-v0.14-003 DecisionOrchestrator | §4.9 | ✓ |
| IR-v0.14-004 PressureStateMachine | §4.5, §4.6 | ✓ |
| IR-v0.14-005 OscillationDetector | §4.8 | ✓ |
| IR-v0.14-006 AdjustmentHistory | §4.7 | ✓ |
| IR-v0.14-007 FeedbackCalibrator | §4.11 | ✓ |
| IR-v0.14-008 LoopSession+LoopEvidenceRecorder | §4.3, §4.13 | ✓ |
| IR-v0.14-009 安全约束 | §6 | ✓ |
| IR-v0.14-010 端到端验证 | §7 测试映射 | ✓ |

**覆盖率**: 10/10 IR 条目 ✓

---

## Findings Summary

| ID | 严重级别 | 位置 | 简要描述 |
|---|---|---|---|
| F01 | P0 | §4.10 AdjustmentLoop 字段 | 两个 `evidenceRecorder` 字段同名（LoopEvidenceRecorder vs EvidenceRecorder） |
| F02 | P0 | §4.4+§4.9 AdjustmentDecision | NO_OP 路径 null 传入 requireNonNull — NPE |
| F03 | P0 | §4.9 updateScorer() | 设计未完成 — "defers to implementation" |
| F04 | P1 | §4.3 LoopSession | endTime 应为 Optional<Instant> |
| F05 | P1 | §4.10 runLoop() step 15 | FeedbackCalibrator 触发代码为空 |
| F06 | P1 | §4.10 构造参数 | 缺少 FeedbackCalibrator 依赖 |
| F07 | P1 | §4.10 runLoop() step 12 | afterClassification 延迟记录机制不明确 |
| F08 | P1 | §4.9 decide() step 5 | PolicyEvaluationInput 使用 Instant.now() 损害可重现性 |
| F09 | P2 | §4.10 finalizeSession | iteration 计数器跨线程访问 |
| F10 | P2 | §4.8 detectPolicySwitching | subList 副本建议 |

**总结**: 3 P0, 5 P1, 2 P2。所有 P0 均为 SR 内部设计缺陷（编译错误、NPE、未完成设计）。P1 为设计细节缺失。P0/P1 必须在 SR disposition 中逐项处置并闭环验证后方可进入 OpenSpec change decomposition。
