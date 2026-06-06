# 复盘记录

本目录记录跨 agent 协作交付中的复盘结论，以及需要影响后续工作的流程改进。

## 目的

- 保存 change 完成后的具体交付事件与问题证据。
- 将经验教训转化为可重复执行的检查项。
- 让后续 agent 在开始 closeout、archive 或交接前，有一个简短、明确的参考入口。
- 让每一个需求、版本或 bounded change 在完成后都留下正式复盘记录，而不是只保留口头总结。

## 文档

1. `docs/08-retrospectives/agent-handoff-closeout-standard.md`
2. `docs/08-retrospectives/2026-06-02-metrics-snapshot-and-recording-retrospective.md`
3. `docs/08-retrospectives/2026-06-05-offline-replay-and-readiness-gate-retrospective.md`
4. `docs/08-retrospectives/2026-06-06-executor-adapter-schema-closeout-retrospective.md`
5. `docs/08-retrospectives/2026-06-06-pressure-data-acquisition-verify-gate-retrospective.md`

## 使用规则

在以下场景必须阅读本目录：

- 继续另一个 agent 已经开始的工作。
- 验证一个已经归档的 change。
- 准备最终交接。
- 排查 `verify.md`、归档 spec、代码和当前仓库状态不一致的问题。

## 强制规则

- 每个需求、版本或 bounded change 完成后，必须新增一份复盘文档。
- 复盘文档必须在归档或收尾提交完成后尽快创建，并记录真实执行偏差。
- 如果复盘提出流程改进，必须同步到治理文档、模板或脚本，不能只停留在复盘正文中。
