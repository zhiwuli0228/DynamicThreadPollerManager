# v0.7.0 SR 独立评审报告

## Header

- Document type: SR independent review
- Version name: `v0.7.0`
- Reviewed artifact: `docs/04-development/versions/v0.7.0/20-sr.md`
- Review date: `2026-06-12`
- Reviewer role: 独立 SR review（非 SR 作者）
- Review basis: `docs/02-harness/managed-change-standard.md` 第 3 节（SR 功能设计）

## 1. 评审输入

已读取以下文件作为评审上下文：

- `docs/00-project/current-state.md`
- `docs/02-harness/managed-change-standard.md`
- `docs/01-architecture/managed-executor-domain-model.md`
- `docs/04-development/versions/v0.7.0/10-ir.md`（修正后）
- `docs/04-development/versions/v0.7.0/11-ir-review.md`
- `docs/04-development/versions/v0.7.0/12-ir-review-disposition.md`
- `docs/04-development/versions/v0.7.0/13-ir-closure-verification.md`
- `docs/04-development/versions/v0.7.0/20-sr.md`
- `src/main/java/.../adjustment/ExecutorAdjustmentAdapter.java`
- `src/main/java/.../adjustment/ExecutorStateSnapshot.java`

## 2. 评审摘要

SR 整体质量高：6 个核心组件设计完整（含 Java 契约伪代码），依赖方向清晰，失败语义枚举完整，3 个候选 change 分解合理且依赖关系正确。IR 阶段延期项（F03/F05/F06）均已给出明确设计决策。但存在 4 个 P1 问题集中在适配器设计歧义和类型一致性上，以及 3 个 P2 边界情况。

## 3. Findings

### F01 [P1] ManagedExecutorAdjustmentAdapter.currentState() 无目标执行器歧义

**位置**: 20-sr.md §4.5

**问题**: `ExecutorAdjustmentAdapter` 接口定义的 `currentState()` 方法无参数。`InMemoryAdjustableExecutorProbe` 只管理自身状态，无歧义。但 `ManagedExecutorAdjustmentAdapter` 包装的是 `ExecutorRegistry`（多执行器），`currentState()` 不知道应该从哪个 `ManagedExecutor` 读取状态。

SR 的伪代码注释说"从默认或显式指定的 ManagedExecutor 读取状态"，但未定义"默认执行器"的指定方式。

**影响**: 实现者可能在 `currentState()` 中引入隐式状态（last-used executor）、硬编码名称、或返回 null — 三者都不合理。

**建议**: SR 明确指定 `ManagedExecutorAdjustmentAdapter` 的构造参数包含一个 `String defaultExecutorName`，`currentState()` 返回该名称对应执行器的快照。

---

### F02 [P1] ParameterBounds 为 int 但 keepAliveTime 为 long

**位置**: 20-sr.md §4.3

**问题**: `ParameterBounds` 定义为 `int minValue, int maxValue`，但 `keepAliveTime` 的类型是 `long`（`ThreadPoolExecutor.getKeepAliveTime(TimeUnit)` 返回 `long`）。

```java
// SR 设计
public final class ParameterBounds {
    private final int minValue;    // ← int
    private final int maxValue;    // ← int
}

// 但 keepAliveTime 需要 long 范围
managedExecutor.setKeepAliveTime(120_000, TimeUnit.MILLISECONDS);  // long
```

`int` 范围的 keepAliveTime（约 24 天以秒计）对绝大多数场景足够，但 SR 不应留下类型不一致的设计债务。

**影响**: 实现者可能创建两套 `ParameterBounds`（int 版和 long 版），或静默截断 long → int。

**建议**: `ParameterBounds` 改为泛型 `<T extends Comparable<T>>`，或分别为 pool size（int）和 time（long）提供 `IntParameterBounds` / `LongParameterBounds`。

---

### F03 [P1] 生命周期图含 shutdownNow() 但 ManagedExecutor API 未暴露

**位置**: 20-sr.md §4.1 vs §5.4

**问题**: 生命周期状态机（§5.4）描述了 `shutdownNow() → STOP` 路径，但 `ManagedExecutor` 接口（§4.1）未暴露 `shutdownNow()` 方法。`ThreadPoolExecutor` 原生支持 `shutdownNow()`，`ManagedExecutor` 没有理由隐藏它。

**影响**: 若实验需要强制中断（如超时场景），调用方必须通过 `unwrap().shutdownNow()` 绕过包装层，破坏封装。

**建议**: `ManagedExecutor` 接口增补 `shutdownNow()` 和 `isStopped()` 方法。

---

### F04 [P1] 闭环实验中"新建实验专用执行路径"描述不够具体

**位置**: 20-sr.md §9 (F05 回应)

**问题**: IR deferred F05 询问 `ManagedExecutor` 与 `BaselineWorkloadExecutor` 的集成关系。SR 的回应是："闭环实验中，`ScenarioExperimentRunner` 通过新建的实验专用执行路径使用 `ManagedExecutor.submit()` 提交任务。"

"新建的实验专用执行路径" (new experiment-specific execution path) 是一个模糊描述。Change 3（`closed-loop-experiment-verification`）的弱实现 agent 不知道应该：
- 创建新的 runner 类？
- 扩展现有 `ScenarioExperimentRunner` 添加重载方法？
- 在测试中直接调用 `ManagedExecutor.submit()` 绕过 runner？

**影响**: Change 3 的实现者可能在"创建新类 vs 扩展现有类 vs 绕过框架"之间摇摆，导致设计不一致。

**建议**: SR 明确闭环实验的执行路径：推荐在测试类中直接编排 `ManagedExecutor` + `PressureSampler` + `PolicyEvaluator` + `ManagedExecutorAdjustmentAdapter` 的调用序列，不依赖 `ScenarioExperimentRunner`。原因：闭环实验本身是一个新能力验证，不应对现有 scenario runner 做侵入性修改。

---

### F05 [P2] canRemove() 在 executor 未注册时的行为未定义

**位置**: 20-sr.md §4.4

**问题**: `DeletionSafety.canRemove(name, registry)` 调用 `registry.get(name)`。若 name 从未注册（`Optional.empty()`），`canRemove()` 应返回 `true`（不存在即可以"移除"——no-op）还是 `false`（从未注册的东西不能安全移除）？SR 未定义。

**影响**: 边界行为歧义，但概率低（调用方通常在注册过的 name 上操作）。P2 可接受。

**建议**: SR 或实现阶段记录：未注册的 name → `canRemove()` 返回 `true`（幂等语义）。

---

### F06 [P2] ManagedExecutor 未实现 ExecutorService 接口

**位置**: 20-sr.md §4.1

**问题**: `ManagedExecutor` 实现了 `AutoCloseable` 并暴露了 `submit()` 和生命周期方法，但未声明 `implements ExecutorService`。`ThreadPoolExecutor` 本身就是 `ExecutorService`，若 `ManagedExecutor` 不继承该接口，`unwrap()` 将成为唯一获取标准 `ExecutorService` 的途径。

**影响**: 某些期望 `ExecutorService` 参数的通用工具方法无法直接接受 `ManagedExecutor`。但 SR 明确不追求通用性，P2 可接受。

**建议**: 后续版本若需要通用性，可考虑 `ManagedExecutor implements ExecutorService`（委托所有方法）。

---

### F07 [P2] toSnapshot() 字段覆盖完整性与叙事可加强

**位置**: 20-sr.md §4.6

**问题**: `toSnapshot()` 列出了 11 个字段，但在文档叙述中未区分"即时反映参数变更"（corePoolSize, maxPoolSize, keepAliveTimeSeconds — setter 后立即可见）与"反映运行时效果"（poolSize, activeCount, queueSize — 受线程调度延迟影响）。虽然没有设计错误，但这个区分对弱实现 agent 理解验收标准至关重要。

**影响**: 低。但若实现者混淆了这两类字段的行为差异，可能导致测试设计不当（如断言 poolSize 等于刚设的 corePoolSize）。

**建议**: `toSnapshot()` 的字段表增加"确定性"列，标注 `DETERMINISTIC`（参数类）vs `OBSERVATIONAL`（运行时状态类）。

---

## 4. 正向检查通过项

- [x] SR 包含 `managed-change-standard.md` §3 要求的全部内容：模块边界、数据模型、接口设计、状态枚举、失败语义、依赖方向、并发/资源/观测边界、测试映射、非范围声明。
- [x] 6 个核心组件均有完备的 Java 伪代码契约，对弱实现 agent 足够具体。
- [x] 依赖方向清晰：`experiment.executor` 单向依赖 `experiment.adjustment` 和 `java.util.concurrent`，其他现有包不依赖新包。
- [x] "安全门先行，调整后行"的调整流程与 v0.5.0 的 `ExecutorAdjustmentAdapter` 契约一致。
- [x] IR 阶段 3 个 P1 deferred 项（F03/F05/F06）均有明确 SR 设计决策。
- [x] 非范围声明覆盖 queue resizing、闭环调度、持久化、REST/API/UI、外部依赖。
- [x] 3 个候选 Change 分解边界清晰，依赖关系正确（串行）。
- [x] 状态与失败语义完整覆盖 `APPLIED/REJECTED/FAILED/NO_OP`。
- [x] 并发策略覆盖所有新组件。
- [x] 测试分层完整（单元 → 集成 → 端到端），验收矩阵与 IR AC 一一对应。
- [x] `InMemoryAdjustableExecutorProbe` 保留策略明确（两个 adapter 共存，DI 区分）。

## 5. Findings 汇总

| ID | 位置 | 描述 | 级别 | 建议动作 |
| --- | --- | --- | --- | --- |
| F01 | §4.5 | `currentState()` 无目标 executor 参数 | P1 | 构造注入 `defaultExecutorName` |
| F02 | §4.3 | `ParameterBounds` int/long 类型不匹配 | P1 | 改为泛型或分拆类 |
| F03 | §4.1/§5.4 | 生命周期图含 `shutdownNow()` 但 API 未暴露 | P1 | 增补 `shutdownNow()` 到 API |
| F04 | §9 | 闭环执行路径描述模糊 | P1 | 明确为测试编排而非侵入 runner |
| F05 | §4.4 | `canRemove()` 未注册 name 行为未定义 | P2 | 记录幂等语义 |
| F06 | §4.1 | 未实现 `ExecutorService` | P2 | 后续版本考虑 |
| F07 | §4.6 | 字段确定性分类未在叙事中区分 | P2 | 增加确定性标注 |

## 6. 评审结论

SR 在完整性、架构一致性和实现可行性方面**合格**。所有核心组件、依赖边界、安全策略、测试矩阵和 change 分解均已到位。无 P0 阻断项。

存在 4 个 P1 关键项：其中 F01（currentState 歧义）和 F03（shutdownNow 缺失）应直接在 SR 中修正；F02（ParameterBounds 类型）和 F04（闭环路径描述）可在 SR 修正或 DEFERRED_TO_CHANGE。P2 项均为边界情况，不阻塞推进。

**评审建议**: 进入 SR disposition（`22-sr-review-disposition.md`）。
