# v0.11.0 IR 独立评审报告

## Header

- Document type: IR independent review
- Version name: `v0.11.0`
- Reviewed artifact: `docs/04-development/versions/v0.11.0/10-ir.md`
- Review date: `2026-06-13`
- Reviewer role: 独立 IR review（非 IR 作者）
- Review basis: `docs/02-harness/managed-change-standard.md` 第 2 节（IR 需求分析）

## 1. 评审输入

已读取以下文件作为评审上下文：

- `docs/00-project/current-state.md`
- `docs/02-harness/managed-change-standard.md`
- `docs/01-architecture/operational-and-evolution-boundaries.md`
- `docs/05-domain/exploration-boundaries.md`
- `docs/04-development/versions/v0.11.0/README.md`
- `docs/04-development/versions/v0.11.0/00-objectives-and-scope.md`
- `docs/04-development/versions/v0.11.0/decision-log.md`
- `docs/04-development/versions/v0.11.0/10-ir.md`
- `src/main/java/.../metrics/EvidenceRecorder.java`
- `src/main/java/.../metrics/InMemoryEvidenceRecorder.java`
- `src/main/java/.../metrics/PressureSampler.java`
- `src/main/java/.../metrics/ManualPressureSampler.java`
- `src/main/java/.../metrics/SnapshotAssembler.java`
- `src/main/java/.../metrics/DefaultSnapshotAssembler.java`
- `src/main/java/.../metrics/RuntimeObservation.java`
- `src/main/java/.../metrics/ObservedSnapshot.java`
- `src/main/java/.../metrics/MetricValue.java`
- `src/main/java/.../model/PressureSnapshot.java`
- `src/main/java/.../acquisition/AcquisitionJsonWriter.java`
- `src/main/java/.../acquisition/AcquisitionReportPaths.java`
- `src/main/java/.../acquisition/AcquisitionReportWriter.java`
- `src/main/java/.../scenario/ManagedExecutorScenarioRunner.java`
- `src/main/java/.../executor/ManagedExecutor.java`
- `src/main/java/.../executor/ManagedExecutorConfig.java`
- `src/main/java/.../adjustment/ExecutorStateSnapshot.java`

## 2. 评审摘要

IR 草案结构完整：6 条需求覆盖了从 `FileBackedEvidenceRecorder` 到端到端验证的完整链路。复用 `AcquisitionJsonWriter` 和 `AcquisitionReportPaths` 基础设施的决策正确。但存在 2 个 P0 阻断项和 3 个 P1 关键项需要处置，主要集中在 JSON 反序列化类型推断不精确、`AcquisitionJsonWriter` 扩展方案缺失、以及 `buildObservation()` 逻辑提取三个问题上。

## 3. Findings

### F01 [P0] JSON 反序列化无法正确推断 Long 类型

**位置**: IR-v0.11-002

**问题**: IR-v0.11-002 要求 `MetricValue` 反序列化时"根据 value 的 JSON 类型推断泛型类型（整数 → Integer，浮点 → Double，字符串 → String）"。但 `PressureSnapshot` 的 `completedTaskCount` 是 `long`，`RuntimeObservation` 的 `completedTaskCount` 是 `MetricValue<Long>`。

JSON 的 number 类型不区分整数和长整数——`{"status": "PRESENT", "value": 150}` 中的 150 对 JSON parser 来说是同一个 number。如果 `MetricValue` 反序列化将所有整数推断为 `Integer`，则 `completedTaskCount` 反序列化后会丢失 `Long` 类型信息，往返序列化 `equals()` 验证将失败（`Integer(150) != Long(150)`）。

`PressureSnapshot` 的 4-arg 构造器和 6-arg 构造器也不同——如果 JSON 不包含 `poolSize` 和 `completedTaskCount`，需要调用 4-arg 构造器。

`AcquisitionJsonWriter` 当前是 package-private 类，只有 `render(Object)` 的静态方法——它不区分 Integer 和 Long。SR 需要解决两个问题：
1. 类型信息如何在序列化时保留（JSON 中添加类型标记 vs. 在反序列化时根据目标字段类型强制转换）
2. `PressureSnapshot` JSON 反序列化时选择哪个构造器

**影响**: IR 的往返序列化验收条件（AC-v0.11-004/005/006）不可验证——在未解决类型推断问题前，往返 equals 测试必然失败。

**建议**: 

方案 A：在 `MetricValue` JSON 中添加类型标记字段 `{"status": "PRESENT", "value": 150, "type": "Long"}`。反序列化时根据 `type` 字段构造正确的 `MetricValue`。

方案 B：反序列化时不推断类型，而是由调用方提供目标类型——`readRuntimeObservation(String json, Map<String, Class<?>> fieldTypes)`。对于 `PressureSnapshot`，直接提供 `PressureSnapshot` 专用的 `fromJson(String json)` 工厂方法，内部已知每个字段的类型。

推荐方案 B——`PressureSnapshot` 和 `RuntimeObservation` 各有专用的序列化/反序列化方法（而非泛型推断），避免类型标记污染 JSON 输出。

**IR 需要明确**: `MetricValue` 反序列化是泛型通用方法还是类型特定方法。推荐在 SR 阶段为每种使用 `MetricValue` 的快照类型定义专用反序列化逻辑。

---

### F02 [P0] AcquisitionJsonWriter 扩展方案缺失——与现有 Map 模式的冲突

**位置**: IR-v0.11-002

**问题**: `AcquisitionJsonWriter` 当前是 package-private 类，只有一个公开 API：`static String render(Object value)`。其内部逻辑是：`Map<String, Object>` → JSON object string，`Iterable` → JSON array，原始类型直接输出。这是一个**通用的** JSON writer。

IR-v0.11-002 要求添加 6 个类型特定的方法：`writePressureSnapshot()` / `readPressureSnapshot()` / `writeRuntimeObservation()` / `readRuntimeObservation()` / `writeObservedSnapshot()` / `readObservedSnapshot()`。

但 IR 没有回答一个关键设计问题：这些方法是添加到 `AcquisitionJsonWriter` 自身，还是创建一个新的类（如 `SnapshotJsonSerializer`）？

如果添加到 `AcquisitionJsonWriter`：
- `AcquisitionJsonWriter` 当前是 package-private——如果 `FileBackedEvidenceRecorder`（位于 `experiment.metrics` 包）需要调用这些方法，需要将 `AcquisitionJsonWriter` 提升为 public
- `AcquisitionJsonWriter` 只有 `render()` 方法，没有 `parse()` 方法——需要新增 JSON parser（当前完全不支持反序列化）
- 新增 JSON parser 的复杂度远高于当前 120 行的 render 实现

**影响**: IR-v0.11-002 的 6 个方法签名和 AC-v0.11-004/005/006 在当前 `AcquisitionJsonWriter` 代码中不可实现——没有 JSON parser。

**建议**: IR 明确推荐方案：

方案 A：在 `AcquisitionJsonWriter` 中新增 `parse(String json)` → `Object`（Map/List/String/Number/Boolean/null），然后各快照类型提供 `toMap()` / `fromMap(Map)` 方法。快照序列化通过 Map 中间层与 JSON writer 解耦。

方案 B：创建新的 package-private `SnapshotJsonSerializer` 类（位于 `experiment.metrics` 包），包含所有 6 个快照序列化方法，内部委托给 `AcquisitionJsonWriter.render()`（序列化方向）和新增的 JSON parser（反序列化方向）。

推荐方案 A——快照类型提供 `toMap()` / `fromMap()`，序列化细节留在 `AcquisitionJsonWriter` 中。这样可以：1) 不改变 `AcquisitionJsonWriter` 的职责（仍然是 Map→JSON），2) 快照类型自带 Map 转换逻辑（单一职责），3) `FileBackedEvidenceRecorder` 在 metrics 包直接调用快照的 `toMap()`，传 Map 给 `AcquisitionJsonWriter.render()`。

---

### F03 [P1] buildObservation() 逻辑无法被 LivePressureSampler 复用

**位置**: IR-v0.11-004

**问题**: IR-v0.11-004 要求 `LivePressureSampler` "复用 `ManagedExecutorScenarioRunner.buildObservation()` 的读取逻辑"。但该方法是 `private`——位于 `experiment.scenario` 包，`LivePressureSampler` 位于 `experiment.metrics` 包，无法访问。

IR 的风险表未提及此问题。

提取逻辑有三种方案：
1. 将 `buildObservation()` 逻辑提取为 `experiment.metrics` 包中的公共工具方法（如 `RuntimeObservation.fromExecutor(ManagedExecutor, Instant)`）
2. 在 `LivePressureSampler` 中复制该逻辑（重复代码）
3. 将 `buildObservation()` 改为 package-private，但 `LivePressureSampler` 不在同一包

**影响**: 如果不在 SR 阶段解决，`LivePressureSampler` 可能复制 `buildObservation()` 的逻辑，导致两份相同代码。

**建议**: IR 推荐方案 1——在 `RuntimeObservation` 上添加静态工厂方法 `fromExecutor(ManagedExecutor executor, Instant timestamp)`，同时让 `ManagedExecutorScenarioRunner.buildObservation()` 委托给此工厂方法。这样既不改变 runner 的行为，又让 `LivePressureSampler` 可以直接复用。

---

### F04 [P1] FileBackedEvidenceRecorder 的 I/O 错误处理未定义异常类型

**位置**: IR-v0.11-001

**问题**: IR 的风险表将"FileBackedEvidenceRecorder 文件 I/O 错误处理"列为 P1 风险，并要求"SR 需定义异常类型和恢复策略"。但 IR 的风险表不应只是"识别问题并推迟"——对于 P1 级别的风险，IR 应至少给出推荐的异常策略方向。

当前 IR-v0.11-001 只说了"record() 在 I/O 失败时抛出 unchecked exception"——没有指定异常类型（`UncheckedIOException`？自定义 `EvidencePersistenceException`？），也没有定义 `record()` 失败后 buffer 是否仍然包含该快照（buffer 是成功的但文件写入失败——数据在内存中有但磁盘上没有）。

**影响**: SR 需要从零设计错误处理策略，而这是 IR 阶段应给出的方向。

**建议**: IR 补充推荐异常策略：
- `record()` 在文件写入失败时抛出 `UncheckedIOException`（包装 `IOException`）
- 内存缓冲在文件写入**之前**更新（先写内存，再写文件）——这样 `snapshots()` 查询的可靠性不依赖文件 I/O
- `flush()` 方法将缓冲写入磁盘，失败时抛出 `UncheckedIOException`
- 调用方负责决定如何处理 I/O 失败（重试、降级到 InMemoryEvidenceRecorder 等）

---

### F05 [P1] LivePressureSampler 对 ManagedExecutor 的线程安全假设需要验证

**位置**: IR-v0.11-004

**问题**: IR-v0.11-004 声称"ManagedExecutor 的状态读取方法是线程安全的（委托给 TPE 的 getter，均为 volatile/同步方法）"。这个声明需要逐方法验证：

| ManagedExecutor 方法 | 底层 TPE getter | 线程安全机制 |
|---|---|---|
| `getActiveCount()` | `executor.getActiveCount()` | 无锁，近似值 |
| `getPoolSize()` | `executor.getPoolSize()` | 无锁，近似值 |
| `getQueueSize()` | `executor.getQueue().size()` | 取决于 Queue 实现 |
| `getCompletedTaskCount()` | `executor.getCompletedTaskCount()` | 无锁，近似值 |
| `getKeepAliveTime(TimeUnit)` | `executor.getKeepAliveTime(TimeUnit)` | volatile |
| `getLargestPoolSize()` | `executor.getLargestPoolSize()` | 无锁，近似值 |
| `getTaskCount()` | `executor.getTaskCount()` | 无锁，近似值 |

"无锁，近似值"并不意味着线程不安全——这些 getter 返回的是近似瞬时值，在多线程环境下是安全的（不会抛异常或返回损坏数据），但值可能不是"最新的"。对于 metrics 采样，近似瞬时值完全可接受——不需要精确的一致性快照。

但 `getQueueSize()` 委托给 `executor.getQueue().size()` ——如果 `workQueue` 是 `LinkedBlockingQueue`，`size()` 是线程安全的。`ManagedExecutor` 的构造器使用 `new LinkedBlockingQueue<>(queueCapacity)`，所以是安全的。

**影响**: 低。IR 的声明正确，但缺乏逐方法验证的严谨性。不影响需求正确性。

**建议**: 在 IR 中补充逐方法线程安全分析表（见上方），确认所有 7 个 getter 均为线程安全或近似安全的近似值读取。不需要改变需求。

---

### F06 [P2] 端到端测试 2 的采样数断言可能不稳定

**位置**: IR-v0.11-006

**问题**: IR-v0.11-006 测试 2 要求"运行 2 秒 → `stop()` → 验证录制的快照数 >= 5（200ms 间隔，2 秒至少 5 个）"。但：
- 如果 `stop()` 调用 `awaitTermination(10s)` 等待当前采样完成，且当前采样刚好在 stop 时开始并阻塞了 2 秒，实际采样数可能只有 3-4 个
- 在极慢的 CI 环境中，线程调度延迟可能导致采样间隔超过 200ms

**影响**: 低。测试在绝大多数环境下会通过，但存在极低概率的 flaky 风险。

**建议**: DEFER_TO_SR。SR 设计具体测试时：
- 使用宽松的断言（如 `>= 3` 而非 `>= 5`）
- 或使用 `CountDownLatch` 等待确定数量的采样完成而非 time-based
- 或使用 `ManualPressureSampler` 替代定时采样进行确定性端到端测试

---

### F07 [P2] AcquisitionReportPaths 新增方法的命名约定与现有模式不一致

**位置**: IR-v0.11-001（隐含）

**问题**: IR 提到 evidence 文件路径为 `outputs/reports/v0.11.0/evidence-<runId>.jsonl`，session 元数据文件为 `outputs/reports/v0.11.0/session-<runId>.json`。但 `AcquisitionReportPaths` 的现有方法是静态方法 + `reportDirectory()` + 文件名的模式——例如 `runManifest(Path, String)` 返回完整路径。

IR 没有要求遵循 `AcquisitionReportPaths` 的现有模式。如果 `FileBackedEvidenceRecorder` 直接拼接路径字符串而非使用 `AcquisitionReportPaths` 的方法，会破坏集中式路径管理的设计。

此外，`.jsonl` 后缀与现有的 `.json` 后缀不一致——`AcquisitionReportPaths` 目前所有文件都使用 `.json` 后缀。

**影响**: 低。SR 可以统一设计。

**建议**: SR 中为 `AcquisitionReportPaths` 新增：
- `evidenceFileName(String runId)` → `evidence-<runId>.jsonl`
- `sessionMetadataFileName(String runId)` → `session-<runId>.json`
- `evidenceFile(Path outputRoot, String runId)` → 完整路径
- `sessionMetadataFile(Path outputRoot, String runId)` → 完整路径

关于 `.jsonl` vs `.json`：JSON Lines 使用 `.jsonl` 是社区惯例（区分每行一个 JSON 对象的文件与单个 JSON 对象的文件）。保持 `.jsonl`。

---

## 4. Findings 汇总

| ID | 位置 | 描述 | 级别 | 建议动作 |
|---|---|---|---|---|
| F01 | IR-v0.11-002 | JSON 反序列化无法正确推断 Long 类型 + PressureSnapshot 双构造器 | P0 | IR 明确类型策略（推荐方案 B：专用工厂方法） |
| F02 | IR-v0.11-002 | AcquisitionJsonWriter 扩展方案缺失——无 JSON parser | P0 | IR 明确序列化架构（推荐方案 A：toMap/fromMap + render/parse） |
| F03 | IR-v0.11-004 | buildObservation() 是 private，LivePressureSampler 无法复用 | P1 | IR 推荐提取为 RuntimeObservation.fromExecutor() |
| F04 | IR-v0.11-001 | FileBackedEvidenceRecorder I/O 错误处理未定义异常类型 | P1 | IR 补充推荐异常策略 |
| F05 | IR-v0.11-004 | LivePressureSampler 对 ManagedExecutor 的线程安全假设需验证 | P1 | IR 补充逐方法分析表 |
| F06 | IR-v0.11-006 | 端到端测试采样数断言可能 flaky | P2 | DEFER_TO_SR |
| F07 | IR-v0.11-001 | AcquisitionReportPaths 命名约定一致性 | P2 | DEFER_TO_SR |

## 5. 正向检查通过项

- [x] IR 不隐含实现授权——各条目使用"候选验收语义"措辞，未声称已实现
- [x] Scope 边界明确排除跨 run 聚合、压缩、CPU 数据源、数据库存储
- [x] 非范围列表与 `00-objectives-and-scope.md` 和 `decision-log.md` 一致
- [x] 正确识别不涉及新的 executor mutation（无需 JDK API 评估）
- [x] 复用 `AcquisitionJsonWriter`、`AcquisitionReportPaths`、`EvidenceRecorder`、`PressureSampler` 等现有基础设施
- [x] `FileBackedEvidenceRecorder` 实现 `EvidenceRecorder` 接口，保持接口不变
- [x] `LivePressureSampler` 实现 `PressureSampler` 接口，保持接口不变
- [x] `ManagedExecutorScenarioRunner` 集成保持向后兼容（现有构造器不变）
- [x] 端到端测试覆盖持久化 + 自主采样 + 并发 + 异常路径
- [x] 并发安全需求已在 IR-v0.11-001（FileBackedEvidenceRecorder）和 IR-v0.11-004（LivePressureSampler）中定义
- [x] 风险和延期项表覆盖了类型推断、时序、I/O 错误、API 膨胀等关键风险
- [x] 出口条件清单与 `managed-change-standard.md` 第 2 节一致
- [x] 不涉及 production 环境、外部依赖、REST/API/UI

## 6. 评审结论

IR 草案在复用现有基础设施（`AcquisitionJsonWriter`、`AcquisitionReportPaths`、`EvidenceRecorder`、`PressureSampler`）和需求边界定义方面**合格**。6 条需求覆盖了从 `FileBackedEvidenceRecorder` 到端到端验证的完整链路。与 v0.10.0 的关键差异（不涉及 executor mutation）被正确识别。但**不能直接进入 SR**：存在 2 个 P0 阻断项（F01 JSON Long 类型推断、F02 AcquisitionJsonWriter 扩展方案缺失——无 JSON parser）和 3 个 P1 关键项（F03 buildObservation 复用、F04 I/O 异常类型、F05 线程安全验证）。P0/P1 必须通过 disposition 关闭。

评审建议：**进入 IR disposition（`12-ir-review-disposition.md`）**，逐项处置 F01-F07。
