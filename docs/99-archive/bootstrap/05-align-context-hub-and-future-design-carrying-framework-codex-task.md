# DynamicThreadPollerManager 文档承载框架整改任务书
## 对齐 DiagnoseToolPy Context Hub 结构，仅建设框架，不创建设计版本、Change 或代码

> 执行端：Codex  
> 目标仓库：`https://github.com/zhiwuli0228/DynamicThreadPollerManager`  
> 目标分支：`claude_master`  
> 参考仓库：`https://github.com/zhiwuli0228/DiagnoseToolPy/tree/claude_master`  
> 任务性质：文档目录治理、权威入口与未来设计/变更承载机制整改  
> 严格状态：当前没有开始版本设计，不允许创建任何 OpenSpec/SuperSpec change，不允许修改业务代码或依赖。

---

# 1. 参考项目经验与本项目整改目标

## 1.1 DiagnoseToolPy 可复用的经验

`DiagnoseToolPy` 已采用 `docs/README.md` 作为项目级 **Harness Context Hub**，并按任务类型要求 Agent 按需读取文档，而不是无脑加载全部文档。其目录结构已经将内容分离为：

```text
docs/
├─ 00-project/       # project brief, current state, glossary, roadmap
├─ 01-architecture/  # architecture design, module boundaries, decisions
├─ 02-harness/       # AI-assisted development governance
├─ 03-openspec/      # OpenSpec artifact rules and lifecycle
├─ 04-development/   # development/test/dependency guidance
├─ 05-domain/        # domain rules and templates
├─ 06-operations/    # operational guidance
├─ 07-templates/     # reusable templates
└─ 99-archive/       # obsolete/historical documentation
```

该结构中最值得迁移的关键原则：

1. `docs/README.md` 是统一文档入口和按任务读取策略的权威来源。
2. `docs/00-project/current-state.md` 保存当前事实，不让 README、Harness、Architecture 各自重复陈述状态。
3. `docs/01-architecture/decisions/` 承载长期架构决策。
4. `docs/02-harness/` 将 Agent 行为、上下文读取、任务执行、验证策略分开管理。
5. `docs/03-openspec/` 只规定 change 工件和生命周期，不替代项目长期设计文档。
6. `docs/07-templates/` 为未来设计与决策提供模板。
7. `docs/99-archive/` 保存历史初始化/过时资产，不让它们继续影响当前 Agent。

## 1.2 DynamicThreadPollerManager 当前问题

当前仓库已经存在 Harness、Architecture、Delivery 与 Bootstrap 资产，但目录职责存在不足：

- 文档没有统一 `docs/README.md` Context Hub 入口；
- 当前状态散落于多个文件；
- Architecture 与未来版本设计承载区未严格分离；
- 没有明确的版本设计目录和状态流转规则；
- 没有明确 ADR/关键决策目录；
- 没有明确规定：版本设计授权前，不得创建 capability change；
- 已完成的 bootstrap/phase 记录仍可能被 Agent 误读为当前执行指令。

## 1.3 本轮整改目标

在不改变任何业务实现状态的前提下，将项目文档承载框架对齐为适配本 Java Demo 的 Context Hub：

```text
docs/
├─ README.md
├─ 00-project/
├─ 01-architecture/
├─ 02-harness/
├─ 03-openspec/
├─ 04-development/
├─ 05-domain/
├─ 06-operations/
├─ 07-templates/
└─ 99-archive/
```

并建立以下不可破坏的承载关系：

```text
项目事实/路线           -> docs/00-project/
长期架构与 ADR          -> docs/01-architecture/
AI 治理规则             -> docs/02-harness/
未来 change 流程规则    -> docs/03-openspec/
版本设计承载规则        -> docs/04-development/versions/
领域术语与业务不变量    -> docs/05-domain/
运行/部署文档（预留）   -> docs/06-operations/
模板                    -> docs/07-templates/
历史过程文档            -> docs/99-archive/
具体批准后实施变更      -> openspec/changes/
已实现行为规格          -> openspec/specs/
```

---

# 2. 本轮硬性边界

## 2.1 允许工作

允许：

- 读取当前所有 `docs/**`、`AGENTS.md`、`CLAUDE.md`、`openspec/config.yaml`；
- 新建规范化 docs 目录和文档入口；
- 迁移/重组现有 Harness、Architecture、Delivery、Bootstrap 文档；
- 更新文档之间的引用；
- 更新 `AGENTS.md`、`CLAUDE.md`、`openspec/config.yaml`、根 `README.md`，使其只指向新的文档入口并禁止提前进入 change/实现；
- 更新 bootstrap ledger 记录本次目录治理迁移；
- 执行验证、commit、push 到 `claude_master`；
- 使用 `gh` 进行远端核验与 push 后确认。

## 2.2 绝对禁止

不得：

```text
- 创建 docs/04-development/versions/v1/ 或任何具体版本设计正文；
- 创建 docs/v1/；
- 创建或修改 openspec/changes/**；
- 向 openspec/specs/** 写入尚未实现行为；
- 执行 /opsx:new、/opsx:continue、/opsx:apply、/opsx:verify、/opsx:archive；
- 修改 pom.xml；
- 修改 src/main/** 或 src/test/**；
- 新增或修改 Java 依赖；
- 创建 V1 implementation mission；
- 将候选 roadmap 描述为批准范围；
- 修改 openspec/schemas/**、.codex/**、.claude/**；
- 为目录漂亮而制造大量无内容空文档。
```

当前唯一允许的推进状态为：

```text
DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY
```

---

# 3. 执行前核验

执行：

```powershell
gh auth status
gh repo view zhiwuli0228/DynamicThreadPollerManager --json nameWithOwner,url,defaultBranchRef
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.name + " " + .commit.sha'

git switch claude_master
git pull --ff-only origin claude_master
git status --short --branch
git log --oneline --decorate -10

.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
```

读取：

```text
README.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
docs/**
```

规则：

- 若发现存在已提交或未提交的 `docs/v1/**`、`openspec/changes/**` 或业务代码修改，先停止并返回 `BLOCKED_UNAUTHORIZED_DESIGN_OR_IMPLEMENTATION_PRESENT`，不得覆盖。
- 若当前工作树不干净且无法安全判断来源，返回 `BLOCKED_DIRTY_WORKTREE`。
- 若基线验证失败，返回 `BLOCKED_BASELINE_VALIDATION`，不得顺手修业务代码。

---

# 4. 目标文档结构

完成后必须形成以下结构。带 `[required content]` 的文件必须有实际内容；标注为索引的文件不得复制大段正文。

```text
docs/
├─ README.md                                        [required content]
│
├─ 00-project/
│  ├─ README.md                                    [index]
│  ├─ project-brief.md                             [required content]
│  ├─ current-state.md                             [required content]
│  ├─ glossary.md                                  [required content]
│  └─ roadmap.md                                   [required content, candidate only]
│
├─ 01-architecture/
│  ├─ README.md                                    [index]
│  ├─ system-context-and-quality-attributes.md     [migrate existing content]
│  ├─ logical-architecture-and-package-boundaries.md [migrate existing content]
│  ├─ managed-executor-domain-model.md             [migrate existing content]
│  ├─ scheduling-reconfiguration-and-recovery-model.md [migrate existing content; future only]
│  ├─ observability-and-experiment-strategy.md     [migrate existing content]
│  ├─ operational-and-evolution-boundaries.md      [migrate existing content]
│  └─ decisions/
│     └─ README.md                                 [ADR policy only; no ADR created]
│
├─ 02-harness/
│  ├─ README.md                                    [index]
│  ├─ harness-standard.md                          [compose from existing constitution/index]
│  ├─ agent-behavior.md                            [compose from AI workflow]
│  ├─ context-policy.md                            [required content]
│  ├─ task-execution-policy.md                     [compose from classification/gates]
│  ├─ verification-policy.md                       [compose from engineering/delivery]
│  └─ change-snapshot-policy.md                    [required content]
│
├─ 03-openspec/
│  ├─ README.md                                    [required content]
│  ├─ artifact-boundary.md                         [required content]
│  ├─ lifecycle-rule.md                            [required content]
│  └─ version-design-to-change-rule.md             [required content]
│
├─ 04-development/
│  ├─ README.md                                    [required content]
│  ├─ development-guide.md                         [framework-only baseline]
│  ├─ testing-guide.md                             [migrate durable testing rules]
│  └─ versions/
│     └─ README.md                                 [version design container rule only]
│
├─ 05-domain/
│  ├─ README.md                                    [index]
│  ├─ executor-domain-glossary.md                  [durable domain language, not V1]
│  └─ exploration-boundaries.md                    [what is candidate/future, not approved]
│
├─ 06-operations/
│  └─ README.md                                    [state that no operating design is authorized yet]
│
├─ 07-templates/
│  ├─ README.md                                    [index]
│  ├─ version-design-template.md                   [template only]
│  ├─ architecture-decision-record-template.md     [template only]
│  └─ change-decomposition-template.md             [template only]
│
└─ 99-archive/
   ├─ README.md                                    [archive rule]
   └─ bootstrap/                                   [migrate historical bootstrap records]
```

---

# 5. Migration Rules for Current Documents

## 5.1 `docs/bootstrap/**`

现有 bootstrap/phase 记录属于历史过程资料，不应继续作为 Agent 当前执行入口。

操作：

```text
docs/bootstrap/** -> docs/99-archive/bootstrap/**
```

要求：

- 保留原文，不改写为当前规则；
- 在 `docs/99-archive/README.md` 声明其仅供追溯，不具有当前执行授权效力；
- 其关键信息（权威分支、框架状态）应重新写入 `docs/00-project/current-state.md`。

## 5.2 `docs/architecture/**`

迁移到：

```text
docs/01-architecture/**
```

规则：

- 保留长期架构内容；
- 将 `06-v1-unified-design-planning-framework.md` 的“版本设计承载规则”抽取到 `docs/04-development/versions/README.md`；
- 删除/归档原 `06-v1-unified-design-planning-framework.md`，不得继续把 V1 规划放在长期 Architecture 主目录；
- Architecture 只写目标架构与长期边界，不批准任何版本范围。

## 5.3 `docs/harness/**`

现有 Constitution 内容迁移重组到：

```text
docs/02-harness/**
```

建议映射：

| Current Asset | Target Asset |
|---|---|
| `project-harness.md` | `docs/02-harness/README.md` + `harness-standard.md` |
| `00-project-constitution.md` | `docs/00-project/project-brief.md` + `docs/02-harness/harness-standard.md` |
| `01-domain-and-experiment-scope.md` | `docs/00-project/roadmap.md` + `docs/05-domain/exploration-boundaries.md` |
| `02-architecture-and-dependency-rules.md` | `docs/02-harness/harness-standard.md` + links to architecture |
| `03-engineering-and-testing-rules.md` | `docs/04-development/testing-guide.md` + `docs/02-harness/verification-policy.md` |
| `04-ai-delivery-workflow.md` | `docs/02-harness/agent-behavior.md` |
| `05-change-classification-and-gates.md` | `docs/02-harness/task-execution-policy.md` + `docs/03-openspec/lifecycle-rule.md` |

迁移完成后：

- 移除旧 `docs/harness/` 或在 `docs/99-archive/` 仅保存历史快照；不得保留两套并行权威规则。
- 当前权威 Harness 路径必须统一为 `docs/02-harness/`。

## 5.4 `docs/delivery/**`

其有效内容迁移并合并到：

| Current Asset | Target Asset |
|---|---|
| toolchain command/readiness | `docs/04-development/development-guide.md` 或 `docs/03-openspec/README.md` |
| branch/change/review lifecycle | `docs/03-openspec/lifecycle-rule.md` |
| framework completion gate | `docs/00-project/current-state.md` + `docs/02-harness/verification-policy.md` |

迁移后：

- 不保留 `docs/delivery/` 作为独立权威分区；
- 若为历史留痕，可整体移入 `docs/99-archive/legacy-delivery/`；
- 当前执行规则由 numbered Context Hub 目录承载。

---

# 6. Required Content Rules

## 6.1 `docs/README.md` — Project Context Hub

内容必须参考 DiagnoseToolPy 的入口思路，但适配本项目。至少包含：

```md
# DynamicThreadPollerManager Docs Entry

This directory is the project-level Harness Context Hub.

## Current Authorized State

- Authoritative branch: `claude_master`.
- Authorized work: documentation framework construction only.
- Version design: not started.
- OpenSpec capability changes: not authorized.
- Java business implementation: not started and not authorized.

## Reading Policy

Do not read every document blindly. Read documents by task type.

### Required for Every Non-trivial Governance or Design Task
1. `AGENTS.md`
2. `docs/00-project/project-brief.md`
3. `docs/00-project/current-state.md`
4. `docs/02-harness/harness-standard.md`
5. `docs/02-harness/context-policy.md`

### Architecture Work
Also read:
- `docs/01-architecture/README.md`
- relevant detailed architecture documents.

### Version Design Work
Only after explicit authorization, also read:
- `docs/04-development/versions/README.md`
- version-specific documents created under `docs/04-development/versions/<version>/`.

### OpenSpec Change Work
Only after a version design status allows change decomposition, also read:
- `docs/03-openspec/README.md`
- `docs/03-openspec/version-design-to-change-rule.md`
- the authorizing version design documents.

### Implementation Work
Only after an approved active change exists.

## Hard Rules

- No version design document may be created in this framework-only task.
- No OpenSpec capability change may be created before version design authorizes decomposition.
- `openspec/specs/` records implemented behavior only after verified delivery.
- Architecture documents do not authorize implementation.
- Archived bootstrap and historical task documents do not authorize current work.
- No source code or dependency change is authorized in the current phase.
```

## 6.2 `docs/00-project/current-state.md`

This is the single authoritative current status file. Must state:

```text
Project stage: DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY
Authoritative branch: claude_master
Harness: being normalized into numbered Context Hub; completed after this commit
Architecture baseline: available and migrated into docs/01-architecture
Version design: NOT_STARTED
OpenSpec capability change: NOT_AUTHORIZED
Business implementation: NOT_STARTED / NOT_AUTHORIZED
Next permitted work after this task: review documentation framework completeness only
```

禁止写任何 V1 已开始或即将自动执行的说明。

## 6.3 `docs/04-development/versions/README.md`

This is the missing design carrier. Must define:

```text
docs/04-development/versions/<version>/
├─ README.md
├─ 00-objectives-and-scope.md
├─ 01-requirements-and-use-cases.md
├─ 02-solution-design.md
├─ 03-api-and-observability-design.md
├─ 04-testing-and-acceptance-design.md
├─ 05-change-decomposition-plan.md
└─ decision-log.md
```

Must define lifecycle:

```text
DRAFT
  -> BASELINED
  -> READY_FOR_CHANGE_DECOMPOSITION
  -> EXECUTION_AUTHORIZED
  -> IMPLEMENTED
  -> SUPERSEDED
```

Hard rule:

```text
No `openspec/changes/<capability>` may be created unless a version design
exists and has status `READY_FOR_CHANGE_DECOMPOSITION` or
`EXECUTION_AUTHORIZED`.
```

Current state must be explicit:

```text
No version directory exists yet. V1 design has not started.
```

## 6.4 `docs/03-openspec/version-design-to-change-rule.md`

Must clearly state:

```text
- OpenSpec/SuperSpec changes implement bounded portions of an authorized
  version design; they do not replace version design.
- `openspec/changes/**` is forbidden before version design decomposition is authorized.
- Each future change proposal must reference:
  - authorizing version design path;
  - version design status;
  - change decomposition entry;
  - included/excluded scope.
- `openspec/specs/**` is for verified implemented behavior, not roadmap,
  brainstorm or unimplemented target architecture.
- If a change modifies long-lived architecture, it must cause an update to
  docs/01-architecture/ or a decision record before closeout.
```

## 6.5 `docs/01-architecture/decisions/README.md`

Must establish ADR rules:

```text
Create an ADR only for long-lived architectural decisions, not for each small task.

ADR states:
PROPOSED -> ACCEPTED -> SUPERSEDED | REJECTED

Examples requiring ADR later:
- adopt a coordination mechanism for multi-node execution;
- change long-lived domain/layering boundary;
- select a persistence strategy;
- introduce virtual thread execution as a supported architecture mode.

Current status:
No ADR is created in this task because no new architectural decision is being approved.
```

---

# 7. Agent and OpenSpec Entrypoint Updates

## 7.1 Update `AGENTS.md`

It must point Codex to the new authoritative entry:

```text
docs/README.md
docs/00-project/current-state.md
docs/02-harness/context-policy.md
```

It must explicitly say:

```text
Current authorized work type: DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY.
Do not create version designs, OpenSpec capability changes or application code
unless a later explicit task updates `docs/00-project/current-state.md`.
Archived task documents are non-authoritative.
```

## 7.2 Update `CLAUDE.md`

It must explicitly say:

```text
Current implementation authorization: NONE.
Do not implement code, modify dependencies, create capability changes or
execute an old implementation mission.
Only the current authoritative state in `docs/00-project/current-state.md`
and a future active authorized task may permit implementation.
```

This is especially important because older task material may still exist in history/archive.

## 7.3 Update `openspec/config.yaml`

Keep:

```yaml
schema: superspec
```

Update concise context to state:

```yaml
context: |
  Project: DynamicThreadPollerManager, a Java 21 / Spring Boot exploratory project.
  Authoritative branch: claude_master.
  Current authorized stage: DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY.
  Current state source of truth: docs/00-project/current-state.md.
  Document boundary:
  - project facts and roadmap live in docs/00-project/.
  - long-lived architecture and ADRs live in docs/01-architecture/.
  - harness governance lives in docs/02-harness/.
  - OpenSpec lifecycle rules live in docs/03-openspec/.
  - future version-level design must live in docs/04-development/versions/<version>/.
  - openspec/changes/ is forbidden until a version design explicitly authorizes decomposition.
  - openspec/specs/ records verified implemented behavior only.
  No version design, capability change or Java implementation is authorized in the current task.
```

Update only the rules necessary to prevent premature change creation:

```yaml
rules:
  brainstorm:
    - When current-state is DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY, do not create or propose a capability change.
  proposal:
    - A capability proposal must reference an authorizing version design path and status; reject it if none exists.
```

Do not remove valid existing scope/verification rules unless they directly contradict this document boundary.

Validate after editing.

## 7.4 Update Root `README.md`

Only adjust navigation and current status:

```text
Current status: documentation framework construction only.
Start reading: docs/README.md.
No version design or implementation has started.
```

Do not describe future capabilities as implemented.

---

# 8. Removal / Archive of Premature V1 Assets

During execution, check whether any of the following exist on the actual latest `claude_master`:

```text
docs/v1/**
openspec/changes/**
any V1 implementation mission document outside archive
```

Decision:

- If they do **not** exist: record `NONE_PRESENT`, continue.
- If they exist but contain only unexecuted premature planning from a rejected task:
  - move them into `docs/99-archive/rejected-v1-planning/`;
  - add a README stating `REJECTED_NOT_AUTHORIZED_FOR_EXECUTION`;
  - ensure `AGENTS.md`, `CLAUDE.md`, README and config no longer reference them as active.
- If they contain code implementation or active OpenSpec change execution evidence:
  - do not rewrite history or delete evidence;
  - return `BLOCKED_UNAUTHORIZED_DESIGN_OR_IMPLEMENTATION_PRESENT` with file list.

---

# 9. Validation and Scope Check

Run after modifications:

```powershell
git diff --name-only
git diff --stat
.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
git status --short --branch
```

Allowed modifications:

```text
README.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
docs/**
```

Must remain unchanged:

```text
pom.xml
src/main/**
src/test/**
openspec/schemas/**
openspec/changes/**       # except BLOCKED evidence; do not create/edit
openspec/specs/**
.codex/**
.claude/**
```

No V1/design/version directory may be created under `docs/04-development/versions/` beyond its README carrier specification.

---

# 10. Commit and Push

If all checks pass:

```powershell
git add README.md AGENTS.md CLAUDE.md openspec/config.yaml docs
git commit -m "docs: align context hub and future design carrying framework"
git push origin claude_master
```

Verify with `gh`:

```powershell
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.commit.sha'
gh api repos/zhiwuli0228/DynamicThreadPollerManager/commits/claude_master --jq '.sha + " " + .commit.message'
```

---

# 11. Final Response Format

```text
STATUS: COMPLETED | BLOCKED_DIRTY_WORKTREE | BLOCKED_BASELINE_VALIDATION | BLOCKED_UNAUTHORIZED_DESIGN_OR_IMPLEMENTATION_PRESENT | BLOCKED_SCOPE_VIOLATION | BLOCKED_PUSH
TASK: align-context-hub-and-future-design-carrying-framework
REPOSITORY: DynamicThreadPollerManager
BRANCH: claude_master
START_HEAD:
END_HEAD:
PUSHED: YES | NO
GH_REMOTE_CONFIRMED: YES | NO

DIAGNOSETOOLPY_PATTERN_APPLIED:
- docs context hub established: YES | NO
- numbered documentation zones established: YES | NO
- current-state source of truth established: YES | NO
- archive policy established: YES | NO

DESIGN_CARRYING_FRAMEWORK:
- architecture/decision carrier established: YES | NO
- version design carrier rules established: YES | NO
- OpenSpec change authorization rule established: YES | NO
- templates established: YES | NO

PREMATURE_ASSET_CHECK:
- docs/v1 present before task: YES | NO
- openspec/changes capability present before task: YES | NO
- rejected material archived: YES | NO | NOT_APPLICABLE
- unauthorized implementation detected: YES | NO

VALIDATION:
- .\mvnw.cmd test: PASS | FAIL
- openspec.cmd validate --all --json: PASS | FAIL
- openspec.cmd schema validate superspec: PASS | FAIL
- diff scope check: PASS | FAIL

SCOPE_CHECK:
- pom.xml changed: NO
- src/main changed: NO
- src/test changed: NO
- openspec/changes created: NO
- openspec/specs updated: NO
- V1 version design created: NO
- Java implementation added: NO

NEXT_ALLOWED_WORK:
- ChatGPT reviews the documentation framework from remote claude_master.
- No V1 design or OpenSpec change is authorized by this task.
```
