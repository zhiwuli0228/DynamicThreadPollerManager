# v0.9.0 目标与范围

## Header

- Version name: `v0.9.0`
- Status: `DRAFT`
- Current phase: `VERSION_DESIGN_DRAFT`
- Requirement theme: runtime queue capacity resizing with executor rebuild strategy

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
| v0.7.0 | ManagedExecutor 域与闭环实验（ManagedExecutor, ExecutorRegistry, AdjustmentAdapter） | IMPLEMENTED |
| v0.8.0 | 真实数据获取与 metrics 管道集成（ManagedExecutorScenarioRunner, G7-G9 门禁） | IMPLEMENTED |

### 1.2 当前缺口

v0.8.0 完成了在真实 ThreadPoolExecutor 上的多场景数据获取。但是，**项目名称 `DynamicThreadPollerManager` 的核心承诺仍有一半未兑现**：

1. **线程数可动态调整** — v0.7.0 已实现（`ScaleAdjustmentCommand` → `ManagedExecutorAdjustmentAdapter` → `setCorePoolSize`/`setMaximumPoolSize`）
2. **队列容量不可动态调整** — `ThreadPoolExecutor` 不支持运行时替换 work queue；`RuntimeSetting` 明确排除了 queue capacity

这是整个版本历史中**最持久的结构性债务**：
- v0.1.0 D3: "queue as first-class resource"
- v0.2.0 scope: "no queue capacity mutation"
- v0.5.0 D1/D2/D5: 明确判定 queue resizing 不能与 pool size adjustment 合并，需单独设计
- v0.7.0 scope: "No queue resizing"
- v0.8.0 scope: "No queue resizing"
- `exploration-boundaries.md`: "Runtime queue-capacity replacement — Out of Scope Until Later Design"
- `operational-and-evolution-boundaries.md`: "Do not replace queue capacity or rejection strategy at runtime without explicit design and safety coverage"

**现在时机成熟**：所有周边基础设施（ManagedExecutor, ExecutorRegistry, SafetyGate, AdjustmentAdapter, EvidenceRecorder, ScenarioRunner）已就位且经过 433 项测试验证。

### 1.3 为什么是现在

- v0.7.0/v0.8.0 已证明 executor 创建、执行、关闭、证据记录的完整链路稳定可靠
- 队列容量是唯一剩下的"静态"配置项 — core/max pool size 已可动态调整
- 8 个版本的 Infrastructure 沉淀足以支撑这个架构挑战
- `ExecutorRegistry` 的 `DeletionSafety` 机制为 executor 替换提供了天然的注册/注销框架
- v0.5.0 的 IR/SR 已对 queue resizing 做了初步需求分析，可直接引用

## 2. 目标

`v0.9.0` 聚焦以下目标：

1. **Queue Resize Command**：定义 `QueueResizeCommand`（类似 `ScaleAdjustmentCommand` 但操作队列容量），包含验证规则（newCapacity > 0, newCapacity != currentCapacity 等）
2. **Executor Rebuild Strategy**：通过 decommission（停旧）+ commission（启新）策略实现队列容量调整，解决 `ThreadPoolExecutor` 不支持运行时 queue 替换的限制
3. **Safety Gate for Queue Resize**：新增队列调整安全门禁，防止危险操作（如在任务堆积时缩小队列）
4. **Resize Evidence**：每次 resize 操作产生完整的证据链（before → after executor state snapshots, rebuild duration, task drain count），通过标准 metrics 管道
5. **端到端验证**：resize → re-run scenario → 验证新队列容量生效，通过 G1-G9 门禁（可扩展 G10 resize 门禁）

## 3. 范围内

- `QueueResizeCommand` record（类似 ScaleAdjustmentCommand，target queue capacity）
- `QueueResizeAdjustmentAdapter` 或扩展现有 `ManagedExecutorAdjustmentAdapter`（接收 QueueResizeCommand）
- `ExecutorRebuildStrategy`（decommission → commission 周期：drain tasks, shutdown old, create new with resized queue, re-register）
- `QueueResizeSafetyGate`（安全检查：当前队列深度 vs 新容量、executor 状态等）
- `QueueResizeEvidence` 或扩展现有 evidence 类型
- Resize + re-acquire 端到端集成测试
- 现有 433 测试零回归

## 4. 范围外

- Rejection policy 运行时切换（单独 defer）
- 多执行器协调 resize
- 自动触发 resize（闭环控制器留给后续版本）
- Queue resizing 的持久化/审计存储
- CLI entry（DFR-01，单独考虑是否纳入 v0.9.0 作为次要目标）
- 生产环境集成

## 5. 架构对齐

| 架构文档 | 本版本对应内容 |
|---|---|
| `operational-and-evolution-boundaries.md` | 满足 "explicit design and safety coverage" 要求 |
| `exploration-boundaries.md` | 将 "Runtime queue-capacity replacement" 从 Out of Scope 移入 In Scope |
| `observability-and-experiment-strategy.md` | 每次 resize 作为实验记录，产出标准化证据 |

## 6. 模块边界

| 模块 | 变更类型 | 说明 |
|---|---|---|
| `experiment.executor` | **修改** `ManagedExecutorAdjustmentAdapter` 或 **新增** adapter | 支持 QueueResizeCommand；可能新增 ExecutorRebuildStrategy |
| `experiment.policy` | **新增** `QueueResizeSafetyGate` | 队列调整安全门禁 |
| `experiment.metrics` | **新增** evidence 类型或扩展现有 | resize before/after 证据 |
| `experiment.scenario` | 不变 | 复用 ManagedExecutorScenarioRunner 做 post-resize 验证 |
| `experiment.acquisition` | 可能扩展 G10 门禁 | resize 后数据质量验证 |

### 依赖方向

```text
experiment.executor (QueueResizeCommand, ExecutorRebuildStrategy)
    ├── experiment.policy (QueueResizeSafetyGate)
    └── experiment.metrics (ResizeEvidence)

experiment.policy (QueueResizeSafetyGate)
    └── experiment.executor (ManagedExecutor, ExecutorRegistry)
```

## 7. 核心技术挑战

### 7.1 ThreadPoolExecutor 不支持 queue 替换

`ThreadPoolExecutor` 的 work queue 在构造时传入，构造后不可替换。两种策略：

**策略 A — Executor Rebuild（推荐）**：
1. 暂停向旧 executor 提交新任务
2. 等待/排空在途任务
3. 关闭旧 executor（shutdown + awaitTermination）
4. 以新 queue capacity 构建新 `ThreadPoolExecutor`
5. 在 `ExecutorRegistry` 中替换注册
6. 记录 before/after 证据

**策略 B — Queue Swapping（不推荐）**：
- 通过反射或自定义 `ThreadPoolExecutor` 子类替换 `workQueue` 字段
- 风险高，JDK 版本依赖，不保证线程安全

### 7.2 在途任务处理

Rebuild 过程中必须处理已在旧 queue 中等待的任务：
- **Drain-and-replay**：排空旧 queue，在新 executor 中重新提交
- **Drain-and-discard**：排空并丢弃（如果 resize 的目标是缩减队列）
- **Graceful-drain**：停止新提交，等待所有已提交任务完成

具体策略由安全门禁和 resize 方向（扩大 vs 缩小）决定。

### 7.3 线程配置保持

Rebuild 后的新 executor 必须保持旧 executor 的 core/max pool size、keep-alive time、thread factory 等配置不变——只改变 queue capacity。

## 8. 成功标准草案

- `QueueResizeCommand` 验证规则拒绝无效输入（capacity <= 0, capacity == current 等）
- `ExecutorRebuildStrategy` 能安全完成 decommission → commission 周期
- Resize 后新的 ThreadPoolExecutor 以指定 queue capacity 运行
- `QueueResizeSafetyGate` 正确拦截危险 resize（如 queue 深度 > 新容量）
- Resize 证据完整记录（before/after executor state, rebuild duration, drained task count）
- Post-resize scenario run 通过 G1-G9 门禁
- 现有 433 测试零回归

## 9. 候选 Change Decomposition

IR/SR 阶段确认后：

| # | Change name | 范围 | 依赖 |
|---|---|---|---|
| 1/? | `queue-resize-command-and-rebuild` | QueueResizeCommand, ExecutorRebuildStrategy, Adapter 扩展 | v0.7.0 ManagedExecutor + ExecutorRegistry |
| 2/? | `queue-resize-safety-and-evidence` | QueueResizeSafetyGate, ResizeEvidence, G10 gate | Change 1 |
| 3/? | `queue-resize-end-to-end-verification` | Resize + re-acquire 集成测试，全门禁通过 | Change 1 + 2 |

## 10. 当前阶段出口

进入 IR 前必须完成：

1. `README.md` 版本索引
2. `00-objectives-and-scope.md`（本文档）
3. `decision-log.md` 记录关键设计判断
4. `docs/00-project/current-state.md` 反映 v0.9.0 版本设计草稿状态
