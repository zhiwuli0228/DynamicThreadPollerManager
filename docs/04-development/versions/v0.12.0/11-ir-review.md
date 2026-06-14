# v0.12.0 IR 独立评审报告

## Header

- Document type: IR independent review
- Version name: `v0.12.0`
- Reviewed artifact: `docs/04-development/versions/v0.12.0/10-ir.md`
- Review date: `2026-06-14`
- Reviewer role: 独立 IR review（非 IR 作者）
- Review basis: `docs/02-harness/managed-change-standard.md` 第 2 节（IR 需求分析）

## 1. 评审输入

已读取以下文件作为评审上下文：

- `docs/00-project/current-state.md`
- `docs/02-harness/managed-change-standard.md`
- `docs/01-architecture/operational-and-evolution-boundaries.md`
- `docs/04-development/versions/v0.12.0/README.md`
- `docs/04-development/versions/v0.12.0/00-objectives-and-scope.md`
- `docs/04-development/versions/v0.12.0/decision-log.md`
- `docs/04-development/versions/v0.12.0/10-ir.md`
- `src/main/java/.../scenario/BaselineExecutorPreset.java`
- `src/main/java/.../scenario/BaselineWorkloadExecutor.java`
- `src/main/java/.../scenario/ScenarioExperimentRunner.java`
- `src/main/java/.../scenario/ScenarioRunOutcome.java`
- `src/main/java/.../scenario/ScenarioDefinition.java`
- `src/main/java/.../scenario/ScenarioPlanner.java`
- `src/main/java/.../scenario/ManagedExecutorScenarioRunner.java`
- `src/main/java/.../executor/ManagedExecutor.java`
- `src/main/java/.../executor/ManagedExecutorConfig.java`
- `src/main/java/.../metrics/EvidenceRecorder.java`
- `src/main/java/.../metrics/ObservedSnapshot.java`
- `src/main/java/.../metrics/RuntimeObservation.java`
- `src/main/java/.../model/PressureSnapshot.java`
- `src/main/java/.../model/ExperimentRun.java`
- `src/main/java/.../acquisition/AcquisitionReportPaths.java`
- `src/main/java/.../acquisition/AcquisitionJsonWriter.java`
- `src/main/java/.../coordinator/ExperimentCoordinator.java`

## 2. 评审摘要

IR 草案结构完整：8 条需求覆盖了从 `BaselineExecutorCatalog` 到端到端验证的完整链路。复用 `ScenarioExperimentRunner`、`ManagedExecutorScenarioRunner`、`AcquisitionJsonWriter` 和 `EvidenceRecorder` 基础设施的决策正确。但存在 **3 个 P0 阻断项**和 **3 个 P1 关键项**需要处置，主要集中在 `ManagedExecutorScenarioRunner` 集成可行性、`ManagedExecutorConfig` JSON 格式未定义、以及 `rejectedTaskCount` 数据来源不可用三个问题上。

## 3. Findings

### F01 [P0] ManagedExecutorScenarioRunner.run() 自包含，ComparableScenarioRunner 无法分离 run 和 outcome 获取

**位置**: IR-v0.12-003

**问题**: IR-v0.12-003 要求 `ComparableScenarioRunner` 驱动 `ManagedExecutorScenarioRunner` 运行 managed executor 侧，并收集 snapshots。IR 要求 `compare()` 方法中 Phase 2 应 "通过 `managedRunner` 运行 managed executor（使用 `managedConfig`）"。

但验证源码后发现 `ManagedExecutorScenarioRunner.run()` 是**完全自包含**的：
- 内部创建 `ManagedExecutor`（`config.toManagedExecutor()` — 第 65 行）
- 内部创建 `ExecutorRegistry`（第 66 行）
- 内部管理完整的 7-phase 生命周期
- 返回 `ScenarioRunOutcome` — 仅包含 runId, scenarioId, policyId, completedStepCount, totalWorkUnits, evidenceCount, finalState

关键问题：`ComparableScenarioRunner` 需要从 managed run 获取 `List<ObservedSnapshot>` 来计算 `NormalizedComparisonMetrics`，但 `ScenarioRunOutcome` **不包含 snapshots**。snapshots 在 `ManagedExecutorScenarioRunner` 内部的 `recorder` 中，通过 `recorder.snapshots(run.runId())` 访问，但 `ComparableScenarioRunner` 需要知道 runId 才能从 `managedRecorder` 获取 snapshots。

当前 `ScenarioRunOutcome.runId()` 已暴露 runId（第 47 行），所以流程可行：
1. `ComparableScenarioRunner` 持有 `managedRecorder`（构造时注入）
2. 调用 `managedRunner.run(definition, managedConfig)` → 得到 `ScenarioRunOutcome`
3. 从 `outcome.runId()` 获取 runId
4. 从 `managedRecorder.snapshots(runId)` 获取 snapshots

**实际可行**。但 IR-v0.12-003 的伪代码写的是 `managedRunner` 返回 snapshots，而实际 `run()` 返回 `ScenarioRunOutcome`（不包含 snapshots）。IR 的描述不够精确，可能导致实现 agent 在 `ManagedExecutorScenarioRunner` 上寻找不存在的方法。

此外一个更深层问题：`ComparableScenarioRunner` 需要两个独立的 `ExperimentCoordinator` 实例（baseline run 和 managed run 的 runId 由各自的 coordinator 生成）。当前 `ExperimentCoordinator` 是独立实例，没有全局状态，所以可以创建两个实例分别用于 baseline 和 managed。

**影响**: IR 对 run → snapshots 的获取路径描述不精确。实现时 agent 必须知道 `ScenarioRunOutcome.runId()` + `recorder.snapshots(runId)` 是获取 snapshots 的正确路径。

**建议**: IR-v0.12-003 明确以下数据流：

```
ComparableScenarioRunner.compare(scenario, presetId, managedConfig):
  1. CommonExecutorPreset preset = catalog.get(presetId)
  2. BaselineExecutorPreset bPreset = toBaselinePreset(preset)
  3. ScenarioRunOutcome bOutcome = baselineRunner.run(scenario, bPreset)
  4. List<ObservedSnapshot> bSnapshots = baselineRecorder.snapshots(bOutcome.runId())
  5. ScenarioRunOutcome mOutcome = managedRunner.run(scenario, managedConfig)
  6. List<ObservedSnapshot> mSnapshots = managedRecorder.snapshots(mOutcome.runId())
  7. NormalizedComparisonMetrics bMetrics = NormalizedComparisonMetrics.fromSnapshots(bSnapshots, ...)
  8. NormalizedComparisonMetrics mMetrics = NormalizedComparisonMetrics.fromSnapshots(mSnapshots, ...)
  9. Map<String, MetricDelta> deltas = computeDeltas(bMetrics, mMetrics)
  10. return new ComparisonResult(...)
```

---

### F02 [P0] ManagedExecutorConfig 的 JSON 序列化格式未定义

**位置**: IR-v0.12-006

**问题**: `ComparisonReportArtifact` record 包含 `ManagedExecutorConfig managedConfig` 字段。`ComparisonJsonWriter` 需要序列化和反序列化此字段。但 `ManagedExecutorConfig` 包含：
- `TimeUnit keepAliveTimeUnit` — Java enum（SECONDS, MILLISECONDS 等 7 个值）
- `ThreadMode threadMode` — 自定义 enum（PLATFORM, VIRTUAL）

IR-v0.12-006 的 JSON 格式示例中 `managedConfig` 只是一个占位符 — 没有定义 `keepAliveTime`, `keepAliveTimeUnit`, `threadMode` 字段的 JSON 表示。

手写 JSON parser 需要知道如何反序列化 `TimeUnit`（使用 `TimeUnit.valueOf(string)`）和 `ThreadMode`（使用 `ThreadMode.valueOf(string)`）。这本身不复杂，但 IR 应该明确 JSON 格式。

`ManagedExecutorConfig` 的 JSON 格式应为：
```json
{
  "corePoolSize": 4,
  "maximumPoolSize": 8,
  "queueCapacity": 20,
  "keepAliveTime": 60,
  "keepAliveTimeUnit": "SECONDS",
  "threadMode": "PLATFORM"
}
```

**影响**: SR 和实现 agent 需要知道 `ManagedExecutorConfig` 的 JSON 表示。如果不定义，`ComparisonJsonWriter` 的往返序列化测试（AC-v0.12-016）无法编写。

**建议**: IR-v0.12-006 补充 `ManagedExecutorConfig` 的 JSON 格式定义。枚举字段使用 `name()` 序列化、`valueOf()` 反序列化，与 `AcquisitionJsonWriter` 处理枚举的模式一致。

---

### F03 [P0] rejectedTaskCount 数据来源不可用 — ManagedExecutor 无拒绝计数暴露

**位置**: IR-v0.12-004, IR-v0.12-007

**问题**: IR-v0.12-004 要求 `NormalizedComparisonMetrics` 包含 `rejectedTaskCount` 字段。IR-v0.12-007 的映射表说 managed executor 的 `rejectedTaskCount` 来源于 "从 executor 或 snapshots 获取"。

验证源码确认：
- `ManagedExecutor` **没有** `getRejectedTaskCount()` 方法
- `ThreadPoolExecutor` **没有** `getRejectedTaskCount()` 公开 API
- `PressureSnapshot` **没有** `rejectedTaskCount` 字段（只有 6 个字段：timestamp, activeThreads, poolSize, queueSize, completedTaskCount, cpuUtilization）
- `RuntimeObservation` **没有** `rejectedTaskCount` 对应的 `MetricValue`

当前项目的拒绝策略是 `ThreadPoolExecutor.AbortPolicy()`（见 `ManagedExecutorConfig.toManagedExecutor()` 第 59-60 行）。AbortPolicy 在被触发时抛出 `RejectedExecutionException` 并**不计数**。

获取拒绝计数的唯一方式是：
- 创建自定义 `RejectedExecutionHandler` 包装器，在 reject 发生时递增计数器
- 或从 `experiment.executor` 已有的 `RejectionPolicyCommand` / `PolicyReplacementEvidence` 中读取（但这些是策略替换证据，不是运行时拒绝计数器）

**这是一个真实的架构缺口**，不仅在 IR 层面，在 `ManagedExecutor` 层面也存在。

**影响**: IR-v0.12-004 的 `rejectedTaskCount` 字段在 managed executor 侧**当前无法填充真实数据**。所有 managed run 的 `rejectedTaskCount` 将始终为 0，即使实际发生了拒绝。

**建议**: 三种处理方案：

方案 A（推荐）：在 `ManagedExecutor` 中添加 rejection counting wrapper。`toManagedExecutor()` 构造时自动包装 `RejectedExecutionHandler`：
```java
// ManagedExecutor 内部
private final AtomicLong rejectedTaskCount = new AtomicLong(0);

// 在构造器中
RejectedExecutionHandler countingHandler = (r, executor) -> {
    rejectedTaskCount.incrementAndGet();
    originalHandler.rejectedExecution(r, executor);
};
// 传递给 TPE
```
然后添加 `public long getRejectedTaskCount() { return rejectedTaskCount.get(); }`

方案 B：v0.12.0 中 `rejectedTaskCount` 始终返回 0，在 comparison report 中记录"rejected task counting not yet implemented"。

方案 C：从 `NormalizedComparisonMetrics` 中移除 `rejectedTaskCount` 字段，延后到支持真实拒绝计数的版本。

推荐方案 A — 代码变更量最小（约 15 行），且 `rejectedTaskCount` 是 comparison 框架的关键可靠性指标。IR 应明确要求此扩展作为 v0.12.0 的 prerequisite。

---

### F04 [P1] CommonExecutorPreset queueCapacity 语义与 BaselineExecutorPreset 不一致

**位置**: IR-v0.12-002

**问题**: IR-v0.12-002 定义 `CommonExecutorPreset.queueCapacity` 为：
- `-1` → 无界队列（LinkedBlockingQueue default）
- `0` → SynchronousQueue
- `> 0` → 有界队列容量

但 `BaselineExecutorPreset`（将被 `ComparableScenarioRunner` 转换自 `CommonExecutorPreset`）的 `queueCapacity` 字段是简单的 `int`：构造时验证 `queueCapacity >= 0`（`BaselineExecutorPreset.java` 第 28 行），不接受 `-1`。

这意味着 `CommonExecutorPreset` 的 `queueCapacity=-1`（无界队列）**无法直接转换为** `BaselineExecutorPreset`。`BaselineWorkloadExecutor` 甚至不使用真实队列（它是同步的），所以 `queueCapacity` 在 baseline 侧的含义只是一个配置记录值。

但语义不一致会在 SR 设计时造成混淆：`ComparableScenarioRunner` 的 `toBaselinePreset()` 转换方法需要决定 `-1` 应该映射为什么值。

**建议**: IR 明确转换规则：`CommonExecutorPreset.queueCapacity == -1` 时，转换为 `BaselineExecutorPreset` 的 `queueCapacity = Integer.MAX_VALUE`（表示无界），或直接传递 `-1` 并修改 `BaselineExecutorPreset` 的验证逻辑。推荐前一种方案（不修改现有类），在 comparison report 的 `baselinePreset` 字段中保留原始 `CommonExecutorPreset` 信息。

---

### F05 [P1] ScenarioExperimentRunner.run() 的 BaselineWorkloadExecutor 生命周期与 ComparableScenarioRunner 不匹配

**位置**: IR-v0.12-003

**问题**: `ScenarioExperimentRunner.run()` 内部创建 `BaselineWorkloadExecutor baselineExecutor`（构造参数），并在 run 完成后返回 `ScenarioRunOutcome`。但验证源码发现 `ScenarioExperimentRunner` 的构造器接受一个**已构建的** `BaselineWorkloadExecutor`（第 36 行），而非在 `run()` 中创建。

这意味着 `ComparableScenarioRunner` 需要在调用 `baselineRunner.run()` 之前：
1. 将 `CommonExecutorPreset` 转换为 `BaselineExecutorPreset`
2. 用 `BaselineExecutorPreset` 创建 `BaselineWorkloadExecutor`
3. …但 `ScenarioExperimentRunner` 已经在构造时绑定了 `BaselineWorkloadExecutor`

当前 `ScenarioExperimentRunner` 使用单一 `BaselineWorkloadExecutor` — 构造时注入，`run()` 方法使用它但任接受 `BaselineExecutorPreset` 参数（仅用于 `buildObservation(preset)` 读取 `preset.corePoolSize()`）。

如果 `ComparableScenarioRunner` 需要运行不同的 baseline preset（如先运行 `fixed-4`，再运行 `fixed-8`），它需要两个不同的 `ScenarioExperimentRunner` 实例（每个绑定不同的 `BaselineWorkloadExecutor`）。

这**技术上可行**：`ComparableScenarioRunner` 在 `compare()` 内部为每次 baseline run 创建新的 `ScenarioExperimentRunner`。但 IR 不应该假设"单一 baselineRunner 实例可重复用于不同 preset"。

**建议**: IR-v0.12-003 明确：`ComparableScenarioRunner` 在每次 `compare()` 调用中，根据 `baselinePresetId` 动态创建 `ScenarioExperimentRunner`（而非持有单一实例）。或改为接受 `ScenarioExperimentRunner` 工厂而非实例。

---

### F06 [P1] NormalizedComparisonMetrics.fromSnapshots() 的 totalDurationMs 参数传递路径不清晰

**位置**: IR-v0.12-004

**问题**: IR-v0.12-004 定义 `fromSnapshots(List<ObservedSnapshot> snapshots, long totalDurationMs, int fallbackPoolSize)`，其中 `totalDurationMs` 是外部传入的参数，而非从 snapshots 中计算。

`ComparableScenarioRunner.compare()` 需要在 baseline run 和 managed run 前后分别记录时间戳，计算 duration。但 IR 没有明确：
- baseline 的 `totalDurationMs` 是从 `baselineRecorder` 中最早/最晚 snapshot 的时间戳计算，还是从 `ScenarioExperimentRunner` 外部的 wall-clock 计时？
- managed run 同理

如果使用 wall-clock 计时（推荐，因为它包含 setup/teardown 开销），那么 `ComparableScenarioRunner` 需要分别在 run 前后调用 `clock.get()`。

**建议**: IR-v0.12-003 补充：`compare()` 在每个 run 前后记录 `clock.get()` 时间戳，`totalDurationMs = Duration.between(start, end).toMillis()`。`fromSnapshots()` 接受外部计算的 `totalDurationMs` 而非从 snapshots 推断。

---

### F07 [P2] BaselineExecutorPreset 到 NormalizedComparisonMetrics 的 completedTaskCount 语义差异

**位置**: IR-v0.12-007

**问题**: IR-v0.12-007 映射表确认 baseline executor 的 `completedTaskCount` 来源于 `baselineExecutor.completedWorkUnits()`（而非真实的 completedTaskCount）。而 managed executor 的来源于 TPE 的 `getCompletedTaskCount()`。

两者的语义不同：
- `BaselineWorkloadExecutor.completedWorkUnits()` — 累计的 step.workUnits() 总和
- `TPE.getCompletedTaskCount()` — 实际完成的任务数（可能小于提交数，因为有些任务可能还在队列中或正在执行）

当前所有 scenario 的 workUnits 均为 1（每个 step 1 workUnit），所以 `completedStepCount == completedWorkUnits`。但如果 scenario 使用 workUnits > 1 的 step，两者的语义差异会暴露。

**影响**: 低 — 当前所有 scenario 使用 workUnits=1。但 comparison report 应注明此映射差异，避免未来 workUnits 变更时的误读。

**建议**: DEFER_TO_SR。在 `ComparisonReportArtifact` 的 `conclusion` 或新增的 `methodologicalNotes` 字段中记录映射规则。

---

### F08 [P2] ComparisonJsonWriter 手写 JSON 解析需要处理嵌套 record 类型

**位置**: IR-v0.12-006

**问题**: `ComparisonReportArtifact` 的 JSON 包含 4 层嵌套：
```
ComparisonReportArtifact
  ├── CommonExecutorPreset (record)
  ├── ManagedExecutorConfig (record, 含 TimeUnit + ThreadMode enum)
  ├── ComparisonResult (record)
  │   ├── ScenarioRunOutcome ×2
  │   ├── NormalizedComparisonMetrics ×2
  │   └── Map<String, MetricDelta> (9 entries)
  └── String conclusion
```

手写 JSON parser 需要为每一层类型编写 parse 方法。`AcquisitionJsonWriter` 当前只有一个 `render(Object)` 方法（序列化方向），没有 parser（反序列化方向）。`MinimalJsonWriter` 也类似。

这意味着 `ComparisonJsonWriter` 需要实现**全新的** JSON parser，不仅仅是"复用 AcquisitionJsonWriter"。代码量预估：6 个 record 类型 × 各自的 write/read 方法 = 约 400-600 行。

这并非阻塞问题（`PressureSnapshot.fromMap()` 和 `RuntimeObservation.fromMap()` 已经有手写反序列化先例），但 IR 应将此评估为复杂度风险，并在 SR 阶段提供完整的 JSON schema。

**影响**: 低。手写 JSON parser 模式已有先例（`PressureSnapshot.fromMap()`、`RuntimeObservation.fromMap()`、`ObservedSnapshot.fromMap()`）。但需在 SR 中完整定义 schema。

**建议**: DEFER_TO_SR。SR 为 `ComparisonReportArtifact` 提供完整的 JSON schema 定义。

---

## 4. Findings 汇总

| ID | 位置 | 描述 | 级别 | 建议动作 |
|---|---|---|---|---|
| F01 | IR-v0.12-003 | ComparableScenarioRunner 获取 snapshots 的数据流不精确 | P0 | IR 补充明确的 runId → snapshots 数据流 |
| F02 | IR-v0.12-006 | ManagedExecutorConfig JSON 格式未定义（含 TimeUnit/ThreadMode enum） | P0 | IR 补充 ManagedExecutorConfig JSON 格式 |
| F03 | IR-v0.12-004 | rejectedTaskCount 数据来源不可用 — ManagedExecutor 无拒绝计数 | P0 | IR 要求 ManagedExecutor 添加 rejection counting wrapper（方案 A） |
| F04 | IR-v0.12-002 | CommonExecutorPreset queueCapacity=-1 无法直接映射到 BaselineExecutorPreset | P1 | IR 明确转换规则 |
| F05 | IR-v0.12-003 | ScenarioExperimentRunner 绑定单一 BaselineWorkloadExecutor | P1 | IR 明确 runner 实例化策略（工厂模式或 compare() 内创建） |
| F06 | IR-v0.12-004 | totalDurationMs 传递路径不清晰 | P1 | IR 补充 wall-clock 计时策略 |
| F07 | IR-v0.12-007 | completedWorkUnits vs completedTaskCount 语义差异 | P2 | DEFER_TO_SR |
| F08 | IR-v0.12-006 | 手写 JSON parser 复杂度（4 层嵌套、6 个 record 类型） | P2 | DEFER_TO_SR |

## 5. 正向检查通过项

- [x] IR 不隐含实现授权 — 各条目使用"候选验收语义"措辞，第 9 节明确"不授权实现或 OpenSpec change"
- [x] Scope 边界明确排除多场景批量比较、统计检验、可视化、新 executor mutation
- [x] 非范围列表与 `00-objectives-and-scope.md` 和 `decision-log.md` 一致
- [x] 正确识别不涉及新的 JDK API 评估（v0.12.0 无新 TPE mutation）
- [x] 复用 `ScenarioExperimentRunner`、`ManagedExecutorScenarioRunner`、`AcquisitionJsonWriter`、`EvidenceRecorder` 等现有基础设施
- [x] `BaselineExecutorCatalog` 预设覆盖 6 个 JDK 标准类型，覆盖固定/缓存/单线程/有界队列维度
- [x] `ComparableScenarioRunner` 顺序执行模型与 decision-log D2 一致
- [x] `NormalizedComparisonMetrics` 的 9 个指标覆盖 throughput/latency-proxy/resource/reliability 四个维度
- [x] `ComparisonJsonWriter` 使用手写 JSON 模式，与 `AcquisitionJsonWriter` 一致，不引入外部依赖
- [x] BaselineWorkloadExecutor 到 NormalizedMetrics 映射表完整定义（§IR-v0.12-007）
- [x] 端到端测试覆盖 catalog + comparison + serialization 完整链路（6 个测试场景）
- [x] `MetricDelta.direction` 规则明确（IMPROVED/REGRESSED/NEUTRAL 判断标准）
- [x] Managed executor regression 结果保留为证据的需求明确
- [x] 风险登记表覆盖了 API 集成、数据来源、类型转换、schema 兼容性等关键风险
- [x] 出口条件清单与 `managed-change-standard.md` 第 2 节一致
- [x] 不涉及 production 环境、外部依赖、REST/API/UI
- [x] `EvidenceRecorder`、`PressureSampler`、`PressureSnapshot` 接口保持不变

## 6. 评审结论

IR 草案在利用现有基础设施（`ScenarioExperimentRunner`、`ManagedExecutorScenarioRunner`、`AcquisitionJsonWriter`、`EvidenceRecorder`）和需求边界定义方面**合格**。8 条需求覆盖了从 `BaselineExecutorCatalog` 到端到端验证的完整链路。Comparison 框架的"evidence-first"定位与 roadmap 一致。

但**不能直接进入 SR**：存在 3 个 P0 阻断项：

- **F01**: `ComparableScenarioRunner` 获取 snapshots 的数据流需要精确化 — 实现 agent 需要明确知道 `ScenarioRunOutcome.runId()` → `recorder.snapshots(runId)` 路径
- **F02**: `ManagedExecutorConfig` JSON 格式未定义 — `ComparisonJsonWriter` 无法序列化/反序列化未定义格式的类型
- **F03**: `rejectedTaskCount` 数据来源不可用 — `ManagedExecutor` 需要添加 rejection counting wrapper（~15 行代码变更），这是 comparison 框架的关键可靠性指标

以及 3 个 P1 关键项（F04 `queueCapacity` 语义映射、F05 runner 实例化策略、F06 `totalDurationMs` 传递路径）。

P0/P1 必须通过 disposition 关闭后才能进入 SR。

评审建议：**进入 IR disposition（`12-ir-review-disposition.md`）**，逐项处置 F01-F08。
