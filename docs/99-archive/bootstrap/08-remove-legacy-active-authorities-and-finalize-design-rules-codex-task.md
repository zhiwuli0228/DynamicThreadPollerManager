# DynamicThreadPollerManager 最终闭环清理任务书
## 删除并行 Active Authority 并补齐文档命名/回写规则

> 执行端：Codex  
> 仓库：`https://github.com/zhiwuli0228/DynamicThreadPollerManager`  
> 分支：`claude_master`  
> 当前授权状态：`DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY`  
> 任务边界：仅清理旧 active 文档路径并补齐文档治理规则；不得设计版本、创建 change 或修改代码。

---

## 1. 已验证现状

已正确对齐的 active authority：

```text
README.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
docs/README.md
docs/00-project/current-state.md
```

这些文件已经声明：

```text
DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY
Version design: not started
OpenSpec capability changes: not authorized
Java implementation: not started and not authorized
```

仍未闭环的问题：

```text
docs/v1/**          仍存在于 active path，同时内容声称 V1 scope decided
docs/harness/**     仍存在于 active path，内容仍声明 V1 / autonomous delivery
docs/delivery/**    仍存在于 active path，内容仍声明 V1 in progress
```

同时，虽然存在归档副本：

```text
docs/99-archive/rejected-v1-planning/**
docs/99-archive/legacy-harness/**
docs/99-archive/legacy-delivery/**
```

但旧 active path 仍在，因此当前存在并行权威风险。

另有规则缺口：

```text
docs/01-architecture/decisions/README.md
docs/03-openspec/version-design-to-change-rule.md
docs/04-development/versions/README.md
```

仍为简版，尚未完整写入 Architecture / ADR / Version / Change 的命名与回写规则。

---

## 2. 本轮唯一目标

完成后必须达到：

```text
1. numbered Context Hub 是唯一 active authority。
2. docs/v1/、docs/harness/、docs/delivery/ 不再存在于 active path。
3. docs/99-archive/** 保留历史资料，并明确其 non-authoritative。
4. Architecture、ADR、Version Design、OpenSpec Change、Specs/Archive
   的命名和回写规则完整落地。
5. 当前状态仍是 DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY。
6. 没有版本设计、change、spec 或代码被创建/修改。
```

---

## 3. 允许修改范围

允许：

```text
docs/01-architecture/README.md
docs/01-architecture/decisions/README.md
docs/03-openspec/version-design-to-change-rule.md
docs/04-development/versions/README.md
docs/99-archive/README.md
docs/99-archive/rejected-v1-planning/**
docs/99-archive/legacy-harness/**
docs/99-archive/legacy-delivery/**
docs/v1/**       # 仅允许删除 active duplicate；历史内容已保存在 archive
docs/harness/**  # 仅允许删除 active duplicate；历史内容已保存在 archive
docs/delivery/** # 仅允许删除 active duplicate；历史内容已保存在 archive
```

仅核验，不主动改动，除非实际出现陈旧 active 引用：

```text
README.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
docs/README.md
docs/00-project/current-state.md
```

---

## 4. 绝对禁止

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
```

不得执行：

```text
任何 /opsx:* change 命令
任何 V1 设计
任何 implementation mission
任何依赖或代码修改
```

---

## 5. 执行前检查

```powershell
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.commit.sha'
git switch claude_master
git pull --ff-only origin claude_master
git status --short --branch

.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec

Get-ChildItem docs -Directory | Select-Object Name
Get-ChildItem docs/v1 -Recurse -ErrorAction SilentlyContinue
Get-ChildItem docs/harness -Recurse -ErrorAction SilentlyContinue
Get-ChildItem docs/delivery -Recurse -ErrorAction SilentlyContinue
Get-ChildItem docs/99-archive -Recurse -ErrorAction SilentlyContinue
```

若发现 `openspec/changes/**` 或业务实现已存在，返回：

```text
BLOCKED_UNAUTHORIZED_CHANGE_OR_IMPLEMENTATION_PRESENT
```

---

## 6. 删除并行 active path

归档副本已存在的情况下，不再复制。确认 archive 中包含对应内容后，从 active path 删除旧副本：

```powershell
git rm -r docs/v1
git rm -r docs/harness
git rm -r docs/delivery
```

如归档副本缺文件，先用 `git mv` 补全遗漏文件到对应 archive 目录，再删除空 active 目录。

归档入口必须明确：

```text
docs/99-archive/rejected-v1-planning/  -> REJECTED_NOT_AUTHORIZED_FOR_EXECUTION
docs/99-archive/legacy-harness/        -> non-authoritative legacy harness
docs/99-archive/legacy-delivery/       -> non-authoritative legacy delivery workflow
```

重要：archive 内部旧文件可以保留旧路径引用，因其仅作历史证据；但 archive 的入口说明必须压过其历史正文，声明不可执行。

---

## 7. 更新 Architecture 命名与承载规则

更新 `docs/01-architecture/README.md`，追加：

```md
## What Belongs Here

This directory carries long-lived architecture design shared across future
versions. It does not carry version scope, implementation tasks, OpenSpec
change artifacts or implementation receipts.

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

Long-lived architectural decisions belong under
`docs/01-architecture/decisions/`.
```

---

## 8. 更新 ADR 规则

更新 `docs/01-architecture/decisions/README.md`，替换或扩展为：

```md
# Architecture Decision Records

## Purpose

This directory carries decisions that alter long-lived architecture across
versions. ADRs are not required for wording fixes or temporary
version-scoped choices.

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

A decision changing cross-version architecture boundaries, supported
technology direction, domain contracts, persistence/coordination strategy or
execution mode must:

1. create an ADR here;
2. update related living architecture documents in the same authorized work;
3. be referenced from the authorizing version design or future change.

### Concrete Implementation Change

Concrete implementation belongs in:

`openspec/changes/<change-name>/`

only after a version design authorizes change decomposition.

### Implemented Behavior

After future verified delivery and OpenSpec archive/synchronization:

- implemented behavior belongs in `openspec/specs/`;
- historical change artifacts belong in `openspec/changes/archive/`;
- living architecture reflects accepted long-lived structure;
- ADRs remain permanent records of accepted or superseded decisions.

## Current Status

No ADR exists or is authorized in the current
`DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY` stage.
```

---

## 9. 更新 Version Design 命名规则

更新 `docs/04-development/versions/README.md`，保留现有生命周期，并追加：

```md
## Version Directory Naming

Use semantic version directory names:

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

No concrete version directory may be created while the current stage is
`DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY`.
```

---

## 10. 更新 OpenSpec Change 与回写规则

更新 `docs/03-openspec/version-design-to-change-rule.md`，追加：

```md
## Change Naming Rule

Future changes use action-oriented kebab-case capability names, for example:

- `establish-local-managed-executor-registry`
- `support-runtime-executor-configuration-update`
- `expose-experiment-workloads-and-observability`

Do not use broad names such as `v1`, `complete-platform` or `all-features`.

## Required Authorization Reference

Each future capability proposal must reference:

- authorizing version design path;
- version design status;
- matching entry in `05-change-decomposition-plan.md`;
- included and excluded scope.

## Closeout and Synchronization Rule

After a future change is implemented and verified:

1. finalized change evidence follows the OpenSpec archive lifecycle;
2. verified implemented behavior is synchronized to `openspec/specs/`;
3. if long-lived architecture changed, update `docs/01-architecture/`;
4. if a long-lived decision was accepted, add or update its ADR;
5. if the change only realizes approved version scope without altering
   long-lived architecture, no ADR is required.
```

---

## 11. Active Reference Verification

执行：

```powershell
git grep -n "docs/v1\|docs/harness\|docs/delivery\|V1 unified design is in progress\|autonomous implementation mission" -- README.md AGENTS.md CLAUDE.md openspec/config.yaml docs/README.md docs/00-project docs/01-architecture docs/02-harness docs/03-openspec docs/04-development docs/05-domain docs/06-operations docs/07-templates
```

预期 active authority 不得出现旧路径或旧授权。  
`docs/99-archive/**` 中允许存在历史原文和说明。

同时执行：

```powershell
Test-Path docs/v1
Test-Path docs/harness
Test-Path docs/delivery
```

预期均为：

```text
False
```

---

## 12. 验证、提交与推送

```powershell
git diff --name-only
git diff --stat
.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
git status --short --branch

git add docs README.md AGENTS.md CLAUDE.md openspec/config.yaml
git commit -m "docs: remove legacy active authorities and finalize design rules"
git push origin claude_master

gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.commit.sha'
gh api repos/zhiwuli0228/DynamicThreadPollerManager/commits/claude_master --jq '.sha + " " + .commit.message'
```

---

## 13. 返回格式

```text
STATUS: COMPLETED | BLOCKED_UNAUTHORIZED_CHANGE_OR_IMPLEMENTATION_PRESENT | BLOCKED_BASELINE_VALIDATION | BLOCKED_SCOPE_VIOLATION | BLOCKED_PUSH
TASK: remove-legacy-active-authorities-and-finalize-design-rules
BRANCH: claude_master
START_HEAD:
END_HEAD:
PUSHED: YES | NO
GH_REMOTE_CONFIRMED: YES | NO

ACTIVE_PATH_CLEANUP:
- docs/v1 removed from active path: YES | NO
- docs/harness removed from active path: YES | NO
- docs/delivery removed from active path: YES | NO
- archive copies preserved: YES | NO
- stale active-reference grep: PASS | FAIL

DESIGN_RULES:
- architecture naming rule complete: YES | NO
- ADR handling rule complete: YES | NO
- version design naming/document rule complete: YES | NO
- OpenSpec change naming/synchronization rule complete: YES | NO

VALIDATION:
- .\mvnw.cmd test: PASS | FAIL
- openspec.cmd validate --all --json: PASS | FAIL
- openspec.cmd schema validate superspec: PASS | FAIL
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
- Remote verification of the completed documentation framework.
- No version design, change creation, or implementation is authorized.
```
