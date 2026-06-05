# Change Decomposition

## Header

- Version name: `v0.5.0`
- Change identifier: `executor-adapter-and-adjustment-evidence`
- Change purpose: 建立 executor adapter contract、runtime safety gate、in-memory adjustable executor probe 和 runtime adjustment evidence，作为后续受控 mutation 能力的最小边界。
- Authorized by: `docs/04-development/versions/v0.5.0/23-sr-closure-verification.md`

## 1. Scope

- In scope:
  - scale adjustment command
  - executor state snapshot
  - runtime safety gate
  - executor adjustment adapter contract
  - in-memory adjustable executor probe
  - adjustment result/evidence
  - boundary isolation tests
- Out of scope:
  - queue resizing
  - production `ThreadPoolExecutor` integration
  - closed-loop scheduler/controller
  - persistence / API / UI
  - new dependencies
  - throughput improvement claims

## 2. Implementation Order

1. Adjustment contracts
2. Runtime safety gate
3. Adapter and in-memory probe
4. Runtime adjustment evidence
5. Boundary isolation and no-new-dependency verification

## 3. Verification Requirements

- 必须有 command deterministic id 测试。
- 必须有 no-op、rejected、failed、applied result 语义测试。
- 必须有 readiness、cooldown、方向翻转和单 run 上限 safety gate 测试。
- 必须有 runtime adjustment evidence 字段测试。
- 必须有 boundary isolation test。
- 必须运行 `openspec.cmd validate --all --json` 与 `.\mvnw.cmd test`。

## 4. Evidence Requirements

- `apply.md` 必须记录实际实现文件、测试命令和是否存在偏差。
- `verify.md` 必须逐项映射 spec -> implementation -> tests -> evidence。
- runtime adjustment evidence 不能与 `offline_replay` evidence 混淆。

## 5. Closeout Steps

1. 完成 implementation 并更新 `tasks.md`。
2. 生成 `apply.md`。
3. 运行 verify 并生成 `verify.md`。
4. finalize/archive 前确认 delivery checklist、verify、current-state 和 OpenSpec 状态一致。
5. archive 后必须完成 retrospective。

## 6. Delivery Checklist

- 本 change 使用 [change-delivery-checklist-template.md](E:/009workspace/claudecode/DynamicThreadPollerManager/docs/07-templates/change-delivery-checklist-template.md) 作为 closeout 基线。
- 具体勾选文件为 [delivery-checklist.md](E:/009workspace/claudecode/DynamicThreadPollerManager/openspec/changes/executor-adapter-and-adjustment-evidence/delivery-checklist.md)。
- 未完成 checklist 前，不得归档。
