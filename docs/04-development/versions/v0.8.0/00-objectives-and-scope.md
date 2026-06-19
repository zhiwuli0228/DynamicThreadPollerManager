# v0.8.0 目标与范围

## Header

- Version name: `v0.8.0`
- Status: `DRAFT`
- Current phase: `VERSION_DESIGN_DRAFT`
- Requirement theme: real ManagedExecutor data acquisition, metrics pipeline integration, evidence-based readiness

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
| v0.7.0 | ManagedExecutor 域与闭环实验（ManagedExecutor, ExecutorRegistry, AdjustmentAdapter, 首次闭环验证） | IMPLEMENTED |

### 1.2 当前缺口

v0.7.0 成功桥接了实验基础设施到真实 `ThreadPoolExecutor`，并完成了首次闭环实验验证。但存在以下结构性缺口：

1. **数据获取仍基于 BaselineWorkloadExecutor**：v0.6.0 的 `ScenarioExperimentRunner` 使用无真实线程的 `BaselineWorkloadExecutor`。v0.7.0 的数据获取方案（`15-experiment-data-acquisition-plan.md`）已设计完成，但尚未实现。在真实 ManagedExecutor 上运行 STEADY/RAMP/BURST 三种场景的数据采集能力缺失。

2. **Metrics 管道未集成 ManagedExecutor**：v0.7.0 IR deferred F06——`ExecutorStateSnapshot` 扩展字段与 `ObservedSnapshot` 有重叠（activeCount, queueSize 两方都有），但 `SnapshotAssembler` 没有从 `ExecutorStateSnapshot` 构建的工厂方法。闭环实验的 `PressureSnapshot` 是手动构造的，不经标准 metrics 管道。

3. **AcquisitionReportPaths 硬编码版本**：`AcquisitionReportPaths.OUTPUT_DIRECTORY` 硬编码为 `outputs/reports/v0.6.0`，不支持多版本数据并存。

4. **缺乏标准化测试数据获取能力**：v0.7.0 以 1 个闭环实验验证了功能正确性，但缺乏用真实线程池跑多场景（STEADY/RAMP/BURST）产出标准化压力证据的能力。

### 1.3 为什么是现在

- v0.7.0 已证明 `ManagedExecutor` 和 `ManagedExecutorAdjustmentAdapter` 在单个端到端实验中工作正常。
- 数据获取方案（`15-experiment-data-acquisition-plan.md`）已完成设计，有明确的架构和执行计划。
- v0.6.0 的数据质量门禁和就绪分类器可直接复用，只需新增 G7-G9 真实执行器门禁。
- v0.7.0 回溯提出的流程改进需要在下一版本中固化为代码实践。
- 此时引入真实数据获取的**风险可控**：新增 runner 不修改现有 pipeline，不删除任何代码。

## 2. 目标

`v0.8.0` 聚焦以下目标：

1. **ManagedExecutor 真实数据获取**：实现 `ManagedExecutorScenarioRunner`，在真实 `ThreadPoolExecutor` 上运行 STEADY/RAMP/BURST 三种场景，采集真实线程池压力数据，产出标准化报告。
2. **Metrics 管道集成**：在 `SnapshotAssembler` 新增 `fromExecutorState(ExecutorStateSnapshot)` 工厂方法，将 `ExecutorStateSnapshot` 统一转换为 `ObservedSnapshot`/`PressureSnapshot`，消除手动构造代码。
3. **AcquisitionReportPaths 版本化**：支持参数化版本标签，v0.7.0 和 v0.8.0 的数据分别输出到对应版本目录，不互相覆盖。
4. **数据质量门禁扩展**：实现 G7（TPE 扩展字段 non-null）、G8（至少 1 个 queueSize > 0）、G9（无线程泄漏）门禁。
5. **流程改进落地**：将 v0.7.0 回溯中的测试规范、枚举测试原则固化为测试代码实践。

## 3. 范围内

- `ManagedExecutorConfig` record（封装 ManagedExecutor 创建参数）
- `ManagedExecutorScenarioRunner`（核心 runner，按 data-acquisition-plan 2.2 节流程实现）
- `ManagedExecutorScenarioRunnerTest`（集成测试，3 profiles × 最小 1 seed 验证）
- `SnapshotAssembler.fromExecutorState(ExecutorStateSnapshot)` 工厂方法
- `AcquisitionReportPaths.forVersion(String)` 静态工厂
- `AcquisitionDataQualityValidator` 扩展（G7-G9 门禁）
- 执行 9 个真实数据获取 run（3 profiles × 3 seeds）并产出报告
- `AcquisitionReportWriter` 复用现有实现（不修改接口）
- 现有 412 测试无回归

## 4. 范围外

- Queue resizing（queue capacity 动态修改）
- 闭环调度器/控制器（自动连续运行）
- 持久化（数据库、文件存储）
- REST / API / UI 暴露
- 生产环境集成
- 外部依赖引入
- Rejection policy 运行时切换
- 多执行器协调或分布式场景
- 性能优化声明

## 5. 架构对齐

| 架构文档 | 本版本对应内容 |
|---|---|
| `observability-and-experiment-strategy.md` | 观察真实线程池行为，产出标准化压力证据 |
| `managed-executor-domain-model.md` | 使用 ManagedExecutor + ExecutorRegistry 作为数据源 |
| `scheduling-reconfiguration-and-recovery-model.md` | 有界、显式的实验运行，shutdown/cleanup 保障 |
| `15-experiment-data-acquisition-plan.md` (v0.7.0) | 直接实现该方案的 Phase 1 + Phase 2 |

## 6. 模块边界

| 模块 | 变更类型 | 说明 |
|---|---|---|
| `experiment.scenario` | **新增** `ManagedExecutorScenarioRunner` | 不修改 `ScenarioExperimentRunner` 或 `BaselineWorkloadExecutor` |
| `experiment.executor` | **新增** `ManagedExecutorConfig` | 轻量 record，不依赖其他模块 |
| `experiment.metrics` | **修改** `SnapshotAssembler` | 新增 `fromExecutorState()` 工厂方法；不修改现有方法签名 |
| `experiment.acquisition` | **修改** `AcquisitionReportPaths`、`AcquisitionDataQualityValidator` | 路径版本化 + G7-G9；不修改现有门禁逻辑 |

### 依赖方向

```text
experiment.scenario (ManagedExecutorScenarioRunner)
    ├── experiment.executor (ManagedExecutor, ExecutorRegistry, ManagedExecutorAdjustmentAdapter)
    ├── experiment.metrics (PressureSampler, EvidenceRecorder, SnapshotAssembler)
    └── experiment.coordinator (ExperimentCoordinator)

experiment.metrics (SnapshotAssembler.fromExecutorState)
    └── experiment.adjustment (ExecutorStateSnapshot)

experiment.acquisition (ReportPaths.forVersion)
    └── (纯路径逻辑，无新依赖)

experiment.policy    ⊥ experiment.executor  (不依赖)
experiment.analysis  ⊥ experiment.executor  (不依赖)
```

## 7. 成功标准草案

- `ManagedExecutorScenarioRunner` 能在真实 `ThreadPoolExecutor` 上运行 STEADY/RAMP/BURST 三种场景
- 每个 run 产出 5 个标准 JSON artifact + raw snapshot JSONL
- `SnapshotAssembler.fromExecutorState()` 正确转换所有字段（non-null 的 activeCount/poolSize/queueSize/completedTaskCount）
- `AcquisitionReportPaths.forVersion("v0.8.0")` 产出路径 `outputs/reports/v0.8.0/`
- 9 个 run 全部通过 G1-G9 数据质量门禁
- `AcquisitionReadinessClassifier` 对数据集给出 `READY` 或 `READY_WITH_RISK` 判定
- 现有 412 测试无回归
- 新增测试覆盖：runner 正确性、SnapShotAssembler 转换精度、路径版本化

## 8. 候选 Change Decomposition

SR 阶段确认后，建议的 OpenSpec change 候选：

| # | Change name | 范围 | 依赖 |
|---|---|---|---|
| 1/3 | `real-executor-data-acquisition` | ManagedExecutorConfig, ManagedExecutorScenarioRunner, ManagedExecutorScenarioRunnerTest | v0.7.0 的 ManagedExecutor + Adapter |
| 2/3 | `metrics-pipeline-executor-integration` | SnapshotAssembler.fromExecutorState(), 相关测试 | Change 1/3 (需要 runner 验证) |
| 3/3 | `acquisition-paths-versioning-and-gates` | AcquisitionReportPaths.forVersion(), AcquisitionDataQualityValidator G7-G9 | Change 1/3 (需要数据验证) |

### 依赖关系

```text
real-executor-data-acquisition (change 1/3)
    ├── metrics-pipeline-executor-integration (change 2/3)
    └── acquisition-paths-versioning-and-gates (change 3/3)
```

Change 2 和 3 可并行（互不依赖），但都依赖 change 1。

## 9. 当前阶段出口

进入 IR 前必须完成：

1. `README.md` 版本索引
2. `00-objectives-and-scope.md`（本文档）
3. `decision-log.md` 记录关键设计判断
4. `docs/00-project/current-state.md` 反映 v0.8.0 版本设计草稿状态
