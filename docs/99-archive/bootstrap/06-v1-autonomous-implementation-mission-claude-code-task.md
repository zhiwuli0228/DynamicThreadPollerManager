# DynamicThreadPollerManager Phase 06 执行任务书
## V1 Autonomous Implementation Mission — Design Closure, SuperSpec Changes, Implementation, Verification and Integration

> 执行端：Claude Code  
> 目标仓库：`https://github.com/zhiwuli0228/DynamicThreadPollerManager`  
> 治理与最终验收基线：`claude_master`  
> V1 实施分支：`ai/v1-implementation`  
> 当前起始基线提交：`a817f0d` (`docs: adopt autonomous ai delivery and define v1 unified design`)  
> GitHub 操作：优先使用 `gh` CLI  
> 执行模式：全自动连续实施；不因单个 change 完成或等待人工审阅而暂停  
> 最终目标：完成 V1，实现、验证、推送，并自动将结果集成回 `claude_master`

---

## 1. Mission Authorization

本任务书正式授权 Claude Code 在本任务定义的 V1 范围内连续完成：

1. 远端基线核验与实施分支建立；
2. V1 设计闭环修订；
3. 按顺序创建并执行 3 个 SuperSpec capability changes；
4. 编写 Java 代码、配置、测试与必要说明文档；
5. 自动修复 V1 范围内的编译、测试、校验和实现一致性问题；
6. 对每个 change 执行验证、提交、推送和证据记录；
7. 在全部 V1 changes 通过后执行最终验证；
8. 使用 `gh` 创建并自动合并 `ai/v1-implementation -> claude_master` 的 PR，或在仓库权限不允许 PR merge 时采用安全的等价自动集成方式；
9. 确认远端 `claude_master` 已包含 V1 已验证结果。

执行期间**不得因阶段切换、change 切换或需要人工审阅而暂停**。仅在本任务定义的 `BLOCKED` 情形出现时停止。

---

## 2. Phase 05 远端审查结论与本任务补正事项

Phase 05 提交 `a817f0d` 已通过总体方向审查：

- Autonomous AI Delivery 模式已建立；
- V1 目标已收敛为单节点内存动态受管线程池实验闭环；
- V1 已明确包含 Web/API、Validation、Actuator/Micrometer、Registry、Runtime Update、Controlled Workloads；
- 动态调度、恢复、Redis、多节点、虚拟线程、数据库、前端、鉴权、Kafka 已排除；
- V1 已拆分为 3 个可连续执行的 changes；
- 使用 `ai/v1-implementation` 单实施分支的方向正确。

但在实施前必须补齐两个设计闭环问题：

### 2.1 Runtime Update 参数集合尚未固定

V1 文档虽将 runtime config update 纳入范围，但未将可动态修改字段固定为不可歧义的集合。实施前必须在 V1 文档中明确：

```text
V1 allowed runtime update fields:
- corePoolSize
- maximumPoolSize
- keepAliveTimeMillis

V1 immutable-after-registration fields:
- queueCapacity
- threadNamePrefix
- rejectionPolicy

V1 explicitly unsupported runtime mutations:
- queue capacity replacement
- rejection policy replacement
- virtual-thread mode switch
- scheduling configuration
```

### 2.2 Closeout 路径必须固定

现有 draft 将 PR 或 merge 路径留给实施时选择。为保证自治执行与可追溯性，本任务固定为：

```text
claude_master
  -> create/push ai/v1-implementation
  -> sequentially complete and push all V1 changes
  -> run final full validation on ai/v1-implementation
  -> create PR: ai/v1-implementation -> claude_master using gh
  -> automatically merge the PR after validations pass
  -> confirm remote claude_master includes the merged V1 result
```

优先使用普通 merge 或 rebase merge，以保留各 change 的提交证据；不要默认 squash 掉按 change 留下的验证提交，除非仓库策略只允许 squash。

---

## 3. V1 Authoritative Scope

## 3.1 In Scope

V1 只实施以下闭环：

| Capability | Authorization |
|---|---|
| Spring Boot REST management surface | IN |
| Bean Validation for management requests | IN |
| Spring Boot Actuator / Micrometer metrics | IN |
| In-memory managed executor registry | IN |
| Executor registration, list, query, safe update and safe removal | IN |
| Runtime updates: `corePoolSize`, `maximumPoolSize`, `keepAliveTimeMillis` | IN |
| Bounded queue configuration at executor registration | IN, immutable after registration |
| Thread naming for managed executor threads | IN, immutable after registration |
| Fixed rejection behavior with observable rejection result | IN |
| Controlled workload scenarios needed to show before/after behavior | IN |
| Stable HTTP error responses using `ProblemDetail` | IN |
| Unit, application, API integration, startup and deterministic concurrency tests | IN |
| Change artifacts, receipts, validation evidence, commits, push and PR/merge closeout | IN |

## 3.2 Explicitly Excluded

The mission must not introduce:

```text
- dynamic scheduled task reconfiguration
- scheduling APIs or scheduler versioning implementation
- stalled-task detection or recovery
- Redis
- Kafka
- database persistence
- frontend or UI
- authentication or authorization
- multi-node coordination
- virtual threads mode
- dynamic queue capacity replacement
- runtime rejection-policy replacement
- external configuration centers
- CI/CD expansion unrelated to implementing V1
```

If completing implementation would require an excluded capability, return `BLOCKED_SCOPE_EXPANSION` rather than expanding scope.

---

## 4. Mandatory Reading Before Work

Before editing any file, read:

```text
README.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
openspec/schemas/superspec/schema.yaml

docs/harness/project-harness.md
docs/harness/00-project-constitution.md
docs/harness/01-domain-and-experiment-scope.md
docs/harness/02-architecture-and-dependency-rules.md
docs/harness/03-engineering-and-testing-rules.md
docs/harness/04-ai-delivery-workflow.md
docs/harness/05-change-classification-and-gates.md

docs/architecture/README.md
docs/architecture/00-system-context-and-quality-attributes.md
docs/architecture/01-logical-architecture-and-package-boundaries.md
docs/architecture/02-managed-executor-domain-model.md
docs/architecture/04-observability-and-experiment-strategy.md
docs/architecture/05-operational-and-evolution-boundaries.md
docs/architecture/06-v1-unified-design-planning-framework.md

docs/delivery/README.md
docs/delivery/00-toolchain-readiness-and-command-map.md
docs/delivery/01-branch-change-and-review-lifecycle.md
docs/delivery/02-framework-completion-gate.md

docs/v1/README.md
docs/v1/00-v1-product-scope-and-success-criteria.md
docs/v1/01-v1-technical-architecture-decisions.md
docs/v1/02-v1-domain-capability-design.md
docs/v1/03-v1-api-observability-and-experiment-design.md
docs/v1/04-v1-testing-and-acceptance-strategy.md
docs/v1/05-v1-change-decomposition-and-autonomous-execution-plan.md
docs/v1/06-v1-claude-code-autonomous-implementation-mission-draft.md
```

Do not read the scheduling/recovery architecture as an implementation instruction; it remains deferred context only.

---

## 5. Start-Up and Branch Setup

## 5.1 Remote Confirmation

Execute:

```powershell
gh auth status
gh repo view zhiwuli0228/DynamicThreadPollerManager --json nameWithOwner,url,defaultBranchRef
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.name + " " + .commit.sha'
gh api repos/zhiwuli0228/DynamicThreadPollerManager/commits/claude_master --jq '.sha + " " + .commit.message'
```

Expected baseline at mission issuance:

```text
claude_master contains commit a817f0d
docs: adopt autonomous ai delivery and define v1 unified design
```

If later non-conflicting governance-only commits are present, re-read them and proceed from the actual remote `claude_master`. If unexpected implementation work or contradictory scope changes are present, return `BLOCKED_BASELINE_CONFLICT`.

## 5.2 Local Sync

Execute safely:

```powershell
git switch claude_master
git pull --ff-only origin claude_master
git status --short --branch
git log --oneline --decorate -12
```

If the working tree contains unexplained edits that cannot be safely preserved, return `BLOCKED_DIRTY_WORKTREE`.

## 5.3 Baseline Verification

Execute:

```powershell
.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
```

If the baseline fails before V1 edits, return `BLOCKED_BASELINE_VALIDATION` with evidence; do not conceal or bypass the failure.

## 5.4 Implementation Branch

Create or safely reuse the mission branch:

```powershell
git switch -c ai/v1-implementation
git push -u origin ai/v1-implementation
```

If the branch already exists remotely:

- determine whether it belongs to this exact V1 mission;
- if it is safely reusable and based on the expected `claude_master` baseline, continue from it;
- otherwise return `BLOCKED_BRANCH_CONFLICT`;
- never force push over unknown work.

---

# Part A — Mandatory V1 Design Closure Commit

## 6. Design Closure Before Capability Implementation

Before creating any capability change, update V1 documentation on `ai/v1-implementation` to remove the implementation ambiguities identified during Phase 05 review.

## 6.1 Files Authorized for Closure Updates

Modify only as needed:

```text
docs/v1/00-v1-product-scope-and-success-criteria.md
docs/v1/01-v1-technical-architecture-decisions.md
docs/v1/02-v1-domain-capability-design.md
docs/v1/03-v1-api-observability-and-experiment-design.md
docs/v1/04-v1-testing-and-acceptance-strategy.md
docs/v1/05-v1-change-decomposition-and-autonomous-execution-plan.md
docs/v1/06-v1-claude-code-autonomous-implementation-mission-draft.md
docs/bootstrap/bootstrap-ledger.md
README.md
```

## 6.2 Required Decisions to Add

### Runtime Configuration Boundary

Explicitly establish:

```text
At executor registration:
- executorId: required stable id
- corePoolSize: required, >= 0
- maximumPoolSize: required, > 0 and >= corePoolSize
- keepAliveTimeMillis: required, >= 0
- queueCapacity: required, > 0; immutable after registration
- threadNamePrefix: required or safely defaulted; immutable after registration
- rejectionPolicy: fixed to the selected V1 policy; not runtime replaceable
```

For V1, select a single rejection policy appropriate for a transparent demo. Recommended decision:

```text
AbortPolicy with observable rejected-execution handling and metric count.
```

Runtime update may update only:

```text
corePoolSize
maximumPoolSize
keepAliveTimeMillis
```

### Safe Update Transition Semantics

Document and implement later:

```text
- validate the complete desired configuration before mutation;
- if increasing core above current max, apply maximumPoolSize first, then corePoolSize;
- if reducing max below current core, apply corePoolSize first, then maximumPoolSize;
- otherwise apply validated changes in a safe order;
- apply keepAliveTimeMillis only after size invariants are safe;
- publish a new snapshot/version only after all live executor mutations succeed;
- rejected updates preserve the previously published state.
```

If rollback after a rare JDK mutation failure is required, design a bounded best-effort rollback and explicit failure receipt; do not silently return success.

### Safe Remove Semantics

The current V1 docs include `DELETE /api/v1/executors/{executorId}` but do not define removal behavior. Select and document a minimal safe policy:

```text
- Removal is allowed only when the executor has no active tasks and no queued tasks.
- A removal request against a busy executor returns a conflict/error response.
- Successful removal deregisters the executor and calls graceful shutdown.
- V1 does not expose forced shutdown of running tasks.
```

### Controlled Workload Boundary

Define a minimal V1 workload set:

```text
- burst workload: submits a bounded number of tasks to observe execution and rejection behavior.
- blocking/gated workload: uses a controllable gate/latch abstraction in tests and bounded runtime inputs in the demo to observe active threads and queue pressure.
```

Implementation may refine names and DTO details, but may not introduce scheduling or external I/O scenarios.

### Closeout Strategy

Replace any “PR or merge path chosen later” wording with:

```text
- use branch `ai/v1-implementation`;
- commit and push evidence after each verified authorized change;
- after final verification, open a PR to `claude_master` with `gh`;
- merge automatically after checks pass and permissions allow;
- preserve per-change traceability; prefer a non-squash merge unless repository policy requires squash;
- if automated merge is blocked by external permissions or repository settings, return BLOCKED_CLOSEOUT after pushing all verified branch work.
```

## 6.3 Closure Validation and Commit

Run:

```powershell
git diff --name-only
.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
git add docs/v1 docs/bootstrap/bootstrap-ledger.md README.md
git commit -m "docs: close v1 implementation boundaries for autonomous delivery"
git push origin ai/v1-implementation
```

Do not pause after this push. Continue immediately into Change 1.

---

# Part B — Authorized SuperSpec Change Set

## 7. General SuperSpec Execution Rules

Authorized changes, in exact order:

```text
1. establish-springboot-management-foundation
2. establish-local-managed-executor-registry
3. expose-experiment-workloads-and-observability
```

For each change:

1. Use the actual repository OpenSpec/SuperSpec workflow available in Claude Code.
2. Create complete required artifacts under `openspec/changes/<change-name>/` according to SuperSpec v4.
3. Reference the V1 documents and explicit exclusions in the artifacts.
4. Treat design review/code review steps as internal AI work within this mission; do not pause for human approval.
5. Implement only that change's scope.
6. Run targeted tests plus full required verification.
7. Correct failures within the active V1 scope automatically.
8. Record apply/verify/finalize evidence required by the schema.
9. Commit and push the verified change result.
10. Continue immediately to the next authorized change.

If the generated workflow offers multiple paths, choose the path compatible with:

```text
SuperSpec v4 + Claude Code Superpowers + autonomous sequential execution on ai/v1-implementation
```

Do not alter `openspec/schemas/**`, `.claude/**` or `.codex/**`.

---

## 8. Change 1 — `establish-springboot-management-foundation`

## 8.1 Purpose

Introduce the minimum Spring Boot foundation required for V1 management APIs and observability, without implementing the managed executor domain behavior.

## 8.2 Authorized Scope

May introduce or modify:

```text
pom.xml
src/main/**
src/test/**
src/main/resources/**
openspec/changes/establish-springboot-management-foundation/**
README.md or docs/v1 evidence/status documents only when needed to reflect actual completion
```

May add V1-approved dependencies:

```text
spring-boot-starter-web
spring-boot-starter-validation
spring-boot-starter-actuator
micrometer dependencies only if not already transitively sufficient for the selected actuator metrics implementation
```

Must establish only the necessary foundation, such as:

- REST management surface conventions;
- `ProblemDetail`-based common error mapping;
- validation support;
- Actuator minimal exposure configuration;
- application/package structure required by the foundation;
- startup/context and foundational API/error tests.

## 8.3 Exclusions

Must not yet implement:

- managed executor registry domain behavior;
- runtime update endpoints that claim to function against an executor;
- workload scenario behavior;
- scheduling/recovery/distributed capability.

## 8.4 Verification Before Proceeding

At minimum:

```powershell
.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
```

Commit after successful verification:

```powershell
git add .
git commit -m "feat: establish spring boot management foundation"
git push origin ai/v1-implementation
```

Continue automatically to Change 2.

---

## 9. Change 2 — `establish-local-managed-executor-registry`

## 9.1 Purpose

Implement the core V1 domain: in-memory managed executors with safe runtime configuration updates, snapshots, validation and management API behavior.

## 9.2 Authorized Domain Behavior

Implement:

- named executor registration;
- list and fetch snapshot/query behavior;
- safe update of only:
  - `corePoolSize`
  - `maximumPoolSize`
  - `keepAliveTimeMillis`
- immutable-at-runtime:
  - `queueCapacity`
  - `threadNamePrefix`
  - selected `rejectionPolicy`
- safe remove semantics:
  - reject removal while active tasks or queued tasks exist;
  - successful idle removal performs graceful shutdown and deregistration;
  - no force shutdown endpoint;
- stable errors via `ProblemDetail`;
- versioned snapshots or equivalent unambiguous update evidence;
- domain/application/API tests and deterministic concurrency tests.

## 9.3 Required Boundaries

Maintain:

```text
api -> application -> domain
infrastructure -> domain
```

Requirements:

- Domain must not depend on REST DTOs, Spring MVC infrastructure, Micrometer registry objects or JDK adapter wiring details.
- Controller logic must remain thin.
- Configuration invariants must be implemented in domain/application boundary, not only through HTTP validation.
- The concrete `ThreadPoolExecutor` adapter must apply size changes in the safe order specified in Part A.
- A rejected update must not publish a new snapshot version.

## 9.4 Verification Before Proceeding

At minimum:

```powershell
.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
```

Commit after successful verification:

```powershell
git add .
git commit -m "feat: establish local managed executor registry"
git push origin ai/v1-implementation
```

Continue automatically to Change 3.

---

## 10. Change 3 — `expose-experiment-workloads-and-observability`

## 10.1 Purpose

Close the V1 experiment loop: run bounded workloads against a selected managed executor and observe the effect of runtime configuration changes through responses and metrics.

## 10.2 Authorized Behavior

Implement:

- workload scenario listing;
- bounded workload execution against a selected registered executor;
- a minimal burst scenario;
- a minimal blocking/gated scenario suitable for deterministic tests;
- workload run result/query surface required by the approved V1 API;
- Micrometer-backed observations for the implemented executor/workload behavior, such as:
  - executor registration/update counts;
  - workload run/rejection counts;
  - observable current executor status or suitable gauges;
- API/application/domain/infrastructure tests appropriate to implemented behavior;
- a concise local experiment usage section or V1 completion receipt document.

## 10.3 Exclusions

Must not add:

- scheduler APIs;
- periodic tasks;
- recovery logic;
- external I/O simulation requiring new integrations;
- distributed metrics backends;
- Prometheus dependency unless strictly necessary and explicitly justified within existing V1 observability scope. Prefer default Actuator metrics surface.

## 10.4 Verification

At minimum:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
```

Commit after successful verification:

```powershell
git add .
git commit -m "feat: expose experiment workloads and observability"
git push origin ai/v1-implementation
```

Continue automatically into final V1 closeout.

---

# Part C — V1 Final Validation and Autonomous Integration

## 11. Final Documentation and Evidence Closure

After Change 3 succeeds, update only the documents necessary to accurately represent implemented V1 state:

```text
README.md
docs/v1/README.md
docs/v1/06-v1-claude-code-autonomous-implementation-mission-draft.md   # mark executed or superseded if appropriate
docs/v1/07-v1-implementation-receipt.md                                 # create
docs/bootstrap/bootstrap-ledger.md
docs/delivery/02-framework-completion-gate.md                           # only if it tracks V1 mission execution status
```

Create:

```text
docs/v1/07-v1-implementation-receipt.md
```

It must record:

- baseline SHA and implementation branch;
- executed change set;
- actual dependencies introduced;
- implemented API surface;
- implemented runtime update fields;
- implemented workload scenarios and metrics;
- explicit excluded capabilities confirmed absent;
- test and OpenSpec validation commands/results;
- commit SHAs for each verified change;
- final PR/merge status;
- any deviations repaired autonomously within scope.

Documentation must describe actual implementation only after verifying it exists.

Commit and push evidence:

```powershell
git add .
git commit -m "docs: record v1 autonomous implementation receipt"
git push origin ai/v1-implementation
```

---

## 12. Full Final Validation

Run all required checks from a clean status perspective:

```powershell
git status --short --branch
.\mvnw.cmd test
.\mvnw.cmd verify
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
```

Additionally perform scope checks:

```powershell
git diff --name-only origin/claude_master...HEAD
```

Verify absence of excluded implementation:

```text
- no Redis dependencies or adapters
- no Kafka dependencies or adapters
- no database persistence dependency or repository layer
- no frontend project
- no authentication/security feature
- no multi-node coordination implementation
- no virtual-thread mode implementation
- no dynamic scheduling/recovery implementation
- no changes to openspec/schemas/**
- no changes to .codex/** or .claude/**
```

If documentation says a capability exists but tests/code do not prove it, repair documentation or implementation within V1 scope and rerun validation.

---

## 13. Automatic PR and Merge to `claude_master`

After final validation succeeds:

```powershell
gh repo view zhiwuli0228/DynamicThreadPollerManager --json nameWithOwner,url
gh pr create --base claude_master --head ai/v1-implementation --title "feat: implement V1 dynamic executor management experiment" --body-file <generated-pr-body-file>
```

Create a concise PR body file containing:

- V1 scope;
- completed change set;
- validation results;
- explicit exclusions;
- implementation receipt path.

Then merge automatically after confirming mergeability and any required checks:

```powershell
gh pr view --json number,state,mergeable,statusCheckRollup,url
gh pr merge --merge --delete-branch
```

Use an alternative supported non-squash merge option if required by repository settings. If only squash merge is permitted, it is authorized; record that per-change commits remain traceable on the source branch/PR history.

After merge:

```powershell
git switch claude_master
git pull --ff-only origin claude_master
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.commit.sha'
.\mvnw.cmd test
.\mvnw.cmd verify
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
```

If PR creation or automated merge is blocked solely by external permissions/settings after all branch work is verified and pushed, return `BLOCKED_CLOSEOUT` with branch evidence; do not discard completed work.

---

## 14. Auto-Fix and Retry Policy

Within the V1 scope, Claude Code is authorized to autonomously:

- resolve compilation errors;
- correct tests and deterministic concurrency behavior;
- fix implementation defects exposed by tests;
- fix V1 documentation inconsistencies;
- refine DTOs, class boundaries and package placement while remaining aligned with Architecture;
- add missing approved dependencies required for V1;
- correct OpenSpec change artifacts/receipts required to make verification pass;
- retry tests, validation, push and PR operations after safe fixes.

Claude Code must not ask for intermediate approval for these operations.

---

## 15. BLOCKED Conditions

Return a blocking status only when one of the following occurs:

| Status | Condition |
|---|---|
| `BLOCKED_AUTH` | `gh`/git authentication or repository permission cannot be completed non-interactively |
| `BLOCKED_DIRTY_WORKTREE` | unexplained pre-existing local work cannot be safely preserved |
| `BLOCKED_BASELINE_CONFLICT` | remote baseline contains contradictory or unexpected implementation work |
| `BLOCKED_BRANCH_CONFLICT` | existing `ai/v1-implementation` branch cannot be safely reused |
| `BLOCKED_TOOLCHAIN` | OpenSpec, SuperSpec, Maven, JDK or required Claude skills are not usable and cannot be repaired within authorization |
| `BLOCKED_SCOPE_EXPANSION` | implementation requires an excluded V1 capability or unapproved architecture expansion |
| `BLOCKED_VALIDATION` | build/test/validation cannot be repaired without leaving V1 scope |
| `BLOCKED_CLOSEOUT` | verified branch is pushed, but PR/merge into `claude_master` is prevented by external repository permissions/settings |

Do **not** return BLOCKED merely because:

- one authorized change is complete;
- a SuperSpec artifact needs to be created;
- tests initially fail but can be repaired within V1;
- a push or PR step remains to be performed under available permissions;
- a design detail can be safely clarified within the boundaries specified in Part A.

---

## 16. Final Report Format

When the mission is complete or genuinely blocked, return only this structured summary:

```text
STATUS: COMPLETED_V1_INTEGRATED | BLOCKED_AUTH | BLOCKED_DIRTY_WORKTREE | BLOCKED_BASELINE_CONFLICT | BLOCKED_BRANCH_CONFLICT | BLOCKED_TOOLCHAIN | BLOCKED_SCOPE_EXPANSION | BLOCKED_VALIDATION | BLOCKED_CLOSEOUT
MISSION: V1-autonomous-dynamic-executor-implementation
EXECUTOR: Claude Code
REPOSITORY: DynamicThreadPollerManager
BASELINE_BRANCH: claude_master
IMPLEMENTATION_BRANCH: ai/v1-implementation
START_HEAD:
IMPLEMENTATION_BRANCH_END_HEAD:
CLAUDE_MASTER_END_HEAD:
PUSHED: YES | NO
PR_CREATED: YES | NO
PR_URL:
PR_MERGED: YES | NO
GH_REMOTE_CONFIRMED: YES | NO

DESIGN_CLOSURE:
- runtime update fields fixed: YES | NO
- removal semantics fixed: YES | NO
- workload boundary fixed: YES | NO
- closeout strategy fixed: YES | NO

CHANGES_EXECUTED:
- establish-springboot-management-foundation: COMPLETED | BLOCKED | NOT_STARTED
- establish-local-managed-executor-registry: COMPLETED | BLOCKED | NOT_STARTED
- expose-experiment-workloads-and-observability: COMPLETED | BLOCKED | NOT_STARTED

IMPLEMENTED_V1:
- dependencies added:
- API endpoints:
- runtime update fields:
- workload scenarios:
- metrics/observations:
- implementation receipt:

EXCLUSION_CHECK:
- scheduling/recovery implementation added: NO
- Redis/Kafka/database added: NO
- frontend/authentication added: NO
- multi-node/virtual-thread implementation added: NO
- openspec/schemas modified: NO
- .codex/.claude modified: NO

VALIDATION:
- .\mvnw.cmd test: PASS | FAIL
- .\mvnw.cmd verify: PASS | FAIL
- openspec.cmd validate --all --json: PASS | FAIL
- openspec.cmd schema validate superspec: PASS | FAIL
- final claude_master validation after merge: PASS | FAIL | NOT_REACHED

EVIDENCE:
- design closure commit:
- change 1 commit:
- change 2 commit:
- change 3 commit:
- receipt commit:
- final merge commit:

NEXT_ACTION:
- If COMPLETED_V1_INTEGRATED: ChatGPT may audit remote V1 result; no user action required.
- If BLOCKED_CLOSEOUT: ChatGPT should assess remote verified branch and issue only the minimum closeout remedy.
- Otherwise: state the genuine blocker and preserve all pushed evidence.
```

---

## 17. Execution Start Instruction

Begin immediately after reading this mission. Do not stop for clarification or per-change approval unless a documented `BLOCKED` condition is encountered.
