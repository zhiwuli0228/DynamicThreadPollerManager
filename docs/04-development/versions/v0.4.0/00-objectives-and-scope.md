# v0.4.0 目标与范围

## Header

- Version name: `v0.4.0`
- Status: `READY_FOR_CHANGE_DECOMPOSITION`
- Current phase: `CHANGE_DECOMPOSITION_AUTHORIZED`
- Requirement theme: baseline pressure evidence, offline policy replay, decision evidence
- Current conclusion: IR/SR review, disposition, and closure are complete; the `offline-replay-and-readiness-gate` change decomposition package is now present

## 1. 背景

当前项目已经具备：

- 实验基础模型和生命周期；
- metrics snapshot 与 evidence recording；
- deterministic scenario runner 与 fixed baseline executor；
- adaptive threshold policy 与 control gate。

但当前还缺少把 baseline scenario evidence 与 policy evaluator 串起来的可审计实验证据。若直接进入 executor mutation，无法判断 policy 阈值是否合理，也无法证明 scale-up、scale-down、hold、cap 的行为在实际压力序列上是否稳定。

## 2. 目标

`v0.4.0` 需求阶段聚焦回答：

- baseline scenario 产生的 pressure evidence 是否足以支撑 policy replay；
- policy replay 在 steady、ramp、burst 场景下会产生怎样的 decision 分布；
- control gate 是否大量 cap/hold，从而暴露配置不合理；
- 是否存在 scale up/down 抖动风险；
- 哪些数据是后续 executor adapter 和 queue resizing 设计前必须具备的输入。

## 3. 范围内

- 定义 baseline pressure evidence 的需求。
- 定义 offline policy replay 的需求。
- 定义 policy decision evidence 的需求。
- 定义最小报告字段和验收数据。
- 定义阈值敏感性分析的最小需求。
- 定义进入 executor mutation 前的决策 gate。

## 4. 范围外

- 不实现 executor mutation。
- 不实现 queue capacity resizing。
- 不修改 scenario runner 行为。
- 不引入真实压测平台。
- 不引入持久化数据库。
- 不新增 REST API、UI 或外部依赖。
- 不实现 trend detection、cooldown state 或 learned policy。

## 5. 成功标准草案

- 能说明需要采集或复用哪些 pressure snapshots。
- 能说明如何离线 replay 已实现的 policy evaluator。
- 能说明 decision evidence 至少需要包含哪些字段。
- 能说明哪些统计结果用于判断是否进入 executor mutation 设计。
- 能明确阻止“没有数据就直接做 executor mutation”。

## 6. 当前阶段出口

IR 和 SR 阶段已经完成。进入 change decomposition 前应满足：

1. `10-ir.md`。
2. `11-ir-review.md`。
3. `12-ir-review-disposition.md`。
4. `13-ir-closure-verification.md`。

上述条件已满足。`offline-replay-and-readiness-gate` change decomposition 和 OpenSpec proposal artifacts 已创建，但仍不允许 Java 实现。
