# v0.7.0 SR Review Disposition

## Header

- Document type: SR review disposition
- Version name: `v0.7.0`
- Reviewed artifact: `docs/04-development/versions/v0.7.0/21-sr-review.md`
- Disposition date: `2026-06-12`
- Disposition basis: `docs/02-harness/managed-change-standard.md` 第 3 节，严重级别规则

## 1. 逐项处置

### F01 [P1] — currentState() 无目标 executor 歧义

**处置**: **FIX**. 在 `ManagedExecutorAdjustmentAdapter` 的设计中增补构造参数。

**动作**: §4.5 的 Java 伪代码修改为：

```java
public class ManagedExecutorAdjustmentAdapter implements ExecutorAdjustmentAdapter {
    private final ExecutorRegistry registry;
    private final RuntimeAdjustmentSafetyGate safetyGate;
    private final String executorName;     // ← 增补

    public ManagedExecutorAdjustmentAdapter(
            ExecutorRegistry registry,
            RuntimeAdjustmentSafetyGate safetyGate,
            String executorName) { ... }    // ← 增补
}
```

`currentState()` 从 `registry.get(executorName)` 指向的执行器读取状态。若未注册，返回 `ExecutorStateSnapshot` 的替代方案：抛 `IllegalStateException("executor not found: " + executorName)`。

**状态**: `FIXED` — 将在 `20-sr.md` 中直接修正。

---

### F02 [P1] — ParameterBounds int/long 类型不匹配

**处置**: **FIX**. `ParameterBounds` 拆分为 `IntParameterBounds`（用于 CORE_POOL_SIZE / MAX_POOL_SIZE）和新增 `LongParameterBounds`（用于 KEEP_ALIVE_TIME）。

**动作**: §4.3 的 Java 伪代码修改：

```java
public final class IntParameterBounds {
    public static IntParameterBounds of(int minValue, int maxValue);
    public boolean within(int value);
}

public final class LongParameterBounds {
    public static LongParameterBounds of(long minValue, long maxValue);
    public boolean within(long value);
}

public final class RuntimeSetting {
    public static final IntParameterBounds CORE_POOL_SIZE_BOUNDS = IntParameterBounds.of(1, Integer.MAX_VALUE);
    public static final IntParameterBounds MAX_POOL_SIZE_BOUNDS = IntParameterBounds.of(1, Integer.MAX_VALUE);
    public static final LongParameterBounds KEEP_ALIVE_TIME_BOUNDS = LongParameterBounds.of(0, Long.MAX_VALUE);
}
```

原 `ParameterBounds` 类删除，替换为上述两个类型特定的 bounds 类。

**状态**: `FIXED` — 将在 `20-sr.md` 中直接修正。

---

### F03 [P1] — 生命周期图含 shutdownNow() 但 API 未暴露

**处置**: **FIX**. 在 `ManagedExecutor` API 增补 `shutdownNow()` 和 `isStopped()`。

**动作**: §4.1 的 Java 伪代码中生命周期段修改为：

```java
// 生命周期
public void shutdown();
public List<Runnable> shutdownNow();    // ← 增补，返回未执行的任务列表
public boolean isShutdown();
public boolean isStopped();             // ← 增补，对应 STOP 状态
public boolean isTerminated();
public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
```

生命周期状态机增补 `RUNNING → shutdownNow() → STOP` 路径。

**状态**: `FIXED` — 将在 `20-sr.md` 中直接修正。

---

### F04 [P1] — 闭环执行路径描述模糊

**处置**: **FIX**. 明确闭环实验的执行路径是直接编排而非侵入现有 runner。

**动作**: §9 F05 回应修改为：

> 闭环实验的执行路径：在端到端测试类中直接编排调用序列，不依赖 `ScenarioExperimentRunner`。编排顺序为：
> 1. 创建 `ManagedExecutor` 并注册到 `ExecutorRegistry`
> 2. 通过 `ManagedExecutor.submit()` 提交 scenario tasks
> 3. 通过 `ManagedExecutorAdjustmentAdapter.currentState()` 采集 `ExecutorStateSnapshot`
> 4. 通过 `PressureSampler` / `ThresholdPolicyEvaluator` 产出 `ScaleDecision`
> 5. 通过 `ManagedExecutorAdjustmentAdapter.apply(command)` 执行调整
> 6. 采集调整后快照并比对
>
> `BaselineWorkloadExecutor` 和 `ScenarioExperimentRunner` 不做任何修改。闭环实验不经过 scenario runner — 它是一条独立的、手动编排的验证路径。

**状态**: `FIXED` — 将在 `20-sr.md` 中直接修正。

---

### F05 [P2] — canRemove() 未注册 name 行为未定义

**处置**: `ACCEPTED`. 边界语义记录到实现备注。

**实现备注**: `canRemove(name, registry)` 在 `registry.get(name)` 返回 `Optional.empty()` 时，返回 `true`（幂等语义：不存在的条目视为已移除）。不阻塞 SR closure。

**状态**: `ACCEPTED_WITH_RECORD`.

---

### F06 [P2] — ManagedExecutor 未实现 ExecutorService

**处置**: `ACCEPTED`. v0.7.0 不追求通用性。`unwrap()` 已提供逃生舱口。后续版本可考虑 `implements ExecutorService`。

**状态**: `ACCEPTED_WITH_RECORD`.

---

### F07 [P2] — toSnapshot() 字段确定性分类未在叙事中区分

**处置**: `ACCEPTED`. 在 §4.6 的字段表中增加"确定性"列。

**动作**: 在 §4.6 表格中增补标注（作为 P2 改进，非阻塞）：

| 字段 | 确定性 | 说明 |
| --- | --- | --- |
| `corePoolSize` | DETERMINISTIC | setter 后立即可见 |
| `maximumPoolSize` | DETERMINISTIC | setter 后立即可见 |
| `keepAliveTimeSeconds` | DETERMINISTIC | setter 后立即可见 |
| `activeCount` | OBSERVATIONAL | 受线程调度影响 |
| `poolSize` | OBSERVATIONAL | 核心线程延迟创建 |
| `queueSize` | OBSERVATIONAL | 受任务提交/完成速率影响 |
| `completedTaskCount` | OBSERVATIONAL | 单调递增，但速率不确定 |
| `largestPoolSize` | OBSERVATIONAL | 取决于历史峰值 |
| `taskCount` | OBSERVATIONAL | 单调递增，但速率不确定 |
| `queueCapacity` | DETERMINISTIC | 构造时设定 |

**状态**: `ACCEPTED_WITH_RECORD` — 将更新 §4.6 表格。

## 2. 处置汇总

| ID | 级别 | 处置 | 动作 |
| --- | --- | --- | --- |
| F01 | P1 | FIXED | 构造注入 `executorName` |
| F02 | P1 | FIXED | 拆分为 `IntParameterBounds` + `LongParameterBounds` |
| F03 | P1 | FIXED | API 增补 `shutdownNow()` / `isStopped()` |
| F04 | P1 | FIXED | 明确闭环为直接编排路径 |
| F05 | P2 | ACCEPTED_WITH_RECORD | 实现备注 |
| F06 | P2 | ACCEPTED_WITH_RECORD | 后续版本考虑 |
| F07 | P2 | ACCEPTED_WITH_RECORD | 表格增补确定性列 |

## 3. 处置后状态

- P1: 4 / 4 FIXED.
- P2: 3 / 3 ACCEPTED_WITH_RECORD.

所有 P1 项均已在 SR 中直接修正。P2 项已记录为实现备注或后续改进。

## 4. 处置结论

所有 SR review findings 已处置。P1 修正包括：adapter 构造注入、bounds 类型拆分、shutdownNow 增补、闭环路径明确化。P2 残余风险可接受。**处置完成，进入 SR closure verification（`23-sr-closure-verification.md`）。**
