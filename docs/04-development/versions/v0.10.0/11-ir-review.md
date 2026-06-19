# v0.10.0 IR 独立评审报告

## Header

- Document type: IR independent review
- Version name: `v0.10.0`
- Reviewed artifact: `docs/04-development/versions/v0.10.0/10-ir.md`
- Review date: `2026-06-13`
- Reviewer role: 独立 IR review（非 IR 作者）
- Review basis: `docs/02-harness/managed-change-standard.md` 第 2 节（IR 需求分析）

## 1. 评审输入

已读取以下文件作为评审上下文：

- `docs/00-project/current-state.md`
- `docs/02-harness/managed-change-standard.md`
- `docs/01-architecture/operational-and-evolution-boundaries.md`
- `docs/05-domain/exploration-boundaries.md`
- `docs/04-development/versions/v0.10.0/README.md`
- `docs/04-development/versions/v0.10.0/00-objectives-and-scope.md`
- `docs/04-development/versions/v0.10.0/10-ir.md`
- `docs/04-development/versions/v0.10.0/decision-log.md`
- `docs/04-development/versions/v0.9.0/decision-log.md`
- `docs/08-retrospectives/2026-06-13-v0.9.0-queue-resize-retrospective.md`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/executor/ManagedExecutor.java`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/executor/ExecutorRebuildStrategy.java`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/executor/QueueResizeAdjustmentAdapter.java`
- `openspec/specs/establish-managed-executor-and-registry/spec.md`

## 2. 评审摘要

IR 草案结构完整：7 条需求覆盖了从 `RejectionPolicyCommand` 到端到端验证的完整链路。与 v0.9.0 queue resize 的关键技术差异（无需 executor rebuild）被正确识别。v0.9.0 复盘的三项流程改进均已应用（独立 result 类型、ControlGate 边界、SR 伪代码校验规则）。但存在 2 个 P0 阻断项和 3 个 P1 关键项需要处置，主要集中在 `PolicyReplacementResult` 类型未定义、resizeInProgress 可见性缺口、以及并发 policy 替换的幂等性判断。

## 3. Findings

### F01 [P0] PolicyReplacementResult 被引用但未定义

**位置**: IR-v0.10-003

**问题**: IR-v0.10-003 的 `apply()` 流程引用 `PolicyReplacementResult.success(...)` / `PolicyReplacementResult.failed(...)` / `PolicyReplacementResult.denied(...)`，但 IR 全文没有定义 `PolicyReplacementResult` 的结构。

v0.9.0 IR 在 IR-v0.9-001（QueueResizeCommand）和 IR-v0.9-005（ResizeEvidence）中分别定义了 command 和 evidence，但 `QueueResizeResult` 实际上是在 SR 阶段才引入的（作为 `AdjustmentResult` 的替代）。v0.10.0 IR 直接引用 `PolicyReplacementResult` 而没有先定义它，导致 IR-v0.10-003 的验收语义不可验证——评审者不知道 `PolicyReplacementResult` 应该包含哪些字段。

**影响**: IR-v0.10-003 的验收条件 AC-v0.10-005/006 无法验证——不知道 result 的字段就无法判断测试是否覆盖完整。

**建议**: IR 新增 `PolicyReplacementResult` 条目（或扩展 IR-v0.10-005 包含 result 定义），至少定义：
- `status`: enum (SUCCESS / DENIED / FAILED)
- `evidence`: PolicyReplacementEvidence
- `failureCode`: String (for DENIED/FAILED)
- `reason`: String

或 DEFER_TO_SR 但在 IR 中显式记录此缺口。

---

### F02 [P0] resizeInProgress 可见性缺口——Safety gate 无法检查并发 resize

**位置**: IR-v0.10-004

**问题**: IR-v0.10-004 要求 safety gate 检查"若 executor 正在 resize 中，DENY"。但 `QueueResizeAdjustmentAdapter.resizeInProgress` 是 `private final ConcurrentHashMap<String, Boolean>`——外部类（包括 `RejectionPolicySafetyGate`）无法访问。

IR 本身识别了这个问题（"SR 需决定是暴露此状态还是使用独立保护机制"），但列为"注意"而非正式的 P0/P1 finding。实际上：
- 如果不解决此问题，rejection policy 替换可能在 queue resize rebuild 过程中执行——此时旧 executor 已 shutdown，新 executor 尚未注册，policy 替换操作的对象是不确定的
- 这是 v0.9.0 与 v0.10.0 的唯一交叉点——两个调整维度在此交汇

**影响**: Safety gate 的关键安全条件无法实现，导致并发保护的 IR 需求悬空。

**建议**: IR 明确推荐方案（IR 做推荐，SR 最终决定）：
- 方案 A：`QueueResizeAdjustmentAdapter` 暴露 `isResizeInProgress(String executorId): boolean` 包级可见方法
- 方案 B：`RejectionPolicyAdjustmentAdapter` 维护独立的 policyChangeInProgress 标记（类似 resizeInProgress 但用于 policy change）
- 方案 C：在 `ExecutorRegistry` 层面统一管理操作互斥（`isOperationInProgress(executorId, OperationType)`）

推荐方案 A——最小侵入，复用已有标记。SR 决定最终方案。

---

### F03 [P1] 并发 policy 替换的幂等性判断不一致

**位置**: IR-v0.10-003, IR-v0.10-004

**问题**: IR-v0.10-003 明确声明"不得实现幂等保护（policy 替换不涉及 executor 重建，无并发风险）"。但 IR-v0.10-004 的 safety gate 要求检查"并发 resize 保护"。

这里有两个不一致：

1. **内部并发（policy-policy）**: IR 说不需要幂等保护。但如果两个线程同时发出不同的 RejectionPolicyCommand（一个设 CallerRunsPolicy，一个设 DiscardPolicy），最后执行的那个会覆盖前一个——没有检测、没有冲突信号。这可能是可接受的行为（last-write-wins），但 IR 应明确说明。

2. **外部并发（policy-resize）**: IR 说需要 resize 保护。但 resize 保护只防止"policy 在 resize 期间执行"，不防止"policy 在另一个 policy 执行期间执行"。如果 IR 认为内部并发无需保护，需要给出理由。

当前 IR 的立场是：内部并发不需要保护，但外部并发需要。这个判断可能是正确的——`ThreadPoolExecutor.setRejectedExecutionHandler()` 本身是原子操作（volatile 写），last-write-wins 是合理的语义——但 IR 需要明确这个设计判断。

**影响**: SR 可能设计出不一致的保护机制（保护了 resize 交叉但忽略了 policy-policy 交叉）。

**建议**: IR 明确记录设计判断：
- Policy-policy 并发：last-write-wins，无需幂等保护（TPE.setRejectedExecutionHandler() 是原子 volatile 写）
- Policy-resize 并发：需要保护（resize 期间 executor 对象在替换中，policy 替换操作对象不确定）
- SR 确认此判断

---

### F04 [P1] ExecutorRebuildStrategy 修复的测试覆盖不足

**位置**: IR-v0.10-006

**问题**: IR-v0.10-006 的验收语义只要求"新增测试：使用非默认 policy（如 CallerRunsPolicy）的 executor 经过 rebuild 后，新 executor 保持 CallerRunsPolicy"。但修复的影响范围值得更全面的测试：

1. 修复后，旧 executor 的非 AbortPolicy 被保留——但 rebuild 结果中其他配置（core/max/keepAlive/threadFactory）也已保留，现有测试已验证这些。是否需要对所有四种 JDK 策略都验证 rebuild 后保留？
2. 当前 `QueueResizeEndToEndTest` 中的所有测试都使用默认 AbortPolicy——修复后这些测试的行为不变（旧=AbortPolicy，新=AbortPolicy，无差异）。但如果某个测试无意中依赖了"rebuild 后一定是 AbortPolicy"的行为，修复会打破这个假设——虽然这种依赖不太可能存在

**影响**: 低。修复本身是一行代码变更，但测试覆盖应至少验证：1) 非默认 policy 被保留，2) 默认 policy 也被保留（回归）。

**建议**: 
- AC-v0.10-011 拆为两个：AC-v0.10-011a [P0] 非默认 policy 保留，AC-v0.10-011b [P1] 所有四种 JDK 策略保留
- 或保持现状（一个 P0 AC），SR 确认测试策略

---

### F05 [P1] RejectionPolicyCommand 的 `fromCurrent()` 类型比较策略未决定

**位置**: IR-v0.10-001

**问题**: IR-v0.10-001 的 `fromCurrent()` 候选验收语义说"若 `target.getClass() == current.getClass()` 且两者为同类型...返回 `Optional.empty()`"，但同时识别了风险："自定义 RejectedExecutionHandler 可能有状态——IR 阶段识别此风险，SR 决定是否使用 `equals()` 或仅比较 class"。

这个决策影响 `fromCurrent()` 的语义正确性：
- JDK 四种内置策略都是无状态单例，class 比较足够
- 自定义 handler 可能有状态（两个 AbortPolicy 子类实例可能行为不同）
- 但自定义 handler 不在 v0.10.0 范围内（scope section 4 明确排除）

既然自定义 handler 明确 out of scope，IR 可以直接决定：**使用 class 比较**。不需要 defer 到 SR。如果未来支持自定义 handler，那是后续版本的扩展。

**影响**: SR 可能需要处理一个本应在 IR 阶段决定的问题。

**建议**: IR 直接决定使用 class 比较（reason: JDK 内置四种策略都是无状态单例，自定义 handler out of scope）。不需要 DEFER_TO_SR。

---

### F06 [P2] DiscardPolicy/DiscardOldestPolicy 端到端验证的断言策略未定义

**位置**: IR-v0.10-007

**问题**: IR-v0.10-007 要求测试 AbortPolicy → DiscardPolicy 和 AbortPolicy → DiscardOldestPolicy 的切换。但 IR 的风险表已识别："这两种策略静默丢弃任务，可观测信号较弱"。

当前 IR 给出的验证方式是"验证任务被静默丢弃"和"验证最旧任务被替换"，但没有具体的断言策略：
- DiscardPolicy：提交 N 个任务填满 queue + threads，再提交额外任务 → 验证额外任务未抛出异常且 completedTaskCount 不变
- DiscardOldestPolicy：提交 N 个任务，再提交新任务 → 验证 queue 中最旧的任务被替换（queue 内容改变但大小不变）

这些是可实现的断言，但 IR 没有给出具体的可观测信号。SR 需要设计具体的测试策略。

**影响**: 低。端到端测试可实现，但断言策略需要在 SR 或测试设计阶段明确。不影响 IR closure。

**建议**: DEFER_TO_SR。IR 给出推荐断言方向，SR 定义具体测试用例。

---

### F07 [P2] `rejectionPolicy` 字段的 `volatile` 必要性

**位置**: IR-v0.10-002

**问题**: IR-v0.10-002 要求 `rejectionPolicy` 字段从 `private final` 改为 `private volatile`。理由是"与底层 TPE 的 volatile 语义一致"。

但 `ManagedExecutor.rejectionPolicy` 字段的读者可能只有 `getRejectionPolicy()` 一个方法，而底层 TPE 的 `getRejectedExecutionHandler()` 已经提供了 volatile 读。`ManagedExecutor.rejectionPolicy` 只是一个缓存副本——如果调用方通过 `getRejectionPolicy()` 获取值，底层 TPE 的 volatile 语义已经保证了可见性。

实际上，最简单的实现是：
```java
public void setRejectionPolicy(RejectedExecutionHandler newPolicy) {
    Objects.requireNonNull(newPolicy);
    this.executor.setRejectedExecutionHandler(newPolicy);
    // 不需要单独维护 rejectionPolicy 字段
}

public RejectedExecutionHandler getRejectionPolicy() {
    return this.executor.getRejectedExecutionHandler(); // 直接从 TPE 读取
}
```

这样就完全消除了 `rejectionPolicy` 字段——getter 直接从 TPE 读取，保证始终一致。是否需要保留独立字段是 SR 的设计选择。

**影响**: 低。两种实现（volatile 字段 vs 直接委托 TPE）在行为上等价。但 SR 应评估更简单的"直接委托"方案。

**建议**: DEFER_TO_SR。IR 记录两种方案，SR 选择。

---

## 4. Findings 汇总

| ID | 位置 | 描述 | 级别 | 建议动作 |
|---|---|---|---|---|
| F01 | IR-v0.10-003 | PolicyReplacementResult 被引用但未定义 | P0 | IR 新增 result 条目或显式 DEFER_TO_SR |
| F02 | IR-v0.10-004 | resizeInProgress 可见性缺口 | P0 | IR 明确推荐方案（暴露包级可见方法） |
| F03 | IR-v0.10-003/004 | 并发 policy 替换幂等性判断不一致 | P1 | IR 明确记录 last-write-wins 设计判断 |
| F04 | IR-v0.10-006 | Rebuild 修复测试覆盖范围 | P1 | 拆分 AC 或 SR 确认 |
| F05 | IR-v0.10-001 | fromCurrent() class vs equals 策略 | P1 | IR 直接决定 class 比较（自定义 handler out of scope） |
| F06 | IR-v0.10-007 | Discard 策略端到端断言策略 | P2 | DEFER_TO_SR |
| F07 | IR-v0.10-002 | rejectionPolicy 字段 volatile vs 直接委托 | P2 | DEFER_TO_SR |

## 5. 正向检查通过项

- [x] IR 不隐含实现授权——各条目使用"候选验收语义"措辞，未声称已实现
- [x] Scope 边界明确排除自定义 handler、closed-loop、多执行器协调
- [x] 非范围列表与 `00-objectives-and-scope.md` 和 `decision-log.md` 一致
- [x] 正确识别 rejection policy 替换无需 executor rebuild（与 v0.9.0 的关键技术差异）
- [x] RejectionPolicyCommand 与 ScaleAdjustmentCommand/QueueResizeCommand 职责清晰分离
- [x] 复用 `ExecutorRegistry`、`ManagedExecutor`、`ExecutorStateSnapshot` 等现有基础设施
- [x] v0.9.0 复盘三项流程改进均已应用（独立 result 类型、ControlGate 边界、SR 伪代码校验规则）
- [x] 现有 `ManagedExecutorAdjustmentAdapter` 和 `QueueResizeAdjustmentAdapter` 行为不受影响
- [x] Safety gate 使用独立接口（不实现 ControlGate），与 QueueResizeSafetyGate 模式一致
- [x] 端到端测试覆盖四种 JDK 策略切换 + rebuild 策略保留 + DENY 路径
- [x] ExecutorRebuildStrategy 修复是最小化变更（一行代码）
- [x] 风险和延期项表覆盖了自定义 handler 相等性、并发交互、Discard 验证难度、字段兼容性
- [x] 出口条件清单与 `managed-change-standard.md` 第 2 节一致
- [x] 不涉及 production 环境、外部依赖、REST/API/UI

## 6. 评审结论

IR 草案在技术差异识别和 v0.9.0 复盘应用方面**合格**。7 条需求覆盖了从 `RejectionPolicyCommand` 到端到端验证的完整链路，与 queue resize 的关键差异（无需 executor rebuild）被正确识别并反映在需求简化中。v0.9.0 复盘的三项流程改进全部落地。但**不能直接进入 SR**：存在 2 个 P0 阻断项（F01 PolicyReplacementResult 未定义、F02 resizeInProgress 可见性缺口）和 3 个 P1 关键项。P0/P1 必须通过 disposition 关闭。

评审建议：**进入 IR disposition（`12-ir-review-disposition.md`）**，逐项处置 F01-F07。
