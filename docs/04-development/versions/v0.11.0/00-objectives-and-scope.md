# v0.11.0 目标与范围

## Header

- Version name: `v0.11.0`
- Status: `DRAFT`
- Current phase: `VERSION_DESIGN_DRAFT`
- Requirement theme: persistent evidence recording, snapshot serialization, recording session lifecycle, live pressure sampling

## 1. 背景

### 1.1 已完成的能力基线

| 版本 | 能力 | 状态 |
|---|---|---|
| v0.1.0 | 实验基础模型（ExperimentRun, LoadScenario, ControlPolicy） | IMPLEMENTED |
| v0.2.0 | 指标采集与记录（ObservedSnapshot, EvidenceRecorder, EvidenceSummary） | IMPLEMENTED |
| v0.3.0 | 场景运行器（ScenarioPlanner, BaselineWorkloadExecutor, ScenarioExperimentRunner） | IMPLEMENTED |
| v0.4.0 | 自适应策略与控制门（ControlGate, ThresholdPolicyEvaluator, PolicyDecision） | IMPLEMENTED |
| v0.5.0 | 离线回放与就绪判定（OfflinePolicyReplay, MutationReadinessGate, AdjustmentResult） | IMPLEMENTED |
| v0.6.0 | 压测数据获取基线（RunManifest, DataQualityValidator, ReadinessClassifier, ReportWriter） | IMPLEMENTED |
| v0.7.0 | ManagedExecutor 域与闭环实验（ManagedExecutor, ExecutorRegistry, AdjustmentAdapter, ScaleAdjustmentCommand） | IMPLEMENTED |
| v0.8.0 | 真实数据获取与 metrics 管道集成（ManagedExecutorScenarioRunner, G7-G9 门禁） | IMPLEMENTED |
| v0.9.0 | 队列容量动态调整（QueueResizeCommand, ExecutorRebuildStrategy, QueueResizeAdjustmentAdapter） | IMPLEMENTED |
| v0.10.0 | 拒绝策略动态替换（RejectionPolicyCommand, RejectionPolicyAdjustmentAdapter, PolicyReplacementEvidence） | IMPLEMENTED |

### 1.2 当前缺口

v0.1.0 交付了 metrics-snapshot-and-recording 的基础能力，但仅限于内存实现：

1. **无持久化存储** — `InMemoryEvidenceRecorder` 将 `ObservedSnapshot` 存储在 `ConcurrentHashMap` 中。JVM 重启后所有证据丢失。无法支持跨会话实验对比或长期数据积累。

2. **无序列化格式** — `PressureSnapshot`、`RuntimeObservation`、`ObservedSnapshot` 均为纯内存对象，无 JSON 或二进制序列化。`AcquisitionJsonWriter` 仅处理聚合摘要类型（`RunManifest`、`PressureSummary`、`ReplaySummary`），不处理原始快照。

3. **无录制会话生命周期** — `EvidenceRecorder` 接口只有 `record()`、`snapshots()`、`runIds()` 三个方法。无 start/stop 概念、无会话元数据、无会话边界。录制是"永远在线"的，无法知道何时开始、何时结束、配置快照是什么。

4. **无自主采样** — 只有 `ManualPressureSampler`，需要调用者提供 `RuntimeObservation`。`ManagedExecutorScenarioRunner` 在 step 执行循环中手动构建 observation 并调用 sampler。不存在按固定间隔自动从 live executor 读取的 `PressureSampler` 实现。

5. **CPU utilization 始终 absent** — `ManagedExecutorScenarioRunner.buildObservation()` 中 `cpuUtilization` 始终为 `MetricValue.absent()`。有字段定义但无数据来源。

这些缺口在三个动态配置维度完成后变得更加突出：系统现在可以动态调整线程数、队列容量和拒绝策略，但无法将调整前后的完整快照序列持久化以供离线分析。

### 1.3 JDK API 可行性评估

v0.11.0 不涉及新的 `ThreadPoolExecutor` 属性变更，无需 JDK API 评估。本版本聚焦于存储和采样基础设施，不引入新的 executor mutation。

### 1.4 与既有基础设施的关系

`AcquisitionReportWriter` 和 `AcquisitionJsonWriter` 已提供 JSON 序列化和版本化输出目录模式。`AcquisitionReportPaths` 已定义路径命名约定。v0.11.0 复用这些基础设施，不引入新的序列化框架或输出目录结构。

`ManagedExecutorScenarioRunner` 已实现从 live executor 读取 7 个指标并构建 `RuntimeObservation`。`LivePressureSampler` 将这一逻辑提取为独立的、可调度的 `PressureSampler` 实现。

### 1.5 为什么是现在

- 三个动态配置维度全部完成，系统可以动态改变 executor 的完整运行时状态
- 持久化存储是离线分析（回放、策略对比、趋势分析）的前提
- 自主采样是长时间运行实验（超过单次 scenario 生命周期）的前提
- `AcquisitionJsonWriter` 和 `AcquisitionReportPaths` 提供了可复用的序列化基础设施
- 在进入更复杂的分析或闭环控制之前，必须先解决"数据去哪了"的问题

## 2. 目标

`v0.11.0` 聚焦以下目标：

1. **FileBackedEvidenceRecorder**：实现 `EvidenceRecorder` 接口，将 `ObservedSnapshot` 序列化为 JSON 文件，写入版本化输出目录
2. **Snapshot JSON 序列化**：为 `PressureSnapshot`、`RuntimeObservation`、`ObservedSnapshot` 添加 JSON 序列化/反序列化支持
3. **RecordingSession**：引入录制会话生命周期 — 创建会话（含配置快照）→ 录制快照 → 关闭会话 → 产出 `RecordingSessionMetadata`
4. **LivePressureSampler**：实现 `PressureSampler` 接口，按可配置的固定间隔从 `ManagedExecutor` 自主读取并采样
5. **ManagedExecutorScenarioRunner 集成**：runner 可选使用 `LivePressureSampler` 替代手动 step 采样，验证自主采样路径
6. **端到端验证**：持久化录制 → 关闭会话 → 从磁盘读取 → 反序列化 → 验证数据完整性

## 3. 范围内

- `FileBackedEvidenceRecorder`（实现 `EvidenceRecorder`，JSON 文件存储）
- `PressureSnapshot` JSON 序列化/反序列化（通过 `AcquisitionJsonWriter` 扩展）
- `RuntimeObservation` JSON 序列化/反序列化（含 `MetricValue` 的 JSON 表示）
- `ObservedSnapshot` JSON 序列化/反序列化
- `RecordingSession`（sessionId, startTime, endTime, executorConfig snapshot, sampleCount, status）
- `RecordingSessionMetadata` record
- `LivePressureSampler`（实现 `PressureSampler`，`ScheduledExecutorService` 驱动，可配置间隔）
- `LivePressureSamplerConfig`（pollIntervalMs, autoStart, sessionId）
- `ManagedExecutorScenarioRunner` 扩展：支持注入 `LivePressureSampler` 作为替代采样路径
- `FileBackedEvidenceRecorder` 的并发安全（多线程写入同一 session）
- 现有 535 测试零回归

## 4. 范围外

- 跨 run 证据聚合
- 证据压缩/归档
- 保留策略强制执行（`RetentionRecord` 已有策略描述，执行延后）
- CPU utilization 真实数据源（`LivePressureSampler` 仍输出 `absent()`，但为后续扩展留下接口）
- 数据库存储（仅文件系统）
- 远程/网络存储
- 自定义序列化格式（仅 JSON）
- 新的 executor mutation 能力
- CLI entry

## 5. 架构对齐

| 架构文档 | 本版本对应内容 |
|---|---|
| `observability-and-experiment-strategy.md` | 满足 "observe behavior before optimizing it" — 持久化快照使跨会话观察成为可能 |
| `managed-executor-domain-model.md` | 扩展 RuntimeSetting 的可观测性维度 |
| `operational-and-evolution-boundaries.md` | 不引入新的 executor mutation，不扩展依赖边界 |

## 6. 模块边界

| 模块 | 变更类型 | 说明 |
|---|---|---|
| `experiment.metrics` | **新增** `FileBackedEvidenceRecorder` | 实现 EvidenceRecorder，JSON 文件持久化 |
| `experiment.metrics` | **新增** `RecordingSession` | 录制会话生命周期管理 |
| `experiment.metrics` | **新增** `RecordingSessionMetadata` | 会话元数据 record |
| `experiment.metrics` | **新增** `LivePressureSampler` | 定时自主采样实现 |
| `experiment.metrics` | **新增** `LivePressureSamplerConfig` | 采样器配置 record |
| `experiment.acquisition` | **扩展** `AcquisitionJsonWriter` | 增加 PressureSnapshot/RuntimeObservation/ObservedSnapshot 序列化方法 |
| `experiment.acquisition` | **扩展** `AcquisitionReportPaths` | 增加 evidence 文件路径命名 |
| `experiment.scenario` | **修改** `ManagedExecutorScenarioRunner` | 支持注入 LivePressureSampler 作为替代采样路径 |
| `experiment.model` | 不变 | PressureSnapshot 保持不变 |
| `experiment.executor` | 不变 | ManagedExecutor 保持不变 |

### 依赖方向

```text
experiment.metrics (FileBackedEvidenceRecorder, RecordingSession, LivePressureSampler)
    ├── experiment.acquisition (AcquisitionJsonWriter extension, AcquisitionReportPaths extension)
    └── experiment.model (PressureSnapshot — unchanged)

experiment.scenario (ManagedExecutorScenarioRunner modification)
    └── experiment.metrics (LivePressureSampler injection point)
```

## 7. 核心技术设计

### 7.1 JSON 序列化设计

复用 `AcquisitionJsonWriter` 的手写 JSON 模式（无外部依赖）：

```
PressureSnapshot JSON:
{
  "timestamp": "2026-06-13T10:30:00Z",
  "activeThreads": 4,
  "poolSize": 8,
  "queueSize": 12,
  "completedTaskCount": 150,
  "cpuUtilization": 0.35
}

RuntimeObservation JSON:
{
  "timestamp": "2026-06-13T10:30:00Z",
  "activeThreads": {"status": "PRESENT", "value": 4},
  "poolSize": {"status": "PRESENT", "value": 8},
  "queueSize": {"status": "PRESENT", "value": 12},
  "completedTaskCount": {"status": "PRESENT", "value": 150},
  "cpuUtilization": {"status": "ABSENT"},
  "keepAliveTimeSeconds": {"status": "ABSENT"},
  "largestPoolSize": {"status": "ABSENT"},
  "taskCount": {"status": "ABSENT"}
}

ObservedSnapshot JSON:
{
  "runId": "run-001",
  "snapshot": { <PressureSnapshot JSON> },
  "observation": { <RuntimeObservation JSON> }
}
```

`MetricValue.Present` 序列化为 `{"status": "PRESENT", "value": <value>}`，`MetricValue.Absent` 序列化为 `{"status": "ABSENT"}`。

### 7.2 FileBackedEvidenceRecorder 设计

```java
public final class FileBackedEvidenceRecorder implements EvidenceRecorder {
    private final Path outputDir;
    private final AcquisitionJsonWriter jsonWriter;
    private final ConcurrentHashMap<String, List<ObservedSnapshot>> buffer;  // 内存缓冲
    private final ConcurrentHashMap<String, RecordingSession> sessions;

    // 每个 runId 对应一个 JSON Lines 文件: outputs/reports/v0.11.0/evidence-<runId>.jsonl
    public void record(ObservedSnapshot snapshot);
    public List<ObservedSnapshot> snapshots(String runId);
    public Set<String> runIds();

    // 会话管理
    public RecordingSession startSession(String runId, ManagedExecutorConfig config);
    public RecordingSessionMetadata closeSession(String runId);
    public void flush(String runId);  // 强制刷盘
}
```

设计要点：
- 每条 snapshot 追加写入一个 JSON Lines 文件（每行一个 JSON 对象）
- 内存中保留缓冲以支持 `snapshots()` 查询
- `flush()` 将缓冲写入磁盘；`closeSession()` 自动调用 `flush()`
- JSON Lines 格式支持流式追加，无需在关闭时一次性序列化整个列表
- 线程安全：`ConcurrentHashMap` + `CopyOnWriteArrayList` 缓冲（与 `InMemoryEvidenceRecorder` 一致）

### 7.3 RecordingSession 设计

```java
public final class RecordingSession {
    private final String sessionId;
    private final String runId;
    private final ManagedExecutorConfig executorConfig;  // 会话开始时的配置快照
    private final Instant startedAt;
    private volatile Instant closedAt;
    private volatile int snapshotCount;
    private volatile SessionStatus status;  // ACTIVE, CLOSED

    public void incrementSnapshotCount();
    public RecordingSessionMetadata close();
}
```

```java
public record RecordingSessionMetadata(
    String sessionId,
    String runId,
    ManagedExecutorConfig executorConfig,
    Instant startedAt,
    Instant closedAt,
    int snapshotCount,
    SessionStatus status
) {}
```

### 7.4 LivePressureSampler 设计

```java
public final class LivePressureSampler implements PressureSampler {
    private final ManagedExecutor executor;
    private final EvidenceRecorder recorder;
    private final ScheduledExecutorService scheduler;
    private final LivePressureSamplerConfig config;
    private final SnapshotAssembler assembler;

    public void start(String runId);
    public void stop();
    public boolean isRunning();

    @Override
    public ObservedSnapshot sample(String runId, RuntimeObservation observation, Instant at);
}
```

设计要点：
- 内部使用 `ScheduledExecutorService` 按 `pollIntervalMs` 定时触发采样
- 每次触发：从 `ManagedExecutor` 读取当前状态 → 构建 `RuntimeObservation` → 调用 `assembler.assemble()` → 写入 `recorder`
- `start()` 启动调度，`stop()` 关闭调度并等待最后一次采样完成
- `sample()` 方法（来自 `PressureSampler` 接口）仍可用于手动采样，与自主调度共存
- `LivePressureSamplerConfig` 包含：`pollIntervalMs`（默认 1000ms）、`autoStart`、`sessionId`

### 7.5 ManagedExecutorScenarioRunner 集成

当前 `ManagedExecutorScenarioRunner` 在 step 执行循环中手动构建 observation 并调用 `sampler.sample()`：

```java
// 当前实现（Phase 3）
RuntimeObservation obs = buildObservation(executor, clock.get());
ObservedSnapshot snapshot = sampler.sample(runId, obs, clock.get());
recorder.record(snapshot);
```

扩展后支持注入 `LivePressureSampler`：
- 如果注入 `LivePressureSampler`，runner 在 Phase 2 之后调用 `liveSampler.start(runId)`，在 Phase 5 之前调用 `liveSampler.stop()`
- 自主采样路径与手动 step 采样路径互斥：要么使用 `LivePressureSampler` 的定时采样，要么使用现有的 step 内手动采样
- 通过构造器重载或 Builder 模式控制采样策略

### 7.6 文件路径约定

扩展 `AcquisitionReportPaths`：

```java
// 新增
public String evidenceFile(String runId);  // outputs/reports/v0.11.0/evidence-<runId>.jsonl
public String sessionMetadataFile(String runId);  // outputs/reports/v0.11.0/session-<runId>.json
```

版本化输出目录使用 `v0.11.0`（通过 `forVersion("v0.11.0")`）。

## 8. 成功标准草案

- `FileBackedEvidenceRecorder.record()` 将 ObservedSnapshot 持久化到 JSON Lines 文件
- `FileBackedEvidenceRecorder.snapshots()` 可从磁盘读取并反序列化完整的快照列表
- `PressureSnapshot` → JSON → `PressureSnapshot` 往返序列化无数据丢失
- `RuntimeObservation` → JSON → `RuntimeObservation` 往返序列化正确保留 Present/Absent 语义
- `RecordingSession` 生命周期正确：创建 → 录制 → 关闭 → 元数据完整
- `LivePressureSampler` 按配置间隔从 live executor 自主采样
- `LivePressureSampler.stop()` 正确停止调度并等待最后一次采样完成
- `ManagedExecutorScenarioRunner` 使用 `LivePressureSampler` 替代手动采样可正常运行完整 7-phase 流程
- 端到端：持久化录制 → 关闭 → 从磁盘读取 → 反序列化 → 快照数量和时间戳一致
- 现有 535 测试零回归

## 9. 候选 Change Decomposition

IR/SR 阶段确认后：

| # | Change name | 范围 | 依赖 |
|---|---|---|---|
| 1/? | `persistent-evidence-recorder` | FileBackedEvidenceRecorder, AcquisitionJsonWriter 扩展, AcquisitionReportPaths 扩展, ObservedSnapshot/RuntimeObservation/PressureSnapshot JSON 序列化, RecordingSession, RecordingSessionMetadata | v0.1.0 EvidenceRecorder, v0.6.0 AcquisitionJsonWriter/AcquisitionReportPaths |
| 2/? | `live-pressure-sampler-and-integration` | LivePressureSampler, LivePressureSamplerConfig, ManagedExecutorScenarioRunner 集成, 端到端持久化录制验证 | Change 1, v0.7.0 ManagedExecutor, v0.8.0 ManagedExecutorScenarioRunner |

## 10. 当前阶段出口

进入 IR 前必须完成：

1. `README.md` 版本索引
2. `00-objectives-and-scope.md`（本文档）
3. `decision-log.md` 记录关键设计判断
4. `docs/00-project/current-state.md` 反映 v0.11.0 版本设计草稿状态
