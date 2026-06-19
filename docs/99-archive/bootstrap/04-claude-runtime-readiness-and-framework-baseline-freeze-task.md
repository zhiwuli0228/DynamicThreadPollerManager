# DynamicThreadPollerManager Phase 04 执行任务书
## Claude Code Runtime Readiness Verification 与 Framework Baseline Freeze

> 执行端：Claude Code（本阶段例外：仅验证 Claude Code 自身未来实现运行环境，不实施业务代码）  
> 设计与审核端：ChatGPT  
> 目标仓库：`https://github.com/zhiwuli0228/DynamicThreadPollerManager`  
> 目标分支：`claude_master`  
> 任务类型：框架门禁闭环 / 运行环境 readiness 验证 / 状态冻结  
> GitHub 操作：允许优先使用 `gh` CLI 进行远端核验与 push 后确认  
> 关键限制：本阶段不创建 OpenSpec change，不进行 V1 设计，不实现任何 Java 业务代码。

---

## 1. 为什么本阶段由 Claude Code 执行

当前项目已完成：

- Harness Constitution；
- Living Architecture；
- Delivery Framework 文档与 Agent/OpenSpec 入口对齐。

但 Phase 03 留下一个不能由 Codex 可靠证明的门禁项：

```text
SuperSpec apply 所依赖的 Superpowers skills 是否在 Claude Code 实际运行环境中可用。
```

远端 `docs/delivery/00-toolchain-readiness-and-command-map.md` 已记录：

```text
C:\Users\18811\.claude\settings.json 显示 superpowers@claude-plugins-official enabled；
但 Codex 可见环境无法确认 apply 依赖的具体 skills；
Result: NOT_VERIFIABLE_FROM_CODEX_ENVIRONMENT。
```

远端 `docs/delivery/02-framework-completion-gate.md` 因此仍保留：

```text
[ ] Superpowers readiness is recorded truthfully.
```

此项属于未来实现端的运行能力验证，应由 Claude Code 在自身真实环境内执行只读/非业务验证，而不是继续让 Codex推测。

---

## 2. 远端已审查基线

开始任务前应核验以下远端事实；若已出现新提交，以真实远端为准并先判断是否影响本任务。

当前预期基线：

```text
Branch: claude_master
Latest reviewed commit: e67020a
Commit message: docs: align delivery framework and agent entrypoints
```

该提交已创建或更新：

```text
README.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
docs/harness/project-harness.md
docs/bootstrap/bootstrap-ledger.md
docs/delivery/README.md
docs/delivery/00-toolchain-readiness-and-command-map.md
docs/delivery/01-branch-change-and-review-lifecycle.md
docs/delivery/02-framework-completion-gate.md
```

已知当前状态：

- 无动态线程池业务实现；
- 无 V1 unified design；
- 无 `openspec/changes/**` capability change；
- `docs/delivery/02-framework-completion-gate.md` 仅因 Superpowers readiness 未被 Claude Code 实际确认而未完全闭环；
- `openspec/config.yaml` 的 current state 仍写为 `Delivery Framework is being aligned`，在门禁确认通过后应更新为 established；
- `README.md` 的 current stage 仍写为 framework baseline construction，在门禁确认通过后应更新为 framework baseline established。

---

## 3. 本阶段目标

本阶段完成以下工作：

1. 在 Claude Code 自身运行环境中核验 SuperSpec v4 实施所需的 Superpowers 能力。
2. 核验 OpenSpec / SuperSpec 命令链仍然可用。
3. 核验 `gh` 可以读取远端 `claude_master` 并在 push 后确认提交。
4. 若 Superpowers readiness 得到可靠证明：
   - 闭合 Framework Completion Gate；
   - 将项目状态更新为“完整框架基线已建立，V1 unified design 可由下一阶段启动”。
5. 若 readiness 无法证明或实际缺失：
   - 如实更新 readiness 证据；
   - 不勾选框架门禁；
   - 返回阻断状态，由 ChatGPT 决定安装/补救任务。
6. 全程不得进入功能 change、V1 设计或代码实现。

---

## 4. 允许修改范围

仅允许修改以下文件：

```text
docs/delivery/00-toolchain-readiness-and-command-map.md
docs/delivery/02-framework-completion-gate.md
docs/bootstrap/bootstrap-ledger.md
docs/harness/project-harness.md
openspec/config.yaml
README.md
```

说明：

- `openspec/config.yaml` 仅允许在 readiness 通过后，将项目当前状态由“Delivery Framework is being aligned”修正为“Delivery Framework is established; V1 unified design has not started”；不得调整 artifact rules 或引入新范围。
- `README.md` 与 `project-harness.md` 仅允许更新当前阶段状态，不得扩充未来能力范围。

---

## 5. 严格禁止范围

不得修改或创建：

```text
pom.xml
src/main/**
src/test/**
AGENTS.md
CLAUDE.md
docs/architecture/**
docs/harness/00-project-constitution.md
docs/harness/01-domain-and-experiment-scope.md
docs/harness/02-architecture-and-dependency-rules.md
docs/harness/03-engineering-and-testing-rules.md
docs/harness/04-ai-delivery-workflow.md
docs/harness/05-change-classification-and-gates.md
docs/delivery/README.md
docs/delivery/01-branch-change-and-review-lifecycle.md
openspec/schemas/**
openspec/changes/**
openspec/specs/**
.codex/**
.claude/**
```

不得执行：

- `/opsx:new`
- `/opsx:continue`
- `/opsx:ff`
- `/opsx:apply`
- `/opsx:verify`
- `/opsx:archive`
- 任何业务实现或测试编写；
- 任何依赖引入；
- 任何生成资产刷新；
- 任何强制 push；
- 任何 V1 capability 范围决定。

---

## 6. 执行前远端与本地核验

优先使用 `gh` 验证远端：

```powershell
gh auth status
gh repo view zhiwuli0228/DynamicThreadPollerManager --json nameWithOwner,url,defaultBranchRef
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.name + " " + .commit.sha'
gh api repos/zhiwuli0228/DynamicThreadPollerManager/commits/claude_master --jq '.sha + " " + .commit.message'
```

随后同步本地：

```powershell
git switch claude_master
git pull --ff-only origin claude_master
git status --short --branch
git log --oneline --decorate -8
```

读取：

```text
README.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
openspec/schemas/superspec/schema.yaml
docs/harness/project-harness.md
docs/architecture/README.md
docs/delivery/README.md
docs/delivery/00-toolchain-readiness-and-command-map.md
docs/delivery/01-branch-change-and-review-lifecycle.md
docs/delivery/02-framework-completion-gate.md
docs/bootstrap/bootstrap-ledger.md
```

若本地工作树存在无法解释的改动，停止并返回：

```text
BLOCKED_DIRTY_WORKTREE
```

---

## 7. 基础命令链验证

执行并保存真实输出摘要：

```powershell
.\mvnw.cmd test
openspec.cmd --version
openspec.cmd schemas
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
```

判断规则：

- `.\mvnw.cmd test` 失败：不得修业务代码，返回 `BLOCKED_BASELINE_TEST_FAILURE`。
- OpenSpec/SuperSpec 验证失败：不得修改 schema 或 generated skills，返回 `BLOCKED_OPENSPEC_VALIDATION` 并记录证据。
- 只有基础命令链通过后，继续验证 Claude Code / Superpowers readiness。

---

## 8. Claude Code Runtime / Superpowers Readiness 核验

## 8.1 待核验能力

根据仓库中的 SuperSpec v4 schema，本阶段必须确认以下实施相关能力在**当前 Claude Code 实际运行环境**中是否可解析/可使用：

```text
superpowers:using-git-worktrees
superpowers:subagent-driven-development
superpowers:test-driven-development
superpowers:requesting-code-review
superpowers:executing-plans
```

其中：

- `using-git-worktrees` 与 `subagent-driven-development` 为 apply 主流程依赖；
- `test-driven-development` 与 `requesting-code-review` 为 apply 主流程传递期望能力；
- `executing-plans` 为缺少 subagent 能力时的 fallback 候选。

## 8.2 验证原则

允许：

- 使用 Claude Code 当前支持的只读命令查看已安装 plugin / skills / commands；
- 读取 Claude Code 实际可访问的 plugin 或 skill 元数据；
- 通过当前 Claude Code 会话确认上述 skill 名称是否可被识别和加载为可调用能力；
- 读取用户级设置中已启用 plugin 的事实；
- 记录明确的证据路径或命令结果。

禁止：

- 为验证而创建 fake OpenSpec change；
- 为验证而运行 `/opsx:apply`；
- 为验证而修改 `.claude/**`、`.codex/**`；
- 未经授权自动安装、升级或重新配置 Superpowers/plugin；
- 仅因为 settings 中存在 plugin enabled 就推断全部所需 skills 已可使用。

## 8.3 判定标准

### `VERIFIED_PRESENT`

只有满足以下条件之一，才可将 readiness 记为 `VERIFIED_PRESENT`：

1. Claude Code 实际列出的 skill/command 能力中明确包含所有所需 skill；或
2. Claude Code 能以只读方式成功解析/载入每一个所需 skill，并能证明它们对应当前会话的可用能力。

### `PARTIALLY_VERIFIED`

满足：

- plugin 已启用；
- 但只能确认部分 required skills 可用，或无法确认全部技能的当前解析状态。

### `NOT_FOUND`

满足：

- Claude Code 明确显示所需 plugin/skills 不存在或未启用。

### `NOT_VERIFIABLE_IN_RUNTIME`

满足：

- 即便在 Claude Code 实际环境中，也没有安全、非执行的方式确认所需 skills；
- 此时不得伪造 readiness 通过。

---

## 9. Readiness 通过时的文档更新

仅当判定为 `VERIFIED_PRESENT` 时执行本节。

## 9.1 更新 `docs/delivery/00-toolchain-readiness-and-command-map.md`

将 Superpowers 结果更新为事实记录，至少包含：

```md
## Claude Code Runtime Verification - Phase 04

- Verification executor: Claude Code runtime.
- Result: `VERIFIED_PRESENT`.
- Evidence:
  - <实际命令或可访问的技能解析证据>
  - <逐项技能结论>
- No fake change, apply execution, dependency modification, or generated-asset
  regeneration was performed during verification.
```

在 readiness 表格中将：

```text
Superpowers skills required by apply: VERIFIED_PRESENT
```

## 9.2 更新 `docs/delivery/02-framework-completion-gate.md`

将 Gate C 的对应项勾选为：

```md
- [x] Superpowers readiness is recorded truthfully and verified from the
      Claude Code runtime for future approved apply execution.
```

增加结论：

```md
## Framework Baseline Decision

- Result: `FRAMEWORK_BASELINE_ESTABLISHED`.
- V1 unified design may be initiated only through a separately authorized
  Codex design task.
- This gate does not approve any capability implementation or OpenSpec change.
```

## 9.3 更新 `docs/harness/project-harness.md`

将状态改为：

```md
- Delivery Framework alignment and runtime readiness: established through Phase 04.
- First-version unified design: not started; may begin only by explicit design authorization.
```

不修改其他范围规则。

## 9.4 更新 `openspec/config.yaml`

只修改 current-state 句意，使其为：

```text
Current state: Harness Constitution, Living Architecture and Delivery Framework are established; V1 unified design and business implementation have not started.
```

不得新增、删除或重写 rules。

## 9.5 更新 `README.md`

将当前阶段改为：

```md
- Current stage: framework baseline established; V1 unified design pending authorization.
```

保持：

```md
- Implemented business capabilities: none.
- V1 unified design: not started.
```

## 9.6 更新 `docs/bootstrap/bootstrap-ledger.md`

追加：

```md
## Phase 04 - Claude Runtime Readiness and Framework Freeze

- Status: completed after validation and push.
- Verification executor: Claude Code runtime.
- Superpowers readiness: `VERIFIED_PRESENT`.
- Framework gate decision: `FRAMEWORK_BASELINE_ESTABLISHED`.
- Boundary retained:
  - No V1 unified design was started.
  - No OpenSpec capability change was created.
  - No application code or dependency was modified.
```

---

## 10. Readiness 未通过时的处理

若结果为 `PARTIALLY_VERIFIED`、`NOT_FOUND` 或 `NOT_VERIFIABLE_IN_RUNTIME`：

### 10.1 允许修改

仅更新：

```text
docs/delivery/00-toolchain-readiness-and-command-map.md
docs/delivery/02-framework-completion-gate.md
docs/bootstrap/bootstrap-ledger.md
```

### 10.2 必须记录

```md
## Claude Code Runtime Verification - Phase 04

- Verification executor: Claude Code runtime.
- Result: `<PARTIALLY_VERIFIED | NOT_FOUND | NOT_VERIFIABLE_IN_RUNTIME>`.
- Evidence:
  - <actual evidence>
- Impact:
  - Framework baseline is not frozen for implementation workflow.
  - V1 design authorization remains blocked or requires an explicit approved
    fallback decision from ChatGPT/user.
```

Gate C 保持未勾选，不得更新 README、project-harness 或 `openspec/config.yaml` 为“framework established”。

### 10.3 返回状态

```text
BLOCKED_SUPERPOWERS_READINESS
```

说明实际缺失或无法验证内容，等待下一份安装/补救/降级策略任务书。

---

## 11. Generated Asset Refresh 判断

本阶段不得修改 `.claude/**` 与 `.codex/**`。

但必须读取或核验是否存在以下风险：

```text
openspec/config.yaml 已更新 rules/context，而生成的 command/skill 资产可能需要 openspec update 才能同步。
```

将结论记录进 `docs/delivery/00-toolchain-readiness-and-command-map.md`：

```text
Generated asset refresh after config change:
- NOT_REQUIRED: only if actual tool behavior/documentation proves config is read dynamically.
- REQUIRED_LATER: if generated assets embed the stale config or command definitions.
- UNDETERMINED: if no reliable evidence is available.
```

`REQUIRED_LATER` 或 `UNDETERMINED` 不允许被静默忽略。在 readiness 通过但 refresh 仍未确定的情况下，Framework Baseline Decision 应注明：

```text
V1 design may proceed, but apply execution remains gated until generated-asset
refresh status is resolved.
```

---

## 12. Scope Check 与验证

完成修改后执行：

```powershell
git diff --name-only
git diff --stat
.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
git status --short --branch
```

允许变化仅限于：

```text
docs/delivery/00-toolchain-readiness-and-command-map.md
docs/delivery/02-framework-completion-gate.md
docs/bootstrap/bootstrap-ledger.md
docs/harness/project-harness.md              # 仅 readiness VERIFIED_PRESENT 时
openspec/config.yaml                         # 仅 readiness VERIFIED_PRESENT 时
README.md                                    # 仅 readiness VERIFIED_PRESENT 时
```

必须没有变化：

```text
pom.xml
src/main/**
src/test/**
AGENTS.md
CLAUDE.md
docs/architecture/**
docs/harness/00-project-constitution.md
docs/harness/01-domain-and-experiment-scope.md
docs/harness/02-architecture-and-dependency-rules.md
docs/harness/03-engineering-and-testing-rules.md
docs/harness/04-ai-delivery-workflow.md
docs/harness/05-change-classification-and-gates.md
docs/delivery/README.md
docs/delivery/01-branch-change-and-review-lifecycle.md
openspec/schemas/**
openspec/changes/**
openspec/specs/**
.codex/**
.claude/**
```

越界无法安全撤销时返回：

```text
BLOCKED_SCOPE_VIOLATION
```

---

## 13. Commit、Push 与 `gh` 远端确认

无论 readiness 通过还是已真实记录为未通过，只要文档修改范围合规且验证通过，均允许提交证据。

### 13.1 Readiness 通过

```powershell
git add README.md openspec/config.yaml docs/harness/project-harness.md docs/delivery/00-toolchain-readiness-and-command-map.md docs/delivery/02-framework-completion-gate.md docs/bootstrap/bootstrap-ledger.md
git commit -m "docs: verify claude runtime readiness and freeze framework baseline"
git push origin claude_master
```

### 13.2 Readiness 未通过

```powershell
git add docs/delivery/00-toolchain-readiness-and-command-map.md docs/delivery/02-framework-completion-gate.md docs/bootstrap/bootstrap-ledger.md
git commit -m "docs: record claude runtime readiness blocker"
git push origin claude_master
```

Push 后执行：

```powershell
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.commit.sha'
gh api repos/zhiwuli0228/DynamicThreadPollerManager/commits/claude_master --jq '.sha + " " + .commit.message'
```

不得 force push，不得创建 PR，不得进入 V1 设计。

---

## 14. 完成判定

### 成功闭环

仅当以下全部满足时返回 `COMPLETED_FRAMEWORK_BASELINE_ESTABLISHED`：

- Superpowers readiness 为 `VERIFIED_PRESENT`；
- Gate C 已如实勾选；
- `FRAMEWORK_BASELINE_ESTABLISHED` 已记录；
- 未开始 V1 设计、未创建 change、未改源码/依赖/生成资产；
- Maven 和 OpenSpec 验证通过；
- push 与 `gh` 远端确认完成。

### 阻断但证据已沉淀

以下情况下返回 `BLOCKED_SUPERPOWERS_READINESS`：

- readiness 不是 `VERIFIED_PRESENT`；
- 未错误关闭 framework gate；
- 阻断证据已按允许范围 commit 并 push；
- 未开始 V1 或实现。

---

## 15. 最终返回格式

```text
STATUS: COMPLETED_FRAMEWORK_BASELINE_ESTABLISHED | BLOCKED_SUPERPOWERS_READINESS | BLOCKED_DIRTY_WORKTREE | BLOCKED_BASELINE_TEST_FAILURE | BLOCKED_OPENSPEC_VALIDATION | BLOCKED_SCOPE_VIOLATION | BLOCKED_PUSH
PHASE: 04-claude-runtime-readiness-and-framework-baseline-freeze
EXECUTOR: Claude Code
REPOSITORY: DynamicThreadPollerManager
BRANCH: claude_master
START_HEAD:
END_HEAD:
PUSHED: YES | NO
GH_REMOTE_CONFIRMED: YES | NO

RUNTIME_READINESS:
- OpenSpec CLI: VERIFIED | FAILED
- SuperSpec schema: VERIFIED | FAILED
- Superpowers apply-required skills: VERIFIED_PRESENT | PARTIALLY_VERIFIED | NOT_FOUND | NOT_VERIFIABLE_IN_RUNTIME
- Generated asset refresh status: NOT_REQUIRED | REQUIRED_LATER | UNDETERMINED

FRAMEWORK_GATE:
- Gate C Superpowers item checked: YES | NO
- Framework baseline decision: FRAMEWORK_BASELINE_ESTABLISHED | NOT_ESTABLISHED

FILES_CHANGED:
- ...

VALIDATION:
- .\mvnw.cmd test: PASS | FAIL
- openspec.cmd validate --all --json: PASS | FAIL
- openspec.cmd schema validate superspec: PASS | FAIL
- git diff scope check: PASS | FAIL

SCOPE_CHECK:
- pom.xml changed: NO
- src/main changed: NO
- src/test changed: NO
- docs/architecture changed: NO
- openspec/changes created: NO
- .codex/.claude modified: NO
- V1 unified design started: NO
- business implementation added: NO

NEXT_ACTION:
- If completed: request ChatGPT review before issuing V1 Unified Design Planning task.
- If blocked: request ChatGPT toolchain remediation decision.
```

---

## 16. 停止点

完成并 push 后停止工作。不要创建 change、不要开始设计 V1、不要开始代码实现。

用户将反馈给 ChatGPT：

```text
Phase 04 已推送，请检查 claude_master。
```
