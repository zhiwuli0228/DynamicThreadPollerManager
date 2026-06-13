# v0.8.0 IR Review Disposition

## Header

- Document type: IR review disposition
- Version name: `v0.8.0`
- Reviewed artifact: `docs/04-development/versions/v0.8.0/11-ir-review.md`
- Disposition date: `2026-06-12`
- Disposition basis: `docs/02-harness/managed-change-standard.md` 第 2 节，严重级别规则

## 1. 处置规则

| 级别 | 处置方式 |
|---|---|
| P0 | 必须修复并闭环，不能进入 SR |
| P1 | 必须修复或明确 defer 到 SR 并记录风险 |
| P2 | 可修复或记录残余风险，需说明后续触发条件 |

## 2. 逐项处置

### F01 [P0] — Runner 未定义 ExecutorRegistry 生命周期归属

**处置**: **FIX**. 在 IR-v0.8-002 增补 registry 生命周期条款。

**动作**: 在 IR-v0.8-002 "候选验收语义" 中增补以下条款：

> - `ExecutorRegistry` 和 `AtomicDeletionSafety` 由 runner 内部创建，不作为构造参数注入。
> - `ManagedExecutorAdjustmentAdapter` 使用的 `RuntimeAdjustmentSafetyGate` 由 runner 内部提供（使用 `DefaultRuntimeAdjustmentSafetyGate` 的宽松配置或直接绕过——runner 只采样不调整，见 F09 处置）。
> - Runner 的 `@AfterEach` 或等效清理路径负责：先 `shutdown()` + `awaitTermination()` 再 `registry.remove()`。

**状态**: `FIXED` — 将在 `10-ir.md` 中直接修正，由 `13-ir-closure-verification.md` 验证。

---

### F02 [P0] — Runner 到 AcquisitionReport 的桥接缺失

**处置**: **FIX**. 新增 IR-v0.8-007 "Runner-to-Report Bridge"，不再扩展现有 IR 条目。

**动作**: 在 `10-ir.md` 中新增以下 IR 条目：

> **IR-v0.8-007 Runner-to-Report Bridge (P0)**
>
> 系统需要将 `ManagedExecutorScenarioRunner` 的产出（`ScenarioRunOutcome` + `EvidenceRecorder` 中的 snapshots）桥接到 v0.6.0 的 acquisition report 流水线（`AcquisitionReportWriter.writeAll()`）。
>
> 候选验收语义：
> - Runner 完成后，必须从 `EvidenceRecorder.snapshots(runId)` 提取所有 `ObservedSnapshot`。
> - 必须聚合 snapshot 数据为 `PressureSummary`（totalSnapshotCount, peakObservedQueueDepth, meanObservedQueueDepth, scaleEventCount=0（runner 不调整））。
> - 必须从 `ManagedExecutorConfig` + `ScenarioDefinition` + 环境信息构建 `RunManifest`。
> - 必须调用 `AcquisitionReportWriter.writeAll(manifest, pressureSummary, replaySummary, readinessSummary, evidenceIndex)` 产出 5 个 JSON artifact。
> - `ReplaySummary` 在 runner-only 模式下填默认值（evidenceCount=snapshotCount, decisionCount=0, 因为没有 policy replay）。
> - `ReadinessSummary` 在 runner-only 模式下可省略或填 `NOT_EVALUATED` 占位。
> - 此桥接逻辑可位于 runner 内部的 `finalizeRun()` 方法，或作为独立的后处理步骤由调用方执行。SR 阶段确定具体归属。

**状态**: `FIXED` — 新增 IR 条目将直接追加到 `10-ir.md`。

---

### F03 [P0] — G7 extendedFieldPresence 数据结构未定义

**处置**: **FIX**. 在 IR-v0.8-006 增补具体数据结构定义。

**动作**: 在 IR-v0.8-006 "候选验收语义" 中增补以下条款：

> - `AcquisitionDataSet.RunSnapshot` 新增可选字段 `Map<String, Boolean> extendedFieldPresence`，默认值为空 Map（nullable，null 等同于空 Map）。
> - Map 的 key 使用 `ExecutorStateSnapshot` 的 getter 方法名：`"poolSize"`, `"completedTaskCount"`, `"keepAliveTimeSeconds"`, `"largestPoolSize"`, `"taskCount"`。
> - Map 的 value 为 `true` 表示该字段在对应 run 的所有 snapshot 中均 non-null。
> - 若 `extendedFieldPresence` 为空或 null，G7 跳过该 run（向后兼容 v0.6.0 BaselineWorkloadExecutor 数据）。
> - G7 失败条件：`extendedFieldPresence` 非空，但任一 required key 的 value 为 `false` 或缺失。

**状态**: `FIXED` — 将在 `10-ir.md` 中直接修正。

---

### F04 [P1] — Step sleep(100ms) 缺乏确定性同步屏障

**处置**: `DEFER_TO_SR`. IR 层面已表达了"等待线程调度生效"的意图，且 IR-v0.8-002 的措辞为"`Thread.sleep(100)` 或等效同步机制"，为 SR 留下了选择空间。

**理由**: 具体的同步屏障设计（`startedLatch` vs `CyclicBarrier` vs sleep）是实现层面决策。IR 的 P1 风险表已记录"线程调度不确定性导致 G8 偶发失败"为 P1 风险，触发条件为"SR 可降低 G8 阈值或使用 CountDownLatch 同步屏障替代 sleep"。

**SR 触发**: SR 必须选择具体的同步机制。推荐：每个 step 使用 `CountDownLatch startedLatch = new CountDownLatch(submittedTaskCount)`，任务线程在执行 `await(blocker)` 前 `startedLatch.countDown()`；runner 在 `startedLatch.await(5, SECONDS)` 后采样。

**状态**: `DEFERRED_TO_SR` with risk record.

---

### F05 [P1] — G8 可能对 STEADY 场景误报

**处置**: **FIX**. 修正 IR-v0.8-006 的 G8 判定逻辑。

**动作**: 将 G8 从：

> 每个 run 的 snapshot 列表中，至少 1 个 snapshot 的 `queueSize > 0`

改为：

> - `STEADY` profile：不要求 `queueSize > 0`（稳态无队列堆积是预期行为）。
> - `RAMP` profile：至少 1 个 snapshot 的 `queueSize > 0`。
> - `BURST` profile：至少 2 个 snapshot 的 `queueSize > 0`。
>
> 若 run 的 profile 信息不可得，降级为至少 1 个 snapshot `queueSize > 0`。

**理由**: STEADY 场景 core=2 线程刚好消费 2 个任务，queueSize=0 是正确行为而非采样错误。

**状态**: `FIXED` — 将在 `10-ir.md` 中直接修正。

---

### F06 [P1] — fromExecutorState() 跨包依赖需明确

**处置**: `DEFER_TO_SR`. IR 层面的需求（消除手动构造 PressureSnapshot）是正确的。具体的包依赖方向由 SR 裁决。

**理由**: `experiment.metrics` → `experiment.adjustment` 的依赖在新方法引入后成为事实。v0.7.0 SR section 3 的模块边界表说 `experiment.metrics`："不直接从 `ManagedExecutor` 读取，通过 `ExecutorStateSnapshot` 桥接"——这暗示通过 adjustment 类型桥接是预期设计。但需要显式记录在架构文档或 SR 的依赖方向表中。

不影响 IR closure，因为 `ExecutorStateSnapshot` 是纯数据类（无 mutation 授权），依赖方向风险可控。

**SR 触发**: SR 必须在依赖方向表中新增 `experiment.metrics → experiment.adjustment` 为允许方向，并标注仅限 `ExecutorStateSnapshot`（纯数据类）。备选方案：在 `experiment.adjustment` 包中提供 `ExecutorStateSnapshot.toRuntimeObservation()` 反向转换，由 `experiment.metrics` 的调用方执行转换。

**状态**: `DEFERRED_TO_SR` with architecture note.

---

### F07 [P1] — ManagedExecutorConfig.toPresetSummary() 语义偏移

**处置**: `DEFER_TO_SR`. IR 层面保留现有方法签名。语义偏移是现有 schema 复用的已知代价。

**理由**: `BaselinePresetSummary` 的字段集（policyId, corePoolSize, maximumPoolSize, queueCapacity）与 ManagedExecutor 配置字段高度重合。policyId 语义偏移可用固定占位值（如 `"managed-executor-v0.8.0"`）处理。

**SR 触发**: SR 确认以下决策：
- `policyId` 字段填写 `"managed-executor-v0.8.0"`。
- `RunManifest.baselinePolicyId` 同理。
- 不为此创建新的 summary 类型，以保持 manifest schema 兼容。
- 决策记录在 `decision-log.md`。

**状态**: `DEFERRED_TO_SR` with decision pending.

---

### F08 [P1] — RAMP capping 行为未具体定义

**处置**: **FIX**. 在 IR-v0.8-002 增补 capping 语义。

**动作**: 在 IR-v0.8-002 RAMP profile 定义中增补：

> - 当 `2 + i > queueCapacity + maximumPoolSize` 时，提交 `queueCapacity + maximumPoolSize` 个任务（不抛 `RejectedExecutionException`）。
> - 在默认配置下（core=2, max=4, queue=10, stepCount=8），`2+7=9 < 14`，cap 不会被触发。
> - Cap 规则为更大 stepCount 或更小 queue 配置提供安全边界。

**状态**: `FIXED` — 将在 `10-ir.md` 中直接修正。

---

### F09 [P2] — Permissive safety gate 未定义

**处置**: **FIX**. 修改 IR-v0.8-002，runner 不通过 adapter 采样，直接使用 `executor.toSnapshot()`。

**理由**: `ManagedExecutorAdjustmentAdapter.currentState()` 的核心逻辑就是 `executor.toSnapshot()`（见 v0.7.0 `ManagedExecutorAdjustmentAdapter.java:currentState()` 实现）。Runner 只采样不调整，完全不需要 adapter 的 `apply()` 路径。绕过 adapter 直接采样：
- 简化代码（不需要创建 adapter、safety gate、readiness assessment）
- 消除 F09 的 API 缺口
- runner 和 adapter 职责更清晰

**动作**: 修改 IR-v0.8-002 Phase 2 和 Phase 4 的措辞：

> Phase 2（旧）: 创建 `ManagedExecutorAdjustmentAdapter`（使用 permissive safety gate + READY readiness）
> Phase 2（新）: 不创建 adapter。Runner 通过 `executor.toSnapshot()` 直接读取 TPE 状态。

> Phase 4（旧）: 通过 `adapter.currentState()` 读取真实 TPE 状态
> Phase 4（新）: 通过 `executor.toSnapshot()` 读取真实 TPE 状态

**状态**: `FIXED` — 将在 `10-ir.md` 中直接修正。

---

### F10 [P2] — Step 间空闲状态判断标准未定义

**处置**: `DEFER_TO_SR`. IR 层面已表达了"等待任务完成"的意图。具体判断标准是实现细节。

**SR 触发**: SR 定义空闲条件。推荐：`executor.getQueueSize() == 0 && executor.getActiveCount() == 0`。需考虑伪唤醒（spurious wakeup from polling loop）和超时保护。

**状态**: `DEFERRED_TO_SR`.

---

## 3. 处置汇总

| Finding | 级别 | 处置 | 目标 |
|---|---|---|---|
| F01 | P0 | FIX — 增补 IR-v0.8-002 registry 生命周期条款 | 直接修正 `10-ir.md` |
| F02 | P0 | FIX — 新增 IR-v0.8-007 Runner-to-Report Bridge | 追加到 `10-ir.md` |
| F03 | P0 | FIX — 增补 G7 extendedFieldPresence 数据结构定义 | 直接修正 `10-ir.md` |
| F04 | P1 | DEFER_TO_SR — 同步屏障选择为实现决策 | SR 风险记录 |
| F05 | P1 | FIX — G8 改为 per-profile 判定 | 直接修正 `10-ir.md` |
| F06 | P1 | DEFER_TO_SR — 依赖方向由 SR 裁决 | SR 架构记录 |
| F07 | P1 | DEFER_TO_SR — policyId 语义偏移决策 | SR decision log |
| F08 | P1 | FIX — 增补 RAMP capping 语义 | 直接修正 `10-ir.md` |
| F09 | P2 | FIX — Runner 直接使用 executor.toSnapshot() | 直接修正 `10-ir.md` |
| F10 | P2 | DEFER_TO_SR — 空闲条件由 SR 定义 | SR |

## 4. IR 修正清单

以下修改需在 `10-ir.md` 中执行（由 `13-ir-closure-verification.md` 验证）：

1. IR-v0.8-002 增补：`ExecutorRegistry` 和 `AtomicDeletionSafety` 由 runner 内部创建。
2. IR-v0.8-002 修改：Phase 2 和 Phase 4 使用 `executor.toSnapshot()` 替代 `adapter.currentState()`（F09 处置）。
3. IR-v0.8-002 增补：RAMP capping 行为定义（F08 处置）。
4. IR-v0.8-006 增补：`extendedFieldPresence` 数据结构（F03 处置）。
5. IR-v0.8-006 修改：G8 改为 per-profile 判定（F05 处置）。
6. 新增 IR-v0.8-007：Runner-to-Report Bridge（F02 处置）。

## 5. 结论

3 个 P0 阻断项全部通过 FIX 处置（直接修改 IR 正文 + 新增 1 条 IR）。5 个 P1 项中 2 个 FIX、3 个 DEFER_TO_SR（均附风险记录和 SR 触发条件）。2 个 P2 项中 1 个 FIX、1 个 DEFER_TO_SR。IR 修正后即可进入 closure verification。
