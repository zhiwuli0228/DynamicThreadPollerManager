# v0.8.0 SR 独立评审报告

## Header

- Document type: SR independent review
- Version name: `v0.8.0`
- Reviewed artifact: `docs/04-development/versions/v0.8.0/20-sr.md`
- Review date: `2026-06-12`
- Reviewer role: 独立 SR review（非 SR 作者）
- Review basis: `docs/02-harness/managed-change-standard.md` 第 3 节（SR 功能设计）

## 1. 评审输入

已读取以下文件作为评审上下文：

- `docs/00-project/current-state.md`
- `docs/02-harness/managed-change-standard.md`
- `docs/01-architecture/observability-and-experiment-strategy.md`
- `docs/01-architecture/managed-executor-domain-model.md`
- `docs/04-development/versions/v0.8.0/10-ir.md`（修正后）
- `docs/04-development/versions/v0.8.0/11-ir-review.md`
- `docs/04-development/versions/v0.8.0/12-ir-review-disposition.md`
- `docs/04-development/versions/v0.8.0/13-ir-closure-verification.md`
- `docs/04-development/versions/v0.8.0/20-sr.md`
- `src/main/java/.../acquisition/AcquisitionReportWriter.java`
- `src/main/java/.../acquisition/AcquisitionReportPaths.java`
- `src/main/java/.../metrics/SnapshotAssembler.java`

## 2. 评审摘要

SR 整体质量高：6 个核心组件设计完整，含 Java 契约伪代码，7-phase runner 流程清晰，依赖方向明确，IR 阶段 4 个 deferred 项（F04/F06/F07/F10）均已给出具体设计决策。2 个候选 change 分解合理。但存在 4 个 P1 问题，集中在 AcquisitionReportWriter API 不匹配、ReadinessSummary 语义偏移、DeletionSafety 过度创建、以及 Bridge 归属歧义。另有 2 个 P2 边界情况。

## 3. Findings

### F01 [P1] AcquisitionReportWriter 构造函数与版本化路径不匹配

**位置**: 20-sr.md §4.4, §4.6

**问题**: 现有 `AcquisitionReportWriter` 构造函数签名为：

```java
public AcquisitionReportWriter(Path outputRoot) {
    this.outputDirectory = outputRoot.resolve(
            AcquisitionReportPaths.OUTPUT_DIRECTORY);  // 硬编码
}
```

SR §4.4 设计了 `AcquisitionReportPaths.forVersion(versionTag)` 返回实例，且 §4.6 的 `AcquisitionReportBridge` 伪代码中构造为 `new AcquisitionReportWriter(outputRoot, paths)` ——即传入一个 `AcquisitionReportPaths` 实例。但现有 `AcquisitionReportWriter` **不接受** `AcquisitionReportPaths` 参数。

两个解决方向：
- 方案 A: 新增 `AcquisitionReportWriter(Path outputRoot, AcquisitionReportPaths paths)` 重载构造器。
- 方案 B: `AcquisitionReportBridge` 手动计算 `outputRoot.resolve(paths.outputDirectory())` 再传给现有的单参构造器。

当前 SR 伪代码暗示方案 A 但没有明确声明这是新增构造器还是替换。

**影响**: 实现者可能发现 `AcquisitionReportWriter` 没有双参构造器而卡住。Bridge 的设计依赖于此 API 变更。

**建议**: SR 明确选择方案 A（新增重载构造器）。理由：让 `AcquisitionReportWriter` 直接接受版本化路径更内聚；调用方不需要手动 resolve。

---

### F02 [P1] AcquisitionReportBridge.notEvaluatedReadiness() 语义偏移

**位置**: 20-sr.md §4.6

**问题**: SR 的 `notEvaluatedReadiness()` 方法返回 `ReadinessStatus.NOT_READY` + reason `"not evaluated in acquisition-only mode"`。但 `NOT_READY` 的语义是"数据不足、不能进入下一阶段"——这与采集模式的实际情况矛盾：

- 采集模式的数据**已经收集完成**（9 runs, profiles 齐全，门禁通过）
- 不是"NOT_READY"——是"没有人来评估 readiness"
- 下游消费者（如后续版本的闭环实验）可能因看到 `NOT_READY` 而拒绝使用实际上质量合格的数据

**影响**: 下游工具和 agent 可能错误地将采集数据标记为不可用，尽管 G1-G9 门禁全部通过。`NOT_READY` 的 reason 虽然解释了"not evaluated"，但 status 字段是机器可读的——自动决策只看 status，不看 reason。

**建议**: 两种修复方案：
- 方案 A: `AcquisitionReportBridge` 不产出 `ReadinessSummary`。`writeAll()` 改为 `writeAll(manifest, pressure, replay, index)`（4 artifact），SR 明确声明纯采集模式 readiness 是 optional 的。
- 方案 B: 维持 5 artifact，但 readiness 使用 `ReadinessStatus.READY_WITH_RISK` + reason `"readiness not evaluated in acquisition-only mode; data quality gates G1-G9 passed"`。

推荐方案 A —— readiness 评估是后续工作（offline replay），采集模式不应越权声明。

---

### F03 [P1] Runner 创建 DeletionSafety 但未使用引用计数

**位置**: 20-sr.md §4.2

**问题**: SR 的 runner Phase 1 创建 `AtomicDeletionSafety` 和 `ExecutorRegistry`，但整个 runner 生命周期中：
- 从未调用 `deletionSafety.acquire()`
- 从未调用 `deletionSafety.release()`
- 只有 `registry.remove()` 时 `DeletionSafety.canRemove()` 被间接调用

`AtomicDeletionSafety.canRemove()` 检查 `refCount == 0 && isTerminated()`。由于 `refCount` 始终为 0（无 acquire），唯一有效的检查是 `isTerminated()`——但 runner 已经在 Phase 5 `shutdownAndTerminate()` 中确保 executor 终止。

结论：`AtomicDeletionSafety` 在此处的引用计数功能完全未被使用。Runner 创建了一个有并发安全机制的组件但只使用了其最简单的检查（`isTerminated()`），这是过度设计。

**影响**: 不影响功能正确性——`canRemove()` 的 `isTerminated()` 检查仍然有效。但代码阅读者可能困惑为什么 reference counting 被引入却不使用。也增加了不必要的对象创建。

**建议**: Runner 直接检查 `executor.isTerminated()` 再调用 `registry.remove()`，不创建 `AtomicDeletionSafety`。`ExecutorRegistry` 可接受无 `DeletionSafety` 的构造器（或传入 `DeletionSafety` 的 no-op 实现）。SR 记录此简化决策。

---

### F04 [P1] AcquisitionReportBridge 归属模糊

**位置**: 20-sr.md §4.6

**问题**: SR §4.6 对 Bridge 的包归属声明为"SR 阶段确定——推荐 `experiment.scenario` 或 `experiment.acquisition`"。但 change 分解将 Bridge 分配到 change 2（`acquisition-paths-and-quality-gates`），暗示它在 `experiment.acquisition` 包中。

如果 Bridge 在 `experiment.acquisition`：
- Bridge 需要依赖 `ManagedExecutorConfig`（在 `experiment.executor`）→ 引入 `experiment.acquisition → experiment.executor` 新依赖方向
- Bridge 需要依赖 `ScenarioRunOutcome`、`ScenarioDefinition`（在 `experiment.scenario`）→ 已有方向

如果 Bridge 在 `experiment.scenario`：
- 需要依赖 `AcquisitionReportWriter`、`PressureSummary`、`RunManifest` 等（在 `experiment.acquisition`）→ 引入 `experiment.scenario → experiment.acquisition` 新依赖方向

当前 SR section 3 的依赖方向表没有记录这两个新方向。

**影响**: 包归属决定哪个模块需要新增依赖。Change 2 的边界取决于此决策。

**建议**: SR 明确 Bridge 归属为 `experiment.acquisition`。理由：
- `experiment.acquisition` 已有 `RunManifest`、`PressureSummary`、`ReplaySummary` 等类型——Bridge 的核心职责是"聚合 snapshot 数据为这些 acquisition 类型"，放 acquisition 包内聚。
- `experiment.acquisition → experiment.executor` 的新依赖仅限于 `ManagedExecutorConfig`（纯数据 record），风险可控。
- `experiment.acquisition → experiment.scenario` 的新依赖仅限于 `ScenarioRunOutcome` 和 `ScenarioDefinition`（纯数据类），风险可控。
- 在 SR section 3 依赖方向表中显式记录这两个新允许方向。

---

### F05 [P2] buildObservation() 逻辑与 SnapshotAssembler.fromExecutorState() 重复

**位置**: 20-sr.md §4.2, §4.3

**问题**: Runner 的 `buildObservation()` 方法与 `SnapshotAssembler.fromExecutorState()` 的 `RuntimeObservation` 构造逻辑完全相同——都是从 `ExecutorStateSnapshot` 的 nullable 字段转为 `MetricValue`。SR 在 §4.3 末尾提到"两种方式等价，SR 不强制选择"。

这留下了两个做同样事情的方法。实现阶段可能出现 subtle 差异（例如一个在 activeCount=null 时用 `absent()`，另一个用 `present(0)`）。

**影响**: 维护负担轻微——两处重复逻辑需要同步更新。不影响功能。P2。

**建议**: SR 建议 runner 优先使用 `sampler.sampleFromExecutorState(runId, executor.toSnapshot())`，删除 runner 内部的 `buildObservation()` 方法。这消除重复并让 runner 代码更简洁。SR 可记录为 "推荐用法" 而非强制。

---

### F06 [P2] startedLatch.await() 超时后行为未定义

**位置**: 20-sr.md §4.2

**问题**: SR 的 Phase 3 使用 `startedLatch.await(5, SECONDS)` 等待任务线程启动。伪代码未处理 `await()` 返回 `false`（超时）的情况：

```java
startedLatch.await(5, TimeUnit.SECONDS);
// 若返回 false = 超时，部分任务可能未启动
// 此时采样可能得到 activeCount < taskCount
```

**影响**: 在极端负载的 CI 环境中，5 秒超时触发后 runner 继续采样。此时 `activeCount` 可能低于预期值，但不影响数据质量门禁判断（G7-G9 不检查 activeCount 的期望值）。实际影响很小——5 秒对于线程启动是充足的。

**建议**: SR 添加超时处理：若 `startedLatch.await()` 返回 `false`，记录警告日志但不抛异常。runner 继续采样——数据质量可能降低但不出错。不需要在 IR/SR 层面强制。

---

## 4. Findings 汇总

| ID | 位置 | 描述 | 级别 | 建议动作 |
|---|---|---|---|---|
| F01 | §4.4/§4.6 | AcquisitionReportWriter 不原生支持版本化路径 | P1 | 新增双参构造器重载 |
| F02 | §4.6 | Bridge 错误使用 NOT_READY 标记未评估数据 | P1 | 不产出 ReadinessSummary；或改为 READY_WITH_RISK |
| F03 | §4.2 | Runner 创建 AtomicDeletionSafety 但无用 | P1 | 移除 DeletionSafety，直接检查 isTerminated() |
| F04 | §4.6 | Bridge 包归属未定，依赖方向未记录 | P1 | 归属 acquisition 包，记录新依赖方向 |
| F05 | §4.2/§4.3 | buildObservation() 与 fromExecutorState() 重复 | P2 | 推荐使用 sampleFromExecutorState() 消除重复 |
| F06 | §4.2 | startedLatch 超时行为未定义 | P2 | 添加超时警告日志 |

## 5. 正向检查通过项

- [x] SR 不授权实现或 OpenSpec change——措辞保持设计阶段。
- [x] 6 个组件设计完整，含 Java 契约伪代码。
- [x] IR 阶段 4 个 deferred 项全部处置：
  - [x] F04 (startedLatch) → SR §4.2 明确定义 `CountDownLatch(taskCount)` + `await(5s)`
  - [x] F06 (跨包依赖) → SR §3 依赖方向表+裁决理由
  - [x] F07 (policyId) → SR §4.1 `toPresetSummary()` 使用 `"managed-executor-v0.8.0"` 占位值
  - [x] F10 (空闲标准) → SR §4.2 `waitForIdle()` 定义 `queueSize==0 && activeCount==0`，10s 超时
- [x] 依赖方向清晰，新依赖方向有裁决理由。
- [x] 7-phase runner 流程完整覆盖创建→运行→采样→清理→移除。
- [x] v0.7.0 P6 教训已纳入（先 countDown 再 shutdown+awaitTermination）。
- [x] G7-G9 门禁逻辑具体、可测试。
- [x] 非回归约束明确——412 测试，不修改现有代码。
- [x] 2 个 change 分解合理，依赖关系正确。
- [x] 测试策略分层清晰，覆盖单元/集成/端到端。
- [x] Runner-to-Report bridge 设计完整，5 个 artifact 产出路径明确。
- [x] G8 per-profile 逻辑正确区分 STEADY（豁免）、RAMP（≥1）、BURST（≥2）。
- [x] 向后兼容保证——`AcquisitionReportPaths` 静态常量保留，`AcquisitionContractsTest` 断言不变。

## 6. 评审结论

SR 设计在组件完整性、依赖方向清晰度和 IR deferred 项处置方面**合格**。6 个核心组件 + bridge 设计覆盖了从 runner 到 report 的完整数据流。但**不能直接进入 change decomposition**：存在 4 个 P1 问题（F01 Writer API 不匹配、F02 Readiness 语义偏移、F03 DeletionSafety 过度创建、F04 Bridge 归属未定）。P1 必须通过 disposition 关闭。

评审建议：**进入 SR disposition（`22-sr-review-disposition.md`）**，逐项处置 F01-F06。
