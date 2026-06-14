# v0.12.0 目标与范围

## Header

- Version name: `v0.12.0`
- Status: `DRAFT`
- Current phase: `VERSION_DESIGN_DRAFT`
- Requirement theme: baseline catalog, comparable scenario runner, normalized result model, comparison report artifact

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
| v0.10.0 | 拒绝策略动态替换（RejectionPolicyCommand, RejectionPolicyAdjustmentAdapter, PolicyReplacementEvidence） | IMPLEMENTED |
| v0.11.0 | 持久化证据录制与自主采样（FileBackedEvidenceRecorder, RecordingSession, LivePressureSampler） | IMPLEMENTED |

### 1.2 当前缺口

系统已具备独立运行 baseline executor 和 managed executor 的能力，但无跨 executor 类型的比较框架：

1. **无 executor baseline 目录** — 项目中有 `BaselineExecutorPreset` 和 `BaselineWorkloadExecutor`，但只能承载单个固定预设。没有注册表来定义和管理多个常见 thread-pool 配置（如 fixed、cached、work-stealing、single-threaded 等 JDK 标准预设），无法在一次实验运行中系统性地对比多种 executor 配置。

2. **无可比场景运行器** — `ScenarioExperimentRunner` 每次运行仅使用单一 `BaselineWorkloadExecutor`；`ManagedExecutorScenarioRunner` 每次运行仅使用单一 `ManagedExecutor`。不存在可以接受"同一 workload、多个 executor 类型"并产出可比较结果的运行器。无法回答"相同 workload 下 baseline 和 managed 哪个更好"。

3. **无归一化结果模型** — `ScenarioRunOutcome` 仅包含完成步数、工作单元数、快照数等简单字段。`PressureSnapshot` 和 `RuntimeObservation` 各包含 5-8 个指标字段，但命名和语义在不同 executor 类型间不统一（如 `BaselineWorkloadExecutor.completedTaskCount()` 返回 `completedWorkUnits` 而非真实 task count）。缺少跨 executor 类型可比较的归一化度量模型。

4. **无比较报告产物** — 现有报告体系（`AcquisitionReportWriter`、`ReplayReportWriter`）各自针对单一 executor 类型的数据获取或回放分析。不存在包含"baseline vs managed"对比维度、delta 计算、逐指标比较的报告格式。

5. **无法证明 managed executor 的优势** — roadmap 的核心战略问题是"managed executor 是否优于 common thread-pool baseline"，但当前工具链无法用记录证据回答此问题。

### 1.3 JDK API 可行性评估

v0.12.0 不引入新的 `ThreadPoolExecutor` 属性变更，无需 JDK API 评估。本版本聚焦于比较框架基础设施，不引入新的 executor mutation。

本版本需要创建的 JDK executor 类型均使用 `java.util.concurrent.Executors` 工厂方法和 `ThreadPoolExecutor` 公开构造函数，这些是 JDK 标准 API，无需额外评估。

### 1.4 与既有基础设施的关系

- `ScenarioExperimentRunner` 已实现"run scenario → sample → record"流程，v0.12.0 的 `ComparableScenarioRunner` 复用此模式并将其扩展为"run scenario against executor A → run same scenario against executor B → compare results"
- `BaselineExecutorPreset` 和 `BaselineWorkloadExecutor` 作为 baseline executor 的 concrete 实现，v0.12.0 引入 `BaselineExecutorCatalog` 管理多个 preset
- `ExperimentCoordinator` 管理 run 生命周期（create → start → stop → finalize），v0.12.0 扩展为支持"comparison run"（一对关联 run：baseline run + managed run）
- `AcquisitionJsonWriter` 提供手写 JSON 序列化基础设施，v0.12.0 复用实现比较报告 JSON 输出
- `FileBackedEvidenceRecorder` 提供持久化存储，comparison run 的 evidence 通过同一 recorder 持久化
- `EvidenceRecorder` 接口、`PressureSampler` 接口、`ObservedSnapshot` 模型保持不变

### 1.5 为什么是现在

- 三个动态配置维度 + 持久化证据 + 自主采样已全部到位，系统可以进行有意义的端到端比较
- 在没有比较框架之前，无法用数据证明"为什么要用 managed executor"
- roadmap 明确将 v0.12.0 定位为 evidence-first 序列的起点："prioritize comparison and measurement before adding more runtime knobs"
- `ScenarioExperimentRunner` 和 `ManagedExecutorScenarioRunner` 各自的独立运行能力已经过充分测试（646 tests）
- 比较框架是后续 pressure classification（v0.13.0）、adaptive closed-loop（v0.14.0）和 strategy explanation（v0.15.0）的前提 — 必须先有"比较基线"才能评估"闭环决策是否改善了结果"

## 2. 目标

`v0.12.0` 聚焦以下目标：

1. **BaselineExecutorCatalog**：注册和管理多个 common thread-pool 预设配置（fixed、cached、work-stealing、single 等 JDK 标准类型），支持按 policyId 查询
2. **CommonExecutorPreset**：定义通用 executor 预设的标准化描述（类型、参数、描述），可被 catalog 管理和 runner 消费
3. **ComparableScenarioRunner**：接受一个 workload scenario 和两个 executor 配置（baseline + managed），依次执行相同 scenario，产出可比较的两组结果
4. **NormalizedComparisonResult**：归一化度量模型，将不同 executor 类型的 observation 映射到统一指标（throughput、avg latency、queue depth、rejection count、completion count 等）
5. **ComparisonReportArtifact**：包含 baseline vs managed 并列对比、逐指标 delta、结论摘要的标准化报告产物
6. **端到端验证**：至少一个 concrete scenario 的 baseline vs managed 完整比较流程，产出持久化比较报告

## 3. 范围内

- `BaselineExecutorCatalog`（preset 注册、查询、默认预设集）
- `CommonExecutorPreset` record（presetId, executorType, corePoolSize, maxPoolSize, queueCapacity, description）
- `ComparableScenarioRunner`（接受 scenario + baseline config + managed config，依次运行并收集两组结果）
- `ComparisonResult` record（scenarioId, baselineRunId, managedRunId, baselineOutcome, managedOutcome, metricDeltas）
- `NormalizedComparisonMetrics` record（throughput, avgQueueDepth, maxQueueDepth, completionCount, rejectionCount, totalDurationMs 等归一化指标）
- `ComparisonReportArtifact` record（comparisonId, scenarioId, createdAt, baselinePreset, managedConfig, comparisonResult, conclusion）
- `ComparisonJsonWriter`（复用 `AcquisitionJsonWriter` 手写 JSON 模式，序列化比较报告）
- `ScenarioExperimentRunner` 扩展：支持 `ComparableScenarioRunner` 作为调用方
- `ManagedExecutorScenarioRunner` 集成：`ComparableScenarioRunner` 通过 `ManagedExecutorScenarioRunner` 运行 managed executor 侧
- 现有 646 测试零回归

## 4. 范围外

- 多场景批量比较（仅支持单场景 baseline vs managed）
- 多于 2 个 executor 的并行比较（仅 1:1 对比）
- 统计显著性检验（如 t-test、confidence intervals）
- 时间序列可视化或图表生成
- pressure classification（v0.13.0 候选）
- adaptive closed-loop adjustment（v0.14.0 候选）
- strategy explanation（v0.15.0 候选）
- CPU utilization 真实数据源（仍输出 `absent()`，DFR-01 延后）
- 跨 run 证据聚合（DFR-03 延后）
- 复杂 workload 类型（burst、long-tail、mixed、downstream-blocked — 候选 v0.16.0）
- CLI entry

## 5. 架构对齐

| 架构文档 | 本版本对应内容 |
|---|---|
| `observability-and-experiment-strategy.md` | "观察行为后再优化" — 比较框架提供观察能力，先于 v0.13.0 的 pressure classification 和 v0.14.0 的闭环调整 |
| `operational-and-evolution-boundaries.md` | 不引入新 executor mutation、不引入外部依赖、不扩展 Redis/Kafka/数据库边界 |
| `managed-executor-domain-model.md` | 扩展 `BaselineExecutorPreset` 为 catalog 模式，不修改 ManagedExecutor 自身 |
| roadmap.md | 实现候选 v0.12.0 的第一个可接受切片："prove at least one common thread-pool baseline and one managed-executor run can be compared under the same scenario with persisted, reviewable result artifacts" |

## 6. 模块边界

| 模块 | 变更类型 | 说明 |
|---|---|---|
| `experiment.scenario` | **新增** `CommonExecutorPreset` | 通用预设 record（类型、参数、描述） |
| `experiment.scenario` | **新增** `BaselineExecutorCatalog` | 预设注册表，管理多个 preset，含默认预设集 |
| `experiment.scenario` | **新增** `ComparableScenarioRunner` | 接受 scenario + two executor configs，依次执行并收集结果 |
| `experiment.scenario` | **新增** `ComparisonResult` | 包含两组 run 结果和逐 metric delta 的比较结果 |
| `experiment.scenario` | **新增** `NormalizedComparisonMetrics` | 跨 executor 类型归一化度量 record |
| `experiment.scenario` | **新增** `ComparisonReportArtifact` | 比较报告产物 record |
| `experiment.scenario` | **修改** `ScenarioExperimentRunner` | 添加 `ComparableScenarioRunner` 消费方法 |
| `experiment.acquisition` | **新增** `ComparisonJsonWriter` | 比较报告 JSON 序列化（手写 JSON，复用 AcquisitionJsonWriter 模式） |
| `experiment.acquisition` | **扩展** `AcquisitionReportPaths` | 增加 comparison 报告文件路径命名 |
| `experiment.model` | 不变 | ExperimentRun, PressureSnapshot 保持不变 |
| `experiment.metrics` | 不变 | EvidenceRecorder, PressureSampler, ObservedSnapshot 保持不变 |
| `experiment.executor` | 不变 | ManagedExecutor, ExecutorRegistry 保持不变 |

### 依赖方向

```text
experiment.scenario (CommonExecutorPreset, BaselineExecutorCatalog, ComparableScenarioRunner,
                     ComparisonResult, NormalizedComparisonMetrics, ComparisonReportArtifact)
    ├── experiment.acquisition (ComparisonJsonWriter extension, AcquisitionReportPaths extension)
    ├── experiment.model (ExperimentRun, PressureSnapshot — unchanged)
    ├── experiment.metrics (EvidenceRecorder, PressureSampler — unchanged)
    └── experiment.executor (ManagedExecutor, ManagedExecutorScenarioRunner — consumption only)
```

## 7. 核心技术设计

### 7.1 BaselineExecutorCatalog 设计

```java
public final class BaselineExecutorCatalog {
    private final Map<String, CommonExecutorPreset> presets;

    public BaselineExecutorCatalog();
    public void register(CommonExecutorPreset preset);
    public CommonExecutorPreset get(String presetId);
    public Set<String> presetIds();
    public int size();

    // 默认预设集
    public static BaselineExecutorCatalog withDefaults();
}
```

默认预设集包含至少以下 JDK 标准 executor 类型：

| Preset ID | 类型 | Core | Max | Queue | 描述 |
|---|---|---|---|---|---|
| `fixed-2` | FixedThreadPool | 2 | 2 | Unlimited | 基础固定线程池 |
| `fixed-4` | FixedThreadPool | 4 | 4 | Unlimited | 中等固定线程池 |
| `fixed-8` | FixedThreadPool | 8 | 8 | Unlimited | 大固定线程池 |
| `cached` | CachedThreadPool | 0 | Integer.MAX | Synchronous | 弹性缓存线程池 |
| `single` | SingleThreadExecutor | 1 | 1 | Unlimited | 单线程执行器 |
| `fixed-2-bounded` | FixedThreadPool | 2 | 2 | 10 | 固定线程 + 有界队列 |

### 7.2 CommonExecutorPreset 设计

```java
public record CommonExecutorPreset(
    String presetId,        // e.g. "fixed-4"
    String executorType,    // e.g. "FIXED_THREAD_POOL", "CACHED_THREAD_POOL", "SINGLE_THREAD_EXECUTOR"
    int corePoolSize,
    int maxPoolSize,
    int queueCapacity,      // -1 for unbounded (LinkedBlockingQueue default), 0 for SynchronousQueue
    String description
) {}
```

### 7.3 NormalizedComparisonMetrics 设计

归一化度量模型统一不同 executor 类型的观测数据为可比较的公共指标集：

```java
public record NormalizedComparisonMetrics(
    long completedTaskCount,       // 完成任务总数
    long rejectedTaskCount,        // 拒绝任务数（baseline executor 通常为 0）
    double avgQueueDepth,          // 平均队列深度
    int maxQueueDepth,             // 最大队列深度
    long totalDurationMs,          // 总运行时长 ms
    double throughputPerSecond,    // 吞吐量（tasks/second）= completedTaskCount / (totalDurationMs / 1000.0)
    int avgActiveThreads,          // 平均活跃线程数
    int maxPoolSize,               // 最大池大小
    int snapshotCount             // 采样快照数量
) {}
```

`ComparableScenarioRunner` 在每次 run 完成后，从 `recorder.snapshots(runId)` 计算以上归一化度量。

### 7.4 ComparableScenarioRunner 设计

```java
public final class ComparableScenarioRunner {
    private final ExperimentCoordinator coordinator;
    private final ScenarioPlanner planner;
    private final BaselineExecutorCatalog baselineCatalog;
    private final EvidenceRecorder baselineRecorder;
    private final EvidenceRecorder managedRecorder;
    private final Supplier<Instant> clock;
    private final ScenarioExperimentRunner baselineRunner;
    private final ManagedExecutorScenarioRunner managedRunner;

    // scenarioId: 要运行的 workload scenario
    // baselinePresetId: baseline executor catalog 中的 preset ID
    // managedConfig: managed executor 的配置
    public ComparisonResult compare(
            String scenarioId,
            String baselinePresetId,
            ManagedExecutorConfig managedConfig);
}
```

执行流程：

1. 从 catalog 获取 baseline preset → 创建 `BaselineWorkloadExecutor` (via `BaselineExecutorPreset`)
2. 从 catalog 获取 managed config → 创建/注册 `ManagedExecutor`
3. Phase 1: `baselineRunner.run(scenario, baselinePreset)` → 收集 baseline snapshots
4. Phase 2: `managedRunner.run(scenario, managedConfig)` → 收集 managed snapshots
5. Phase 3: 计算两组 `NormalizedComparisonMetrics`
6. Phase 4: 构建 `ComparisonResult`（含逐指标 delta）
7. Phase 5: 通过 `ComparisonJsonWriter` 写 `ComparisonReportArtifact`

### 7.5 ComparisonResult 和 ComparisonReportArtifact 设计

```java
public record ComparisonResult(
    String comparisonId,                        // UUID
    String scenarioId,
    String baselinePresetId,
    String managedConfigId,
    ScenarioRunOutcome baselineOutcome,
    ScenarioRunOutcome managedOutcome,
    NormalizedComparisonMetrics baselineMetrics,
    NormalizedComparisonMetrics managedMetrics,
    Map<String, MetricDelta> deltas,            // key = metric name
    Instant createdAt
) {}

public record MetricDelta(
    String metricName,
    double baselineValue,
    double managedValue,
    double absoluteDelta,                        // managed - baseline
    double relativeDelta,                        // (managed - baseline) / baseline * 100
    String direction                             // "IMPROVED", "REGRESSED", "NEUTRAL"
) {}

public record ComparisonReportArtifact(
    String comparisonId,
    String scenarioId,
    Instant createdAt,
    CommonExecutorPreset baselinePreset,
    ManagedExecutorConfig managedConfig,
    ComparisonResult result,
    String conclusion                            // human-readable summary
) {}
```

### 7.6 ComparisonJsonWriter 设计

复用 `AcquisitionJsonWriter` 的手写 JSON 模式：

```java
public final class ComparisonJsonWriter {
    private final AcquisitionReportPaths paths;

    public ComparisonJsonWriter(AcquisitionReportPaths paths);
    public String writeComparisonReport(ComparisonReportArtifact artifact);
    public ComparisonReportArtifact readComparisonReport(Path filePath);
}
```

JSON 输出格式：

```json
{
  "comparisonId": "cmp-uuid",
  "scenarioId": "cpu-bound-10k",
  "createdAt": "2026-06-14T10:30:00Z",
  "baselinePreset": {
    "presetId": "fixed-4",
    "executorType": "FIXED_THREAD_POOL",
    "corePoolSize": 4,
    "maxPoolSize": 4,
    "queueCapacity": -1
  },
  "managedConfig": {
    "corePoolSize": 4,
    "maxPoolSize": 8,
    "queueCapacity": 20
  },
  "result": {
    "baselineMetrics": {
      "completedTaskCount": 10000,
      "rejectedTaskCount": 0,
      "avgQueueDepth": 0.0,
      "maxQueueDepth": 0,
      "totalDurationMs": 5230,
      "throughputPerSecond": 1912.0,
      "avgActiveThreads": 0,
      "maxPoolSize": 4,
      "snapshotCount": 100
    },
    "managedMetrics": { /* same structure */ },
    "deltas": {
      "throughputPerSecond": {
        "baselineValue": 1912.0,
        "managedValue": 1845.0,
        "absoluteDelta": -67.0,
        "relativeDelta": -3.5,
        "direction": "REGRESSED"
      }
    }
  },
  "conclusion": "Managed executor throughput slightly regressed (-3.5%) compared to fixed-4 baseline under CPU-bound workload. Queue depth and rejection counts comparable."
}
```

### 7.7 文件路径约定

扩展 `AcquisitionReportPaths`：

```java
// 新增
public String comparisonReportFile(String comparisonId);  // outputs/reports/v0.12.0/comparison-<comparisonId>.json
```

### 7.8 BaselineWorkloadExecutor 到 NormalizedComparisonMetrics 的映射

由于 `BaselineWorkloadExecutor` 是同步的、无真实线程池的模拟执行器，部分归一化指标需要合理映射：

| NormalizedComparisonMetrics 字段 | BaselineWorkloadExecutor 来源 | ManagedExecutor 来源 |
|---|---|---|
| `completedTaskCount` | `baselineExecutor.completedWorkUnits()` | `executor.getCompletedTaskCount()` |
| `rejectedTaskCount` | 0（baseline 从不拒绝） | executor 拒绝计数 |
| `avgQueueDepth` | 0（baseline 无队列） | snapshots 的 queueSize 平均值 |
| `maxQueueDepth` | 0 | snapshots 的 queueSize 最大值 |
| `totalDurationMs` | scenario steps 总执行时间 | scenario steps 总执行时间 |
| `throughputPerSecond` | `completedTaskCount / (totalDurationMs/1000.0)` | 同 |
| `avgActiveThreads` | 0（baseline 无真实线程） | snapshots 的 activeThreads 平均值 |
| `maxPoolSize` | `preset.corePoolSize()` | snapshots 的 poolSize 最大值 |
| `snapshotCount` | `recorder.snapshots(runId).size()` | 同 |

## 8. 成功标准草案

- `BaselineExecutorCatalog.withDefaults()` 注册至少 6 个常见 JDK executor 预设
- `BaselineExecutorCatalog.get(presetId)` 可查询任意已注册预设
- `ComparableScenarioRunner.compare()` 接受 scenario + baseline preset + managed config → 依次运行两者
- `NormalizedComparisonMetrics` 可从 `List<ObservedSnapshot>` 正确计算所有 9 个归一化指标
- `ComparisonResult` 包含逐 metric delta（absolute + relative + direction）
- `ComparisonJsonWriter.writeComparisonReport()` 产出合法 JSON 比较报告
- `ComparisonJsonWriter.readComparisonReport()` 可从磁盘反序列化并保持所有字段一致
- 端到端：`ComparableScenarioRunner.compare()` → `ComparisonJsonWriter.writeComparisonReport()` → 文件存在且 JSON 合法 → 所有 delta 计算正确
- Managed executor regression/no-improvement 结果被保留为证据（不被隐藏或视为失败）
- 现有 646 测试零回归

## 9. 候选 Change Decomposition

IR/SR 阶段确认后：

| # | Change name | 范围 | 依赖 |
|---|---|---|---|
| 1/2 | `baseline-catalog-and-comparison-runner` | CommonExecutorPreset, BaselineExecutorCatalog, NormalizedComparisonMetrics, ComparisonResult, ComparableScenarioRunner, ScenarioExperimentRunner 扩展 | v0.3.0 ScenarioExperimentRunner, v0.7.0 ManagedExecutorScenarioRunner, v0.7.0 ManagedExecutor |
| 2/2 | `comparison-report-and-end-to-end-verification` | ComparisonReportArtifact, ComparisonJsonWriter, AcquisitionReportPaths 扩展, 端到端 baseline-vs-managed 比较验证 | Change 1, v0.6.0 AcquisitionJsonWriter/AcquisitionReportPaths, v0.11.0 FileBackedEvidenceRecorder |

### 独立可验证性检查（预检）

- Change 1 可独立编译和测试：preset catalog + comparison runner + 归一化 metrics 计算可在不使用 comparison report artifact 的情况下验证
- Change 2 依赖 Change 1 的 `ComparableScenarioRunner` 和 `ComparisonResult`，但 Change 2 的测试基础设施（comparison report + end-to-end）独立于 Change 1 的内部测试
- 两个 change 均可独立运行 `mvn test` 并通过各自范围内的测试

## 10. 当前阶段出口

进入 IR 前必须完成：

1. `README.md` 版本索引
2. `00-objectives-and-scope.md`（本文档）
3. `decision-log.md` 记录关键设计判断
4. `docs/00-project/current-state.md` 反映 v0.12.0 版本设计草稿状态
