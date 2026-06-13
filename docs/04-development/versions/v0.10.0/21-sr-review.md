# v0.10.0 SR 独立评审报告

## Header

- Document type: SR independent review
- Version name: `v0.10.0`
- Reviewed artifact: `docs/04-development/versions/v0.10.0/20-sr.md`
- Review date: `2026-06-13`
- Reviewer role: 独立 SR review（非 SR 作者）
- Review basis: `docs/02-harness/managed-change-standard.md` 第 3 节（SR 功能设计）

## 1. 评审输入

已读取以下文件作为评审上下文：

- `docs/00-project/current-state.md`
- `docs/02-harness/managed-change-standard.md`
- `docs/04-development/versions/v0.10.0/10-ir.md` (IR closure verified)
- `docs/04-development/versions/v0.10.0/11-ir-review.md`
- `docs/04-development/versions/v0.10.0/12-ir-review-disposition.md`
- `docs/04-development/versions/v0.10.0/13-ir-closure-verification.md`
- `docs/04-development/versions/v0.10.0/20-sr.md`
- `docs/04-development/versions/v0.10.0/decision-log.md`
- `docs/08-retrospectives/2026-06-13-v0.9.0-queue-resize-retrospective.md`
- `src/main/java/.../executor/ManagedExecutor.java`
- `src/main/java/.../executor/ExecutorRebuildStrategy.java`
- `src/main/java/.../executor/QueueResizeAdjustmentAdapter.java`
- `src/main/java/.../executor/QueueResizeResult.java`
- `src/main/java/.../executor/QueueResizeCommand.java`
- `src/main/java/.../policy/QueueResizeSafetyGate.java`

## 2. 评审摘要

SR 整体设计质量高：5 个核心新组件 + 3 个既有代码修改，伪代码完整。与 v0.9.0 的关键技术差异（无需 executor rebuild）正确反映在设计的简化中。v0.9.0 复盘三项流程改进全部落地（独立 result 类型、ControlGate 边界、SR 伪代码 API 校验）。IR 阶段 deferred 的 3 个 P2 项（F04/F06/F07）均已在本 SR 中给出明确设计决策。

## 3. API 签名抽样校验

按照 `managed-change-standard.md` SR 伪代码强制验证规则，随机抽取 3 个 API 调用点进行实际源码验证：

### 抽样点 1: `ManagedExecutor.isShutdown()` / `isTerminated()`

**SR 伪代码**: `executor.isShutdown()` / `executor.isTerminated()`（§4.3 RejectionPolicySafetyGate）
**实际源码**: `public boolean isShutdown()` / `public boolean isStopped()` / `public boolean isTerminated()`
**验证结果**: **签名匹配**。`isShutdown()` 和 `isTerminated()` 均存在，返回 `boolean`。

---

### 抽样点 2: `PolicyReplacementResult` 工厂方法模式

**SR 伪代码**: `PolicyReplacementResult.success(evidence)` / `PolicyReplacementResult.denied(failureCode, reason, evidence)` / `PolicyReplacementResult.failed(failureCode, reason)` / `PolicyReplacementResult.failed(failureCode, reason, evidence)`
**实际基准**: `QueueResizeResult.success(ResizeEvidence)` / `QueueResizeResult.failed(String, String)` / `QueueResizeResult.failed(String, String, ResizeEvidence)`
**验证结果**: **模式匹配**。`PolicyReplacementResult` 遵循 `QueueResizeResult` 的工厂方法模式，且新增 `denied()` 工厂（safety gate 拦截场景的独立语义——`QueueResizeResult` 通过 `failed("SAFETY_GATE_DENIED", ...)` 表达，`PolicyReplacementResult` 将 DENIED 独立为工厂方法，语义更清晰）。参数顺序一致（failureCode 在前，reason/errorMessage 在后）。

---

### 抽样点 3: `ExecutorRebuildStrategy.rebuild()` 中 `oldTpe.getRejectedExecutionHandler()`

**SR 伪代码**: `oldTpe.getRejectedExecutionHandler()`（§4.7）
**实际源码**: `ThreadPoolExecutor.getRejectedExecutionHandler()` — JDK 公开方法，返回 `RejectedExecutionHandler`
**验证结果**: **签名匹配**。JDK 标准 API，存在且返回类型正确。

---

### 抽样结论

3 个抽样点全部通过。额外验证项：
- `RejectionPolicyCommand` record 构造器使用 `java.util.Objects.requireNonNull` — 匹配项目代码风格（`ManagedExecutor` 使用 `Objects.requireNonNull`）
- `RejectionPolicySafetyGate.evaluate()` 三参数签名 — 与 `QueueResizeSafetyGate.evaluate(QueueResizeCommand, ManagedExecutor)` 两参数不同，差异是有意的（需要 executorId 进行 Predicate 检查）
- `ExecutorRegistry.get(String)` 返回 `Optional<ManagedExecutor>` — SR 伪代码正确使用 `found.isEmpty()`（匹配实际 `Optional` API）

## 4. Findings

### F01 [P1] ManagedExecutor 构造器中 `rejectionPolicy` 赋值需同步移除

**位置**: 20-sr.md §4.2

**问题**: SR §4.2 设计删除 `private final RejectedExecutionHandler rejectionPolicy` 字段。但当前 `ManagedExecutor` 的 7 参数构造器中有 `this.rejectionPolicy = rejectionHandler;`（第 50 行）——删除字段后此行必须一并移除，否则编译错误。

SR 伪代码只展示了 `getRejectionPolicy()` 和 `setRejectionPolicy()` 的变更，未标注构造器中的同步变更。这是一个确定性变更（删除字段必然需要删除赋值），但影响范围描述不完整。

当前构造器（第 29-51 行）:
```java
public ManagedExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime,
                       TimeUnit unit, BlockingQueue<Runnable> workQueue,
                       ThreadFactory threadFactory, RejectedExecutionHandler rejectionHandler) {
    // ... validation ...
    this.executor = new ThreadPoolExecutor(...);
    this.queueCapacity = workQueue.remainingCapacity() + workQueue.size();
    this.rejectionPolicy = rejectionHandler;  // ← 此行需删除
}
```

5 参数构造器不直接引用 `rejectionPolicy` 字段（委托给 7 参数构造器），不受影响。

**影响**: 编译错误（如果只删除字段声明而忘记删除赋值）。但这是实现阶段的确定性修复，不影响设计正确性。

**建议**: SR §4.2 补充标注：`this.rejectionPolicy = rejectionHandler;` 行一并删除。SR review 可 CLOSE（实现阶段必然修复）。

---

### F02 [P1] RejectionPolicySafetyGate 三参数签名与 QueueResizeSafetyGate 两参数不一致

**位置**: 20-sr.md §4.3

**问题**: `RejectionPolicySafetyGate.evaluate(RejectionPolicyCommand, ManagedExecutor, String executorId)` 是三参数，而 `QueueResizeSafetyGate.evaluate(QueueResizeCommand, ManagedExecutor)` 是两参数。额外的 `executorId` 参数仅用于 `isResizeInProgress.test(executorId)`。

设计问题：`executorId` 参数对于 PERMIT/DENY 判断的核心逻辑不是必需的——它只在 Check 4（并发 resize 保护）中使用。如果未来有其他 safety gate 也需要 executorId，三参数签名会成为模式。但目前两个 safety gate 签名不一致。

替代方案：
- 方案 A（当前）: `evaluate(command, executor, executorId)` — 三参数，executorId 仅用于 Predicate
- 方案 B: `evaluate(command, executor)` — 两参数，与 QueueResizeSafetyGate 一致。Predicate 改为双参数 `BiPredicate<String, ManagedExecutor>` 或在 safety gate 内部通过其他方式获取 executorId

**影响**: 低。两个 safety gate 独立使用，签名不一致不影响功能。但 SR 应记录这个设计选择。

**建议**: SR 记录签名差异及理由（方案 A 更简单：executorId 直接传入，避免 safety gate 需要知道如何从 ManagedExecutor 获取 ID——实际上 ManagedExecutor 不携带 registry ID）。DEFER_TO_SR 或直接记录决策。

---

### F03 [P1] PolicyReplacementEvidence 缺少 DENIED 与 FAILED 场景的区分字段

**位置**: 20-sr.md §4.6

**问题**: `PolicyReplacementEvidence` record 有 `success: boolean` 字段，但 DENIED（safety gate 拦截）和 FAILED（setRejectionPolicy 调用异常）都设 `success=false`，仅通过 `reason` 字符串区分。

对比：
- `ResizeEvidence` 也有 `boolean success` 但通过 `errorMessage`（null on success）区分失败原因
- `PolicyReplacementResult` 有 `failureCode: String` 区分 DENIED vs FAILED——这是正确的分层：result 层区分失败类型，evidence 层记录操作事实

当前设计是合理的——evidence 是操作记录，"成功/失败"是二元事实，`reason` 提供上下文。`failureCode` 属于 result 层。但 SR 应明确记录这个分层设计，避免实现时混淆。

**影响**: 低。当前设计分层清晰（result 区分 failureCode，evidence 记录 bool + reason）。实现不会混淆。

**建议**: SR 添加注释说明 evidence 和 result 的分层职责。或 ACCEPT（无需修改）。

---

### F04 [P2] RejectionPolicyAdjustmentAdapter 缺少 resizeInProgress 的自我检查

**位置**: 20-sr.md §4.4

**问题**: `RejectionPolicyAdjustmentAdapter.apply()` 通过 safety gate 检查 resize-in-progress，但 adapter 自身没有 policy-change-in-progress 标记。

`QueueResizeAdjustmentAdapter` 有 `resizeInProgress` ConcurrentHashMap 防止并发 resize——因为两个 resize 同时执行会导致 executor 状态混乱（两个 rebuild 操作同一 executor）。但 `RejectionPolicyAdjustmentAdapter` 没有类似保护——因为 SR 判断"policy-policy 并发是 last-write-wins，无需幂等保护"（IR F03 处置）。

这个判断是合理的：`TPE.setRejectedExecutionHandler()` 是原子 volatile 写，两个线程同时调用不会导致中间状态。但 SR 应明确记录"为什么 policy adapter 不需要幂等保护而 resize adapter 需要"——两者的差异可能让后续维护者困惑。

**影响**: 无功能影响。纯文档层面。

**建议**: SR §4.4 添加简短说明："与 QueueResizeAdjustmentAdapter 不同，本 adapter 不需要幂等保护——TPE.setRejectedExecutionHandler() 是原子 volatile 写，并发 policy 替换是 last-write-wins，不产生中间状态。"

---

### F05 [P2] DiscardOldestPolicy 端到端测试的断言策略需要具体化

**位置**: 20-sr.md §4.9 Test 3

**问题**: SR §4.9 Test 3 的 DiscardOldestPolicy 验证策略是"提交 Task-A → Task-B → Task-C(过载) → 验证 Task-C 被执行，Task-A 被丢弃"。但这个断言依赖于能区分三个任务——需要每个任务有唯一标识（如 AtomicInteger 计数器或任务名称）。

IR F06 将 Discard 断言策略 DEFER_TO_SR，SR 给出了高层策略但未指定具体实现方式。这在实际编码时可能需要调整。

**影响**: 低。端到端测试可实现，但断言策略的具体实现方式需要在实现阶段确定。

**建议**: DEFER_TO_IMPLEMENTATION。SR 给出的策略方向正确，实现阶段使用可区分的任务标识（如 `Runnable` 子类带名称字段）完成验证。

---

## 5. Findings 汇总

| ID | 位置 | 描述 | 级别 | 建议动作 |
|---|---|---|---|---|
| F01 | SR §4.2 | 构造器中 rejectionPolicy 赋值需同步删除 | P1 | SR 补充标注，CLOSE |
| F02 | SR §4.3 | Safety gate 三参数 vs 两参数签名不一致 | P1 | SR 记录设计选择理由 |
| F03 | SR §4.6 | Evidence/Result 分层职责需明确 | P1 | SR 添加注释或 ACCEPT |
| F04 | SR §4.4 | Adapter 幂等保护差异需说明 | P2 | SR 添加简短说明 |
| F05 | SR §4.9 | DiscardOldestPolicy 断言策略待具体化 | P2 | DEFER_TO_IMPLEMENTATION |

## 6. 正向检查通过项

- [x] SR 不授权实现——明确声明 "不授权 Java 源码或测试实现"
- [x] 5 个核心新组件 + 3 个既有代码修改伪代码完整
- [x] API 签名抽样校验 3/3 通过（isShutdown/isTerminated、PolicyReplacementResult 工厂、getRejectedExecutionHandler）
- [x] RejectionPolicyAdjustmentAdapter 新建独立类，不修改现有两个 adapter（处置 IR F02）
- [x] RejectionPolicySafetyGate 使用独立接口（处置 v0.9.0 复盘流程改进 #3）
- [x] PolicyReplacementResult 是独立 result 类型（处置 v0.9.0 复盘流程改进 #2 + IR F01）
- [x] ManagedExecutor.getRejectionPolicy() 直接委托 TPE——方案 B（处置 IR F07）
- [x] resizeInProgress 通过 Predicate 注入（处置 IR F02）
- [x] fromCurrent() 使用 class 比较（处置 IR F05）
- [x] 并发语义明确：policy-policy last-write-wins, policy-resize 需保护（处置 IR F03）
- [x] ExecutorRebuildStrategy 修复是最小化变更（一行代码）
- [x] 依赖方向明确且裁决完整
- [x] 测试策略分层清晰：单元 → 集成 → 端到端
- [x] 非回归约束覆盖现有 476 测试 + 三个 adapter + runner
- [x] 2 个候选 change 分解合理，独立可验证性已检查
- [x] 验收矩阵覆盖 16 个 AC，包括 P0 关键路径
- [x] 不涉及 reflection hack、自定义 handler、closed-loop、多执行器协调
- [x] IR deferred 3 个 P2 项全部处置

## 7. 评审结论

SR 设计在技术差异识别、架构边界、组件契约和测试策略方面**合格**。5 个核心新组件 + 3 个既有代码修改设计完整，与 v0.9.0 的关键差异（无需 executor rebuild）正确反映在设计的简化中。v0.9.0 复盘三项流程改进全部落地，API 签名抽样校验 3/3 通过。存在 3 个 P1 项（F01 构造器清理、F02 签名差异、F03 分层职责）——均为文档/标注层面的问题，不涉及设计缺陷。P1 可通过 disposition 快速关闭。

评审建议：**进入 SR disposition（`22-sr-review-disposition.md`）**，逐项处置 F01-F05。
