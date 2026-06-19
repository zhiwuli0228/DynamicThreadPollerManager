# v0.11.0 Decision Log

## D1: JSON 序列化方案

**背景**: `PressureSnapshot`、`RuntimeObservation`、`ObservedSnapshot` 需要序列化格式用于文件持久化。

**选项**:
- A: 复用 `AcquisitionJsonWriter`（手写 JSON，无外部依赖）
- B: 引入 Jackson/Gson 外部依赖

**决策**: 选 A — 复用 `AcquisitionJsonWriter`。

**理由**:
- 项目已建立"无外部序列化依赖"的基线（`AcquisitionJsonWriter` 已处理 Map/List/primitive/String/enum）
- `PressureSnapshot` 只有 6 个 primitive 字段，`MetricValue` 只有 2 个 case，手写序列化代码量小
- 引入 Jackson/Gson 会打破依赖边界，违反 `operational-and-evolution-boundaries.md` 的非目标约束
- `AcquisitionJsonWriter` 已在 v0.6.0 中验证可用

**影响**: 需要为 `AcquisitionJsonWriter` 添加 3 个类型的序列化/反序列化方法，约 100-150 行代码。

---

## D2: 文件布局和命名约定

**背景**: `FileBackedEvidenceRecorder` 需要一个文件布局来存储每个 run 的快照数据。

**选项**:
- A: 每个 runId 一个 JSON Lines 文件（`evidence-<runId>.jsonl`）
- B: 每个 runId 一个目录，每个 snapshot 一个文件
- C: 所有 run 写入同一个文件

**决策**: 选 A — 每个 runId 一个 JSON Lines 文件。

**理由**:
- JSON Lines 支持流式追加（每行一个完整 JSON 对象），无需在内存中保留所有快照后才写入
- 单文件便于复制、传输、按 run 清理
- 不需要为每个 snapshot 创建独立文件（B 会导致大量小文件）
- 不需要合并多个 run 的数据（C 会导致文件无限增长）

**影响**: 输出目录 `outputs/reports/v0.11.0/` 下每个 run 产生一个 `.jsonl` 文件和一个 `session-<runId>.json` 元数据文件。

---

## D3: LivePressureSampler 调度模型

**背景**: `LivePressureSampler` 需要按固定间隔从 `ManagedExecutor` 读取状态并采样。

**选项**:
- A: `ScheduledExecutorService.scheduleAtFixedRate()` — 固定速率
- B: `ScheduledExecutorService.scheduleWithFixedDelay()` — 固定延迟
- C: 手动 Thread + sleep 循环

**决策**: 选 B — `scheduleWithFixedDelay()`。

**理由**:
- 固定延迟确保前一次采样完成后才调度下一次，避免采样积压
- 如果单次采样因 executor 状态读取阻塞，`scheduleAtFixedRate` 会堆积任务
- 采样间隔不需要精确到固定速率级别（100ms 偏差可接受）
- `ScheduledExecutorService` 是 JDK 标准库，无需额外依赖

**配置**: `LivePressureSamplerConfig.pollIntervalMs` 默认 1000ms，最小 100ms。

---

## D4: RecordingSession 与 EvidenceRecorder 的关系

**背景**: `RecordingSession` 管理会话生命周期，`EvidenceRecorder` 管理快照存储。两者需要协同。

**选项**:
- A: `FileBackedEvidenceRecorder` 同时实现 `EvidenceRecorder` 和会话管理（一体化）
- B: `RecordingSession` 独立于 `FileBackedEvidenceRecorder`，通过组合关联
- C: `RecordingSession` 包装 `EvidenceRecorder`

**决策**: 选 A — `FileBackedEvidenceRecorder` 内部集成会话管理。

**理由**:
- `FileBackedEvidenceRecorder` 需要知道何时创建文件、何时关闭文件 — 会话生命周期直接映射到文件生命周期
- 一体化设计避免了"创建 recorder 但忘记创建 session"或"session 关闭后继续 record"的状态不一致
- `InMemoryEvidenceRecorder` 不需要会话管理（内存实现无文件概念），保持接口简单
- 会话元数据文件（`session-<runId>.json`）与 evidence 文件（`evidence-<runId>.jsonl`）属于同一存储层

**影响**: `FileBackedEvidenceRecorder` 新增 `startSession()`、`closeSession()`、`flush()` 方法。`EvidenceRecorder` 接口不变。

---

## D5: Change 分解策略

**背景**: v0.11.0 包含两个相对独立的子能力：持久化存储和自主采样。

**选项**:
- A: 单 change（persistent-recording-and-live-sampling）
- B: 双 change（persistent-evidence-recorder → live-pressure-sampler-and-integration）

**决策**: 选 B — 双 change。

**理由**:
- Change 1（`persistent-evidence-recorder`）可独立编译、独立测试：序列化 + 文件读写 + 会话生命周期
- Change 2（`live-pressure-sampler-and-integration`）依赖 Change 1 的 `FileBackedEvidenceRecorder` 和 `RecordingSession`
- 两个 change 的测试边界清晰：Change 1 测试"数据能否持久化"，Change 2 测试"数据能否自动采集并持久化"
- 符合 managed-change-standard 的独立可验证性规则

---

## D6: MetricValue JSON 表示

**背景**: `MetricValue<T>` 是 sealed interface（`Present<T>` | `Absent`），需要 JSON 表示。

**选项**:
- A: `{"status": "PRESENT", "value": <json-value>}` / `{"status": "ABSENT"}`
- B: 使用 JSON null 表示 Absent，直接值表示 Present

**决策**: 选 A — 显式 status 字段。

**理由**:
- JSON null 无法区分"值为 null"和"值不存在"（虽然当前所有 MetricValue 包装 non-null 值，但语义精确性重要）
- 显式 status 字段使 JSON 自描述，反序列化时无需类型推断
- 与 `MetricValue.isPresent()`/`isAbsent()` 语义直接对应

**影响**: 反序列化时需要读取 `status` 字段决定构造 `Present` 还是 `Absent`。

---

## DFR: Deferred 项

| ID | 描述 | 理由 | 后续版本 |
|---|---|---|---|
| DFR-01 | CPU utilization 真实数据源 | 需要跨平台（Windows/Linux）的 CPU 读取实现；LivePressureSampler 设计预留了接口，但当前 `cpuUtilization` 仍为 `absent()` | 候选 v0.12.0 |
| DFR-02 | 证据压缩与归档 | 文件级压缩和旧实验归档在实验数量少时价值有限 | 候选 v0.13.0 |
| DFR-03 | 跨 run 证据聚合 | 需要先有持久化存储和足够的实验数据积累 | 候选 v0.12.0 |
| DFR-04 | 保留策略强制执行 | `RetentionRecord` 已有策略描述，但自动清理需要更成熟的生命周期管理 | 候选 v0.13.0 |
| DFR-05 | 数据库/远程存储 | 文件系统优先；数据库存储引入新依赖，违反当前架构边界 | 未来架构修订后 |
