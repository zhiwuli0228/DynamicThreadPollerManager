# v0.12.0 SR Independent Review

## Header

- Document type: SR independent review
- Version name: `v0.12.0`
- Reviewed artifact: `docs/04-development/versions/v0.12.0/20-sr.md`
- Review date: `2026-06-14`
- Reviewer: independent design reviewer (separate from SR author)
- Review basis: SR functional design, IR baseline (10-ir.md), IR review disposition (12-ir-review-disposition.md), existing codebase verification

## Review Method

逐组件阅读 SR 设计，对照现有代码库验证每个 API 签名声明，检查内部一致性（组件间契约对齐）、架构约束遵守（依赖方向、模块边界）、IR FIX 项落地、SR 伪代码强制验证规则（3 个随机 API 调用点签名验证）、以及测试策略覆盖度。对所有发现分配 P0/P1/P2 级别。

## API 签名强制验证（SR 伪代码规则）

随机抽取 3 个 API 调用点，读取实际源码验证签名匹配：

| # | SR 调用点 | SR 位置 | 实际源码 | 验证结果 |
|---|---|---|---|---|
| 1 | `new ScenarioExperimentRunner(ExperimentCoordinator, ScenarioPlanner, BaselineWorkloadExecutor, PressureSampler, EvidenceRecorder, Supplier<Instant>)` | §4.6 | `ScenarioExperimentRunner.java:35-46` — 构造器 6 参数签名完全匹配 | **PASS** |
| 2 | `ManagedExecutorScenarioRunner.run(ScenarioDefinition, ManagedExecutorConfig) → ScenarioRunOutcome` | §4.6 | `ManagedExecutorScenarioRunner.java:60` — 签名 `run(ScenarioDefinition, ManagedExecutorConfig)` 返回值 `ScenarioRunOutcome` 完全匹配 | **PASS** |
| 3 | `AcquisitionReportPaths.forVersion(String) → AcquisitionReportPaths` | §4.8 | `AcquisitionReportPaths.java:40` — 静态工厂方法签名完全匹配，返回 `new AcquisitionReportPaths(versionTag)` | **PASS** |

3/3 API 签名验证通过。补充验证：
- `AcquisitionReportPaths.outputDirectory()` → `String` — verified (line 52)
- `DefaultSnapshotAssembler()` — 无参构造器 — verified (implicit)
- `ManualPressureSampler(SnapshotAssembler)` — verified (pattern established in v0.1.0)
- `InMemoryEvidenceRecorder()` — 无参构造器 — verified (standard)

## Findings Summary

| Total Findings | P0 | P1 | P2 |
|---|---|---|---|
| 7 | 2 | 3 | 2 |

---

## P0 Findings (Blockers — must resolve before SR closure)

### F01 [P0] ComparisonJsonWriter.renderArtifact() 中的 parseArtifact() 伪代码错误

**位置**: SR 4.8 `ComparisonJsonWriter.parseArtifact()` (line ~"Object parsed = ... AcquisitionJsonWriter.render(json)")

**问题**: 伪代码中写的是：
```java
Object parsed = com.zhiwu.dynamicthreadpollermanager.experiment.acquisition.AcquisitionJsonWriter
        .render(json);  // 注: 实际使用 parse(String) — 此处示意
```

`AcquisitionJsonWriter.render(Object)` 的签名是 `static String render(Object value)` — **序列化**方向，接受 Object 返回 String。此处需要的是**反序列化**方向：`AcquisitionJsonWriter.parse(String json)` — `static Object parse(String json)`，接受 String 返回 Object（Map/List/primitive）。

已验证 `AcquisitionJsonWriter.parse()` 存在（`AcquisitionJsonWriter.java:183`: `public static Object parse(String json)`）。

**影响**: 实现 agent 若直接复制此伪代码调用 `render(json)` 会将 JSON 字符串当作普通 String 值序列化（加引号 escape），而非解析为 Map。`write → read` 往返测试（AC-v0.12-016）将失败。

**推荐处置**: FIX。将伪代码修正为：
```java
Object parsed = com.zhiwu.dynamicthreadpollermanager.experiment.acquisition.AcquisitionJsonWriter.parse(json);
Map<String, Object> map = (Map<String, Object>) parsed;
return artifactFromMap(map);
```

---

### F02 [P0] AcquisitionReportPaths 命名约定不一致 — comparison-{id}.json vs {id}-comparison.json

**位置**: SR 4.9 `AcquisitionReportPaths.comparisonReportFileName()`

**问题**: SR §4.9 定义:
```java
public static String comparisonReportFileName(String comparisonId) {
    return "comparison-" + requireSafeRunId(comparisonId, "comparisonId") + ".json";
}
```
产生文件名: `comparison-<comparisonId>.json`

已验证 `AcquisitionReportPaths` 现有命名约定（`AcquisitionReportPaths.java:114-119`）:
- `evidenceFileName(runId)` → `{runId}-evidence.jsonl`（`runId` 在前，描述在后）
- `sessionMetadataFileName(runId)` → `{runId}-session.json`（同上）
- `runManifestFileName(runId)` → `{runId}-run-manifest.json`（同上）
- `compositeReportFileName(runId)` → `{runId}-acquisition-report.md`（同上）

所有现有方法遵循 `{id}-{descriptor}.{ext}` 模式。SR 的 `comparison-{comparisonId}.json` 是 `{descriptor}-{id}.{ext}` 模式，**与现有约定相反**。

对于 `comparisonId = "abc123"`:
- SR 当前: `comparison-abc123.json` ❌
- 约定要求: `abc123-comparison.json` ✓

**影响**: 破坏 `AcquisitionReportPaths` 的命名一致性。在同一个类中同时存在 `{id}-{descriptor}` 和 `{descriptor}-{id}` 两种模式会混淆使用者（哪个 ID 在前？）。

**推荐处置**: FIX。修正为：
```java
public static String comparisonReportFileName(String comparisonId) {
    return requireSafeRunId(comparisonId, "comparisonId") + ".json"
            .replace(".json", "-comparison.json");
}
```
同时修正 `comparisonReportFile()` 中的输出路径版本标签为 `v0.12.0`（非硬编码 `v0.11.0`）。

---

## P1 Findings (Important — should resolve; acceptable with documented rationale)

### F03 [P1] ComparisonJsonWriter 不使用 toMap/fromMap 模式 — 与 v0.11.0 架构不一致

**位置**: SR 4.8 `ComparisonJsonWriter`（整体方法）

**问题**: v0.11.0 建立了 `toMap()`/`fromMap()` + `AcquisitionJsonWriter.render(map)`/`parse(json)` 的序列化架构。快照类型携带自己的 Map 转换逻辑，JSON writer 只做通用的 Map↔JSON 转换。

v0.12.0 SR 的 `ComparisonJsonWriter` **不使用此模式**。它用 `StringBuilder` + `String.format()` 直接为每种 record 类型编写专用的序列化方法（`renderPreset()`、`renderManagedConfig()`、`renderMetrics()`、`renderDeltas()`），共约 80 行手动 JSON 拼接代码。

这带来的问题：
1. 对于 `CommonExecutorPreset`、`NormalizedComparisonMetrics` 等 scenario 包的 record 类型，它们的 JSON 表示知识被下沉到了 acquisition 包的 `ComparisonJsonWriter` — 违反"数据知道自己的序列化格式"原则
2. 如果任何 record 添加新字段，必须同时修改 `ComparisonJsonWriter` 的序列化和反序列化方法 — 两个独立位置的同步风险
3. 手写 JSON 拼接容易出现引号 escape 遗漏（`renderPreset()` 中的 `String.format` 使用 `%s` 插入 description 等字段 — 若字段值包含双引号或换行符，产出非法 JSON）

**已确认**: `AcquisitionJsonWriter.render()` 是 package-private（`static String render(Object value)` 无 public 修饰符）。`ComparisonJsonWriter` 在同一包（`experiment.acquisition`）可访问。

**推荐处置**: FIX。让 scenario 包的 record 类型实现 `toMap()` / `fromMap()`，`ComparisonJsonWriter` 委托给 `AcquisitionJsonWriter.render(map)` / `parse(json)`。这最小化 `ComparisonJsonWriter` 的代码量（约 40 行 vs 当前约 120 行），且保持与 v0.11.0 架构一致。

若选择保留当前方案（直接 StringBuilder），则必须：
- 在 SR 中记录此设计选择与 v0.11.0 方案的差异及理由
- 在 `renderPreset()` 中对 `description` 等字符串字段使用 escape 函数（SR 已有 `escape()` 方法但 `renderPreset()` 中未调用）

---

### F04 [P1] NormalizedComparisonMetrics.fromSnapshots() 的 rejectedTaskCount 始终为 0 — 数据流中断

**位置**: SR 4.3 `NormalizedComparisonMetrics.fromSnapshots()` + SR 4.6 `ComparableScenarioRunner.compare()`

**问题**: `fromSnapshots()` 内部硬编码 `long rejected = 0L`。SR §4.6 的 `compare()` 在 managed run 后调用 `withRejectedTaskCount(mRejected)`，但 `mRejected` 同样硬编码为 `0L`：

```java
// SR §4.6:
long mRejected = 0L;   // ← 硬编码 0
...
mMetrics = NormalizedComparisonMetrics.fromSnapshots(...)
        .withRejectedTaskCount(mRejected);
```

SR §4.6 有一段注释说明问题：
> "managed runner 的拒绝计数通过 ManagedExecutorScenarioRunner 的额外方法获取，或在 SR review 中重新评估此方案。"

SR §4.11 提出了 `ScenarioRunOutcome` + `rejectedTaskCount` 的方案 A，但 `ComparableScenarioRunner.compare()` 伪代码**尚未使用此路径** — `mOutcome.rejectedTaskCount()` 未被调用。

**影响**: 即使 `ManagedExecutor.getRejectedTaskCount()` 和 `ScenarioRunOutcome.rejectedTaskCount()` 都已正确实现（IR-v0.12-009 + SR §4.11），`ComparableScenarioRunner.compare()` 仍会丢失此值。managed executor 的 `rejectedTaskCount` 在 comparison result 中始终为 0。

**推荐处置**: FIX。更新 `ComparableScenarioRunner.compare()` 伪代码中的 managed run 部分：

```java
ScenarioRunOutcome mOutcome = managedRunner.run(scenario, managedConfig);
// ...
long mRejected = mOutcome.rejectedTaskCount();  // 从 outcome 读取
NormalizedComparisonMetrics mMetrics = NormalizedComparisonMetrics.fromSnapshots(
        mSnapshots, managedEndMs - managedStartMs, managedConfig.corePoolSize())
        .withRejectedTaskCount(mRejected);
```

---

### F05 [P1] BaselineWorkloadExecutor 的 poolSize() 返回值 — fromSnapshots() 的 fallbackPoolSize 语义不清晰

**位置**: SR 4.3 `NormalizedComparisonMetrics.fromSnapshots()` + IR-v0.12-007 映射表

**问题**: SR §4.3 的 `fromSnapshots()` 方法使用 `fallbackPoolSize` 参数仅在 snapshot 为空或 poolSize 全部为 0 时使用。但验证源码：

- `BaselineWorkloadExecutor.poolSize()` 返回 `preset.corePoolSize()`（非 0 — e.g., `fixed-4` → 4）
- `ScenarioExperimentRunner.buildObservation()` 使用 `MetricValue.present(preset.corePoolSize())` 设置 poolSize
- `DefaultSnapshotAssembler.assemble()` 将 poolSize 解析为 `PressureSnapshot.poolSize()`

因此 baseline snapshots 中的 `poolSize` **不会是 0**（始终有 preset 值）。`fallbackPoolSize` 只会在 snapshot 列表为空时使用。

但 SR §4.6 的 `compare()` 调用 `fromSnapshots(bSnapshots, ..., bPreset.corePoolSize())` — `fallbackPoolSize` 使用 `bPreset.corePoolSize()` 是正确的。

IR-v0.12-007 映射表说 baseline 的 `maxPoolSize` 来自 `preset.corePoolSize()`（fallback 值），但实际数据优先从 snapshots 的 poolSize 计算（因为 poolSize 在 snapshot 中是 preset.corePoolSize()）。两者值相同，因此语义没有错误 — 但文档不精确。

**影响**: 低 — 功能正确但文档混淆。不影响实现正确性。

**推荐处置**: FIX。SR §4.3 `fromSnapshots()` 文档明确：`fallbackPoolSize` 仅在 snapshot 列表为空时使用（`if (snapshots.isEmpty())` return fallback），当 snapshot 存在时从 `PressureSnapshot.poolSize()` 计算 `maxPoolSize`。移除"或所有 poolSize 为 0"的模糊措辞。

---

## P2 Findings (Minor — document and defer)

### F06 [P2] AcquisitionReportPaths.evidenceFile/sessionMetadataFile 硬编码 v0.11.0 路径 — v0.12.0 的 comparisonReportFile 需要同步版本号

**位置**: SR 4.9 `comparisonReportFile()`

**问题**: 已验证 `AcquisitionReportPaths.java:123-129` — `evidenceFile()` 和 `sessionMetadataFile()` 硬编码 `"outputs/reports/v0.11.0"`。SR §4.9 的 `comparisonReportFile()` 伪代码写为：
```java
outputRoot.resolve("outputs/reports/v0.12.0")
```

版本号正确（v0.12.0）。但 `evidenceFile/sessionMetadataFile` 仍硬编码 v0.11.0 — 这是 v0.11.0 的技术债务，v0.12.0 不负责修复。只需确保 v0.12.0 的 comparison 文件使用正确的版本号。

**影响**: 低。v0.12.0 comparison 文件写入正确目录。

**推荐处置**: DEFER。v0.12.0 不修改 v0.11.0 的 evidence/session 路径。若未来版本需要统一版本化路径管理，另开 change。

---

### F07 [P2] ComparisonReportArtifact.conclusion 可为 null — JSON 序列化中 null 处理不一致

**位置**: SR 4.7 `ComparisonReportArtifact` + SR 4.8 `renderArtifact()`

**问题**: SR §4.7 允许 `conclusion` 为 null（`// conclusion 可为 null`）。SR §4.8 的 `renderArtifact()` 处理了 null：
```java
sb.append("  \"conclusion\": ").append(a.conclusion() != null
        ? "\"" + escape(a.conclusion()) + "\"" : "null").append("\n");
```

但 `parseArtifact()` 的 `artifactFromMap()` 未定义如何反序列化 null conclusion（从 Map 读取 null JSON value → 保持 null → record 允许 null ✓）。这不会导致 bug，但：
1. `MetricDelta.direction` 不允许 null（构造验证 `requireNonNull`）
2. 同一 report 中 mixed null/non-null 处理可能混淆反序列化逻辑

**影响**: 低。不影响功能正确性。

**推荐处置**: DEFER_TO_IMPLEMENTATION。实现 agent 确保反序列化时 `conclusion` 的 null 处理与序列化一致。

---

## 正向检查通过项

- [x] SR 不隐含实现授权 — 第 8 节明确"不授权 Java 源码或测试实现"
- [x] 模块边界明确（§3）: 7 个新组件在 scenario，1 个在 acquisition，1 个 executor 修改
- [x] 依赖方向正确: scenario → metrics/executor/coordinator（已存在），acquisition → scenario（新增 record import）
- [x] 无循环依赖: scenario → acquisition 通过 `ComparisonJsonWriter` 构造注入而非继承
- [x] IR FIX 全部落地: F01（精确数据流 §4.6）、F02（JSON 格式 §4.8）、F03（rejection counting §4.10）、F04（queueCapacity 转换 §4.1）、F05（runner 实例化 §4.6）、F06（wall-clock 计时 §4.6）
- [x] `CommonExecutorPreset.toBaselinePreset()` 转换规则与 IR F04 一致（-1→MAX_VALUE, 0→0, >0→direct）
- [x] `ComparableScenarioRunner.compare()` 顺序执行（IR F01 + decision-log D2）
- [x] `ComparableScenarioRunner` 每次 compare() 动态创建 runner 实例（IR F05 fix）
- [x] `MetricDelta.compute()` NEUTRAL 阈值 1%（IR-v0.12-003 要求）
- [x] `ManagedExecutor` rejection counting 使用 `AtomicLong` + handler 包装（IR F03 方案 A）
- [x] `ScenarioRunOutcome` 扩展向后兼容（旧 7-arg 构造器委托到新 8-arg，默认 rejectedTaskCount=0）
- [x] `ManagedExecutorConfig` JSON 格式 6 字段 + enum.name() 序列化（IR F02 fix）
- [x] 测试策略覆盖 6+ E2E 场景（§6.4），与 IR-v0.12-008 一致
- [x] 非回归约束明确（646 tests, §6.3）
- [x] Change 分解独立可验证性检查通过（§5）
- [x] 非范围再次声明（§8）与 `decision-log.md` DFR 项一致
- [x] 与 v0.12.0 decision-log 一致性: D1 (6 presets), D2 (sequential), D3 (9 metrics), D4 (single JSON), D5 (2 changes), D6 (reuse ScenarioDefinition)
- [x] 不涉及新外部依赖、新 JDK API 评估、跨模块循环依赖

## Review Conclusion

SR functional design is structurally complete with 11 component designs covering the full comparison framework. All IR FIX items are incorporated, module boundaries are clear, and the change decomposition is independently verifiable.

However, **2 P0 blockers** must be resolved before SR closure:
- **F01**: `parseArtifact()` calls `render()` instead of `parse()` — will break round-trip serialization
- **F02**: `comparisonReportFileName()` naming convention reversed vs existing pattern

Plus **3 P1 findings** that should be addressed:
- **F03**: Serialization architecture inconsistency (direct StringBuilder vs toMap/fromMap + render/parse)
- **F04**: `rejectedTaskCount` data flow broken in `compare()` — mRejected hardcoded to 0
- **F05**: `fallbackPoolSize` documentation ambiguity

Recommendation: proceed to SR disposition (`22-sr-review-disposition.md`). P0/P1 must be FIXED before SR closure verification.
