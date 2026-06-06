# executor-adapter-and-adjustment-evidence 流程卡死复盘

## 交付对象

- Change：`executor-adapter-and-adjustment-evidence`
- 日期：`2026-06-06`
- 阶段：实现完成后的 verify / finalize / archive 前收尾

## 计划路径

预期路径是基于 superspec 的完整机器可推进流程：

1. `proposal.md`
2. `design.md`
3. `specs/**/spec.md`
4. `tasks.md`
5. `plan.md`
6. `apply.md`
7. `verify.md`
8. `finalize.md`
9. archive

其中 verify 通过后，agent 应能根据机器可执行状态继续进入 finalize，不需要用户反复介入。

## 实际路径

当前 change 的 `.openspec.yaml` 固定为：

```yaml
schema: spec-driven
created: 2026-06-05
```

因此 OpenSpec CLI 只跟踪 `proposal`、`design`、`specs`、`tasks` 四个 artifact。
`apply.md`、`verify.md` 和 `finalize.md` 对此 change 而言只是管理变更证据文件，不是 DAG artifact。

这导致 `/opsx:continue` 无法创建 `finalize.md`，并返回 schema 中不存在 `finalize` artifact 的结论。

## 发生的问题

- 用户明确希望“基于 superspec”推进，但实际 change 使用了 `spec-driven`。
- 后续治理和 verify 文案按 superspec 的 apply / verify / finalize 流程书写，和实际 schema 不一致。
- `verify.md` 曾保留“等待用户控制 archive”“independent review”一类模糊终态，弱 agent 会把它理解成必须停下等待用户。
- 门禁脚本最初只识别 `Change name:`，而 `current-state.md` 使用 `Authorized OpenSpec change:`，导致 pre-finalize guard 真实失败过。
- 修复门禁后，没有立即识别 schema 错配，继续建议 `/opsx:continue`，造成二次卡死。
- archive 文件移动和 main spec 同步完成后，`docs/00-project/current-state.md` 仍保留 `EXECUTION_AUTHORIZED` 和 active change 授权，导致 post-archive guard 失败。

## 影响

- agent 反复停在 closeout 阶段，不能自主推进。
- 用户被迫多次判断 schema、finalize、archive 的细节，违背“不需要用户过多参与”的目标。
- 交付节奏被流程问题阻断，而不是被实现质量问题阻断。

## 根因

1. **schema 事实未作为首要检查项**
   开始 closeout 前没有强制读取 `.openspec.yaml`，导致按 superspec 假设推进。

2. **项目默认 schema 与 change-local schema 不一致**
   仓库默认 `openspec/config.yaml` 是 `superspec`，但当前 change 自身覆盖为 `spec-driven`。
   实际执行时 change-local schema 才是 CLI 依据。

3. **文档终态缺少机器可执行字段**
   `verify.md` 早期只写自然语言结论，没有稳定的 `Agent next action` 和
   `User action required before next agent action` 字段。

4. **“用户控制 archive”表达过宽**
   该表达没有区分“需要用户验收/授权”与“agent 可以继续执行提交或 finalize”，导致弱 agent 选择停止。

5. **历史兼容路径没有标准化**
   对已存在的 `spec-driven` change，没有明确的一次性兼容收尾规则。

6. **archive 后状态同步没有固化为强制动作**
   上一次流程修正覆盖了 pre-finalize、schema 识别和 verify 终态，但没有把
   `archive -> current-state 同步 -> version 状态同步 -> post-archive guard -> 提交`
   做成不可跳过步骤。结果 agent 仍可能把 archive 文件移动误判为交付闭环。

## 本次已采纳的修复

- 为当前 change 手工补齐 `finalize.md`，明确这是 `spec-driven` 历史兼容收尾，不再要求 `/opsx:continue` 生成 finalize。
- 更新当前 `verify.md`，把下一步改为提交当前实现、证据、finalize 和治理修正，不再指向 `/opsx:continue`。
- 更新 `openspec/config.yaml`，规定新 capability change 必须使用 `schema: superspec`。
- 更新 `docs/02-harness/managed-change-standard.md`，把 schema 强制规则写入中文管理标准。
- 更新 superspec `verify.md` 模板，加入 `Machine-Actionable Closeout State`。
- 更新 `scripts/openspec-archive-guard.ps1`，让 pre-finalize guard 同时兼容 `Change name:` 和 `Authorized OpenSpec change:`。
- archive 后补充同步 `docs/00-project/current-state.md`，退出 `EXECUTION_AUTHORIZED`，移除 active change，并记录归档目录和同步后的 main spec。
- 更新 `docs/04-development/versions/v0.5.0/README.md`，把版本状态改为 `IMPLEMENTED` 和 `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`。
- 在 `docs/02-harness/managed-change-standard.md` 和 `openspec/config.yaml` 中增加 archive 后强制同步规则。

## 后续禁止项

- 禁止新建 capability change 使用 `schema: spec-driven`。
- 禁止在未读取 `.openspec.yaml` 的情况下决定 closeout 命令。
- 禁止对 `spec-driven` change 声称 `/opsx:continue` 可以生成 `finalize.md`。
- 禁止在 `verify.md` 同时保留历史失败结论和当前通过结论作为并列终态。
- 禁止用“user-controlled archive”替代明确的 `Agent next action`。
- 禁止把 OpenSpec 文件移动或 `openspec validate` 通过当成 archive 完成；post-archive guard 通过前不得称为闭环。
- 禁止在 `openspec list --json` 已无 active change 时，让 `current-state.md` 继续声明 active change。

## 后续 agent 必须遵守

1. 创建新 change 时必须验证 `.openspec.yaml` 为 `schema: superspec`。
2. 继续既有 change 时必须先读取 `.openspec.yaml`。
3. 若 schema 是 `superspec`，按 DAG 运行 apply / verify / finalize。
4. 若历史 schema 是 `spec-driven`，采用兼容收尾：手工补真实 `finalize.md`，记录原因，不重试 `/opsx:continue`。
5. `verify.md` 必须包含机器可执行 closeout 状态，并明确是否需要用户动作。
6. archive 后必须立即同步 `current-state.md` 和版本状态文件。
7. post-archive guard 不通过时，必须继续修正状态同步，不得转入新需求。

## 仍未解决的问题

- `AGENTS.md` 中仍有历史授权状态描述，后续应单独治理，避免与 `docs/00-project/current-state.md` 产生认知噪声。

## 结论

本次问题是流程设计和 schema 事实校验缺失造成的，不是实现失败。
后续统一使用 `superspec`，并把 `.openspec.yaml` 检查作为 closeout 前置条件，才能避免同类卡死再次发生。
