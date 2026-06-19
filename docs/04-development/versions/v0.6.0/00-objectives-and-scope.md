# v0.6.0 目标与范围

## Header

- Version name: `v0.6.0`
- Status: `EXECUTION_AUTHORIZED`
- Current phase: `EXECUTION_AUTHORIZED`
- Requirement theme: pressure data acquisition and reproducible baseline evidence
- Current conclusion: IR review、disposition、closure verification、SR review、disposition 和 closure verification 已完成；`pressure-data-acquisition-and-baseline` 现已授权进入 OpenSpec execution

## 1. 背景

当前项目已经具备以下已归档能力：

- `scenario-runner-and-baseline`： deterministic scenario definition、steady/ramp/burst planning、fixed baseline executor、baseline scenario outcome。
- `metrics-snapshot-and-recording`： pressure snapshot、append-only evidence、summary generation、deterministic sampling path。
- `adaptive-policy-and-control-gate`： threshold policy config、policy evaluation、control gate bounds、`ScaleDecision` conversion。
- `offline-replay-and-readiness-gate`： replay evidence validation、offline policy replay、summary、threshold sensitivity、mutation readiness assessment、controlled report artifacts。
- `executor-adapter-and-adjustment-evidence`： bounded runtime adjustment command、state snapshot、safety gate、in-memory adjustable probe、runtime adjustment evidence。

这些能力为数据获取提供了组件基础，但还没有一个受管理的“压测数据获取”需求基线。当前缺口不是“马上调参”，而是明确：

- 哪些数据能证明 baseline pressure 的形态；
- 哪些数据能支撑 policy replay / readiness 判断；
- 哪些数据足以阻止或支持下一阶段 executor mutation / queue resizing 设计；
- 如何保证后续压测结果可复现、可审计、可比较。

## 2. 目标

`v0.6.0` 需求阶段聚焦以下目标：

- 定义压测数据获取的实验对象、输入矩阵和输出证据。
- 定义 scenario profile、baseline executor preset、policy config、run count、seed、step count 的最小组合。
- 定义必须采集和汇总的指标。
- 定义数据质量门禁和失败处理。
- 定义压测结果如何进入 offline replay、threshold sensitivity 和 readiness assessment。
- 定义哪些结论可以从数据中得出，哪些结论必须禁止。
- 明确后续是否需要新 OpenSpec change 来实现自动化数据获取或报告能力。

## 3. 范围内

- 压测数据获取需求。
- 可复现实验矩阵。
- 采样指标和 evidence schema 草案。
- 数据质量门禁。
- 报告输出目录和命名约定草案。
- 与现有 scenario、metrics、policy、analysis、adjustment specs 的追踪关系。
- 后续 SR / OpenSpec decomposition 的候选方向。

## 4. 范围外

- Java 源码或测试实现。
- 实际运行压测或采集新数据。
- executor mutation 实现。
- queue resizing 实现。
- 生产 `ThreadPoolExecutor` 接入。
- closed-loop controller 或 scheduler。
- persistence、REST/API/UI、外部依赖。
- 性能优化结论或 throughput improvement claim。

## 5. 成功标准草案

- IR 能说明为什么需要先获取压力数据，而不是直接进入 mutation。
- 实验计划能明确 scenario matrix、run matrix、采样指标、证据路径和质量门禁。
- 实验计划能区分 baseline pressure、offline replay、runtime adjustment evidence，不混淆证据类型。
- 实验计划能定义“数据不足时不得进入下一阶段”的阻塞规则。
- 文档能为较弱实现 agent 提供足够具体的后续 SR 输入。

## 6. 当前阶段出口

进入 IR review 前必须完成：

1. `10-ir.md` 覆盖需求来源、问题、范围、IR 条目、验收草案和风险。
2. `15-experiment-data-acquisition-plan.md` 覆盖实验矩阵、指标、数据质量、报告输出和执行前置条件。
3. `decision-log.md` 记录关键需求判断。
4. `docs/00-project/current-state.md` 与 v0.6.0 IR 草案状态一致。

进入 OpenSpec execution 前必须完成：

1. `11-ir-review.md`
2. `12-ir-review-disposition.md`
3. `13-ir-closure-verification.md`
4. `20-sr.md`
5. `21-sr-review.md`
6. `22-sr-review-disposition.md`
7. `23-sr-closure-verification.md`
8. `docs/00-project/current-state.md` 明确授权进入 `EXECUTION_AUTHORIZED`

进入实现前必须完成：

1. `openspec/changes/pressure-data-acquisition-and-baseline/` 分解包完整
2. `docs/00-project/current-state.md` 明确授权当前 OpenSpec change
