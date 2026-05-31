# DynamicThreadPollerManager Context Hub 闭环整改任务书
## 清除旧 Active Authority 与 Premature V1 误授权，仅保留文档框架状态

> 执行端：Codex  
> 仓库：`https://github.com/zhiwuli0228/DynamicThreadPollerManager`  
> 分支：`claude_master`  
> 任务类型：文档承载框架闭环修复  
> 当前授权状态：`DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY`  
> 严禁：版本设计、OpenSpec change、代码、依赖、implementation mission 执行

---

# 1. 审查发现的事实

新 Context Hub 资产已经存在，并且方向正确：

```text
docs/README.md
docs/00-project/current-state.md
docs/01-architecture/decisions/README.md
docs/02-harness/context-policy.md
docs/03-openspec/README.md
docs/03-openspec/version-design-to-change-rule.md
docs/04-development/versions/README.md
docs/99-archive/README.md
```

其中已声明：

```text
Current stage: DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY
Version design: not started
OpenSpec capability changes: not authorized
Java implementation: not started and not authorized
No active version directory exists yet
No openspec/changes/** before authorized version design decomposition
```

但迁移未闭环，存在会误导 Agent 的冲突入口：

## P0 冲突入口仍为旧状态

以下根入口仍声称 V1 正在设计或未来 V1 autonomous implementation mission 可直接运行：

```text
README.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
```

必须以 `docs/00-project/current-state.md` 为准完成覆正。

## P0 Premature V1 资料仍在 active 路径

仍存在：

```text
docs/v1/**
```

其内容声明 “V1 scope decided” 与 “mission draft”，违反当前状态：

```text
Version design: not started
```

本轮必须将其整体归档为 rejected/premature historical material，不得删除证据，不得保留为 active 入口。

## P1 旧目录仍可能继续构成并行 authority

仍可直接读取旧路径：

```text
docs/harness/**
docs/delivery/**
```

这些文件仍包含旧 V1/autonomous delivery 语义，不能继续作为当前权威入口。

本轮应检查以下旧目录是否仍存在：

```text
docs/bootstrap/**
docs/architecture/**
docs/harness/**
docs/delivery/**
docs/v1/**
```

若存在，必须按本任务规则归档或消除 active 引用，确保当前权威体系只有 numbered Context Hub。

---

# 2. 本轮完成标准

完成后必须满足：

```text
1. README.md、AGENTS.md、CLAUDE.md、openspec/config.yaml 全部指向 docs/README.md 与 docs/00-project/current-state.md。
2. 任何 active 入口不得出现：
   - V1 unified design in progress
   - V1 scope decided
   - autonomous implementation mission active/ready
   - docs/v1/ 作为当前入口
   - docs/harness/ 或 docs/delivery/ 作为当前权威入口
3. docs/v1/** 已归档到 docs/99-archive/rejected-v1-planning/**。
4. 旧 docs/bootstrap、docs/architecture、docs/harness、docs/delivery 已按事实迁入 archive，或不存在；不能与 numbered Context Hub 并行生效。
5. docs/00-project/current-state.md 仍保持 DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY。
6. 不创建任何版本设计目录、change、spec 或代码。
```

---

# 3. 执行前核验

执行：

```powershell
gh auth status
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.commit.sha'

git switch claude_master
git pull --ff-only origin claude_master
git status --short --branch
git log --oneline --decorate -12

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
docs/README.md
docs/00-project/current-state.md
docs/02-harness/context-policy.md
docs/03-openspec/version-design-to-change-rule.md
docs/04-development/versions/README.md
```

检查目录：

```powershell
Get-ChildItem docs -Directory | Select-Object Name
Get-ChildItem docs/v1 -Recurse -ErrorAction SilentlyContinue
Get-ChildItem openspec/changes -Recurse -ErrorAction SilentlyContinue
Get-ChildItem openspec/specs -Recurse -ErrorAction SilentlyContinue
```

若发现 `src/**` 或 `pom.xml` 已被非框架任务修改，或 `openspec/changes/**` 存在已执行 change 证据，返回：

```text
BLOCKED_UNAUTHORIZED_IMPLEMENTATION_OR_CHANGE_PRESENT
```

不得覆盖证据。

---

# 4. 更新根 `README.md`

将根 README 改为简洁入口。必须声明：

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

该项目用于探索动态线程池与后续调度治理方案，并沉淀
Codex / Claude Code / OpenSpec-SuperSpec 的 AI 辅助开发框架。
当前仅建设文档与治理承载机制，不批准任何版本能力或代码实现。

## Start Reading

- `docs/README.md` — Context Hub 总入口
- `docs/00-project/current-state.md` — 当前授权状态唯一事实源
- `docs/02-harness/context-policy.md` — Agent 上下文读取规则
- `docs/03-openspec/version-design-to-change-rule.md` — 未来版本设计到 change 的授权规则

## Current Boundary

Architecture and roadmap documents describe future exploration candidates only.
No version design, OpenSpec capability change, implementation mission or Java
business change is authorized by the current state.
```

不得引用 active `docs/v1/`、旧 `docs/harness/` 或旧 `docs/delivery/`。

---

# 5. 更新 `AGENTS.md`

将 `AGENTS.md` 改为 Codex 当前权威入口，不保留旧 Phase 05 / V1 语义。

必须包含：

```md
# AGENTS.md

## Current Authorization

- Authoritative branch: `claude_master`.
- Current authorized work type: `DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY`.
- Source of truth for current authority: `docs/00-project/current-state.md`.
- No version design, OpenSpec capability change, implementation mission or
  application code is authorized in the current stage.

## Mandatory Reading Order

For any non-trivial task:

1. `docs/README.md`
2. `docs/00-project/current-state.md`
3. `docs/02-harness/context-policy.md`

Then read task-specific assets only as permitted by `docs/README.md`.

## Codex Responsibility in Current Stage

Codex may:

- normalize documentation structure;
- maintain project facts, architecture baselines, harness governance,
  OpenSpec lifecycle rules, templates and archive material;
- validate documentation/configuration consistency;
- commit and push authorized documentation-framework changes.

Codex must not:

- create `docs/04-development/versions/<version>/`;
- create or modify `openspec/changes/**`;
- write to `openspec/specs/**`;
- produce a V1 scope, mission or implementation plan;
- modify `pom.xml`, `src/main/**` or `src/test/**`;
- act on archived task books as current authorization.

## Future Authorization Rule

Version design may begin only after a later task explicitly changes the
authorized state. A capability change may be created only after an authorized
version design reaches `READY_FOR_CHANGE_DECOMPOSITION` or
`EXECUTION_AUTHORIZED`.
```

---

# 6. 更新 `CLAUDE.md`

将 `CLAUDE.md` 改为严格禁止实现的当前入口。

必须包含：

```md
# CLAUDE.md

## Current Implementation Authorization

- Authoritative branch: `claude_master`.
- Current authorized stage: `DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY`.
- Current implementation authorization: `NONE`.
- Source of truth: `docs/00-project/current-state.md`.

Do not implement code, modify dependencies, create or apply OpenSpec changes,
execute implementation missions, or act on archived/rejected V1 planning
material.

## Mandatory Reading Before Any Authorized Future Work

1. `docs/README.md`
2. `docs/00-project/current-state.md`
3. `docs/02-harness/context-policy.md`
4. Task-specific authorized documents identified by the future task.

## Current Prohibitions

- No modification to `pom.xml`.
- No modification to `src/main/**` or `src/test/**`.
- No creation or execution of `openspec/changes/**`.
- No updates to `openspec/specs/**`.
- No V1 design or implementation mission.
- No use of archived materials as execution authority.

## Future Implementation Gate

Implementation may begin only when:

1. `docs/00-project/current-state.md` explicitly permits execution;
2. an authorized version design exists;
3. its change decomposition is authorized;
4. a current active task grants implementation authority.

Until then, Claude Code may perform only explicitly authorized framework
documentation work.
```

---

# 7. 更新 `openspec/config.yaml`

保持：

```yaml
schema: superspec
```

替换旧 V1/autonomous implementation 状态语义，使 context 明确为：

```yaml
context: |
  Project: DynamicThreadPollerManager, a Java 21 and Spring Boot exploratory project.
  Authoritative branch: claude_master.
  Current authorized stage: DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY.
  Current state source of truth: docs/00-project/current-state.md.
  Documentation context hub: docs/README.md.
  Document authority boundaries:
  - Project facts and current state live in docs/00-project/.
  - Long-lived architecture and ADRs live in docs/01-architecture/.
  - Harness governance lives in docs/02-harness/.
  - OpenSpec lifecycle rules live in docs/03-openspec/.
  - Future version designs must live in docs/04-development/versions/<version>/.
  - Domain vocabulary and exploration boundaries live in docs/05-domain/.
  - Historical or rejected material lives in docs/99-archive/ and is non-authoritative.
  Current prohibitions:
  - No version design is authorized.
  - No openspec/changes/** may be created.
  - No openspec/specs/** may be written with unimplemented behavior.
  - No Java code, tests or dependency changes are authorized.
  - Archived V1 or implementation mission material must not be executed.
```

Rules 必须至少包含：

```yaml
rules:
  brainstorm:
    - If current stage is DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY, restrict work to documentation-framework governance and do not propose or create a capability change.
    - Do not use archived or rejected material as current authorization.
  proposal:
    - A capability proposal is forbidden unless it references an authorized version design path whose status is READY_FOR_CHANGE_DECOMPOSITION or EXECUTION_AUTHORIZED.
```

可保留与验证证据、边界一致性相关且不冲突的旧规则；必须删除/改写以下含义：

```text
V1 unified design is in progress
Claude Code may continuously implement under a future V1 autonomous implementation mission
docs/v1/ is required reference
```

修改后必须执行 OpenSpec 校验。

---

# 8. Archive Premature and Legacy Active Material

## 8.1 `docs/v1/**`

若存在，执行迁移：

```text
docs/v1/** -> docs/99-archive/rejected-v1-planning/**
```

在：

```text
docs/99-archive/rejected-v1-planning/README.md
```

写明：

```md
# Rejected Premature V1 Planning

- Status: `REJECTED_NOT_AUTHORIZED_FOR_EXECUTION`.
- Reason: these documents were created before the project authorized any
  version design; current state reverted to documentation-framework
  construction only.
- These files are preserved solely as historical evidence.
- They must not authorize OpenSpec changes, implementation missions, source
  code changes, dependency changes or future V1 scope decisions.
```

## 8.2 旧并行权威目录

检查是否存在：

```text
docs/bootstrap/**
docs/architecture/**
docs/harness/**
docs/delivery/**
```

如果存在，执行以下处理：

### 先确认新编号目录已有等价有效承载

```text
docs/00-project/**
docs/01-architecture/**
docs/02-harness/**
docs/03-openspec/**
docs/04-development/**
```

### 再将旧目录迁移到 archive

```text
docs/bootstrap/**    -> docs/99-archive/legacy-bootstrap/**
docs/architecture/** -> docs/99-archive/legacy-architecture/**
docs/harness/**      -> docs/99-archive/legacy-harness/**
docs/delivery/**     -> docs/99-archive/legacy-delivery/**
```

要求：

- 保留历史证据；
- 不留旧目录作为 active path；
- 如果新目录缺少旧文档中的必要长期内容，先将必要内容迁移/合并到正确 numbered 目录，再归档旧文档；
- 不因迁移而批准 V1 或 change；
- 更新 `docs/99-archive/README.md` 列出归档目录和 non-authoritative 规则。

---

# 9. 检查新 Context Hub 的完整性

确保至少存在并内容有效：

```text
docs/README.md
docs/00-project/README.md
docs/00-project/project-brief.md
docs/00-project/current-state.md
docs/00-project/glossary.md
docs/00-project/roadmap.md

docs/01-architecture/README.md
docs/01-architecture/decisions/README.md

docs/02-harness/README.md
docs/02-harness/harness-standard.md
docs/02-harness/agent-behavior.md
docs/02-harness/context-policy.md
docs/02-harness/task-execution-policy.md
docs/02-harness/verification-policy.md
docs/02-harness/change-snapshot-policy.md

docs/03-openspec/README.md
docs/03-openspec/artifact-boundary.md
docs/03-openspec/lifecycle-rule.md
docs/03-openspec/version-design-to-change-rule.md

docs/04-development/README.md
docs/04-development/development-guide.md
docs/04-development/testing-guide.md
docs/04-development/versions/README.md

docs/05-domain/README.md
docs/05-domain/executor-domain-glossary.md
docs/05-domain/exploration-boundaries.md

docs/06-operations/README.md

docs/07-templates/README.md
docs/07-templates/version-design-template.md
docs/07-templates/architecture-decision-record-template.md
docs/07-templates/change-decomposition-template.md

docs/99-archive/README.md
```

重要检查：

```text
docs/04-development/versions/ 下只能有 README.md，不得存在 v1/ 或其他版本目录。
openspec/changes/ 不得新增 capability change。
openspec/specs/ 不得新增未实现规格。
```

---

# 10. 验证与 Scope Check

执行：

```powershell
git diff --name-only
git diff --stat
.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
git status --short --branch
```

允许变化：

```text
README.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
docs/**
```

必须没有变化：

```text
pom.xml
src/main/**
src/test/**
openspec/schemas/**
openspec/changes/**
openspec/specs/**
.codex/**
.claude/**
```

必须验证没有 active 旧引用：

```powershell
git grep -n "docs/v1\|docs/harness\|docs/delivery\|V1 unified design is in progress\|autonomous implementation mission" -- README.md AGENTS.md CLAUDE.md openspec/config.yaml docs/README.md docs/00-project docs/02-harness docs/03-openspec docs/04-development
```

期望：

- 不存在将 `docs/v1/`、旧 `docs/harness/`、旧 `docs/delivery/` 作为 active authority 的引用；
- 如在 archive 规则中提到这些路径用于说明历史归档，可保留；
- 不存在 active V1/design/implementation 授权表述。

---

# 11. Commit 与 Push

验证通过后：

```powershell
git add README.md AGENTS.md CLAUDE.md openspec/config.yaml docs
git commit -m "docs: close context hub migration and revoke premature v1 authority"
git push origin claude_master
```

使用 `gh` 确认：

```powershell
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.commit.sha'
gh api repos/zhiwuli0228/DynamicThreadPollerManager/commits/claude_master --jq '.sha + " " + .commit.message'
```

---

# 12. 最终返回格式

```text
STATUS: COMPLETED | BLOCKED_DIRTY_WORKTREE | BLOCKED_BASELINE_VALIDATION | BLOCKED_UNAUTHORIZED_IMPLEMENTATION_OR_CHANGE_PRESENT | BLOCKED_SCOPE_VIOLATION | BLOCKED_PUSH
TASK: close-context-hub-migration-and-revoke-premature-v1-authority
REPOSITORY: DynamicThreadPollerManager
BRANCH: claude_master
START_HEAD:
END_HEAD:
PUSHED: YES | NO
GH_REMOTE_CONFIRMED: YES | NO

ACTIVE_AUTHORITY_ALIGNMENT:
- README aligned to current-state: YES | NO
- AGENTS aligned to current-state: YES | NO
- CLAUDE aligned to current-state: YES | NO
- openspec/config.yaml aligned to current-state: YES | NO

ARCHIVE_CLOSURE:
- docs/v1 archived as rejected premature planning: YES | NO | NOT_PRESENT
- legacy bootstrap archived: YES | NO | NOT_PRESENT
- legacy architecture archived: YES | NO | NOT_PRESENT
- legacy harness archived: YES | NO | NOT_PRESENT
- legacy delivery archived: YES | NO | NOT_PRESENT
- active legacy references eliminated: YES | NO

CONTEXT_HUB:
- docs/README.md present: YES | NO
- current-state authority present: YES | NO
- version carrier rule present: YES | NO
- ADR carrier rule present: YES | NO
- OpenSpec authorization rule present: YES | NO

VALIDATION:
- .\mvnw.cmd test: PASS | FAIL
- openspec.cmd validate --all --json: PASS | FAIL
- openspec.cmd schema validate superspec: PASS | FAIL
- stale active-reference grep check: PASS | FAIL
- git diff scope check: PASS | FAIL

SCOPE_CHECK:
- pom.xml changed: NO
- src/main changed: NO
- src/test changed: NO
- openspec/changes created or modified: NO
- openspec/specs created or modified: NO
- openspec/schemas changed: NO
- .codex/.claude changed: NO
- version design created: NO
- business implementation added: NO

NEXT_ALLOWED_WORK:
- Remote review of closed Context Hub framework only.
- No version design, OpenSpec change or implementation is authorized.
```
