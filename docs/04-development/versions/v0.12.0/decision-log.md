# v0.12.0 Decision Log

## D1: Executor Baseline Catalog 范围和预设

**背景**: 需要在 `BaselineExecutorCatalog` 中定义默认预设集，覆盖常见 JDK thread-pool 配置类型以提供有意义的比较基线。

**选项**:
- A: 6 个预设（fixed-2, fixed-4, fixed-8, cached, single, fixed-2-bounded）
- B: 仅 3 个预设（fixed-4, cached, single）
- C: 8+ 预设（含 work-stealing、virtual-thread、scheduled 等）

**决策**: 选 A — 6 个预设覆盖 JDK 标准 executor 类型的主要变体。

**理由**:
- 6 个预设覆盖了固定池（小/中/大）、弹性缓存、单线程、有界队列四个基本维度
- 固定线程池的三个变体（2/4/8）是业界最常见的 thread-pool 配置，覆盖"不足/适中/充裕"三种资源状态
- 3 个预设（选项 B）太少，无法反映真实场景中 thread-pool 选型的多样性
- 8+ 个预设（选项 C）会引入目前场景运行基础设施无法充分负载的类型（如 work-stealing pool 需要额外的工作窃取负载模式；virtual threads 在 v0.11.0 已有基础支持但不是本次焦点）
- 预设列表可后续扩展，`BaselineExecutorCatalog.register()` 是公开 API

**影响**: `BaselineExecutorCatalog.withDefaults()` 初始化 6 个 `CommonExecutorPreset` 实例。每个预设约 5 行构造代码。

---

## D2: ComparableScenarioRunner 执行模型

**背景**: `ComparableScenarioRunner.compare()` 需要运行两次 scenario（baseline + managed），需要决定是顺序执行还是并发执行。

**选项**:
- A: 顺序执行 — 先 baseline 后 managed
- B: 并发执行 — baseline 和 managed 同时运行
- C: 交替执行 — step 级别交替（baseline step 1 → managed step 1 → baseline step 2 → ...）

**决策**: 选 A — 顺序执行。

**理由**:
- 顺序执行确保两次 run 的资源环境一致（CPU、内存、JVM 状态不会被并发 run 干扰）
- 比较报告需要"相同条件"下的对比；并发执行会引入资源竞争噪音，使得 delta 度量难以归因
- `ScenarioExperimentRunner` 和 `ManagedExecutorScenarioRunner` 当前均为同步设计，并发化需要额外线程管理
- 顺序执行的实现最简单、最可预测、最容易验证
- 如果需要并发比较，可以在后续版本中通过 runner 组合实现，无需修改当前 API

**影响**: `ComparableScenarioRunner.compare()` 的执行顺序固定为 baseline-run → managed-run。两个 run 使用独立的 `EvidenceRecorder` 实例（或通过不同的 `runId` 区分）。

---

## D3: 归一化结果模型和指标映射

**背景**: 需要定义一个跨 executor 类型的归一化度量模型，将 `BaselineWorkloadExecutor`（同步，无真实线程池）和 `ManagedExecutor`（真实 ThreadPoolExecutor）的不同观测数据映射到可比较的公共指标。

**选项**:
- A: 9 个归一化指标（`completedTaskCount`, `rejectedTaskCount`, `avgQueueDepth`, `maxQueueDepth`, `totalDurationMs`, `throughputPerSecond`, `avgActiveThreads`, `maxPoolSize`, `snapshotCount`）
- B: 仅 4 个指标（`completedTaskCount`, `totalDurationMs`, `throughputPerSecond`, `rejectedTaskCount`）
- C: 12+ 个指标（含 p50/p95/p99 latency、queue wait time、adjustment count 等）

**决策**: 选 A — 9 个归一化指标。

**理由**:
- 9 个指标覆盖了 throughput（吞吐）、latency proxy（队列深度）、resource utilization（活跃线程、池大小）、reliability（拒绝计数）四个关键维度
- 少于 9 个指标（选项 B）不支持队列行为和资源利用率分析
- 12+ 个指标（选项 C）中的 latency percentile 和 queue wait time 需要额外的数据采集基础设施（每个 task 级别的开始/结束时间戳），目前不存在，且引入后需要修改 `LoadScenario` 或 `ScenarioStep` 模型
- 9 个指标均可从现有 `List<ObservedSnapshot>` 纯数据计算（无需修改任何采集接口）
- roadmap 中的 evaluation metrics 列表包含 throughput、latency、p95/p99、queue depth、rejection count 等 —— 9 个指标覆盖了其中的可计算子集

**影响**: `NormalizedComparisonMetrics` record 包含 9 个字段 + 1 个 `fromSnapshots(List<ObservedSnapshot>, Duration totalDuration, BaselineExecutorPreset preset)` 工厂方法用于从原始快照计算归一化指标。

---

## D4: 比较报告产物格式

**背景**: 需要确定 `ComparisonReportArtifact` 的序列化格式和在磁盘上的组织方式。

**选项**:
- A: 单个 JSON 文件（`comparison-<comparisonId>.json`），包含完整的报告
- B: 一个目录，内含 `baseline-run/` 和 `managed-run/` 子目录（含各自 evidence）+ `report.json`
- C: JSON Lines 文件，每行一个 metric delta

**决策**: 选 A — 单个 JSON 文件。

**理由**:
- 单文件便于复制、传输、按 comparison 管理
- 单个 run 的 evidence 已通过 `FileBackedEvidenceRecorder` 持久化（v0.11.0）；比较报告引用 run ID 即可关联，不需要内联所有原始快照
- 与 `AcquisitionReportWriter` 的单文件报告模式一致
- JSON Lines（选项 C）适合流式追加场景，comparison report 是一次性写入的聚合产物，不需要流式追加
- 目录方案（选项 B）增加了路径复杂度而不增加可读性

**影响**: `ComparisonJsonWriter` 使用手写 JSON（无外部依赖），与 `AcquisitionJsonWriter` 和 `MinimalJsonWriter` 保持一致。`AcquisitionReportPaths` 新增 `comparisonReportFile(String comparisonId)` 方法。

---

## D5: Change 分解策略

**背景**: v0.12.0 包含两个子能力：baseline catalog + comparison runner（框架核心），和 comparison report + end-to-end verification（报告和验证）。

**选项**:
- A: 单 change（`baseline-comparison-experiment-framework`）
- B: 双 change（`baseline-catalog-and-comparison-runner` → `comparison-report-and-end-to-end-verification`）

**决策**: 选 B — 双 change。

**理由**:
- Change 1（`baseline-catalog-and-comparison-runner`）可独立编译、独立测试：catalog 注册/查询 + 默认预设 + normalized metrics 计算 + comparison runner 双 run 流程
- Change 2（`comparison-report-and-end-to-end-verification`）依赖 Change 1 的 `ComparableScenarioRunner`、`ComparisonResult`、`NormalizedComparisonMetrics`
- Change 1 的测试验证"系统能运行 baseline vs managed 比较并产出 ComparisonResult"，Change 2 的测试验证"ComparisonResult 能序列化为 JSON 并端到端验证完整流程"
- 符合 managed-change-standard 的独立可验证性规则
- 符合 v0.11.0 复盘 P3 驱动的 change 分解独立验证规则

---

## D6: ScenarioDefinition 复用 vs 新建 WorkloadDefinition

**背景**: `ComparableScenarioRunner` 需要接受一个 workload 描述来运行。现有 `ScenarioDefinition` 包含 steps 和 scenario profile，可被 `ScenarioPlanner` 消费。

**选项**:
- A: 直接复用 `ScenarioDefinition` 和 `ScenarioPlanner`（无需新 workload 类型）
- B: 新建 `WorkloadDefinition` 作为 scenario 的上层抽象

**决策**: 选 A — 直接复用 `ScenarioDefinition`。

**理由**:
- `ScenarioDefinition` 和 `ScenarioPlanner` 已支持 deterministic step 生成，完全满足 comparison 需求
- 新建 `WorkloadDefinition` 会增加不必要的抽象层和类型转换
- `ScenarioExperimentRunner.run(ScenarioDefinition, BaselineExecutorPreset)` 已接受 `ScenarioDefinition` 作为参数
- 保持简单：comparison framework 不需要新的 workload 概念，它只是"同一个 `ScenarioDefinition` 运行两次"

**影响**: `ComparableScenarioRunner.compare()` 接受 `ScenarioDefinition`（略作 `scenarioId` 参数），而非新 workload 类型。

---

## DFR: Deferred 项

| ID | 描述 | 理由 | 后续版本 |
|---|---|---|---|
| DFR-01 | CPU utilization 真实数据源 | 需要跨平台 CPU 读取实现；已在 v0.11.0 DFR-01 中记录 | 候选 v0.13.0 |
| DFR-02 | 多场景批量比较 | 单场景比较一次一个 scenario；批量需要 orchestration 层和聚合报告 | 候选 v0.13.0+ |
| DFR-03 | 统计显著性检验 | t-test、confidence intervals 需要多 run 数据积累，单 run 无统计意义 | 候选 v0.14.0+ |
| DFR-04 | 并发比较执行 | 顺序执行已满足 v0.12.0 需求，并发执行引入资源竞争噪音 | 候选 v0.15.0+ |
| DFR-05 | Latency percentile (p50/p95/p99) | 需要 per-task 级别时间戳采集基础设施，当前不存在 | 候选 v0.16.0 |
| DFR-06 | Work-stealing / virtual-thread / scheduled executor 预设 | 当前场景类型（步骤序列）无法充分测试这些 executor 类型的优势 | 候选 v0.13.0+ |
