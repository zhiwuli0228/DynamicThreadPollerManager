# v0.9.0 IR 独立评审报告

## Header

- Document type: IR independent review
- Version name: `v0.9.0`
- Reviewed artifact: `docs/04-development/versions/v0.9.0/10-ir.md`
- Review date: `2026-06-13`
- Reviewer role: 独立 IR review（非 IR 作者）
- Review basis: `docs/02-harness/managed-change-standard.md` 第 2 节（IR 需求分析）

## 1. 评审输入

已读取以下文件作为评审上下文：

- `docs/00-project/current-state.md`
- `docs/02-harness/managed-change-standard.md`
- `docs/01-architecture/operational-and-evolution-boundaries.md`
- `docs/05-domain/exploration-boundaries.md`
- `docs/04-development/versions/v0.9.0/README.md`
- `docs/04-development/versions/v0.9.0/00-objectives-and-scope.md`
- `docs/04-development/versions/v0.9.0/10-ir.md`
- `docs/04-development/versions/v0.9.0/decision-log.md`
- `docs/04-development/versions/v0.5.0/decision-log.md`
- `docs/08-retrospectives/2026-06-13-v0.8.0-real-data-acquisition-retrospective.md`
- `openspec/specs/establish-managed-executor-and-registry/spec.md`
- `openspec/specs/bridge-adjustment-to-real-executor/spec.md`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/executor/ManagedExecutorAdjustmentAdapter.java`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/executor/ExecutorRegistry.java`

## 2. 评审摘要

IR 草案结构完整：6 条需求覆盖了从 `QueueResizeCommand` 到端到端 resize 验证的完整链路，范围边界清晰（明确排除 reflection hack、closed-loop、多执行器协调），技术挑战认识到位（`ThreadPoolExecutor` 不支持 queue 替换 → ExecutorRebuildStrategy）。但存在 2 个 P0 阻断项和 4 个 P1 关键项需要处置，主要集中在 drain-and-replay 语义不完整、adapter 扩展方式未决定、registry 替换注册细节缺失、以及 resize evidence 与现有 evidence 体系的关系未明确。

## 3. Findings

### F01 [P0] Drain-and-replay 的原子性语义未定义

**位置**: IR-v0.9-002

**问题**: IR-v0.9-002 要求 decommission 阶段 drain 旧 queue 中的等待任务，commission 阶段在新 executor 中 replay。但 drain 和 replay 之间的原子性未定义：

- drain 出的 `List<Runnable>` 是旧任务引用——这些任务可能已经过期或状态不一致
- 若 drain 后、replay 前发生异常（如新 TPE 创建失败），drained 任务丢失
- 若 resize 方向为 SHRINK 且 drained 任务数 > 新 queue capacity，replay 会失败——IR 说"记录为 rejectedTaskCount"，但未说明调用方如何感知

当前 IR 对这些失败模式只有散点描述，缺少统一的原子性契约。

**影响**: SR 可能设计出部分 drain、部分 replay 的半完成状态。端到端测试无法覆盖所有边界情况。

**建议**: IR-v0.9-002 补充：
1. Drain 是 shallow copy（只移出 queue reference），旧 TPE 关闭后任务引用仍然有效
2. 若新 TPE 创建失败（commission 阶段异常），rebuild 整体失败，返回 failure result + 旧 executor 保持运行（或已 shutdown，记录为 partial failure）
3. SHRINK + drained > newCapacity：replay 时逐任务调用 `newExecutor.submit()`，若抛 `RejectedExecutionException` 则计数并在 evidence 中记录

---

### F02 [P0] Adapter 扩展方式未决定：修改现有 vs 新建

**位置**: IR-v0.9-004

**问题**: IR-v0.9-004 标题写"扩展现有 ManagedExecutorAdjustmentAdapter 或新增 adapter"，但整条 IR 的候选验收语义在两种策略之间摇摆：

- "新增 `apply(QueueResizeCommand)` 方法（或在同一个 adapter 中，或在新 adapter 中）"
- "不得移除现有的 `ScaleAdjustmentCommand` 支持"

如果新增到同一个 adapter：
- `ManagedExecutorAdjustmentAdapter` 同时处理 `ScaleAdjustmentCommand` 和 `QueueResizeCommand`，adapter 的职责从"线程数调整"膨胀为"所有 executor 调整"
- `apply()` 方法需要 overload 或泛型化，调用方需要知道传哪种 command

如果新建 adapter：
- 需要新类 `QueueResizeAdjustmentAdapter`，可能与现有 adapter 共享 `ExecutorRegistry` 依赖
- 两个 adapter 各自独立，测试隔离性更好

这个决策影响 SR 的模块边界设计、类命名和测试策略。

**影响**: SR 可能产出两种不同的 adapter 设计，返工成本高。

**建议**: IR 或 SR 早期明确选择。推荐**新建 `QueueResizeAdjustmentAdapter`**（遵循 v0.8.0 D1 的 "新建不修改" 原则，且 Scale 和 Resize 操作不同对象维度）。

---

### F03 [P1] Registry 替换注册的 executorId 语义未定义

**位置**: IR-v0.9-002

**问题**: IR-v0.9-002 Commission 阶段说"在 ExecutorRegistry 中替换注册（remove old → register new，保持同一 executorId，或分配新 ID 并在 evidence 中记录映射）"。但 executorId 的语义对调用方很重要：

- 若保持同一 executorId：registry 的 `get(executorId)` 在 rebuild 前后返回不同对象——调用方持有的旧 reference 会失效
- 若分配新 ID：调用方需要感知 ID 变更，否则后续操作（如再次 resize）找不到 executor
- `ExecutorRegistry` 当前使用 `ConcurrentHashMap<String, ManagedExecutor>`，替换注册是原子操作——但旧 executor 的 shutdown/termination 状态需要与 registry 状态一致

当前 IR 只说"两种选择都行"，未给出倾向。

**影响**: SR 在 registry 设计和端到端测试编排上缺乏明确输入。

**建议**: 推荐**保持同一 executorId**——rebuild 的语义是"同一个逻辑 executor 的配置变更"，不是"新建一个 executor"。旧 executor reference 失效是可接受的行为（rebuild 完成后旧 executor 已 terminated，任何对它的操作都会失败）。SR 确认后写入 API 设计。

---

### F04 [P1] ResizeEvidence 与现有 EvidenceRecorder 的关系未定义

**位置**: IR-v0.9-005

**问题**: IR-v0.9-005 要求 ResizeEvidence "可通过 EvidenceRecorder 或等效机制记录"。但 `EvidenceRecorder` 当前只处理 `ObservedSnapshot`（v0.2.0 接口：`record(ObservedSnapshot)`），不处理 resize evidence。

选择：
- 扩展 `EvidenceRecorder` 接口新增 `recordResize(ResizeEvidence)` 方法
- 新建独立 recorder（`ResizeEvidenceRecorder`）
- 在 `AdjustmentResult` 中直接携带 `ResizeEvidence`，不经过 recorder

当前 IR 未给出倾向，三种方案各有 tradeoff。

**影响**: SR 需要决定 evidence 的持久化路径。

**建议**: 推荐在 `AdjustmentResult` 中直接携带 `ResizeEvidence`（方案 C），与现有 `AdjustmentResult.evidence()` 模式一致。`EvidenceRecorder` 扩展留给后续版本（当需要跨 resize 事件聚合分析时）。

---

### F05 [P1] SHRINK resize 时旧 queue 中有积压任务的处置不完整

**位置**: IR-v0.9-002, IR-v0.9-003

**问题**: IR-v0.9-003 Safety Gate 说"若 resizeDirection == SHRINK 且 queue 深度 > 新容量，必须 DENY"。但未覆盖以下场景：

- Queue 深度 = 3，新容量 = 5 → SHRINK（从 10 → 5），queue 深度 3 < 5 → safety gate PERMIT
- Drain 后 3 个任务 replay 到新 queue（capacity=5），3 < 5 → 全部成功 ✓
- 但 drain 过程中可能有新任务被提交到旧 executor——这些任务在 drain 和 shutdown 之间进入 queue

Race condition：drain → 新任务提交到旧 queue → shutdown。如果新任务在 drain 之后、shutdown 之前入队，它们不会被 drain，从而在 shutdown 时被丢弃（取决于 RejectedExecutionHandler）。

**影响**: 高并发场景下可能出现任务静默丢失。当前单线程测试环境不会触发，但 SR 设计需要考虑。

**建议**: IR-v0.9-002 的 decommission 流程增加 "stop accepting new tasks" 步骤（通过 registry 标记或 AtomicBoolean 门控），在 drain 之前先阻止新提交。或者明确声明 resize 操作期间的并发提交由调用方负责协调。

---

### F06 [P1] AC 对应关系有缺口

**位置**: IR section 4（验收条件）

**问题**: 验收条件表有 15 个 AC，但：
- AC-v0.9-011（ResizeEvidence）标记为 P1，但没有对应的 P0 AC 覆盖"resize 操作一定产出 evidence"这个基本要求
- AC-v0.9-008（Safety gate DENY 非 RUNNING 状态）标记为 P1，但 executor 状态检查是 safety gate 的基本功能——应该至少有一个 P0 AC 覆盖"executor 状态检查"
- G10 resize gate 在 00-objectives-and-scope.md section 8 中提到，但 IR 中没有对应的 IR 条目或 AC

**影响**: 验收条件覆盖不完整可能导致端到端测试遗漏关键路径。

**建议**: 
1. 将 AC-v0.9-008 提升为 P0（executor 状态检查是 safety gate 的必需项）
2. 新增 AC-v0.9-011a [P0]：每次 resize 调用必须产出 ResizeEvidence（非空）
3. G10 resize gate 明确为 DEFER_TO_SR（SR 决定是否需要，若需要则 SR 阶段定义）

---

### F07 [P2] ThreadPoolExecutor 线程配置在 rebuild 期间的读取时机

**位置**: IR-v0.9-002 Commission

**问题**: IR-v0.9-002 要求"用旧 executor 的 corePoolSize、maximumPoolSize、keepAliveTime、threadFactory 创建新的 ThreadPoolExecutor"。但旧 executor 的这些值在 decommission 过程中可能被其他线程的 `ScaleAdjustmentCommand` 修改。

例如：rebuild 开始 → 读取 corePoolSize=2 → 另一个线程执行 ScaleAdjustmentCommand(8) → decommission 使用 core=8 → commission 新 TPE 使用 core=8。这不是 bug（最新的值是正确的），但需要明确：**commission 使用的是 decommission 开始时的快照值还是最新值**。

**影响**: 低。当前系统单线程执行 adjust，并发修改不会发生。但 SR 应记录"读取时机"的语义。

**建议**: 明确 Commission 使用 drain 之前（decommission 入口处）读取的 snapshot 值。这些值在 `beforeState: ExecutorStateSnapshot` 中已记录，直接复用保证一致性。

---

### F08 [P2] 术语 "drain-and-discard" 未出现在 IR 中但可能有用

**位置**: IR-v0.9-002

**问题**: `00-objectives-and-scope.md` 7.2 节提到三种策略：drain-and-replay, drain-and-discard, graceful-drain。但 IR-v0.9-002 只覆盖了 drain-and-replay。SHRINK 场景下"drain-and-discard"（排空并丢弃旧任务）可能是更安全的选择——避免新 queue 容量不足导致的 replay 失败。

**影响**: 低。Drain-and-replay 是更通用的策略，SHRINK 时的 rejection 有明确记录（rejectedTaskCount）。但 SR 可以评估是否在 SHRINK 时默认使用 drain-and-discard 以减少失败路径。

**建议**: SR 阶段讨论是否将 drain-and-discard 作为 SHRINK 的默认策略，或作为 QueueResizeCommand 的可选 flag（`discardOnShrink: boolean`）。

---

## 4. Findings 汇总

| ID | 位置 | 描述 | 级别 | 建议动作 |
|---|---|---|---|---|
| F01 | IR-v0.9-002 | Drain-and-replay 原子性语义未定义 | P0 | 补充 drain→replay 原子性契约 |
| F02 | IR-v0.9-004 | Adapter 扩展方式未决定（修改 vs 新建） | P0 | 明确选择新建 QueueResizeAdjustmentAdapter |
| F03 | IR-v0.9-002 | Registry 替换注册的 executorId 语义 | P1 | 推荐保持同一 executorId |
| F04 | IR-v0.9-005 | ResizeEvidence 与 EvidenceRecorder 的关系 | P1 | 推荐 AdjustmentResult 直接携带 |
| F05 | IR-v0.9-002/003 | SHRINK resize 时 drain→shutdown 间隙 race | P1 | 补充 "stop accepting" 步骤或声明调用方协调 |
| F06 | IR section 4 | AC 对应关系缺口（G10, executor state, evidence） | P1 | 调整 AC 优先级，补充 missing AC |
| F07 | IR-v0.9-002 | 线程配置读取时机（snapshot vs latest） | P2 | SR 记录语义 |
| F08 | IR-v0.9-002 | Drain-and-discard 策略未探索 | P2 | SR 阶段评估 |

## 5. 正向检查通过项

- [x] IR 不隐含实现授权——各条目使用"候选验收语义"措辞，未声称已实现
- [x] Scope 边界明确排除 reflection hack、rejection policy、closed-loop、多执行器协调
- [x] 非范围列表与 `00-objectives-and-scope.md` 和 `decision-log.md` 一致
- [x] 不修改 `ThreadPoolExecutor` 子类化或不使用反射——executor rebuild 是显式操作
- [x] 复用 `ExecutorRegistry`、`ManagedExecutor`、`ExecutorStateSnapshot`、`ControlGate` 等现有基础设施
- [x] Safety gate 复用 `ControlGate` 接口模式
- [x] 现有 `ScaleAdjustmentCommand` 行为不变
- [x] QueueResizeCommand 与 ScaleAdjustmentCommand 职责清晰分离
- [x] 端到端测试覆盖 EXPAND + SHRINK + DENY 三条路径
- [x] v0.7.0 回溯教训已纳入（P6: latch before shutdown → drain before shutdown）
- [x] 风险和延期项表覆盖了 drain 丢失、SHRINK reject、timeout、并发 resize
- [x] 出口条件清单与 `managed-change-standard.md` 第 2 节一致
- [x] 不涉及 production 环境、外部依赖、REST/API/UI

## 6. 评审结论

IR 草案在范围控制和技术挑战认识方面**合格**。6 条需求覆盖了从 `QueueResizeCommand` 到端到端验证的完整链路，ExecutorRebuildStrategy 的设计方向正确（decommission → commission），安全门禁检查清单基本全面。但**不能直接进入 SR**：存在 2 个 P0 阻断项（F01 drain-replay 原子性、F02 adapter 扩展方式）和 4 个 P1 关键项。P0/P1 必须通过 disposition 关闭。

评审建议：**进入 IR disposition（`12-ir-review-disposition.md`）**，逐项处置 F01-F08。
