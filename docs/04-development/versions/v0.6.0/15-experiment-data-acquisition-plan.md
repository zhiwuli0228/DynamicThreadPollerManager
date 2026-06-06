# v0.6.0 实验数据获取计划草案

## Header

- Document type: experiment data acquisition plan draft
- Version name: `v0.6.0`
- Status: `DRAFT`
- Current phase: `PRESSURE_DATA_ACQUISITION_REQUIREMENT_DRAFT`
- Authoring date: `2026-06-06`
- Execution status: not authorized; do not run pressure acquisition from this draft alone

## 1. 计划目标

本计划定义后续如何获取可复现的 pressure data。当前文档不执行压测，不修改代码，不创建 OpenSpec change。

目标：

- 用固定 scenario profile 获取 baseline pressure evidence。
- 用固定 baseline executor preset 保持非 adaptive 对照。
- 用固定 policy config 进行 offline replay 和 sensitivity comparison。
- 用数据质量门禁决定是否允许进入后续 SR。
- 用受控报告输出支持审阅，而不是把大型 raw evidence 默认纳入版本控制。

## 2. 输入基线

| 输入 | 来源 | 用途 |
| --- | --- | --- |
| Scenario profiles | `scenario-runner-and-baseline` spec | 覆盖 steady/ramp/burst |
| Baseline preset | `scenario-runner-and-baseline` spec | 固定 core/max/queue capacity |
| Pressure snapshots | `metrics-snapshot-and-recording` spec | baseline pressure evidence |
| Policy configs | `adaptive-policy-and-control-gate` spec | replay decision input |
| Replay/readiness rules | `offline-replay-and-readiness-gate` spec | sensitivity 和 readiness |
| Adjustment boundary | `executor-adapter-and-adjustment-evidence` spec | 后续 mutation 设计约束，不在本计划执行 |

## 3. 实验矩阵草案

### 3.1 Scenario profile 覆盖

| Profile | 目的 | 最小 run 数 | 说明 |
| --- | --- | ---: | --- |
| `STEADY` | 验证稳定压力下的 hold/scale-down 倾向 | 3 | 作为低抖动基线 |
| `RAMP` | 验证压力逐步上升时的 scale-up 倾向 | 3 | 关注阈值触发位置 |
| `BURST` | 验证突发压力下 capped/oscillation 风险 | 3 | 关注方向翻转和短期高压 |

### 3.2 Seed 与重复策略

草案默认：

| Profile | Seeds | Repetitions | 备注 |
| --- | --- | ---: | --- |
| `STEADY` | `101`, `102`, `103` | 3 | 每个 seed 一个 run |
| `RAMP` | `201`, `202`, `203` | 3 | 每个 seed 一个 run |
| `BURST` | `301`, `302`, `303` | 3 | 每个 seed 一个 run |

SR 可调整 seed，但必须保证：

- seed 固定并记录；
- 同一 profile 至少 `3` 个有效 run；
- seed 变更必须记录在 decision log；
- 不得混用未记录的随机输入。

### 3.3 Step 与 work units 草案

| Profile | Step count 草案 | Base work units 草案 | 目的 |
| --- | ---: | ---: | --- |
| `STEADY` | 12 | 100 | 观察稳定负载下的低波动压力 |
| `RAMP` | 12 | 100 | 观察递增负载触发 policy 的位置 |
| `BURST` | 12 | 100 | 观察突发压力下 capped 和 oscillation 风险 |

这些值是需求阶段草案，不是执行配置。SR 必须根据现有 scenario planner 行为确认实际可用参数。

### 3.4 Baseline executor preset 草案

每个 run 必须记录：

- `baselinePolicyId`
- `corePoolSize`
- `maximumPoolSize`
- `queueCapacity`

草案建议 baseline preset：

| Field | Draft value | 说明 |
| --- | --- | --- |
| `baselinePolicyId` | `fixed-baseline-v0.6.0-draft` | 标识非 adaptive baseline |
| `corePoolSize` | 2 | SR 可调整 |
| `maximumPoolSize` | 4 | 必须大于等于 core |
| `queueCapacity` | 32 | 只作为固定容量，不做 resize |

## 4. Policy replay 矩阵草案

每个有效 baseline run 后续至少 replay 三组配置：

| Config label | 用途 | 来源 |
| --- | --- | --- |
| `default` | readiness 主判定 | existing offline replay convention |
| `conservative` | 较少 scale-up / 更保守风险比较 | existing offline replay convention |
| `aggressive` | 更频繁 scale-up / 风险上界比较 | existing offline replay convention |

要求：

- replay 只读，不调用 runtime adjustment。
- replay decision timestamp 必须等于 source snapshot timestamp。
- 每条 replay decision 必须可追踪 source run id、snapshot index、policy config label。

## 5. 必采指标

### 5.1 Run metadata

- `version`
- `runId`
- `scenarioId`
- `scenarioProfile`
- `seed`
- `stepCount`
- `baseWorkUnits`
- `baselinePolicyId`
- `corePoolSize`
- `maximumPoolSize`
- `queueCapacity`
- `environmentSummary`
- `commandLine`

### 5.1.1 Environment fingerprint

每个 run 的 `environmentSummary` 至少应覆盖：

- `osName`
- `osVersion`
- `javaVersion`
- `javaVendor`
- `cpuModel` 或等价硬件标识
- `availableProcessors`
- `maxMemory`
- `workingDirectory`

如果某项不可得，必须显式标记为 `unavailable`，不得静默省略。

### 5.2 Snapshot metrics

- `runId`
- `sampleTimestamp`
- `activeCount`
- `poolSize`
- `queueSize`
- `completedTaskCount`
- unavailable metric markers

### 5.3 Summary metrics

- `sampleCount`
- `firstSampleTimestamp`
- `lastSampleTimestamp`
- `completedStepCount`
- `totalCompletedWorkUnits`
- `evidenceCount`

### 5.4 Replay/readiness metrics

- `decisionCount`
- `skippedCount`
- `scaleUpCount`
- `scaleDownCount`
- `holdCount`
- `acceptedCount`
- `cappedCount`
- `gateHoldCount`
- `rejectedCount`
- `holdRatio`
- `cappedRatio`
- `directionFlipCount`
- `alternatingStreakMax`
- readiness status and reasons

## 6. 数据质量门禁

| Gate ID | Rule | Blocking level |
| --- | --- | --- |
| DQ-v0.6-001 | required profiles `STEADY`、`RAMP`、`BURST` 均存在 | P0 |
| DQ-v0.6-002 | 每个 profile 至少 3 个有效 run | P0 |
| DQ-v0.6-003 | 每个 run `sampleCount >= 3`，SR 可提高 | P0 |
| DQ-v0.6-004 | 同一 run 内所有 snapshot `runId` 一致 | P0 |
| DQ-v0.6-005 | snapshot timestamp 非降序 | P0 |
| DQ-v0.6-006 | run metadata 完整记录 scenario、seed、preset、environment | P0 |
| DQ-v0.6-007 | replay `decisionCount + skippedCount == evidenceCount` | P0 |
| DQ-v0.6-008 | raw evidence 未被默认纳入版本控制 | P1 |

若任一 P0 gate 失败，数据集不得用于 readiness 结论或下一阶段 mutation / queue resizing SR。

## 7. 输出 artifact 草案

建议目录：

```text
outputs/reports/v0.6.0/
```

建议文件：

| Artifact | Versioned by default | 内容 |
| --- | --- | --- |
| `run-manifest-<group>.json` | yes | run ids、scenario、seed、preset、command、environment |
| `pressure-summary-<group>.json` | yes | 每个 run 的摘要指标 |
| `replay-summary-<group>.json` | yes | replay action/gate/oscillation summary |
| `readiness-summary-<group>.md` | yes | readiness 结论、原因、阻塞项 |
| `raw-snapshots-<runId>.jsonl` | no | 原始 snapshot evidence，默认不纳入版本控制 |

### 7.1 Raw evidence retention

- raw evidence 默认只作为执行后中间产物，不进入版本控制。
- 若需要保留 raw evidence，用于 review 或后续 repeatability 检查，必须在执行前记录保留位置和清理责任。
- 若 raw evidence 超过审阅需要，必须在 versioned summary 之外另行清理，不得长期堆积在仓库根目录或未受控输出目录。
- 清理规则应在后续 SR 或实现阶段进一步自动化，但当前 IR 只要求定义原则和责任边界。

## 8. 执行前置条件

实际执行压测前必须满足：

1. IR review、disposition、closure verification 完成。
2. SR 明确是否需要新增自动化 runner/report writer。
3. 若需要新增代码，必须创建 `schema: superspec` 的 OpenSpec change。
4. `docs/00-project/current-state.md` 明确进入相应授权阶段。
5. 运行命令、输出目录、环境信息、数据清理策略已确认。

## 9. 禁止事项

- 不得在本计划阶段执行 queue resizing。
- 不得接入生产 `ThreadPoolExecutor`。
- 不得基于单次 run 声明 readiness。
- 不得把 offline replay 结论描述为 runtime mutation 收益。
- 不得把 raw evidence 大文件默认提交到仓库。
- 不得在没有 OpenSpec change 的情况下新增 runner、CLI、report writer 或测试。

## 10. 下一阶段候选

若 IR/SR 闭环确认需要实现数据获取自动化，候选 change：

- Change name draft: `pressure-data-acquisition-and-baseline`
- Required schema: `superspec`
- Likely scope: controlled pressure acquisition runner、manifest/report artifact writer、data quality validator
- Explicit non-scope: executor mutation、queue resizing、closed-loop controller、production executor integration

当前文档不授权创建该 change。
