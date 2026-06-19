# DynamicThreadPollerManager 修正任务书
## Finalize Context Hub Authority and Architecture Change Carrying Rules

> 执行端：Codex  
> 仓库：`https://github.com/zhiwuli0228/DynamicThreadPollerManager`  
> 目标分支：`claude_master`  
> 任务性质：文档权威入口闭环 + 架构/版本/change 承载规则固化  
> 当前唯一授权状态：`DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY`  
> 严禁：版本设计、OpenSpec/SuperSpec capability change、实现 mission、Java 代码或依赖修改

---

## 1. 本轮为什么必须执行

Context Hub 新结构已经建立，以下新文档已经正确声明当前仅建设文档框架：

```text
docs/README.md
docs/00-project/current-state.md
docs/02-harness/**
docs/03-openspec/version-design-to-change-rule.md
docs/04-development/versions/README.md
docs/01-architecture/decisions/README.md
docs/99-archive/README.md
```

其中 `docs/00-project/current-state.md` 是当前执行授权的唯一事实源，并已经声明：

```text
Current stage: DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY
Version design status: not started
OpenSpec capability changes: not authorized
Java implementation status: not started and not authorized
```

但以下 active 路径仍存在冲突信息，必须立即撤销：

```text
README.md
openspec/config.yaml
docs/v1/**
docs/harness/**
docs/delivery/**
```

已知冲突：

- 根 `README.md` 仍声明 `V1 unified design planning in progress` 并引用 `docs/v1/README.md`。
- `openspec/config.yaml` 仍声明 V1 设计正在进行、未来 V1 autonomous implementation mission 可以实施，并引用旧目录。
- `docs/v1/README.md` 仍声明 V1 scope 已决定并作为 authorized V1 scope 的 source of truth。
- `docs/harness/project-harness.md` 与 `docs/delivery/README.md` 仍声明 Phase 05 V1/autonomous delivery 状态。

本轮必须使 numbered Context Hub 成为唯一 active authority。

---

## 2. 本轮最终目标

完成后必须满足：

```text
1. 根 README.md 只导航到 numbered Context Hub，不再声明 V1 active。
2. openspec/config.yaml 只承认 DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY。
3. docs/v1/** 被移动到 docs/99-archive/rejected-v1-planning/**。
4. 旧 docs/harness/** 与 docs/delivery/** 被移动到 archive，不能继续作为 active authority。
5. 若仍存在旧 docs/bootstrap/**、docs/architecture/**，确认新编号目录承接有效内容后移入 archive。
6. docs/01-architecture/decisions/README.md 补齐 Architecture Change Handling 与 ADR 命名规则。
7. docs/01-architecture/README.md 明确长期架构文档命名规则。
8. docs/04-development/versions/README.md 明确版本设计目录命名规则。
9. docs/03-openspec/version-design-to-change-rule.md 明确 change 与 specs/archive 回写规则。
10. 任何 V1、change、spec、代码、依赖、实现任务均不得创建或执行。
```

---

## 3. 严格允许修改范围

允许创建、移动或修改：

```text
README.md
openspec/config.yaml
docs/README.md
docs/00-project/current-state.md
docs/01-architecture/README.md
docs/01-architecture/decisions/README.md
docs/03-openspec/README.md
docs/03-openspec/version-design-to-change-rule.md
docs/04-development/versions/README.md
docs/99-archive/README.md
docs/99-archive/rejected-v1-planning/**
docs/99-archive/legacy-harness/**
docs/99-archive/legacy-delivery/**
docs/99-archive/legacy-bootstrap/**
docs/99-archive/legacy-architecture/**
```

只读核验，原则上不修改：

```text
AGENTS.md
CLAUDE.md
docs/02-harness/**
docs/04-development/development-guide.md
docs/04-development/testing-guide.md
docs/05-domain/**
docs/06-operations/**
docs/07-templates/**
```

仅当 `AGENTS.md` 或 `CLAUDE.md` 在实际最新分支仍包含 `docs/v1`、旧 `docs/harness`/`docs/delivery` 作为 active authority，或仍声明 V1/实现已授权时，允许最小修改以对齐：

```text
docs/README.md
docs/00-project/current-state.md
```

---

## 4. 绝对禁止范围

不得修改或创建：

```text
pom.xml
src/main/**
src/test/**
openspec/schemas/**
openspec/changes/**
openspec/specs/**
.codex/**
.claude/**
docs/04-development/versions/v*/
docs/v1/**             # 只能 git mv 到 archive，不得继续编辑为 active 内容
```

不得执行：

```text
/opsx:new
/opsx:continue
/opsx:ff
/opsx:apply
/opsx:verify
/opsx:archive
任何 V1 设计任务
任何 implementation mission
任何 Java 代码实现
任何依赖修改
```

---

## 5. 执行前核验

优先使用 `gh`：

```powershell
gh auth status
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.name + " " + .commit.sha'
gh api repos/zhiwuli0228/DynamicThreadPollerManager/commits/claude_master --jq '.sha + " " + .commit.message'
```

同步本地：

```powershell
git switch claude_master
git pull --ff-only origin claude_master
git status --short --branch
git log --oneline --decorate -12
```

执行基线校验：

```powershell
.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
```

读取并比对：

```text
README.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
docs/README.md
docs/00-project/current-state.md
docs/01-architecture/README.md
docs/01-architecture/decisions/README.md
docs/03-openspec/version-design-to-change-rule.md
docs/04-development/versions/README.md
docs/99-archive/README.md
docs/v1/README.md                         # 如存在
docs/harness/project-harness.md           # 如存在
docs/delivery/README.md                   # 如存在
```

若发现 `pom.xml` 或 `src/**` 已在远端含有本项目业务实现改动，或 `openspec/changes/**` 已含执行工件，返回：

```text
BLOCKED_UNAUTHORIZED_IMPLEMENTATION_OR_CHANGE_PRESENT
```

不得删除或覆盖证据。

---

# 6. 修正根 README.md

根 `README.md` 改为极简导航入口，必须只表达当前事实：

```md
# DynamicThreadPollerManager

## Current Status

- Project type: benchmark-oriented exploratory Spring Boot demo.
- Authoritative branch: `claude_master`.
- Current authorized stage: `DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY`.
- Version design: not started.
- OpenSpec capability changes: not authorized.
- Business implementation: not started and not authorized.

## Purpose

This project explores dynamic thread-pool management and later scheduling
governance while building a reusable AI-assisted engineering framework based
on Codex, Claude Code, OpenSpec and SuperSpec.

The current phase constructs document authority, architecture carriers and
future change governance only. It does not authorize any version scope or
implementation.

## Start Reading

1. `docs/README.md` — project Context Hub.
2. `docs/00-project/current-state.md` — single source of truth for current authority.
3. `docs/01-architecture/README.md` — long-lived architecture carrier.
4. `docs/02-harness/README.md` — AI governance and execution boundaries.
5. `docs/03-openspec/README.md` — future change lifecycle rules.
6. `docs/04-development/versions/README.md` — future version-design carrier rules.

## Current Boundary

Architecture and roadmap materials describe exploration candidates only.
No version design directory, OpenSpec capability change, implementation
mission or Java business change is authorized in the current state.
```

必须删除 active 引用：

```text
docs/v1/**
docs/harness/**
docs/delivery/**
V1 unified design in progress
autonomous implementation mission
```

---

# 7. 修正 openspec/config.yaml

保持：

```yaml
schema: superspec
```

将 `context` 替换/整理为以下语义，不保留旧 V1 active 状态：

```yaml
context: |
  Project: DynamicThreadPollerManager, a Java 21 and Spring Boot exploratory project.
  Authoritative branch: claude_master.
  Current authorized stage: DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY.
  Current state source of truth: docs/00-project/current-state.md.
  Documentation context hub: docs/README.md.

  Document authority boundaries:
  - Project facts and current state live in docs/00-project/.
  - Long-lived architecture and architecture decision records live in docs/01-architecture/.
  - Harness governance lives in docs/02-harness/.
  - OpenSpec lifecycle rules live in docs/03-openspec/.
  - Future version-level design must live in docs/04-development/versions/<version>/.
  - Historical, retired or rejected material lives in docs/99-archive/ and is non-authoritative.

  Current prohibitions:
  - No version design is authorized.
  - No openspec/changes/** capability change may be created.
  - No openspec/specs/** may be populated with unimplemented behavior.
  - No Java source, test or dependency change is authorized.
  - Archived V1 or implementation mission material must not be executed.

  Future authorization sequence:
  - Version design must first be explicitly authorized and created under docs/04-development/versions/<version>/.
  - Only a version design with status READY_FOR_CHANGE_DECOMPOSITION or EXECUTION_AUTHORIZED may authorize openspec/changes/** creation.
  - Implemented behavior enters openspec/specs/** only after verified delivery and archive/synchronization.
```

Rules 至少包含以下约束；如已有不冲突的 verification/scope rule 可保留：

```yaml
rules:
  brainstorm:
    - When current stage is DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY, restrict work to documentation-framework governance and do not propose, create or execute a capability change.
    - Do not use archived, rejected or historical task material as current authorization.
  proposal:
    - A capability proposal is forbidden unless it references an authorizing version design path and a status of READY_FOR_CHANGE_DECOMPOSITION or EXECUTION_AUTHORIZED.
    - Reject any proposal that treats roadmap or architecture target material as implementation authorization.
  design:
    - Long-lived architecture changes must identify whether an ADR and an update to docs/01-architecture/ are required.
  finalize:
    - After verified implementation in a future authorized stage, record whether implemented behavior was synchronized to openspec/specs/** and whether architecture or ADR updates were required.
```

不得引用：

```text
docs/v1/**
docs/harness/**
docs/delivery/**
V1 unified design is in progress
future V1 autonomous implementation mission
```

---

# 8. 归档仍在 Active Path 的旧材料

## 8.1 归档被否决的 docs/v1

若 `docs/v1/**` 存在，使用 `git mv`：

```powershell
New-Item -ItemType Directory -Force docs/99-archive/rejected-v1-planning | Out-Null
git mv docs/v1/* docs/99-archive/rejected-v1-planning/
```

在归档目录增加/覆盖入口说明：

```md
# Rejected Premature V1 Planning

- Status: `REJECTED_NOT_AUTHORIZED_FOR_EXECUTION`.
- These documents were produced before version design was authorized.
- Current authority is `docs/00-project/current-state.md`.
- This archive must not authorize:
  - version scope;
  - OpenSpec/SuperSpec changes;
  - implementation missions;
  - code or dependency modifications.
- Any future version design must be created afresh under:
  `docs/04-development/versions/<version>/`
  only after explicit authorization.
```

## 8.2 归档旧 docs/harness

若 `docs/harness/**` 存在，确认 `docs/02-harness/**` 内容有效后，使用 `git mv` 移至：

```text
docs/99-archive/legacy-harness/
```

增加：

```md
# Legacy Harness Archive

This directory preserves the pre-Context-Hub harness structure.
It is non-authoritative. Current harness authority is `docs/02-harness/`.
```

## 8.3 归档旧 docs/delivery

若 `docs/delivery/**` 存在，确认有效规则已被 numbered Hub 承接；本任务不需要把旧 V1/autonomous mission 内容迁移为 active 规则。使用 `git mv` 移至：

```text
docs/99-archive/legacy-delivery/
```

增加：

```md
# Legacy Delivery Archive

This directory preserves prior delivery-framework documents.
Any references to V1 or implementation missions are historical and rejected
as current authorization. Current rules are defined through the Context Hub.
```

## 8.4 处理旧 bootstrap / architecture 路径

若仍存在：

```text
docs/bootstrap/**
docs/architecture/**
```

按以下规则处理：

- 若新 `docs/00-project/` 与 `docs/01-architecture/` 已承接必要内容，则使用 `git mv` 分别移至：
  - `docs/99-archive/legacy-bootstrap/`
  - `docs/99-archive/legacy-architecture/`
- 若存在新目录尚未承接的长期有效内容，先将内容合并到正确 numbered 路径，再归档旧文件。
- 不从旧文件恢复任何 V1、change 或 implementation 授权。

更新 `docs/99-archive/README.md`，明确所有 archive 内容均为 non-authoritative。

---

# 9. 固化架构设计与架构变更的承载规则

## 9.1 更新 `docs/01-architecture/README.md`

补充以下规则：

```md
## What Belongs Here

`docs/01-architecture/` carries long-lived architecture design shared across
future versions, including system context, package boundaries, domain models,
observability principles and evolution boundaries.

It does not carry:

- a concrete version scope;
- implementation tasks;
- OpenSpec change artifacts;
- implementation receipts.

## Naming Rule

Living architecture documents use stable semantic kebab-case names:

- `system-context-and-quality-attributes.md`
- `logical-architecture-and-package-boundaries.md`
- `managed-executor-domain-model.md`
- `scheduling-reconfiguration-and-recovery-model.md`
- `observability-and-experiment-strategy.md`
- `operational-and-evolution-boundaries.md`

Do not create version-suffixed copies such as `architecture-v1-final.md`.
Accepted long-lived changes update the living document; Git preserves history.

## Decision Records

Long-lived architectural decisions belong under `docs/01-architecture/decisions/`.
```

## 9.2 更新 `docs/01-architecture/decisions/README.md`

将其扩充为：

```md
# Architecture Decision Records

## Purpose

This directory carries decisions that alter long-lived architecture across
versions. ADRs are not required for wording fixes or temporary version-scoped
choices.

## Naming Rule

Use:

`ADR-<four-digit-sequence>-<kebab-case-decision-topic>.md`

Examples:

- `ADR-0001-adopt-redis-lease-for-distributed-coordination.md`
- `ADR-0002-support-virtual-thread-execution-mode.md`

Do not create an ADR in the current documentation-framework-only task.

## Status Flow

`PROPOSED -> ACCEPTED -> SUPERSEDED | REJECTED`

## Architecture Change Handling

### Documentation Clarification

Typos, clarifying wording and diagram readability corrections update living
architecture documents directly. No ADR is required.

### Version-Scoped Decision

A decision affecting only one future version belongs in:

`docs/04-development/versions/<version>/decision-log.md`

It does not become an ADR unless it establishes a long-lived architecture rule.

### Long-Lived Architecture Decision

A decision that changes cross-version architecture boundaries, supported
technology direction, domain contracts, persistence/coordination strategy or
execution mode must:

1. create an ADR here;
2. update the related living architecture documents in the same authorized work;
3. be referenced from the authorizing version design or future change.

### Concrete Implementation Change

Concrete implementation belongs in:

`openspec/changes/<change-name>/`

only after a version design authorizes change decomposition.

### Implemented Behavior

After future verified delivery and OpenSpec archive/synchronization:

- implemented behavior belongs in `openspec/specs/`;
- historical implementation change artifacts belong in `openspec/changes/archive/`;
- living architecture reflects accepted long-lived structure;
- ADRs remain permanent records of accepted or superseded decisions.

## Current Status

No ADR exists or is authorized in the current
`DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY` stage.
```

---

# 10. 固化版本设计与 Change 命名规则

## 10.1 更新 `docs/04-development/versions/README.md`

保留已有生命周期规则，并补充：

```md
## Version Directory Naming

Use semantic version directory names for concrete version designs:

- `v0.1.0/` for the first exploratory runnable version.
- `v0.2.0/` for a later experimental increment.
- `v1.0.0/` only when a stable version boundary is intentionally established.

Do not use ambiguous paths such as `v1-final/`, `latest/` or `new-design/`.

## Version Document Set

A future authorized version design uses:

docs/04-development/versions/<version>/
├─ README.md
├─ 00-objectives-and-scope.md
├─ 01-requirements-and-use-cases.md
├─ 02-solution-design.md
├─ 03-api-and-observability-design.md
├─ 04-testing-and-acceptance-design.md
├─ 05-change-decomposition-plan.md
└─ decision-log.md

## Current Rule

No concrete version directory may be created while current stage is
`DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY`.
```

## 10.2 更新 `docs/03-openspec/version-design-to-change-rule.md`

补充：

```md
## Change Naming Rule

Future changes use action-oriented kebab-case capability names, for example:

- `establish-local-managed-executor-registry`
- `support-runtime-executor-configuration-update`
- `expose-experiment-workloads-and-observability`

Do not use broad names such as `v1`, `complete-platform` or `all-features`.

## Closeout and Synchronization Rule

After a future change is implemented and verified:

1. finalized change evidence remains under OpenSpec's archive lifecycle;
2. verified implemented behavior is synchronized to `openspec/specs/`;
3. if long-lived architecture changed, update `docs/01-architecture/`;
4. if a long-lived decision was accepted, add or update its ADR;
5. if the change only realizes the approved version scope without changing
   long-lived architecture, no new ADR is required.
```

---

# 11. Validation

执行：

```powershell
git diff --name-only
git diff --stat

.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec

git grep -n "V1 unified design is in progress\|docs/v1/README.md\|docs/harness/project-harness.md\|docs/delivery/README.md\|autonomous implementation mission" -- README.md AGENTS.md CLAUDE.md openspec/config.yaml docs/README.md docs/00-project docs/01-architecture docs/02-harness docs/03-openspec docs/04-development
git status --short --branch
```

## Grep 判定规则

Active authority 路径中必须不再出现：

```text
V1 unified design is in progress
docs/v1/README.md
docs/harness/project-harness.md
docs/delivery/README.md
autonomous implementation mission
```

如果某条文字仅出现在 `docs/99-archive/**` 的历史说明中，不影响通过。

## 允许变化

```text
README.md
openspec/config.yaml
AGENTS.md / CLAUDE.md                     # 仅实际存在残留冲突时
docs/README.md
docs/00-project/current-state.md
docs/01-architecture/**
docs/03-openspec/**
docs/04-development/versions/README.md
docs/99-archive/**
```

## 禁止变化

```text
pom.xml
src/main/**
src/test/**
openspec/schemas/**
openspec/changes/**
openspec/specs/**
.codex/**
.claude/**
docs/04-development/versions/v*/
```

---

# 12. Commit 与 Push

验证通过后：

```powershell
git add README.md AGENTS.md CLAUDE.md openspec/config.yaml docs
git commit -m "docs: finalize context hub authority and architecture change rules"
git push origin claude_master
```

确认远端：

```powershell
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.commit.sha'
gh api repos/zhiwuli0228/DynamicThreadPollerManager/commits/claude_master --jq '.sha + " " + .commit.message'
```

---

# 13. 最终返回格式

```text
STATUS: COMPLETED | BLOCKED_UNAUTHORIZED_IMPLEMENTATION_OR_CHANGE_PRESENT | BLOCKED_BASELINE_VALIDATION | BLOCKED_SCOPE_VIOLATION | BLOCKED_PUSH
TASK: finalize-context-hub-authority-and-architecture-change-rules
BRANCH: claude_master
START_HEAD:
END_HEAD:
PUSHED: YES | NO
GH_REMOTE_CONFIRMED: YES | NO

AUTHORITY_CLOSURE:
- root README aligned: YES | NO
- openspec config aligned: YES | NO
- AGENTS/CLAUDE clean of active V1 authority: YES | NO
- docs/v1 archived: YES | NO | NOT_PRESENT
- legacy harness archived: YES | NO | NOT_PRESENT
- legacy delivery archived: YES | NO | NOT_PRESENT
- legacy bootstrap/architecture archived: YES | NO | NOT_PRESENT

DESIGN_CARRYING_RULES:
- architecture naming rule established: YES | NO
- ADR naming/lifecycle/handling rule established: YES | NO
- version naming/document/lifecycle rule established: YES | NO
- OpenSpec change naming/synchronization rule established: YES | NO

VALIDATION:
- .\mvnw.cmd test: PASS | FAIL
- openspec.cmd validate --all --json: PASS | FAIL
- openspec.cmd schema validate superspec: PASS | FAIL
- active stale-reference grep: PASS | FAIL
- diff scope check: PASS | FAIL

SCOPE_CHECK:
- version design created: NO
- openspec/changes created or modified: NO
- openspec/specs modified: NO
- pom.xml changed: NO
- src/main changed: NO
- src/test changed: NO
- business implementation added: NO

NEXT_ALLOWED_WORK:
- Remote review of final documentation framework closure only.
- Version design, change creation and implementation remain unauthorized.
```
