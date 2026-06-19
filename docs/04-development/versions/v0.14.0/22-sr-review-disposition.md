# v0.14.0 SR Review Disposition

## Header

- Document type: SR review disposition
- Version name: `v0.14.0`
- Reviewed artifact: `docs/04-development/versions/v0.14.0/21-sr-review.md`
- Disposition date: `2026-06-14`
- Disposition basis: `docs/02-harness/managed-change-standard.md` §3

## Disposition Summary

| ID | 级别 | 处置 | 理由 |
|---|---|---|---|
| F01 | P0 | **FIX** | 重命名 LoopEvidenceRecorder 字段 |
| F02 | P0 | **FIX** | 移除 selectedScore/selectedPolicy 的 requireNonNull |
| F03 | P0 | **FIX** | 删除 updateScorer()，AdjustmentLoop 重建 orchestrator |
| F04 | P1 | **FIX** | endTime → Optional<Instant> |
| F05 | P1 | **FIX** | 填充 calibrator 触发代码 |
| F06 | P1 | **FIX** | 添加 FeedbackCalibrator 构造参数 |
| F07 | P1 | **FIX** | 明确 afterClassification 延迟记录机制 |
| F08 | P1 | **FIX** | lastSnapshot.timestamp() 替代 Instant.now() |
| F09 | P2 | **DEFER_TO_IMPLEMENTATION** | iteration 计数器自然实现 |
| F10 | P2 | **DEFER_TO_IMPLEMENTATION** | subList 副本是标准实践 |

## Detailed Disposition

### F01 [P0 → FIX] 两个 evidenceRecorder 字段同名

**处置**: FIX。重命名 `LoopEvidenceRecorder` 字段为 `loopEvidenceRecorder`。

修改 §4.10 AdjustmentLoop:
- 字段: `private final LoopEvidenceRecorder loopEvidenceRecorder;`
- 字段: `private final EvidenceRecorder evidenceRecorder;`（保留，用于 snapshot 查询）
- 构造参数: `LoopEvidenceRecorder loopEvidenceRecorder, EvidenceRecorder evidenceRecorder`
- 所有 `evidenceRecorder.recordXxx()` 调用 → `loopEvidenceRecorder.recordXxx()`

---

### F02 [P0 → FIX] AdjustmentDecision NO_OP 路径 null NPE

**处置**: FIX。从 compact constructor 移除 `selectedScore` 和 `selectedPolicy` 的 `requireNonNull`。

修改 §4.4 AdjustmentDecision compact constructor:
```java
// REMOVE:
Objects.requireNonNull(selectedScore, "selectedScore must not be null");
Objects.requireNonNull(selectedPolicy, "selectedPolicy must not be null");

// REPLACE WITH comment:
// selectedScore and selectedPolicy are null only for NO_OP decisions
// (isNoOp() checks policyDecision.action() == HOLD, not these fields)
```

`isNoOp()` 不依赖 `selectedScore`/`selectedPolicy`，安全性不受影响。

---

### F03 [P0 → FIX] updateScorer() 设计未完成

**处置**: FIX。删除 `DecisionOrchestrator.updateScorer()` 方法。`AdjustmentLoop` 在校准后重建整个 orchestrator。

修改 §4.9 DecisionOrchestrator:
- 删除 `updateScorer()` 方法
- `DecisionOrchestrator` 保持完全不可变
- 添加注释: "不可变。权重更新通过 AdjustmentLoop 重建 orchestrator 实例实现"

修改 §4.10 AdjustmentLoop:
- orchestrator 字段改为 non-final（`private volatile DecisionOrchestrator orchestrator`）
- 在 calibrator 触发后: `this.orchestrator = new DecisionOrchestrator(classifier, new PolicyRanker(newScorer), evaluator, classifierConfig)`

---

### F04 [P1 → FIX] endTime 类型安全性

**处置**: FIX。`endTime` 改为 `Optional<Instant>`。

修改 §4.3 LoopSession:
```java
Optional<Instant> endTime,  // Optional.empty() if running
```
- `started()`: `Optional.empty()`
- `ended()`: `Optional.of(Instant.now())`
- compact constructor: 无需 null 检查（Optional 自身保证 non-null）

---

### F05 [P1 → FIX] FeedbackCalibrator 触发代码为空

**处置**: FIX。填充完整调用代码。

修改 §4.10 AdjustmentLoop.runLoop() 步骤 15:（与 F06 合并处理）

---

### F06 [P1 → FIX] 缺少 FeedbackCalibrator 依赖

**处置**: FIX。添加 `FeedbackCalibrator` 到构造参数并完成触发代码。

修改 §4.10 AdjustmentLoop:
- 添加字段: `private final FeedbackCalibrator calibrator;`
- 添加构造参数: `FeedbackCalibrator calibrator`
- 步骤 15 完整代码:
```java
if (history.totalAdjustmentCount() > 0
        && history.totalAdjustmentCount() % config.feedbackCalibrationWindow() == 0) {
    ThresholdPolicyScorer currentScorer = getScorerFromOrchestrator();
    ThresholdPolicyScorer newScorer = calibrator.calibrate(
            history, currentScorer, config.feedbackCalibrationWindow());
    if (newScorer != currentScorer) {
        this.orchestrator = new DecisionOrchestrator(
                classifier, new PolicyRanker(newScorer), evaluator, classifierConfig);
    }
}
```
- `getScorerFromOrchestrator()` 通过 `orchestrator` 内部的 `PolicyRanker` 获取 scorer（或直接在 `AdjustmentLoop` 中持有 scorer 引用）

---

### F07 [P1 → FIX] afterClassification 延迟记录

**处置**: FIX。使用 `previousClassification` 字段进行正确的 before/after 记录。

修改 §4.10 AdjustmentLoop.runLoop() 步骤 12:
```java
// Step 12: record in history with correct before/after
PressureClassification beforeClass = previousClassification;
PressureClassification afterClass = decision.classification();
history.record(decision, result, beforeClass != null ? beforeClass : afterClass, afterClass);
previousClassification = afterClass;
```
逻辑：第一次迭代 before=after（同一分类），后续迭代 before=上次分类, after=本次分类。`isImprovement(before, after)` 判断状态是否改善。

---

### F08 [P1 → FIX] Instant.now() 损害可重现性

**处置**: FIX。使用 `lastSnapshot.timestamp()` 替代 `Instant.now()`。

修改 §4.9 DecisionOrchestrator.decide() 步骤 5:
```java
// BEFORE:
PolicyEvaluationInput input = new PolicyEvaluationInput(runId, lastSnapshot, Instant.now());

// AFTER:
PolicyEvaluationInput input = new PolicyEvaluationInput(runId, lastSnapshot, lastSnapshot.timestamp());
```
整个决策链的时间戳现在完全可重现（仅依赖快照数据）。

---

### F09 [P2 → DEFER] iteration 计数器跨线程访问

**处置**: DEFER_TO_IMPLEMENTATION。实现阶段自然使用 `AtomicInteger` 或 `volatile int`。

---

### F10 [P2 → DEFER] subList 副本

**处置**: DEFER_TO_IMPLEMENTATION。实现阶段使用 `new ArrayList<>(subList)` 是标准实践。

---

## Post-Disposition SR 修改清单

| 修改项 | SR 位置 | 涉及 |
|---|---|---|
| 重命名 loopEvidenceRecorder 字段 | §4.10 字段+构造器+调用点 | F01 |
| 移除 selectedScore/selectedPolicy requireNonNull | §4.4 compact constructor | F02 |
| 删除 updateScorer()，标记不可变 | §4.9 | F03 |
| orchestrator 字段 non-final volatile | §4.10 字段 | F03 |
| endTime → Optional\<Instant\> | §4.3 | F04 |
| 填充 calibrator 触发代码 | §4.10 步骤 15 | F05+F06 |
| 添加 FeedbackCalibrator 依赖 | §4.10 字段+构造器 | F06 |
| 修正 afterClassification 记录逻辑 | §4.10 步骤 12 | F07 |
| PolicyEvaluationInput 使用 snapshot timestamp | §4.9 步骤 5 | F08 |

## 出口状态

- P0 (3) → FIX
- P1 (5) → FIX
- P2 (2) → DEFER_TO_IMPLEMENTATION
- 进入 `23-sr-closure-verification.md` 条件：上述 8 项修改已应用到 `20-sr.md`
