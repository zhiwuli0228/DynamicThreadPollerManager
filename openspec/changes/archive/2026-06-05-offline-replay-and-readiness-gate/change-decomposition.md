# Change Decomposition

## Header

- Version name: `v0.4.0`
- Change identifier: `offline-replay-and-readiness-gate`
- Change purpose: 为 baseline evidence 建立只读离线 replay、敏感性比较与 mutation readiness gate，在 executor mutation 前形成结构化证据层。
- Authorized by: `docs/04-development/versions/v0.4.0/23-sr-closure-verification.md`

## 1. Scope

- In scope:
  - baseline evidence validation
  - offline replay with fixed `default` / `conservative` / `aggressive` configs
  - replay decision evidence
  - replay run/scenario summary
  - threshold sensitivity comparison
  - mutation readiness assessment
  - controlled local report artifacts
- Out of scope:
  - executor mutation
  - queue resizing
  - runtime scheduling
  - persistence / API / UI
  - new dependencies

## 2. Implementation Order

1. Analysis contracts
2. Evidence validation
3. Offline replay
4. Summary and sensitivity
5. Readiness gate
6. Report artifact and boundary verification

## 3. Verification Requirements

- 必须有 validation 负例测试。
- 必须有 replay 时间确定性测试。
- 必须有 summary 计数守恒与抖动指标测试。
- 必须有 readiness 三态判定测试。
- 必须有 analysis boundary isolation test。
- 必须运行 `openspec.cmd validate --all --json` 与 `.\mvnw.cmd test`。

## 4. Evidence Requirements

- `apply.md` 必须记录实际实现文件、测试命令、是否存在偏差。
- `verify.md` 必须逐项映射 spec -> implementation -> tests -> evidence。
- 运行输出如生成，只记录摘要路径，不提交大体量原始 evidence。

## 5. Closeout Steps

1. 更新 `tasks.md` 复选框
2. 生成 `apply.md`
3. 执行 verify 并生成 `verify.md`
4. 完成 finalize 和 archive 前，确认 `delivery-checklist.md`、`verify.md`、`current-state.md` 叙事一致

## 6. Delivery Checklist

- 本 change 使用 [change-delivery-checklist-template.md](E:/009workspace/claudecode/DynamicThreadPollerManager/docs/07-templates/change-delivery-checklist-template.md) 作为 closeout 基线。
- 具体勾选文件为 [delivery-checklist.md](E:/009workspace/claudecode/DynamicThreadPollerManager/openspec/changes/offline-replay-and-readiness-gate/delivery-checklist.md)。
- 未完成 checklist 前，不得归档。
