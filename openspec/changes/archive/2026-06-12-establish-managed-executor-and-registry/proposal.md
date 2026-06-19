## Why

v0.1.0 到 v0.6.0 构建了完整的实验基础设施（scenario、metrics、policy、analysis、adjustment、acquisition），但所有能力都运行在 `InMemoryAdjustableExecutorProbe` —— 一个不执行真实任务的内存探针。项目名字是 `DynamicThreadPollerManager`，却还没有真正管理过 `ThreadPoolExecutor`。

v0.7.0 IR + SR 已闭环，设计确认第一步是建立 `ManagedExecutor` 和 `ExecutorRegistry` 基础域层，不涉及调整桥接。本 change 只做"包装真实线程池"和"注册管理"，不做任何 mutation 路径。

## What Changes

**ManagedExecutor**
- From: 实验框架通过 `InMemoryAdjustableExecutorProbe` 模拟执行器状态。
- To: `ManagedExecutor` 包装 `java.util.concurrent.ThreadPoolExecutor`，暴露可控参数（core/max pool size, keep-alive）、只读状态（active, pool, queue, completed）、生命周期（shutdown/termination）。
- Reason: 为后续调整桥接和闭环实验提供真实线程池基础。
- Impact: 新增 `experiment.executor` 包，不修改 `experiment.adjustment` 接口（仅扩展 `ExecutorStateSnapshot` 字段）。

**ExecutorRegistry**
- From: 不存在执行器注册管理。
- To: `ExecutorRegistry` 提供命名注册、查找、列举和受保护的删除（`DeletionSafety`）。
- Reason: 后续 adapter 需要按名称查找目标执行器。
- Impact: 新增类，单进程 `ConcurrentHashMap` 实现。

**RuntimeSetting**
- From: 不存在运行时参数分类。
- To: 枚举可调整参数（CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME）与不可调整参数（QUEUE_CAPACITY, REJECTION_POLICY），并提供类型安全的边界检查。
- Reason: 为安全门校验提供参数合法性定义。
- Impact: 新增 value objects。

**DeletionSafety**
- From: 不存在删除保护。
- To: 基于 `AtomicInteger` 引用计数的删除安全判断，防止移除仍在使用的执行器。
- Reason: 架构文档 `managed-executor-domain-model.md` 要求的删除安全语义。
- Impact: 新增接口 + `AtomicDeletionSafety` 实现。

**ExecutorStateSnapshot 扩展**
- From: 快照只覆盖 corePoolSize, maxPoolSize, activeCount, queueSize, queueCapacity。
- To: 增补 5 个 nullable 字段：poolSize, completedTaskCount, keepAliveTimeSeconds, largestPoolSize, taskCount。
- Reason: 真实 ThreadPoolExecutor 能提供比探针更丰富的状态，快照字段应覆盖。
- Impact: 修改 `experiment.adjustment.ExecutorStateSnapshot`（兼容扩展，现有测试不受影响）。

## Capabilities

### New Capabilities
- `establish-managed-executor-and-registry`: `ManagedExecutor` 包装、`ExecutorRegistry` 注册管理、`RuntimeSetting` 参数分类、`DeletionSafety` 删除保护、`ExecutorStateSnapshot` 扩展。

### Modified Capabilities
- `executor-adapter-and-adjustment-evidence`: `ExecutorStateSnapshot` 新增 5 个 nullable 字段。现有 builder 路径兼容，无 breaking change。

## Impact

新增 `experiment.executor` 包（6 个类），扩展 `experiment.adjustment.ExecutorStateSnapshot`（5 个 nullable 字段）。不修改任何现有接口，不引入外部依赖，不涉及 queue resizing、调度器、持久化或 REST/API。
