# v0.12.0 IR Review Disposition

## Header

- Document type: IR review disposition
- Version name: `v0.12.0`
- Reviewed artifact: `docs/04-development/versions/v0.12.0/11-ir-review.md`
- Disposition date: `2026-06-14`
- Disposition basis: `docs/02-harness/managed-change-standard.md` §2（出口条件：P0/P1 findings 已处置并通过闭环验证）

## Disposition Summary

| ID | 级别 | 处置 | 理由 |
|---|---|---|---|
| F01 | P0 | **FIX** | IR-v0.12-003 补充精确数据流 |
| F02 | P0 | **FIX** | IR-v0.12-006 补充 ManagedExecutorConfig JSON 格式 |
| F03 | P0 | **FIX** | IR 新增 IR-v0.12-009: ManagedExecutor rejection counting |
| F04 | P1 | **FIX** | IR-v0.12-002 补充 queueCapacity 转换规则 |
| F05 | P1 | **FIX** | IR-v0.12-003 明确 runner 实例化策略 |
| F06 | P1 | **FIX** | IR-v0.12-004 补充 totalDurationMs 计时策略 |
| F07 | P2 | **DEFER_TO_SR** | 当前无 workUnits > 1 的 scenario，SR 阶段记录到 report |
| F08 | P2 | **DEFER_TO_SR** | 手写 parser 有先例，SR 提供完整 JSON schema |

## Detailed Disposition

### F01 [P0 → FIX] ComparableScenarioRunner 数据流精确化

**处置**: FIX。更新 IR-v0.12-003，补充完整数据流。

修改 `10-ir.md` IR-v0.12-003 的候选验收语义，在 `compare()` 方法后添加精确的步骤描述：

```
compare(ScenarioDefinition scenario, String baselinePresetId, ManagedExecutorConfig managedConfig):
  1. CommonExecutorPreset preset = catalog.get(baselinePresetId)
  2. BaselineExecutorPreset bPreset = toBaselinePreset(preset)
  3. long baselineStart = clock.get().toEpochMilli()
  4. ScenarioRunOutcome bOutcome = baselineRunner.run(scenario, bPreset)
  5. long baselineEnd = clock.get().toEpochMilli()
  6. List<ObservedSnapshot> bSnapshots = baselineRecorder.snapshots(bOutcome.runId())
  7. long managedStart = clock.get().toEpochMilli()
  8. ScenarioRunOutcome mOutcome = managedRunner.run(scenario, managedConfig)
  9. long managedEnd = clock.get().toEpochMilli()
  10. NormalizedComparisonMetrics bMetrics = NormalizedComparisonMetrics.fromSnapshots(
        bSnapshots, baselineEnd - baselineStart, bPreset.corePoolSize())
  11. NormalizedComparisonMetrics mMetrics = NormalizedComparisonMetrics.fromSnapshots(
        mSnapshots, managedEnd - managedStart, managedConfig.corePoolSize())
  12. Map<String, MetricDelta> deltas = computeDeltas(bMetrics, mMetrics)
  13. return new ComparisonResult(...)
```

关键澄清：
- snapshots 通过 `recorder.snapshots(outcome.runId())` 获取，而非从 runner 直接返回
- `totalDurationMs` 从 wall-clock 计时（`clock.get()` before/after run）计算
- `fallbackPoolSize` 传入 `preset.corePoolSize()`（baseline）或 `managedConfig.corePoolSize()`（managed）

---

### F02 [P0 → FIX] ManagedExecutorConfig JSON 格式定义

**处置**: FIX。更新 IR-v0.12-006，补充 `ManagedExecutorConfig` 的 JSON 格式。

在 IR-v0.12-006 中添加完整的 `ManagedExecutorConfig` JSON 表示：

```json
"managedConfig": {
  "corePoolSize": 4,
  "maximumPoolSize": 8,
  "queueCapacity": 20,
  "keepAliveTime": 60,
  "keepAliveTimeUnit": "SECONDS",
  "threadMode": "PLATFORM"
}
```

序列化/反序列化规则：
- `keepAliveTimeUnit` 使用 `TimeUnit.name()` 序列化（返回 "SECONDS" / "MILLISECONDS" 等），`TimeUnit.valueOf(string)` 反序列化
- `threadMode` 使用 `ThreadMode.name()` 序列化，`ThreadMode.valueOf(string)` 反序列化
- 与 `AcquisitionJsonWriter` 处理枚举的模式一致

---

### F03 [P0 → FIX] ManagedExecutor 添加 rejection counting

**处置**: FIX。新增 IR-v0.12-009，要求 `ManagedExecutor` 添加 rejection counting wrapper。

**理由**: `rejectedTaskCount` 是 comparison 框架的关键可靠性指标。没有它，baseline（始终为 0）和 managed（实际可能发生拒绝）之间的比较在可靠性维度上不完整。代码变更量最小（~15 行），且不影响任何现有行为。

**新增 IR 条目**:

#### IR-v0.12-009 ManagedExecutor rejection counting

系统需要 `ManagedExecutor` 暴露拒绝任务计数。

候选验收语义：

- `ManagedExecutor` 新增方法 `getRejectedTaskCount()`：返回 `long`
- 构造时自动包装 `RejectedExecutionHandler`：当原始 handler 的 `rejectedExecution()` 被调用时，先递增内部 `AtomicLong` 计数器，再委托给原始 handler
- 对已有公开 API 零影响：构造器签名不变（内部包装对外部透明）
- PLATFORM 和 VIRTUAL 模式均支持
- 现有所有 ManagedExecutor 测试继续通过

优先级：P0（v0.12.0 的 prerequisite）。

---

### F04 [P1 → FIX] CommonExecutorPreset queueCapacity 转换规则

**处置**: FIX。更新 IR-v0.12-002，明确转换规则。

在 IR-v0.12-002 中添加：

`CommonExecutorPreset` 到 `BaselineExecutorPreset` 的转换规则：
- `queueCapacity == -1` → 转换为 `BaselineExecutorPreset` 的 `queueCapacity = Integer.MAX_VALUE`（语义：无界）
- `queueCapacity == 0` → 转换为 `BaselineExecutorPreset` 的 `queueCapacity = 0`（语义：SynchronousQueue — baseline 不使用真实队列，保留原始值作记录）
- `queueCapacity > 0` → 直接传递

不修改 `BaselineExecutorPreset` 的现有验证逻辑。`ComparableScenarioRunner` 内部执行转换。

---

### F05 [P1 → FIX] ScenarioExperimentRunner 实例化策略

**处置**: FIX。更新 IR-v0.12-003，明确 runner 实例化策略。

在 IR-v0.12-003 的构造参数部分添加：

`ComparableScenarioRunner` 每次 `compare()` 调用：
1. 根据 `baselinePresetId` 从 catalog 获取 `CommonExecutorPreset`
2. 转换为 `BaselineExecutorPreset`
3. 用 `BaselineExecutorPreset` 创建新的 `BaselineWorkloadExecutor`
4. 用此 executor 创建新的 `ScenarioExperimentRunner` 实例
5. 调用 `baselineRunner.run(scenario, baselinePreset)`

或等效地：
- `ComparableScenarioRunner` 持有 `ExperimentCoordinator` 工厂（而非 `ScenarioExperimentRunner` 实例）
- 在 `compare()` 内部动态创建 `ScenarioExperimentRunner`

不得持有单一 `ScenarioExperimentRunner` 实例并假设可重复用于不同 preset。

---

### F06 [P1 → FIX] totalDurationMs 计时策略

**处置**: FIX。更新 IR-v0.12-004，补充 wall-clock 计时策略。

在 IR-v0.12-004 的 `fromSnapshots()` 方法后添加：

`totalDurationMs` 参数来源：
- `ComparableScenarioRunner.compare()` 在每个 run 前后调用 `clock.get()` 记录 wall-clock 时间戳
- `totalDurationMs = Duration.between(startInstant, endInstant).toMillis()`
- 使用 wall-clock 计时（而非 snapshots 的 first/last timestamp 差值）因为它包含 setup/teardown 开销，更准确反映 executor 的端到端性能
- `fromSnapshots()` 接受外部传入的 `totalDurationMs`，不从 snapshots 内部推断

---

### F07 [P2 → DEFER_TO_SR]

**处置**: DEFER_TO_SR。SR 阶段在 `ComparisonReportArtifact` 中记录 `completedWorkUnits` vs `completedTaskCount` 的语义映射。当前所有 scenario workUnits=1，差异不可见。

### F08 [P2 → DEFER_TO_SR]

**处置**: DEFER_TO_SR。手写 JSON parser 模式已有先例（`PressureSnapshot.fromMap()` 等）。SR 阶段提供完整的 `ComparisonReportArtifact` JSON schema 定义。
