# v0.10.0 目标与范围

## Header

- Version name: `v0.10.0`
- Status: `DRAFT`
- Current phase: `VERSION_DESIGN_DRAFT`
- Requirement theme: runtime rejection-policy replacement with rebuild policy preservation

## 1. 背景

### 1.1 已完成的能力基线

| 版本 | 能力 | 状态 |
|---|---|---|
| v0.1.0 | 实验基础模型（ExperimentRun, LoadScenario, ControlPolicy） | IMPLEMENTED |
| v0.2.0 | 指标采集与记录（ObservedSnapshot, EvidenceRecorder, EvidenceSummary） | IMPLEMENTED |
| v0.3.0 | 场景运行器（ScenarioPlanner, BaselineWorkloadExecutor, ScenarioExperimentRunner） | IMPLEMENTED |
| v0.4.0 | 自适应策略与控制门（ControlGate, ThresholdPolicyEvaluator, PolicyDecision） | IMPLEMENTED |
| v0.5.0 | 离线回放与就绪判定（OfflinePolicyReplay, MutationReadinessGate, AdjustmentResult） | IMPLEMENTED |
| v0.6.0 | 压测数据获取基线（RunManifest, DataQualityValidator, ReadinessClassifier, ReportWriter） | IMPLEMENTED |
| v0.7.0 | ManagedExecutor 域与闭环实验（ManagedExecutor, ExecutorRegistry, AdjustmentAdapter, ScaleAdjustmentCommand） | IMPLEMENTED |
| v0.8.0 | 真实数据获取与 metrics 管道集成（ManagedExecutorScenarioRunner, G7-G9 门禁） | IMPLEMENTED |
| v0.9.0 | 队列容量动态调整（QueueResizeCommand, ExecutorRebuildStrategy, QueueResizeAdjustmentAdapter） | IMPLEMENTED |

### 1.2 当前缺口

v0.9.0 兑现了 `DynamicThreadPollerManager` 的第二个核心承诺 — 队列容量动态调整。三个动态配置维度中，两个已完成：

1. **线程数可动态调整** — v0.7.0 已实现（`ScaleAdjustmentCommand` → `ManagedExecutorAdjustmentAdapter` → `setCorePoolSize`/`setMaximumPoolSize`）
2. **队列容量可动态调整** — v0.9.0 已实现（`QueueResizeCommand` → `QueueResizeAdjustmentAdapter` → `ExecutorRebuildStrategy`）
3. **拒绝策略不可动态调整** — `ThreadPoolExecutor.setRejectedExecutionHandler()` 是 JDK 公开 API，但 `ManagedExecutor` 将其存储为 `private final`，无修改入口

这最后一个维度被多个文档标记为 deferred：

- v0.9.0 decision-log DFR-03: "Rejection policy runtime switching — 与 queue resizing 正交；需单独设计"
- `exploration-boundaries.md`: "Runtime rejection-policy replacement — Out of Scope Until Later Design"
- `operational-and-evolution-boundaries.md`: "Do not replace queue capacity or rejection strategy at runtime without explicit design and safety coverage"
- v0.9.0 retrospective 结论: "剩余 `Runtime rejection-policy replacement` 是下一个也是最后一个动态配置维度"

### 1.3 与 v0.9.0 的关键差异

v0.9.0 的核心挑战是 `ThreadPoolExecutor` 不支持 work queue 替换，需要 ExecutorRebuildStrategy（decommission → commission）。**v0.10.0 不面临这个约束**：

- `ThreadPoolExecutor.setRejectedExecutionHandler(RejectedExecutionHandler)` 是公开的、线程安全的 mutator
- 不需要 executor 重建
- Java 标准库提供四种内置策略：`AbortPolicy`（默认）、`CallerRunsPolicy`、`DiscardPolicy`、`DiscardOldestPolicy`
- 也可以接受自定义 `RejectedExecutionHandler` 实现

### 1.4 关联修复：ExecutorRebuildStrategy 策略丢失

`ExecutorRebuildStrategy.rebuild()` 第 75 行硬编码 `new ThreadPoolExecutor.AbortPolicy()`，导致 queue resize rebuild 后丢失原始拒绝策略。v0.10.0 必须修复此问题 — rebuild 后的新 executor 应保留旧 executor 的 `RejectedExecutionHandler`。

这是一个 **v0.9.0 遗留缺陷**，在 v0.10.0 中作为附带修复处理（范围小，与 rejection policy 主题强相关）。

### 1.5 为什么是现在

- v0.9.0 完成了三个动态维度中最难的一个（queue resize），证明 executor 生命周期管理基础设施成熟
- Rejection policy 替换技术难度远低于 queue resize（JDK 原生支持，无需 rebuild）
- `ExecutorRebuildStrategy` 的策略丢失 bug 是一个真实缺陷，v0.10.0 是修复它的自然时机
- 完成 v0.10.0 后，`DynamicThreadPollerManager` 的三个动态配置维度全部兑现

## 2. 目标

`v0.10.0` 聚焦以下目标：

1. **RejectionPolicyCommand**：定义 `RejectionPolicyCommand`，携带目标 `RejectedExecutionHandler` 及验证规则（非 null、与当前策略不同等）
2. **ManagedExecutor.setRejectionPolicy()**：为 `ManagedExecutor` 添加 rejection policy setter，委托给底层 `ThreadPoolExecutor.setRejectedExecutionHandler()`
3. **RejectionPolicyAdjustmentAdapter**：新增 adapter，接收 `RejectionPolicyCommand`，通过 safety gate 评估后执行策略替换
4. **RejectionPolicySafetyGate**：新增安全门禁，防止危险替换（如 executor 已 shutdown 时替换）
5. **PolicyReplacementEvidence**：每次策略替换产出标准化证据（before/after policy class name、executor state snapshot、timestamp）
6. **ExecutorRebuildStrategy 修复**：rebuild 后保留原始 rejection policy，不再硬编码 AbortPolicy
7. **端到端验证**：policy switch → re-run scenario → 验证新策略行为差异可观测（如从 AbortPolicy 切换到 CallerRunsPolicy）

## 3. 范围内

- `RejectionPolicyCommand` record（target policy, reason, timestamp）
- `ManagedExecutor.setRejectionPolicy(RejectedExecutionHandler)` 方法
- `RejectionPolicyAdjustmentAdapter`（接收 RejectionPolicyCommand，执行 policy 替换）
- `RejectionPolicySafetyGate`（安全检查：executor 非 shutdown、policy 非 null、policy 与当前不同）
- `PolicyReplacementEvidence` record（before/after policy class name, executor state snapshot, duration, success）
- `ExecutorRebuildStrategy` 修复：使用旧 executor 的 rejection policy 而非硬编码 AbortPolicy
- Policy switch + scenario re-run 端到端集成测试
- 现有 476 测试零回归

## 4. 范围外

- 自定义 `RejectedExecutionHandler` 实现（仅使用 JDK 内置四种策略；自定义 handler 可通过 command 传入但不作为设计目标）
- 多执行器协调 policy 替换
- 自动触发 policy 替换（闭环控制器）
- Policy 替换的持久化/审计存储
- 生产环境集成
- CLI entry
- 新的调整维度（thread count、queue capacity、rejection policy 三者之外）

## 5. 架构对齐

| 架构文档 | 本版本对应内容 |
|---|---|
| `operational-and-evolution-boundaries.md` | 满足 "explicit design and safety coverage" 要求，将 rejection strategy 从禁止项移入已设计项 |
| `exploration-boundaries.md` | 将 "Runtime rejection-policy replacement" 从 Out of Scope 移入 In Scope |
| `managed-executor-domain-model.md` | 扩展 RuntimeSetting：rejection policy 成为可调整参数 |

## 6. 模块边界

| 模块 | 变更类型 | 说明 |
|---|---|---|
| `experiment.executor` | **修改** `ManagedExecutor` — 添加 `setRejectionPolicy()` | 委托给 TPE.setRejectedExecutionHandler() |
| `experiment.executor` | **修改** `ExecutorRebuildStrategy` — 修复策略丢失 | 使用 `oldTpe.getRejectedExecutionHandler()` 替代硬编码 AbortPolicy |
| `experiment.executor` | **新增** `RejectionPolicyCommand` | 命令 record，类似 QueueResizeCommand 但更简单 |
| `experiment.executor` | **新增** `RejectionPolicyAdjustmentAdapter` | Adapter，类似 QueueResizeAdjustmentAdapter 但无需 rebuild |
| `experiment.policy` | **新增** `RejectionPolicySafetyGate` | 安全门禁 |
| `experiment.metrics` | **新增** `PolicyReplacementEvidence` | 证据 record |
| `experiment.scenario` | 不变 | 复用 ManagedExecutorScenarioRunner 做 post-switch 验证 |

### 依赖方向

```text
experiment.executor (RejectionPolicyCommand, ManagedExecutor.setRejectionPolicy, RejectionPolicyAdjustmentAdapter)
    ├── experiment.policy (RejectionPolicySafetyGate)
    └── experiment.metrics (PolicyReplacementEvidence)

experiment.executor (ExecutorRebuildStrategy 修复)
    └── 使用 TPE.getRejectedExecutionHandler() 替代硬编码
```

## 7. 核心技术设计

### 7.1 直接 setter — 无需 Executor Rebuild

`ThreadPoolExecutor.setRejectedExecutionHandler()` 是 JDK 公开的、线程安全的 API（`RejectedExecutionHandler` 字段为 `volatile`）。与 queue resize 不同，policy 替换不需要 decommission → commission 周期：

```java
// ManagedExecutor
public void setRejectionPolicy(RejectedExecutionHandler newPolicy) {
    Objects.requireNonNull(newPolicy, "rejectionPolicy must not be null");
    this.executor.setRejectedExecutionHandler(newPolicy);
    this.rejectionPolicy = newPolicy;  // 当前为 private final，需改为非 final
}
```

### 7.2 ManagedExecutor.rejectionPolicy 字段变更

当前：`private final RejectedExecutionHandler rejectionPolicy;`
变更为：`private volatile RejectedExecutionHandler rejectionPolicy;`

理由：`setRejectionPolicy()` 需要更新此字段以保持 `getRejectionPolicy()` 返回值与底层 TPE 一致。`volatile` 确保跨线程可见性。

### 7.3 ExecutorRebuildStrategy 修复

当前（第 75 行）：
```java
new ThreadPoolExecutor.AbortPolicy()  // 硬编码，丢失原始策略
```

修复后：
```java
oldTpe.getRejectedExecutionHandler()  // 保留原始策略
```

此修复影响范围极小（一行变更），但语义重要 — rebuild 后的 executor 应在所有方面（除 queue capacity 外）与旧 executor 一致。

### 7.4 RejectionPolicySafetyGate 检查条件

1. Executor 存在且未 shutdown/terminated
2. 新 policy 非 null
3. 新 policy 与当前 policy 不同（no-op 检测）
4. Executor 不在 resize 操作中（避免与 queue resize 并发）

注意：与 QueueResizeSafetyGate 不同，rejection policy 替换不需要检查 queue 深度 — policy 替换不涉及任务排空或丢失。

### 7.5 Policy 行为差异的可观测性

不同 rejection policy 在过载时行为不同，端到端测试需验证：

| Policy | 过载行为 | 可观测信号 |
|---|---|---|
| AbortPolicy | 抛出 RejectedExecutionException | 异常 + 任务被拒绝 |
| CallerRunsPolicy | 调用方线程执行任务 | 无异常 + activeCount 变化模式不同 |
| DiscardPolicy | 静默丢弃 | 无异常 + 任务数不增加 |
| DiscardOldestPolicy | 丢弃最旧任务后重试 | 无异常 + 队列中旧任务被替换 |

端到端测试通过故意过载场景（提交超过 queue capacity 的任务）验证策略行为切换生效。

## 8. 成功标准草案

- `RejectionPolicyCommand` 验证规则拒绝无效输入（null policy 等）
- `ManagedExecutor.setRejectionPolicy()` 正确委托给底层 TPE，`getRejectionPolicy()` 返回一致值
- `RejectionPolicyAdjustmentAdapter.apply()` 完成 policy 替换并返回 evidence
- `RejectionPolicySafetyGate` 正确拦截危险替换（shutdown executor、null policy、no-op）
- `PolicyReplacementEvidence` 完整记录 before/after policy class name 和 executor state
- `ExecutorRebuildStrategy` rebuild 后保留原始 rejection policy（回归测试验证）
- Policy switch 后过载场景行为与目标策略一致
- 现有 476 测试零回归

## 9. 候选 Change Decomposition

IR/SR 阶段确认后：

| # | Change name | 范围 | 依赖 |
|---|---|---|---|
| 1/? | `rejection-policy-command-and-adapter` | RejectionPolicyCommand, ManagedExecutor.setRejectionPolicy(), RejectionPolicyAdjustmentAdapter, RejectionPolicySafetyGate, PolicyReplacementEvidence, ExecutorRebuildStrategy 修复 | v0.7.0 ManagedExecutor + v0.9.0 ExecutorRebuildStrategy |
| 2/? | `rejection-policy-end-to-end-verification` | Policy switch + scenario re-run 集成测试，过载行为验证，rebuild 策略保留验证 | Change 1 |

## 10. 当前阶段出口

进入 IR 前必须完成：

1. `README.md` 版本索引
2. `00-objectives-and-scope.md`（本文档）
3. `decision-log.md` 记录关键设计判断
4. `docs/00-project/current-state.md` 反映 v0.10.0 版本设计草稿状态
