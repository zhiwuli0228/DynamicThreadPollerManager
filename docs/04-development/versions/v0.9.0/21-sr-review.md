# v0.9.0 SR 独立评审报告

## Header

- Document type: SR independent review
- Version name: `v0.9.0`
- Reviewed artifact: `docs/04-development/versions/v0.9.0/20-sr.md`
- Review date: `2026-06-13`
- Reviewer role: 独立 SR review（非 SR 作者）
- Review basis: `docs/02-harness/managed-change-standard.md` 第 3 节（SR 功能设计）

## 1. 评审输入

已读取以下文件作为评审上下文：

- `docs/00-project/current-state.md`
- `docs/02-harness/managed-change-standard.md`
- `docs/01-architecture/operational-and-evolution-boundaries.md`
- `docs/05-domain/exploration-boundaries.md`
- `docs/04-development/versions/v0.9.0/10-ir.md` (IR closure verified)
- `docs/04-development/versions/v0.9.0/11-ir-review.md`
- `docs/04-development/versions/v0.9.0/12-ir-review-disposition.md`
- `docs/04-development/versions/v0.9.0/13-ir-closure-verification.md`
- `docs/04-development/versions/v0.9.0/20-sr.md`
- `src/main/java/.../executor/ManagedExecutorAdjustmentAdapter.java`
- `src/main/java/.../executor/ExecutorRegistry.java`
- `src/main/java/.../experiment/adjustment/AdjustmentResult.java`
- `src/main/java/.../policy/ControlGate.java`
- `src/main/java/.../policy/SafetyGateResult.java`
- `src/main/java/.../experiment/adjustment/ExecutorStateSnapshot.java`

## 2. 评审摘要

SR 整体设计质量高：5 个核心组件伪代码完整，ExecutorRebuildStrategy 的 decommission→commission 流程清晰，QueueResizeSafetyGate 的安全检查清单全面。IR 阶段 3 个 deferred 项（F03/F07/F08）均已在本 SR 中给出明确设计决策。2 个候选 change 分解合理。但存在 2 个 P1 问题和 2 个 P2 建议，集中在 AdjustmentResult 泛型类型兼容性、RebuildResult 与 ResizeEvidence 耦合、以及幂等保护缺失。

## 3. Findings

### F01 [P1] AdjustmentResult 泛型类型兼容性

**位置**: 20-sr.md §4.4, §4.5

**问题**: SR §4.4 中 `QueueResizeAdjustmentAdapter.apply()` 返回 `AdjustmentResult`，§4.5 中 `ResizeEvidence` 通过 `AdjustmentResult.evidence()` 携带。但需确认现有 `AdjustmentResult` 的泛型设计是否允许此用法。

当前代码中的 `AdjustmentResult<T>` 可能定义为：
- `class AdjustmentResult<T> { T evidence(); }` — 泛型灵活，ResizeEvidence 可直接使用
- `class AdjustmentResult { AdjustmentEvidence evidence(); }` — 固定 evidence 类型，ResizeEvidence 需要继承或实现 AdjustmentEvidence

SR 伪代码中调用 `AdjustmentResult.failed("SAFETY_GATE_DENIED", reason, null)` 和 `AdjustmentResult.success(executorId, evidence)` —— 没有指定泛型参数或工厂方法签名。需要确认现有 API 的实际签名。

**影响**: 若现有 `AdjustmentResult` 的 evidence 类型是固定的 `AdjustmentEvidence` 接口，ResizeEvidence 需要实现该接口或单独包装。若泛型灵活则无问题。

**建议**: SR 确认现有 `AdjustmentResult` API 签名。若泛型灵活 → 无需修改。若固定类型 → ResizeEvidence 需要实现 `AdjustmentEvidence` 接口（需确认该接口是否存在及契约）。

---

### F02 [P1] 幂等保护缺失：并发 resize 请求

**位置**: 20-sr.md §4.3, §4.4

**问题**: SR §4.3 中 QueueResizeSafetyGate 只检查 executor RUNNING 状态和容量合法性，但没有幂等保护。IR review F05 提到的并发 resize race 在 SR 中未彻底解决。

场景：两个 resize 请求同时到达 → 两个 adapter.apply() 同时执行 → 第一个开始 rebuild → 第二个也通过 safety gate（此时 executor 仍在 registry 中且状态为 RUNNING） → 第二个也执行 rebuild → 第一个 rebuild 完成注册新 executor → 第二个 rebuild 操作的对象是旧 executor（已被 shutdown）或新 executor（已被替换）

SR §4.3 备注说"幂等保护留在 adapter 层处理"，但 adapter 的伪代码中没有实现幂等保护。

**影响**: 并发 resize 请求可能导致 executor 状态混乱或重复 rebuild。

**建议**: 在 `QueueResizeAdjustmentAdapter` 中增加 `ConcurrentHashMap<String, AtomicBoolean> resizeInProgress` 标记：
- `apply()` 入口处 `resizeInProgress.putIfAbsent(executorId, true)` 返回 null → 继续
- 返回非 null → 说明已有 resize 在进行中 → `return AdjustmentResult.failed("RESIZE_IN_PROGRESS", ...)`
- `apply()` 出口处（finally）清除标记

---

### F03 [P2] RebuildResult 中间类型与 ResizeEvidence 耦合

**位置**: 20-sr.md §4.2, §4.5

**问题**: SR 定义了 `RebuildResult`（ExecutorRebuildStrategy 的返回值）和 `ResizeEvidence`（公开 evidence record）两个类型。`RebuildResult` 仅用于 strategy → adapter 的内部传递，adapter 通过 `ResizeEvidence.from(rebuildResult)` 转换后再放入 `AdjustmentResult`。

但 `RebuildResult` 携带的字段与 `ResizeEvidence` 高度重叠（success, beforeState, afterState, duration, drainedCount, rejectedCount, direction, oldCapacity, newCapacity, errorMessage）。两个类型维护 10 个相似字段增加了同步负担。

**影响**: SR 阶段可接受——两个类型的语义边界清晰（内部 vs 公开）。但实现阶段需要注意同步性。

**建议**: 考虑合并：让 `RebuildResult` 直接 extends `ResizeEvidence` 或移除 `RebuildResult` 直接使用 `ResizeEvidence`。或保持分离但记录为设计决策。

---

### F04 [P2] ControlGate 接口适配：readiness 参数

**位置**: 20-sr.md §4.3

**问题**: SR §4.3 中 `QueueResizeSafetyGate` 实现 `ControlGate<QueueResizeCommand>`，`evaluate()` 方法签名包含 `ReadinessSummary readiness` 参数。SR 伪代码中传 `null` 作为 readiness 参数并在方法内说明"不使用 readiness 参数"。

但 `ControlGate` 接口的原始设计可能假设 readiness 参数是必需的——若接口定义 `evaluate(T command, State state, ReadinessSummary readiness)` 且 readiness 标注 `@NonNull`，传 null 会在静态分析中报警。

**影响**: 低。实现时可通过 permissive `ReadinessSummary.NOT_EVALUATED` 占位值替代 null。但 SR 应记录接口适配决策。

**建议**: SR 明确 `evaluate()` 的第三个参数传入 `ReadinessSummary.NOT_EVALUATED` 占位值或等效 sentinel，而非 null。或 SR 声明 `ControlGate` 接口允许 nullable readiness。

---

## 4. Findings 汇总

| ID | 位置 | 描述 | 级别 | 建议动作 |
|---|---|---|---|---|
| F01 | SR §4.4/4.5 | AdjustmentResult 泛型类型兼容性需确认 | P1 | 确认现有 API 签名；若固定类型则 ResizeEvidence 需适配 |
| F02 | SR §4.3/4.4 | 幂等保护缺失——并发 resize 请求 | P1 | Adapter 增加 resizeInProgress 标记 |
| F03 | SR §4.2/4.5 | RebuildResult 与 ResizeEvidence 字段重叠 | P2 | 考虑合并或记录决策 |
| F04 | SR §4.3 | ControlGate readiness 参数 null 传参 | P2 | 使用 NOT_EVALUATED 占位值替代 null |

## 5. 正向检查通过项

- [x] SR 不授权实现——明确声明 "不授权 Java 源码或测试实现"
- [x] 5 个核心组件伪代码完整，含构造器、方法签名、关键逻辑
- [x] Decommission 流程顺序正确：shutdown → drain → awaitTermination（处置 IR F05）
- [x] QueueResizeAdjustmentAdapter 新建独立类，不修改 ManagedExecutorAdjustmentAdapter（处置 IR F02）
- [x] ResizeEvidence 通过 AdjustmentResult 携带，不经过 EvidenceRecorder（处置 IR F04）
- [x] 保持同一 executorId 在 rebuild 前后（处置 IR F03）
- [x] SHRINK drain-and-discard 设计合理（处置 IR F08）
- [x] 依赖方向明确且裁决完整
- [x] 测试策略分层清晰：单元 → 集成 → 端到端
- [x] 非回归约束覆盖现有 adapter 和 runner 测试
- [x] 2 个候选 change 分解合理，依赖关系清晰
- [x] 现有 433 测试零回归承诺
- [x] 不涉及 reflection hack、rejection policy、closed-loop、多执行器
- [x] 验收矩阵覆盖 16 个 AC，包括 P0 关键路径

## 6. 评审结论

SR 设计在架构边界、组件契约和测试策略方面**合格**。5 个核心组件设计完整，ExecutorRebuildStrategy 的安全顺序（shutdown→drain→awaitTermination）正确，Safety gate 检查清单全面。但**不能直接进入 SR closure**：存在 2 个 P1 问题（F01 AdjustmentResult 类型兼容性需确认、F02 幂等保护缺失）。P1 必须通过 disposition 处置。

评审建议：**进入 SR disposition（`22-sr-review-disposition.md`）**，逐项处置 F01-F04。
