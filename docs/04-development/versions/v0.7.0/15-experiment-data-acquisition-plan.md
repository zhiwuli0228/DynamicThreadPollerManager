# v0.7.0 ManagedExecutor 实验数据获取方案

## Header

- Document type: experiment data acquisition plan
- Version name: `v0.7.0`
- Status: `DRAFT`
- Authoring date: `2026-06-12`
- Execution status: not authorized from this document alone; use `current-state.md` for the active authorization boundary
- Authoritative branch: `claude_master`

## 1. 背景与动机

### 1.1 v0.6.0 数据获取基线回顾

v0.6.0 建立了完整的数据获取流水线：

```
ScenarioExperimentRunner
  → BaselineWorkloadExecutor (无真实线程，同步计数)
    → ManualPressureSampler (确定性快照组装)
      → EvidenceRecorder (证据记录)
        → AcquisitionReportWriter (5 个 JSON artifact)
          → AcquisitionDataQualityValidator (G1-G6 门禁)
            → AcquisitionReadinessClassifier (就绪判定)
```

这条流水线已经产出合格的实验数据和报告 artifact，但存在一个结构性限制：**`BaselineWorkloadExecutor` 不拥有真实线程**。

| 指标 | BaselineWorkloadExecutor | 真实 ThreadPoolExecutor |
|---|---|---|
| `activeThreads()` | 恒为 0 | 实际活跃线程数 |
| `queueSize()` | 恒为 0 | 阻塞队列实际深度 |
| `poolSize()` | 返回 preset 固定值 | 当前池内线程数 |
| `completedTaskCount()` | 等于累计 workUnits | 实际完成任务数 |
| 任务执行方式 | 调用线程同步执行 | 池内线程异步执行 |

这些数据对于验证实验基础设施的"管道连通性"足够，但对于验证 **policy 在真实线程池压力下的决策有效性** 不够。

### 1.2 v0.7.0 新增能力

v0.7.0 交付了以下关键能力：

- `ManagedExecutor` — ThreadPoolExecutor 的受控包装
- `ExecutorRegistry` — 命名注册表
- `ManagedExecutorAdjustmentAdapter` — ScaleAdjustmentCommand 到真实 TPE 的桥接
- `ExecutorStateSnapshot` 扩展 — 5 个来自真实 TPE 的新增字段（poolSize, completedTaskCount, keepAliveTimeSeconds, largestPoolSize, taskCount）
- `ClosedLoopExperimentTest` — 首次端到端闭环实验（参考实现）

### 1.3 本方案目标

在 **真实 ManagedExecutor（ThreadPoolExecutor）** 上运行 STEADY/RAMP/BURST 三种场景，
采集真实线程池压力数据，通过数据质量门禁，产出可复现的实验报告。

**不在此方案范围**：
- 自动化闭环调度器
- 生产环境集成
- 性能优化声明
- Queue resizing

## 2. 架构设计

### 2.1 总体方案：新增 ManagedExecutorScenarioRunner

不在现有 `ScenarioExperimentRunner` 上修改，而是新建一个专门的 runner。理由：

1. `ScenarioExperimentRunner` 与 `BaselineWorkloadExecutor` 紧耦合（`buildObservation` 直接调用 `baselineExecutor.activeThreads()` 等方法）
2. 真实 ManagedExecutor 场景需要完全不同的执行模型：提交阻塞任务 → 等待线程启动 → 采样实时状态 → 释放任务 → 清理
3. 避免影响 v0.3.0-v0.6.0 既有的 0-regression 保证

### 2.2 执行流程

```
ManagedExecutorScenarioRunner.run(scenarioDefinition, managedExecutorConfig)
  │
  ├── 1. 创建 ManagedExecutor(core, max, keepAlive, queue)
  ├── 2. 注册到 ExecutorRegistry
  ├── 3. 创建 ManagedExecutorAdjustmentAdapter
  ├── 4. 协调 ExperimentCoordinator 生命周期 (create → start → stop → finalize)
  │
  ├── 5. For each step in DeterministicScenarioPlanner.plan(definition):
  │       ├── Phase A: 按 profile 规则提交阻塞任务到 ManagedExecutor
  │       ├── Phase B: 等待线程调度生效 (短 sleep 或 countDown 前采样)
  │       ├── Phase C: 通过 adapter.currentState() 读取真实 TPE 状态
  │       ├── Phase D: 构建 RuntimeObservation + ObservedSnapshot
  │       ├── Phase E: EvidenceRecorder.record(snapshot)
  │       └── Phase F: 释放已提交任务 (countDown 对应 latch)
  │
  ├── 6. 关闭并终止 ManagedExecutor
  ├── 7. 从 Registry 移除
  └── 8. 返回 ScenarioRunOutcome
```

### 2.3 与 ClosedLoopExperimentTest 的关系

`ClosedLoopExperimentTest.fullClosedLoopScaleUp()` 是本方案的核心参考实现。关键复用的模式：

| 模式 | ClosedLoopExperimentTest | 本方案 |
|---|---|---|
| 创建执行器 | `new ManagedExecutor(2, 4, 60, SECONDS, new LinkedBlockingQueue<>(10))` | 同，参数化 |
| 提交阻塞任务 | `executor.submit(() -> await(blocker))` | 同，按 profile 规则控制数量 |
| 状态采样 | `adapter.currentState()` | 同，每 step 采样一次 |
| 构建快照 | `new PressureSnapshot(now, active, pool, queue, completed, 0.0)` | 同 |
| 释放任务 | `blocker.countDown()` | 同（每 step 独立 latch） |
| 清理 | `shutdown/awaitTermination` + latch 先释放 | 严格遵守 P6 修复后的顺序 |

### 2.4 核心组件

```
ManagedExecutorScenarioRunner
  - ExperimentCoordinator coordinator
  - ScenarioPlanner planner (复用 DeterministicScenarioPlanner)
  - Supplier<Instant> clock
  - PressureSampler sampler (复用 ManualPressureSampler / DefaultSnapshotAssembler)
  - EvidenceRecorder recorder

ManagedExecutorConfig (新 record)
  - int corePoolSize
  - int maximumPoolSize
  - int queueCapacity
  - long keepAliveTime
  - TimeUnit keepAliveTimeUnit
```

## 3. 场景矩阵

### 3.1 ManagedExecutor 配置

| 参数 | 值 | 说明 |
|---|---|---|
| `corePoolSize` | 2 | 基线核心线程数 |
| `maximumPoolSize` | 4 | 最大线程数 |
| `queueCapacity` | 10 | 阻塞队列容量 |
| `keepAliveTime` | 60 | 空闲线程存活时间 |
| `keepAliveTimeUnit` | `SECONDS` | — |

### 3.2 Profile 工作量规则

每个 profile 在每一步的 **任务提交数** 和 **采样时机** 不同：

| Profile | Step 任务数 | 说明 |
|---|---|---|
| `STEADY` | 每步固定 2 个阻塞任务 | 模拟稳定负载，观察 core 线程饱和状态 |
| `RAMP` | 第 i 步提交 `2 + i` 个任务（cap 到 queueCapacity+max） | 逐步增加压力，观察队列堆积增长 |
| `BURST` | `i % 3 == 0` 时提交 6 个任务，否则 2 个 | 周期性突发压力，观察队列深度震荡 |

任务等待策略：
- 每个 step 使用独立的 `CountDownLatch(1)`
- 提交任务后 `Thread.sleep(100)` 等待线程调度生效
- 采样后 `latch.countDown()` 释放该步所有任务
- 全部释放后等待 executor 回到空闲状态再进入下一步

### 3.3 Seed 与重复

| Profile | Seeds | Runs | 总 run 数 |
|---|---|---|---|
| `STEADY` | `101`, `102`, `103` | 3 | 3 |
| `RAMP` | `201`, `202`, `203` | 3 | 3 |
| `BURST` | `301`, `302`, `303` | 3 | 3 |

**合计：9 个 run**，满足 G2（每个 profile 至少 3 个 run）。

### 3.4 Step 配置

| Profile | stepCount | 说明 |
|---|---|---|
| `STEADY` | 8 | 8 步 × 2 任务 = 16 个任务采样点 |
| `RAMP` | 8 | 任务数递增：2, 3, 4, 5, 6, 7, 8, 9 |
| `BURST` | 9 | 3 个 burst 周期 × 3 步，burst 步 6 任务，普通步 2 任务 |

`baseWorkUnits` 在真实执行器模式下不直接使用（任务执行时长由 latch 等待时间控制），保留在 `ScenarioDefinition` 中以保持兼容。

## 4. 指标采集

### 4.1 每步采集指标

通过 `adapter.currentState()` → `ExecutorStateSnapshot` 获取：

| 指标 | 来源 | 类型 |
|---|---|---|
| `observedAt` | `Instant.now()` | `Instant` |
| `corePoolSize` | `executor.getCorePoolSize()` | `int` |
| `maximumPoolSize` | `executor.getMaximumPoolSize()` | `int` |
| `activeCount` | `executor.getActiveCount()` | `Integer` (non-null) |
| `poolSize` | `executor.getPoolSize()` | `Integer` (non-null) |
| `queueSize` | `executor.getQueueSize()` | `Integer` (non-null) |
| `queueCapacity` | 预设值 | `Integer` |
| `completedTaskCount` | `executor.getCompletedTaskCount()` | `Long` (non-null) |
| `keepAliveTimeSeconds` | `executor.getKeepAliveTime(SECONDS)` | `Long` (non-null) |
| `largestPoolSize` | `executor.getLargestPoolSize()` | `Integer` (non-null) |
| `taskCount` | `executor.getTaskCount()` | `Long` (non-null) |

### 4.2 RuntimeObservation 映射

```
RuntimeObservation(
    timestamp       → Instant.now()
    activeThreads   → ExecutorStateSnapshot.activeCount()
    poolSize        → ExecutorStateSnapshot.poolSize()
    queueSize       → ExecutorStateSnapshot.queueSize()
    completedTaskCount → ExecutorStateSnapshot.completedTaskCount()
    cpuUtilization  → MetricValue.absent()  (无真实 CPU 采样)
)
```

### 4.3 Run metadata

每个 run 记录：

- `version`: `"v0.7.0"`
- `runId`: UUID
- `scenarioId`: 来自 ScenarioDefinition
- `scenarioProfile`: STEADY / RAMP / BURST
- `seed`: 固定 seed
- `stepCount`: 实际步数
- `managedExecutorConfig`: core/max/queue/keepAlive
- `environmentSummary`: os / java / cpu / memory
- `commandLine`: 运行命令
- `createdAt`: 创建时间

## 5. 数据质量门禁

### 5.1 复用 v0.6.0 门禁 (G1-G6)

| Gate | 规则 | 阻塞级别 |
|---|---|---|
| G1 | 必须覆盖 STEADY / RAMP / BURST 三种 profile | P0 |
| G2 | 每个 profile 至少 3 个有效 run | P0 |
| G3 | 每个 run 至少 3 个 snapshot | P0 |
| G4 | 同一 run 内 snapshot timestamp 非降序 | P0 |
| G5 | 同一 run 内所有 snapshot `runId` 一致 | P0 |
| G6 | run metadata 完整（scenario, seed, preset, environment） | P0 |

### 5.2 v0.7.0 新增门禁 (G7-G9)

| Gate | 规则 | 阻塞级别 |
|---|---|---|
| G7 | 每个 snapshot 的 `activeCount` / `poolSize` / `queueSize` / `completedTaskCount` / `keepAliveTimeSeconds` / `largestPoolSize` / `taskCount` 均 non-null | P0 |
| G8 | 每个 run 至少 1 个 snapshot 的 `queueSize > 0`（证明真实队列压力被采样到） | P1 |
| G9 | 每个 run 的 ManagedExecutor 在 `@AfterEach` 或 equivalent cleanup 中完成 shutdown + awaitTermination，无线程泄漏 | P0 |

**G7 说明**：v0.7.0 的 `ExecutorStateSnapshot` 扩展了 5 个来自真实 TPE 的字段。在 `InMemoryAdjustableExecutorProbe` 时代这些字段为 null；在 ManagedExecutor 上它们必须 non-null。

**G8 说明**：如果所有 snapshot 的 `queueSize == 0`，说明任务没有产生队列压力，采样时机可能有误（例如在任务完成之后才采样）。

**G9 说明**：线程泄漏验证是 v0.7.0 回溯教训 P6 的直接产物。每个 run 结束后必须确认 executor 已终止。

## 6. 输出 artifact

### 6.1 输出目录

```
outputs/reports/v0.7.0/
```

### 6.2 文件清单

| Artifact | 文件名 | 版本控制 | 内容 |
|---|---|---|---|
| Run Manifest | `<runId>-run-manifest.json` | yes | run 标识、scenario、seed、preset、environment、commandLine |
| Pressure Summary | `<runId>-pressure-summary.json` | yes | 每 run 的摘要指标（snapshot 数、queue depth min/max/mean、activeCount 峰值） |
| Replay Summary | `<runId>-replay-summary.json` | yes | 离线策略回放结果（如执行 replay） |
| Readiness Summary | `<runId>-readiness-summary.json` | yes | 就绪判定结论、原因、阻塞项 |
| Evidence Index | `<runId>-evidence-index.json` | yes | 所有 artifact 路径索引 + retention 记录 |
| Raw Snapshots | `raw-snapshots-<runId>.jsonl` | **no** | 原始 snapshot evidence，纳入 `.gitignore` |
| Composite Report | `<runId>-acquisition-report.md` | yes | 人类可读的综合报告 |

### 6.3 复用现有 writer

`AcquisitionReportWriter` 及其 `writeAll()` 方法在以下条件下可直接复用：

- 需要将 `OUTPUT_DIRECTORY` 从 `outputs/reports/v0.6.0` 更新为 `outputs/reports/v0.7.0`
- 或新建 `AcquisitionReportPaths` 子类/参数化版本标签

建议方案：在 `AcquisitionReportPaths` 中新增 `forVersion(String)` 静态工厂，返回版本化路径；保留 `v0.6.0` 默认值以保证向后兼容。

## 7. 执行计划

### 7.1 代码变更清单

| # | 变更 | 类型 | 说明 |
|---|---|---|---|
| 1 | 新增 `ManagedExecutorConfig` record | 新文件 | 封装 ManagedExecutor 创建参数 |
| 2 | 新增 `ManagedExecutorScenarioRunner` | 新文件 | 核心 runner，上述 2.2 执行流程 |
| 3 | 新增 `ManagedExecutorScenarioRunnerTest` | 新测试 | runner 的集成测试（3 profile × 1 seed 最小验证） |
| 4 | 修改 `AcquisitionReportPaths` | 修改 | 支持版本化输出目录 |
| 5 | 新增数据获取执行 main/CLI | 可选 | 命令行入口，方便非 IDE 环境下执行（视需要决定） |

### 7.2 执行前置条件

1. `docs/00-project/current-state.md` 授权进入实现阶段
2. 创建对应 OpenSpec change（`schema: superspec`）
3. `mvn test` 全量通过（412 tests, 0 failures）作为基线
4. `.gitignore` 已包含 `outputs/`（已在 v0.7.0 P4 修复中完成）

### 7.3 执行步骤

```
Phase 1: 设计与代码
  1. 实现 ManagedExecutorConfig
  2. 实现 ManagedExecutorScenarioRunner
  3. 更新 AcquisitionReportPaths（版本化输出目录）
  4. 编写 ManagedExecutorScenarioRunnerTest

Phase 2: 数据获取
  5. 运行 9 个 run（3 profiles × 3 seeds）
  6. 每个 run 产出 5 个 JSON artifact + raw snapshots
  7. 运行 AcquisitionDataQualityValidator（G1-G9）
  8. 运行 AcquisitionReadinessClassifier

Phase 3: 验证
  9. 全量 mvn test 确认零回归
  10. 手动检查至少 1 个 BURST run 的 queueSize time series
  11. 人工确认报告 artifact 可读
```

### 7.4 预计产出

| 产出 | 数量 |
|---|---|
| JSON artifact 文件 | 9 runs × 5 = 45 个 |
| Raw snapshot JSONL | 9 个文件（不入版本控制） |
| Composite report MD | 9 个 |
| 新增 Java 源文件 | ~2-3 个 |
| 新增 Java 测试文件 | ~1 个 |
| 测试增量 | 估计 9-12 个新测试 |

## 8. 风险与缓解

### 8.1 线程调度不确定性

**风险**: `Thread.sleep(100)` 不保证线程已启动并进入等待状态，采样时 activeCount 可能 < 预期值。

**缓解**:
- 不要求精确的 activeCount 值，只要求采集到非零值
- G8 只要求至少 1 个 snapshot 有 queue pressure，不要求所有 snapshot
- 在测试中使用 CountDownLatch 而非 sleep 做同步屏障

### 8.2 测试超时

**风险**: 真实线程池的 shutdown + awaitTermination 可能在 CI 环境超时。

**缓解**:
- 每个 step 的任务通过独立 latch 释放，确保任务不长时间阻塞
- awaitTermination 超时设为 10 秒（v0.6.0 为 5 秒，v0.7.0 任务量更大）
- @AfterEach 先 countDown 再 shutdown（遵循 P6 修复）

### 8.3 队列容量限制

**风险**: RAMP 场景后期 `2+i` 超过 `queueCapacity + maximumPoolSize`，导致 `RejectedExecutionException`。

**缓解**:
- RAMP 任务提交数 cap 在 `queueCapacity + maximumPoolSize` 以内
- 或使用 `LinkedBlockingQueue` 的无界/大容量变体
- SR 阶段确认具体 capping 策略

### 8.4 数据量与磁盘占用

**风险**: 9 个 run × 每 run ~72 snapshot (8-9 step × ~8) 产生大量 raw evidence。

**缓解**:
- raw snapshot JSONL 不入版本控制
- 在 `AcquisitionReportWriter` 中控制 summary 聚合粒度
- 数据清理策略在 SR 阶段确认

## 9. 与现有测试的关系

### 9.1 不修改的代码

- `ScenarioExperimentRunner` — 保持不变
- `BaselineWorkloadExecutor` — 保持不变
- `ManualPressureSampler` — 保持不变（复用）
- `DefaultSnapshotAssembler` — 保持不变（复用）
- `EvidenceRecorder` — 保持不变（复用）
- `DeterministicScenarioPlanner` — 保持不变（复用）
- 所有现有测试文件 — 保持不变

### 9.2 新增依赖

- `ManagedExecutorScenarioRunner` → `ManagedExecutor`, `ExecutorRegistry`, `ManagedExecutorAdjustmentAdapter`（v0.7.0 已存在）
- 不引入新的外部依赖

## 10. 决策记录

### D1: 新 runner vs 修改现有 runner

**决策**: 新增 `ManagedExecutorScenarioRunner`，不修改 `ScenarioExperimentRunner`。

**理由**:
- `ScenarioExperimentRunner` 与 `BaselineWorkloadExecutor` 紧耦合
- 两种 runner 服务于不同的目的（基线验证 vs 真实数据获取）
- 零回归保证优先

### D2: 采样方式

**决策**: 通过 `adapter.currentState()` 采样，不直接访问 `executor.toSnapshot()`。

**理由**:
- `adapter.currentState()` 是 v0.7.0 的公共 API
- 确保采样路径与闭环实验一致
- 保持单一数据来源

### D3: 输出目录版本化

**决策**: v0.7.0 数据输出到 `outputs/reports/v0.7.0/`，与 v0.6.0 数据分开放置。

**理由**:
- v0.6.0 数据基于 BaselineWorkloadExecutor（无真实线程）
- v0.7.0 数据基于 ManagedExecutor（真实线程池）
- 分开存放便于对比验证和回归分析

### D4: Raw evidence 处理

**决策**: raw snapshot JSONL 默认不入版本控制，与 v0.6.0 方案一致。

**理由**:
- 数据量大，不适合进入 Git
- summary JSON 已包含足够的审计信息
- 如需保留 raw evidence，通过 `RetentionRecord` 记录位置

## 11. 待确认事项（SR 阶段）

1. **AcquisitionReportPaths 版本化方案**: 是新增 `forVersion(String)` 工厂方法，还是直接修改常量？
2. **RAMP capping 策略**: 任务数上限的具体值和处理方式（cap vs 增大队列容量）。
3. **是否需要 CLI/main 入口**: 用于非 IDE 环境下的数据获取执行。
4. **数据清理自动化**: 是否需要脚本自动清理超过 N 天的 raw evidence。
5. **BURST 参数**: burst 步 6 个任务的合理性（`queueCapacity=10, max=4` 时 6 < 10+4 安全）。

## 12. 禁止事项

- 不得修改 `ScenarioExperimentRunner` 或 `BaselineWorkloadExecutor`
- 不得在生产环境运行数据获取
- 不得在数据获取过程中执行 queue resizing
- 不得把 raw evidence 大文件默认提交到仓库
- 不得在没有 OpenSpec change 的情况下新增 runner 实现代码
- 不得修改现有的 412 个测试或降低测试覆盖
