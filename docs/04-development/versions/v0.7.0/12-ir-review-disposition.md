# v0.7.0 IR Review Disposition

## Header

- Document type: IR review disposition
- Version name: `v0.7.0`
- Reviewed artifact: `docs/04-development/versions/v0.7.0/11-ir-review.md`
- Disposition date: `2026-06-11`
- Disposition basis: `docs/02-harness/managed-change-standard.md` 第 2 节，严重级别规则

## 1. 处置规则

| 级别 | 处置方式 |
| --- | --- |
| P0 | 必须修复并闭环，不能进入 SR |
| P1 | 必须修复或明确 defer 到 SR 并记录风险 |
| P2 | 可修复或记录残余风险，需说明后续触发条件 |

## 2. 逐项处置

### F01 [P0] — ManagedExecutor 生命周期缺失

**处置**: **FIX**. 直接在 IR-v0.7-001 增补生命周期需求条目。

**动作**: 在 IR-v0.7-001 "候选验收语义" 中增补以下条款：

> - `ManagedExecutor` 必须提供 `shutdown()` 方法，委托给底层 `ThreadPoolExecutor.shutdown()`，发起有序关闭。
> - 必须提供 `isShutdown()` 和 `isTerminated()` 查询方法。
> - 必须提供 `awaitTermination(long timeout, TimeUnit)` 方法。
> - 实验结束时，调用方负责调用 `shutdown()` 并等待终止。

**状态**: `FIXED` — 将在 `10-ir.md` 中直接修正，由 `13-ir-closure-verification.md` 验证。

---

### F02 [P0] — ExecutorRegistry.remove() 与 shutdown 解耦

**处置**: **FIX**. 在 IR-v0.7-002 增补生命周期耦合条款。

**动作**: 在 IR-v0.7-002 "候选验收语义" 中增补以下条款：

> - `remove(name)` 不自动 shutdown 执行器。调用者必须确保执行器已终止（`isTerminated() == true`）后再调用 `remove()`。
> - `DeletionSafety.canRemove(name)` 必须额外检查：若执行器未终止（`isTerminated() == false`），返回 `false` 并记录未终止原因。

**状态**: `FIXED` — 将在 `10-ir.md` 中直接修正。

---

### F03 [P1] — 调整后快照语义混淆

**处置**: `DEFER_TO_SR`. IR 层面已足够清晰地表达了"采集调整后状态"的意图。`ThreadPoolExecutor` 的延迟线程创建是一个实现细节，应在 SR 中设计精确的采集时机和验证策略。

**理由**: IR-v0.6.0 的风险表第 1 行已记录"线程调度不确定性影响可复现性"为 P1 风险，触发条件为"SR 必须确认 deterministic time bounds 或固定 seed 策略"。F03 属于同一风险域，IR 阶段不必解决实现层时序问题。

**SR 触发**: SR 必须定义：pre-adjustment snapshot / post-adjustment snapshot 的采集时机、允许的 poolSize 偏差范围、以及是否需要等待任务到达来触发核心线程创建。

**状态**: `DEFERRED_TO_SR` with risk record.

---

### F04 [P1] — 闭环实验可复现性声明过强

**处置**: **FIX**. 修正 IR-v0.7-008 的可复现性措辞。

**动作**: 将 IR-v0.7-008 的验收条款从：

> 实验必须在确定性可控环境下可复现（相同输入 → 相同调整结果）

改为：

> 实验的策略决策路径必须可复现（相同 scenario / policy config / baseline preset → 相同 `ScaleDecision` 和调整参数）。timing-dependent 的运行时状态值（`activeCount`、`poolSize`、`queueSize` 时序）允许在合理范围内因线程调度而产生波动，不作为严格复现判定依据。

**状态**: `FIXED` — 将在 `10-ir.md` 中直接修正。

---

### F05 [P1] — ManagedExecutor 与 BaselineWorkloadExecutor 交互未定义

**处置**: `DEFER_TO_SR`. 这是设计层面的集成问题，不是需求缺失。

**理由**: IR 的职责是定义"需要什么"，不是"如何连接"。`ManagedExecutor` 已经定义了 `submit()` 方法（IR-v0.7-001），这就是它可以作为任务执行目标的充分需求声明。如何与 `ScenarioExperimentRunner` 装配属于 SR 设计。

**SR 触发**: SR 必须定义 `ManagedExecutor` 在 scenario execution 中的角色：是实现 `BaselineWorkloadExecutor` 接口，还是作为独立组件被 `ScenarioExperimentRunner` 直接使用。

**状态**: `DEFERRED_TO_SR` with design requirement.

---

### F06 [P1] — 与现有 metrics 管道集成缺失

**处置**: `DEFER_TO_SR`. 需求已定义了 `ExecutorStateSnapshot` 的字段集（IR-v0.7-006），这足以作为 metrics 管道的输入定义。如何组装到 `SnapshotAssembler` 是 SR 设计问题。

**SR 触发**: SR 必须定义 `ExecutorStateSnapshot` 与 `ObservedSnapshot` / `SnapshotAssembler` 的关系：是扩展、组合还是并行路径。

**状态**: `DEFERRED_TO_SR` with design requirement.

---

### F07 [P1] — 引用计数并发安全未定义

**处置**: **FIX**. 在 IR-v0.7-004 中增补原子性要求。

**动作**: 将 IR-v0.7-004 中的：

> 引用计数不得为负数。

改为：

> 引用计数必须使用原子操作（`AtomicInteger` 或等效机制），`acquire()` / `release()` 必须线程安全，引用计数不得为负数。

**状态**: `FIXED` — 将在 `10-ir.md` 中直接修正。

---

### F08 [P2] — Thread factory / rejection policy 默认值未指定

**处置**: `ACCEPTED`. 记录为 SR 备忘。不影响 IR closure。

**SR 备忘**: 默认 thread factory = `Executors.defaultThreadFactory()`，默认 rejection policy = `AbortPolicy`。

**状态**: `ACCEPTED_WITH_RECORD`.

---

### F09 [P2] — 调整失败后无回滚定义

**处置**: `ACCEPTED`. v0.7.0 闭环实验仅做单参数调整，部分成功概率极低。SR 阶段记录 edge case。

**SR 备忘**: 若 SR 设计多参数调整（同时设置 core + max），需记录部分成功场景和回滚策略。

**状态**: `ACCEPTED_WITH_RECORD`.

---

### F10 [P2] — Adapter 选择策略未定义

**处置**: `ACCEPTED`. SR 阶段通过 DI 或工厂解决。

**SR 备忘**: 推荐使用 Spring `@Qualifier("managedExecutor")` / `@Qualifier("probe")` 区分两个 `ExecutorAdjustmentAdapter` 实现。

**状态**: `ACCEPTED_WITH_RECORD`.

---

### F11 [P2] — 与现有快照类的字段兼容性未展开

**处置**: `ACCEPTED`. SR 阶段逐字段对比。

**SR 备忘**: SR 必须列出 `ExecutorStateSnapshot`（v0.5.0）与新的真实执行器快照之间的字段映射：保留/弃用/新增。

**状态**: `ACCEPTED_WITH_RECORD`.

## 3. 处置汇总

| ID | 级别 | 处置 | 动作 |
| --- | --- | --- | --- |
| F01 | P0 | FIXED | IR-v0.7-001 增补生命周期条款 |
| F02 | P0 | FIXED | IR-v0.7-002 增补 shutdown 前置条件 |
| F03 | P1 | DEFERRED_TO_SR | SR 定义采集时序 |
| F04 | P1 | FIXED | IR-v0.7-008 缩小可复现性定义 |
| F05 | P1 | DEFERRED_TO_SR | SR 定义集成点 |
| F06 | P1 | DEFERRED_TO_SR | SR 定义 metrics 管道集成 |
| F07 | P1 | FIXED | IR-v0.7-004 增补原子性要求 |
| F08 | P2 | ACCEPTED_WITH_RECORD | SR 记录默认值 |
| F09 | P2 | ACCEPTED_WITH_RECORD | SR 记录 edge case |
| F10 | P2 | ACCEPTED_WITH_RECORD | SR 定义选择策略 |
| F11 | P2 | ACCEPTED_WITH_RECORD | SR 逐字段对比 |

## 4. 处置后状态

- P0 findings: 2 / 2 FIXED.
- P1 findings: 2 / 5 FIXED, 3 / 5 DEFERRED_TO_SR with design requirements.
- P2 findings: 4 / 4 ACCEPTED_WITH_RECORD.

所有 P0/P1 均已处置。SR 携带 3 个 P1 deferred design requirements 和 4 个 P2 memo items。

## 5. 处置结论

所有 P0 阻断项已通过 IR 修正消除。所有 P1 项已处置（修正或明确延期到 SR 并记录触发条件）。P2 项已记录为 SR 备忘。**处置完成，进入 IR closure verification（`13-ir-closure-verification.md`）。**
