# v0.9.0 IR Review Disposition

## Header

- Document type: IR review disposition
- Version name: `v0.9.0`
- Reviewed artifact: `docs/04-development/versions/v0.9.0/11-ir-review.md`
- Disposition date: `2026-06-13`
- Disposition by: IR author, responding to independent review

## Disposition Summary

| Total Findings | FIX | DEFER_TO_SR | ACCEPT | CLOSED |
|---|---|---|---|---|
| 8 | 4 | 3 | 0 | 1 |

## Per-Finding Disposition

### F01 [P0] Drain-and-replay 原子性语义 → **FIX**

**处置**: 补充 IR-v0.9-002 drain-and-replay 原子性契约。

**具体修改**:
1. Drain 是 shallow copy（只移出 `BlockingQueue` 中的引用），任务 Runnable 对象不受影响
2. 若新 TPE 创建失败（commission 阶段异常）：
   - Rebuild 整体标记为 FAILED
   - 旧 executor 状态：
     - 如果已 shutdown：旧 executor 不能恢复运行，返回 failure result，证据中记录 `partialFailure=true`
     - 如果尚未 shutdown：保持旧 executor 运行，不执行 rebuild
   - Drained 任务：若无法 replay 到新 executor 且旧 executor 不可用，记录 `lostTaskCount`
3. SHRINK + drained > newCapacity：逐任务 `newExecutor.submit()`，抛 `RejectedExecutionException` 时 `rejectedTaskCount++` 并在 evidence 中记录
4. Commission 成功前不得将新 executor 注册到 registry

**IR 更新**: 在 IR-v0.9-002 的 Decommission/Commission 阶段增加原子性契约段落。

---

### F02 [P0] Adapter 扩展方式 → **FIX**

**处置**: 新建 `QueueResizeAdjustmentAdapter`，不修改 `ManagedExecutorAdjustmentAdapter`。

**理由**:
- 遵循 v0.8.0 D1 "新建不修改" 原则（新建 `ManagedExecutorScenarioRunner` 而非修改 `ScenarioExperimentRunner`）
- `ScaleAdjustmentCommand` 操作线程数维度，`QueueResizeCommand` 操作队列容量维度——不同的对象 → 不同的 adapter
- 两个 adapter 共享 `ExecutorRegistry` 和 `ExecutorStateSnapshot` 依赖，但不混合在同一个类中
- 测试隔离性更好：每个 adapter 独立测试，不会相互干扰
- 未来若引入更多 command 类型（如 `RejectionPolicyCommand`），adapter 爆炸的风险由注册表或策略模式解决

**IR 更新**: IR-v0.9-004 明确"新建 `QueueResizeAdjustmentAdapter`，不修改 `ManagedExecutorAdjustmentAdapter`"。

---

### F03 [P1] Registry executorId 语义 → **DEFER_TO_SR**

**处置**: DEFER_TO_SR。IR 给出推荐方向（保持同一 executorId），SR 最终决定。

**推荐**: 保持同一 executorId。Rebuild 的语义是"同一个逻辑 executor 的配置变更"。旧 reference 在 rebuild 后失效是可接受的行为。若调用方持有旧 reference，任何操作（如 submit）会在 shutdown/terminated executor 上失败并抛出明确的 `RejectedExecutionException`。

**并发考虑**: `ExecutorRegistry` 的 `register()` 和 `remove()` 操作是原子的（`ConcurrentHashMap.put/remove`），替换注册（remove + register）中间有微秒级窗口。若需要严格原子替换，SR 可考虑新增 `replace(String id, ManagedExecutor old, ManagedExecutor new)` 方法。

**IR 更新**: 无（IR 已记录两种选择，SR 决定）。

---

### F04 [P1] ResizeEvidence 与 EvidenceRecorder 关系 → **FIX**

**处置**: `ResizeEvidence` 通过 `AdjustmentResult` 直接携带，不经过 `EvidenceRecorder`。

**理由**:
- 与现有 `AdjustmentResult.evidence()` 模式一致（v0.5.0 定义）
- `EvidenceRecorder` 只处理 `ObservedSnapshot`（per-run 采样数据），扩展其接口会引入类型爆炸
- Resize evidence 是 per-adjustment 粒度的（不是 per-run 粒度），语义不匹配
- 未来若需要跨 resize 事件聚合分析，可新增 `ResizeEvidenceRecorder`——但那是后续版本的优化

**IR 更新**: IR-v0.9-005 明确"ResizeEvidence 通过 AdjustmentResult.evidence() 携带，不经过 EvidenceRecorder"。

---

### F05 [P1] SHRINK resize drain→shutdown race → **FIX**

**处置**: Decommission 流程增加 "stop accepting new tasks" 步骤。

**具体修改**: 在 drain 之前增加：
- 通过 registry 中的标记（`AtomicBoolean acceptingTasks`）或直接调用旧 executor 的 `shutdown()` 在 drain 之前（shutdown 拒绝新任务但不中断已在 queue 中的任务）
- 流程变为：`markNotAccepting() → shutdown() → drainTo() → awaitTermination()`
- 注意：`shutdown()` 在 drain 之前调用意味着旧 executor 不再接受新任务，但已在 queue 中的任务不受影响（仍可被 drain）
- 这消除了 drain 和 shutdown 之间的 race window

**IR 更新**: IR-v0.9-002 的 Decommission 阶段更新流程顺序。

---

### F06 [P1] AC 对应关系缺口 → **FIX**

**处置**:
1. AC-v0.9-008（Safety gate DENY 非 RUNNING 状态）提升为 P0
2. 新增 AC-v0.9-011a [P0]：每次 resize 必须产出非空 ResizeEvidence
3. G10 resize gate → DEFER_TO_SR（SR 决定是否需要独立的 resize gate）

**IR 更新**: 更新验收条件表。

---

### F07 [P2] 线程配置读取时机 → **DEFER_TO_SR**

**处置**: DEFER_TO_SR。IR 给出推荐方向（使用 decommission 入口处的 snapshot 值），SR 最终决定。

**推荐**: Commission 使用 `beforeState: ExecutorStateSnapshot` 中记录的值（即 decommission 入口处的 snapshot）。这些值在 rebuild 开始时已通过 `executor.toSnapshot()` 获取，保证与 `ResizeEvidence.beforeState` 一致。

**IR 更新**: 无（SR 记录语义）。

---

### F08 [P2] Drain-and-discard 策略 → **DEFER_TO_SR**

**处置**: DEFER_TO_SR。IR 推荐 drain-and-replay 为默认策略，SR 阶段评估是否需要 drain-and-discard 作为 SHRINK 的可选行为。

**推荐**: Drain-and-replay 是通用策略。若 SR 认为 SHRINK 时 replay 到更小的 queue 的高失败率不可接受，可增加 `QueueResizeCommand.discardOnShrink: boolean` 字段（默认 false → drain-and-replay）。

**IR 更新**: 无（SR 阶段决策）。

---

## 修改后的 IR 更新计划

| IR 条目 | 变更 |
|---|---|
| IR-v0.9-002 | 补充 drain-replay 原子性契约；Decommission 流程增加 stop-accepting → shutdown → drain 顺序 |
| IR-v0.9-004 | 明确新建 QueueResizeAdjustmentAdapter（不修改现有 adapter） |
| IR-v0.9-005 | 明确 ResizeEvidence 通过 AdjustmentResult 携带，不经过 EvidenceRecorder |
| IR section 4 | AC-v0.9-008 提升 P0；新增 AC-v0.9-011a [P0]；G10 → DEFER_TO_SR |

## 处置后状态

| Finding | 原始级别 | 处置 | 状态 |
|---|---|---|---|
| F01 | P0 | FIX (IR 补充原子性契约) | CLOSED |
| F02 | P0 | FIX (明确新建 adapter) | CLOSED |
| F03 | P1 | DEFER_TO_SR (保持同一 executorId) | CLOSED |
| F04 | P1 | FIX (AdjustmentResult 直接携带 evidence) | CLOSED |
| F05 | P1 | FIX (stop-accepting → shutdown → drain 顺序) | CLOSED |
| F06 | P1 | FIX (调整 AC 优先级和补充) | CLOSED |
| F07 | P2 | DEFER_TO_SR (snapshot 值读取时机) | CLOSED |
| F08 | P2 | DEFER_TO_SR (drain-and-discard 策略) | CLOSED |

## 出口条件

所有 P0/P1 findings 已处置。可进入 IR closure verification。
