# v0.12.0 SR Review Disposition

## Header

- Document type: SR review disposition
- Version name: `v0.12.0`
- Reviewed artifact: `docs/04-development/versions/v0.12.0/21-sr-review.md`
- Disposition date: `2026-06-14`
- Disposition basis: `docs/02-harness/managed-change-standard.md` §3（SR 出口条件：P0/P1 findings 已处置）

## Disposition Summary

| ID | 级别 | 处置 | 理由 |
|---|---|---|---|
| F01 | P0 | **FIX** | `parseArtifact()` 调错方法 — 修正为 `parse(json)` |
| F02 | P0 | **FIX** | 文件名约定修正为 `{comparisonId}-comparison.json` |
| F03 | P1 | **FIX** | 改用 toMap/fromMap + render/parse 模式（与 v0.11.0 一致） |
| F04 | P1 | **FIX** | `compare()` 中从 `mOutcome.rejectedTaskCount()` 读取拒绝计数 |
| F05 | P1 | **FIX** | 明确 `fallbackPoolSize` 仅在空 snapshot 列表时使用 |
| F06 | P2 | **DEFER** | v0.12.0 不修改 v0.11.0 硬编码路径 |
| F07 | P2 | **DEFER_TO_IMPLEMENTATION** | conclusion null 处理在实现阶段完成 |

## Detailed Disposition

### F01 [P0 → FIX] parseArtifact() 调用修正

**处置**: FIX。修正 SR §4.8 `parseArtifact()`：

```java
// Before (WRONG):
Object parsed = AcquisitionJsonWriter.render(json);

// After (CORRECT):
Object parsed = AcquisitionJsonWriter.parse(json);
Map<String, Object> map = (Map<String, Object>) parsed;
return artifactFromMap(map);
```

同时补充 `artifactFromMap()` 方法的伪代码签名（从 Map 构造 `ComparisonReportArtifact` 及所有嵌套 record）。

---

### F02 [P0 → FIX] 文件命名约定修正

**处置**: FIX。修正 SR §4.9 `comparisonReportFileName()` 为 `{comparisonId}-comparison.json` 模式：

```java
public static String comparisonReportFileName(String comparisonId) {
    return requireSafeRunId(comparisonId, "comparisonId") + ".json"
            .replace(".json", "-comparison.json");
}
```

输出示例: `abc123-comparison.json`（与 `{runId}-evidence.jsonl`、`{runId}-session.json` 约定一致）。

---

### F03 [P1 → FIX] 序列化架构对齐

**处置**: FIX。SR §4.8 `ComparisonJsonWriter` 改用 toMap/fromMap + `AcquisitionJsonWriter.render(map)` / `AcquisitionJsonWriter.parse(json)` 模式。

变更内容：
- `ComparisonReportArtifact`、`CommonExecutorPreset`、`NormalizedComparisonMetrics`、`MetricDelta`、`ComparisonResult` 各新增 `toMap()` / `fromMap()` 方法
- `ComparisonJsonWriter` 的序列化逻辑缩减为：call `artifact.toMap()` → `AcquisitionJsonWriter.render(map)` → write to file
- `ComparisonJsonWriter` 的反序列化逻辑缩减为：read file → `AcquisitionJsonWriter.parse(json)` → `artifactFromMap(map)`
- 移除 `renderPreset()`、`renderManagedConfig()`、`renderMetrics()`、`renderDeltas()` 等直接 StringBuilder 方法
- `escape()` 方法保留（字符串字段的 Map value 不需要 escape — render 已处理）

`ComparisonJsonWriter` 缩减后（约 40 行）:
```java
public final class ComparisonJsonWriter {
    private final AcquisitionReportPaths paths;
    
    public ComparisonJsonWriter(AcquisitionReportPaths paths) {
        this.paths = Objects.requireNonNull(paths);
    }
    
    public String writeComparisonReport(ComparisonReportArtifact artifact) {
        String json = AcquisitionJsonWriter.render(artifact.toMap());
        Path outputPath = Path.of(paths.outputDirectory())
                .resolve(comparisonReportFileName(artifact.comparisonId()));
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, json);
        return outputPath.toString();
    }
    
    @SuppressWarnings("unchecked")
    public ComparisonReportArtifact readComparisonReport(Path filePath) {
        String json = Files.readString(filePath);
        Map<String, Object> map = (Map<String, Object>) AcquisitionJsonWriter.parse(json);
        return ComparisonReportArtifact.fromMap(map);
    }
}
```

---

### F04 [P1 → FIX] rejectedTaskCount 数据流修复

**处置**: FIX。修正 SR §4.6 `ComparableScenarioRunner.compare()` 中 managed run 部分：

```java
// Before:
long mRejected = 0L;

// After:
long mRejected = mOutcome.rejectedTaskCount();  // 从 outcome 读取 (F04 fix)

NormalizedComparisonMetrics mMetrics = NormalizedComparisonMetrics.fromSnapshots(
        mSnapshots, managedEndMs - managedStartMs, managedConfig.corePoolSize())
        .withRejectedTaskCount(mRejected);
```

前提：`ManagedExecutorScenarioRunner.run()` 已在 Phase 4 读取 `executor.getRejectedTaskCount()` 并传入 `ScenarioRunOutcome` 的 8-arg 构造器（SR §4.11 已描述此变更）。

---

### F05 [P1 → FIX] fallbackPoolSize 文档澄清

**处置**: FIX。修正 SR §4.3 `NormalizedComparisonMetrics.fromSnapshots()` 的文档注释：

```
@param fallbackPoolSize 仅在 snapshot 列表为空时使用作为 maxPoolSize 和 avgActiveThreads 的返回值。
                         当 snapshot 非空时，maxPoolSize 从 PressureSnapshot.poolSize() 计算。
```

移除"或所有 poolSize 为 0"的模糊措辞（因为 `BaselineWorkloadExecutor.poolSize()` 始终返回 `preset.corePoolSize()`，非 0）。

---

### F06 [P2 → DEFER]

v0.12.0 不修改 v0.11.0 的硬编码路径。`comparisonReportFile()` 使用正确的版本号 `v0.12.0`。

### F07 [P2 → DEFER_TO_IMPLEMENTATION]

`conclusion` 字段 null 处理由实现 agent 确保反序列化一致。
