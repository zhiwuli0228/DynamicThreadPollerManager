# v0.11.0 SR Independent Review

## Header

- Document type: SR independent review
- Version name: `v0.11.0`
- Reviewed artifact: `docs/04-development/versions/v0.11.0/20-sr.md`
- Review date: `2026-06-13`
- Reviewer: independent design reviewer (separate from SR author)
- Review basis: SR functional design, IR baseline (10-ir.md), IR review disposition (12-ir-review-disposition.md), existing codebase verification

## Review Method

逐组件阅读 SR 设计，对照现有代码库验证每个 API 签名声明，检查内部一致性（组件间契约对齐）、架构约束遵守（依赖方向、模块边界）、IR FIX 项落地、以及测试策略覆盖度。对所有发现分配 P0/P1/P2 级别。

## Findings Summary

| Total Findings | P0 | P1 | P2 |
|---|---|---|---|
| 7 | 2 | 3 | 2 |

---

## P0 Findings (Blockers — must resolve before SR closure)

### F01 [P0] `record()` 在 session CLOSED 后抛异常——与 SR 自身设计意图矛盾

**位置**: SR 4.6 `FileBackedEvidenceRecorder.record()` (line 402-405) + 4.5 `RecordingSession.incrementSnapshotCount()` (line 316-321)

**问题**: `record()` 在 session 存在时无条件调用 `session.incrementSnapshotCount()`，而 `incrementSnapshotCount()` 在 `status != ACTIVE` 时抛出 `IllegalStateException`。`closeSession()` 调用 `session.close()` 将状态设为 `CLOSED`，但**未从 `sessions` map 中移除该 session**。因此 `closeSession()` 之后任何对同一 runId 的 `record()` 调用都会抛出 `IllegalStateException`。

这与 SR line 538 的声明直接矛盾：
> "session 不在 ACTIVE 状态时 record() 仍可工作（快照录制与会话独立——会话是可选的元数据边界）"

**影响**: 若 `LivePressureSampler` 在 session close 后仍在运行（stop 尚未完成）或有外部调用者手动 `record()`，都会触发未预期异常。破坏"录制与会话独立"的设计原则。

**推荐处置**: FIX。两个可选方案：
- **方案 A（推荐）**: `record()` 中检查 session 状态，仅在 `ACTIVE` 时调用 `incrementSnapshotCount()`——对齐 "录制与会话独立" 的设计意图。
- **方案 B**: `closeSession()` 后从 `sessions` map 移除 session——但丢失 session 引用可能影响后续查询。

```java
// record() 中修改 (方案 A):
RecordingSession session = sessions.get(runId);
if (session != null && session.status() == SessionStatus.ACTIVE) {
    session.incrementSnapshotCount();
}
```

同时建议 `closeSession()` 中将 session 从 map 移除（或保留但 record() 容错）。

---

### F02 [P0] 文件命名约定内部不一致——`FileBackedEvidenceRecorder` 与 `AcquisitionReportPaths` 产生不同路径

**位置**: 
- SR 4.6 `FileBackedEvidenceRecorder.evidenceFilePath()` (line 479): `"evidence-" + runId + ".jsonl"` → `evidence-{runId}.jsonl`
- SR 4.6 `FileBackedEvidenceRecorder.sessionMetadataPath()` (line 483): `"session-" + runId + ".json"` → `session-{runId}.json`
- SR 4.7 `AcquisitionReportPaths.evidenceFileName()` (line 548-551): `runId + ".jsonl".replace(".jsonl", "-evidence.jsonl")` → `{runId}-evidence.jsonl`
- SR 4.7 `AcquisitionReportPaths.sessionMetadataFileName()` (line 553-556): `runId + ".json".replace(".json", "-session.json")` → `{runId}-session.json`

**问题**: 对于同一个 runId（如 `"run-001"`）：
- `FileBackedEvidenceRecorder` 写入: `evidence-run-001.jsonl`
- `AcquisitionReportPaths` 对外暴露: `run-001-evidence.jsonl`

外部代码通过 `AcquisitionReportPaths.evidenceFile()` 查找文件时将**找不到** `FileBackedEvidenceRecorder` 实际写入的文件。这是设计级不一致——同一 SR 内两个组件对同一概念（evidence file path）产生不同结果。

**影响**: 端到端测试（E2E-1 "持久化→关闭→读取→反序列化"）如果通过 `AcquisitionReportPaths` 定位文件将失败。Change 2 的集成测试依赖 change 1 的文件，若使用不同路径约定则会读不到数据。

**推荐处置**: FIX。统一命名约定，且 `FileBackedEvidenceRecorder` 应委托给 `AcquisitionReportPaths` 而非自行构造路径：
1. 确定一个命名约定（推荐 `{runId}-evidence.jsonl` ——与现有 `AcquisitionReportPaths` 模式一致：`{runId}-run-manifest.json`、`{runId}-pressure-summary.json`）
2. `FileBackedEvidenceRecorder.evidenceFilePath()` 改为调用 `AcquisitionReportPaths.evidenceFile(outputDir, runId)`
3. `FileBackedEvidenceRecorder.sessionMetadataPath()` 改为调用 `AcquisitionReportPaths.sessionMetadataFile(outputDir, runId)`
4. SR line 570 的注释 "遵循现有模式：evidence-<runId>.jsonl" 修正为实际约定

---

## P1 Findings (Important — should resolve; acceptable with documented rationale)

### F03 [P1] `FileBackedEvidenceRecorder` 硬编码版本路径且重复路径逻辑

**位置**: SR 4.6 `FileBackedEvidenceRecorder` 构造器 (line 377-379):
```java
this.outputDir = outputRoot.resolve("outputs/reports/v0.11.0");
```

**问题**: 
1. 版本号 `"v0.11.0"` 硬编码在构造器中——未来版本变更需要修改源码
2. `evidenceFilePath()` 和 `sessionMetadataPath()` 自行构造路径，而 `AcquisitionReportPaths` 已提供相同功能（SR 4.7）
3. SR 4.7 新增的 `evidenceFile()` 和 `sessionMetadataFile()` 方法未被 `FileBackedEvidenceRecorder` 使用——形成死代码风险

**推荐处置**: FIX。`FileBackedEvidenceRecorder` 应接受 `AcquisitionReportPaths` 参数或直接使用其静态方法：
```java
public FileBackedEvidenceRecorder(Path outputRoot, String versionTag) {
    AcquisitionReportPaths paths = AcquisitionReportPaths.forVersion(versionTag);
    this.outputDir = outputRoot.resolve(paths.outputDirectory());
    // ...
}
```
此修改同时解决 F02（路径一致性）和 F03（版本管理）。

---

### F04 [P1] `LivePressureSampler.start()` 存在 TOCTOU 竞态条件

**位置**: SR 4.8 `LivePressureSampler.start()` (line 623-628):
```java
public void start(String runId) {
    if (running) {
        throw new IllegalStateException("sampler is already running");
    }
    this.running = true;
    // ...
}
```

**问题**: `running` 是 `volatile boolean`——保证可见性但不保证原子性。两个线程同时调用 `start()` 时，两者都可能通过 `if (running)` 检查（都读到 `false`），然后都设置 `running = true` 并各自提交任务到 scheduler。结果：采样任务被重复提交，同一 runId 的采样频率翻倍。

实际风险较低（`start()` 预期由 `ManagedExecutorScenarioRunner` 单线程调用），但作为公共 API 应防御。

**推荐处置**: FIX。使用 `AtomicBoolean.compareAndSet()`:
```java
private final AtomicBoolean running = new AtomicBoolean(false);

public void start(String runId) {
    if (!running.compareAndSet(false, true)) {
        throw new IllegalStateException("sampler is already running");
    }
    // ...
}

public void stop() {
    if (!running.compareAndSet(true, false)) {
        return; // already stopped
    }
    // ...
}

public boolean isRunning() {
    return running.get();
}
```

---

### F05 [P1] `LivePressureSampler` 静默吞没所有异常——采样失败无感知

**位置**: SR 4.8 `LivePressureSampler.start()` 内部 lambda (line 635-637):
```java
} catch (RuntimeException e) {
    // log and continue — 单次采样失败不影响后续
}
```

**问题**: 注释说 "log and continue"，但代码中没有任何 log 语句（空的 catch block）。如果采样持续失败（例如 `ManagedExecutor` 已被 shutdown、`EvidenceRecorder.record()` 抛异常），调度器会静默循环，调用方完全无感知。`sample()` 方法（手动采样路径）没有此保护——异常会传播给调用者——两条路径行为不一致。

**推荐处置**: FIX。最低限度添加 log 语句。进一步：考虑连续失败计数器，超过阈值时自动 stop（熔断保护）：
```java
} catch (RuntimeException e) {
    consecutiveFailures.incrementAndGet();
    if (consecutiveFailures.get() > MAX_CONSECUTIVE_FAILURES) {
        stop();
    }
}
```
`MAX_CONSECUTIVE_FAILURES` 可放在 `LivePressureSamplerConfig` 中（默认值如 10）。

---

## P2 Findings (Minor — deferrable to implementation)

### F06 [P2] `AcquisitionReportPaths` 的 `.replace()` 模式脆弱——runId 含 `.json`/`.jsonl` 时损坏

**位置**: SR 4.7 `AcquisitionReportPaths.evidenceFileName()` (line 548-551) ——延续现有模式（`AcquisitionReportPaths.java` line 62-87）

**问题**: 现有 `AcquisitionReportPaths` 使用 `runId + ".json".replace(".json", "-xxx.json")` 模式。如果 runId 包含 `.json` 子串（如 `"my.json.test"`），`.replace()` 会替换所有出现，产生损坏的文件名。`requireSafeRunId()` 只过滤 `/`、`\\`、`..`，不过滤 `.json`。

实际风险低——runId 由系统生成（UUID 或简单标识符），不会包含 `.json`——但与 SR line 570 "遵循现有模式" 相呼应时，应同时标记现有模式的已知局限。

**推荐处置**: DEFER_TO_IMPLEMENTATION。建议改为直接拼接：`runId + "-evidence.jsonl"`。此修改应同时应用到现有 `AcquisitionReportPaths` 所有方法和 SR 新增方法。列为 change 1 的 AC 或 code review checklist。

---

### F07 [P2] `RecordingSessionMetadata` JSON 序列化无独立往返测试

**位置**: SR 4.6 `writeSessionMetadata()` (line 486-495) + 4.10 (line 769-789)

**问题**: Session metadata 的 JSON 序列化在 `FileBackedEvidenceRecorder` 的私有方法中完成，格式已在 SR 4.10 定义。但测试矩阵（SR 6.1）中没有针对 `RecordingSessionMetadata` JSON 格式往返的独立单元测试——仅通过 `closeSession()` 端到端隐式覆盖。若 metadata JSON 格式有 bug（如 `executorConfig` 嵌套序列化错误），定位困难。

**推荐处置**: DEFER_TO_IMPLEMENTATION。在 change 1 的测试中增加 `RecordingSessionMetadata` JSON 往返测试：`assert metadata.equals(parseMetadata(renderMetadata(metadata)))`。或在 `FileBackedEvidenceRecorder` 单元测试中增加 `closeSession()` 后读取 metadata 文件并验证字段完整性。

---

## IR FIX 项落地验证

| IR Finding | SR 落地位置 | 状态 |
|---|---|---|
| F01 [P0] JSON 类型推断 | SR 4.1 (PressureSnapshot.fromMap Number casting), 4.2 (RuntimeObservation.fromMap targetType) | [x] 正确落地 |
| F02 [P0] AcquisitionJsonWriter parse() | SR 4.4 (AcquisitionJsonWriter.parse()) | [x] 正确落地 |
| F03 [P1] buildObservation() 复用 | SR 4.2 (RuntimeObservation.fromExecutor()), 4.9 (委托) | [x] 正确落地 |
| F04 [P1] I/O 异常类型 | SR 4.6 (write-before-file, UncheckedIOException) | [x] 正确落地 |
| F05 [P1] 线程安全 | SR 4.2 line 230 (逐方法验证结论) | [x] 正确落地 |

IR DEFER 项验证：

| IR Deferred | SR 落地位置 | 状态 |
|---|---|---|
| F06 [P2] 端到端断言策略 | SR 6.4 E2E-2: `snapshots >= 3`（宽松断言） | [x] 正确落地 |
| F07 [P2] AcquisitionReportPaths 命名 | SR 4.7 新增 4 个方法 | [!] 命名约定内部不一致（见 F02） |

---

## 架构约束检查

### 依赖方向

| 检查项 | 状态 |
|---|---|
| acquisition → metrics (已存在) | [x] FileBackedEvidenceRecorder 放入 acquisition，利用现有方向 |
| metrics → executor (已存在) | [x] LivePressureSampler 依赖 ManagedExecutor getter（只读） |
| metrics → acquisition (新增) | [x] **未引入**——FileBackedEvidenceRecorder 在 acquisition 而非 metrics |
| 无循环依赖 | [x] 依赖图为 DAG |

### 模块边界

| 检查项 | 状态 |
|---|---|
| EvidenceRecorder 接口不变 | [x] |
| PressureSampler 接口不变 | [x] |
| ManagedExecutor 不修改 | [x] |
| ManagedExecutorConfig 不修改 | [x] |
| 现有构造器向后兼容 | [x] ManagedExecutorScenarioRunner 现有 5-arg 构造器委托给 6-arg |

### 非回归约束

| 检查项 | 状态 |
|---|---|
| ManagedExecutorScenarioRunnerTest 不经修改通过 | [x] 旧构造器不变 |
| InMemoryEvidenceRecorder 测试不经修改通过 | [x] FileBackedEvidenceRecorder 是新实现，非替换 |
| ManualPressureSampler 测试不经修改通过 | [x] LivePressureSampler 是新实现，非替换 |
| AcquisitionReportWriterTest 不经修改通过 | [x] 不在 scope 内 |

---

## Change Decomposition 独立可验证性复核

| 检查项 | 状态 |
|---|---|
| Change 1 可独立编译 | [x] 仅依赖现有 metrics/acquisition/model 包 |
| Change 1 可独立运行 mvn test | [x] 所有新增测试在 change 1 scope 内 |
| Change 1 不依赖 change 2 源码 | [x] LivePressureSampler 在 change 2 |
| Change 2 依赖 change 1 | [x] 端到端测试需要 FileBackedEvidenceRecorder |
| Change 1+2 串行交付可行 | [x] |

---

## 现有代码验证结果摘要

对所有 SR 中 "已验证 API 签名" 声明的独立复核：

- `EvidenceRecorder` 接口 (3 methods): [x] 一致
- `PressureSampler` 接口 (1 method): [x] 一致
- `PressureSnapshot` 构造器 (4-field + 6-field): [x] 一致
- `RuntimeObservation` 构造器 (3 个重载) + `withTimestamp()`: [x] 一致
- `ObservedSnapshot` 构造器 + 3 个访问器: [x] 一致
- `ManagedExecutor` 7 个 getter: [x] 一致
- `ManagedExecutorConfig` record (5 字段): [x] 一致
- `MetricValue` Present/Absent sealed interface: [x] 一致
- `SnapshotAssembler` / `DefaultSnapshotAssembler`: [x] 一致
- `AcquisitionJsonWriter` package-private + `render()` + `map()`: [x] 一致
- `AcquisitionReportPaths` 现有方法 + `.replace()` 模式: [x] 一致
- `AcquisitionReportBridge` 接收 `List<ObservedSnapshot>`: [x] 一致
- `ManagedExecutorScenarioRunner` 5-arg 构造器 + `buildObservation()` private: [x] 一致
- `ManualPressureSampler.sample()` 调用 `observation.withTimestamp(at)`: [x] 与 SR `LivePressureSampler.sample()` 行为一致

---

## 测试策略评估

| 层 | 评价 |
|---|---|
| 单元测试覆盖 | 充分——每个新组件/方法有对应单元测试 |
| JSON 往返测试 | 充分——`fromMap(toMap(obj)).equals(obj)` 模式正确 |
| 并发写入测试 | 充分——4 线程 × 100 snapshots |
| 端到端场景 | 充分——5 个场景覆盖持久化、采样、集成、并发、停止 |
| 异常路径 | 部分——`parse()` 无效 JSON（AC-v0.11-007），但缺少 I/O 错误注入测试（磁盘满、权限拒绝） |
| Session metadata 往返 | 缺失——见 F07 |

建议在 SR 6.1 中增加一行：`FileBackedEvidenceRecorder` I/O 错误路径测试（如使用只读目录、无权限文件）。

---

## Review Conclusion

SR 功能设计整体质量良好：所有 IR FIX 项正确落地，架构约束（依赖方向、模块边界、向后兼容）均满足，API 签名声明与现有代码库一致。Change decomposition 独立可验证。

**Blockers (2 P0)**:
- **F01**: `record()` 在 session CLOSED 后抛异常——与"录制与会话独立"设计意图矛盾
- **F02**: `FileBackedEvidenceRecorder` 与 `AcquisitionReportPaths` 文件命名不一致——外部代码找不到 recorder 写入的文件

**Important (3 P1)**:
- **F03**: `FileBackedEvidenceRecorder` 硬编码版本路径并重复路径逻辑
- **F04**: `LivePressureSampler.start()` TOCTOU 竞态
- **F05**: 采样失败静默吞异常

**Minor (2 P2)**: 脆弱的 `.replace()` 模式、session metadata 往返测试缺失

建议：F01-F02 必须在 SR closure 前处置；F03-F05 推荐处置；F06-F07 可推迟到实现阶段。
