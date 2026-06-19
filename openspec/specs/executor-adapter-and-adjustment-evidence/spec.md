# Executor Adapter and Adjustment Evidence Specification

## Purpose

The executor adapter and adjustment evidence capability defines a bounded runtime adjustment boundary for experiment runs. It exposes a deterministic `ScaleAdjustmentCommand` as the only mutation input to a runtime `ExecutorAdjustmentAdapter`, an `ExecutorStateSnapshot` that records pool state before and after each adjustment (with queue state exposed only as read-only observation), a `RuntimeAdjustmentSafetyGate` that blocks `NOT_READY`, unaccepted `READY_WITH_RISK`, cooldown violations, immediate opposite-direction changes, and per-run limit overruns, and an in-memory `InMemoryAdjustableExecutorProbe` used as the only adapter implementation in the first cut. Every applied, rejected, failed, or deferred adjustment produces an `AdjustmentEvidence` record with a fixed `evidenceType=runtime_adjustment`, command/source-decision references, before/requested/applied/after state, status, reason, and (when applicable) a failure code, so that runtime adjustment evidence can never be confused with offline replay evidence. The package is isolated from the scenario runner, the policy evaluator, the offline analysis layer, and any queue-capacity mutation: it MUST NOT instantiate or integrate a production `ThreadPoolExecutor`, MUST NOT define a `QueueCapacityController` or any queue capacity mutation API, and MUST NOT introduce persistence, REST/API/UI, scheduler, or external dependencies.

## Requirements

### Requirement: Scale adjustment command
系统 MUST 定义 deterministic scale adjustment command，作为 runtime executor adapter 的唯一 mutation 输入。

#### Scenario: 从可执行 scale decision 创建 command
- **WHEN** 输入包含 `runId`、`decisionTimestamp`、`currentPoolSize`、`targetPoolSize`、`reason` 和 `sourceDecisionRef`
- **THEN** 系统 MUST 创建包含 deterministic `commandId` 的 `ScaleAdjustmentCommand`

#### Scenario: 拒绝 no-op command
- **WHEN** `currentPoolSize` 等于 `targetPoolSize`
- **THEN** 系统 MUST 拒绝创建 executor-applicable mutation command 或将其标记为 `NO_OP`

#### Scenario: 保持 source decision 可追踪
- **WHEN** command 由 `ScaleDecision` 或等价 decision target 派生
- **THEN** command MUST 保留 source decision reference 和 decision timestamp

### Requirement: Executor state snapshot
系统 MUST 定义 executor state snapshot，用于在 adjustment 前后记录受控 executor 状态。

#### Scenario: 暴露 pool state
- **WHEN** adapter 读取 current state
- **THEN** snapshot MUST 至少暴露 `observedAt`、`corePoolSize` 和 `maximumPoolSize`

#### Scenario: Queue state is read-only
- **WHEN** snapshot 包含 queue state
- **THEN** queue size 或 queue capacity MUST 仅作为只读观测字段，不得授权 queue resizing

### Requirement: Runtime adjustment safety gate
系统 MUST 在 adapter 执行 mutation 前评估 runtime safety gate。

#### Scenario: Block not ready assessment
- **WHEN** readiness status 为 `NOT_READY`
- **THEN** safety gate MUST reject the adjustment command before adapter mutation

#### Scenario: Block ready with risk unless accepted
- **WHEN** readiness status 为 `READY_WITH_RISK` 且没有显式 accepted risk profile
- **THEN** safety gate MUST reject the adjustment command

#### Scenario: Enforce cooldown
- **WHEN** 最近一次 applied adjustment 之后尚未经过 `cooldownDecisionIntervals=2`
- **THEN** safety gate MUST reject the new adjustment command

#### Scenario: Block immediate opposite direction
- **WHEN** 上一次 applied adjustment 方向与当前 command 方向相反，且 `blockImmediateOppositeDirection=true`
- **THEN** safety gate MUST reject the new adjustment command

#### Scenario: Enforce per-run adjustment limit
- **WHEN** 当前 run 的 applied adjustment 数量已经达到 `maxAdjustmentsPerRun=5`
- **THEN** safety gate MUST reject additional adjustment commands

#### Scenario: Treat same target as no-op
- **WHEN** command target equals the current executor pool size
- **THEN** safety gate MUST return a no-op decision and MUST NOT call adapter mutation

### Requirement: Executor adjustment adapter
系统 MUST 定义 executor adjustment adapter contract，用于受控应用 pool size adjustment 并返回结构化 result。

#### Scenario: Apply allowed pool size adjustment
- **WHEN** safety gate allows a valid command
- **THEN** adapter MUST apply the requested pool size to the controlled executor probe and return `APPLIED`

#### Scenario: Return rejected result for invalid command
- **WHEN** command fails validation or safety gate rejects it
- **THEN** system MUST return an adjustment result with status `REJECTED` and a non-blank reason

#### Scenario: Return failed result for runtime failure
- **WHEN** adapter attempts an allowed adjustment and the controlled executor reports failure
- **THEN** system MUST return status `FAILED` with a failure code and reason

#### Scenario: Avoid production executor integration
- **WHEN** first implementation of this capability is inspected
- **THEN** it MUST NOT instantiate or integrate a production `ThreadPoolExecutor`

### Requirement: Adjustment evidence
系统 MUST 生成 runtime adjustment evidence，并与 offline replay evidence 明确区分。

#### Scenario: Record applied adjustment evidence
- **WHEN** an adjustment is applied
- **THEN** evidence MUST include `evidenceType=runtime_adjustment`、`commandId`、`runId`、`sourceDecisionRef`、`beforeState`、`requestedPoolSize`、`appliedPoolSize`、`afterState`、`status`、`reason` 和 `decisionTimestamp`

#### Scenario: Record rejected adjustment evidence
- **WHEN** an adjustment is rejected by safety gate or validation
- **THEN** evidence MUST include status `REJECTED`、reason、failure code or rejection code, and MUST preserve before state

#### Scenario: Prevent replay evidence confusion
- **WHEN** runtime adjustment evidence is created
- **THEN** it MUST NOT use `replayMode=offline_replay` and MUST NOT be written to `outputs/reports/v0.4.0/`

### Requirement: Boundary isolation
系统 MUST keep adjustment capability isolated from scenario generation, offline analysis execution, and queue resizing implementation.

#### Scenario: Keep policy package independent
- **WHEN** policy package source is inspected
- **THEN** it MUST NOT reference `experiment.adjustment` types

#### Scenario: Keep analysis package read-only
- **WHEN** analysis package source is inspected
- **THEN** it MUST NOT invoke executor adapter mutation or create runtime adjustment evidence

#### Scenario: Exclude queue resizing
- **WHEN** adjustment package source is inspected
- **THEN** it MUST NOT define or invoke `QueueCapacityController` or any queue capacity mutation API
