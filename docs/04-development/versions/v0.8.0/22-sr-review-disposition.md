# v0.8.0 SR Review Disposition

## Header

- Document type: SR review disposition
- Version name: `v0.8.0`
- Reviewed artifact: `docs/04-development/versions/v0.8.0/21-sr-review.md`
- Disposition date: `2026-06-12`
- Disposition basis: `docs/02-harness/managed-change-standard.md` 第 3 节，严重级别规则

## 1. 处置规则

| 级别 | 处置方式 |
|---|---|
| P0 | 必须修复并闭环，不能进入 change decomposition |
| P1 | 必须修复或明确 defer 到 OpenSpec change 阶段并记录风险 |
| P2 | 可修复或记录残余风险 |

## 2. 逐项处置

### F01 [P1] — AcquisitionReportWriter 构造函数与版本化路径不匹配

**处置**: **FIX**. SR §4.4 明确新增重载构造器。

**动作**: 在 SR §4.4 的 `AcquisitionReportPaths` 设计末增补：

> `AcquisitionReportWriter` 新增双参构造器：
> ```java
> public AcquisitionReportWriter(Path outputRoot, AcquisitionReportPaths paths) {
>     this.outputDirectory = outputRoot.resolve(paths.outputDirectory());
> }
> ```
> 现有单参构造器保留不变（向后兼容），内部委托给 `AcquisitionReportPaths.forVersion("v0.6.0")`。

更新 SR §4.6 `AcquisitionReportBridge` 的构造为直接使用新构造器。

**状态**: `FIXED` — 将在 `20-sr.md` 中直接修正，由 `23-sr-closure-verification.md` 验证。

---

### F02 [P1] — Bridge 错误使用 NOT_READY 标记未评估数据

**处置**: **FIX**. 采用方案 A：纯采集模式不产出 `ReadinessSummary`。

**理由**: `ReadinessSummary` 的语义是"数据已通过 readiness 评估并给出结论"。纯采集模式没有做 readiness 评估，产出 readiness 本身就是语义错误。`NOT_READY` 标记会误导下游自动决策。

**动作**: 
1. SR §4.6 `AcquisitionReportBridge.bridge()` 方法不产出 `ReadinessSummary`，只产出 4 个 artifact（`RunManifest`、`PressureSummary`、`ReplaySummary`、`EvidenceIndex`）。
2. `AcquisitionReportWriter.writeAll()` 新增 4-参数重载（不含 `ReadinessSummary`），或 bridge 分别调用 4 个独立 write 方法。
3. `EvidenceIndex` 的 `readinessSummaryPath` 字段填 `null` 或空字符串（nullable）。
4. 在下游执行 readiness 评估时（后续版本），再产出 `ReadinessSummary`。

**状态**: `FIXED` — 将在 `20-sr.md` 中直接修正。

---

### F03 [P1] — Runner 创建 DeletionSafety 但未使用引用计数

**处置**: **FIX**. Runner 不创建 `AtomicDeletionSafety`，直接检查 `isTerminated()`。

**理由**: Runner 是单线程顺序执行——没有并发引用需要保护。引用计数器始终为 0，只有 `isTerminated()` 检查有效。消除不必要的抽象层。

**动作**: 
1. SR §4.2 runner Phase 1 从：
   ```java
   AtomicDeletionSafety deletionSafety = new AtomicDeletionSafety();
   ExecutorRegistry registry = new ExecutorRegistry(deletionSafety);
   ```
   改为：
   ```java
   ExecutorRegistry registry = new ExecutorRegistry(null);  // no deletion safety needed
   ```
2. `ExecutorRegistry` 构造器接受 `null` deletionSafety（表示跳过引用计数检查，只检查 `isTerminated()`）。
3. 若 `ExecutorRegistry` 现有实现不接受 null，在 constructor 中提供默认的 no-op `DeletionSafety` 实现或记录 SR 决策要求 OpenSpec change 阶段修改 `ExecutorRegistry` 构造器以支持 nullable deletionSafety。

4. Phase 6 移除前显式检查：
   ```java
   if (!executor.isTerminated()) {
       throw new IllegalStateException("executor not terminated before remove");
   }
   registry.remove(definition.scenarioId());
   ```

**状态**: `FIXED` — 将在 `20-sr.md` 中直接修正。

---

### F04 [P1] — Bridge 包归属未定，依赖方向未记录

**处置**: **FIX**. Bridge 归属 `experiment.acquisition`，显式记录新依赖方向。

**理由**: 
- Bridge 核心职责是"聚合 → acquisition 类型"——与 `experiment.acquisition` 包的职责（acquisition report 相关类型）一致。
- 新依赖仅为纯数据类（`ManagedExecutorConfig`, `ScenarioRunOutcome`, `ScenarioDefinition`），无 mutation 授权风险。

**动作**:
1. SR §4.6 Bridge 包归属明确为 `com.zhiwu.dynamicthreadpollermanager.experiment.acquisition`。
2. SR §3 依赖方向表增补：
   ```text
   experiment.acquisition (AcquisitionReportBridge)
       ├── experiment.executor (ManagedExecutorConfig — 纯数据 record)
       └── experiment.scenario (ScenarioRunOutcome, ScenarioDefinition — 纯数据类)
   ```
3. 标注 "仅限纯数据类，不扩展到 mutation 类型"。

**状态**: `FIXED` — 将在 `20-sr.md` 中直接修正。

---

### F05 [P2] — buildObservation() 与 fromExecutorState() 逻辑重复

**处置**: `ACCEPT_AS_IS`. SR 添加推荐用法注释，不强制。

**理由**: 两处代码的语义边界不同：
- `buildObservation()` 是 runner 内部的便利方法，runner 不必依赖 `SnapshotAssembler` 的特定实现来选择采样路径。
- `fromExecutorState()` 是 `SnapshotAssembler` 的公共 API，供所有调用方使用。

消除重复是 code review 层面的优化，不是设计层面的缺陷。SR 记录推荐但不强制执行。

**动作**: SR §4.2 末尾添加注释：
> 推荐：runner 可优先使用 `sampler.sampleFromExecutorState(runId, executor.toSnapshot())` 以减少重复代码。保留 `buildObservation()` 作为备选路径（runner 内部细节，不强制）。

**状态**: `ACCEPTED` with recommendation.

---

### F06 [P2] — startedLatch 超时行为未定义

**处置**: `FIX`. SR §4.2 添加超时处理。

**动作**: SR §4.2 Phase 3 伪代码中 `startedLatch.await()` 后增补：

```java
boolean allStarted = startedLatch.await(5, TimeUnit.SECONDS);
if (!allStarted) {
    // 记录警告但不阻断采样 — 数据质量可能降低
    // 实际影响：activeCount 可能低于预期，不影响 G7-G9 门禁
}
```

**状态**: `FIXED` — 将在 `20-sr.md` 中直接修正。

---

## 3. 处置汇总

| Finding | 级别 | 处置 | 目标 |
|---|---|---|---|
| F01 | P1 | FIX — 新增 AcquisitionReportWriter 双参构造器 | 直接修正 `20-sr.md` |
| F02 | P1 | FIX — Bridge 不产出 ReadinessSummary | 直接修正 `20-sr.md` |
| F03 | P1 | FIX — Runner 移除 DeletionSafety 创建 | 直接修正 `20-sr.md` |
| F04 | P1 | FIX — Bridge 归属 acquisition 包 + 记录依赖 | 直接修正 `20-sr.md` |
| F05 | P2 | ACCEPT — 添加推荐注释 | 直接修正 `20-sr.md` |
| F06 | P2 | FIX — 添加 startedLatch 超时日志 | 直接修正 `20-sr.md` |

## 4. SR 修正清单

以下修改需在 `20-sr.md` 中执行（由 `23-sr-closure-verification.md` 验证）：

1. §4.4 增补：`AcquisitionReportWriter(Path, AcquisitionReportPaths)` 双参构造器（F01）
2. §4.6 修改：Bridge 不产出 `ReadinessSummary`，仅 4 artifact（F02）
3. §4.2 修改：Phase 1 移除 `AtomicDeletionSafety` 创建；Phase 6 显式 `isTerminated()` 检查（F03）
4. §4.6 明确：Bridge 包归属 `experiment.acquisition`（F04）
5. §3 增补：新依赖方向 `acquisition → executor`, `acquisition → scenario`（F04）
6. §4.2 增补：推荐使用 `sampleFromExecutorState()`（F05）
7. §4.2 增补：`startedLatch` 超时警告（F06）

## 5. 结论

4 个 P1 项全部 FIX（直接修改 SR 正文）。2 个 P2 项 1 FIX + 1 ACCEPT。修正后即可进入 SR closure verification 和 change decomposition。
