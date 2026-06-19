# v0.13.0 IR 独立评审报告

## Header

- Document type: IR independent review
- Version name: `v0.13.0`
- Reviewed artifact: `docs/04-development/versions/v0.13.0/10-ir.md`
- Review date: `2026-06-14`
- Reviewer role: 独立 IR review（非 IR 作者）
- Review basis: `docs/02-harness/managed-change-standard.md` 第 2 节（IR 需求分析）

## 1. 评审输入

已读取以下文件作为评审上下文：

- `docs/00-project/current-state.md`
- `docs/02-harness/managed-change-standard.md`
- `docs/01-architecture/operational-and-evolution-boundaries.md`
- `docs/04-development/versions/v0.13.0/README.md`
- `docs/04-development/versions/v0.13.0/00-objectives-and-scope.md`
- `docs/04-development/versions/v0.13.0/decision-log.md`
- `docs/04-development/versions/v0.13.0/10-ir.md`
- `src/main/java/.../model/PressureSnapshot.java`
- `src/main/java/.../metrics/ObservedSnapshot.java`
- `src/main/java/.../metrics/RuntimeObservation.java`
- `src/main/java/.../metrics/MetricValue.java`
- `src/main/java/.../metrics/DefaultSnapshotAssembler.java`
- `src/main/java/.../metrics/EvidenceRecorder.java`
- `src/main/java/.../metrics/SnapshotAssembler.java`
- `src/main/java/.../scenario/NormalizedComparisonMetrics.java`
- `src/main/java/.../scenario/ComparableScenarioRunner.java`
- `src/main/java/.../scenario/ManagedExecutorScenarioRunner.java`
- `src/main/java/.../scenario/ScenarioRunOutcome.java`
- `src/main/java/.../policy/ThresholdPolicyConfig.java`
- `src/main/java/.../policy/ThresholdPolicyEvaluator.java`
- `src/main/java/.../policy/PolicyDecision.java`
- `src/main/java/.../policy/PolicyEvaluator.java`
- `src/main/java/.../executor/ManagedExecutor.java`
- `src/main/java/.../executor/ManagedExecutorConfig.java`

## 2. 评审摘要

IR 草案结构完整：8 条需求覆盖了从 `PressureState` 到端到端验证的完整链路。复用 `ObservedSnapshot`、`PressureSnapshot`、`ThresholdPolicyConfig`、`EvidenceRecorder` 等现有基础设施的决策正确。但存在 **3 个 P0 阻断项**和 **3 个 P1 关键项**需要处置，主要集中在 `rejectedTaskCount` 数据来源、RECOVERY 检测与无状态分类器的矛盾、以及 `queueCapacity` 在分类器中的可用性三个问题上。

## 3. Findings

### F01 [P0] NormalizedPressureMetrics.rejectedTaskCount 数据来源不可用——fromSnapshots() 硬编码为 0

**位置**: IR-v0.13-004

**问题**: IR-v0.13-004 要求 `NormalizedPressureMetrics.fromSnapshots()` 的"前 9 个基础指标与 NormalizedComparisonMetrics.fromSnapshots() 使用相同的计算逻辑"。验证 `NormalizedComparisonMetrics.fromSnapshots()` 源码（第 22-71 行）确认：`rejectedTaskCount` 在第 69 行硬编码为 `0L`，不参与从 snapshots 的计算。

`rejectedTaskCount` 的真实数据来源是 `ScenarioRunOutcome.rejectedTaskCount()`（来自 `ManagedExecutor.getRejectedTaskCount()`），由 `ComparableScenarioRunner` 在快照之外通过 `withRejectedTaskCount()` 方法（第 73-78 行）注入。

如果 `NormalizedPressureMetrics` 完全复用 `NormalizedComparisonMetrics` 的计算逻辑，则 `rejectedTaskCount` 将始终为 0，导致 REJECTION_ACTIVE 分类永远无法触发（IR-v0.13-002 依赖 `rejectedTaskCount > 0`）。

这与 IR-v0.13-002 的 REJECTION_ACTIVE 触发条件直接冲突。

**影响**: 分类器的 REJECTION_ACTIVE 状态在当前 IR 定义下无法工作。`rejectedTaskCount` 必须从快照之外的数据源获取。

**建议**: 三种方案：

方案 A（推荐）：`NormalizedPressureMetrics` 添加 `withRejectedTaskCount(long)` 方法（与 `NormalizedComparisonMetrics.withRejectedTaskCount()` 相同模式）。IR 明确：分类器消费方需要从 `ScenarioRunOutcome` 或 `ManagedExecutor` 获取 rejectedTaskCount，并通过该方法注入到 metrics 中。

方案 B：`NormalizedPressureMetrics.fromSnapshots()` 增加 `rejectedTaskCount` 参数，类似 `totalDurationMs`。

方案 C：从 `ObservedSnapshot.observation()` 中检查 `RuntimeObservation` 某个字段的值——但当前 `RuntimeObservation` 没有 `rejectedTaskCount` 对应的 `MetricValue` 字段。需要先扩展 `RuntimeObservation`。

推荐方案 A —— 代码路径最小，且与 v0.12.0 的 `NormalizedComparisonMetrics` 模式完全一致。

---

### F02 [P0] RECOVERY 状态检测需要"前一个分类"——与 SnapshotPressureClassifier 的无状态设计矛盾

**位置**: IR-v0.13-002

**问题**: IR-v0.13-002 定义 RECOVERY 状态的触发条件为："前一个分类为 OVERLOAD/QUEUE_BUILDUP/REJECTION_ACTIVE 且 queueGrowthRate < 0 且 activeThreads 趋势向下"。同时 IR 明确要求 `SnapshotPressureClassifier` 是"无状态实现（可安全共享）"。

这两个需求矛盾：要判断"前一个分类是什么"，分类器必须记住上一次 `classify()` 调用的结果——这是有状态行为。

此外，IR 的端到端测试（IR-v0.13-008 测试 2.6）也隐含了有状态语义："构造 RECOVERY 快照序列（前 5 个 OVERLOAD 特征 + 后 5 个递减特征）→ 验证分类结果从 OVERLOAD 转为 RECOVERY"。这里的"前 5 个 / 后 5 个"意味着同一快照序列的不同段应该产生不同的分类——这要求 snapshot 序列本身包含足够的前后信息，而不是依赖分类器的内存。

**影响**: RECOVERY 检测逻辑必须在无状态约束下重新定义，或 IR 必须放弃无状态要求。

**建议**: 重新定义 RECOVERY 的触发条件，使其完全基于快照序列的内在特征（不依赖前一次 `classify()` 调用结果）：

- 选项 A：RECOVERY 条件改为 `queueGrowthRate < -queueGrowthThreshold`（队列显著下降中）且 `threadUtilizationRatio < 0.5`。这使得 RECOVERY 可以仅从当前快照序列的趋势特征检测，无需状态。
- 选项 B：保留有状态要求——`classify()` 增加一个可选的 `PressureClassification previousClassification` 参数。无此参数时 RECOVERY 检测退化为选项 A。
- 选项 C：从 IR 中移除无状态要求，允许 `SnapshotPressureClassifier` 持有内部状态（如最后一次分类的内存）。

推荐选项 A —— 最小复杂度，且趋势下降（queueGrowthRate < -threshold）本身就是 RECOVERY 的有效信号。与 `QUEUE_BUILDUP`（queueGrowthRate > +threshold）形成对称。

---

### F03 [P0] OVERLOAD 条件需要 queueCapacity——但 ClassifierConfig 不含此参数，PressureSnapshot 也不提供

**位置**: IR-v0.13-002

**问题**: IR-v0.13-002 的 OVERLOAD 触发条件为："threadUtilizationRatio >= 0.8 且 maxQueueDepth >= queueCapacity * 0.5"。IR 注释中标注"queueCapacity 来自 config 或从 snapshot 推断"。

验证源码确认：
- `PressureSnapshot`（6 字段：timestamp, activeThreads, poolSize, queueSize, completedTaskCount, cpuUtilization）— 不含 `queueCapacity`
- `ObservedSnapshot` — 不含 `queueCapacity`
- `ClassifierConfig`（trendWindowSize, queueGrowthThreshold, rejectionWindowSize）— 不含 `queueCapacity`

**queueCapacity 在当前数据模型中不存在于任何分类器可访问的位置**。对于 managed executor，它存在于 `ManagedExecutorConfig.queueCapacity()`。对于 baseline executor，队列概念不适用（baseline executor 同步执行，无真实队列）。

**影响**: OVERLOAD 分类条件中的 `queueCapacity * 0.5` 阈值无法计算。分类器要么无法触发 OVERLOAD，要么需要使用替代条件。

**建议**: 三种方案：

方案 A（推荐）：在 `ClassifierConfig` 中添加 `queueCapacity` 字段（默认值如 Integer.MAX_VALUE 表示"未知/无界"）。OVERLOAD 条件中，当 `queueCapacity == Integer.MAX_VALUE` 时，退化为仅检查 `threadUtilizationRatio >= 0.8` 和 `maxQueueDepth > 0`。

方案 B：移除 OVERLOAD 条件中对 `queueCapacity` 的依赖，改用绝对阈值（如 `maxQueueDepth >= 10`）。但绝对阈值对不同的 executor 配置不通用。

方案 C：将 `queueCapacity` 设为 `PressureClassifier.classify()` 的方法参数而非 ClassifierConfig 字段。

推荐方案 A —— 最小接口变更，`ClassifierConfig` 已经承担了分类器配置职责，添加一个字段（含合理的默认值）不破坏其设计。

---

### F04 [P1] ThresholdPolicyConfig.minPoolSize 构造时已验证 > 0——safetyScore=0 的测试用例无法构造

**位置**: IR-v0.13-005, IR-v0.13-008

**问题**: IR-v0.13-008 测试 5.5 要求验证："safetyScore=0 对无效策略（minPoolSize=0）"。

验证 `ThresholdPolicyConfig` 源码（第 32-34 行）：构造器明确验证 `minPoolSize <= 0` 时抛出 `IllegalArgumentException`。**无法创建 `minPoolSize=0` 的 `ThresholdPolicyConfig` 实例**。

这意味着 IR 的测试用例不可执行——`ThresholdPolicyScorer` 永远不会收到 `minPoolSize=0` 的配置。

**影响**: 测试 5.5 不可执行。Safety 维度的"边界检查"需要针对实际上可能出现的无效配置重新定义。

**建议**: 重新定义 safety 维度的评分规则，使其仅检查策略参数相对于当前压力状态的合理性，而非检查不可能出现的参数值：

- `maxPoolSize <= currentMaxObservablePoolSize` → 不安全（无法容纳当前负载）→ safetyScore 降低
- `scaleStep > maxPoolSize * 0.5` → 步长过大，可能导致激进扩缩容 → safetyScore 降低
- `maxPoolSize > ManagedExecutor.MAX_POOL_SIZE_CEILING` → 超出硬上限 → safetyScore 降低（但 `ManagedExecutorConfig` 已有此验证，可能需要定义 scorer 自己的上限）

这样 safetyScore 始终可计算，且不依赖构造不可执行配置的前提。

---

### F05 [P1] PolicyScorer efficiency 维度引用 "peakPoolSize"——术语在 NormalizedPressureMetrics 中不存在

**位置**: IR-v0.13-005

**问题**: IR-v0.13-005 的 Efficiency 评分逻辑描述为："maxPoolSize 相对于观测 peakPoolSize 的过度配置程度。maxPoolSize >> peakPoolSize → 低分。maxPoolSize 接近 peakPoolSize → 高分。"

但 `NormalizedPressureMetrics` 中没有 `peakPoolSize` 字段。相关的字段是 `maxPoolSize`（所有 snapshot 中 poolSize 的最大值）和 `avgActiveThreads`。术语"peakPoolSize"在 IR 中未在其他地方定义。

**影响**: SR 和实现阶段需要明确 Efficiency 维度的数据来源。`maxPoolSize`（来自 `NormalizedPressureMetrics`，即 snapshot 中观测到的最大 poolSize）是"peakPoolSize"的最接近等价物。

**建议**: 将 Efficiency 评分逻辑的"peakPoolSize"改为 `PressureClassification.metrics().maxPoolSize()`（即观测到的最大池大小），并在 IR 中统一术语。

---

### F06 [P1] SystemCpuProbe 注入 RuntimeObservation.fromExecutor() 的方式未定义

**位置**: IR-v0.13-007

**问题**: IR-v0.13-007 要求 `RuntimeObservation.fromExecutor()` 集成 `SystemCpuProbe`，但同时也要求"不得修改 fromExecutor() 的方法签名（保持向后兼容）"。

当前 `fromExecutor(ManagedExecutor, Instant)` 是静态方法，不接受外部依赖注入。要在不修改签名的情况下使用 `SystemCpuProbe`，只能：
- 在方法内部直接 `new SystemCpuProbe()`（简单但不可测试——无法 mock CPU probe）
- 或提供重载方法（`fromExecutor(ManagedExecutor, Instant, SystemCpuProbe)`）同时保持原方法签名不变

**影响**: 如果 `RuntimeObservation.fromExecutor()` 内部直接创建 `SystemCpuProbe`，则所有现有调用方的行为都会自动改变（`cpuUtilization` 从 absent 变为 present 值）。这本身不破坏向后兼容性（`DefaultSnapshotAssembler` 仍然将值映射到 `PressureSnapshot`），但测试中无法控制 CPU probe 的行为。

对于单元测试，`cpuUtilization` 将返回平台实际值（在 CI 上可能为 0 或接近 0），导致测试结果不可重现。对于压力分类器测试（IR-v0.13-008），cpuUtilization 在构造快照时直接传入 `PressureSnapshot`（测试构造的是快照数据而非 RuntimeObservation），所以不受影响。

**建议**: 明确策略：`fromExecutor()` 内部直接创建 `new SystemCpuProbe()`（无参数注入），并在 IR 中记录：
- 对于通过 `RuntimeObservation.fromExecutor()` 创建的真实 observation，`cpuUtilization` 将具有真实的平台相关值
- 分类器测试直接构造 `PressureSnapshot`（设置 `cpuUtilization` 值），不通过 `RuntimeObservation.fromExecutor()` 路径
- `SystemCpuProbe` 自身通过单元测试验证（可 mock `OperatingSystemMXBean`？——但 `ManagementFactory` 的静态方法难以 mock）

或提供 `fromExecutor(ManagedExecutor, Instant, SystemCpuProbe)` 重载，允许测试注入 mock probe。推荐后者。

---

### F07 [P2] PolicyScore 构造器中 compositeScore ≈ weighted sum 校验不可靠

**位置**: IR-v0.13-005

**问题**: IR-v0.13-005 要求 `PolicyScore` 构造时验证："compositeScore 应近似等于 responsivenessScore * 0.35 + safetyScore * 0.30 + stabilityScore * 0.20 + efficiencyScore * 0.15（允许微小浮点误差）"。

"微小浮点误差"的主观定义（0.001? 0.0001?）加上浮点运算的非确定性（不同 JVM 实现可能产生略微不同的 double 结果），使得此校验在构造器中不可靠。一个有效的 `PolicyScore` 可能因浮点精度问题被拒绝。

**影响**: 低。record 的 compact constructor 可以放松此约束，或移除构造时验证，改为在 `PolicyScorer.score()` 方法中保证一致性。

**建议**: DEFER_TO_SR。移除 compact constructor 中的此校验，改为在 `PolicyScorer` 接口文档中声明"实现必须保证 compositeScore 为加权和"。测试验证 scorer 的输出而非 record 的构造器约束。

---

### F08 [P2] ClassifierConfig 缺少 OVERLOAD 条件所需的 queueCapacity 和 maxPoolSize 配置

**位置**: IR-v0.13-002, IR-v0.13-003

**问题**: 与 F03 相关但角度不同。`ClassifierConfig` 只包含趋势相关配置（trendWindowSize, queueGrowthThreshold, rejectionWindowSize），但 OVERLOAD 和 UNDER_UTILIZED 条件需要了解 executor 的容量边界（queueCapacity, corePoolSize, maxPoolSize）。这些参数当前在 `ClassifierConfig` 中不存在。

如果采用 F03 方案 A（在 ClassifierConfig 中添加 queueCapacity），则 `ClassifierConfig` 还需要添加 `corePoolSize`（或 `maxPoolSize`）来支持 UNDER_UTILIZED 条件的 `threadUtilizationRatio < 0.3` 和 OVERLOAD 的 `threadUtilizationRatio >= 0.8` 计算。

因为 `threadUtilizationRatio = avgActiveThreads / maxPoolSize`，而 `maxPoolSize` 来自 `NormalizedPressureMetrics`（从 snapshots 计算），不依赖 `ClassifierConfig`。所以 `threadUtilizationRatio` 的计算本身不需要 config 中的 maxPoolSize。

但 UNDER_UTILIZED 条件中的 `activeThreads < corePoolSize * 0.3`（来自 00-objectives-and-scope.md）使用了 `corePoolSize`，该值必须从某处获取。当前 IR 的触发条件使用 `threadUtilizationRatio` 替代了此计算。需确认两者是否等价。

**影响**: 低。IR-v0.13-002 已经使用 `threadUtilizationRatio` 来定义触发条件，避免了直接引用 `corePoolSize`。F03 的方案 A 可以解决 `queueCapacity` 问题。`corePoolSize` 和 `maxPoolSize` 不需要加入 `ClassifierConfig`。

**建议**: DEFER_TO_SR。确认 IR-v0.13-002 的触发条件仅依赖 `NormalizedPressureMetrics` + `ClassifierConfig`（含 F03 新增的 `queueCapacity`），不依赖 executor 配置对象。

---

## 4. Findings 汇总

| ID | 位置 | 描述 | 级别 | 建议动作 |
|---|---|---|---|---|
| F01 | IR-v0.13-004 | rejectedTaskCount 数据来源不可用——fromSnapshots() 硬编码 0，与 REJECTION_ACTIVE 条件冲突 | P0 | FIX：NormalizedPressureMetrics 添加 withRejectedTaskCount() |
| F02 | IR-v0.13-002 | RECOVERY 检测需要前一次分类结果——与无状态设计矛盾 | P0 | FIX：重新定义 RECOVERY 条件为纯趋势特征 |
| F03 | IR-v0.13-002 | OVERLOAD 条件需要 queueCapacity——ClassifierConfig 不含此参数 | P0 | FIX：ClassifierConfig 添加 queueCapacity 字段 |
| F04 | IR-v0.13-005 | minPoolSize=0 的 ThresholdPolicyConfig 无法构造 | P1 | FIX：重新定义 safety 维度评分规则 |
| F05 | IR-v0.13-005 | "peakPoolSize" 术语在 NormalizedPressureMetrics 中不存在 | P1 | FIX：统一术语为 metrics().maxPoolSize() |
| F06 | IR-v0.13-007 | SystemCpuProbe 注入方式未定义——静态方法签名不能变 | P1 | FIX：添加重载方法或明确内部创建策略 |
| F07 | IR-v0.13-005 | compositeScore ≈ weighted sum 构造器校验不可靠 | P2 | DEFER_TO_SR |
| F08 | IR-v0.13-003 | ClassifierConfig 需要确认不依赖 executor 配置 | P2 | DEFER_TO_SR |

## 5. 正向检查通过项

- [x] IR 不隐含实现授权 — 各条目使用"候选验收语义"措辞，第 9 节明确"不授权实现或 OpenSpec change"
- [x] Scope 边界明确排除闭环调整、策略学习、多策略投票、统计检验、新 executor mutation
- [x] 非范围列表与 `00-objectives-and-scope.md` 和 `decision-log.md` 的 DFR 一致
- [x] 正确识别不涉及新的 TPE 属性变更（v0.13.0 无新 executor mutation）
- [x] JDK API 可行性评估完整（ManagementFactory 零依赖）
- [x] 复用 `ObservedSnapshot`、`PressureSnapshot`、`ThresholdPolicyConfig`、`EvidenceRecorder` 等基础设施
- [x] PressureState 的 6 个状态覆盖了压力生命周期的关键阶段（前兆→高峰→恶化→恢复→低谷→稳态）
- [x] `NormalizedPressureMetrics` 明确与 `NormalizedComparisonMetrics` 独立（无继承关系）
- [x] 策略评分 4 维度（responsiveness/safety/stability/efficiency）与 decision-log D4 一致
- [x] Change decomposition 与 decision-log D6 一致（2 changes）
- [x] 端到端测试覆盖分类→评分→排序完整链路（9 个测试场景）
- [x] PolicyRanker 排序语义明确（降序、稳定、空列表处理）
- [x] CPU probe 跨平台策略明确（Sun JDK → getProcessCpuLoad, 非 Sun JDK → 0.0）
- [x] 风险登记表覆盖了分类准确性、字段同步、跨平台兼容、参数依赖等关键风险
- [x] 出口条件清单与 `managed-change-standard.md` 第 2 节一致
- [x] 不涉及 production 环境、外部依赖、REST/API/UI
- [x] `EvidenceRecorder`、`PressureSampler`、`PolicyEvaluator` 接口保持不变
- [x] IR Review 输入包引用 23 个文件，覆盖所有相关源码

## 6. 评审结论

IR 草案在利用现有基础设施（`ObservedSnapshot`、`PressureSnapshot`、`ThresholdPolicyConfig`、`NormalizedComparisonMetrics` 模式）和需求边界定义方面**合格**。8 条需求覆盖了从 `PressureState` 到端到端验证的完整链路。分类→评分→排序的诊断层设计与 roadmap 的 evidence-first 策略一致。

但**不能直接进入 SR**：存在 3 个 P0 阻断项：

- **F01**: `rejectedTaskCount` 数据来源不可用——`NormalizedPressureMetrics.fromSnapshots()` 会硬编码为 0，导致 REJECTION_ACTIVE 分类永远无法触发。需要添加 `withRejectedTaskCount()` 方法
- **F02**: RECOVERY 检测需要"前一个分类"信息——与 `SnapshotPressureClassifier` 的"无状态"要求矛盾。需要重新定义 RECOVERY 条件为纯趋势特征
- **F03**: OVERLOAD 条件的 `queueCapacity * 0.5` 阈值无法计算——`ClassifierConfig` 不含 `queueCapacity`。需要添加该字段

以及 3 个 P1 关键项（F04 safetyScore 测试不可执行、F05 "peakPoolSize" 术语不一致、F06 CPU probe 注入方式未定义）。

P0/P1 必须通过 disposition 关闭后才能进入 SR。

评审建议：**进入 IR disposition（`12-ir-review-disposition.md`）**，逐项处置 F01-F08。
