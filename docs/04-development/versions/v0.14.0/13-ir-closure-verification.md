# v0.14.0 IR Closure Verification

## Header

- Document type: IR closure verification
- Version name: `v0.14.0`
- Verified artifact: `docs/04-development/versions/v0.14.0/10-ir.md` (post-disposition)
- Verification date: `2026-06-14`
- Verifier: Independent IR closure verifier
- Disposition reference: `12-ir-review-disposition.md`
- Review reference: `11-ir-review.md`

## Verification Method

1. 对照 `12-ir-review-disposition.md` 中逐项处置方案，检查 `10-ir.md` 是否已应用所有修改
2. 对每条 P0/P1 finding，验证修正后的文本与处置方案一致
3. 检查修正后的 IR 是否存在新引入的不一致

## P0 Verification

### F01 [P0 → FIX] PolicyEvaluationInput 构造路径

**处置要求**: IR-v0.14-003 decide() 步骤从 `snapshots.get(snapshots.size()-1).snapshot()` 获取 PressureSnapshot，传入 `PolicyEvaluationInput`

**实际修正**:
- ✓ `DecisionOrchestrator.decide()` 步骤 6：`snapshots.get(snapshots.size()-1).snapshot()` 获取 PressureSnapshot
- ✓ 步骤 7：`new PolicyEvaluationInput(runId, lastSnapshot, Instant.now())` 使用正确构造器签名
- ✓ 删除了原"注：PolicyEvaluationInput 构造需要确认构造器签名"
- ✓ 添加了 `PolicyEvaluationInput` 构造路径已验证的说明

**验证结果**: **PASS** — 构造路径与 `PolicyEvaluationInput(String, PressureSnapshot, Instant)` 签名一致

---

### F02 [P0 → FIX] SafetyGate.evaluate() 签名为 3 参数

**处置要求**: 修正为 3 参数签名，明确 ReadinessAssessment 和 ExecutorStateSnapshot 来源

**实际修正**:
- ✓ 主循环步骤 8 使用 3 参数：`safetyGate.evaluate(command, executorState, loopReadiness)`
- ✓ `executorState` 来源：`adapter.currentState()`
- ✓ `loopReadiness` 来源：`start()` 中构造运行时 `ReadinessAssessment`（status=READY, configLabel="runtime-loop", inputRunIds=[sessionId]）
- ✓ IR-v0.14-009 SafetyGate 前置描述已更新为完整 3 参数签名

**验证结果**: **PASS** — 签名和参数来源与 `RuntimeAdjustmentSafetyGate.evaluate(ScaleAdjustmentCommand, ExecutorStateSnapshot, ReadinessAssessment)` 一致

---

### F03 [P0 → FIX] ThresholdPolicyScorer 权重不可读写

**处置要求**: calibrate() 返回新 scorer 实例，添加权重 getter

**实际修正**:
- ✓ `calibrate()` 签名变更：`ThresholdPolicyScorer calibrate(AdjustmentHistory, ThresholdPolicyScorer)` — 返回新实例
- ✓ 通过 `currentScorer.wResponsiveness()` 等 getter 读取当前权重（需求中已标注）
- ✓ 返回语句：`new ThresholdPolicyScorer(newWR, newWS, newWSt, newWE)`
- ✓ 主循环步骤 15：`ThresholdPolicyScorer newScorer = calibrator.calibrate(history, currentScorer); orchestrator.updateScorer(newScorer);`
- ✓ `ThresholdPolicyScorer` 权重 getter 需求已添加

**验证结果**: **PASS** — 不可变模式，不修改传入 scorer

---

### F04 [P0 → FIX] SafetyGateDecision outcome 判断

**处置要求**: 使用 outcome() 枚举判断，区分 REJECTED/NO_OP/ALLOW

**实际修正**:
- ✓ 主循环步骤 9 分别处理三种 outcome：`REJECTED`、`NO_OP`、`ALLOW`
- ✓ 语法使用 `gateDecision.outcome()` 而非 `rejected()` boolean
- ✓ AC-v0.14-031 已同步更新

**验证结果**: **PASS** — 枚举判断与 `SafetyGateDecision.Outcome` 一致

---

### F05 [P0 → FIX] 双重冷却机制

**处置要求**: 移除 `LoopConfig.cooldownPeriodMs`，统一使用 SafetyGate cooldown

**实际修正**:
- ✓ `LoopConfig` 字段列表已移除 `cooldownPeriodMs`
- ✓ 范围内列表已同步移除
- ✓ 添加注：调整冷却由 SafetyGate 内置机制保证
- ✓ IR-v0.14-009 冷却期约束已重写：描述 SafetyGate cooldown 流程（evaluate 返回 REJECTED → continue → recordApplied 重置）
- ✓ 主循环步骤 11 添加 `safetyGate.recordApplied(gateDecision)`

**验证结果**: **PASS** — 无双重冷却，职责清晰

---

## P1 Verification

### F06 [P1 → FIX] 快照获取路径

**处置要求**: 通过 `evidenceRecorder.snapshots(runId)` 获取，处理并发安全

**实际修正**:
- ✓ 主循环步骤 2：`evidenceRecorder.snapshots(session.sessionId())` + subList
- ✓ 空数据检查：`if (recent.isEmpty()) continue`
- ✓ `AdjustmentLoop` 构造参数移除 `LivePressureSampler`，改为 `EvidenceRecorder`
- ✓ 风险表并发风险已提升至 P1

**验证结果**: **PASS** — API 路径与 `EvidenceRecorder.snapshots(String runId)` 一致

---

### F07 [P1 → FIX] DecisionOrchestrator 冗余字段

**处置要求**: 移除独立 PolicyScorer 字段，仅保留 PolicyRanker

**实际修正**:
- ✓ 构造参数：`PressureClassifier`, `PolicyRanker`, `PolicyEvaluator`, `ClassifierConfig`
- ✓ 移除独立的 `PolicyScorer` 字段
- ✓ decide() 步骤 4 直接调用 `ranker.rank(classification, candidates)`
- ✓ 添加 `updateScorer()` 方法说明（通过重新创建 PolicyRanker）

**验证结果**: **PASS** — 无冗余

---

### F08 [P1 → FIX] 缺少 reset() 方法

**处置要求**: 添加 reset() 方法

**实际修正**:
- ✓ `reset()` 方法已添加：状态必须 STOPPED/EMERGENCY_STOPPED → IDLE，清除历史/状态机/session
- ✓ AC-v0.14-035 已添加
- ✓ 主循环伪代码中不受影响

**验证结果**: **PASS** — EMERGENCY_STOPPED/STOPPED → IDLE 可达

---

### F09 [P1 → FIX] runId 来源

**处置要求**: runId = sessionId

**实际修正**:
- ✓ `toCommand(ManagedExecutor executor, String runId, Supplier<Instant> clock)` — runId 由调用方提供
- ✓ 主循环步骤 7：`decision.toCommand(executor, session.sessionId(), Instant::now)`
- ✓ `AdjustmentDecision.toCommand()` 文档中明确 runId 由 `LoopSession.sessionId` 提供

**验证结果**: **PASS** — runId 来源明确

---

### F12 [P2→P1 → FIX] 并发风险升级

**处置要求**: 风险级别从 P2 提升至 P1

**实际修正**:
- ✓ 风险表条目已更新为 P1
- ✓ 详细描述了 `ArrayList` 非线程安全问题
- ✓ 明确了 `ConcurrentModificationException` 风险

**验证结果**: **PASS** — 风险级别正确

---

## 残余 P2 检查

| ID | 级别 | 处置 | 验证 |
|---|---|---|---|
| F10 | P2 | DEFER_TO_SR | 非阻塞 — `TransitionLegality` 命名空间在 SR 确定 |
| F11 | P2 | DEFER_TO_SR | 非阻塞 — iteration/adjustment 计数语义在 SR 精确化 |
| F13 | P2 | DEFER_TO_SR | 非阻塞 — candidate 策略参数在 SR 测试设计中指定 |

---

## 一致性检查

- **范围内列表** 与 LoopConfig 字段列表一致（✓ 均移除 cooldownPeriodMs）
- **主循环伪代码与 SafetyGate API** 一致（✓ 3 参数签名 + outcome 枚举 + recordApplied）
- **DecisionOrchestrator 与 PolicyRanker** 关系一致（✓ 不重复持有 scorer）
- **FeedbackCalibrator 与 ThresholdPolicyScorer** 不可变性一致（✓ 返回新实例 + getter）
- **验收条件 AC-v0.14-035** 与 reset() 方法一致（✓ 已添加）
- **风险表** 并发风险级别与 F06 修改一致（✓ P1）

---

## Closure Summary

| 指标 | 值 |
|---|---|
| P0 findings | 5 |
| P0 → FIX, verified | 5 ✓ |
| P1 findings | 4 |
| P1 → FIX, verified | 4 ✓ |
| P2 findings | 4 |
| P2 → DEFER_TO_SR | 3 ✓ |
| P2 → FIX (risk upgrade) | 1 ✓ |
| 新引入的不一致 | 0 |
| 未处置的阻塞项 | 0 |

## Gate Status: PASS

所有 P0/P1 findings (9/9) 已修正并在 `10-ir.md` 中验证通过。3 个 P2 项已明确 defer 到 SR 阶段，不阻塞 IR closure。

IR 需求基线已准备就绪，可进入 SR 功能设计阶段。

## Machine-Actionable Closeout State

- **Gate status**: PASS
- **Worktree status**: n/a（文档工作，非 worktree）
- **Blocking reason**: none
- **Agent next action**: 更新 `docs/00-project/current-state.md` 授权进入 SR，创建 `20-sr.md`
- **User action required before next agent action**: no（可按 managed-change-standard 继续 SR）
