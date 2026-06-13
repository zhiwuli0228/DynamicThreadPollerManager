# v0.8.0 Decision Log

## Header

- Version: `v0.8.0`
- Status: `DRAFT`
- Authoring date: `2026-06-12`

## Active Decisions

### D1: 新增 runner vs 修改 ScenarioExperimentRunner

**状态**: ACCEPTED
**日期**: 2026-06-12

**决策**: 新建 `ManagedExecutorScenarioRunner`，不修改 `ScenarioExperimentRunner`。

**理由**:
- `ScenarioExperimentRunner` 与 `BaselineWorkloadExecutor` 紧耦合（`buildObservation()` 直接调用 `baselineExecutor.activeThreads()` 等方法）
- 两种 runner 服务于不同目的：`BaselineWorkloadExecutor` 用于快速基线验证（无真实线程），`ManagedExecutor` 用于真实压力数据采集
- 零回归保证优先——v0.3.0-v0.7.0 的所有测试不应因新增 runner 而修改
- 新增 runner 可直接复用 `DeterministicScenarioPlanner`、`ManualPressureSampler`、`EvidenceRecorder` 等现有组件

**备选方案**: 修改 `ScenarioExperimentRunner` 支持可替换的 executor 实现（通过接口抽象）。被拒绝原因：接口抽象会强制 `BaselineWorkloadExecutor` 实现不相关的方法（如 `activeThreads()` 恒为 0 的语义不同），且增加不必要的复杂度。

### D2: SnapshotAssembler 扩展方式

**状态**: ACCEPTED  
**日期**: 2026-06-12

**决策**: 在 `SnapshotAssembler` 接口新增 `fromExecutorState(ExecutorStateSnapshot)` 默认方法，在 `DefaultSnapshotAssembler` 中实现具体转换逻辑。

**理由**:
- v0.7.0 SR F06 deferred 明确要求此集成
- `ClosedLoopExperimentTest` 当前手动构造 `PressureSnapshot`——存在重复代码风险
- 默认方法避免破坏现有 `SnapshotAssembler` 实现者
- 转换逻辑：`ExecutorStateSnapshot.activeCount()` → `activeThreads`, `queueSize()` → `queueSize`, `poolSize()` → `poolSize`, `completedTaskCount()` → `completedTaskCount`

**备选方案**: 新建独立转换工具类 `ExecutorStateToObservationConverter`。被拒绝原因：增加一个新类而不提供额外抽象价值；`SnapshotAssembler` 已是"组装快照"的语义所有者。

### D3: AcquisitionReportPaths 版本化策略

**状态**: ACCEPTED
**日期**: 2026-06-12

**决策**: 新增 `AcquisitionReportPaths.forVersion(String versionTag)` 静态工厂方法；保留无参构造的 `OUTPUT_DIRECTORY = "outputs/reports/v0.6.0"` 作为向后兼容默认值。

**理由**:
- v0.6.0 数据（BaselineWorkloadExecutor）和 v0.8.0 数据（ManagedExecutor）必须分离
- 向后兼容：现有调用方（如测试）不传版本时仍得到 v0.6.0 路径
- 版本标签由调用方传入（如 `"v0.7.0"`, `"v0.8.0"`），不做自动推断

**备选方案**: 直接修改 `OUTPUT_DIRECTORY` 为最新版本。被拒绝原因：破坏现有测试期望（`AcquisitionContractsTest` 中验证路径以 `v0.6.0` 结尾）。

### D4: 数据质量门禁 G7-G9 的实现位置

**状态**: ACCEPTED
**日期**: 2026-06-12

**决策**: 扩展 `AcquisitionDataQualityValidator.validate(AcquisitionDataSet)` 方法，在现有 G1-G6 后追加 G7-G9 检查。不在 `AcquisitionDataSet.RunSnapshot` 中新增字段。

**理由**:
- G7（TPE 扩展字段 non-null）：需要在 validator 中访问 `ExecutorStateSnapshot` 的 nullable 字段。方案：在 `AcquisitionDataSet.RunSnapshot` 中新增可选的 `Map<String, Boolean> extendedFieldPresence` 字段，指示每个扩展字段的存在性。
- G8（queue pressure）：同样通过扩展字段 presence 或 snapshot metadata 传递。
- G9（线程泄漏）：在 runner 层保证（`@AfterEach` 验证 `isTerminated()`），不在 validator 中重复检查。

**待 SR 确认**: `RunSnapshot` 扩展字段的具体设计——新增 Map 还是新增显式字段。

### D5: Change 分解策略

**状态**: ACCEPTED
**日期**: 2026-06-12

**决策**: 3 个 change 按顺序交付：change 1（runner）→ change 2（metrics 集成）和 change 3（路径+门禁）可并行。

**理由**:
- Change 1 是数据获取的核心，产出可执行 runner
- Change 2 和 3 都在 change 1 基础上验证，但互不依赖
- 3 个 change 每个独立可验证、独立可归档
- 遵循 v0.7.0 的串行 + 并行混合交付模式

## Deferred

| ID | 项 | 原因 |
|---|---|---|
| DFR-01 | 自动化 CLI 入口 | v0.8.0 通过 JUnit 测试执行数据获取；CLI 入口留给后续版本 |
| DFR-02 | 数据清理自动化脚本 | raw evidence 通过 .gitignore 排除；自动化清理留给运维工具化阶段 |
| DFR-03 | 多执行器协调场景 | 架构范围外 |
