# v0.11.0 SR Review Disposition

## Header

- Document type: SR review disposition
- Version name: `v0.11.0`
- Reviewed artifact: `docs/04-development/versions/v0.11.0/21-sr-review.md`
- Disposition date: `2026-06-13`
- Disposition by: SR author, responding to independent review

## Disposition Summary

| Total Findings | FIX | DEFER_TO_IMPLEMENTATION | CLOSED |
|---|---|---|---|
| 7 | 5 | 2 | 0 |

## Per-Finding Disposition

### F01 [P0] `record()` 在 session CLOSED 后抛异常 → **FIX**

**处置**: 选择 review 推荐方案 A——`record()` 中仅在 session ACTIVE 时调用 `incrementSnapshotCount()`。

**具体内容**:

`FileBackedEvidenceRecorder.record()` 修改 (SR 4.6 line 402-405):

```java
// Before:
RecordingSession session = sessions.get(runId);
if (session != null) {
    session.incrementSnapshotCount();
}

// After:
RecordingSession session = sessions.get(runId);
if (session != null && session.status() == SessionStatus.ACTIVE) {
    session.incrementSnapshotCount();
}
```

此修改对齐 SR line 538 的设计意图："session 不在 ACTIVE 状态时 record() 仍可工作"。`closeSession()` 保持现有逻辑（session 保留在 map 中，状态变为 CLOSED）——不移除 session 以支持关闭后的元数据查询。

SR 更新：4.6 `record()` 方法体中增加状态检查；4.5 `RecordingSession` 增加 `status()` 访问器（已在类定义中列出）。

---

### F02 [P0] 文件命名约定内部不一致 → **FIX**

**处置**: 统一命名约定，且 `FileBackedEvidenceRecorder` 委托给 `AcquisitionReportPaths` 构造路径。

**具体内容**:

**命名约定统一为 `{runId}-{type}.{ext}` 模式**（与现有 `AcquisitionReportPaths` 一致: `{runId}-run-manifest.json`、`{runId}-pressure-summary.json` 等）:

| 文件 | 约定 |
|---|---|
| Evidence file | `{runId}-evidence.jsonl` |
| Session metadata file | `{runId}-session.json` |

**`AcquisitionReportPaths` 新增方法修正** (SR 4.7):

```java
public static String evidenceFileName(String runId) {
    return requireSafeRunId(runId, "runId") + "-evidence.jsonl";
}

public static String sessionMetadataFileName(String runId) {
    return requireSafeRunId(runId, "runId") + "-session.json";
}
```

放弃 `.replace()` 间接写法，改用直接拼接——同时处置 F06（脆弱 replace 模式）。

**`FileBackedEvidenceRecorder` 路径方法修正** (SR 4.6):

```java
private Path evidenceFilePath(String runId) {
    return outputDir.resolve(AcquisitionReportPaths.evidenceFileName(runId));
}

private Path sessionMetadataPath(String runId) {
    return outputDir.resolve(AcquisitionReportPaths.sessionMetadataFileName(runId));
}
```

不再自行构造路径，委托给 `AcquisitionReportPaths`——同时处置 F03（路径逻辑重复）。

SR 更新：4.6 `evidenceFilePath()` / `sessionMetadataPath()` 改为委托；4.7 四个新方法改用直接拼接；4.6 line 570 注释修正为 "遵循现有模式：{runId}-evidence.jsonl、{runId}-session.json"。

---

### F03 [P1] 硬编码版本路径 + 重复路径逻辑 → **FIX**

**处置**: `FileBackedEvidenceRecorder` 构造器接受 `AcquisitionReportPaths` 参数，使用其 `outputDirectory()` 而非硬编码。

**具体内容**:

`FileBackedEvidenceRecorder` 构造器修改 (SR 4.6):

```java
// Before:
public FileBackedEvidenceRecorder(Path outputRoot) {
    this.outputDir = outputRoot.resolve("outputs/reports/v0.11.0");
    // ...
}

// After:
public FileBackedEvidenceRecorder(Path outputRoot, String versionTag) {
    AcquisitionReportPaths paths = AcquisitionReportPaths.forVersion(versionTag);
    this.outputDir = outputRoot.resolve(paths.outputDirectory());
    // ...
}
```

`evidenceFilePath()` / `sessionMetadataPath()` 委托给 `AcquisitionReportPaths`（见 F02 处置）——消除路径逻辑重复。

SR 更新：4.6 构造器签名从 `FileBackedEvidenceRecorder(Path outputRoot)` 改为 `FileBackedEvidenceRecorder(Path outputRoot, String versionTag)`；`evidenceFilePath()` / `sessionMetadataPath()` 改为委托。

---

### F04 [P1] `LivePressureSampler.start()` TOCTOU 竞态 → **FIX**

**处置**: 使用 `AtomicBoolean.compareAndSet()` 替代 `volatile boolean`。

**具体内容**:

`LivePressureSampler` 修改 (SR 4.8):

```java
// Before:
private volatile boolean running;

public void start(String runId) {
    if (running) {
        throw new IllegalStateException("sampler is already running");
    }
    this.running = true;
    // ...
}

public void stop() {
    this.running = false;
    // ...
}

public boolean isRunning() {
    return running;
}

// After:
private final AtomicBoolean running = new AtomicBoolean(false);

public void start(String runId) {
    if (!running.compareAndSet(false, true)) {
        throw new IllegalStateException("sampler is already running");
    }
    // ...
}

public void stop() {
    if (!running.compareAndSet(true, false)) {
        return; // already stopped, idempotent
    }
    scheduler.shutdown();
    // ...
}

public boolean isRunning() {
    return running.get();
}
```

`stop()` 也获得原子性保证——重复调用 `stop()` 变为 idempotent（第二次调用直接 return），而非再次 shutdown 已终止的 scheduler。

SR 更新：4.8 `LivePressureSampler` 字段和 `start()`/`stop()`/`isRunning()` 方法签名。

---

### F05 [P1] 采样失败静默吞异常 → **FIX**

**处置**: 添加 log 语句；增加连续失败计数器，超过阈值自动 stop。

**具体内容**:

`LivePressureSampler` 修改 (SR 4.8):

```java
// 新增字段:
private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
private static final int MAX_CONSECUTIVE_FAILURES = 10;

// start() 内 lambda 修改:
} catch (RuntimeException e) {
    int failures = consecutiveFailures.incrementAndGet();
    // log at WARN level with failure count
    if (failures >= MAX_CONSECUTIVE_FAILURES) {
        // log at ERROR level: circuit breaker tripped
        stop();
    }
}

// 成功采样后重置计数器 (在 try block 末尾):
consecutiveFailures.set(0);
```

`MAX_CONSECUTIVE_FAILURES` 硬编码为 10（非 `LivePressureSamplerConfig` 字段——避免 config record 膨胀；若后续需要可提取）。

SR 更新：4.8 `LivePressureSampler` 增加 `consecutiveFailures` 字段和熔断逻辑；`start()` 内 lambda 异常处理补充完整代码。

---

### F06 [P2] `.replace()` 模式脆弱 → **DEFER_TO_IMPLEMENTATION**

**处置**: DEFER_TO_IMPLEMENTATION。已在 F02 处置中一并解决——`evidenceFileName()` 和 `sessionMetadataFileName()` 改用直接拼接而非 `.replace()`。现有 `AcquisitionReportPaths` 的 6 个旧方法（`runManifestFileName` 等）不在 v0.11.0 scope 内，不在此 SR 中修改。

SR 更新：无——F02 处置已覆盖新增方法。

---

### F07 [P2] `RecordingSessionMetadata` JSON 往返测试缺失 → **DEFER_TO_IMPLEMENTATION**

**处置**: DEFER_TO_IMPLEMENTATION。在 change 1 (`persistent-evidence-recorder`) 实现时增加 `FileBackedEvidenceRecorder` 单元测试中的 metadata JSON 往返验证：`closeSession()` → 读取 metadata 文件 → 验证字段完整性。

SR 更新：6.1 分层测试表中 `FileBackedEvidenceRecorder` 行补充 "含 session metadata JSON 往返验证"。

---

## 修改后的 SR 更新计划

| SR 组件 | 变更 |
|---|---|
| 4.5 RecordingSession | `status()` 访问器已在设计列表中——确认保留 |
| 4.6 FileBackedEvidenceRecorder | 构造器签名改为 `(Path, String)`；`record()` 增加 session 状态检查；`evidenceFilePath()` / `sessionMetadataPath()` 委托给 `AcquisitionReportPaths` |
| 4.7 AcquisitionReportPaths | 四个新方法改用直接拼接（放弃 `.replace()`） |
| 4.8 LivePressureSampler | `running` 改为 `AtomicBoolean`；`start()`/`stop()` 原子化；增加连续失败计数器和熔断；lambda 异常处理补充完整代码 |
| 6.1 分层测试 | `FileBackedEvidenceRecorder` 行补充 session metadata JSON 往返 |

## 处置后状态

| Finding | 原始级别 | 处置 | 状态 |
|---|---|---|---|
| F01 | P0 | FIX (record() 增加 session 状态检查) | CLOSED |
| F02 | P0 | FIX (统一命名 + 委托 AcquisitionReportPaths) | CLOSED |
| F03 | P1 | FIX (构造器接受 versionTag + 委托路径) | CLOSED |
| F04 | P1 | FIX (AtomicBoolean.compareAndSet) | CLOSED |
| F05 | P1 | FIX (log + 连续失败熔断) | CLOSED |
| F06 | P2 | DEFER_TO_IMPLEMENTATION (F02 已覆盖新增方法) | CLOSED |
| F07 | P2 | DEFER_TO_IMPLEMENTATION (change 1 实现时增加) | CLOSED |

## 出口条件

所有 P0/P1 findings 已处置。可进入 SR closure verification。
