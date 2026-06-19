## Why

项目已经具备 deterministic baseline scenario、pressure snapshot recording，以及独立的 adaptive policy evaluator，但仍缺少一个把这些证据串起来的离线分析层。没有这层能力，团队无法回答三个关键问题：默认阈值在真实 baseline evidence 上会做出什么决策、阈值敏感性是否会引发抖动、以及是否有足够证据授权下一阶段 executor mutation 设计。

这个 change 的目的，是在不触碰运行时线程池突变的前提下，补齐 offline replay、decision evidence、summary aggregation、sensitivity comparison 和 readiness gate，使下游 agent 能基于结构化证据推进后续阶段，而不是凭感觉扩展 adaptive control。

## What Changes

**Evidence validation**
- From: baseline run evidence 只能作为原始 snapshot 记录存在。
- To: 新增离线 replay 输入校验，明确 run metadata、snapshot 顺序、最小 evidence 数和 failure 语义。
- Reason: readiness 和 replay 必须建立在可验证输入上。
- Impact: 非破坏性内部新增。

**Offline policy replay**
- From: policy evaluator 只能被单点调用，缺少对整段 scenario evidence 的系统重放。
- To: 新增离线 replay 服务，使用固定三组 `ThresholdPolicyConfig` 对 snapshots 逐条产生 `ReplayDecisionEvidence`。
- Reason: 需要审计默认、保守、激进三组阈值对同一证据的决策差异。
- Impact: 非破坏性内部新增。

**Replay summary and sensitivity**
- From: 当前没有结构化的 action/gate 分布、抖动信号或配置比较结果。
- To: 新增单 run summary、scenario 聚合 summary 和 sensitivity comparison。
- Reason: 下一阶段是否值得进入 mutation 设计，取决于这些比较结果是否稳定。
- Impact: 非破坏性内部新增。

**Mutation readiness gate**
- From: 当前没有正式 gate 判断 baseline evidence 是否足够支持后续 mutation 设计。
- To: 新增 `READY` / `READY_WITH_RISK` / `NOT_READY` 判定，并固定最小 profile 完整性与风险阈值。
- Reason: 需要把“感觉可做”变成可审计的治理结论。
- Impact: 非破坏性内部新增。

**Controlled report artifacts**
- From: 没有受控输出目录，也没有证据卫生规则。
- To: 新增本地 JSON/Markdown 摘要 artifact，统一输出到 `outputs/reports/v0.4.0/`，不默认提交原始大 evidence。
- Reason: 既要保留人审阅证据，也要避免仓库污染和语义夸大。
- Impact: 非破坏性内部新增。

## Capabilities

### New Capabilities

- `offline-replay-and-readiness-gate`: baseline evidence validation、offline policy replay、summary aggregation、threshold sensitivity comparison、mutation readiness assessment 和受控报告输出。

### Modified Capabilities

- none

## Impact

- Affected code: 新增 `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/analysis/` 包及对应测试包。
- Affected APIs: 内部 Java 合同，新增 replay、summary、sensitivity、readiness 和 report writer 类型。
- Affected dependencies: none。
- Affected systems: 后续 executor adapter / queue resizing change 将以本 change 的 readiness 输出作为前置依据，但本 change 自身不产生任何 runtime mutation。
