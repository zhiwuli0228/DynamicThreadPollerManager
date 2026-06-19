# v0.14.0 SR Closure Verification

## Header

- Document type: SR closure verification
- Version name: `v0.14.0`
- Verified artifact: `docs/04-development/versions/v0.14.0/20-sr.md` (post-disposition)
- Verification date: `2026-06-14`
- Verifier: Independent SR closure verifier
- Disposition reference: `22-sr-review-disposition.md`
- Review reference: `21-sr-review.md`

## Verification Method

1. 对照 `22-sr-review-disposition.md` 中逐项处置方案，检查 `20-sr.md` 是否已应用所有修改
2. 对每条 P0/P1 finding，验证修正后的代码与处置方案一致
3. 检查修正后的 SR 是否存在新引入的不一致

## P0 Verification

### F01 [P0 → FIX] 两个 evidenceRecorder 字段同名

**处置要求**: 重命名为 `loopEvidenceRecorder` + `evidenceRecorder`

**实际修正**:
- ✓ 字段: `private final LoopEvidenceRecorder loopEvidenceRecorder`
- ✓ 字段: `private final EvidenceRecorder evidenceRecorder`
- ✓ 构造参数: `LoopEvidenceRecorder loopEvidenceRecorder, EvidenceRecorder evidenceRecorder`
- ✓ `recordSessionStart/recordSessionEnd/recordIteration` → `loopEvidenceRecorder.*`
- ✓ `evidenceRecorder.snapshots()` 保留（用于快照查询）

**验证结果**: **PASS**

---

### F02 [P0 → FIX] NO_OP 路径 null NPE

**处置要求**: 移除 selectedScore/selectedPolicy 的 requireNonNull

**实际修正**:
- ✓ compact constructor 中删除了 `requireNonNull(selectedScore)` 和 `requireNonNull(selectedPolicy)`
- ✓ 添加注释说明 null 仅用于 NO_OP
- ✓ `isNoOp()` 检查 `policyDecision.action() == HOLD`（不依赖 nullable 字段）

**验证结果**: **PASS**

---

### F03 [P0 → FIX] updateScorer() 设计未完成

**处置要求**: 删除 updateScorer()，AdjustmentLoop 重建 orchestrator

**实际修正**:
- ✓ 删除了 `DecisionOrchestrator.updateScorer()` 方法及其实现注释
- ✓ 添加注释: "DecisionOrchestrator is immutable. Weight updates... handled by AdjustmentLoop creating a new orchestrator instance"
- ✓ `orchestrator` 字段改为 `private volatile DecisionOrchestrator orchestrator`
- ✓ 添加了 `classifier`, `evaluator`, `classifierConfig` 字段用于重建
- ✓ 校准触发代码中正确重建 orchestrator

**验证结果**: **PASS**

---

## P1 Verification

### F04 [P1 → FIX] endTime 类型安全性

**处置要求**: endTime → Optional\<Instant\>

**实际修正**:
- ✓ record 字段: `Optional<Instant> endTime`
- ✓ `started()`: `Optional.empty()`
- ✓ `ended()`: `Optional.of(Instant.now())`
- ✓ `finalizeSession`: `currentSession.endTime().isEmpty()`

**验证结果**: **PASS**

---

### F05+F06 [P1 → FIX] Calibrator 触发

**处置要求**: 填充 calibrator 触发代码 + 添加 FeedbackCalibrator 依赖

**实际修正**:
- ✓ 字段: `private final FeedbackCalibrator calibrator`
- ✓ 构造参数: `FeedbackCalibrator calibrator`
- ✓ 步骤 15 完整代码: `calibrator.calibrate(history, currentScorer, window)` + 重建 orchestrator

**验证结果**: **PASS**

---

### F07 [P1 → FIX] afterClassification 延迟记录

**处置要求**: 使用 previousClassification 进行正确的 before/after 记录

**实际修正**:
- ✓ before = `previousClassification`（第一次为 null 时 fallback 到 afterClass）
- ✓ after = `decision.classification()`
- ✓ `previousClassification = afterClass`（为下次迭代准备）
- ✓ 删除了旧的 placeholder 注释

**验证结果**: **PASS**

---

### F08 [P1 → FIX] Instant.now() 可重现性

**处置要求**: 使用 lastSnapshot.timestamp() 替代 Instant.now()

**实际修正**:
- ✓ `PolicyEvaluationInput` 构造: `lastSnapshot.timestamp()` 替代 `Instant.now()`
- ✓ 添加注释: "Use snapshot timestamp for reproducibility (not wall-clock)"

**验证结果**: **PASS**

---

## 残余 P2 检查

| ID | 处置 | 验证 |
|---|---|---|
| F09 | DEFER_TO_IMPLEMENTATION | 非阻塞 — iteration 计数器自然实现 |
| F10 | DEFER_TO_IMPLEMENTATION | 非阻塞 — subList 副本标准实践 |

---

## 一致性检查

- **AdjustmentLoop 构造参数** 与字段声明一致 ✓（11 个字段全部匹配）
- **loopEvidenceRecorder vs evidenceRecorder** 使用区分正确 ✓
- **AdjustmentDecision nullable 字段** 与 NO_OP 路径一致 ✓
- **LoopSession.endTime Optional** 与 started()/ended() 一致 ✓
- **DecisionOrchestrator 不可变** 与 calibrator 重建模式一致 ✓
- **afterClassification 逻辑** 正确（first iter: before=after, subsequent: before=previous）✓
- **PolicyEvaluationInput 时间戳** 来自快照数据 ✓

## Closure Summary

| 指标 | 值 |
|---|---|
| P0 findings | 3 |
| P0 → FIX, verified | 3 ✓ |
| P1 findings | 5 |
| P1 → FIX, verified | 5 ✓ |
| P2 findings | 2 |
| P2 → DEFER_TO_IMPLEMENTATION | 2 ✓ |
| 新引入的不一致 | 0 |
| 未处置的阻塞项 | 0 |

## Gate Status: PASS

所有 P0/P1 findings (8/8) 已修正并在 `20-sr.md` 中验证通过。SR 功能设计基线已准备就绪，可进入 OpenSpec change decomposition。

## Machine-Actionable Closeout State

- **Gate status**: PASS
- **Worktree status**: n/a（文档工作）
- **Blocking reason**: none
- **Agent next action**: 更新 `docs/00-project/current-state.md` 授权进入 change decomposition
- **User action required before next agent action**: no
