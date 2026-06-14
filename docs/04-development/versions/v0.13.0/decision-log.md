# v0.13.0 Decision Log

## D1: 压力状态模型设计

**背景**: 需要定义一组语义化压力状态标签，使系统能够从快照数据中识别当前 executor 的运行状态。`ThresholdPolicyEvaluator` 已有简单的三向决策（SCALE_UP/SCALE_DOWN/HOLD），但缺少独立的状态分类。

**选项**:
- A: 6 状态模型（UNDER_UTILIZED, NORMAL, QUEUE_BUILDUP, OVERLOAD, REJECTION_ACTIVE, RECOVERY）
- B: 3 状态模型（UNDER_UTILIZED, NORMAL, OVERLOAD）——合并 QUEUE_BUILDUP 到 OVERLOAD，合并 REJECTION_ACTIVE 到 OVERLOAD，省略 RECOVERY
- C: 8+ 状态模型（含 CRITICAL, THRASHING, STARVATION, IDLE 等细粒度状态）

**决策**: 选 A — 6 状态模型。

**理由**:
- 6 状态覆盖了压力生命周期的关键阶段：前兆（QUEUE_BUILDUP）、高峰（OVERLOAD）、恶化（REJECTION_ACTIVE）、恢复（RECOVERY）、低谷（UNDER_UTILIZED）、稳态（NORMAL）
- 3 状态（选项 B）丢失了趋势信息——QUEUE_BUILDUP 和 OVERLOAD 的关键区别在于"线程是否已饱和"，这个区别直接影响策略选择（build-up 阶段优先扩容线程可避免进入 overload；overload 阶段扩容已无济于事）
- REJECTION_ACTIVE 独立于 OVERLOAD 是必要的——队列有界时可能在线程未完全饱和时就出现拒绝
- RECOVERY 独立于 NORMAL 是必要的——恢复阶段的策略应保守（避免在恢复过程中过早缩容导致二次过载）
- 8+ 状态（选项 C）在当前数据采集精度下无法可靠区分（如 THRASHING 需要短时间内多次扩缩容的统计，STARVATION 需要区分 CPU 密集型和 IO 等待型过载——需要 CPU utilization 数据及 classification 历史）
- 6 状态模型可扩展——后续版本可添加子状态或复合状态

**影响**: `PressureState` enum 包含 6 个值。`SnapshotPressureClassifier` 的触发条件逻辑需要维护 6 条规则的优先级链。

---

## D2: 分类器设计——时间序列趋势 vs 单快照

**背景**: `ThresholdPolicyEvaluator` 基于单个 `PressureSnapshot` 做决策。分类器可以选择同样基于单快照（简单、与 evaluator 一致），或基于时间序列趋势（更准确、但更复杂）。

**选项**:
- A: 时间序列趋势分析——输入 `List<ObservedSnapshot>`（通常 10-100 个快照），计算连续快照间的 delta
- B: 单快照分类——输入单个 `ObservedSnapshot`，仅根据即时值判断状态
- C: 双模式——接口同时支持单快照和批量分类

**决策**: 选 A — 时间序列趋势分析。

**理由**:
- 时间序列是区分 QUEUE_BUILDUP（队列增长中）和 NORMAL（队列稳定但高）的唯一方式——单快照无法区分两者
- RECOVERY 需要前后快照比较（"曾经高 → 现在降低"），单快照无法实现
- v0.11.0 的 `FileBackedEvidenceRecorder` 和 v0.12.0 的 `EvidenceRecorder.snapshots(runId)` 使时间序列数据随时可用
- 分类器输入时间序列但可接受不同长度的序列：短序列（< 5 个快照）退化为单快照行为（置信度降低）；长序列启用完整趋势分析
- 单快照（选项 B）的简单性优势不足以弥补语义损失——缺少趋势分析意味着分类器的输出与 `ThresholdPolicyEvaluator` 的决策无异，失去了诊断层的独立价值
- 双模式（选项 C）增加接口复杂度但未增加实质能力——分类器内部已经处理短序列退化

**影响**: `PressureClassifier.classify()` 接受 `List<ObservedSnapshot>`。分类器内部使用 `trendWindowSize`（默认 5）计算趋势。短序列（< trendWindowSize）时置信度自动降低。

---

## D3: NormalizedPressureMetrics — 扩展 vs 新建

**背景**: v0.12.0 的 `NormalizedComparisonMetrics` 已经定义了 9 个归一化指标。压力分类需要这 9 个指标加上 queueGrowthRate 和 threadUtilizationRatio 两个派生信号。需要决定是扩展已有 record 还是新建独立 record。

**选项**:
- A: 新建 `NormalizedPressureMetrics` — 包含全部 11 个字段（9 基础 + 2 派生），独立的 `fromSnapshots()` 工厂方法
- B: 扩展 `NormalizedComparisonMetrics` — 在其上添加 2 个新字段
- C: 组合模式 — `NormalizedPressureMetrics` 包含一个 `NormalizedComparisonMetrics` 字段 + 2 个额外字段

**决策**: 选 A — 新建独立 record。

**理由**:
- `NormalizedComparisonMetrics` 用于比较报告，其 API 契约（`rejectedTaskCount` 默认 0、通过 `withRejectedTaskCount()` 修改）针对 comparison 场景设计
- 分类器需要的工厂方法签名不同：`fromSnapshots()` 需要 `trendWindowSize` 参数来计算派生信号，而 `NormalizedComparisonMetrics.fromSnapshots()` 的签名不接受此参数
- 两个 record 的使用场景不同：`NormalizedComparisonMetrics` 服务于 comparison report（A vs B 对比），`NormalizedPressureMetrics` 服务于分类器输入（单 executor 的诊断）
- 独立 record 避免修改 v0.12.0 的稳定 API——comparison report 不受分类器变更影响
- 组合模式（选项 C）增加一层间接寻址而不增加复用价值——分类器需要扁平访问所有 11 个字段
- 9 个基础字段在两个 record 中同名同语义，但这是有意为之——两个 record 服务于不同用例，保持独立可避免耦合

**影响**: `NormalizedPressureMetrics` 是一个新 record，包含 11 个独立字段。它与 `NormalizedComparisonMetrics` 共享 9 个字段的名称和语义，但在类型系统中无继承关系。

---

## D4: 策略评分模型 — 规则式 vs 模拟 vs 历史

**背景**: 策略评分需要为给定的 `PressureClassification` 和 `ThresholdPolicyConfig` 计算一个 [0.0, 1.0] 的评分。有几种不同的评分方法可选。

**选项**:
- A: 规则式启发评分（Rule-based heuristic）——基于预设规则和公式计算评分
- B: 模拟式评分（Simulation-based）——对历史快照回放策略，计算假设决策的匹配度
- C: 历史性能评分（Historical performance）——使用 v0.12.0 的比较结果作为 ground truth

**决策**: 选 A — 规则式启发评分。

**理由**:
- 规则式评分可解释、可审查、无需额外运行开销
- 4 维度分解（responsiveness, safety, stability, efficiency）覆盖了策略适配性的关键方面
- 模拟式评分（选项 B）需要完整的回放基础设施（`OfflinePolicyReplayService`），且评分结果依赖于模拟质量——"假设某策略在某历史快照下做出某决策"不一定反映策略在当前状态下的适配性
- 历史性能评分（选项 C）需要大量 comparison run 数据——v0.12.0 的比较基础设施是单场景单次运行，不具备统计意义的 ground truth；且不同场景的压力模式不同，历史数据不可直接迁移
- 规则式评分可作为后续版本的基线——v0.14.0（闭环调整）可以在实际运行中收集反馈数据，v0.15.0 可以用实际结果校准评分模型
- 启发式规则是透明的——每个维度的评分逻辑可以用自然语言解释，满足 roadmap 的"explain why a decision was made"目标

**评分权重分配理由**:
- Responsiveness (0.35): 最重要的维度——策略对当前压力状态的响应恰当性是核心目标
- Safety (0.30): 次重要——不安全的策略（如 min=0 或 max 过大）即使响应性好也不应被选中
- Stability (0.20): 避免振荡——频繁扩缩容的策略不适合波动性压力
- Efficiency (0.15): 资源效率——在满足前三个条件后，倾向于资源使用更高效的策略

**影响**: `ThresholdPolicyScorer` 实现 4 维度的启发式评分公式。权重可在 `ClassifierConfig` 或单独的 `ScorerConfig` 中配置。

---

## D5: CPU Probe 实现方式

**背景**: v0.12.0 DFR-01 将 CPU 利用率数据源延后到 v0.13.0。`PressureSnapshot.cpuUtilization` 字段存在但始终为 0.0。需要在 JDK 标准 API 范围内实现跨平台 CPU 读取。

**选项**:
- A: `com.sun.management.OperatingSystemMXBean` — 使用 JDK 内置的 Sun 扩展 API
- B: 外部库（如 OSHI `com.github.oshi:oshi-core`）— 全平台支持
- C: JNI/JNA 自定义实现 — 直接调用 OS API

**决策**: 选 A — JDK 内置 `com.sun.management.OperatingSystemMXBean`。

**理由**:
- `com.sun.management.OperatingSystemMXBean` 在 Oracle JDK 和 OpenJDK 上均可用（自 Java 7 起），提供 `getProcessCpuLoad()`（进程级）和 `getSystemCpuLoad()`（系统级）
- 零外部依赖——符合项目架构约束（`operational-and-evolution-boundaries.md` 明确禁止引入不必要的依赖）
- `getProcessCpuLoad()` 返回 [0.0, 1.0] 的 double（自上次调用以来的 CPU 时间占比），语义与 `PressureSnapshot.cpuUtilization` 的 double 字段完全匹配
- 首次调用返回 -1（未初始化）——`SystemCpuProbe` 封装此细节，映射为 0.0
- 对于不支持 `com.sun.management` 的 JVM（理论上可能，实际上罕见），fallback 到 `getSystemLoadAverage()`（Unix）或返回 0.0（Windows 非 Sun JVM）
- OSHI（选项 B）功能全面但引入 ~2MB 依赖（oshi-core + JNA），对于单个 CPU 读数的需求而言过于重量级
- JNI/JNA（选项 C）引入平台相关原生代码，维护成本高，与项目"纯 Java"目标冲突

**跨平台策略**:

| 平台 | 主要 API | Fallback |
|---|---|---|
| Linux/OpenJDK | `com.sun.management.getProcessCpuLoad()` | `getSystemLoadAverage()` |
| macOS/OpenJDK | `com.sun.management.getProcessCpuLoad()` | `getSystemLoadAverage()` |
| Windows/OpenJDK | `com.sun.management.getProcessCpuLoad()` | 返回 0.0（无 fallback） |

**影响**: `SystemCpuProbe` 是一个轻量封装类（~30 行）。`RuntimeObservation.fromExecutor()` 中新增一行 `cpuUtilization = MetricValue.present(cpuProbe.sampleProcessCpuLoad())`。无新增 Maven 依赖。

---

## D6: Change 分解策略

**背景**: v0.13.0 包含两个子能力：pressure classification engine（分类器核心）和 policy scoring + CPU probe（评分和 CPU 数据源）。

**选项**:
- A: 单 change（`pressure-classification-and-policy-scoring`）
- B: 双 change（`pressure-classification-engine` → `policy-scoring-and-cpu-probe`）

**决策**: 选 B — 双 change。

**理由**:
- Change 1（`pressure-classification-engine`）可独立编译、独立测试：PressureState + PressureClassifier + SnapshotPressureClassifier + NormalizedPressureMetrics 可从快照序列产出 PressureClassification，无需任何策略评分代码
- Change 2（`policy-scoring-and-cpu-probe`）依赖 Change 1 的 `PressureClassification` 和 `NormalizedPressureMetrics` 类型，但评分和排序逻辑独立于分类器内部实现
- Change 1 的测试验证"系统能从快照序列正确分类压力状态"，Change 2 的测试验证"系统能对分类结果评分、排序并集成 CPU 数据源"
- 符合 managed-change-standard 的独立可验证性规则
- 遵循 v0.11.0/v0.12.0 建立的双 change 模式

**影响**: 2 个 OpenSpec changes。Change 1 创建新包 `experiment.classification`。Change 2 创建新包 `experiment.probe` 并修改 `RuntimeObservation`。

---

## DFR: Deferred 项

| ID | 描述 | 理由 | 后续版本 |
|---|---|---|---|
| DFR-01 | 策略评分权重的自动校准 | 需要 v0.14.0 闭环调整的实际运行数据作为反馈信号 | 候选 v0.14.0+ |
| DFR-02 | 状态转换图/状态机 | 6 状态之间的合法转换（如 UNDER_UTILIZED → RECOVERY 是否合法）在当前版本使用优先级线解决，不需要显式状态机 | 候选 v0.14.0 |
| DFR-03 | 复合压力状态（如 OVERLOAD + REJECTION_ACTIVE） | 当前使用优先级链（REJECTION_ACTIVE > OVERLOAD），单标签足够；复合状态增加复杂度而不在当前需求范围内 | 候选 v0.15.0+ |
| DFR-04 | 分类器性能基准（分类延迟 < 1ms for 100 snapshots） | 分类器仅对历史快照数据做数值计算，性能瓶颈不在当前范围 | 候选 v0.14.0+ |
| DFR-05 | CPU system load 与 process load 的关联分析 | 两个指标分别反映系统级和进程级 CPU 使用，关联分析需要更多数据积累 | 候选 v0.15.0+ |
| DFR-06 | 多核 CPU 的 per-core 利用率 | `getProcessCpuLoad()` 返回的是所有核的平均值，per-core 需要额外的 API（`getAvailableProcessors()` 已可用） | 候选 v0.15.0+ |
