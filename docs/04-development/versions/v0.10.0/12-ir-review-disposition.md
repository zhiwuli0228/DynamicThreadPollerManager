# v0.10.0 IR Review Disposition

## Header

- Document type: IR review disposition
- Version name: `v0.10.0`
- Reviewed artifact: `docs/04-development/versions/v0.10.0/11-ir-review.md`
- Disposition date: `2026-06-13`
- Disposition by: IR author, responding to independent review

## Disposition Summary

| Total Findings | FIX | DEFER_TO_SR | ACCEPT | CLOSED |
|---|---|---|---|---|
| 7 | 3 | 3 | 0 | 1 |

## Per-Finding Disposition

### F01 [P0] PolicyReplacementResult 被引用但未定义 → **FIX**

**处置**: IR 新增 `IR-v0.10-003a PolicyReplacementResult` 条目。

**具体内容**:

`PolicyReplacementResult` record 必须包含：
- `status`: `PolicyReplacementStatus` enum — `SUCCESS` / `DENIED` / `FAILED`
- `evidence`: `PolicyReplacementEvidence`（非 null）
- `failureCode`: `String`（SUCCESS 时为 null，DENIED/FAILED 时为非空错误码）
- `reason`: `String`（非空）
- 静态工厂方法 `success(PolicyReplacementEvidence)` / `denied(String failureCode, String reason, PolicyReplacementEvidence)` / `failed(String failureCode, String reason, PolicyReplacementEvidence)`

遵循 v0.9.0 复盘流程改进 #2 "独立 result 类型模式"——`PolicyReplacementResult` 是 `RejectionPolicyAdjustmentAdapter` 的专用返回类型，不复用 `AdjustmentResult`（对应 `ScaleAdjustmentCommand`）或 `QueueResizeResult`（对应 `QueueResizeCommand`）。

**IR 更新**: 新增 IR-v0.10-003a，在 IR-v0.10-003 中引用。

---

### F02 [P0] resizeInProgress 可见性缺口 → **FIX**

**处置**: 在 `QueueResizeAdjustmentAdapter` 中暴露包级可见方法 `isResizeInProgress(String executorId): boolean`。

**理由**:
- 最小侵入（新增一个 package-visible 方法，不改变现有 API）
- `QueueResizeAdjustmentAdapter.resizeInProgress` 是 `ConcurrentHashMap<String, Boolean>`，读取操作线程安全
- `RejectionPolicySafetyGate` 位于 `experiment.policy` 包——与 `experiment.executor` 不同包。包级可见性不够
- 修正为：暴露 **public** 方法 `isResizeInProgress(String executorId): boolean`

**方案细化**:
1. `QueueResizeAdjustmentAdapter` 新增 `public boolean isResizeInProgress(String executorId)`
2. `RejectionPolicySafetyGate` 构造时接收 `QueueResizeAdjustmentAdapter` 引用（或接收 `Predicate<String>` 函数式接口以降低耦合）
3. Safety gate 的 `evaluate()` 中调用此方法检查并发 resize

推荐函数式接口方案（`Predicate<String>` 或自定义 `ResizeInProgressChecker`），使 `RejectionPolicySafetyGate` 不直接依赖 `QueueResizeAdjustmentAdapter` 类型。SR 决定最终实现方式。

**IR 更新**: IR-v0.10-004 的安全检查条件补充"通过注入的 `Predicate<String> isResizeInProgress` 检查并发 resize"，移除"SR 需决定"的悬空表述。

---

### F03 [P1] 并发 policy 替换的幂等性判断不一致 → **FIX**

**处置**: IR 明确记录设计判断。

**IR 更新**: IR-v0.10-003 补充并发语义段落：

```
并发语义:
- Policy-policy 并发 (两个线程同时发出不同的 RejectionPolicyCommand):
  last-write-wins. TPE.setRejectedExecutionHandler() 是原子 volatile 写,
  无需幂等保护. 两个替换都会成功, 最后执行的决定最终 policy.
- Policy-resize 并发 (policy 替换与 queue resize 重叠):
  需要保护. resize 期间 executor 对象正在替换中 (decommission →
  commission), policy 替换的操作对象不确定. 通过
  QueueResizeAdjustmentAdapter.isResizeInProgress() 检查.
```

---

### F04 [P1] Rebuild 修复测试覆盖范围 → **DEFER_TO_SR**

**处置**: DEFER_TO_SR。IR 保持一个 P0 AC（AC-v0.10-011），SR 决定是否需要覆盖所有四种 JDK 策略。

**理由**:
- 修复本身是一行变更：`new ThreadPoolExecutor.AbortPolicy()` → `oldTpe.getRejectedExecutionHandler()`
- 语义等价：只要旧 executor 的 policy 是什么，新 executor 就保留什么——不需要逐策略验证
- 测试一个非默认 policy（如 CallerRunsPolicy）足以证明"保留"行为——如果代码逻辑是"保留"，那么保留任何值都成立
- 若 SR 认为需要全策略覆盖，可以将 AC 拆分为多个测试

**IR 更新**: 无（SR 决定测试策略）。

---

### F05 [P1] fromCurrent() class vs equals 策略 → **FIX**

**处置**: IR 直接决定使用 class 比较（`target.getClass() == current.getClass()`）。

**理由**:
- JDK 四种内置 `RejectedExecutionHandler` 实现都是无状态单例——`AbortPolicy`、`CallerRunsPolicy`、`DiscardPolicy`、`DiscardOldestPolicy` 都不携带任何实例状态。class 比较即语义比较
- 自定义 handler 明确不在 v0.10.0 范围内（scope section 4）
- 若未来引入有状态的自定义 handler，后续版本可扩展 `fromCurrent()` 的重载或引入 `RejectionPolicyEquivalence` 策略接口
- 不需要 DEFER_TO_SR——IR 阶段就可以拍板

**IR 更新**: IR-v0.10-001 的 `fromCurrent()` 语义从"若 `target.getClass() == current.getClass()` 且两者为同类型...SR 决定是否使用 `equals()`"改为"若 `target.getClass() == current.getClass()`，返回 `Optional.empty()`。理由：JDK 四种内置策略都是无状态单例，class 比较即语义比较。自定义 handler out of scope"。

---

### F06 [P2] Discard 策略端到端断言策略 → **DEFER_TO_SR**

**处置**: DEFER_TO_SR。IR 给出推荐断言方向，SR 定义具体测试用例。

**推荐方向**:
- DiscardPolicy：提交 N 个任务填满 queue + threads → 再提交 M 个额外任务 → 验证无 `RejectedExecutionException` 抛出，且 `completedTaskCount` 不变（额外任务被静默丢弃）
- DiscardOldestPolicy：提交 N 个任务填满 queue → 记录 queue 头部任务 → 再提交新任务 → 验证 queue 头部任务被替换（不再是原来那个），且 queue size 不变

**IR 更新**: 无（SR 设计具体断言）。

---

### F07 [P2] rejectionPolicy 字段 volatile vs 直接委托 → **DEFER_TO_SR**

**处置**: DEFER_TO_SR。IR 记录两种方案，SR 选择。

**方案 A（当前 IR）**: `rejectionPolicy` 字段改为 `private volatile`，setter 同时更新字段和 TPE。
- 优点：`getRejectionPolicy()` 不经过 TPE，字段读取快（但差别可忽略）
- 缺点：维护两份状态（字段 + TPE 内部 handler），存在不一致风险

**方案 B（review 建议）**: 删除 `rejectionPolicy` 字段，`getRejectionPolicy()` 直接从 TPE 读取。
- 优点：单一数据源，零不一致风险
- 缺点：每次 `getRejectionPolicy()` 调用都读 volatile（性能影响可忽略）

**推荐**: 方案 B（直接委托）。更简单，更安全。

**IR 更新**: IR-v0.10-002 补充"备选方案：删除 rejectionPolicy 字段，getRejectionPolicy() 直接委托给 `this.executor.getRejectedExecutionHandler()`。SR 选择最终方案"。

---

## 修改后的 IR 更新计划

| IR 条目 | 变更 |
|---|---|
| IR-v0.10-001 | `fromCurrent()` 明确使用 class 比较，移除 DEFER_TO_SR 悬空表述 |
| IR-v0.10-002 | 补充备选方案 B（删除字段，直接委托 TPE） |
| IR-v0.10-003 | 补充并发语义段落（internal vs external concurrency）；引用 IR-v0.10-003a |
| 新增 IR-v0.10-003a | PolicyReplacementResult 定义 |
| IR-v0.10-004 | resizeInProgress 检查改为注入 `Predicate<String>`，移除悬空表述 |

## 处置后状态

| Finding | 原始级别 | 处置 | 状态 |
|---|---|---|---|
| F01 | P0 | FIX (新增 IR-v0.10-003a PolicyReplacementResult) | CLOSED |
| F02 | P0 | FIX (暴露 resizeInProgress + 注入 Predicate) | CLOSED |
| F03 | P1 | FIX (明确记录 last-write-wins 并发语义) | CLOSED |
| F04 | P1 | DEFER_TO_SR (测试覆盖范围) | CLOSED |
| F05 | P1 | FIX (IR 直接决定 class 比较) | CLOSED |
| F06 | P2 | DEFER_TO_SR (Discard 断言策略) | CLOSED |
| F07 | P2 | DEFER_TO_SR (字段 volatile vs 直接委托) | CLOSED |

## 出口条件

所有 P0/P1 findings 已处置。可进入 IR closure verification。
