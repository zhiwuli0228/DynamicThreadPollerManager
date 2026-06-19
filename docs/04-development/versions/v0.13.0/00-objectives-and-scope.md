# v0.13.0 目标与范围

## Header

- Version name: `v0.13.0`
- Status: `VERSION_DESIGN_DRAFT`
- Current phase: `VERSION_BASELINE`
- Requirement theme: pressure state classification, trend-based classifier, policy scoring, policy ranking, CPU utilization probe

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
| v0.12.0 | 基线比较实验框架（BaselineExecutorCatalog, ComparableScenarioRunner, NormalizedComparisonMetrics, ComparisonReportArtifact） | IMPLEMENTED |

### 1.2 当前缺口

系统已具备完整的压力采样、持久化录制和基线比较能力，但在"诊断层"存在三个关键缺口：

1. **无压力状态分类** — `ThresholdPolicyEvaluator` 能根据单快照阈值做出 SCALE_UP/SCALE_DOWN/HOLD 的二值决策（第 35-57 行），但从不说"当前处于 overload 状态"或"正经历 queue buildup"。策略评估器关注"应该做什么"，不关注"现在是什么状态"。缺失的是一个独立的 `PressureClassifier`，能将快照序列映射到语义化的压力状态标签。

2. **无趋势分析** — 现有评估器仅检查单个 `PressureSnapshot` 的即时值。它无法区分"队列正在增长"（queue buildup——需要关注但尚未超载）和"队列稳定但高"（可能是正常高吞吐状态）。缺少对快照时间序列的 delta 分析（连续快照间队列深度变化率、活跃线程变化率）。

3. **无策略评分** — 当存在多个 `ThresholdPolicyConfig` 变体（不同的扩缩容阈值、不同的步长、不同的边界）时，没有机制评估哪个策略更适合当前压力状态。系统可以应用策略，但无法对策略进行评分或排序。

此外，`PressureSnapshot.cpuUtilization` 字段自 v0.1.0 起定义但始终为 `0.0`（通过 `DefaultSnapshotAssembler` 将 absent 映射为 0）。v0.12.0 DFR-01 将其延后到 v0.13.0。`RuntimeObservation` 的 `cpuUtilization` 字段在 `fromExecutor()` 中始终设置为 `MetricValue.absent()`。

### 1.3 JDK API 可行性评估

v0.13.0 不引入新的 `ThreadPoolExecutor` 属性变更，无需 JDK API 评估。本版本聚焦于诊断层（classification + scoring）和数据源（CPU probe）。

CPU probe 使用 JDK 标准 API：

| 问题 | 答案 |
|---|---|
| 是否需要新 `ThreadPoolExecutor` 属性变更？ | 否 — 本版本不引入新 executor mutation |
| CPU 数据源是否需要外部依赖？ | 否 — `java.lang.management.ManagementFactory.getOperatingSystemMXBean()` 提供标准 API |
| CPU 数据源是否跨平台？ | 部分 — `com.sun.management.OperatingSystemMXBean` 在 Oracle/OpenJDK 上可用；`getSystemLoadAverage()` 作为 Unix fallback；Windows 上可能需要 `com.sun.management` 扩展 |
| 分类器是否需要修改 executor 行为？ | 否 — 分类器仅消费已存在的快照数据 |

### 1.4 与既有基础设施的关系

- `ObservedSnapshot` 和 `PressureSnapshot` 已包含分类所需的全部原始字段（activeThreads, poolSize, queueSize, completedTaskCount, cpuUtilization）
- `NormalizedComparisonMetrics`（v0.12.0）的 9 个归一化指标是分类器的天然输入——`NormalizedPressureMetrics` 可直接复用此模式
- `EvidenceRecorder.snapshots(runId)` 提供分类器所需的时间序列输入
- `ThresholdPolicyConfig` 的 7 个字段为策略评分提供完整的策略参数
- `PressureSampler` 接口保持不变——分类器是采样流水线的新消费者，不改变采样行为
- `PolicyEvaluator` 和 `ThresholdPolicyEvaluator` 保持不变——分类是独立于策略评估的正交关注点
- `ComparableScenarioRunner`（v0.12.0）的比较基础设施可用于验证"不同策略在同一压力状态下评分不同"的语义

### 1.5 为什么是现在

- v0.12.0 完成了比较框架——系统现在能证明"managed 是否优于 baseline"，但无法解释"在当前压力状态下哪个策略最合适"
- roadmap 将 v0.13.0 定位为 v0.14.0（adaptive closed-loop adjustment）的直接前置——必须先有诊断层，才能做闭环决策
- 压力分类是安全闭环调整的前提：在不知道"现在是什么状态"之前，不应让系统自主调整 executor 参数
- 策略评分是策略选择的依据：闭环调整需要从多个候选策略中选择最优策略，评分提供了选择标准
- CPU utilization 是压力分类的关键信号之一：仅靠线程数和队列深度无法区分"CPU 密集型过载"和"IO 等待型队列堆积"

## 2. 目标

`v0.13.0` 聚焦以下目标：

1. **PressureState 分类模型**：定义 6 个语义化压力状态（UNDER_UTILIZED, NORMAL, QUEUE_BUILDUP, OVERLOAD, REJECTION_ACTIVE, RECOVERY），每种状态有明确的触发条件和置信度评分
2. **趋势感知分类器**：`SnapshotPressureClassifier` 从快照时间序列中计算队列增长率、线程利用率变化率，区分"队列增长中"和"队列稳定但高"
3. **NormalizedPressureMetrics**：从 `List<ObservedSnapshot>` 计算 11 个指标（9 个复用 NormalizedComparisonMetrics + 2 个派生信号：queueGrowthRate, threadUtilizationRatio）
4. **策略评分模型**：4 维度评分（Responsiveness, Safety, Stability, Efficiency），每个策略针对当前压力状态产出 0.0-1.0 的综合评分及逐维度分解
5. **策略排序器**：`PolicyRanker` 对多个 `ThresholdPolicyConfig` 按评分排序，产出排序列表和选择理由
6. **CPU 利用率数据源**：`SystemCpuProbe` 通过 JDK `ManagementFactory` 读取真实 CPU 利用率，集成到 `RuntimeObservation` → `PressureSnapshot` 流水线
7. **端到端验证**：至少一个场景中分类器正确识别压力状态转换（UNDER_UTILIZED → NORMAL → QUEUE_BUILDUP → OVERLOAD），且至少 3 个策略的评分排序符合直觉

## 3. 范围内

- `PressureState` enum（6 个状态值）
- `PressureClassification` record（state, confidence, evidence, classifiedAt）
- `PressureClassifier` 接口（classify from snapshots）
- `SnapshotPressureClassifier` 实现（趋势感知，使用连续快照 delta）
- `NormalizedPressureMetrics` record（11 个指标：9 个基础 + 2 个派生信号）
- `PolicyScore` record（compositeScore, responsivenessScore, safetyScore, stabilityScore, efficiencyScore, explanation）
- `PolicyScorer` 接口（score policy config against pressure classification）
- `ThresholdPolicyScorer` 实现（基于规则的启发式评分）
- `PolicyRanker` 类（对多个 config 排序）
- `SystemCpuProbe` 类（封装 ManagementFactory CPU 读取）
- `RuntimeObservation` 修改：`fromExecutor()` 中集成 `SystemCpuProbe` 读取 CPU
- `experiment.classification` 新包
- `experiment.probe` 新包
- 现有 708 测试零回归

## 4. 范围外

- 自适应闭环调整（v0.14.0 候选）
- 策略参数自动调优/学习
- 多策略集成或投票
- 统计显著性检验（v0.12.0 DFR-03）
- 跨 run 策略性能比较（v0.12.0 比较基础设施已存在，但不用于本版本评分模型）
- 复杂 workload 类型扩展（v0.16.0 候选）
- 新 executor mutation 或动态配置维度
- 外部监控系统集成
- CLI entry

## 5. 架构对齐

| 架构文档 | 本版本对应内容 |
|---|---|
| `observability-and-experiment-strategy.md` | "观察行为后再优化" — 压力分类是观察层的最后一环，策略评分是优化决策的前置条件 |
| `operational-and-evolution-boundaries.md` | 不引入新 executor mutation、不引入外部依赖、不扩展 Redis/Kafka/数据库边界 |
| `managed-executor-domain-model.md` | 扩展诊断层（classification + scoring），不修改 ManagedExecutor 自身 |
| roadmap.md | 实现候选 v0.13.0："classify overload, underutilization, queue buildup, rejection pressure, and recovery states before taking action" |

## 6. 模块边界

| 模块 | 变更类型 | 说明 |
|---|---|---|
| `experiment.classification` | **新增** `PressureState` | 6 状态枚举 |
| `experiment.classification` | **新增** `PressureClassification` | 分类结果 record（state + confidence + evidence） |
| `experiment.classification` | **新增** `PressureClassifier` | 分类器接口 |
| `experiment.classification` | **新增** `SnapshotPressureClassifier` | 趋势感知实现 |
| `experiment.classification` | **新增** `NormalizedPressureMetrics` | 11 指标 record |
| `experiment.classification` | **新增** `PolicyScore` | 策略评分 record（4 维度 + 综合） |
| `experiment.classification` | **新增** `PolicyScorer` | 评分器接口 |
| `experiment.classification` | **新增** `ThresholdPolicyScorer` | 规则式启发评分实现 |
| `experiment.classification` | **新增** `PolicyRanker` | 策略排序器 |
| `experiment.probe` | **新增** `SystemCpuProbe` | CPU 利用率数据源 |
| `experiment.metrics` | **修改** `RuntimeObservation` | `fromExecutor()` 集成 CPU probe |
| `experiment.metrics` | **修改** `DefaultSnapshotAssembler` | 保留 cpuUtilization 从 absent 到实际值的映射 |
| `experiment.model` | 不变 | PressureSnapshot 字段已存在 |
| `experiment.policy` | 不变 | 分类和评分是正交关注点 |
| `experiment.scenario` | 不变 | 仅作为分类器输入消费 |
| `experiment.executor` | 不变 | 无新 mutation |

### 依赖方向

```text
experiment.classification (PressureState, PressureClassifier, SnapshotPressureClassifier,
                           NormalizedPressureMetrics, PolicyScore, PolicyScorer,
                           ThresholdPolicyScorer, PolicyRanker)
    ├── experiment.metrics (ObservedSnapshot, PressureSnapshot, RuntimeObservation — 读取)
    ├── experiment.policy (ThresholdPolicyConfig — 读取，PolicyEvaluator — 不变)
    ├── experiment.scenario (NormalizedComparisonMetrics — 模式复用)
    └── experiment.probe (SystemCpuProbe — CPU 数据源)

experiment.probe (SystemCpuProbe)
    └── java.lang.management.ManagementFactory (JDK 标准 API)
```

## 7. 核心技术设计

### 7.1 PressureState 枚举

```java
public enum PressureState {
    UNDER_UTILIZED,    // 线程空闲，队列空或接近空
    NORMAL,            // 平衡状态：线程工作中，队列可管理
    QUEUE_BUILDUP,     // 队列增长中，线程尚未饱和
    OVERLOAD,          // 线程饱和，队列增长，可能出现拒绝
    REJECTION_ACTIVE,  // 任务正被拒绝（仅 managed executor）
    RECOVERY           // 曾过载/堆积，正在降温：线程减少，队列排空中
}
```

**触发条件（设计草案，IR 阶段细化）：**

| 状态 | 条件 |
|---|---|
| UNDER_UTILIZED | activeThreads < corePoolSize * 0.3 且 queueSize == 0 且无拒绝 |
| NORMAL | activeThreads 在 [corePoolSize * 0.3, corePoolSize * 0.8] 且 queueSize < queueCapacity * 0.3 |
| QUEUE_BUILDUP | queueSize 在连续 3+ 快照中递增（queueGrowthRate > 0）且 activeThreads < maxPoolSize * 0.8 |
| OVERLOAD | activeThreads >= maxPoolSize * 0.8 且 queueSize >= queueCapacity * 0.5 |
| REJECTION_ACTIVE | rejectedTaskCount > 0（最近快照窗口内） |
| RECOVERY | 前一个状态为 OVERLOAD/QUEUE_BUILDUP/REJECTION_ACTIVE 且 activeThreads 递减 且 queueSize 递减 |

### 7.2 PressureClassifier 接口和实现

```java
public interface PressureClassifier {
    PressureClassification classify(List<ObservedSnapshot> snapshots, ClassifierConfig config);
}

public record ClassifierConfig(
    int trendWindowSize,         // 趋势计算窗口（快照数），默认 5
    double queueGrowthThreshold, // 队列增长率阈值，默认 0.1（10%/快照）
    int rejectionWindowSize      // 拒绝检测窗口（快照数），默认 10
) {
    public static ClassifierConfig defaults() { ... }
}

public record PressureClassification(
    PressureState state,
    double confidence,           // 0.0-1.0
    List<String> evidence,       // 分类依据（可读描述）
    NormalizedPressureMetrics metrics,
    Instant classifiedAt
) {}
```

`SnapshotPressureClassifier` 实现：
1. 从快照列表计算 `NormalizedPressureMetrics`（含 queueGrowthRate, threadUtilizationRatio）
2. 检查拒绝信号：如果 `rejectedTaskCount > 0`（最近 rejectionWindowSize 个快照内） → REJECTION_ACTIVE
3. 计算趋势：连续 trendWindowSize 个快照的 queueSize delta
4. 应用触发条件规则（按优先级：REJECTION_ACTIVE > OVERLOAD > QUEUE_BUILDUP > RECOVERY > UNDER_UTILIZED > NORMAL）
5. 计算置信度（基于条件匹配程度：完全匹配 → 0.95+，部分匹配 → 0.6-0.9）
6. 组装 evidence 列表（哪些条件被触发，当前值，阈值）

### 7.3 NormalizedPressureMetrics

复用 `NormalizedComparisonMetrics` 的 9 个字段，添加 2 个派生信号：

```java
public record NormalizedPressureMetrics(
    // 9 个基础指标（与 NormalizedComparisonMetrics 同名同语义）
    long completedTaskCount,
    long rejectedTaskCount,
    double avgQueueDepth,
    int maxQueueDepth,
    long totalDurationMs,
    double throughputPerSecond,
    double avgActiveThreads,
    int maxPoolSize,
    int snapshotCount,

    // 2 个派生分类信号
    double queueGrowthRate,         // 最近 trendWindowSize 个快照的 queueSize 线性增长率
    double threadUtilizationRatio   // avgActiveThreads / maxPoolSize
) {
    public static NormalizedPressureMetrics fromSnapshots(
        List<ObservedSnapshot> snapshots,
        long totalDurationMs,
        int fallbackPoolSize,
        int trendWindowSize
    ) { ... }
}
```

`queueGrowthRate` 计算：对最近 trendWindowSize 个快照的 `queueSize` 做线性回归斜率，归一化为每快照增长率（正=增长，负=减少，0=稳定）。

`threadUtilizationRatio` 计算：`avgActiveThreads / maxPoolSize`，范围 [0.0, 1.0]。

### 7.4 策略评分模型

```java
public interface PolicyScorer {
    PolicyScore score(PressureClassification classification, ThresholdPolicyConfig config);
}

public record PolicyScore(
    String policyId,
    double compositeScore,          // 0.0-1.0 综合评分
    double responsivenessScore,     // 响应性：策略阈值是否匹配当前压力水平
    double safetyScore,             // 安全性：策略参数是否在安全边界内
    double stabilityScore,          // 稳定性：步长是否适合当前压力波动
    double efficiencyScore,         // 效率：是否避免过度配置
    String explanation              // 人类可读的评分解释
) {}
```

**ThresholdPolicyScorer** 使用规则式启发评分：

| 维度 | 评分逻辑 |
|---|---|
| Responsiveness | 策略的 scaleUpActiveThreadsThreshold 与当前 activeThreads 的匹配度；scaleUpQueueSizeThreshold 与当前 queueSize 的匹配度。阈值越接近实际压力水平（但不低于），分数越高 |
| Safety | minPoolSize/maxPoolSize 是否在合理范围内（基于当前 poolSize 和历史波动）；scaleStep 是否 <= maxPoolSize * 0.25（避免过大步长） |
| Stability | scaleStep 与 queueGrowthRate 的匹配度；高波动压力 + 大步长 → 低分；低波动 + 适中步长 → 高分 |
| Efficiency | maxPoolSize 是否过度配置（相对于观测到的峰值 activeThreads） |

综合评分 = responsiveness * 0.35 + safety * 0.30 + stability * 0.20 + efficiency * 0.15

**PolicyRanker**:
```java
public final class PolicyRanker {
    private final PolicyScorer scorer;

    public List<PolicyScore> rank(PressureClassification classification,
                                   List<ThresholdPolicyConfig> candidates);

    public Optional<PolicyScore> best(PressureClassification classification,
                                       List<ThresholdPolicyConfig> candidates);
}
```

### 7.5 CPU 利用率数据源

```java
public final class SystemCpuProbe {
    private final OperatingSystemMXBean osBean;

    public SystemCpuProbe() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
    }

    public double sampleProcessCpuLoad() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
            double load = sunBean.getProcessCpuLoad();
            return load < 0 ? 0.0 : load; // -1 = 不可用，映射为 0
        }
        return 0.0; // 非 Sun JDK fallback
    }

    public double sampleSystemCpuLoad() {
        double load = osBean.getSystemLoadAverage();
        return load < 0 ? 0.0 : load;
    }
}
```

**集成点：** 修改 `RuntimeObservation.fromExecutor(ManagedExecutor, Instant)`，调用 `new SystemCpuProbe().sampleProcessCpuLoad()` 填充 `cpuUtilization` 字段（当前为 `MetricValue.absent()`）。

`DefaultSnapshotAssembler` 无需修改：它已经将 `MetricValue.absent()` 映射为 `0.0`，现在 `cpuUtilization` 将有实际值。

### 7.6 分类器与策略评估器的关系

分类器（`PressureClassifier`）和策略评估器（`PolicyEvaluator`）是正交的、互补的组件：

| 维度 | PressureClassifier | ThresholdPolicyEvaluator |
|---|---|---|
| 输入 | `List<ObservedSnapshot>`（时间序列） | `PolicyEvaluationInput`（单快照） |
| 输出 | `PressureClassification`（状态标签 + 置信度） | `PolicyDecision`（动作 + 门状态） |
| 目的 | "现在是什么状态？" | "现在应该做什么？" |
| 趋势 | 使用（delta 分析） | 不使用 |
| 消费关系 | 可为策略评分提供输入 | 独立于分类器 |

### 7.7 与 v0.12.0 比较基础设施的关系

v0.12.0 的 `NormalizedComparisonMetrics` 和 `ComparisonResult` 不直接用于分类或评分，但它们提供了验证路径：

- 可以通过比较同一场景下不同策略的 `ComparisonResult`，验证策略评分的高低是否与实际运行结果一致
- 这个交叉验证属于 v0.14.0 范围（闭环调整的反馈回路），但 v0.13.0 的设计确保数据模型兼容此扩展

## 8. 成功标准草案

- `SnapshotPressureClassifier` 从快照序列正确分类全部 6 种压力状态
- 至少 1 个场景中分类器经历 UNDER_UTILIZED → NORMAL → QUEUE_BUILDUP → OVERLOAD 完整转换链
- REJECTION_ACTIVE 状态在 managed executor 出现拒绝时正确触发
- RECOVERY 状态在 overload 后压力缓解时正确触发
- `NormalizedPressureMetrics` 正确计算所有 11 个指标（含 queueGrowthRate, threadUtilizationRatio）
- `NormalizedPressureMetrics` 对 baseline executor（无真实线程池）给出合理的默认值（activeThreads=0, queueDepth=0）
- `PolicyScorer` 产出 [0.0, 1.0] 范围评分，含 4 维度分解
- `PolicyRanker` 对至少 3 个不同策略配置正确排序
- 对 overload 状态，激进的 scale-up 策略（低阈值 + 大步长）的 responsiveness 评分 > 保守策略
- 对 under-utilized 状态，保守的 scale-down 策略的 efficiency 评分 > 激进策略
- `SystemCpuProbe.sampleProcessCpuLoad()` 在当前平台上返回非零值（integration test）
- CPU 利用率端到端流通：probe → RuntimeObservation.fromExecutor() → PressureSnapshot → 分类器可用
- 现有 708 测试零回归

## 9. 候选 Change Decomposition

IR/SR 阶段确认后：

| # | Change name | 范围 | 依赖 |
|---|---|---|---|
| 1/2 | `pressure-classification-engine` | PressureState, PressureClassifier 接口, SnapshotPressureClassifier, PressureClassification, NormalizedPressureMetrics, 单元测试 | ObservedSnapshot, PressureSnapshot, NormalizedComparisonMetrics（模式复用）, ClassifierConfig |
| 2/2 | `policy-scoring-and-cpu-probe` | PolicyScore, PolicyScorer 接口, ThresholdPolicyScorer, PolicyRanker, SystemCpuProbe, RuntimeObservation CPU 集成, 端到端验证 | Change 1（PressureClassification）, ThresholdPolicyConfig, ManagementFactory |

### 独立可验证性检查（预检）

- Change 1 可独立编译和测试：classifier 从快照列表产出 PressureClassification，无需任何策略评分代码
- Change 2 依赖 Change 1 的 `PressureClassification` 和 `NormalizedPressureMetrics`，但 Change 2 的评分和排序逻辑独立于 Change 1 的分类器内部实现
- 两个 change 均可独立运行 `mvn test` 并通过各自范围内的测试

## 10. 当前阶段出口

进入 IR 前必须完成：

1. `README.md` 版本索引
2. `00-objectives-and-scope.md`（本文档）
3. `decision-log.md` 记录关键设计判断
4. `docs/00-project/current-state.md` 反映 v0.13.0 版本设计草稿状态
