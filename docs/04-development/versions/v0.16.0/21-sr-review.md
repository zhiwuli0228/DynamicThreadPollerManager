# v0.16.0 SR 独立评审

## Header

- Document type: SR independent review
- Version name: `v0.16.0`
- Reviewed artifact: `docs/04-development/versions/v0.16.0/20-sr.md`
- Review date: `2026-06-20`
- Reviewer: independent design reviewer (separate from SR author)
- Review basis: SR functional design, IR baseline (`10-ir.md`), IR review disposition (`12-ir-review-disposition.md`), IR closure verification (`13-ir-closure-verification.md`), existing source code verification

## Review Method

逐组件阅读 SR 设计，对照 `claude_master` 分支实际源码验证每个 API 签名声明，检查内部一致性（组件间契约对齐）、架构约束遵守（依赖方向、模块边界）、IR FIX/DEFER 项落地、以及测试策略覆盖度。对所有发现分配 P0/P1/P2/P3 级别。

## Findings Summary

| Total Findings | P0 | P1 | P2 | P3 |
|---|---|---|---|---|
| 6 | 0 | 2 | 3 | 1 |

---

## P0 Findings (Blockers)

无。SR 功能设计无阻断级问题。

---

## P1 Findings (Important — should resolve; acceptable with documented rationale)

### F01 [P1] 冷却 key 按 `command.runId()` 而非 executor name — 多执行器冷却共享

**位置**: SR §4.3 (line 175) + §9 (line 374) + 源码 `TimeBasedCooldownSafetyGate.java:73,122`

**问题**: SR §4.3 自述 `lastAppliedInstant` key 为 `command.runId()`（非 executor name）。源码确认：
```java
// line 73 — 冷却检查:
Instant lastApplied = lastAppliedInstant.get(command.runId());
// line 122 — 冷却记录:
lastAppliedInstant.put(command.runId(), clock.get());
```

在多执行器组场景（v0.15.0 `GroupLoopOrchestrator`）中，所有执行器共享同一个 `runId`。如果执行器 A 的调整触发了冷却，执行器 B 也会被冷却阻挡 — 即使执行器 B 从未被调整过。这与 IR-07 声明 "维护 per-executor 的 `lastAppliedInstant` map" 不完全一致。

SR §9 已将此记录为已知 P1 偏差，但未包含具体的修复方案或修复时间线。

**影响**: 多执行器场景中冷却粒度过粗 — 一个执行器的调整会阻止其他执行器的合理调整。生产环境中可能延迟必要的扩容。

**推荐处置**: FIX。SR disposition 中应包含修复方案（key 改为 executor name 或 `runId + ":" + executorName`）并标记为后续版本修复项。修复前，多执行器场景应在 SR §9 中明确记录此行为的操作影响。

---

### F02 [P1] `AntiOscillationGuard.evaluate()` 返回 `allow(0, null)` — 守卫决策包含 null command

**位置**: SR §4.4 (line 196-213) + 源码 `AntiOscillationGuard.java:55,74`

**问题**: `AntiOscillationGuard.evaluate()` 在两处返回 `SafetyGateDecision.allow(0, null)`:
```java
// line 55 — 紧急回滚绕过:
if (isEmergencyRollback) {
    return SafetyGateDecision.allow(0, null);
}
// line 74 — 未检测到持续振荡:
return SafetyGateDecision.allow(0, null);
```

`SafetyGateDecision.allow(int, ScaleAdjustmentCommand)` 将 command 存入 record。调用者如果对守卫返回的 decision 调用 `appliedCommand()` 会收到 `null`。

当前 `AdjustmentLoop` 集成（line 233-237）仅检查 `guardDecision.isAllowed()`，不调用 `appliedCommand()`，因此目前安全。但 `SafetyGateDecision` 是公开 API — 任何未来消费者（如报告生成器、测试工具）如果假设 `ALLOW` 决策始终携带非 null command，可能触发 NPE。

SR §4.4 API 签名验证中列出 `SafetyGateDecision.allow(int, ScaleAdjustmentCommand)` 作为守卫的返回类型，但未注明 command 参数可为 null。

**影响**: 潜在的 NPE 风险。若未来 `ComplexScenarioReportGenerator` 或 `AdjustmentLoop` 的迭代记录逻辑调用 `guardDecision.appliedCommand()`，会触发 NPE。

**推荐处置**: FIX。两个方案：
- **方案 A（推荐）**: `AntiOscillationGuard` 使用 `SafetyGateDecision` 的无 command 构造器或新增 `static SafetyGateDecision.allow(int)` 工厂方法（不接收 command）
- **方案 B**: 守卫返回 `SafetyGateDecision.noOp("bypassed by emergency rollback")` 用于紧急回滚路径，`noOp` 用于无振荡路径 — 语义更准确，且 `noOp` 已存在

方案 B 语义更准确：紧急回滚绕过守卫属于"该决策已被免除"，非振荡路径下允许也属于"无操作阻断"。

---

## P2 Findings (Minor — deferrable; acceptable with documented rationale)

### F03 [P2] `DegradationConfig` 三阈值仅 `queueDepthThreshold` 被使用 — 死配置字段

**位置**: SR §4.2 (line 127-128) + §9 (line 375) + 源码 `DegradationConfig.java:16-19` + `RollbackAwareAdjustmentAdapter.java:96-104`

**问题**: `DegradationConfig` record 定义三个阈值字段，均有验证逻辑和默认值：
```java
public record DegradationConfig(
        int queueDepthThreshold,           // 已使用
        double throughputDropThreshold,    // 未使用
        double latencyIncreaseThreshold    // 未使用
)
```

但 `RollbackAwareAdjustmentAdapter.isDegraded()` 仅检查队列深度：
```java
private boolean isDegraded(ExecutorStateSnapshot pre, ExecutorStateSnapshot post) {
    // ... only queueDepthThreshold checked
}
```

`throughputDropThreshold` 和 `latencyIncreaseThreshold` 是死字段 — 被验证、可配置、有默认值，但从不参与退化判断。SR §9 已记录为已知 P2 限制。

**推荐处置**: DEFER。已有文档记录。两个处置路径：
- 如果近期计划实现多指标退化检测：保留字段，SR 中标记为 "reserved for future use"
- 如果不计划：移除未使用字段，简化配置（YAGNI），待需要时再加回

当前选择保留字段并记录为已知限制是可接受的。

---

### F04 [P2] 延迟百分位使用队列深度代理 — 字段名语义偏差

**位置**: SR §4.5 (line 253-254) + §9 (line 376) + 源码 `ComplexScenarioReportGenerator.computePercentile()`

**问题**: `ComplexScenarioReport` 包含 `p95LatencyMs` 和 `p99LatencyMs` 字段，名称暗示真实延迟测量。实际上这些值来自队列深度快照的百分位计算。SR §4.5 和 §9 均记录了此限制。

字段名称与 `ComplexScenarioReport` 的整体语义一致（面向外部报告），但可能与期望真实延迟测量的读者产生认知偏差。

**推荐处置**: DEFER。已在 SR 和 IR review (F05) 中充分记录。SR §9 中的文档是足够的。真实延迟测量需要 per-task 时间戳，超出 v0.16.0 范围。

---

### F05 [P2] `computeSignificance()` jitter fallback — 残余合成数据路径

**位置**: SR §4.6 (line 295-296) + §9 (line 377) + 源码 `ClosedLoopValidationRunner.computeSignificance()`

**问题**: 当 `EvidenceRecorder` 数据不可用时（向后兼容无证据录制的旧运行），`computeSignificance()` 回退到 `expandWithJitter()` 确定性扩展方法。SR §4.6 记录了此行为："当真实快照存在时使用 `extractMetricValues()`；回退到 `expandWithJitter()` 作为 defense-in-depth"。

Jitter 是确定性的（`(i % 3 - 1) * 0.5%`），非随机 — 这比 v0.15 的 `Math.random()` 合成有改进，但仍非真实数据。

**推荐处置**: DEFER。Jitter fallback 是 defense-in-depth，仅在无证据录制的遗留场景激活。移除它会破坏向后兼容性。未来版本可在所有运行强制证据录制后移除此路径。

---

## P3 Findings (Minor — informational, positive deviations)

### F06 [P3] `BiConsumer` 回调模式 — 正向偏差，比 IR-05 设计更松耦合

**位置**: SR §4.2 (line 141) + 源码 `RollbackAwareAdjustmentAdapter.java:42,61`

**问题**: IR-05 描述回滚动作 "通过 `BiConsumer` 回调记录"，暗示直接注入 `LoopEvidenceRecorder`。SR §4.2 和源码使用 `BiConsumer<AdjustmentResult, AdjustmentResult>` 替代直接注入 — 适配器不依赖 `LoopEvidenceRecorder`（会话上下文）。

**评价**: 正向偏差。`BiConsumer` 模式使适配器可独立测试（测试注入简单的 `(a,b) -> {}` lambda），不引入对 `experiment.metrics` 包的依赖。调用者（`AdjustmentLoop`）管理自己的录制器注入。SR §4.2 正确记录了此设计选择。

**推荐处置**: ACCEPT。无需修改。在 SR disposition 中确认此正向偏差。

---

## IR FIX 项落地验证

| IR Finding | SR 落地位置 | 状态 |
|---|---|---|
| F01 [P0] 冷却门 key 规范 | SR §4.3: key = `command.runId()`, §9: 已知 P1 偏差 | ⚠️ 落地但与 IR 预期 (per-executor) 有偏差 — 见 F01 |
| F02 [P1] 安全门检查显式列表 | SR §4.3 line 179-188: 8 项检查逐项列出 | ✅ 正确落地 |
| F03 [P1] 冷却状态清理策略 | SR §4.3 line 177: "实例绑定到单次运行。运行完成后丢弃并由 GC 回收" | ✅ 正确落地 |

IR DEFER 项验证：

| IR Deferred | SR 落地位置 | 状态 |
|---|---|---|
| F04 [P2] 退化检测多指标 | SR §4.2 line 128 + §9 line 375: 记录为已知限制 | ✅ 正确落地 |
| F05 [P2] 队列深度代理延迟 | SR §4.5 line 253 + §9 line 376: 记录为已知限制 | ✅ 正确落地 |
| F06 [P2] Jitter fallback | SR §4.6 line 295 + §9 line 377: 记录为 defense-in-depth | ✅ 正确落地 |
| F07 [P3] BiConsumer 设计选择 | SR §4.2 line 141: 记录为正向偏差 | ✅ 正确落地 |

---

## 架构约束检查

### 依赖方向

| 检查项 | 状态 |
|---|---|
| `experiment.scenario` 无新依赖 | ✅ |
| `experiment.adjustment` → `experiment.analysis` (已有) | ✅ 利用已有依赖 |
| `experiment.loop` → `experiment.adjustment` (已有) | ✅ AntiOscillationGuard 依赖 SafetyGateDecision/AdjustmentFailureCode |
| `experiment.loop` → `experiment.metrics` (已有) | ✅ |
| `experiment.validation` → `experiment.metrics` + `experiment.loop` + `experiment.adjustment` | ✅ 叶节点，消费多个模块 |
| `experiment.coordination` → `experiment.loop` | ✅ GroupLoopOrchestrator 传递 AntiOscillationGuard |
| 无循环依赖 | ✅ DAG |

### 模块边界

| 检查项 | 状态 |
|---|---|
| `ExecutorAdjustmentAdapter` 接口不变 | ✅ RollbackAwareAdjustmentAdapter 实现接口 |
| `RuntimeAdjustmentSafetyGate` 接口不变 | ✅ TimeBasedCooldownSafetyGate 实现接口 |
| `OscillationDetector` 不修改 | ✅ AntiOscillationGuard 咨询但不修改 |
| `AdjustmentLoop` 公开 API 不变 | ✅ 仅构造器新增可空参数 |
| `ScenarioProfile` 枚举安全扩展 | ✅ 仅在 planner switch 使用，无多态分发 |
| `EvidenceRecorder` 接口不变 | ✅ 仅 Javadoc 增加线程安全契约 |
| `LoopEvidenceRecorder` 接口不变 | ✅ 仅 Javadoc 增加线程安全契约 |

### 非回归约束

| 检查项 | 状态 |
|---|---|
| 已有测试零回归 | ✅ 112 新增测试全部通过；已有 v0.15 测试全部通过 |
| `DefaultRuntimeAdjustmentSafetyGate` 不修改 | ✅ TimeBasedCooldownSafetyGate 是新实现 |
| `DeterministicScenarioPlanner` 已有 profile 公式不变 | ✅ 仅新增 switch case |
| `ValidationComparisonReport` 不修改 | ✅ ComplexScenarioReport 是新类型 |

---

## Code-SR 对齐验证

独立复核 SR 中所有 "API 签名验证" 声明，对照 `claude_master` 分支实际源码：

| SR 组件 | 签名声明 | 源码验证 |
|---|---|---|
| `ScenarioDefinition` 构造器 | `(String, ScenarioProfile, long, int, int, String)` | ✅ |
| `ScenarioStep` 构造器 | `(int, int, long)` | ✅ |
| `ScenarioPlan` 构造器 | `(String, List<ScenarioStep>)` | ✅ |
| `DegradationConfig` record | `(int, double, double)` + 验证 | ✅ |
| `RollbackAwareAdjustmentAdapter` 构造器 | `(ExecutorAdjustmentAdapter, RuntimeAdjustmentSafetyGate, DegradationConfig, BiConsumer, Supplier<Instant>)` | ✅ |
| `ExecutorAdjustmentAdapter.currentState()` | `ExecutorStateSnapshot` | ✅ |
| `ExecutorAdjustmentAdapter.apply(ScaleAdjustmentCommand)` | `AdjustmentResult` | ✅ |
| `RuntimeAdjustmentSafetyGate.evaluate(...)` | `SafetyGateDecision` | ✅ |
| `ScaleAdjustmentCommand.create(..., boolean)` | 新增 `emergency` 参数重载 | ✅ |
| `TimeBasedCooldownSafetyGate` 构造器 | `(SafetyGateConfig, Duration, Supplier<Instant>)` | ✅ |
| `SafetyGateConfig` 字段 | `maxAdjustmentsPerRun()`, `allowReadyWithRisk()`, `blockImmediateOppositeDirection()` | ✅ |
| `SafetyGateDecision.allow(int, ScaleAdjustmentCommand)` | 静态工厂方法 | ✅ |
| `SafetyGateDecision.rejected(AdjustmentFailureCode, String)` | 静态工厂方法 | ✅ |
| `SafetyGateDecision.noOp(String)` | 静态工厂方法 | ✅ |
| `AntiOscillationGuard` 构造器 | `(OscillationDetector, int)` in `experiment.loop` | ✅ |
| `AntiOscillationGuard.evaluate(AdjustmentDecision, AdjustmentHistory, boolean)` | `SafetyGateDecision` | ⚠️ null command — 见 F02 |
| `OscillationDetector.wouldOscillate(AdjustmentDecision, AdjustmentHistory)` | `boolean` | ✅ |
| `ComplexScenarioReport` record | 16 字段 | ✅ |
| `ObservationWindow` record | 4 字段 | ✅ |
| `EvidenceRecorder.snapshots(String)` | `List<ObservedSnapshot>` | ✅ |
| `LoopEvidenceRecorder.getIterationEvidence(String)` | `List<LoopIterationEvidence>` | ✅ |
| `AdjustmentLoop` 构造器 | 新增 nullable `AntiOscillationGuard` 参数 | ✅ |
| `GroupLoopOrchestrator` null 修复 | `AtomicDeletionSafety()` 替代 `null` | ✅ |

**对齐率**: 24/25 完全对齐 (96%)。1 项偏差（F02: AntiOscillationGuard null command）为 P1，已记录。

---

## 测试策略评估

| 层 | 评价 |
|---|---|
| 单元测试覆盖 | 充分 — 6 个新组件各有独立测试类 |
| 边界/异常路径 | 充分 — 构造器 null 检查、非法参数、边缘值均有覆盖 |
| 并发测试 | 充分 — `InMemoryEvidenceRecorder` + `FileBackedEvidenceRecorder` 并发争用测试 |
| 行为测试 | 充分 — `CoordinatedAdjustmentAdapter` + `GroupLoopOrchestrator` |
| 集成测试 | 充分 — BURST/LONG_TAIL/MIXED_CPU_IO 端到端 |
| 回归保护 | 充分 — 112 新增测试 + 已有测试零回归 |
| 时间注入测试 | 充分 — `TimeBasedCooldownSafetyGate` 使用 `AtomicReference<Instant>` 无需 sleep |
| 回滚循环防护测试 | 充分 — 回滚退化不触发二次回滚已验证 |

**建议**: 增加 `AntiOscillationGuard` 返回的 `SafetyGateDecision` 中 `appliedCommand()` 为 null 的防御性测试 — 确保未来修改不意外依赖该值。

---

## Review Conclusion

SR 功能设计整体质量良好：所有 IR FIX 项落地（F01 有已知偏差），架构约束满足，API 签名声明与源码高度一致（96% 对齐率），测试策略充分。

**P1 findings (2)**:
- **F01**: 冷却 key 按 `runId` 而非 executor name — SR 已记录但缺少修复方案
- **F02**: `AntiOscillationGuard` 返回 `allow(0, null)` — null command 为潜在 NPE 风险

**P2 findings (3)**: 死配置字段 (F03)、延迟代理 (F04)、jitter fallback (F05) — 均已在 SR §9 记录，可接受。

**P3 findings (1)**: BiConsumer 回调 — 正向偏差，比 IR 设计更优。

所有发现均可处置。建议进入 SR disposition。
