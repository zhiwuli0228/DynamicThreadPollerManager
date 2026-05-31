# DynamicThreadPollerManager 标杆化增强 Phase 03 执行任务书
## Delivery Framework Alignment 与 Toolchain Readiness Closure

> 执行端：Codex  
> 审核端：ChatGPT  
> 目标仓库：`https://github.com/zhiwuli0228/DynamicThreadPollerManager`  
> 目标分支：`claude_master`  
> 任务类型：项目框架闭环与工具链接入校正，不涉及 V1 功能设计或业务实现  
> GitHub 操作约定：允许并建议使用 `gh` CLI 完成远端状态核验、分支/提交查看及后续 PR 能力探测；本地文件修改、diff 与 commit 仍使用标准 `git`。

---

## 1. 已审查基线与本轮定位

### 1.1 已通过的 Phase 02 产物

远端 `claude_master` 的最新可审查提交为：

```text
9ef6594 docs: establish living architecture baseline for benchmark framework
```

该提交已完成：

- `docs/architecture/README.md`；
- 七份 Living Architecture 设计基线文档；
- `docs/bootstrap/bootstrap-ledger.md` 中权威分支修正为 `claude_master`；
- `docs/harness/project-harness.md` 向索引页迁移；
- Harness 中将能力拆分表达为候选路线，而非立即批准的 change 顺序；
- V1 Unified Design Planning Framework，明确未审核 V1 之前不得进入实现。

该提交范围仅包含 `docs/` 下 14 个文件，未修改源码、依赖或 OpenSpec/Agent 工具入口。

### 1.2 已发现的小型修正项

`docs/harness/project-harness.md` 的当前状态描述同时包含以下含义：

- “Living Architecture remains a subsequent documentation phase”；
- “Living Architecture baseline: established in Phase 02”。

两者语义冲突。本轮必须将其统一为：

```text
Living Architecture baseline: established in Phase 02 and authoritative for subsequent design planning.
```

### 1.3 本轮战略边界

用户已明确：

```text
先搭建完整框架，第一个版本的设计后续统一规划。
```

因此，本轮只完成 Delivery Framework 与工具链对齐，严禁创建功能 change 或 V1 设计正文。

---

## 2. 本轮目标

完成后，项目应具备完整的“设计之前的框架基线”：

```text
Harness Constitution            已完成
Living Architecture             已完成
Delivery Framework              本轮建立
Agent Entrypoint Alignment      本轮完成
OpenSpec/SuperSpec Context      本轮完成
Toolchain Readiness Evidence    本轮完成
V1 Unified Design               尚未开始，等待下一阶段
Business Implementation         尚未开始
```

本轮具体交付：

1. 修正 `project-harness.md` 的阶段状态矛盾。
2. 更新 `AGENTS.md`，使 Codex 读取 Harness + Architecture + Delivery 文档，并明确“当前不得自行启动 V1/change”。
3. 更新 `CLAUDE.md`，使 Claude Code 在未来实现前读取批准工件与相关架构文档，并明确当前无可实施 change。
4. 更新 `openspec/config.yaml`：
   - 保持 `schema: superspec`；
   - 注入框架完成前后的准确项目摘要；
   - 按实际 SuperSpec artifact IDs 添加少量高价值 rules；
   - 通过 OpenSpec 验证证明 rules key 有效。
5. 创建 `docs/delivery/`，记录工具链 readiness、分支/评审生命周期、框架完成门禁。
6. 创建或更新根目录 `README.md`，提供项目状态与文档导航，不宣传未实现能力。
7. 使用 `gh` 对远端分支与推送结果做事实核验。
8. 验证、提交并 push 到 `origin/claude_master`。

---

## 3. 严格修改范围

## 3.1 允许创建或修改

```text
docs/harness/project-harness.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
docs/delivery/README.md
docs/delivery/00-toolchain-readiness-and-command-map.md
docs/delivery/01-branch-change-and-review-lifecycle.md
docs/delivery/02-framework-completion-gate.md
docs/bootstrap/bootstrap-ledger.md
README.md
```

若 `README.md` 当前不存在，则创建；若已存在，保留有效内容并增加导航/状态说明。

## 3.2 只读读取，不得修改

```text
docs/harness/00-project-constitution.md
docs/harness/01-domain-and-experiment-scope.md
docs/harness/02-architecture-and-dependency-rules.md
docs/harness/03-engineering-and-testing-rules.md
docs/harness/04-ai-delivery-workflow.md
docs/harness/05-change-classification-and-gates.md
docs/architecture/**
openspec/schemas/superspec/**
.codex/**
.claude/**
```

说明：`.codex/` 与 `.claude/` 中 OpenSpec 生成的 commands/skills 是否需要刷新，必须作为本轮检查结论记录；**不得在本轮自动执行 `openspec update` 导致生成资产大面积变化**。若确认后续必须刷新，列入下一阶段前置动作，由 ChatGPT 审核后单独授权。

## 3.3 严禁修改或创建

```text
pom.xml
src/main/**
src/test/**
openspec/changes/**
openspec/specs/**
```

## 3.4 严禁执行

- 不创建任何 `/opsx:new` 或功能 change。
- 不执行 `/opsx:apply`、`/opsx:verify`、`/opsx:archive`。
- 不规划具体 V1 capability envelope。
- 不决定 V1 是一个 change 还是多个 changes。
- 不添加 Java/Spring 依赖。
- 不新增业务代码、测试代码或空包骨架。
- 不引入 CI、部署、前端、Redis、Kafka、数据库、认证。

---

## 4. 执行前读取与远端确认

## 4.1 使用 `gh` 核验远端事实

先执行：

```powershell
gh auth status
gh repo view zhiwuli0228/DynamicThreadPollerManager --json nameWithOwner,defaultBranchRef,url
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.name + " " + .commit.sha'
gh api repos/zhiwuli0228/DynamicThreadPollerManager/commits/claude_master --jq '.sha + " " + .commit.message'
```

要求：

- `gh auth status` 若未登录但公开只读 API 可执行，不阻断文档修改；记录为后续 push/PR 能力风险。
- 若无法读取远端 `claude_master`，返回 `BLOCKED_REMOTE_BASELINE_UNAVAILABLE`。
- 记录远端起始 SHA，预计应对应 Phase 02 最新提交 `9ef6594...`；若已存在更新，以真实最新 SHA 为准并重新审阅影响范围。

## 4.2 同步本地分支

```powershell
git switch claude_master
git pull --ff-only origin claude_master
git status --short --branch
git log --oneline --decorate -8
```

若本地存在不能解释的未提交变更，返回 `BLOCKED_DIRTY_WORKTREE`，不得覆盖。

## 4.3 读取框架资产

完整读取：

```text
docs/bootstrap/bootstrap-ledger.md
docs/harness/project-harness.md
docs/harness/*.md
docs/architecture/README.md
docs/architecture/*.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
openspec/schemas/superspec/schema.yaml
```

## 4.4 验证现有基线

```powershell
.\mvnw.cmd test
openspec.cmd schemas
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
```

若 Maven 基线失败，不修代码，返回 `BLOCKED_BASELINE_TEST_FAILURE`。  
若 OpenSpec 校验失败，允许在本轮授权范围内修复 `openspec/config.yaml` 造成或暴露的入口/规则问题；若失败来自 schema 文件或生成 skills，停止并在结果中说明。

---

## 5. 修正 Harness 兼容入口

更新：

```text
docs/harness/project-harness.md
```

只修正状态与导航，不重新复制 Harness 正文。

其 `Current Governance Baseline` 必须明确为：

```md
## Current Governance Baseline

- Authoritative branch: `claude_master`.
- OpenSpec / SuperSpec toolchain bootstrap: completed.
- Harness Constitution: established.
- Living Architecture baseline: established in Phase 02 and authoritative for
  subsequent design planning.
- Delivery Framework alignment: established in Phase 03 once this task passes
  validation and push.
- First-version unified design: not started.
- Dynamic thread-pool business implementation: not started.
```

增加：

```md
## Delivery Framework Reading

Operational toolchain and review workflow guidance is maintained under:

- `docs/delivery/README.md`
```

保留现有的 Rule of Precedence，必要时将优先级扩展为：

```text
1. Harness defines durable governance.
2. Architecture defines living target-system design.
3. Delivery Framework defines toolchain and review mechanics.
4. A future approved OpenSpec/SuperSpec change defines bounded delivery scope.
5. Roadmaps and candidate capability lists do not prove implementation.
```

---

## 6. 更新 `AGENTS.md`：Codex 设计入口

## 6.1 文件目的

`AGENTS.md` 仅作为 Codex 的入口索引与行为边界，不复制 Harness/Architecture 全文。

## 6.2 必须包含的内容

```md
# AGENTS.md

## Project State

- Authoritative branch: `claude_master`.
- Current phase: framework construction and alignment.
- Harness Constitution and Living Architecture are established.
- V1 unified design has not started.
- No capability change may be created unless explicitly authorized by a later
  task document.

## Mandatory Reading Order

For governance or framework work:

1. `docs/harness/project-harness.md`
2. `docs/delivery/README.md`
3. Task-specific referenced files.

For V1 unified design work, only after explicitly authorized:

1. `docs/harness/project-harness.md`
2. all `docs/harness/00-*.md` through `05-*.md`
3. `docs/architecture/README.md`
4. all detailed `docs/architecture/*.md`
5. `docs/delivery/README.md`
6. `openspec/config.yaml`

For a future bounded change design:

1. Harness rules relevant to scope and gates.
2. Architecture documents relevant to the capability.
3. Delivery workflow rules.
4. `openspec/config.yaml`.
5. Existing specs and active change artifacts, if any.

## Codex Responsibility

- Codex is the design and governance execution agent.
- Codex may create or revise architecture, workflow, and future approved
  SuperSpec design artifacts only within an explicit task authorization.
- Codex does not implement application code by default.

## Scope Guardrails

- Do not begin V1 unified design during framework-alignment tasks.
- Do not create OpenSpec changes without explicit authorization.
- Do not combine multiple roadmap capabilities implicitly.
- Do not add dependencies or application code during documentation/tooling work.
- Do not modify generated OpenSpec/agent assets unless a task explicitly
  authorizes regeneration.

## GitHub and Review Source

- GitHub branch `claude_master` is the review source of truth.
- `gh` CLI may be used for remote inspection, branch verification and future
  PR operations.
- Push completed bounded documentation work directly to `claude_master` only
  when the task authorizes it.
```

---

## 7. 更新 `CLAUDE.md`：未来实现入口

## 7.1 文件目的

当前尚无实现任务；该文件必须为未来 Claude Code 实现提供准确门禁，并避免其误读 roadmap 后直接开发。

## 7.2 必须包含的内容

```md
# CLAUDE.md

## Project State

- Authoritative branch for reviewed framework assets: `claude_master`.
- Current repository state: governance and architecture framework construction.
- No V1 capability implementation is currently approved.
- Do not implement dynamic thread-pool behavior without an approved active
  OpenSpec/SuperSpec change and an explicit implementation task.

## Mandatory Reading Before Any Future Implementation

1. `docs/harness/project-harness.md`
2. `docs/harness/02-architecture-and-dependency-rules.md`
3. `docs/harness/03-engineering-and-testing-rules.md`
4. `docs/harness/04-ai-delivery-workflow.md`
5. `docs/harness/05-change-classification-and-gates.md`
6. `docs/delivery/README.md`
7. Relevant `docs/architecture/*.md` documents referenced by the approved change.
8. All approved artifacts under the active `openspec/changes/<change-name>/`.

## Implementation Gate

Implementation is authorized only when all are true:

- V1 or later design has been reviewed.
- A bounded change exists and its required design artifacts are approved.
- The task explicitly instructs Claude Code to run the implementation flow.
- Required SuperSpec/Superpowers skills have been verified as available or an
  approved fallback path is documented.

## Execution Rules

- Implement only approved tasks.
- Use tests and verification required by the active change.
- Do not expand scope, add dependencies, alter architecture boundaries, or
  begin a neighboring capability without design revision.
- Record actual verification evidence and push only under task authorization.

## Engineering Baseline

- Java 21
- Maven Wrapper
- JUnit 5 and Mockito
- No PowerMock
- Deterministic concurrency testing rules are defined in
  `docs/harness/03-engineering-and-testing-rules.md`.
```

---

## 8. 创建 Delivery Framework 文档

创建目录：

```text
docs/delivery/
├─ README.md
├─ 00-toolchain-readiness-and-command-map.md
├─ 01-branch-change-and-review-lifecycle.md
└─ 02-framework-completion-gate.md
```

---

# 8.1 `docs/delivery/README.md`

## 必须包含

```md
# Delivery Framework Index

## Purpose

This directory records how the established Harness and Living Architecture
are executed through OpenSpec, SuperSpec, Codex, Claude Code and GitHub.
It defines delivery mechanics, not product capability scope.

## Current Status

- Authoritative branch: `claude_master`.
- Harness Constitution: established.
- Living Architecture: established.
- Delivery Framework alignment: established in Phase 03 after validation.
- V1 unified design: pending.
- Implementation: not authorized.

## Documents

1. `docs/delivery/00-toolchain-readiness-and-command-map.md`
2. `docs/delivery/01-branch-change-and-review-lifecycle.md`
3. `docs/delivery/02-framework-completion-gate.md`

## Reading Rule

- Codex reads this directory when planning or executing governance/design flow.
- Claude Code reads it only when an approved implementation change reaches its
  apply/verify/finalize lifecycle.
- No delivery document approves a product capability by itself.
```

---

# 8.2 `00-toolchain-readiness-and-command-map.md`

## 目的

记录工具链真实能力与待核验项，不把“schema 文件存在”误写成“实现流已验证可运行”。

## 必须包含的事实区分

| Capability | Evidence to collect this phase | Status vocabulary |
|---|---|---|
| OpenSpec CLI installed | `openspec.cmd --version` | VERIFIED / NOT_VERIFIED |
| SuperSpec schema recognized | `openspec.cmd schemas` / schema validation | VERIFIED / FAILED |
| Codex entrypoint assets exist | inspect `.codex/skills/` only | PRESENT / ABSENT |
| Claude entrypoint assets exist | inspect `.claude/` only | PRESENT / ABSENT |
| `gh` usable for remote inspection | `gh auth status`, `gh repo view` or public API | VERIFIED / PARTIAL / FAILED |
| Superpowers skills required by apply | inspect actual Claude Code available skills/config paths without modifying them | VERIFIED / NOT_VERIFIED / BLOCKED |
| Generated skills need refresh after context change | compare behavior/CLI guidance; do not refresh this phase | REQUIRED_LATER / NOT_REQUIRED / UNDETERMINED |

## SuperSpec v4 要记录的关键流程

从实际 `openspec/schemas/superspec/schema.yaml` 读取并记录：

```text
brainstorm -> proposal -> specs -> tasks -> plan -> apply -> verify -> finalize
design is optional but expected for non-trivial architecture-sensitive change.
```

记录 apply 依赖的技能：

```text
superpowers:using-git-worktrees
superpowers:subagent-driven-development
superpowers:test-driven-development (transitively expected)
superpowers:requesting-code-review (transitively expected)
superpowers:executing-plans (fallback only)
```

### 重要限制

本轮只核验“是否可用/可定位”，不运行一次虚假的 apply，不创建测试 change，不改 `.claude/`/`.codex/`。

## Command Map

列出当前 Windows 环境后续优先使用的命令形式，例如：

```powershell
openspec.cmd --version
openspec.cmd schemas
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
gh auth status
gh repo view zhiwuli0228/DynamicThreadPollerManager
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master
```

OpenSpec slash 命令仅记录已生成能力，不执行：

```text
/opsx:new
/opsx:continue
/opsx:apply
/opsx:verify
/opsx:archive
```

---

# 8.3 `01-branch-change-and-review-lifecycle.md`

## 目的

定义当前框架文档工作与未来能力 change 的分支、提交、远端审查方式，并纳入 `gh`。

## 必须包含两类流程

### A. Framework / Governance Work（当前适用）

```text
Work on claude_master
  -> change only authorized framework files
  -> run validation
  -> commit with docs/chore message
  -> push origin claude_master
  -> ChatGPT reviews remote commit through GitHub
```

适用范围：

- Harness；
- Architecture；
- Delivery Framework；
- Agent/OpenSpec context alignment；
- Bootstrap ledger。

### B. Future V1 Capability Work（当前未授权，仅定义机制）

SuperSpec v4 推荐 feature branch 作为 change 的规范起点。因此未来 V1 被批准后，流程应记录为候选机制：

```text
Approved V1 unified design
  -> create a dedicated feature/spec branch from claude_master
  -> Codex produces bounded SuperSpec design artifacts on that branch
  -> optional pre-review PR using gh
  -> ChatGPT/user approves design
  -> Claude Code executes apply in SuperSpec-managed worktree
  -> verify and finalize
  -> gh-backed PR/review or approved closeout path
  -> merge accepted result toward claude_master
```

必须明确：

- 当前 `claude_master` 是集成/验收基线，不应作为未来 SuperSpec feature apply 的直接开发分支，除非后续明确选择 manual escape hatch；
- 具体 branch naming、PR 是否强制、merge policy 在 V1 design closure 时确认；
- `gh` 可用于创建/查看 PR 与提交审核；
- 本文定义流程机制，不创建分支或 PR。

---

# 8.4 `02-framework-completion-gate.md`

## 目的

明确“什么时候框架已经完整到可以启动 V1 统一设计”，防止 Phase 03 完成后直接误入实现。

## 必须包含检查表

```md
# Framework Completion Gate

## Gate A - Durable Governance
- [ ] Harness index points to structured constitution.
- [ ] Authoritative branch is consistently `claude_master`.
- [ ] Change classification and scope gates are explicit.

## Gate B - Living Architecture
- [ ] Architecture index exists.
- [ ] Context, package boundaries, executor model, scheduling model,
      observability, evolution boundary and V1 planning framework exist.
- [ ] Documents distinguish target design from implementation status.

## Gate C - Delivery Framework
- [ ] AGENTS.md reads the new framework assets.
- [ ] CLAUDE.md prohibits implementation without approved change.
- [ ] `openspec/config.yaml` references Harness, Architecture and Delivery
      framework with concise context.
- [ ] OpenSpec/SuperSpec validation passes.
- [ ] Superpowers readiness is recorded truthfully.
- [ ] GitHub/gh remote review method is documented.

## Gate D - V1 Entry Decision
- [ ] No product capability change exists before V1 design authorization.
- [ ] V1 unified design task may be issued only after Gates A-C are reviewed
      from the remote `claude_master` branch.
```

Codex 必须在本阶段结尾对该清单填入真实结果。无法证明的项目保持 `[ ]` 并解释原因，禁止虚假勾选。

---

## 9. 更新 `openspec/config.yaml`

## 9.1 原则

OpenSpec 项目配置用于向工件生成和执行流程注入高杠杆上下文，不应复制 Harness 或 Architecture 全文。

保持：

```yaml
schema: superspec
```

## 9.2 推荐 `context` 内容

按实际 YAML 格式更新为以下含义；允许根据真实文件换行调整，但不得扩大为长文档：

```yaml
context: |
  Project: DynamicThreadPollerManager, a benchmark-oriented Java 21 and Spring Boot 4.0.6 exploratory project for dynamic thread-pool and scheduling design.
  Authoritative branch: claude_master is the reviewed governance and integration baseline.
  Current state: Harness Constitution and Living Architecture are established; Delivery Framework is being aligned; V1 unified design and business implementation have not started.
  Delivery roles: ChatGPT provides master design and remote review; Codex creates approved design/governance artifacts; Claude Code implements only approved bounded changes.
  Design boundary: target capabilities include managed executors, scheduling reconfiguration, observation and experiment scenarios, but no V1 scope is approved until unified design review.
  Technology boundary: Redis, Kafka, database, frontend, authentication, multi-node deployment and virtual-thread mode require explicit future approval.
  Architecture boundary: api -> application -> domain; infrastructure supplies adapters; domain must not depend on transport DTOs or infrastructure clients.
  Engineering boundary: Java 21, Maven Wrapper, JUnit 5 and Mockito, no PowerMock, deterministic concurrency testing, no unrelated refactoring.
  Required references: read docs/harness/project-harness.md, docs/architecture/README.md and docs/delivery/README.md before creating or applying an approved change.
```

## 9.3 Artifact Rules

从实际 `openspec/schemas/superspec/schema.yaml` 确认 artifact IDs。当前远端 schema 中可见的 IDs 为：

```text
brainstorm
proposal
design
specs
tasks
plan
apply
verify
finalize
```

添加少量 rules，重点防止范围漂移。规则必须与当前 CLI 可接受的 key 匹配；修改后必须执行 OpenSpec 校验。

建议规则意图如下：

```yaml
rules:
  brainstorm:
    - State whether this work is framework-only, V1 unified design, or an approved bounded capability change.
    - Do not infer approved V1 scope from the architecture roadmap.
  proposal:
    - Declare included scope, excluded scope, affected architecture documents, and whether any dependency or boundary change is proposed.
    - Reject proposals that silently combine unrelated capabilities.
  design:
    - Reference relevant Harness and Living Architecture documents.
    - Identify any long-lived architecture decision requiring documentation update or ADR evaluation.
  specs:
    - Define observable and testable scenarios, including negative or failure cases for concurrency-sensitive behavior.
  tasks:
    - Keep tasks within approved capability scope; do not include unapproved middleware, frontend, authentication, or unrelated refactoring.
  plan:
    - Include verification commands and scope checks before implementation completion.
```

### Apply / Verify / Finalize Rules

当前 OpenSpec 版本是否支持为 `apply`、`verify`、`finalize` 注入项目 rules，必须通过实际 CLI 验证决定：

- 若支持并校验通过，可添加：
  - `apply`: 仅实现批准 tasks，读取相关 Architecture 与 Delivery 文件，禁止扩范围。
  - `verify`: 验证测试结果、文件范围、未批准依赖与 change 验收条件。
  - `finalize`: 使用 `gh`/Git 流程记录实际分支与审查状态。
- 若添加后校验不通过，撤回这些运行期 rules，只保留已验证可接受的设计工件 rules，并在 readiness 文档记录限制。

禁止修改 `openspec/schemas/superspec/schema.yaml` 来迎合 config。

---

## 10. 更新根目录 `README.md`

创建或更新 README，只作为项目入口，不写成宣传稿。

必须包含：

```md
# DynamicThreadPollerManager

## Status

- Project type: benchmark-oriented exploratory Spring Boot demo.
- Current stage: project framework baseline construction.
- Implemented business capabilities: none.
- V1 unified design: pending after framework gate review.
- Authoritative review branch: `claude_master`.

## Purpose

简要说明目标：探索动态受管线程池、调度重配置、观测与恢复策略，
并沉淀 Codex 设计 / Claude Code 实现 / OpenSpec-SuperSpec 管理的
AI-assisted delivery framework。

## Documentation Map

- `docs/harness/project-harness.md`
- `docs/architecture/README.md`
- `docs/delivery/README.md`
- `docs/bootstrap/bootstrap-ledger.md`
- `openspec/config.yaml`

## Workflow Boundary

明确在 V1 统一设计批准前，不应开始动态线程池业务实现或创建功能 change。
```

不得描述 REST API、监控、调度、Redis 协调等已可使用。

---

## 11. 更新 Bootstrap Ledger

更新：

```text
docs/bootstrap/bootstrap-ledger.md
```

增加 Phase 02 审查结论和 Phase 03 完成记录：

```md
## Phase 02 Review Outcome

- Remote commit reviewed: `9ef6594...`
- Result: PASS_WITH_MINOR_ALIGNMENT_REMEDIATION.
- Verified from remote:
  - Living Architecture index and seven detailed documents exist.
  - V1 unified design is explicitly deferred until framework completion.
  - Change scope remained within documentation assets.
- Minor remediation routed to Phase 03:
  - Correct the contradictory Living Architecture state wording in
    `docs/harness/project-harness.md`.

## Phase 03 - Delivery Framework Alignment

- Status: completed after validation and push.
- Output:
  - `docs/delivery/README.md`
  - `docs/delivery/00-toolchain-readiness-and-command-map.md`
  - `docs/delivery/01-branch-change-and-review-lifecycle.md`
  - `docs/delivery/02-framework-completion-gate.md`
  - updated `AGENTS.md`
  - updated `CLAUDE.md`
  - updated `openspec/config.yaml`
  - root `README.md`
```

`Status` 只能在完成校验与准备 commit 前写为 completed；校验失败时不要提交虚假状态。

---

## 12. 工具链 Readiness 核验要求

本轮必须真实执行并记录结果：

```powershell
node --version
openspec.cmd --version
openspec.cmd schemas
openspec.cmd validate --all --json
openspec.cmd schema validate superspec

gh auth status
gh repo view zhiwuli0228/DynamicThreadPollerManager --json nameWithOwner,url,defaultBranchRef
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.commit.sha'
```

## 12.1 Superpowers Readiness

因为 SuperSpec v4 的 `apply` 明确依赖 Superpowers 技能，本轮必须检查本地 Claude Code 环境中是否存在或可调用下列能力的证据：

```text
superpowers:using-git-worktrees
superpowers:subagent-driven-development
superpowers:test-driven-development
superpowers:requesting-code-review
superpowers:executing-plans
```

执行方式：

- 读取 Claude Code 可见的 skill/command 安装目录或配置；
- 若 Claude Code 提供列出 skills 的安全命令，可执行只读探测；
- 不运行 skill；
- 不安装 skill；
- 不创建假 change 验证 apply；
- 将每项结果写入 `docs/delivery/00-toolchain-readiness-and-command-map.md`：
  - `VERIFIED_PRESENT`
  - `NOT_FOUND`
  - `NOT_VERIFIABLE_FROM_CODEX_ENVIRONMENT`

若从 Codex 环境无法访问 Claude Code skills，不视为阻断 Phase 03，但框架门禁中该项不得勾选为已通过；应将“Claude Code 会话中验证 Superpowers”列为 V1 设计前必做前置项。

---

## 13. 验证与 Scope Check

执行：

```powershell
git diff --name-only
git diff --stat
.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
git status --short --branch
```

## 13.1 允许变更范围

```text
docs/harness/project-harness.md
docs/delivery/**
docs/bootstrap/bootstrap-ledger.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
README.md
```

## 13.2 必须没有变化

```text
pom.xml
src/main/**
src/test/**
docs/architecture/**
docs/harness/00-project-constitution.md
docs/harness/01-domain-and-experiment-scope.md
docs/harness/02-architecture-and-dependency-rules.md
docs/harness/03-engineering-and-testing-rules.md
docs/harness/04-ai-delivery-workflow.md
docs/harness/05-change-classification-and-gates.md
openspec/schemas/**
openspec/changes/**
openspec/specs/**
.codex/**
.claude/**
```

如发现禁止范围变化，撤销本轮造成的越界修改并重新验证；无法安全撤销时返回：

```text
BLOCKED_SCOPE_VIOLATION
```

---

## 14. Commit、Push 与远端确认

验证通过后：

```powershell
git add README.md AGENTS.md CLAUDE.md openspec/config.yaml docs/harness/project-harness.md docs/delivery docs/bootstrap/bootstrap-ledger.md
git commit -m "docs: align delivery framework and agent entrypoints"
git push origin claude_master
```

随后使用 `gh` 核查实际远端提交：

```powershell
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.commit.sha'
gh api repos/zhiwuli0228/DynamicThreadPollerManager/commits/claude_master --jq '.sha + " " + .commit.message'
```

禁止 force push，禁止顺手创建 PR，禁止进入 V1 设计。

---

## 15. 完成判定

仅当以下全部成立时返回 `COMPLETED`：

- 工作基线为远端最新 `claude_master`；
- `project-harness.md` 状态矛盾已修正；
- `AGENTS.md` 与 `CLAUDE.md` 已接入完整框架阅读入口；
- `docs/delivery/` 完整创建；
- `openspec/config.yaml` 摘要化引用 Harness、Architecture、Delivery；
- 任何配置的 artifact rules 均通过实际 OpenSpec 验证；
- Superpowers readiness 被诚实记录，不虚假判定；
- 根 README 准确反映“框架阶段，无业务实现，无 V1 批准”；
- 未创建 change、未修改源码依赖或生成资产；
- Maven 与 OpenSpec 校验通过；
- commit、push 与 `gh` 远端确认完成。

---

## 16. 最终返回格式

完成后仅返回摘要：

```text
STATUS: COMPLETED | BLOCKED | BLOCKED_PUSH | BLOCKED_SCOPE_VIOLATION | BLOCKED_BASELINE_TEST_FAILURE | BLOCKED_OPENSPEC_VALIDATION
PHASE: 03-delivery-framework-alignment-and-toolchain-readiness
REPOSITORY: DynamicThreadPollerManager
BRANCH: claude_master
START_HEAD:
END_HEAD:
PUSHED: YES | NO
GH_REMOTE_CONFIRMED: YES | NO

PHASE_02_REVIEW_REMEDIATION:
- project-harness architecture-state contradiction fixed: YES | NO

FILES_CREATED:
- ...

FILES_UPDATED:
- ...

TOOLCHAIN_READINESS:
- OpenSpec CLI: VERIFIED | FAILED
- SuperSpec schema: VERIFIED | FAILED
- Codex entrypoints: PRESENT | ABSENT
- Claude entrypoints: PRESENT | ABSENT
- gh remote inspection: VERIFIED | PARTIAL | FAILED
- Superpowers required skills: VERIFIED_PRESENT | NOT_FOUND | NOT_VERIFIABLE_FROM_CODEX_ENVIRONMENT

OPENSPEC_CONFIG:
- context aligned with framework state: YES | NO
- artifact rules configured: list keys or NONE
- rule validation: PASS | FAIL

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
- openspec/schemas changed: NO
- openspec/changes created: NO
- .codex/.claude regenerated: NO
- feature implementation added: NO
- V1 unified design started: NO

NEXT_PHASE:
- ChatGPT remote review of completed framework gate.
- If approved, issue a separate V1 Unified Design Planning task to Codex.
```

---

## 17. 本轮结束后的暂停点

本轮推送成功后，停止执行，不创建任何 change，不开始 V1 统一设计。

用户将通知 ChatGPT：

```text
Phase 03 已推送，请检查 claude_master。
```

ChatGPT 审查远端框架门禁通过后，才会输出独立的 **V1 Unified Design Planning** 执行任务书。
