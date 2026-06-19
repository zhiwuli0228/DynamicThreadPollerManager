# pressure-data-acquisition-and-baseline Brainstorm

## Design Summary

本次 change 的目标不是实现新的压测能力本身，而是建立一个受控、可复现、可审计的数据获取能力边界，让后续实现 agent 可以基于统一的 `RunManifest`、`PressureSummary`、`ReplaySummary`、`ReadinessSummary` 和 `EvidenceIndex` 产出证据。

核心结论如下：

- 直接复用已归档能力作为输入基础：
  - `scenario-runner-and-baseline`
  - `metrics-snapshot-and-recording`
  - `adaptive-policy-and-control-gate`
  - `offline-replay-and-readiness-gate`
- 新 change 的职责边界应聚焦在“受控 acquisition 编排”，而不是 executor mutation、queue resizing 或生产 `ThreadPoolExecutor` 接入。
- 输出必须进入一个固定的报告目录，推荐为 `outputs/reports/v0.6.0/`。
- 原始 raw evidence 默认不进入版本控制；如需保留，必须显式记录保留位置和清理责任。
- 数据质量门禁必须在 replay / readiness 结论之前执行，且对缺失 profile、样本不足、时间戳无序、runId 不一致等情况直接阻断。

## Alternatives Considered

### Alternative A: 独立的受控 acquisition 编排层

- **Approach**: 新增一个受限的 `experiment.acquisition` 变更范围，负责读取 scenario / baseline / policy / analysis 输入，编排运行，生成 manifest 和汇总报告，不触碰 mutation。
- **Pros**:
  - 责任边界清楚。
  - 与现有 scenario/metrics/policy/analysis 模块解耦。
  - 便于后续将数据获取逻辑单独测试和审计。
- **Cons**:
  - 需要明确定义输入输出和报告契约。
  - 初期会引入一个新的 bounded capability 目录。
- **Why not chosen**: 这是最符合当前 v0.6.0 目标的方案，风险最小，且最适合作为后续实现的第一版能力边界。

### Alternative B: 直接扩展 analysis 模块承担 acquisition

- **Approach**: 让现有 `experiment.analysis` 同时负责数据获取编排、摘要生成和 readiness 判断。
- **Pros**:
  - 目录变动少。
  - 复用现有分析能力较直接。
- **Cons**:
  - 分析与采集职责混杂。
  - 容易把只读分析和运行编排耦合在一起。
  - 后续容易误把 acquisition 当成 analysis 的副作用。
- **Why not chosen**: 会模糊模块边界，不利于弱实现 agent 按职责拆分任务。

### Alternative C: 构建通用实验流水线框架

- **Approach**: 设计一个通用 experiment pipeline framework，未来所有压力/回放/评估场景都走统一框架。
- **Pros**:
  - 长期可复用性高。
  - 架构上更“整齐”。
- **Cons**:
  - 范围过大。
  - 当前 v0.6.0 只需要数据获取，不需要泛化平台。
  - 很容易滑向未授权的工程扩展。
- **Why not chosen**: 超出当前版本授权范围，且会显著拖慢交付。

## Agreed Approach

采用 **Alternative A**：建立一个受控的 `experiment.acquisition` 候选变更，专门负责 pressure data acquisition 的编排、证据组织和报告输出。

选择它的原因：

1. 与现有基础能力的输入输出边界最清晰。
2. 只覆盖当前授权需要的数据获取，不会把 executor mutation 或 queue resizing 提前带进来。
3. 对后续实现 agent 最友好，任务可以拆成 manifest、summary、evidence index、report hygiene 和 data quality validator。

## Key Decisions

- 候选 change 名称固定为 `pressure-data-acquisition-and-baseline`。
- 只做受控 acquisition，不做 runtime mutation。
- 只消费 baseline pressure evidence，不消费 runtime adjustment evidence。
- 输出目录固定为 `outputs/reports/v0.6.0/`。
- `RunManifest` 必须包含环境指纹、命令行、种子、场景、基线参数和创建时间。
- `ReadinessSummary.recommendedNextStep` 必须明确指向 `READY`、`READY_WITH_RISK` 或 `NOT_READY` 之一，不能含糊。
- raw evidence 默认不纳入版本控制。

## Open Questions

- `RunManifest` 的环境指纹字段是否需要在第一版里进一步细化为固定强制字段集。
- raw evidence 的保留目录是否要在仓库外部统一规定，还是允许 change 级别自定义。
- `EvidenceIndex` 是否需要在第一版里同时记录文件哈希，还是只记录相对路径和关联 runId。
