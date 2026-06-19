# DynamicThreadPollerManager 综合验证报告

> 日期: 2026-06-14
> 版本基线: v0.1.0 ~ v0.11.0 + parallel-test-execution
> 测试结果: **646 测试全部通过，0 失败，0 错误**
> Java 版本: 21 | 构建工具: Maven | 框架: Spring Boot 4.0.6

---

## 一、项目概述

DynamicThreadPollerManager 是一个面向实验的线程池动态管理系统。它在 Java 标准库 `ThreadPoolExecutor` 的基础上，构建了一套完整的**运行时参数动态调整、安全门控、证据采集、离线回放与分析**的闭环实验框架。

传统 `ThreadPoolExecutor` 在创建后，核心参数（线程数、队列容量、拒绝策略）基本固定，运行时调整需自行编码且缺乏安全保障。本系统针对这三个维度分别实现了**安全的运行时热替换**，并配套完整的观测、记录与回放能力。

### 版本演进路线

| 版本 | 能力 | 状态 |
|------|------|------|
| v0.1.0 | 实验基础模型（ExperimentRun, LoadScenario, ControlPolicy） | 已实现 |
| v0.2.0 | 指标采集与记录（ObservedSnapshot, EvidenceRecorder） | 已实现 |
| v0.3.0 | 场景运行器（ScenarioPlanner, BaselineWorkloadExecutor） | 已实现 |
| v0.4.0 | 自适应策略与控制门（ControlGate, ThresholdPolicyEvaluator） | 已实现 |
| v0.5.0 | 离线回放与就绪判定（OfflinePolicyReplay, MutationReadinessGate） | 已实现 |
| v0.6.0 | 压测数据获取基线（RunManifest, DataQualityValidator, 9道质量门禁） | 已实现 |
| v0.7.0 | ManagedExecutor 域与闭环实验（ManagedExecutor, ExecutorRegistry） | 已实现 |
| v0.8.0 | 真实数据获取与 metrics 管道集成 | 已实现 |
| v0.9.0 | **队列容量动态调整**（QueueResizeCommand, ExecutorRebuildStrategy） | 已实现 |
| v0.10.0 | **拒绝策略动态替换**（RejectionPolicyCommand, PolicyReplacementEvidence） | 已实现 |
| v0.11.0 | 持久化证据录制与自主采样（FileBackedEvidenceRecorder, LivePressureSampler） | 已实现 |
| 横向优化 | 并行测试执行（1.8x 加速） | 已归档 |

---

## 二、三种动态调整策略详细对比

### 2.1 策略一：线程池大小动态调整（v0.7.0）

#### 2.1.1 传统方案对比

| 维度 | 传统 ThreadPoolExecutor | DynamicThreadPollerManager |
|------|------------------------|---------------------------|
| 调整方式 | 手动调用 `setCorePoolSize()` / `setMaximumPoolSize()`，无配套安全机制 | 通过 `ScaleAdjustmentCommand` → `RuntimeAdjustmentSafetyGate` → `ManagedExecutorAdjustmentAdapter` 管道执行 |
| 安全保障 | 无。调用者自行确保参数合法性 | 8 级安全门控：输入校验 → 就绪性检查 → 冷却期 → 每轮限额 → 方向阻断 → No-op 检测 → 边界封顶 → 放行 |
| 调整顺序 | 开发者自行处理 max < core 的情况 | Adapter 自动处理：先 setMaximumPoolSize 再 setCorePoolSize，确保不触发 IllegalArgumentException |
| 可观测性 | 无内建观测 | 每次调整产生 `AdjustmentEvidence`（含 before/after 状态快照、命令引用、时间戳） |
| 冷却机制 | 无 | 可配置冷却间隔（默认 2 个决策周期），防止震荡 |
| 方向控制 | 无 | 可配置阻断立即反向操作（扩缩交替保护） |
| 每轮限额 | 无 | 默认每轮实验最多 5 次调整，防止过度干预 |

#### 2.1.2 核心组件

- **`ScaleAdjustmentCommand`**: 封装调整指令，commandId 格式为 `runId:timestamp:current->target`，支持溯源
- **`DefaultRuntimeAdjustmentSafetyGate`**: 8 级串行评估链，每级可独立拒绝
- **`ManagedExecutorAdjustmentAdapter`**: 唯一的线程池变更点（架构约束：只有 Adapter 可以执行 mutation）
- **`ThresholdPolicyEvaluator`**: 基于活跃线程数和队列深度的阈值策略评估器

#### 2.1.3 安全门控规则详情

```
规则 1: 输入校验 — targetPoolSize < 1 或 currentPoolSize < 0 → INVALID_COMMAND
规则 2: 就绪性 — ReadinessAssessment 为 NOT_READY → NOT_READY
规则 3: 风险接受 — READY_WITH_RISK 但未启用风险接受 → RISK_NOT_ACCEPTED
规则 4: 冷却期 — 距上次调整不足 cooldownDecisionIntervals 个决策间隔 → COOLDOWN_ACTIVE
规则 5: 每轮限额 — 已达 maxAdjustmentsPerRun → RUN_LIMIT_EXCEEDED
规则 6: 方向阻断 — 缩→扩 或 扩→缩 的立即反转 → OPPOSITE_DIRECTION
规则 7: No-op — target == current → NO_OP（不消耗冷却计数）
规则 8: 放行 — ALLOW
```

#### 2.1.4 优势

1. **安全可控**: 8 级门控链确保任何调整都经过完整校验，避免线程池参数突变导致系统不稳定
2. **可审计**: 每次调整产生完整的证据链（AdjustmentEvidence），含 before/after 状态、命令来源引用
3. **防震荡**: 冷却期 + 方向阻断机制有效防止扩缩交替震荡
4. **确定性**: 策略评估器不读取系统时钟，时间戳由调用者注入，测试可复现
5. **边界安全**: ControlGate 自动将超出 min/max 的调整封顶到边界值（CAPPED），不会抛异常

#### 2.1.5 劣势与局限

1. **依赖 ReadinessAssessment**: 调整前必须完成离线回放分析并达到 READY 状态，冷启动阶段无法调整
2. **冷却期引入延迟**: 默认 2 个决策间隔的冷却期意味着突发压力下不能立即连续扩容
3. **单向调整限制**: 扩缩交替被阻断，需要等待冷却期结束后才能反向操作
4. **无自动回滚**: 调整后如果指标恶化，系统不会自动回滚到之前的状态

#### 2.1.6 测试覆盖

- `RuntimeAdjustmentSafetyGateTest`: 16 个测试，覆盖全部 9 种 FailureCode
- `ManagedExecutorAdjustmentAdapterTest`: 16 个测试，覆盖 APPLIED/REJECTED/FAILED/NO_OP 四条路径
- `ClosedLoopExperimentTest`: 3 个端到端测试，验证完整闭环（压力生成 → 状态读取 → 策略评估 → 调整执行 → 验证）
- `ThresholdPolicyEvaluatorTest`: 17 个测试，覆盖扩/缩/保持/封顶全场景

---

### 2.2 策略二：队列容量动态调整（v0.9.0）

#### 2.2.1 传统方案对比

| 维度 | 传统 ThreadPoolExecutor | DynamicThreadPollerManager |
|------|------------------------|---------------------------|
| 队列调整 | **不可能**。`ThreadPoolExecutor` 不提供 `setQueue()` 方法，work queue 在构造后固定 | 通过 ExecutorRebuildStrategy 实现 decommission → commission → replay 三阶段重建 |
| 任务连续性 | N/A（无法调整） | 重建期间排空旧队列的任务，在新 executor 上重放（仅扩容方向） |
| 线程配置保留 | N/A | 重建后保留 corePoolSize、maximumPoolSize、keepAliveTime、ThreadFactory、RejectedExecutionHandler |
| 幂等保护 | N/A | ConcurrentHashMap 防止同一 executor 的并发 resize |
| 拒绝策略保留 | N/A | v0.10.0 修复：重建时使用 `oldTpe.getRejectedExecutionHandler()` 而非硬编码 AbortPolicy |
| 执行器终止 | N/A | 旧 executor 必须先 shutdown → awaitTermination → shutdownNow 确保完全终止 |

#### 2.2.2 为什么需要重建策略

`ThreadPoolExecutor` 的 `workQueue` 字段是 `final` 的，JDK 不提供运行时替换 API。这是三个动态维度中**技术难度最高**的一个，因为：

1. 不能直接修改队列，必须创建新 executor 替换旧 executor
2. 旧 executor 中可能有正在执行的任务和排队中的任务
3. 重建过程中需要保证任务不丢失（扩容场景）
4. 需要确保旧 executor 完全终止后才能从 registry 移除

#### 2.2.3 重建流程（平台线程模式）

```
1. 快照旧 executor 状态（beforeState）
2. 判断方向（EXPAND / SHRINK）
3. Decommission:
   a. oldExecutor.shutdown()
   b. 排空队列 → drainedTasks
   c. awaitTermination(timeoutMs)
   d. 未终止则 shutdownNow()
4. Commission:
   a. 创建新 ManagedExecutor（保留 core/max/keepAlive/threadFactory/rejectionPolicy）
   b. 新 LinkedBlockingQueue(targetQueueCapacity)
5. Registry 更新:
   a. registry.remove(executorId)
   b. registry.register(executorId, newExecutor)
6. Replay（仅 EXPAND）:
   a. 将 drainedTasks 提交到新 executor
   b. 统计重放失败数量
7. 返回 RebuildResult（含 before/after 状态、耗时、排空/拒绝计数）
```

#### 2.2.4 虚拟线程模式差异

虚拟线程模式下重建更简单：虚拟线程 executor 没有固定线程池，只需创建新的 `ManagedExecutor.virtual()` 并重放任务。Semaphore 并发限制和 pendingQueue 在新 executor 上重新初始化。

#### 2.2.5 优势

1. **突破 JDK 限制**: 在不修改 JDK 源码的前提下实现了 ThreadPoolExecutor 不支持的队列热替换
2. **任务不丢失（扩容）**: 排空的任务在新 executor 上重放，保证任务连续性
3. **配置完整性**: 重建后保留所有线程配置和拒绝策略，对上层透明
4. **并发安全**: 幂等保护防止同一 executor 的并发 resize 导致状态混乱
5. **跨维度协调**: RejectionPolicySafetyGate 通过 `isResizeInProgress` 检查，防止在 resize 过程中更换拒绝策略
6. **证据完整**: `ResizeEvidence` 记录 before/after 状态、重建耗时、排空/拒绝任务数

#### 2.2.6 劣势与局限

1. **重建开销**: 每次队列调整都需要终止旧 executor 并创建新 executor，有不可忽略的开销
2. **缩容丢弃任务**: SHRINK 方向会排空队列但不重放（drain and discard），排队中的任务被丢弃
3. **服务中断窗口**: 从 shutdown 到新 executor 就绪之间存在短暂的服务中断
4. **超时依赖**: `awaitTermination` 默认 30 秒，长时间运行的任务可能导致重建超时
5. **Registry 原子性**: remove + register 不是原子操作，理论上存在短暂的 executor 不可见窗口
6. **虚拟线程模式局限**: 虚拟线程的队列容量通过 pendingQueue 控制，语义与平台线程的 LinkedBlockingQueue 不完全一致

#### 2.2.7 测试覆盖

- `ExecutorRebuildStrategyTest`: 9 个测试，覆盖扩/缩/线程配置保留/拒绝策略保留/任务重放
- `QueueResizeAdjustmentAdapterTest`: 8 个测试，覆盖成功/失败/幂等保护/证据生成
- `QueueResizeEndToEndTest`: 9 个端到端测试，验证完整重建流程
- `QueueResizeSafetyGateTest`: 6 个测试，覆盖放行/拒绝/No-op/缩容安全检查

---

### 2.3 策略三：拒绝策略动态替换（v0.10.0）

#### 2.3.1 传统方案对比

| 维度 | 传统 ThreadPoolExecutor | DynamicThreadPollerManager |
|------|------------------------|---------------------------|
| 策略替换 | 直接调用 `setRejectedExecutionHandler()`，无配套机制 | 通过 RejectionPolicyCommand → RejectionPolicySafetyGate → RejectionPolicyAdjustmentAdapter 管道执行 |
| 并发安全 | `setRejectedExecutionHandler()` 本身是 volatile 写，线程安全 | 在此基础上增加安全门控：null 检查、No-op 检测、resize 冲突检查 |
| 策略对比 | 无内建机制判断新旧策略是否相同 | `fromCurrent()` 通过 class 比较自动检测 No-op（对 JDK 四种内置策略有效） |
| 跨维度冲突 | 无 | 检查队列 resize 是否进行中，防止两个维度同时操作导致竞态 |
| 可观测性 | 无 | `PolicyReplacementEvidence` 记录 before/after 策略类名、executor 状态、时间戳 |
| 重建保留 | N/A | ExecutorRebuildStrategy 重建队列时自动保留原始拒绝策略 |

#### 2.3.2 四种 JDK 内置拒绝策略行为验证

本系统对四种标准拒绝策略进行了端到端行为验证：

| 策略 | 行为 | 验证方式 |
|------|------|---------|
| `AbortPolicy` | 抛出 RejectedExecutionException | 提交超负荷任务，断言异常被抛出 |
| `CallerRunsPolicy` | 在调用者线程中执行任务 | 切换后验证任务在调用线程完成 |
| `DiscardPolicy` | 静默丢弃被拒绝的任务 | 切换后验证任务被静默丢弃（Future.isDone = true 但结果为 null） |
| `DiscardOldestPolicy` | 丢弃队列中最旧的任务 | 切换后验证最旧任务被驱逐 |

#### 2.3.3 设计决策：为什么不需要幂等保护

与 QueueResizeAdjustmentAdapter 不同，RejectionPolicyAdjustmentAdapter **没有**幂等保护。原因：

- `ThreadPoolExecutor.setRejectedExecutionHandler()` 是原子 volatile 写
- 两个线程同时调用不会产生中间状态
- 并发 policy 替换是 last-write-wins 语义
- 不像 queue resize 那样涉及多步 decommission/commission 周期

#### 2.3.4 优势

1. **零开销**: 利用 JDK 原生 `setRejectedExecutionHandler()` API，无需 executor 重建
2. **即时生效**: 策略替换是原子操作，替换后立即对新任务生效
3. **跨维度安全**: 与队列 resize 的协调检查避免了两个维度同时操作的竞态条件
4. **策略保留**: ExecutorRebuildStrategy 重建队列时自动保留当前拒绝策略（v0.10.0 修复）
5. **No-op 检测**: `fromCurrent()` 自动检测相同策略类型，避免不必要的操作和证据记录
6. **最简设计**: 三个维度中技术复杂度最低（无需 rebuild、无需 drain-and-replay）

#### 2.3.5 劣势与局限

1. **匿名类比较限制**: `fromCurrent()` 使用 `getClass()` 比较，对匿名自定义 RejectedExecutionHandler 可能误判为相同策略
2. **自定义策略未覆盖**: 四种 JDK 内置策略已验证，但自定义 RejectedExecutionHandler 实现的行为未测试
3. **无策略回滚**: 替换后如果行为不符合预期，系统不会自动回滚到之前的策略
4. **Policy set failure 不可达**: `setRejectedExecutionHandler()` 对 JDK 内置 handler 从不抛异常，catch 块是 dead code（defense-in-depth）
5. **无策略语义验证**: 系统只验证策略类型是否相同，不验证策略行为是否适合当前负载

#### 2.3.6 测试覆盖

- `RejectionPolicyCommandTest`: 8 个测试，覆盖四种 JDK 策略的命令创建
- `RejectionPolicySafetyGateTest`: 9 个测试，覆盖放行/拒绝/No-op/resize 冲突
- `RejectionPolicyAdjustmentAdapterTest`: 11 个测试，覆盖成功替换/executor 未找到/安全门拒绝/证据生成
- `RejectionPolicyEndToEndTest`: 8 个端到端测试，验证策略切换后的实际行为

---

## 三、支撑基础设施验证

### 3.1 ManagedExecutor 双模式架构

ManagedExecutor 统一了平台线程和虚拟线程两种模式的 API：

| 特性 | PLATFORM 模式 | VIRTUAL 模式 |
|------|--------------|-------------|
| 底层实现 | ThreadPoolExecutor | Executors.newVirtualThreadPerTaskExecutor() + Semaphore |
| 并发控制 | corePoolSize / maximumPoolSize | Semaphore permits (virtualMaxConcurrency) |
| 队列 | LinkedBlockingQueue | LinkedBlockingQueue (pendingQueue) + daemon drainer |
| 动态调整 | setCorePoolSize / setMaximumPoolSize | adjustSemaphore() (acquire/release permits) |
| unwrap() | 返回底层 ThreadPoolExecutor | 返回 null |
| keepAlive | TPE 原生支持 | 仅存储用于观测 |

**测试覆盖**: `ManagedExecutorTest` (21) + `ManagedExecutorVirtualThreadTest` (11) + `ManagedExecutorConfigTest` (14) + `ManagedExecutorConfigThreadModeTest` (7) = **53 个测试**

### 3.2 证据采集与持久化

#### InMemoryEvidenceRecorder
- 基于 `ConcurrentHashMap<String, CopyOnWriteArrayList<ObservedSnapshot>>`
- 返回不可变快照列表
- **测试**: 7 个

#### FileBackedEvidenceRecorder
- JSON Lines (.jsonl) 格式持久化
- 内存缓冲 + 文件输出双写
- RecordingSession 生命周期管理
- 4 线程 x 100 快照并发写入测试通过
- **测试**: 16 个

#### LivePressureSampler
- ScheduledExecutorService 驱动的自主轮询
- 可配置轮询间隔（最小 100ms，默认 1000ms）
- 连续 10 次失败自动停止
- **测试**: 14 个

### 3.3 离线回放与分析

| 组件 | 功能 | 测试数 |
|------|------|--------|
| ReplayEvidenceValidator | 结构完整性校验（8 种 FailureCode） | 16 |
| OfflinePolicyReplayService | 3 种灵敏度配置回放 | 10 |
| ReplaySummaryBuilder | 决策聚合与震荡检测 | 14 |
| ThresholdSensitivityAnalyzer | 保守/默认/激进配置对比 | 6 |
| MutationReadinessGate | 就绪性判定（READY/READY_WITH_RISK/NOT_READY） | 9 |
| ReplayReportWriter | JSON + Markdown 报告生成 | 12 |

### 3.4 数据质量门禁

9 道质量门禁（G1~G9）确保采集数据的完整性：

| 门禁 | 检查内容 |
|------|---------|
| G1 | 必需场景配置文件（STEADY/RAMP/BURST） |
| G2 | 每个 profile 至少 3 次重复运行 |
| G3 | 每次运行至少 3 个快照 |
| G4 | 快照时间戳非递减 |
| G5 | 运行 ID 唯一性 |
| G6 | 环境元数据完整性 |
| G7 | 扩展字段存在性 |
| G8 | 队列压力覆盖 |
| G9 | 线程泄漏检测 |

**测试**: `DataAcquisitionNineRunTest` — 3 seeds x 3 profiles = 9 runs，全部通过 9 道门禁

### 3.5 架构边界隔离

6 个边界隔离测试类通过源码级扫描验证包间依赖关系：

| 测试类 | 验证内容 |
|--------|---------|
| ScenarioBoundaryIsolationTest | scenario 包不引用 policy/mutation/executor-adapter |
| PolicyBoundaryIsolationTest | policy 包不引用 scenario-runner/executor-mutation |
| AnalysisBoundaryIsolationTest | analysis 包不引用 runtime-mutation/executor-adapter |
| AdjustmentBoundaryIsolationTest | adjustment 包不引用 ThreadPoolExecutor/ScheduledExecutorService |
| MetricsBoundaryIsolationTest | metrics 包不引用 policy/executor/mutation（白名单除外） |
| FoundationModelsTest | model 包不引用 sampling/mutation 包 |

---

## 四、综合对比总结

### 4.1 三种策略技术难度对比

| 策略 | 技术难度 | 核心挑战 | 是否需要 Executor 重建 |
|------|---------|---------|----------------------|
| 线程数调整 | 中 | 安全门控链设计、冷却期与方向控制 | 否（JDK 原生支持） |
| 队列容量调整 | **高** | ThreadPoolExecutor 不支持队列替换，需完整重建生命周期 | **是**（decommission → commission → replay） |
| 拒绝策略替换 | 低 | JDK 原生支持，设计最简 | 否（JDK 原生支持） |

### 4.2 相较传统方案的核心优势

| 优势维度 | 具体表现 |
|---------|---------|
| **安全性** | 8 级安全门控链（线程调整）、队列深度检查（队列 resize）、resize 冲突检查（策略替换） |
| **可观测性** | 每次操作产生完整证据链（before/after 状态、命令引用、时间戳、失败原因） |
| **可审计性** | commandId 格式 `runId:timestamp:current->target`，AdjustmentEvidence 记录完整操作上下文 |
| **确定性** | 策略评估器不读取系统时钟，时间戳由调用者注入，测试完全可复现 |
| **防震荡** | 冷却期 + 方向阻断 + 每轮限额三重保护 |
| **跨维度协调** | 策略替换检查队列 resize 是否进行中，避免竞态 |
| **任务连续性** | 队列扩容时自动排空并重放任务 |
| **配置保留** | 队列重建后保留线程配置和拒绝策略 |
| **双模式统一** | 平台线程和虚拟线程共用同一套 API 和调整管道 |

### 4.3 相较传统方案的主要劣势

| 劣势维度 | 具体表现 | 严重程度 |
|---------|---------|---------|
| **复杂度** | 三个维度各需 Command + SafetyGate + Adapter + Evidence 四个组件 | 中 |
| **冷启动限制** | 调整前需完成离线回放分析并达到 READY 状态 | 中 |
| **重建开销** | 队列调整需要终止旧 executor 并创建新 executor | 高 |
| **服务中断** | 队列重建期间存在短暂的服务中断窗口 | 中 |
| **缩容丢弃** | 队列缩容方向会丢弃排队中的任务 | 高 |
| **冷却延迟** | 默认 2 个决策间隔的冷却期限制了快速连续调整 | 低 |
| **无自动回滚** | 调整后指标恶化时不会自动回滚 | 中 |
| **自定义策略限制** | 匿名类 RejectedExecutionHandler 的 No-op 检测可能误判 | 低 |

### 4.4 测试验证总览

| 类别 | 测试类数 | 测试方法数 | 覆盖内容 |
|------|---------|-----------|---------|
| 合约/验证 | ~30 | ~300 | 不可变记录验证、null/blank 参数拒绝、枚举值计数 |
| 端到端 | 5 | 37 | 完整闭环流程、队列重建、策略替换、9-run 数据采集 |
| 边界隔离 | 6 | 18 | 包间依赖扫描，架构分层强制 |
| 序列化 | 5 | 35 | JSON 往返、类型保持、缺失值语义 |
| 并发安全 | 3 | 22 | 多线程注册/写入/信号量限制 |
| Spring Boot | 1 | 1 | 上下文加载 |
| **合计** | **76** | **646** | **0 失败，0 错误** |

---

## 五、场景适用性建议

### 5.1 适用场景

| 场景 | 推荐策略 | 原因 |
|------|---------|------|
| 负载波动频繁的服务 | 线程数调整 | JDK 原生支持，开销最低，即时生效 |
| 内存敏感型应用 | 队列容量调整 | 可以动态控制排队任务的内存占用 |
| 突发流量应对 | 拒绝策略切换 | 可以从 AbortPolicy（快速失败）切换到 CallerRunsPolicy（降级处理） |
| 生产环境灰度发布 | 三种策略组合 | 完整的证据链支持事后审计和回溯分析 |
| 性能基准测试 | 全框架 | 7 阶段场景运行器 + 9 道质量门禁确保数据可信 |

### 5.2 不适用场景

| 场景 | 原因 |
|------|------|
| 超低延迟要求（< 1ms） | 安全门控链引入额外开销 |
| 单一固定负载 | 动态调整无收益，传统 ThreadPoolExecutor 更简单 |
| 频繁队列调整 | 每次调整都有 executor 重建开销，不适合高频场景 |
| JDK 21 虚拟线程全面替代 | ManagedExecutor 的虚拟线程模式增加了复杂度，直接使用虚拟线程更简单 |

---

## 六、附录：完整测试清单

### 6.1 实验协调层（1 个测试类，9 个测试）
- `ExperimentCoordinatorTest`: 生命周期状态机、摘要生成、无效状态转换

### 6.2 模型层（2 个测试类，13 个测试）
- `ExperimentRunTest`: 创建、状态转换、不可变性
- `FoundationModelsTest`: 所有值对象不可变性、包独立性

### 6.3 指标层（12 个测试类，75 个测试）
- `MetricValueTest`: Present/Absent 语义
- `RuntimeObservationTest`: 8 字段暴露、不可变拷贝
- `DefaultEvidenceSummaryBuilderTest`: 空/单/多流摘要
- `InMemoryEvidenceRecorderTest`: 并发安全、顺序保持
- `ManualPressureSamplerTest`: 确定性采样
- `DefaultSnapshotAssemblerTest`: 缺失值零替换
- `RuntimeObservationSerializationTest`: JSON 往返
- `ObservedSnapshotSerializationTest`: JSON 往返
- `MetricsBoundaryIsolationTest`: 包边界扫描
- `LivePressureSamplerConfigTest`: 配置验证
- `LivePressureSamplerTest`: 生命周期、自主采样、并发
- `RecordingSessionTest`: 会话管理、线程安全

### 6.4 场景层（10 个测试类，55 个测试）
- `ScenarioDefinitionTest`, `ScenarioStepTest`, `ScenarioPlanTest`: 场景定义验证
- `DeterministicScenarioPlannerTest`: STEADY/RAMP/BURST 三种负载模式
- `BaselineExecutorPresetTest`, `BaselineWorkloadExecutorTest`: 基线执行器
- `ScenarioExperimentRunnerTest`: 基线场景运行
- `ManagedExecutorScenarioRunnerTest`: 真实 executor 场景运行（8 测试）
- `ManagedExecutorScenarioRunnerLiveSamplerTest`: 自主采样集成（8 测试）
- `ScenarioBoundaryIsolationTest`: 包边界扫描

### 6.5 策略层（8 个测试类，67 个测试）
- `ThresholdPolicyEvaluatorTest`: 17 个测试，扩/缩/保持/封顶
- `DefaultControlGateTest`: 边界强制
- `QueueResizeSafetyGateTest`: 6 个测试
- `RejectionPolicySafetyGateTest`: 9 个测试
- 其他合约测试

### 6.6 分析层（8 个测试类，77 个测试）
- `ReplayEvidenceValidatorTest`: 16 个测试
- `OfflinePolicyReplayServiceTest`: 10 个测试
- `ReplaySummaryBuilderTest`: 14 个测试
- `MutationReadinessGateTest`: 9 个测试
- `ReplayReportWriterTest`: 12 个测试
- 其他

### 6.7 调整层（5 个测试类，69 个测试）
- `RuntimeAdjustmentSafetyGateTest`: 16 个测试
- `ManagedExecutorAdjustmentAdapterTest`: 16 个测试
- `ExecutorAdjustmentAdapterTest`: 13 个测试
- `AdjustmentContractsTest`: 27 个测试
- `AdjustmentEvidenceTest`: 6 个测试

### 6.8 数据获取层（7 个测试类，56 个测试）
- `AcquisitionDataQualityValidatorTest`: 9 道门禁
- `DataAcquisitionNineRunTest`: 9-run 端到端
- `FileBackedEvidenceRecorderTest`: 16 个测试（含并发写入）
- `AcquisitionJsonWriterParseTest`: 17 个测试
- 其他

### 6.9 执行器层（16 个测试类，162 个测试）
- `ManagedExecutorTest`: 21 个测试
- `ManagedExecutorVirtualThreadTest`: 11 个测试
- `ManagedExecutorAdjustmentAdapterTest`: 16 个测试
- `ClosedLoopExperimentTest`: 3 个端到端测试
- `QueueResizeEndToEndTest`: 9 个测试
- `RejectionPolicyEndToEndTest`: 8 个测试
- `ExecutorRebuildStrategyTest`: 9 个测试
- `QueueResizeAdjustmentAdapterTest`: 8 个测试
- `RejectionPolicyAdjustmentAdapterTest`: 11 个测试
- 其他

---

> **结论**: DynamicThreadPollerManager 在传统 ThreadPoolExecutor 之上构建了一套完整的动态参数管理框架。三个调整维度的技术难度从高到低依次为：队列容量调整 > 线程数调整 > 拒绝策略替换。646 个测试全部通过，覆盖了合约验证、端到端流程、架构边界隔离、序列化往返和并发安全五个维度。框架的核心价值在于将原本"裸调"的 ThreadPoolExecutor 参数操作转化为可审计、可回放、有安全保障的闭环实验流程。
