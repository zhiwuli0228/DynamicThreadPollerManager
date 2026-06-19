# v0.13.0 SR Review Disposition

## Header

- Document type: SR review disposition
- Version name: `v0.13.0`
- Reviewed artifact: `docs/04-development/versions/v0.13.0/21-sr-review.md`
- Disposition date: `2026-06-14`
- Disposition basis: `docs/02-harness/managed-change-standard.md` §3（SR 出口条件：P0/P1 findings 已处置）

## Disposition Summary

| ID | 级别 | 处置 | 理由 |
|---|---|---|---|
| F01 | P0 | **FIX** | classify() 添加 rejectedTaskCount 参数 |
| F02 | P0 | **FIX** | 将 shortSequenceConfidenceFactor() 移入 classify() 内部 |
| F03 | P1 | **FIX** | scorer responsiveness 对齐 classifier 指标（使用 utilizationRatio） |
| F04 | P1 | **FIX** | NormalizedPressureMetrics 添加 toMap() |
| F05 | P1 | **FIX** | classify() 添加 totalDurationMs 参数，移除 computeDurationMs() |
| F06 | P2 | **DEFER** | evidence 保留 List\<String\>，记录升级路径 |
| F07 | P2 | **DEFER** | CPU probe 测试通过重载方法注入，不需 mock MXBean |

## Detailed Disposition

### F01 [P0 → FIX] classify() 添加 rejectedTaskCount 参数

**处置**: FIX。修改 `PressureClassifier` 接口和 `SnapshotPressureClassifier` 实现。

**接口变更**:
```java
// Before:
PressureClassification classify(List<ObservedSnapshot> snapshots, ClassifierConfig config);

// After:
PressureClassification classify(
    List<ObservedSnapshot> snapshots,
    ClassifierConfig config,
    long rejectedTaskCount);
```

**实现变更** (§4.6):
```java
@Override
public PressureClassification classify(
        List<ObservedSnapshot> snapshots,
        ClassifierConfig config,
        long rejectedTaskCount) {

    // ... 计算 metrics ...
    
    // F01 fix: 注入 rejectedTaskCount 后再分类
    NormalizedPressureMetrics metrics = NormalizedPressureMetrics
            .fromSnapshots(snapshots, totalDurationMs, fallbackPoolSize,
                           config.trendWindowSize())
            .withRejectedTaskCount(rejectedTaskCount);
    
    // REJECTION_ACTIVE 现在可达
    if (metrics.rejectedTaskCount() > 0) { ... }
    // ...
}
```

**调用方责任**: 调用 classify() 前从 `ScenarioRunOutcome.rejectedTaskCount()` 获取值传入。baseline executor 传入 0。

**接口兼容性**: `PressureClassifier` 是新接口，无可兼容性问题。

---

### F02 [P0 → FIX] 将 shortSequenceConfidenceFactor() 移入 classify() 内部

**处置**: FIX。将置信度衰减内置到 classify() 中。

**变更**: SR §4.6 `SnapshotPressureClassifier`:
- `shortSequenceConfidenceFactor()` 从 `public` 改为 `private`
- `build()` helper 方法内部调用 `shortSequenceConfidenceFactor()`:
```java
private PressureClassification build(
        NormalizedPressureMetrics metrics, PressureState state,
        double confidence, String evidence, Instant now,
        int snapshotCount, int trendWindowSize) {
    double factor = shortSequenceConfidenceFactor(snapshotCount, trendWindowSize);
    double adjustedConfidence = clamp(confidence * factor);
    return new PressureClassification(
            state, adjustedConfidence,
            List.of(evidence), metrics, now);
}
```

**理由**: IR 明确要求短序列置信度降低是分类器行为的一部分，不应推给消费方。

---

### F03 [P1 → FIX] Scorer responsiveness 对齐 classifier 指标

**处置**: FIX。修改 SR §4.9 `ThresholdPolicyScorer.scoreResponsiveness()`。

**变更**: 将 OVERLOAD/QUEUE_BUILDUP 的 responsiveness 计算从使用绝对值改为使用比例值：

```java
private double scoreResponsiveness(PressureState state,
                                    ThresholdPolicyConfig config,
                                    NormalizedPressureMetrics metrics) {
    return switch (state) {
        case OVERLOAD, QUEUE_BUILDUP -> {
            // F03 fix: 使用 utilizationRatio 替代 avgActiveThreads
            // utilizationRatio 高 → 策略的 scale-up 阈值越相关 → 高分
            double uScore = utilizationProximity(
                    metrics.threadUtilizationRatio(),
                    config.scaleUpActiveThreadsThreshold(),
                    config.maxPoolSize());
            double qScore = thresholdProximity(
                    metrics.maxQueueDepth(),
                    config.scaleUpQueueSizeThreshold(), false);
            yield (uScore + qScore) / 2.0;
        }
        case UNDER_UTILIZED, RECOVERY -> {
            // F03 fix: 使用 utilizationRatio 替代 avgActiveThreads
            double score = utilizationProximity(
                    metrics.threadUtilizationRatio(),
                    config.scaleDownActiveThreadsThreshold(),
                    config.maxPoolSize());
            yield 1.0 - score;  // utilizationRatio 低 → scale-down 更相关 → 高分
        }
        case REJECTION_ACTIVE -> 1.0;
        case NORMAL -> 0.7;
    };
}

/**
 * 计算 threadUtilizationRatio 与 scale 阈值的接近程度。
 * utilizationRatio 接近 threshold/maxPoolSize → 策略阈值接近实际 → 高分
 */
private static double utilizationProximity(
        double utilizationRatio, int scaleThreshold, int maxPoolSize) {
    if (maxPoolSize == 0) return 0.5;
    double thresholdRatio = (double) scaleThreshold / maxPoolSize;
    double proximity = 1.0 - Math.abs(utilizationRatio - thresholdRatio);
    return clamp(proximity);
}
```

---

### F04 [P1 → FIX] NormalizedPressureMetrics 添加 toMap()

**处置**: FIX。SR §4.3 `NormalizedPressureMetrics` 添加 `toMap()` 方法。

```java
public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("completedTaskCount", completedTaskCount);
    map.put("rejectedTaskCount", rejectedTaskCount);
    map.put("avgQueueDepth", avgQueueDepth);
    map.put("maxQueueDepth", maxQueueDepth);
    map.put("totalDurationMs", totalDurationMs);
    map.put("throughputPerSecond", throughputPerSecond);
    map.put("avgActiveThreads", avgActiveThreads);
    map.put("maxPoolSize", maxPoolSize);
    map.put("snapshotCount", snapshotCount);
    map.put("queueGrowthRate", queueGrowthRate);
    map.put("threadUtilizationRatio", threadUtilizationRatio);
    return map;
}
```

`fromMap()` 不添加——当前无需反序列化（分类结果是 transient）。

---

### F05 [P1 → FIX] classify() 添加 totalDurationMs 参数

**处置**: FIX。将 totalDurationMs 添加为 classify() 参数，移除内部 `computeDurationMs()`。

**接口变更** (与 F01 合并):
```java
PressureClassification classify(
    List<ObservedSnapshot> snapshots,
    ClassifierConfig config,
    long rejectedTaskCount,
    long totalDurationMs);
```

**实现变更**: 移除 `computeDurationMs()` 私有方法。`totalDurationMs` 由调用方 wall-clock 计时后传入（与 v0.12.0 `ComparableScenarioRunner` 的 F06 fix 一致）。

**理由**: 分类器不应推断运行时长——这是其消费方的职责。与 IR-v0.12-004 F06 fix 的 wall-clock 计时策略保持一致。

---

### F06 [P2 → DEFER]

`PressureClassification.evidence` 保留 `List<String>`。在 SR 中记录：后续版本如需结构化 evidence（如 v0.14.0 闭环调整的分类历史分析），可升级为 `List<EvidenceItem>` record。

### F07 [P2 → DEFER]

`SystemCpuProbe` 构造器保留硬编码 `ManagementFactory.getOperatingSystemMXBean()`。`fromExecutor()` 重载已提供足够的测试注入路径。
