# v0.7.0 目标与范围

## Header

- Version name: `v0.7.0`
- Status: `DRAFT`
- Current phase: `VERSION_DESIGN_DRAFT`
- Requirement theme: managed executor domain, real ThreadPoolExecutor bridging, first closed-loop experiment

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

### 1.2 当前缺口

上述六个版本围绕 `InMemoryAdjustableExecutorProbe`（一个内存探针，不是真实线程池）构建了完整的实验基础设施。项目存在以下结构性缺口：

- **没有受管理的真实执行器**：`InMemoryAdjustableExecutorProbe` 只记录"如果调整会怎样"，不执行真实任务。
- **调整命令没有真实落点**：`ScaleAdjustmentCommand` 设计完整，但 `ExecutorAdjustmentAdapter` 只对接了探针。
- **就绪判定没有真实对象**：`MutationReadinessGate` 和 `ReadinessClassifier` 评估的是模拟数据。
- **项目名称与能力不匹配**：`DynamicThreadPollerManager` 尚未动态管理过任何一个 `ThreadPoolExecutor`。

### 1.3 为什么是现在

- v0.6.0 已完结数据获取基线，证明实验框架可以产出合格的压力证据。
- v0.5.0 的 `MutationReadinessGate` 和 `RuntimeAdjustmentSafetyGate` 已就绪，可以在调整前提供安全防护。
- 架构文档中 "Managed Executor Domain Model" 为 executor registry、runtime setting、deletion safety 提供了目标模型。
- 此时引入真实 `ThreadPoolExecutor` 接管的**风险可控**：只做受控实验，不进生产。

## 2. 目标

`v0.7.0` 聚焦以下目标：

1. **ManagedExecutor 抽象**：设计并实现 `ManagedExecutor`，包装 `java.util.concurrent.ThreadPoolExecutor`，暴露可控参数和只读状态。
2. **ExecutorRegistry**：实现命名注册表，支持创建、查找、列举和受保护的删除。
3. **RuntimeSetting 模型**：明确定义哪些参数可在运行时安全调整（core/max pool size, keep-alive），哪些不可（queue capacity, rejection policy）。
4. **DeletionSafety**：实现删除安全规则，防止移除正在使用的执行器。
5. **调整桥接**：将 `ScaleAdjustmentCommand` 连接到真实 `ThreadPoolExecutor.setCorePoolSize()` / `setMaximumPoolSize()`。
6. **状态桥接**：将 `ExecutorStateSnapshot` 连接到真实 `ThreadPoolExecutor` 状态读取（active count, pool size, queue size, completed task count）。
7. **闭环实验验证**：运行首次完整闭环实验 — 部署场景 → 采集指标 → 策略评估 → 调整执行器 → 验证效果。

## 3. 范围内

- `ManagedExecutor` 抽象及 `ThreadPoolExecutor` 包装实现。
- `ExecutorRegistry` 命名注册、查找和删除安全。
- `RuntimeSetting` 枚举和调整边界定义。
- `DeletionSafety` 规则和检查接口。
- `ExecutorAdjustmentAdapter` 的真实执行器实现（替换/补充 `InMemoryAdjustableExecutorProbe`）。
- `ExecutorStateSnapshot` 的真实状态采集实现。
- 一个闭环实验场景（steady workload + policy trigger + adjustment + post-adjustment observation）。
- 现有 `MutationReadinessGate`、`SafetyGate`、`ReadinessClassifier` 在真实调整前的集成。
- 确定性场景和可复现实验矩阵。
- 所有现有测试继续通过。

## 4. 范围外

- Queue resizing（queue capacity 的动态修改）。
- 生产 `ThreadPoolExecutor` 集成或线上环境部署。
- 闭环调度器/控制器（自动连续运行、定时触发）。
- 持久化（数据库、文件存储）。
- REST / API / UI 暴露。
- 外部依赖引入。
- Rejection policy 运行时切换。
- 多执行器协调或分布式场景。
- Throughput improvement claim / 性能优化声明。
- `ThreadPoolExecutor` 以外的执行器类型（virtual threads, ForkJoinPool 等）。

## 5. 架构对齐

本版本直接实现以下架构文档中描述的目标模型：

| 架构文档 | 本版本对应内容 |
|---|---|
| `managed-executor-domain-model.md` | `ManagedExecutor`, `ExecutorRegistry`, `RuntimeSetting`, `DeletionSafety` |
| `scheduling-reconfiguration-and-recovery-model.md` | 有界、显式、可测试的重配置通道 |
| `observability-and-experiment-strategy.md` | 先观察再优化，真实状态采集 |

## 6. 成功标准草案

- `ManagedExecutor` 能正确包装 `ThreadPoolExecutor`，参数调整后行为与直接操作 `ThreadPoolExecutor` 一致。
- `ExecutorRegistry` 能创建、查找、列举执行器，删除时触发安全检查。
- `ScaleAdjustmentCommand` 能通过 `ExecutorAdjustmentAdapter` 真实调整 `ThreadPoolExecutor` 的 core/max pool size。
- `ExecutorStateSnapshot` 能从真实 `ThreadPoolExecutor` 读取 active/pool/queue/completed 状态。
- 一次闭环实验中，scenario → metrics → policy → adjustment → post-observation 链路完整，且调整前后状态差异可验证。
- `MutationReadinessGate` 和 `SafetyGate` 在真实调整前正确拦截越界请求。
- 现有测试套件无回归。
- 新增测试覆盖：ManagedExecutor 生命周期、注册表操作、调整安全边界、状态采集精度。

## 7. 当前阶段出口

进入 IR 前必须完成：

1. `README.md` 版本索引。
2. `00-objectives-and-scope.md`（本文档）。
3. `decision-log.md` 记录关键设计判断。
4. `docs/00-project/current-state.md` 反映 v0.7.0 版本设计草稿状态。

## 8. 候选 Change Decomposition

SR 阶段确认后的候选分解方向上会使用 action-oriented kebab-case 命名：

- `establish-managed-executor-and-registry` — ManagedExecutor + ExecutorRegistry + DeletionSafety
- `bridge-adjustment-to-real-executor` — 调整命令与真实状态桥接
- `closed-loop-experiment-verification` — 首次闭环实验验证

具体数量和边界由 SR 阶段确定，不在此阶段预设。
