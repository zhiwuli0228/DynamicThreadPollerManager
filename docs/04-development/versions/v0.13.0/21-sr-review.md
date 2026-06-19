# v0.13.0 SR Independent Review

## Header

- Document type: SR independent review
- Version name: `v0.13.0`
- Reviewed artifact: `docs/04-development/versions/v0.13.0/20-sr.md`
- Review date: `2026-06-14`
- Reviewer: independent design reviewer (separate from SR author)
- Review basis: SR functional design, IR baseline (10-ir.md), IR review disposition (12-ir-review-disposition.md), existing codebase verification

## Review Method

逐组件阅读 SR 设计，对照现有代码库验证每个 API 签名声明，检查内部一致性（组件间契约对齐）、架构约束遵守（依赖方向、模块边界）、IR FIX 项落地、SR 伪代码强制验证规则（3 个随机 API 调用点签名验证）、以及测试策略覆盖度。对所有发现分配 P0/P1/P2 级别。

## API 签名强制验证（SR 伪代码规则）

随机抽取 3 个 API 调用点，读取实际源码验证签名匹配：

| # | SR 调用点 | SR 位置 | 实际源码 | 验证结果 |
|---|---|---|---|---|
| 1 | `ManagedExecutor.getActiveCount() → int` | §4.12 fromExecutor() | `ManagedExecutor.java:308` — `public int getActiveCount()` 签名完全匹配 | **PASS** |
| 2 | `PressureSnapshot.timestamp() → Instant` | §4.6 computeDurationMs() | `PressureSnapshot.java:34` — `public Instant timestamp()` 签名完全匹配 | **PASS** |
| 3 | `RuntimeObservation.fromExecutor(ManagedExecutor, Instant)` 委托到 3-arg 重载 | §4.12 | `RuntimeObservation.java:83` — 当前 public static 方法签名完全匹配；3-arg 重载为新增，参数类型均可解析 | **PASS** |

3/3 API 签名验证通过。补充验证：
- `ManagedExecutor.getPoolSize() → int` — verified (line 315)
- `ManagedExecutor.getQueueSize() → int` — verified (line 322)
- `ManagedExecutor.getCompletedTaskCount() → long` — verified (line 329)
- `ManagedExecutor.getKeepAliveTime(TimeUnit) → long` — verified (line 301)
- `ManagedExecutor.getLargestPoolSize() → int` — verified (line 336)
- `ManagedExecutor.getTaskCount() → long` — verified (line 343)
- `MetricValue.present(T)` / `MetricValue.absent()` — verified (MetricValue.java)
- `ThresholdPolicyConfig` 7 getters — verified (ThresholdPolicyConfig.java:71-97)
- `PressureSnapshot.activeThreads() → int`, `queueSize() → int`, `poolSize() → int`, `completedTaskCount() → long` — verified (PressureSnapshot.java)

## Findings Summary

| Total Findings | P0 | P1 | P2 |
|---|---|---|---|
| 7 | 2 | 3 | 2 |

---

## P0 Findings (Blockers — must resolve before SR closure)

### F01 [P0] SnapshotPressureClassifier 内部创建 NormalizedPressureMetrics 导致 rejectedTaskCount 永远为 0 — REJECTION_ACTIVE 无法触发

**位置**: SR 4.6 `SnapshotPressureClassifier.classify()` + SR 4.3 `NormalizedPressureMetrics.fromSnapshots()`

**问题**: 分类器的 classify() 方法内部调用 `NormalizedPressureMetrics.fromSnapshots(...)` 创建 metrics，其中 `rejectedTaskCount = 0L`。然后同一方法中检查：

```java
if (metrics.rejectedTaskCount() > 0) {
    return build(metrics, PressureState.REJECTION_ACTIVE, ...);
}
```

但 `fromSnapshots()` 硬编码 `0L` 作为 rejectedTaskCount（SR §4.3 第 30 行），且 `classify()` **从未调用 `withRejectedTaskCount()`**。

SR §4.6 的注释说"消费方可以在 classify() 前通过 withRejectedTaskCount() 注入"——但 `withRejectedTaskCount()` 是 `NormalizedPressureMetrics` 的实例方法，而 metrics 是在 `classify()` **内部**创建的。消费方无法在 classify() 内部注入此值。

数据流断裂:
```
ObservedSnapshot list (no rejectedTaskCount)
  → fromSnapshots() → rejectedTaskCount = 0
  → classify() → REJECTION_ACTIVE check → NEVER TRUE
```

IR-v0.13-002 要求 `REJECTION_ACTIVE: rejectedTaskCount > 0` 触发——此需求在当前 SR 设计中不可达。

**影响**: 分类器永远无法返回 REJECTION_ACTIVE 状态。IR 的 6 状态模型中有一个状态完全不可用。

**推荐处置**: FIX。三种方案：

方案 A（推荐）: 在 `PressureClassifier.classify()` 接口中添加 `long rejectedTaskCount` 参数：
```java
PressureClassification classify(
    List<ObservedSnapshot> snapshots,
    ClassifierConfig config,
    long rejectedTaskCount);
```
分类器内部将此值传给 `metrics.withRejectedTaskCount(rejectedTaskCount)`，再执行分类逻辑。

方案 B: classify() 接受预构建的 `NormalizedPressureMetrics` 而非原始快照列表——消费方负责构建 metrics（含 rejectedTaskCount 注入），分类器只做分类逻辑。

方案 C: 从 IR 中移除 REJECTION_ACTIVE 状态，将其延后到 rejectedTaskCount 可直接从快照获取的版本。

推荐方案 A——最小接口变更，保持分类器对快照列表的直接访问（趋势计算需要原始序列），同时明确 rejectedTaskCount 来自外部源。

---

### F02 [P0] shortSequenceConfidenceFactor() 未被 classify() 调用——短序列置信度衰减不生效

**位置**: SR 4.6 `SnapshotPressureClassifier.shortSequenceConfidenceFactor()` + `classify()`

**问题**: SR §4.6 将 `shortSequenceConfidenceFactor()` 定义为 public static 方法，注释说明"调用方应在 classify() 后乘以衰减因子"。但 `classify()` 方法自身**从未调用**此方法。SR 将衰减责任推给 classify() 的消费方，但：

1. `classify()` 已经返回 `PressureClassification` record（不可变），消费方需要解构→修改 confidence→重建 record——这是不必要的复杂度
2. 如果消费方忘记调用 `shortSequenceConfidenceFactor()`，短序列将以完整置信度返回分类结果——与 IR-v0.13-002 的退化要求冲突

IR-v0.13-002 明确要求："短序列处理（< trendWindowSize 个快照）：退化为单快照分类（trend 相关信号使用 0）。置信度降低（乘以 snapshots.size() / trendWindowSize）"

**影响**: 短序列（1-4 个快照）的置信度不会自动降低。消费方容易遗漏此调用，导致分类器行为与 IR 需求不一致。

**推荐处置**: FIX。将 `shortSequenceConfidenceFactor()` 调用移入 `classify()` 方法内部：

在 `classify()` 的 return 路径（每个 `build()` 调用）中，对 confidence 乘以衰减因子后再构建 `PressureClassification`:
```java
private PressureClassification build(...) {
    double adjustedConfidence = confidence * shortSequenceConfidenceFactor(
            snapshotCount, config.trendWindowSize());
    return new PressureClassification(state, clamp(adjustedConfidence), ...);
}
```

同时将 `shortSequenceConfidenceFactor()` 从 public 改为 private。

---

## P1 Findings (Important — should resolve; acceptable with documented rationale)

### F03 [P1] ThresholdPolicyScorer.scoreResponsiveness() 使用 avgActiveThreads 和 maxQueueDepth——与分类器指标不一致

**位置**: SR 4.9 `ThresholdPolicyScorer.scoreResponsiveness()`

**问题**: scorer 的 Responsiveness 维度使用 `metrics.avgActiveThreads()` 与 `config.scaleUpActiveThreadsThreshold()` 比较，以及 `metrics.maxQueueDepth()` 与 `config.scaleUpQueueSizeThreshold()` 比较。

但分类器（`SnapshotPressureClassifier`）在判断 OVERLOAD 时使用的是 `threadUtilizationRatio`（比例值）和 `maxQueueDepth >= queueCapacity * 0.5`（相对值）。两者使用不同的指标体系：scorer 用绝对值，classifier 用相对值。

对于相同的压力状态，classifier 说 "OVERLOAD (utilization=0.85)"，但 scorer 的 responsiveness 计算说 "activeThreads=6, threshold=24 → ratio=0.25 → low proximity"。同一个 executor 状态在不同组件中被用不同标准评估。

这不是功能 bug——scorer 和 classifier 的职责不同——但这种不一致可能导致：同一状态下 classifier 判定 OVERLOAD 但 scorer 给出低 responsiveness 评分（因为 scorer 用绝对 activeThreads 而非比例）。

**影响**: 中等。OVERLOAD 状态下 classifier 和 scorer 可能产生直觉上矛盾的结果。

**推荐处置**: FIX。将 scorer 的 responsiveness 计算对齐到 classifier 的指标：
- 使用 `metrics.threadUtilizationRatio()` 替代 `metrics.avgActiveThreads()` 进行 scale-up 响应性评估
- OVERLOAD/QUEUE_BUILDUP 状态下，utilizationRatio 越高 → responsivenessScore 越高（策略的 scale-up 阈值越可能被触发）
- UNDER_UTILIZED/RECOVERY 状态下，utilizationRatio 越低 → responsivenessScore 越高（策略的 scale-down 阈值越可能被触发）

---

### F04 [P1] NormalizedPressureMetrics 缺乏 toMap/fromMap——阻碍调试和序列化

**位置**: SR 4.3 `NormalizedPressureMetrics`

**问题**: v0.12.0 建立 toMap/fromMap 模式（`PressureSnapshot.toMap/fromMap`、`NormalizedComparisonMetrics.toMap/fromMap`），v0.12.0 SR review F03 fix 将 ComparisonJsonWriter 对齐到此模式。v0.13.0 SR 的 `NormalizedPressureMetrics` 设计中没有 `toMap()`/`fromMap()` 方法。

当前 v0.13.0 SR 没有序列化需求（分类结果是 transient 诊断数据），但 `toMap()` 对于调试日志、测试断言（验证指标值）和未来可能的序列化需求是必要的。`NormalizedComparisonMetrics` 在 v0.12.0 中已经实现了 toMap/fromMap——`NormalizedPressureMetrics` 应遵循相同模式。

**影响**: 低——当前 v0.13.0 无需序列化分类结果。但调试和测试时无法方便地检查 11 个指标值。

**推荐处置**: FIX。添加 `toMap()` 方法（与 `NormalizedComparisonMetrics.toMap()` 模式一致）:
```java
public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("completedTaskCount", completedTaskCount);
    // ... 11 fields
    map.put("queueGrowthRate", queueGrowthRate);
    map.put("threadUtilizationRatio", threadUtilizationRatio);
    return map;
}
```
`fromMap()` 可留待实际序列化需求出现时再添加。

---

### F05 [P1] computeDurationMs 使用 snapshot timestamp 差值——当 snapshots 非均匀采样时 duration 可能不准确

**位置**: SR 4.6 `SnapshotPressureClassifier.computeDurationMs()`

**问题**: `computeDurationMs()` 使用 `first.timestamp()` 和 `last.timestamp()` 的差值作为 totalDurationMs:
```java
Instant first = snapshots.get(0).snapshot().timestamp();
Instant last = snapshots.get(snapshots.size() - 1).snapshot().timestamp();
return Duration.between(first, last).toMillis();
```

但 IR-v0.13-004 的 `totalDurationMs` 语义定义为 "wall-clock 计时"（与 v0.12.0 F06 fix 一致）。`fromSnapshots()` 的 `totalDurationMs` 参数应**由调用方外部提供**，而非从 snapshot timestamp 推断。

当 snapshot 采样间隔不均匀时（如 LivePressureSampler 可能在启动时有 warmup 延迟），snapshot timestamp 的 first→last 跨度可能不等于 executor 的实际运行时长。更关键的是：snapshot timestamp 是采样时刻，而 executor 可能在采样开始前就已启动、在采样结束后才停止。

**影响**: 中等。throughputPerSecond 和 queueGrowthRate（归一化）的计算会受 duration 不准确影响。但在大多数测试场景下，采样覆盖了 executor 的完整生命周期，first/last timestamp 近似等于运行时长。

**推荐处置**: FIX。将 `totalDurationMs` 添加为 `classify()` 方法的参数（或通过 `ClassifierConfig` 传递），由调用方 wall-clock 计时后传入，而非从 snapshot timestamp 推断。与 IR-v0.12-004 F06 fix 保持一致。

---

## P2 Findings (Minor — document and defer)

### F06 [P2] PressureClassification record 的 evidence 使用 List<String> — 结构化证据不可查询

**位置**: SR 4.4 `PressureClassification`

**问题**: `evidence` 字段为 `List<String>`——每个 evidence 是一个自由文本字符串（如 "threadUtilization=0.85, maxQueueDepth=15, queueCapacity=20"）。这在人类可读场景下工作良好，但无法进行程序化查询（如 "筛选所有 queueGrowthRate > 0.5 的分类"）。

如果需要构建分类历史或统计分析（v0.14.0 闭环调整可能需要"过去的分类序列"），结构化 evidence（如 `Map<String, Object>` 或 `List<EvidenceItem>` record）更合适。

**影响**: 低——v0.13.0 的分类结果是即时消费品（被 scorer 消费），不需要结构化查询。后续版本如需分类历史分析，可添加结构化 evidence。

**推荐处置**: DEFER。保留 `List<String>`，在 SR 中记录此设计选择及未来的升级路径。

---

### F07 [P2] SystemCpuProbe 构造器硬编码 ManagementFactory.getOperatingSystemMXBean() — 不可注入 mock

**位置**: SR 4.11 `SystemCpuProbe`

**问题**: `SystemCpuProbe` 的构造器硬编码：
```java
this.osBean = ManagementFactory.getOperatingSystemMXBean();
```

这导致在单元测试中无法注入 mock `OperatingSystemMXBean`。虽然 SR §4.12 的 `fromExecutor()` 重载允许注入 mock `SystemCpuProbe`（返回固定值），但 `SystemCpuProbe` 自身的单元测试只能依赖平台的实际 CPU 值。

**影响**: 低——`SystemCpuProbe` 的集成测试（AC-v0.13-023）验证平台真实值即可。单元测试可以通过重载方法注入 mock probe。

**推荐处置**: DEFER。当前设计满足测试需求。如需严格的 CPU probe 单元测试，可后续添加 package-private 构造器 `SystemCpuProbe(OperatingSystemMXBean)` 用于测试。

---

## 正向检查通过项

- [x] SR 不隐含实现授权 — §8 明确"不授权 Java 源码或测试实现"
- [x] 模块边界明确（§3）: 11 个新组件在 `experiment.classification`，1 个在 `experiment.probe`，1 个修改在 `experiment.metrics`
- [x] 依赖方向正确: classification → metrics (只读 snapshot)，probe → java.lang.management，无循环依赖
- [x] IR FIX 全部落地: F01 (withRejectedTaskCount)、F02 (RECOVERY 纯趋势)、F03 (ClassifierConfig.queueCapacity)、F04 (safety 规则)、F05 (术语统一)、F06 (fromExecutor 重载)
- [x] `PressureState` 枚举声明顺序正确（REJECTION_ACTIVE 最高优先级）
- [x] `ClassifierConfig` 构造验证完整（window>=2, threshold>0, capacity 边界）
- [x] `NormalizedPressureMetrics` 11 字段定义与 IR-v0.13-004 一致
- [x] `SnapshotPressureClassifier` 纯趋势 RECOVERY 检测（F02 fix），无状态要求满足
- [x] `ThresholdPolicyScorer` 权重可配置，默认值与 decision-log D4 一致
- [x] `PolicyRanker` 排序稳定（空列表/单元素/多元素处理完整）
- [x] `RuntimeObservation.fromExecutor()` 向后兼容（原 2-arg 签名保持不变）
- [x] Change 分解独立可验证性检查通过（§5）— change 1 不依赖 change 2
- [x] 非范围再次声明（§8）与 `decision-log.md` DFR 项一致
- [x] 测试策略覆盖 8 个 E2E 场景（§6.3），与 IR-v0.13-008 一致
- [x] 非回归约束明确（708 tests, §6.2）
- [x] 不涉及新外部依赖、新 JDK API 评估（CPU probe 使用已有 JDK API）
- [x] 序列化方向: SR 未定义 JSON 序列化（分类结果是 transient）——无序列化方向错误风险
- [x] Record 反序列化 null 兼容性: `PressureClassification` 所有字段 non-null ——无 null 风险
- [x] 跨类型转换边界值: `fromSnapshots()` 的 `fallbackPoolSize` 处理空列表 ——已正确处理

## Review Conclusion

SR functional design is structurally complete with 11 component designs covering the full pressure classification and policy scoring pipeline. Module boundaries are clear with new packages `experiment.classification` and `experiment.probe` properly isolated. All IR FIX items are incorporated.

However, **2 P0 blockers** must be resolved before SR closure:
- **F01**: `rejectedTaskCount` injection gap — `classify()` creates metrics internally but never calls `withRejectedTaskCount()`, making REJECTION_ACTIVE unreachable
- **F02**: `shortSequenceConfidenceFactor()` never called inside `classify()` — short-sequence confidence decay doesn't activate automatically

Plus **3 P1 findings** that should be addressed:
- **F03**: Scorer responsiveness uses absolute metrics (avgActiveThreads) while classifier uses relative (threadUtilizationRatio) — inconsistent
- **F04**: `NormalizedPressureMetrics` lacks `toMap()` — inconsistency with v0.12.0 pattern
- **F05**: `computeDurationMs()` infers duration from snapshot timestamps — should accept external wall-clock duration

Recommendation: proceed to SR disposition (`22-sr-review-disposition.md`). P0/P1 must be FIXED before SR closure verification.
