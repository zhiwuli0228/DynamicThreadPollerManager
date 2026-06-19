# Design

## Context

`v0.4.0` 已交付 read-only analysis layer，能够从 baseline evidence 生成 replay summary 和 `ReadinessAssessment`。`v0.5.0` SR 已闭环，明确第一批后续能力应只建立 executor adapter contract 和 adjustment evidence，不接真实生产线程池，不做 queue resizing。

当前相关实现事实：

- `PolicyDecision.toScaleDecision()` 只允许 `GateStatus.ACCEPTED` / `CAPPED` 且非 `HOLD` action 转为 `ScaleDecision`。
- `ScaleDecision` 位于 `experiment.model` 包，包含 timestamp、runId、currentPoolSize、proposedPoolSize 和 reasoning。
- `ReadinessAssessment` 位于 `experiment.analysis` 包，状态为 `READY`、`READY_WITH_RISK`、`NOT_READY`。
- 当前没有 `QueueCapacityController`，也没有 runtime executor adapter。

## Goals / Non-Goals

**Goals:**

- 新增 `experiment.adjustment` 子层，隔离 runtime adjustment 相关合同。
- 定义 deterministic `ScaleAdjustmentCommand`，由 `ScaleDecision` 或等价输入稳定派生。
- 定义 `ExecutorStateSnapshot`、`AdjustmentResult`、`AdjustmentEvidence` 和状态枚举。
- 定义 `RuntimeAdjustmentSafetyGate`，固定最小阻断规则。
- 定义 `ExecutorAdjustmentAdapter` contract 和 in-memory adjustable executor probe，支持确定性测试。
- 增加边界隔离测试，证明 policy/analysis/scenario 不反向依赖 adjustment。

**Non-Goals:**

- 不实现 queue resizing。
- 不接真实生产 `ThreadPoolExecutor`。
- 不实现 closed-loop scheduler/controller。
- 不修改 scenario runner、policy evaluator 或 offline replay 语义。
- 不新增依赖、REST/API/UI、persistence。
- 不声明 throughput improvement。

## Decisions

### Decision 1: command 是 adapter 的唯一 mutation 输入

Runtime adapter MUST NOT 直接消费 replay evidence 或原始 `PolicyDecision`。后续实现应使用 `ScaleAdjustmentCommand`，并保留 `sourceDecisionRef`。默认 `commandId` 稳定格式为 `<runId>:<decisionTimestamp>:<currentPoolSize>-><targetPoolSize>`。

Rationale: command 是 policy/replay 与 runtime mutation 之间的明确边界，可测试且可审计。

### Decision 2: safety gate 在 adapter 前执行

`RuntimeAdjustmentSafetyGate` 先评估 command、current state 和 readiness context，输出 allow/reject/no-op。adapter 只执行已允许的 pool size adjustment。

Rationale: 把风险阻断和 mutation 执行拆开，便于独立测试。

### Decision 3: 第一批使用 in-memory adjustable executor probe

第一批实现使用可确定性测试的 probe，而不是生产 `ThreadPoolExecutor`。

Rationale: 先证明 command/result/evidence 和 gate 语义，再进入真实 executor 集成，可以降低运行时副作用风险。

### Decision 4: queue resizing 默认延期

本 change 只允许读取 queue size/capacity state，不允许修改 queue capacity。

Rationale: 当前没有安全 queue abstraction，queue capacity mutation 与 pool size adjustment 风险不同。

## Candidate Types

候选包：`com.zhiwu.dynamicthreadpollermanager.experiment.adjustment`

候选类型：

- `ScaleAdjustmentCommand`
- `ExecutorStateSnapshot`
- `AdjustmentStatus`
- `AdjustmentFailureCode`
- `AdjustmentResult`
- `AdjustmentEvidence`
- `SafetyGateDecision`
- `RuntimeAdjustmentSafetyGate`
- `DefaultRuntimeAdjustmentSafetyGate`
- `ExecutorAdjustmentAdapter`
- `InMemoryAdjustableExecutorProbe`

## Safety Defaults

第一批 OpenSpec 固定默认值：

| 配置 | 默认值 |
| --- | --- |
| `cooldownDecisionIntervals` | `2` |
| `maxAdjustmentsPerRun` | `5` |
| `blockImmediateOppositeDirection` | `true` |
| `allowReadyWithRisk` | `false` |

## Risks / Trade-offs

- [Risk] in-memory probe 不能证明真实线程池行为 → Mitigation: 明确真实 `ThreadPoolExecutor` integration 为 non-scope。
- [Risk] readiness 被误解为实现授权 → Mitigation: spec 明确 readiness 只是 gate input，`current-state.md` 仍是执行授权源。
- [Risk] command id 格式后续需要变短 → Mitigation: 允许稳定派生替代，但必须在 OpenSpec 中说明并测试。
- [Risk] queue resizing 被顺手实现 → Mitigation: spec 和 boundary test 明确禁止 `QueueCapacityController` 和 queue capacity mutation。

## Migration Plan

1. 新增 adjustment 合同和 tests。
2. 新增 safety gate 和 in-memory adapter tests。
3. 新增 boundary isolation tests。
4. 不迁移现有 policy、analysis、scenario 行为。

Rollback: 删除 `experiment.adjustment` 包和对应测试，不影响已归档能力。

## Open Questions

- 后续真实 executor integration 是否需要单独 ADR。
- `AdjustmentEvidence` 是否只保留内存模型，还是在后续版本接入 report writer。
