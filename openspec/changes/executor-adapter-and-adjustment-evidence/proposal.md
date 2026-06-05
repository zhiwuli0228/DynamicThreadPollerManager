## Why

`v0.4.0` 已经提供 offline replay、sensitivity 和 readiness assessment，但项目仍没有运行时 adjustment 边界。若直接把 policy decision 接到线程池，会把 replay evidence、实现授权和实际 mutation 混在一起，并放大抖动、no-op、失败不可审计等风险。本 change 先建立 executor adapter contract、safety gate 和 adjustment evidence，让后续实现具备可测试、可审计的最小突变边界。

## What Changes

**Executor adjustment boundary**
- From: policy decision 只能转换成 `ScaleDecision`，没有 runtime adapter contract。
- To: 新增 `experiment.adjustment` 候选能力，定义 deterministic scale command、executor state snapshot、adapter interface 和 result contract。
- Reason: mutation 必须通过受控边界执行。
- Impact: 非破坏性内部新增。

**Runtime safety gate**
- From: `v0.4.0` readiness 只提供 offline design input。
- To: 新增 runtime safety gate 语义，阻断 `NOT_READY`、未接受风险、cooldown 未满足、立即反向调整和单 run 超限。
- Reason: 防止把不稳定 decision 直接应用到 executor。
- Impact: 非破坏性内部新增。

**Adjustment evidence**
- From: 只有 offline replay decision evidence。
- To: 新增 runtime adjustment evidence，记录 command、source decision、before/requested/applied/after state、status、reason 和 failure。
- Reason: 实际突变必须可审计，且不能与 replay evidence 混淆。
- Impact: 非破坏性内部新增。

**Queue resizing exclusion**
- From: queue pressure 会影响 policy，但没有 runtime queue controller。
- To: 明确本 change 不实现 queue resizing，只允许只读 queue state。
- Reason: queue capacity mutation 需要单独安全 abstraction。
- Impact: queue resizing 延期。

## Capabilities

### New Capabilities

- `executor-adapter-and-adjustment-evidence`: scale adjustment command、runtime safety gate、executor adjustment adapter contract、in-memory adjustable executor probe、adjustment result/evidence 和边界隔离。

### Modified Capabilities

- none

## Impact

- Affected code: 后续实现候选为 `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/` 及对应测试包。
- Affected APIs: 内部 Java 合同，新增 adjustment command/result/evidence 和 safety gate 类型。
- Affected dependencies: none。
- Affected systems: 不接入真实生产 `ThreadPoolExecutor`，不实现 queue resizing，不改变 scenario runner、policy evaluator 或 offline analysis 行为。
