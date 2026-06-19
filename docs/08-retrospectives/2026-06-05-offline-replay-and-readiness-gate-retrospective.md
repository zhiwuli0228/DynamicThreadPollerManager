# Offline Replay and Readiness Gate 复盘

## 头信息

- 日期：`2026-06-05`
- 对象：`offline-replay-and-readiness-gate`
- 范围：`v0.4.0` bounded change 的实现、验证、归档和治理收尾
- 完成状态：已归档，主 spec 已同步，治理补强已提交
- 关联提交：
  - `e0c30c3 feat(v0.4.0): archive offline replay and readiness gate`
  - `3a5c755 docs(openspec): enforce archive guard and sync checks`
- 参与 agent：Codex + 其他实现 agent

## 计划路径与实际路径

| 项 | 计划 | 实际 | 偏差 |
| --- | --- | --- | --- |
| 需求/设计推进 | IR -> SR -> change decomposition -> execution | 按计划完成 | 基本一致 |
| 实现/验证 | 实现后一次 verify 收口 | 出现多轮返工和复查 | 验证口径前期偏重代码和测试 |
| 归档/收尾 | finalize 后直接 archive 并同步 | 先出现半完成 archive，再追加修复和治理增强 | archive 一致性未在首轮被工具硬拦截 |

## 发生了什么

`offline-replay-and-readiness-gate` 的代码实现本身较早达到可测试状态，`mvn test` 和 `openspec validate` 也能通过 change 执行阶段的检查。但在归档收尾阶段，连续暴露了几类问题：

- 主 spec 缺少 `## Purpose`，导致 archive 后的主 spec 无法通过全量 OpenSpec 校验。
- `docs/00-project/current-state.md` 没有及时从 `EXECUTION_AUTHORIZED` 回切，出现“active change 已消失，但状态文件仍声称执行中”的不一致。
- archive、主 spec、current-state 和 git worktree 没有一次性收敛到同一状态，导致出现“看起来已经 archive，实际上仓库仍未闭环”的半完成态。
- `docs/README.md` 等入口文档仍保留旧状态，说明动态状态信息在多个位置重复，容易漂移。

问题最终通过两类动作修复：

- 一类是补齐当前 change 的归档收尾，包括主 spec、current-state、finalize 叙事和最终提交。
- 另一类是补齐长期门禁，包括 superspec 模板、archive guard 脚本和验证规则。

## 根因

### 1. 归档被当作文件移动，而不是状态迁移

- 归档真正需要同时满足 archive 目录、主 spec、current-state、git clean 四个对象一致。
- 首轮执行时，只完成了其中一部分，导致归档停留在局部完成状态。

### 2. 验证口径早期偏向代码和测试

- 首轮检查更关注 `mvn test`、`tasks.md`、`apply.md`、`verify.md`。
- 但 archive 后真正关键的是主 spec 合法性和仓库全局叙事一致性。

### 3. 动态授权状态曾在多个文档中重复出现

- `current-state.md` 是权威来源，但 `docs/README.md` 也写了阶段事实。
- 当入口文档没有同步更新时，会误导后续 agent。

### 4. OpenSpec/SuperSpec 默认流程与当前主分支直推模型不完全匹配

- superspec 更偏向 branch/worktree/finalize/PR 语义。
- 本项目当前主要在 `claude_master` 直接推进，导致 finalize/archive 叙事容易和实际操作脱节。

### 5. 缺少“完成后必须复盘”的正式阶段

- 之前有复盘经验文档，但没有把复盘纳入每次完成后的标准出口条件。
- 结果是问题往往在多轮口头总结中才被系统化，而不是在流程里自动沉淀。

## 已采纳的改进

### 1. 新增 archive hard gate

- 新增 `scripts/openspec-archive-guard.ps1`。
- `post-archive` 默认严格检查：
  - `openspec validate --all --json`
  - active change 已消失
  - archive 目录存在
  - 主 spec 同时包含 `## Purpose` 和 `## Requirements`
  - `current-state.md` 不再保留执行中状态
  - `git status --short` 干净

### 2. 强化 superspec verify / finalize 模板

- `verify.md` 和 `finalize.md` 模板已补充 archive guard、main spec 结构检查、状态同步和 worktree clean 检查。
- 现在不再允许把“文件挪到 archive”当成归档完成。

### 3. 收敛动态授权源

- `docs/00-project/current-state.md` 明确作为唯一动态授权源。
- 其他配置和规则文档只保留稳定规则，不再硬编码当前阶段事实。

### 4. 将复盘提升为正式阶段

- `docs/02-harness/managed-change-standard.md` 新增 Retrospective 复盘阶段。
- `docs/07-templates/managed-change-stage-package-template.md` 增加 `60 Retrospective`。
- 新增 `docs/07-templates/retrospective-template.md`。

## 仍然存在的问题

- 当前仍主要依赖脚本和文档门禁，而不是 OpenSpec CLI 原生 hook。
- `docs/README.md` 这类入口文档虽然已修正，但未来仍可能再次漂移，需要持续约束只引用 `current-state.md` 的事实。
- branch/worktree 模型与 superspec 默认工作方式仍没有完全统一，后续若继续长期使用 `claude_master` 直推，需要进一步简化 finalize 语义。

## 后续必须遵守的行为

- 每个需求、版本或 bounded change 完成后，必须创建 `docs/08-retrospectives/<date>-<name>-retrospective.md`。
- 复盘前，不得把交付结论描述为“完全关闭”。
- 复盘中提出的流程改进，必须同步到治理文档、模板或脚本；只写在复盘正文里不算完成。
- 后续 agent 在交接、verify、archive 或 closeout 前，必须先阅读：
  - `docs/00-project/current-state.md`
  - `docs/02-harness/managed-change-standard.md`
  - `docs/08-retrospectives/README.md`
  - 必要时阅读最近一次相关复盘文档
