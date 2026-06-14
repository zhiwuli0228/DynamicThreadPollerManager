# v0.13.0 IR Review Disposition

## Header

- Document type: IR review disposition
- Version name: `v0.13.0`
- Reviewed artifact: `docs/04-development/versions/v0.13.0/11-ir-review.md`
- Disposition date: `2026-06-14`
- Disposition basis: `docs/02-harness/managed-change-standard.md` §2（出口条件：P0/P1 findings 已处置并通过闭环验证）

## Disposition Summary

| ID | 级别 | 处置 | 理由 |
|---|---|---|---|
| F01 | P0 | **FIX** | IR-v0.13-004 添加 withRejectedTaskCount() |
| F02 | P0 | **FIX** | IR-v0.13-002 重新定义 RECOVERY 条件为纯趋势特征 |
| F03 | P0 | **FIX** | IR-v0.13-003 ClassifierConfig 添加 queueCapacity 字段 |
| F04 | P1 | **FIX** | IR-v0.13-005 重新定义 safety 评分规则 |
| F05 | P1 | **FIX** | IR-v0.13-005 统一术语为 metrics().maxPoolSize() |
| F06 | P1 | **FIX** | IR-v0.13-007 添加 fromExecutor() 重载方法 |
| F07 | P2 | **DEFER_TO_SR** | 移除 record 构造器校验，改为 scorer 接口文档声明 |
| F08 | P2 | **DEFER_TO_SR** | SR 确认分类器不依赖 executor 配置对象 |

## Detailed Disposition

### F01 [P0 → FIX] NormalizedPressureMetrics 添加 withRejectedTaskCount()

**处置**: FIX。更新 IR-v0.13-004，添加 `withRejectedTaskCount(long)` 方法并澄清 rejectedTaskCount 数据流。

修改 `10-ir.md` IR-v0.13-004：

在 `fromSnapshots()` 方法描述后添加：

`NormalizedPressureMetrics` 添加实例方法 `withRejectedTaskCount(long rejected)`：
- 返回新的 `NormalizedPressureMetrics` 实例，`rejectedTaskCount` 设置为传入值，其他 10 个字段保持不变
- 与 `NormalizedComparisonMetrics.withRejectedTaskCount()` 模式一致（v0.12.0）

`rejectedTaskCount` 数据流澄清：
- `NormalizedPressureMetrics.fromSnapshots()` 中 `rejectedTaskCount` 初始为 0（从 snapshots 无法计算）
- 分类器消费方（如测试或 runner）需要从 `ScenarioRunOutcome.rejectedTaskCount()` 获取真实值
- 通过 `metrics.withRejectedTaskCount(outcome.rejectedTaskCount())` 注入后再传给 `PressureClassifier.classify()`
- 对于 baseline executor（无拒绝机制），保持 0

---

### F02 [P0 → FIX] 重新定义 RECOVERY 条件为纯趋势特征

**处置**: FIX。更新 IR-v0.13-002，使用纯趋势特征定义 RECOVERY，移除对"前一个分类"的依赖。

修改 `10-ir.md` IR-v0.13-002 的触发条件规则：

将 RECOVERY 条件从：
> RECOVERY: 前一个分类为 OVERLOAD/QUEUE_BUILDUP/REJECTION_ACTIVE 且 queueGrowthRate < 0 且 activeThreads 趋势向下

改为：
> RECOVERY: queueGrowthRate < -queueGrowthThreshold（队列显著下降中）且 threadUtilizationRatio < 0.5（线程利用率回落）且 maxQueueDepth > 0（曾有队列压力——区分于始终空闲的 UNDER_UTILIZED）

理由：
- 使用纯趋势特征（queueGrowthRate 显著为负 + 利用率回落 + 曾有队列压力证据）完全可在无状态前提下从快照序列检测
- 与 QUEUE_BUILDUP（queueGrowthRate > +threshold）形成对称——BUILDUP 是"正增长但未饱和"，RECOVERY 是"负增长且已回落"
- 优先级置于 QUEUE_BUILDUP 之后、UNDER_UTILIZED 之前（修正优先级链：REJECTION_ACTIVE > OVERLOAD > QUEUE_BUILDUP > RECOVERY > UNDER_UTILIZED > NORMAL）

`SnapshotPressureClassifier` 保持无状态要求不变。

同步修正 IR-v0.13-008 测试 2.6：
> 构造 RECOVERY 快照序列（前 5 个 OVERLOAD 特征 + 后 5 个递减特征）→ 验证后 5 个快照的分类为 RECOVERY

改为：
> 构造 RECOVERY 快照序列：10 个快照，queueSize 持续递减（10→9→...→1），activeThreads 持续递减（5→4→...→1），maxPoolSize=8 → 验证分类结果为 RECOVERY

---

### F03 [P0 → FIX] ClassifierConfig 添加 queueCapacity 字段

**处置**: FIX。更新 IR-v0.13-003，`ClassifierConfig` 添加 `queueCapacity` 字段。

修改 `10-ir.md` IR-v0.13-003：

`ClassifierConfig` record 字段更新为：
- `trendWindowSize`: int — 趋势计算窗口（快照数），必须 >= 2，默认 5
- `queueGrowthThreshold`: double — 队列增长率阈值（/快照），默认 0.1
- `rejectionWindowSize`: int — 拒绝检测窗口（快照数），必须 >= 1，默认 10
- `queueCapacity`: int — **新增**。executor 的队列容量，用于 OVERLOAD 条件中的相对阈值计算。默认 `Integer.MAX_VALUE`（表示无界队列——此时相对阈值退化为绝对条件）

OVERLOAD 条件（IR-v0.13-002）修正为：
> OVERLOAD: threadUtilizationRatio >= 0.8 且 maxQueueDepth >= queueCapacity * 0.5（当 queueCapacity == Integer.MAX_VALUE 时，仅检查 threadUtilizationRatio >= 0.8 且 maxQueueDepth > 0）

当 `queueCapacity` 为 0（SynchronousQueue）时，任何 `threadUtilizationRatio >= 0.8` 应被分类为 OVERLOAD（SynchronousQueue 的"队列深度"语义上是提交等待数而非队列容量）。

---

### F04 [P1 → FIX] 重新定义 safety 评分规则

**处置**: FIX。更新 IR-v0.13-005 的 Safety 维度评分逻辑和 IR-v0.13-008 测试 5.

修改 `10-ir.md` IR-v0.13-005 Safety 维度评分逻辑：

将 Safety 维度从检查不可能的边界值改为检查策略参数相对于当前压力状态的合理性：

| 检查项 | 低分条件 | 说明 |
|---|---|---|
| 容量充足性 | maxPoolSize < PressureClassification.metrics().maxPoolSize() | 策略无法容纳当前观测到的最大线程数 → safety 降低 |
| 步长合理性 | scaleStep > maxPoolSize * 0.5 | 步长超过池大小的一半，可能导致剧烈波动 → safety 降低 |
| 边界合理性 | maxPoolSize > 128（硬上限）或 maxPoolSize < 1 | 超出系统安全边界 → safety 降低 |

修改 IR-v0.13-008 测试 5.5：
> safetyScore=0 对无效策略（minPoolSize=0）

改为：
> safetyScore 对容量不足策略（maxPoolSize < observedMaxPoolSize）显著低于容量充足策略

---

### F05 [P1 → FIX] 统一 Efficiency 术语

**处置**: FIX。更新 IR-v0.13-005 Efficiency 评分逻辑中的术语。

修改 `10-ir.md` IR-v0.13-005 Efficiency 维度：

将 "peakPoolSize" 替换为 `PressureClassification.metrics().maxPoolSize()`（观测到的最大池大小）：
> maxPoolSize 相对于 metrics().maxPoolSize() 的过度配置程度。maxPoolSize >> metrics().maxPoolSize() → 低分。maxPoolSize 接近 metrics().maxPoolSize() → 高分

---

### F06 [P1 → FIX] SystemCpuProbe 注入方式

**处置**: FIX。更新 IR-v0.13-007，添加 `fromExecutor()` 重载方法。

修改 `10-ir.md` IR-v0.13-007：

`RuntimeObservation` 修改策略：
1. **新增重载方法** `fromExecutor(ManagedExecutor executor, Instant timestamp, SystemCpuProbe cpuProbe)`：
   - 使用传入的 `cpuProbe.sampleProcessCpuLoad()` 填充 `cpuUtilization`
   - 如果 CPU 读取返回 < 0 或 probe 为 null，保持 `MetricValue.absent()`
2. **保持原方法签名** `fromExecutor(ManagedExecutor executor, Instant timestamp)` 不变：
   - 内部委托到重载方法：`return fromExecutor(executor, timestamp, new SystemCpuProbe())`
   - 行为变更：`cpuUtilization` 从始终 absent → 变为调用真实 CPU probe（graceful degradation）
3. **向后兼容性**：`DefaultSnapshotAssembler` 已正确处理 absent → 0.0 映射，无需修改
4. **测试可注入性**：测试可以通过重载方法注入 mock `SystemCpuProbe`（返回固定值），实现可重现测试

---

### F07 [P2 → DEFER_TO_SR]

**处置**: DEFER_TO_SR。移除 `PolicyScore` compact constructor 中的 `compositeScore ≈ weighted sum` 校验。改为在 `PolicyScorer` 接口文档中声明"实现必须保证 compositeScore 为加权和"。测试验证 scorer 输出而非 constructor 约束。

### F08 [P2 → DEFER_TO_SR]

**处置**: DEFER_TO_SR。F03 的 FIX 已将 `queueCapacity` 添加到 `ClassifierConfig`。SR 阶段确认分类器触发条件仅依赖 `NormalizedPressureMetrics` + `ClassifierConfig`（含 queueCapacity），不依赖 `ManagedExecutorConfig` 或 `BaselineExecutorPreset` 等 executor 配置对象。
