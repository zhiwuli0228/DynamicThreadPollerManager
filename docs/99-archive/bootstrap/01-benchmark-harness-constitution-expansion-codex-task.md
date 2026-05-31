# DynamicThreadPollerManager 标杆化增强 Phase 01 执行任务书
## 修正初始化基线并建立 Harness Constitution

> 执行端：Codex  
> 审核端：ChatGPT  
> 目标仓库：`https://github.com/zhiwuli0228/DynamicThreadPollerManager`  
> 目标分支：`claude_master`  
> 任务类型：项目治理文档增强，不涉及业务实现  
> 后续阶段：Living Architecture Baseline

---

## 1. 本轮背景与已知事实

当前 `claude_master` 已包含最小初始化成果：

- OpenSpec 与 SuperSpec schema 已接入；
- 已存在 `.codex/`、`.claude/`、`AGENTS.md`、`CLAUDE.md`；
- 已存在 `openspec/config.yaml`；
- 已存在 `docs/harness/project-harness.md`；
- 已存在 `docs/bootstrap/bootstrap-ledger.md`；
- 当前没有动态线程池业务实现代码。

但远端审查发现以下问题需要在本轮修正：

1. `docs/bootstrap/bootstrap-ledger.md` 仍将有效分支记录为 `main`，与实际初始化和后续验收基线 `claude_master` 不一致。
2. 当前 Harness 只有 `docs/harness/project-harness.md` 一份简要文件，不能完整承载标杆型项目的项目宪章、领域范围、依赖规则、测试规则、AI 协作与 change 门禁。
3. 现有 roadmap 以 `Local managed executor registry` 作为第一项能力；对于标杆化项目，后续应先设计 `establish-springboot-technical-foundation` 技术基础 change，再进入线程池领域能力设计。
4. 历史 minimal bootstrap 任务书可以保留用于经验追溯，但不能继续作为当前项目治理依据。

本轮仅修正上述治理资产，不建立详细 Architecture，不创建 OpenSpec change，不实现代码。

---

## 2. 本轮目标

在 `claude_master` 分支上完成以下交付：

1. 修正 bootstrap ledger 中的真实基线分支与治理结论。
2. 将单文件 Harness 升级为结构化的 Harness Constitution 文档集。
3. 保留 `docs/harness/project-harness.md` 作为兼容入口索引，避免现有 Agent 入口立即失效。
4. 在文档中明确后续架构阶段与 change 顺序调整。
5. 验证本轮没有修改源码、依赖或 OpenSpec change。
6. commit 并 push 到 `origin/claude_master`。

---

## 3. 强制边界

### 3.1 允许修改

本轮只允许修改或创建以下内容：

```text
docs/bootstrap/bootstrap-ledger.md
docs/harness/project-harness.md
docs/harness/00-project-constitution.md
docs/harness/01-domain-and-experiment-scope.md
docs/harness/02-architecture-and-dependency-rules.md
docs/harness/03-engineering-and-testing-rules.md
docs/harness/04-ai-delivery-workflow.md
docs/harness/05-change-classification-and-gates.md
```

如果你认为必须同步调整其他文件，停止扩大范围，在最终响应中列为 `RECOMMENDED_NEXT_PHASE_ADJUSTMENT`，本轮不要执行。

### 3.2 明确禁止

本轮不得：

- 修改 `pom.xml`；
- 修改 `src/main/**` 或 `src/test/**`；
- 修改 `openspec/config.yaml`；
- 修改 `.codex/**` 或 `.claude/**`；
- 修改 `AGENTS.md` 或 `CLAUDE.md`；
- 创建 `docs/architecture/**`；
- 创建 `openspec/changes/**`；
- 新增任何依赖；
- 编写线程池、调度、监控、REST API 或测试实现代码；
- 执行 `/opsx:new`、`/opsx:apply`、`/opsx:verify` 等业务 change 流程；
- 删除历史 minimal bootstrap 任务书；
- 将尚未实现的能力写成已实现功能；
- 将本轮顺带扩展成 CI、静态扫描、发布门禁或生产化治理建设。

---

## 4. 执行前读取与核验

在修改前完整读取：

```text
docs/bootstrap/bootstrap-ledger.md
docs/harness/project-harness.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
openspec/schemas/superspec/schema.yaml
```

执行：

```powershell
git switch claude_master
git pull --ff-only origin claude_master
git status --short --branch
git log --oneline --decorate -5
.\mvnw.cmd test
```

要求：

- 必须基于 `claude_master` 最新远端状态工作。
- 若 `git pull --ff-only` 失败、工作树存在无法解释的本地改动，或目标分支不存在，返回 `BLOCKED`，不得自行覆盖或强制处理。
- 记录开始时的 HEAD SHA，供最终响应使用。

---

## 5. 修改 `docs/bootstrap/bootstrap-ledger.md`

### 5.1 修正错误事实

将有效治理/验收基线修正为：

```text
Branch: `claude_master`
```

并明确增加以下结论：

```md
## Governance Baseline

- The authoritative branch for current bootstrap, governance enhancement,
  OpenSpec change review, and subsequent implementation acceptance is
  `claude_master`.
- The earlier minimal bootstrap is preserved as historical initialization
  evidence, but it is no longer the complete governance target for this
  benchmark-oriented demo.
```

### 5.2 增加本阶段记录

追加：

```md
## Benchmark Enhancement Progress

### Harness Constitution Expansion

- Status: completed in Phase 01 after validation and push.
- Reason: the minimal single-file harness was sufficient for toolchain
  bootstrap, but insufficient for a benchmark project intended to preserve
  architecture and AI-assisted delivery practices.
- Output:
  - `docs/harness/00-project-constitution.md`
  - `docs/harness/01-domain-and-experiment-scope.md`
  - `docs/harness/02-architecture-and-dependency-rules.md`
  - `docs/harness/03-engineering-and-testing-rules.md`
  - `docs/harness/04-ai-delivery-workflow.md`
  - `docs/harness/05-change-classification-and-gates.md`
```

如本轮尚未成功完成提交与推送，在实际完成前不得将状态伪写为 completed；可在编辑时先写 `pending validation and push`，在验证通过且提交前改为 completed。

### 5.3 记录后续顺序调整

增加：

```md
## Roadmap Correction

For benchmark-quality development, the first formal capability design change
will be:

`establish-springboot-technical-foundation`

Only after it is designed, approved, implemented, and verified should the
project proceed to:

`establish-local-managed-executor-registry`
```

---

## 6. 重构 `docs/harness/project-harness.md` 为兼容索引页

现有 `project-harness.md` 不能删除，因为当前 `AGENTS.md` 和 `CLAUDE.md` 已引用该路径。本轮将其重构为索引页，不保留与拆分文档重复的长正文。

内容应采用以下结构：

```md
# Project Harness Index

This file is the stable compatibility entrypoint for project governance.
The authoritative harness rules are organized in the following documents.

## Mandatory Reading for Design Work

1. `docs/harness/00-project-constitution.md`
2. `docs/harness/01-domain-and-experiment-scope.md`
3. `docs/harness/02-architecture-and-dependency-rules.md`
4. `docs/harness/03-engineering-and-testing-rules.md`
5. `docs/harness/04-ai-delivery-workflow.md`
6. `docs/harness/05-change-classification-and-gates.md`

## Current Governance Baseline

- Authoritative branch: `claude_master`.
- Current state: OpenSpec/SuperSpec minimal bootstrap completed; benchmark
  harness expansion established; Living Architecture remains a subsequent
  documentation phase.
- Current implementation status: no dynamic thread-pool capability has been
  implemented yet.

## Rule of Precedence

For project work:

1. Active approved OpenSpec/SuperSpec change artifacts define the bounded
   delivery scope of a specific change.
2. Harness documents define durable project governance and boundary rules.
3. Architecture documents, once created, define the living system design.
4. No agent may infer implemented behavior solely from roadmap documentation.
```

不要在索引页再次复制全部规则正文。

---

## 7. 创建 Harness Constitution 文档集

创建以下六个文件。文档允许使用中文阐述，技术术语、类名候选、路径和 change 名保持英文。

```text
docs/harness/
├─ 00-project-constitution.md
├─ 01-domain-and-experiment-scope.md
├─ 02-architecture-and-dependency-rules.md
├─ 03-engineering-and-testing-rules.md
├─ 04-ai-delivery-workflow.md
└─ 05-change-classification-and-gates.md
```

---

# 7.1 `00-project-constitution.md`

## 目标

定义该项目长期不轻易变化的使命、定位、质量目标与非目标。

## 必须包含的章节

```md
# Project Constitution

## 1. Project Identity
## 2. Mission
## 3. Problem Origin
## 4. Project Nature
## 5. Quality Objectives
## 6. Explicit Non-Goals
## 7. Governance Principles
## 8. Current Status Declaration
```

## 内容要求

### Project Identity

明确：

- 项目名称：`DynamicThreadPollerManager`；
- 项目类型：Java 21 + Spring Boot exploratory demo / laboratory project；
- 治理与验收基线分支：`claude_master`。

### Mission

说明项目用于探索和验证：

- 动态受管线程池的注册、查询和运行时参数调整；
- 动态调度周期调整与任务链重建；
- 执行可观测性、模拟工作负载和实验验证；
- 后续多节点唯一执行、恢复策略和虚拟线程模式评估。

### Problem Origin

说明该项目源于真实工程问题的验证诉求，例如：

- 调度任务周期在运行期动态变化；
- 被取消/过期的旧调度链可能残留；
- 任务长期不执行或卡死后的检测与重建；
- 多节点下仅一个节点应实际执行任务；
- 并发和远程 I/O 场景中资源控制与可观测性。

注意：这些是探索输入，不是当前已实现能力。

### Project Nature

明确：

- 本项目是验证型 Demo，不是已生产可用的平台；
- 追求可运行、可验证、可演进、可复用的方法论；
- 标杆化的对象是工程结构与 AI 协作流程，而不是提前堆叠生产基础设施。

### Quality Objectives

至少包括：

- `Traceable`：设计、实现和验证可沿 change 追溯；
- `Bounded`：每个 change 范围可控；
- `Testable`：并发行为和配置变更可验证；
- `Observable`：关键运行状态能够被观测；
- `Evolvable`：单节点实验可逐步演进至协调与恢复实验；
- `Agent-safe`：AI 实现端不能在压缩上下文后扩大修改范围。

### Explicit Non-Goals

至少排除：

- 当前阶段的生产级鉴权；
- 完整管理前端；
- 未经 change 批准的 Redis/Kafka/数据库引入；
- 提前建设复杂发布流水线和组织级门禁；
- 将 Demo 直接视为生产组件交付。

### Governance Principles

至少包括：

- Stable rules live in Harness.
- Living system design lives in Architecture.
- Bounded feature decisions live in OpenSpec/SuperSpec change artifacts.
- Codex designs; Claude Code implements approved changes.
- No unapproved scope expansion.

### Current Status Declaration

明确：

- 当前处于治理资产增强阶段；
- OpenSpec/SuperSpec 工具链已初始化；
- 动态线程池业务能力尚未实现；
- Architecture 文档将在下一阶段建立。

---

# 7.2 `01-domain-and-experiment-scope.md`

## 目标

定义领域问题、实验边界、术语词汇和演进顺序，防止 Agent 把未来规划当成当前实现。

## 必须包含的章节

```md
# Domain and Experiment Scope

## 1. Domain Problem Statement
## 2. Experiment Strategy
## 3. Ubiquitous Language
## 4. Capability Roadmap
## 5. Current Scope
## 6. Deferred Scope
## 7. Scope Control Rules
```

## 必须定义的术语

以表格形式定义，至少包含：

| Term | Definition | Current/Future |
|---|---|---|
| Managed Executor | 被项目注册、查询和受控更新的执行器抽象 | Future capability |
| Executor Definition | 线程池静态/期望配置描述 | Future capability |
| Runtime Snapshot | 某一时刻执行器实际状态快照 | Future capability |
| Configuration Update | 对允许动态变更参数的受控更新命令 | Future capability |
| Managed Scheduled Task | 可管理周期、状态和执行记录的周期任务 | Future capability |
| Schedule Version | 防止旧调度链继续生效的版本标识 | Future capability |
| Execution Record | 单次任务运行的结果与时间信息 | Future capability |
| Stall Detection Policy | 判断任务未正常执行并触发恢复的规则 | Future capability |
| Coordination Lease | 多节点下唯一执行资格的抽象 | Deferred future capability |

不得将术语表中的 future capability 写成当前已有实现。

## Capability Roadmap

将路线改为：

```text
0. establish-springboot-technical-foundation
1. establish-local-managed-executor-registry
2. expose-executor-runtime-metrics-and-workloads
3. support-dynamic-scheduled-task-reconfiguration
4. detect-and-rebuild-stalled-scheduling-chain
5. coordinate-single-execution-across-nodes
6. evaluate-virtual-thread-execution-mode
```

并说明：

- 第 0 步建立 Web/API、Validation、Observability 与测试承载边界的设计；
- 第 1 步才进入核心线程池能力；
- 每项能力必须由独立 change 管理；
- 路线顺序是当前设计基线，若 change 导致调整，必须同步 Architecture 和 Harness 中相关决策。

---

# 7.3 `02-architecture-and-dependency-rules.md`

## 目标

固化长期架构约束，不设计具体实现细节。

## 必须包含的章节

```md
# Architecture and Dependency Rules

## 1. Architectural Style
## 2. Dependency Direction
## 3. Layer Responsibilities
## 4. Forbidden Dependencies
## 5. Infrastructure Introduction Rules
## 6. Concurrency Boundary Rules
## 7. Architecture Change Governance
```

## 强制规则

必须写入：

```text
api -> application -> domain
infrastructure -> domain
experiment -> application/domain ports as approved by change design
```

并阐明：

| Layer | Responsibility | Must Not Do |
|---|---|---|
| `api` | HTTP contract、DTO、request validation、response mapping | 不直接操作 executor 实现 |
| `application` | 用例编排、命令/查询协调 | 不直接承载线程池底层状态算法 |
| `domain` | 核心模型、不变量、端口和策略 | 不依赖 Spring MVC DTO、Redis/Kafka 客户端、Micrometer 具体实现 |
| `infrastructure` | JDK executor、metrics、future Redis/Kafka adapter | 不反向定义领域规则 |
| `experiment` | 工作负载与实验场景 | 不成为生产业务逻辑入口 |

必须明确：

- 当前只允许 single-node / in-memory 作为首轮能力边界；
- Redis、Kafka、database、frontend、authentication 必须分别由明确 change 授权；
- queue capacity 在线动态替换不得在未完成设计前直接实现；
- 与线程池、调度、恢复相关的核心行为必须通过 domain/application 边界表达，而不是散落在 Controller 或 infrastructure 中。

---

# 7.4 `03-engineering-and-testing-rules.md`

## 目标

定义实现端必须遵守的 Java、测试、并发验证与范围控制规则。

## 必须包含的章节

```md
# Engineering and Testing Rules

## 1. Technology Baseline
## 2. Coding Rules
## 3. Testing Pyramid for This Demo
## 4. Deterministic Concurrency Testing
## 5. Observability and Error Handling Rules
## 6. Prohibited Engineering Behaviors
## 7. Verification Evidence
```

## 强制内容

### Technology Baseline

写入当前真实基线：

- Java 21；
- Spring Boot 4.0.6；
- Maven Wrapper；
- JUnit 5；
- Mockito；
- 不使用 PowerMock。

### Deterministic Concurrency Testing

至少写入：

- 禁止主要依赖长时间 `Thread.sleep` 验证并发正确性；
- 优先使用 `CountDownLatch`、`CyclicBarrier`、受控 `Executor`、可替换时钟/调度器、轮询超时断言等方式；
- 所有并发测试必须有超时保护，避免测试永久挂起；
- 对动态配置更新、版本失效、任务取消/重建等行为必须有正向与失败路径测试。

### Observability and Error Handling

写入：

- 非法配置转换不得静默忽略；
- 执行器或任务状态变化必须可通过查询、指标、日志或测试证据观测；
- 任务拒绝、任务失败、重建触发等关键事件应在对应 change 中定义可验证信号。

### Prohibited Behaviors

写入：

- 不进行无关重构；
- 不在功能 change 中顺手替换技术栈；
- 不创建仅用于“看起来架构完整”的空实现类；
- 不因 Agent 推测而实现未批准功能。

---

# 7.5 `04-ai-delivery-workflow.md`

## 目标

固化你当前采用的 AI 辅助开发协作链路，并避免执行端扩范围。

## 必须包含的章节

```md
# AI Delivery Workflow

## 1. Roles
## 2. Authoritative Inputs
## 3. Delivery Workflow
## 4. Context Minimization Rules
## 5. Remote Review and Git Rules
## 6. Blocked Conditions
## 7. Anti-Drift Rules
```

## Roles

必须明确：

| Role | Responsibility | Prohibited by Default |
|---|---|---|
| ChatGPT | 总设计、审查远端状态、输出下一步任务书、审查 change/实现结果 | 不直接声称未检查的仓库状态 |
| Codex | 项目设计文档、OpenSpec/SuperSpec 设计工件、架构和范围决策落地 | 不默认实现业务代码 |
| Claude Code | 按批准工件实现、测试、验证、提交与推送 | 不改变已批准范围和架构边界 |
| User | 下发任务文件、决定是否继续下一阶段、提供外部权限操作 | 不需人工复制可由远端直接核查的执行全文 |

## Delivery Workflow

写入：

```text
Harness / Architecture baseline
  -> Codex creates bounded SuperSpec design artifacts
  -> ChatGPT reviews pushed design artifacts from GitHub
  -> Claude Code implements approved change
  -> Claude Code verifies and pushes evidence
  -> ChatGPT reviews remote implementation and authorizes next change
```

## Context Minimization Rules

必须包括：

- 实现会话只读取 active change 所需的 Harness/Architecture 文档，不灌入全部历史对话；
- `openspec/config.yaml` 只注入摘要化高杠杆规则；
- change 只承担一个 capability；
- 长期规则不重复复制进每个 change；
- GitHub 远端分支为审查事实来源，当前以 `claude_master` 为准。

## Blocked Conditions

只有遇到以下情况才允许返回阻断：

- 账号登录或授权；
- 无法非交互完成的 push 权限；
- 仓库冲突无法安全 fast-forward；
- 必需工具确实不可用且无法按任务授权安装；
- 已批准设计工件与真实实现约束发生不可自行解决的冲突。

---

# 7.6 `05-change-classification-and-gates.md`

## 目标

定义不同规模变更采用的流程，避免小 Demo 被完整流程拖慢，也避免核心并发能力无设计落地。

## 必须包含的章节

```md
# Change Classification and Gates

## 1. Why Change Classification Exists
## 2. Change Types
## 3. Mandatory Gates by Type
## 4. Scope Escalation Rules
## 5. Evidence Requirements
## 6. Current Planned Capability Changes
```

## Change Types

采用以下分类：

| Type | Examples | Required Flow |
|---|---|---|
| Bootstrap / Governance Change | Harness、Architecture、Agent 入口、工具链配置对齐 | Codex bounded task → validate → commit/push → ChatGPT remote review |
| Capability Change | 线程池 Registry、动态调度、恢复、分布式协调 | Codex SuperSpec full design → ChatGPT approval → Claude Code apply/verify → remote review |
| Minor Correction | 拼写、链接、非行为性文档修复 | Direct bounded edit → targeted validation → commit |
| Emergency Correction | 后续 Demo 代码中的阻断性小 bug | 先定义最小修改范围与回归命令；是否创建 change 由影响面决定 |

## Mandatory Gates

必须明确：

- Capability Change 未经设计工件审核不得进入实现；
- 若实现过程中需要新增未批准依赖、突破架构边界或扩展 capability，Claude Code 必须停止并请求设计修订；
- Redis、Kafka、database、frontend、authentication、virtual thread mode 的首次引入一律视为 Capability Change；
- 并发语义改变一律视为 Capability Change；
- 仅文档迁移、索引修复和事实校正属于 Bootstrap / Governance Change。

## Current Planned Capability Changes

列出：

```text
establish-springboot-technical-foundation
establish-local-managed-executor-registry
expose-executor-runtime-metrics-and-workloads
support-dynamic-scheduled-task-reconfiguration
detect-and-rebuild-stalled-scheduling-chain
coordinate-single-execution-across-nodes
evaluate-virtual-thread-execution-mode
```

并说明每次只允许一个 change 处于主要设计/实现推进状态。

---

## 8. 文档质量与一致性要求

所有新建 Harness 文档必须满足：

- 文档名称、标题和内容职责一致；
- 不重复复制大段正文；
- 不声称尚未实现的代码或功能已经存在；
- 不将 Phase 02 才要完成的详细领域模型、Mermaid 架构图、调度状态机提前写入本阶段；
- 可引用下一阶段将建立 `docs/architecture/`，但不得创建其文件；
- 语言可为中文，但 Java package、类名候选、change 名、文件路径保持英文；
- 文档应可长期保留，不写一次性的执行流水账。

---

## 9. 验证要求

完成编辑后，执行：

```powershell
git diff --name-only
git diff --stat
.\mvnw.cmd test
git status --short --branch
```

## 9.1 强制 Scope Check

根据 `git diff --name-only` 验证：

```text
允许变化：
- docs/bootstrap/bootstrap-ledger.md
- docs/harness/**

必须没有变化：
- pom.xml
- src/main/**
- src/test/**
- openspec/**
- AGENTS.md
- CLAUDE.md
- .codex/**
- .claude/**
```

若出现禁止范围修改：

- 撤销本轮造成的禁止范围修改；
- 重新验证；
- 若无法安全撤销，返回 `BLOCKED_SCOPE_VIOLATION`，不得提交。

## 9.2 测试结果

要求：

```text
.\mvnw.cmd test: PASS
```

若基线测试本身失败：

- 不尝试顺手修代码；
- 记录实际失败；
- 返回 `BLOCKED_BASELINE_TEST_FAILURE`。

---

## 10. Commit 与 Push

验证通过后执行：

```powershell
git add docs/bootstrap/bootstrap-ledger.md docs/harness
git commit -m "docs: establish benchmark harness constitution"
git push origin claude_master
```

禁止：

- force push；
- 将无关未跟踪文件加入提交；
- squash 或 rewrite 已存在初始化提交；
- 将本轮任务书作为仓库产物再次提交，除非它本来已由用户放入仓库且明确要求保留。

---

## 11. 完成判定

本轮只有同时满足以下条件才为 `COMPLETED`：

- 工作基线分支为 `claude_master`；
- ledger 已纠正分支事实并记录标杆化增强状态；
- `project-harness.md` 已转为稳定兼容索引页；
- 六份 Harness Constitution 文档已创建；
- 未修改源码、依赖、OpenSpec 配置和 Agent 入口；
- Maven 测试通过；
- commit 已创建；
- push 至 `origin/claude_master` 成功。

---

## 12. 最终返回格式

完成后仅返回以下摘要，不粘贴全部文档正文：

```text
STATUS: COMPLETED | BLOCKED | BLOCKED_PUSH | BLOCKED_SCOPE_VIOLATION | BLOCKED_BASELINE_TEST_FAILURE
PHASE: 01-benchmark-harness-constitution-expansion
REPOSITORY: DynamicThreadPollerManager
BRANCH: claude_master
START_HEAD:
END_HEAD:
PUSHED: YES | NO

FILES_CREATED:
- ...

FILES_UPDATED:
- ...

VALIDATION:
- git diff scope check: PASS | FAIL
- .\mvnw.cmd test: PASS | FAIL

SCOPE_CHECK:
- pom.xml changed: NO
- src/main changed: NO
- src/test changed: NO
- openspec changed: NO
- AGENTS.md / CLAUDE.md changed: NO
- dynamic thread-pool business implementation added: NO

NEXT_PHASE:
- docs/architecture Living Architecture Baseline
```

---

## 13. 本轮结束后的协作方式

本轮推送成功后，不要自行开始 Phase 02。

用户只需通知 ChatGPT：

```text
Phase 01 已推送，请检查 claude_master。
```

ChatGPT 将直接基于 GitHub 远端审查 Harness 产物，并在通过后输出 Phase 02 的 Architecture 执行任务书。
