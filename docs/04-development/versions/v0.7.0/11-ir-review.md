# v0.7.0 IR 独立评审报告

## Header

- Document type: IR independent review
- Version name: `v0.7.0`
- Reviewed artifact: `docs/04-development/versions/v0.7.0/10-ir.md`
- Review date: `2026-06-11`
- Reviewer role: 独立 IR review（非 IR 作者）
- Review basis: `docs/02-harness/managed-change-standard.md` 第 2 节（IR 需求分析）

## 1. 评审输入

已读取以下文件作为评审上下文：

- `docs/00-project/current-state.md`
- `docs/02-harness/managed-change-standard.md`
- `docs/01-architecture/managed-executor-domain-model.md`
- `docs/04-development/versions/v0.7.0/README.md`
- `docs/04-development/versions/v0.7.0/00-objectives-and-scope.md`
- `docs/04-development/versions/v0.7.0/10-ir.md`
- `docs/04-development/versions/v0.7.0/decision-log.md`
- `openspec/specs/executor-adapter-and-adjustment-evidence/spec.md`
- `openspec/specs/offline-replay-and-readiness-gate/spec.md`
- `openspec/specs/adaptive-policy-and-control-gate/spec.md`

## 2. 评审摘要

IR 草案整体质量良好：10 条需求覆盖了从 `ManagedExecutor` 抽象到闭环验证的完整链路，范围和约束边界清晰，非范围条款明确。但存在 2 个 P0 阻断项和 5 个 P1 关键项需要处置，主要集中在生命周期缺失、语义歧义和集成间隙三个方面。

## 3. Findings

### F01 [P0] ManagedExecutor 生命周期缺失 — shutdown / termination

**位置**: IR-v0.7-001

**问题**: IR-v0.7-001 覆盖了构造、参数读写、状态读取和任务提交。但 `ManagedExecutor` 包装的是 `ThreadPoolExecutor`（实现 `ExecutorService`），其生命周期方法完全未被提及：

- `shutdown()` — 有序关闭
- `shutdownNow()` — 立即中断
- `isShutdown()` / `isTerminated()` — 状态查询
- `awaitTermination(long, TimeUnit)` — 阻塞等待终止

AC-v0.7-009 的测试覆盖列表中提到了 "shutdown"，但 IR 正文从未将生命周期纳入需求。实验有明确的起点和终点：闭环实验结束后，执行器线程必须能被安全终止，否则造成资源泄漏。

**影响**: SR 设计者可能遗漏生命周期设计，导致 `ManagedExecutor` 成为一个只创建不销毁的抽象。`ExecutorRegistry.remove()` 是否触发 shutdown 也是 F02 的关联问题。

**建议**: IR-v0.7-001 增补 shutdown/termination 需求条目，至少覆盖 `shutdown()` 和 `isTerminated()`。

---

### F02 [P0] ExecutorRegistry.remove() 与 executor shutdown 解耦 — 资源泄漏

**位置**: IR-v0.7-002

**问题**: IR-v0.7-002 定义了 `remove(name)` 操作，要求删除前通过 `DeletionSafety` 检查。但未定义删除后 `ManagedExecutor` 的线程如何处理。两个可能路径都有问题：

- 路径 A：`remove()` 只从注册表移除引用，不调用 `shutdown()` → 执行器线程继续运行，无人引用也无人停止 → 资源泄漏。
- 路径 B：`remove()` 自动调用 `shutdown()` → 与 `DeletionSafety` 的引用计数语义产生冲突（引用计数降到 0 后自动 shutdown 还是手动 shutdown？）。

**影响**: 不明确的生命周期耦合会导致 SR 设计歧义，最坏情况下测试线程泄漏导致 CI 超时。

**建议**: IR 或 SR 必须明确 `remove()` 与 `shutdown()` 的关系。推荐方案：`remove()` 不自动 shutdown；调用者负责先 shutdown 再 remove；`DeletionSafety` 检查 `isTerminated()` 或 `isShutdown()` 作为额外条件。

---

### F03 [P1] 调整后快照语义：参数变更 vs 运行时效果

**位置**: IR-v0.7-005, IR-v0.7-008

**问题**: IR-v0.7-005 要求"调整后必须立即采集 `ExecutorStateSnapshot`"。但 `ThreadPoolExecutor.setCorePoolSize(n)` 改变的是目标值，核心线程按需创建。若调整后没有新任务到达，`getPoolSize()` 返回值可能不变。IR-v0.7-008 又要求"调整前后 poolSize 必须有可验证的变化"——这两个语句存在时序语义冲突。

```java
executor.setCorePoolSize(8);               // 目标变为 8
snapshot = collect(executor);               // getPoolSize() 可能仍是旧值
assert snapshot.poolSize() == 8;            // 可能失败
```

**影响**: AC-v0.7-008 的可验证性依赖于对 `ThreadPoolExecutor` 延迟线程创建的误解。闭实验验收可能因非代码缺陷而失败。

**建议**: SR 必须精确定义"调整后快照"的采集时机和预期语义。建议区分"parameter snapshot"（目标参数）和"runtime snapshot"（实际状态），并允许 `poolSize` 在目标值和实际值之间存在合理偏差。

---

### F04 [P1] 闭环实验可复现性声明过强

**位置**: IR-v0.7-008

**问题**: IR 要求"实验必须在确定性可控环境下可复现（相同输入 → 相同调整结果）"。但 `ThreadPoolExecutor` 的线程调度受 JVM、OS 和时间影响，完全相同的输入不会产生完全相同的 `activeCount`、`poolSize`、`queueSize` 时序值。

"相同调整结果"需要被限定：是"相同的调整决策"（policy 给定相同输入产出相同 decision），还是"相同的执行后状态"？前者可实现（纯计算路径），后者不可保证（线程调度非确定性）。

**影响**: 若严格按字面验收，AC-v0.7-008 永远无法通过。这不是实现缺陷，而是需求定义过度。

**建议**: 将可复现性缩小为"相同输入 → 相同 `ScaleDecision` 和调整参数"，明确排除 timing-dependent 的状态值。

---

### F05 [P1] ManagedExecutor 与 BaselineWorkloadExecutor 交互未定义

**位置**: IR-v0.7-001, IR-v0.7-008

**问题**: 现有 scenario runner（v0.3.0）使用 `BaselineWorkloadExecutor` 和 `ScenarioExperimentRunner`，底层是 `InMemoryAdjustableExecutorProbe`。IR 未说明 `ManagedExecutor` 与这些组件的关系：

- `ManagedExecutor` 是否替代 `BaselineWorkloadExecutor`？
- 还是 `BaselineWorkloadExecutor` 适配为委托给 `ManagedExecutor`？
- `ScenarioExperimentRunner` 直接使用 `ManagedExecutor` 还是通过中间层？

IR-v0.7-008 的闭环实验步骤说"部署 ManagedExecutor + 场景"，但没有引用任何现有 scenario 组件。

**影响**: SR 设计者不知道集成点在哪里，可能设计出与现有 scenario 框架不兼容的接口。

**建议**: IR 或 SR 必须绘制 `ManagedExecutor` → `ScenarioExperimentRunner` → `BaselineWorkloadExecutor` 的关系图。推荐：`ManagedExecutor` 实现 `BaselineWorkloadExecutor` 接口或作为其委托目标。

---

### F06 [P1] 与现有 metrics 管道（SnapshotAssembler / PressureSampler）集成缺失

**位置**: IR-v0.7-006

**问题**: v0.2.0 已有 `SnapshotAssembler`、`PressureSampler`、`ObservedSnapshot` 等指标组件。IR-v0.7-006 定义了 `ExecutorStateSnapshot` 从 `ManagedExecutor` 采集，但未说明：

- 新的快照是否通过 `SnapshotAssembler` 进入现有管道？
- `PressureSampler` 是否需要新的数据源？
- 现有 `ObservedSnapshot` 的字段集是否兼容真实线程池状态？

**影响**: SR 设计者可能创建与现有管道并行的新采集路径，破坏 v0.2.0 建立的 append-only recording 和 summary 约定。

**建议**: SR 必须明确 `ExecutorStateSnapshot` 与 `ObservedSnapshot` / `SnapshotAssembler` 的关系。推荐：`ExecutorStateSnapshot` 作为 `ObservedSnapshot` 的扩展或新字段源。

---

### F07 [P1] 引用计数的线程安全未定义

**位置**: IR-v0.7-004

**问题**: IR-v0.7-004 要求"引用计数不得为负数"，但未指定并发语义。`acquire()` 和 `release()` 可能从 `ExperimentCoordinator` 和 `ScenarioExperimentRunner` 的不同线程调用。当前草案只说"引用计数语义"，未要求原子操作。

**影响**: 非原子的引用计数在并发场景下可能 double-release 或计数错误，导致 `DeletionSafety` 误判。

**建议**: IR-v0.7-004 明确要求 `acquire()` / `release()` 使用原子操作（如 `AtomicInteger` 或等效机制）。

---

### F08 [P2] Thread factory 和 rejection policy 默认值未指定

**位置**: IR-v0.7-001

**问题**: `ManagedExecutor` 构造参数中 thread factory 和 rejection policy 为"可选"，但未指定默认值。`ThreadPoolExecutor` 的标准默认是 `Executors.defaultThreadFactory()` 和 `AbortPolicy`。

**影响**: 不影响 SR 推进，但 SR 需要明确记录默认值。

**建议**: SR 阶段记录默认值。不阻塞 IR closure。

---

### F09 [P2] 调整失败后无回滚定义

**位置**: IR-v0.7-005

**问题**: IR-v0.7-005 定义了调整失败返回 `FAILED` 状态。但未覆盖部分成功场景：若 `setCorePoolSize` 成功而 `setMaximumPoolSize` 失败，执行器处于部分调整状态。是否需要回滚到调整前参数？

**影响**: 当前闭环实验只需单参数调整，部分成功概率低。可在 SR 阶段作为 edge case 处理。

**建议**: SR 记录此边界情况，当前 P2 可接受。

---

### F10 [P2] 两个 ExecutorAdjustmentAdapter 实现的选择策略未定义

**位置**: IR-v0.7-010

**问题**: `InMemoryAdjustableExecutorProbe` 和 `ManagedExecutorAdjustmentAdapter` 共存时，调用方如何知道使用哪一个？IR 未定义工厂、DI 标记或策略选择。

**影响**: 不影响 IR 语义，但 SR 需要明确。P2 可接受。

**建议**: SR 阶段定义 adapter selection mechanism（推荐基于 Spring `@Qualifier` 或显式工厂）。

---

### F11 [P2] ExecutorStateSnapshot 与现有类"字段语义兼容"未展开

**位置**: IR-v0.7-006

**问题**: IR 要求"必须与现有 `ExecutorStateSnapshot` 的字段语义兼容"，但未列出哪些现有字段保留、哪些新增。现有快照类来自 v0.5.0，可能包含探针特有的虚构字段。

**影响**: SR 设计者可能面临字段映射歧义。不阻塞 IR closure。

**建议**: SR 阶段逐字段对比现有 `ExecutorStateSnapshot`，记录保留/弃用/新增决策。

---

## 4. Findings 汇总

| ID | 位置 | 描述 | 级别 | 建议动作 |
| --- | --- | --- | --- | --- |
| F01 | IR-v0.7-001 | `ManagedExecutor` 缺少 shutdown/termination 生命周期需求 | P0 | 增补 IR 条目 |
| F02 | IR-v0.7-002 | `remove()` 与 `shutdown()` 解耦导致资源泄漏风险 | P0 | 明确生命周期耦合 |
| F03 | IR-v0.7-005 | 调整后快照语义混淆参数变更与运行时效果 | P1 | SR 精确定义时序 |
| F04 | IR-v0.7-008 | 闭环可复现性声明过强，与线程调度非确定性冲突 | P1 | 缩小可复现性定义 |
| F05 | IR-v0.7-001 | `ManagedExecutor` 与 `BaselineWorkloadExecutor` 交互未定义 | P1 | 明确集成关系 |
| F06 | IR-v0.7-006 | 与现有 metrics 管道集成缺失 | P1 | 明确与 SnapshotAssembler 关系 |
| F07 | IR-v0.7-004 | 引用计数并发安全未要求原子操作 | P1 | 增补原子性要求 |
| F08 | IR-v0.7-001 | Thread factory / rejection policy 默认值未指定 | P2 | SR 记录，不阻塞 |
| F09 | IR-v0.7-005 | 调整失败后无回滚语义 | P2 | SR 记录 edge case |
| F10 | IR-v0.7-010 | Adapter 选择策略未定义 | P2 | SR 定义，不阻塞 |
| F11 | IR-v0.7-006 | 与现有快照类的字段兼容性未展开 | P2 | SR 逐字段对比 |

## 5. 正向检查通过项

- [x] IR 不隐含实现授权——各条目使用"候选验收语义"和"必须定义"措辞，未声称已实现。
- [x] scope 边界明确排除 queue resizing、闭环调度、生产环境。
- [x] 非范围列表与 `00-objectives-and-scope.md` 和 `decision-log.md` 一致。
- [x] 安全门集成指向 v0.5.0 的 `RuntimeAdjustmentSafetyGate` 和 `MutationReadinessGate`，未创建新门。
- [x] 探针降级策略明确保留而非删除 `InMemoryAdjustableExecutorProbe`。
- [x] IR 与架构文档 `managed-executor-domain-model.md` 的域模型一致。
- [x] 术语表覆盖了所有关键概念。
- [x] 风险和延期项表覆盖了主要的 P0/P1 风险。
- [x] 出口条件清单完整，与 `managed-change-standard.md` 第 2 节一致。

## 6. 评审结论

IR 草案在结构性、范围控制和架构对齐方面**合格**。10 条需求覆盖了从抽象到验证的完整链路。但**不能直接进入 SR**：存在 2 个 P0 阻断项（F01 生命周期缺失、F02 remove/shutdown 解耦）和 5 个 P1 关键项。P0/P1 必须通过 disposition 关闭。

评审建议：**进入 IR disposition（`12-ir-review-disposition.md`）**，逐项处置 F01-F11。
