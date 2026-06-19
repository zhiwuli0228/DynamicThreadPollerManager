# v0.8.0 IR 独立评审报告

## Header

- Document type: IR independent review
- Version name: `v0.8.0`
- Reviewed artifact: `docs/04-development/versions/v0.8.0/10-ir.md`
- Review date: `2026-06-12`
- Reviewer role: 独立 IR review（非 IR 作者）
- Review basis: `docs/02-harness/managed-change-standard.md` 第 2 节（IR 需求分析）

## 1. 评审输入

已读取以下文件作为评审上下文：

- `docs/00-project/current-state.md`
- `docs/02-harness/managed-change-standard.md`
- `docs/01-architecture/observability-and-experiment-strategy.md`
- `docs/01-architecture/managed-executor-domain-model.md`
- `docs/04-development/versions/v0.8.0/README.md`
- `docs/04-development/versions/v0.8.0/00-objectives-and-scope.md`
- `docs/04-development/versions/v0.8.0/10-ir.md`
- `docs/04-development/versions/v0.8.0/decision-log.md`
- `docs/04-development/versions/v0.7.0/15-experiment-data-acquisition-plan.md`
- `docs/08-retrospectives/2026-06-12-v0.7.0-managed-executor-domain-retrospective.md`
- `openspec/specs/closed-loop-experiment-verification/spec.md`
- `openspec/specs/pressure-data-acquisition-and-baseline/spec.md`

## 2. 评审摘要

IR 草案整体结构完整：6 条需求覆盖了从 `ManagedExecutorConfig` 参数封装到 G7-G9 质量门禁的完整链路，范围边界清晰（明确排除 queue resizing、闭环调度、CLI 入口），非回归约束到位。但存在 3 个 P0 阻断项和 5 个 P1 关键项需要处置，主要集中在 ExecutorRegistry 生命周期归属、runner 到 acquisition report 的桥接缺失、G7 数据结构未定义、以及 step-level sleep 的确定性风险。

## 3. Findings

### F01 [P0] ManagedExecutorScenarioRunner 未定义 ExecutorRegistry 生命周期归属

**位置**: IR-v0.8-002

**问题**: IR-v0.8-002 的 Phase 1 要求"创建 `ManagedExecutor` 并注册到 `ExecutorRegistry`"，Phase 7 要求"从 `ExecutorRegistry` 移除"。但 `ExecutorRegistry` 实例的创建和所有权未定义：

- `ExecutorRegistry` 是由 runner 内部创建并持有？还是作为构造参数注入？
- 若内部创建：`DeletionSafety` 实例（`AtomicDeletionSafety`）也需要配套创建。
- 若注入：调用方需要感知 registry 和 deletion safety 的存在——但这与 runner 作为"一站式"数据获取入口的设计意图冲突。

当前 IR 的构造参数列表中没有 `ExecutorRegistry`，暗示 runner 内部创建。但 IR 正文未明确声明这一点。Phase 2 还说"创建 `ManagedExecutorAdjustmentAdapter`"——adapter 需要 `ExecutorRegistry` + `RuntimeAdjustmentSafetyGate` + executor name。IR 未说明这些依赖的来源。

**影响**: SR 设计者可能产出两种不同的 runner 构造函数设计，导致 OpenSpec change 阶段返工。

**建议**: IR-v0.8-002 明确 `ExecutorRegistry` 和 `AtomicDeletionSafety` 由 runner 内部创建，不暴露给调用方。adapter 使用的 safety gate 也由 runner 内部选择（permissive gate），不注入。

---

### F02 [P0] Runner 到 AcquisitionReport 的桥接缺失

**位置**: IR-v0.8-002, 全局

**问题**: IR-v0.8-002 的 runner 产出 `ScenarioRunOutcome`（v0.3.0 的合约：runId + scenarioId + policyId + stepCount + workUnits + evidenceCount + finalState）。但 v0.8.0 的数据获取目标是产出 5 个标准 JSON artifact（`RunManifest`、`PressureSummary`、`ReplaySummary`、`ReadinessSummary`、`EvidenceIndex`）。

当前 IR 在以下链路存在断裂：

```
ManagedExecutorScenarioRunner.run()
  → ScenarioRunOutcome          // 只有计数和状态，没有 snapshot 数组，没有 manifest 字段
  → ???                         // 缺失：谁调用 AcquisitionReportWriter.writeAll()？
  → 5 个 JSON artifact          // 目标产出
```

具体缺口：
- `RunManifest` 需要 `scenarioProfile`、`seed`、`stepCount`、`baselinePreset`、`environmentSummary`、`commandLine` — `ScenarioRunOutcome` 不携带其中大部分字段。
- `PressureSummary` 需要 `totalSnapshotCount`、`profileSnapshotCounts`、`scaleEventCount`、`queueDepth` — runner 不产出这些聚合指标。
- `EvidenceRecorder.record()` 在 runner 中写入 snapshot，但没有定义谁在 runner 完成后调用 `recorder.snapshots(runId)` 并聚合为 `PressureSummary`。

**影响**: Runner 跑完了，数据在 `EvidenceRecorder` 中，但没有从 runner 输出到 report writer 的桥接代码。SR 可能遗漏这一整段集成逻辑。

**建议**: IR 新增一条需求，定义 runner 输出到 acquisition report 的桥接：runner 完成后，调用方（或 runner 自身）从 `EvidenceRecorder` 提取 snapshots → 聚合为 `PressureSummary` → 构建 `RunManifest` → `AcquisitionReportWriter.writeAll()`。明确这段逻辑的归属（runner 内部还是外部调用方）。

---

### F03 [P0] G7 extendedFieldPresence 数据结构未具体定义

**位置**: IR-v0.8-006

**问题**: IR-v0.8-006 要求 G7 检查"每个 snapshot 必须携带 `extendedFieldPresence` 标记"。但 `extendedFieldPresence` 的数据结构未定义：

- 是 `Map<String, Boolean>`（字段名 → 是否存在）？
- 是 `AcquisitionDataSet.RunSnapshot` 的新增字段？
- 是 snapshot 元数据的一部分还是独立数据结构？
- 包含哪些字段？IR 正文列出 5 个（poolSize, completedTaskCount, keepAliveTimeSeconds, largestPoolSize, taskCount），但 `activeCount` 和 `queueSize` 在 v0.6.0 的探针数据中已经存在——它们是否也纳入检查？

当前 `AcquisitionDataSet.RunSnapshot` 只有 `runId`, `scenarioId`, `profile`, `seed`, `baselinePolicyId`, `snapshotTimestamps`。G7 需要的数据没有落盘位置。

**影响**: SR 无法设计具体的数据结构。G7 门禁的验证逻辑在 OpenSpec change 阶段无法实现。

**建议**: IR-v0.8-006 明确 `RunSnapshot` 的扩展方式。建议新增 `Map<String, Boolean> extendedFieldPresence` 字段（nullable，默认为空 Map；v0.6.0 数据不填充此字段时 G7 自动跳过）。字段名称使用 `ExecutorStateSnapshot` 的 getter 名（`poolSize`, `completedTaskCount`, `keepAliveTimeSeconds`, `largestPoolSize`, `taskCount`）。

---

### F04 [P1] Step-level sleep(100ms) 缺乏确定性备选方案

**位置**: IR-v0.8-002

**问题**: IR-v0.8-002 Phase 4 要求"等待线程调度生效（`Thread.sleep(100)` 或等效同步机制）"。但 `Thread.sleep(100)` 是纯时间等待，不保证线程已实际启动并进入 `CountDownLatch.await()` 状态。在 CI 环境或高负载机器上，100ms 可能不够。

v0.7.0 回溯 P1 明确指出"测试未考虑 ThreadPoolExecutor 实际验证规则"——当前 IR 延续了同样的模式：先写测试（sleep 等待），后理解 TPE 行为。

可能的确定性替代方案：使用第二个 `CountDownLatch` 或 `CyclicBarrier`，让任务线程在开始 `await(blocker)` 之前先 signal 一个 `startedLatch`，runner 在 `startedLatch.await()` 上等待所有任务确认启动后再采样。

**影响**: 偶发的 G8 失败（queueSize=0，因为任务尚未入队），增加 CI 不稳定性和调试成本。

**建议**: IR-v0.8-002 明确要求使用同步屏障（如 `startedLatch`）替代或补充 sleep。至少声明 sleep 时间可配置，且 G8 的 P1 级别允许个别 run 在 queue pressure 不足时降级。

---

### F05 [P1] G8 queue pressure 门禁可能在 STEADY 场景误报

**位置**: IR-v0.8-006

**问题**: IR-v0.8-006 G8 要求"每个 run 的 snapshot 列表中，至少 1 个 snapshot 的 `queueSize > 0`"。但 STEADY 场景的定义是"每步固定 2 个阻塞任务"，core=2 时刚好 2 个线程消费 2 个任务，队列可能始终为空。

实际情况：
- core=2, 提交 2 个 `await(latch)` 任务 → 两个核心线程各取一个 → queueSize=0
- 下一步先 countDown 释放上一步的任务 → 任务完成 → 提交新的 2 个任务 → 核心线程立即拾取 → queueSize 仍为 0

STEADY 场景下 queueSize=0 是**正确的行为**（队列无堆积 = steady 状态），不是采样时机错误。

**影响**: STEADY 的 3 个 run 全部 G8 失败，数据集被判定为 INVALID，但实际上数据是正确的。这会导致 `AcquisitionReadinessClassifier` 给出错误的 `NOT_READY` 判定。

**建议**: G8 改为 per-profile 判定：STEADY 不要求 queueSize > 0（稳态无堆积是预期行为）；RAMP 至少 1 个 snapshot queueSize > 0；BURST 至少 2 个 snapshot queueSize > 0。或者更简单：G8 只对 RAMP 和 BURST 强制执行，STEADY 豁免。

---

### F06 [P1] fromExecutorState() 引入跨包依赖

**位置**: IR-v0.8-004

**问题**: `SnapshotAssembler` 位于 `experiment.metrics` 包。`ExecutorStateSnapshot` 位于 `experiment.adjustment` 包。IR-v0.8-004 要求 `SnapshotAssembler` 接口新增接受 `ExecutorStateSnapshot` 的方法。这引入了 `experiment.metrics` → `experiment.adjustment` 的依赖。

当前 v0.7.0 SR section 6 的依赖方向表明确记录 `experiment.metrics ⊥ experiment.executor`（禁止依赖）。但 `experiment.metrics` → `experiment.adjustment` 的关系未被明确禁止。需要确认这是否构成架构违规。

v0.7.0 SR section 3 的模块边界表说 `experiment.metrics`："不直接从 `ManagedExecutor` 读取，通过 `ExecutorStateSnapshot` 桥接"。这暗示 metrics 通过 adjustment 包的类型（`ExecutorStateSnapshot`）桥接是预期的。但需要显式记录这个新增依赖方向。

**影响**: 如果被认为是架构违规，`fromExecutorState()` 需要移到 `experiment.adjustment` 包或新的转换工具类。SR 需要明确裁决。

**建议**: IR 或 SR 明确记录 `experiment.metrics` → `experiment.adjustment` 的依赖为允许方向（因为 `ExecutorStateSnapshot` 是纯数据类，不携带 mutation 授权）。或者在 `experiment.adjustment` 包中提供反向转换方法。

---

### F07 [P1] ManagedExecutorConfig.toPresetSummary() 命名词义偏移

**位置**: IR-v0.8-001

**问题**: IR-v0.8-001 要求 `toPresetSummary()` 返回 `RunManifest.BaselinePresetSummary`。但 `BaselinePresetSummary` 的名称和语义来自 v0.6.0 的 baseline 概念——"baseline" 指的是固定的、非 adaptive 的 executor preset。v0.8.0 的 `ManagedExecutor` 是真实线程池，"preset" 的词义不完全匹配。

技术层面，`BaselinePresetSummary` 的字段（`policyId`, `corePoolSize`, `maximumPoolSize`, `queueCapacity`）确实能承载 ManagedExecutor 的配置信息。问题在于：
- `policyId` 字段对于 ManagedExecutor 数据来说是伪造的——ManagedExecutorConfig 没有 policyId 概念。
- `RunManifest.baselinePolicyId` 如何填写？

**影响**: 不影响功能实现，但会在 manifest JSON 中留下语义不一致的字段值。SR 可以决定接受这个语义偏移（因为这已经是现有 schema），也可决定为 v0.8.0 新增字段。

**建议**: SR 明确决策：是复用 `BaselinePresetSummary`（policyId 填固定值如 `"managed-executor-v0.8.0"`），还是为 ManagedExecutor 数据新增 `ManagedExecutorConfigSummary` 类型。推荐前者（保持 manifest schema 兼容），SR 记录语义偏移。

---

### F08 [P1] RAMP capping 策略未具体定义

**位置**: IR-v0.8-002

**问题**: IR-v0.8-002 对 RAMP profile 定义："第 i 步提交 `2 + i` 个任务，cap 到 `queueCapacity + maximumPoolSize`"。但 cap 行为的具体语义未定义：

- 当 `2 + i > queueCapacity + max` 时，是只提交 `queueCapacity + max` 个任务，还是抛出错误？
- 如果是前者，后期 step 的任务数全部等于 cap 值——这不再是 RAMP（递增），而是 RAMP 到达上限后变为 STEADY。
- `queueCapacity=10, max=4` → cap=14 → `2+12=14` 在第 12 步到达 cap。但 RAMP 只有 8 步，`2+7=9 < 14`。所以 8-step RAMP 不会触发 cap。

但 IR 没有说明 8-step 选择与 cap 的关系。如果 SR 调整 stepCount 或 queueCapacity，cap 行为需要明确。

**影响**: SR 设计者可能对 capping 做出不同假设，导致 runner 行为不一致。

**建议**: IR 或 SR 明确：在 8-step RAMP 和 queueCapacity=10 的默认配置下，cap 不会被触发。Cap 规则的存在是为了防止更大 stepCount 配置下的 `RejectedExecutionException`。超过 cap 时的行为是提交 cap 值数量的任务（不抛异常）。

---

### F09 [P2] Permissive safety gate 未定义

**位置**: IR-v0.8-002 Phase 2

**问题**: IR-v0.8-002 要求"创建 `ManagedExecutorAdjustmentAdapter`（使用 permissive safety gate + READY readiness）"。但 `permissive safety gate` 是 v0.7.0 实现中的测试用匿名类模式（`ClosedLoopExperimentTest` 不曾使用它；`ManagedExecutorAdjustmentAdapterTest` 中的 permissive gate 是一个 inline anonymous class），不是稳定的公共 API。

当前代码库中没有名为 `PermissiveSafetyGate` 的类。v0.8.0 runner 需要的是一个"允许所有值"的 safety gate——这需要 SR 明确是复用 `DefaultRuntimeAdjustmentSafetyGate` 配宽松配置，还是新建一个明确命名的实现。

**影响**: SR 需要决定 safety gate 策略。不影响 IR closure（P2），但必须在 SR 中处置。

**建议**: SR 阶段决策：是 runner 内部使用 `DefaultRuntimeAdjustmentSafetyGate` + max bounds 配置，还是 runner 直接绕过 safety gate 读状态（因为 runner 只采样不调整，不需要 safety gate）。推荐后者：runner 不需要 adapter 来采样——直接使用 `executor.toSnapshot()` 更简单直接。

---

### F10 [P2] step 间"回到空闲状态"未定义

**位置**: IR-v0.8-002 Phase 4

**问题**: IR-v0.8-002 Phase 4 要求在 latch.countDown() 释放上一步任务后，"等待 executor 回到空闲状态（所有已提交任务完成）"再进入下一步。但"空闲状态"的判断标准未定义：

- `getActiveCount() == 0`？
- `getCompletedTaskCount() - previousCompletedTaskCount == submittedThisStep`？
- `getQueueSize() == 0 && getActiveCount() == 0`？

每种标准的准确性和开销不同。`getActiveCount() == 0` 最直接但不保证队列为空。组合标准最准确但需要 tracking 已提交任务数。

**影响**: Step 间残留任务会影响下一步的采样（activeCount 和 queueSize 偏高）。可能影响 G8 判断但不影响功能正确性。

**建议**: SR 定义空闲条件：`getQueueSize() == 0` 且 `getActiveCount() == 0`。P2 可接受，不影响 IR closure。

---

## 4. Findings 汇总

| ID | 位置 | 描述 | 级别 | 建议动作 |
|---|---|---|---|---|
| F01 | IR-v0.8-002 | Runner 未定义 ExecutorRegistry 生命周期归属 | P0 | 明确 registry 由 runner 内部创建 |
| F02 | IR-v0.8-002 | Runner 到 AcquisitionReport 的桥接缺失 | P0 | 新增 IR 条目定义桥接逻辑 |
| F03 | IR-v0.8-006 | G7 extendedFieldPresence 数据结构未定义 | P0 | 明确 RunSnapshot 扩展方式 |
| F04 | IR-v0.8-002 | Step sleep(100ms) 缺乏确定性同步屏障 | P1 | 要求使用 startedLatch 替代或补充 |
| F05 | IR-v0.8-006 | G8 可能对 STEADY 场景误报 | P1 | G8 改为 per-profile 判定 |
| F06 | IR-v0.8-004 | fromExecutorState() 跨包依赖需明确 | P1 | SR 明确依赖方向允许性 |
| F07 | IR-v0.8-001 | toPresetSummary() 语义偏移 | P1 | SR 记录字段映射决策 |
| F08 | IR-v0.8-002 | RAMP capping 行为未具体定义 | P1 | 明确 cap 行为和触发条件 |
| F09 | IR-v0.8-002 | Permissive safety gate 未定义 | P2 | SR 决定 safety gate 策略 |
| F10 | IR-v0.8-002 | Step 间空闲状态判断标准未定义 | P2 | SR 定义空闲条件 |

## 5. 正向检查通过项

- [x] IR 不隐含实现授权——各条目使用"候选验收语义"措辞，未声称已实现。
- [x] scope 边界明确排除 queue resizing、闭环调度、生产环境、CLI 入口。
- [x] 非范围列表与 `00-objectives-and-scope.md` 和 `decision-log.md` 一致。
- [x] 不修改 `ScenarioExperimentRunner` 或 `BaselineWorkloadExecutor`，确保零回归。
- [x] 复用 `DeterministicScenarioPlanner`、`ManualPressureSampler`、`EvidenceRecorder` 等现有组件，不新建并行 pipeline。
- [x] Runner 只采集不调整——policy evaluation 和 adjustment 不在 runner 范围内。
- [x] v0.7.0 回溯 P6（latch before shutdown）已纳入 runner 的 cleanup 要求。
- [x] v0.7.0 回溯 P1（测试先理解 TPE 行为）虽未完全解决但已识别为 F04。
- [x] `SnapshotAssembler` 扩展使用默认方法保持二进制兼容。
- [x] `AcquisitionReportPaths` 版本化保持向后兼容。
- [x] G7-G9 门禁不影响 v0.6.0 BaselineWorkloadExecutor 数据。
- [x] 风险和延期项表覆盖了线程调度、cap、执行时间、performance claim 禁止。
- [x] 出口条件清单与 `managed-change-standard.md` 第 2 节一致。

## 6. 评审结论

IR 草案在范围控制、架构对齐和组件复用方面**合格**。6 条需求覆盖了从 `ManagedExecutorConfig` 到 G7-G9 门禁的完整链路，非回归约束清晰。但**不能直接进入 SR**：存在 3 个 P0 阻断项（F01 registry 生命周期归属、F02 runner-to-report 桥接缺失、F03 G7 数据结构未定义）和 5 个 P1 关键项。P0/P1 必须通过 disposition 关闭。

评审建议：**进入 IR disposition（`12-ir-review-disposition.md`）**，逐项处置 F01-F10。
