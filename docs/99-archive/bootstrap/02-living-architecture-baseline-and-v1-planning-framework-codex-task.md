# DynamicThreadPollerManager 标杆化增强 Phase 02 执行任务书
## Phase 01 遗留修正与 Living Architecture Baseline 建设

> 执行端：Codex  
> 审核端：ChatGPT  
> 目标仓库：`https://github.com/zhiwuli0228/DynamicThreadPollerManager`  
> 目标分支：`claude_master`  
> 任务类型：项目框架设计资产建设，不涉及业务实现  
> 当前战略修正：先完整搭建 Harness / Architecture / Delivery Framework，再统一规划第一个版本的设计；本阶段不得创建任何功能 change。

---

## 1. 本轮背景与远端审查结论

`claude_master` 已完成 OpenSpec、SuperSpec 与最小 Agent 入口初始化，并已新增六份 Harness Constitution 文档：

```text
docs/harness/
├─ 00-project-constitution.md
├─ 01-domain-and-experiment-scope.md
├─ 02-architecture-and-dependency-rules.md
├─ 03-engineering-and-testing-rules.md
├─ 04-ai-delivery-workflow.md
└─ 05-change-classification-and-gates.md
```

上述六份文档内容方向基本符合标杆化目标，包括：

- 以 `claude_master` 作为治理和验收基线；
- Stable rules / Living Architecture / bounded change artifacts 的分层原则；
- Codex 负责设计，Claude Code 负责批准后的实现；
- 动态线程池、动态调度、恢复与分布式协调作为未来能力；
- 并发测试需避免以长时间 `Thread.sleep` 作为主要证明手段。

但 Phase 01 仍存在必须在本轮首先修正的未闭环项：

1. `docs/bootstrap/bootstrap-ledger.md` 仍记录 `Branch: main`，未同步为 `claude_master`。
2. `docs/harness/project-harness.md` 仍保留 minimal bootstrap 版本正文，未改造成稳定兼容索引页，且仍保留“先做 Local managed executor registry”的旧路线。
3. 当前不存在 `docs/architecture/`，尚未建立完整项目框架所需的 Living Architecture。
4. 用户已明确修正推进策略：**先完整搭建项目框架，第一个版本的设计在框架完成后统一规划**。因此，不得继续将 `establish-springboot-technical-foundation` 描述为“下一步立即创建的首个 change”。

---

## 2. 本轮目标

本轮需要一次性完成两类工作：

### A. 闭合 Phase 01 遗留项

- 修正 `bootstrap-ledger.md` 的事实与推进策略；
- 将 `project-harness.md` 改造成兼容入口索引；
- 对 Harness 中将 capability change 描述为立即下一步的表述做最小必要调整，改为“候选能力路线，待首版统一规划后确定”。

### B. 建立 Living Architecture Baseline

创建完整的 `docs/architecture/` 架构设计资产，覆盖：

- 系统目标、上下文和质量属性；
- 逻辑模块与包结构边界；
- 动态受管线程池领域模型；
- 动态调度、版本失效与恢复模型；
- 可观测性与实验验证策略；
- 运行、安全、配置和演进边界；
- 首版统一设计规划框架与待决策清单。

本轮的完成状态应是：

```text
Harness Constitution 已建立且入口一致；
Living Architecture 基线已建立；
OpenSpec/SuperSpec 接入仍保持可用；
首版能力设计尚未创建 change；
后续可进入 Toolchain Alignment / V1 Unified Design Planning 阶段。
```

---

## 3. 强制边界

### 3.1 本轮允许修改或创建

```text
docs/bootstrap/bootstrap-ledger.md
docs/harness/project-harness.md
docs/harness/00-project-constitution.md              # 仅在需同步当前推进策略时最小修改
docs/harness/01-domain-and-experiment-scope.md       # 仅在需将 immediate change 改为 candidate roadmap 时最小修改
docs/harness/04-ai-delivery-workflow.md              # 仅在需体现“框架先行、首版后续统一设计”时最小修改
docs/harness/05-change-classification-and-gates.md   # 仅在需调整 planned -> candidate wording 时最小修改
docs/architecture/00-system-context-and-quality-attributes.md
docs/architecture/01-logical-architecture-and-package-boundaries.md
docs/architecture/02-managed-executor-domain-model.md
docs/architecture/03-scheduling-reconfiguration-and-recovery-model.md
docs/architecture/04-observability-and-experiment-strategy.md
docs/architecture/05-operational-and-evolution-boundaries.md
docs/architecture/06-v1-unified-design-planning-framework.md
docs/architecture/README.md
```

### 3.2 本轮禁止修改

```text
pom.xml
src/main/**
src/test/**
openspec/**
AGENTS.md
CLAUDE.md
.codex/**
.claude/**
README.md
```

说明：`AGENTS.md`、`CLAUDE.md`、`openspec/config.yaml` 与根目录 `README.md` 的统一对齐属于下一阶段；本轮只建设长期设计事实源，避免同时修改过多入口。

### 3.3 本轮明确禁止执行

- 不创建 `openspec/changes/**`。
- 不创建 `establish-springboot-technical-foundation` change。
- 不创建任何动态线程池相关 change。
- 不执行 `/opsx:new`、`/opsx:continue`、`/opsx:apply`、`/opsx:verify` 或 `/opsx:archive`。
- 不修改 Java 源码、测试源码或依赖。
- 不将架构目标写成已实现事实。
- 不引入 CI、静态扫描、部署配置、Redis、Kafka、数据库、前端或认证。
- 不把本轮架构文档拆成空壳文件；每份必须形成真实设计基线。

---

## 4. 执行前读取与基线确认

在修改前完整读取：

```text
docs/bootstrap/bootstrap-ledger.md
docs/harness/project-harness.md
docs/harness/00-project-constitution.md
docs/harness/01-domain-and-experiment-scope.md
docs/harness/02-architecture-and-dependency-rules.md
docs/harness/03-engineering-and-testing-rules.md
docs/harness/04-ai-delivery-workflow.md
docs/harness/05-change-classification-and-gates.md
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
git log --oneline --decorate -8
.\mvnw.cmd test
```

要求：

- 必须基于最新 `claude_master` 工作。
- 若存在未提交改动，先确认其是否属于已推送内容之外的本地脏数据；不得覆盖无法解释的改动。
- 若基线测试失败，不修代码，直接返回 `BLOCKED_BASELINE_TEST_FAILURE`。
- 记录起始 HEAD SHA。

---

## 5. 修正 Phase 01 遗留项

## 5.1 更新 `docs/bootstrap/bootstrap-ledger.md`

必须将错误分支事实修正为：

```md
- Branch: `claude_master`
```

并增加或更新以下内容：

```md
## Governance Baseline

- The authoritative branch for bootstrap review, governance enhancement,
  architecture baseline, subsequent design review, and implementation
  acceptance is `claude_master`.
- The minimal bootstrap established OpenSpec, SuperSpec and agent entrypoints.
- The benchmark enhancement phase expands durable Harness and Living
  Architecture assets before any first-version feature design begins.

## Benchmark Enhancement Progress

### Phase 01 - Harness Constitution Expansion

- Status: completed and reviewed with remediation items.
- Valid outputs:
  - `docs/harness/00-project-constitution.md`
  - `docs/harness/01-domain-and-experiment-scope.md`
  - `docs/harness/02-architecture-and-dependency-rules.md`
  - `docs/harness/03-engineering-and-testing-rules.md`
  - `docs/harness/04-ai-delivery-workflow.md`
  - `docs/harness/05-change-classification-and-gates.md`
- Remediation completed in Phase 02:
  - Corrected the authoritative branch record.
  - Converted `project-harness.md` into a stable compatibility index.
  - Removed the implication that a feature change should start before the
    full project framework baseline is complete.

### Phase 02 - Living Architecture Baseline

- Status: completed after validation and push.
- Purpose: establish the target system model, domain concepts, experimental
  strategy, and first-version planning framework without implementing code or
  creating feature changes.
- Output:
  - list all created `docs/architecture/*.md` documents.
```

在验证和 push 前，可暂时写 `pending validation and push`；提交前必须更新为真实状态。

## 5.2 重构 `docs/harness/project-harness.md`

该文件必须转为稳定兼容入口，禁止继续保留旧版压缩正文和旧版 roadmap。目标内容结构如下：

```md
# Project Harness Index

This file is the stable compatibility entrypoint for project governance.
The authoritative durable rules are maintained in the structured Harness
documents listed below.

## Mandatory Reading for Governance and Design

1. `docs/harness/00-project-constitution.md`
2. `docs/harness/01-domain-and-experiment-scope.md`
3. `docs/harness/02-architecture-and-dependency-rules.md`
4. `docs/harness/03-engineering-and-testing-rules.md`
5. `docs/harness/04-ai-delivery-workflow.md`
6. `docs/harness/05-change-classification-and-gates.md`

## Architecture Reading

The Living Architecture baseline is maintained under:

- `docs/architecture/README.md`

Detailed architecture documents must be read according to the scope of the
design or implementation task.

## Current Governance Baseline

- Authoritative branch: `claude_master`.
- OpenSpec / SuperSpec toolchain bootstrap: completed.
- Harness Constitution: established.
- Living Architecture baseline: established by Phase 02 once this task passes validation.
- First-version design: not yet planned as a change.
- Dynamic thread-pool business capability implementation: not started.

## Rule of Precedence

1. Durable governance rules are defined by the Harness documents.
2. Target system structure and design constraints are defined by the Living
   Architecture documents.
3. A future approved OpenSpec/SuperSpec change defines only the bounded scope
   for its specific design or implementation effort.
4. Roadmaps and candidate capabilities do not prove that a behavior has been
   implemented.
```

## 5.3 调整 Harness 中“立即创建首个 Change”的误导表述

读取六份新 Harness 文档，保留有效正文，仅最小修订以下语义：

- `establish-springboot-technical-foundation` 不再写成“下一步必须立即创建的第一个正式 change”；
- 将其与其他能力统一标注为 **candidate capability decomposition** 或 **candidate change sequence**；
- 增加声明：首版边界与 change 切分将在框架资产全部建立、工具链重新对齐后，由后续统一设计阶段确定；
- 不删除该 candidate sequence，因为它仍具有规划参考价值；
- 不在 Harness 中提前书写首版需求正文。

---

## 6. 创建 `docs/architecture/README.md`

## 目的

作为 Living Architecture 的稳定导航入口，供后续 `AGENTS.md`、`CLAUDE.md`、`openspec/config.yaml` 在下一阶段引用。

## 必须包含

```md
# Living Architecture Index

## Purpose

说明本目录描述目标系统模型、边界、质量属性与实验策略；它是设计基线，不是当前实现清单。

## Current Status

- Architecture baseline status: established in Phase 02.
- Current implementation status: no dynamic thread-pool business capability implemented.
- First-version design status: pending unified planning after framework completion.

## Document Map
```

列出下述 7 份 architecture 文档及其阅读场景。

```md
## Reading Rules

- Governance or scope work must read Harness first.
- Unified version design must read all architecture documents.
- A bounded implementation task reads only the architecture documents relevant
  to its approved change.
- No document in this directory proves implementation completion.
```

---

## 7. 创建 Living Architecture 文档

# 7.1 `00-system-context-and-quality-attributes.md`

## 目的

定义系统为什么存在、与哪些外部角色交互、它将以什么质量属性指导首版设计。

## 必须包含的章节

```md
# System Context and Quality Attributes

## 1. System Objective
## 2. Context and Actors
## 3. Current Baseline vs Target Architecture
## 4. Deployment Evolution
## 5. Quality Attribute Scenarios
## 6. Architecture Constraints
## 7. Open Questions for V1 Unified Design
```

## 具体要求

### Context and Actors

至少描述：

| Actor / External Element | Role | Current or Future |
|---|---|---|
| Operator / Developer | 触发实验、查询状态、修改配置 | Target V1 consideration |
| REST API Client | 访问管理与实验接口 | Target V1 consideration |
| DynamicThreadPollerManager Application | 实验系统主体 | Current shell / target system |
| JVM Executor Runtime | 承载实际线程池和调度行为 | Target capability |
| Metrics Consumer | 读取实验指标 | Future / V1 decision |
| Redis Coordination Backend | 提供多节点协调候选能力 | Deferred |
| External Configuration Source | 配置输入候选来源 | Deferred |

### 图示要求

必须包含一份 Mermaid system context diagram，并清楚区分：

- 当前：只有 Spring Boot shell + governance/toolchain；
- 目标首版：尚待统一设计；
- 后续实验能力：Redis、多节点等 deferred。

### Quality Attribute Scenarios

以表格定义至少：

- Modifiability；
- Testability；
- Observability；
- Safety of runtime reconfiguration；
- Scope traceability；
- Experimental reproducibility。

每项写明 stimulus、expected response、how it will be verified in future design。

---

# 7.2 `01-logical-architecture-and-package-boundaries.md`

## 目的

定义未来代码结构承载方式和依赖方向，但不创建任何 Java 类或空包。

## 必须包含的章节

```md
# Logical Architecture and Package Boundaries

## 1. Target Logical Components
## 2. Dependency Rules
## 3. Candidate Java Package Map
## 4. Component Responsibilities
## 5. Cross-Cutting Concerns
## 6. Packaging Decisions Deferred to V1 Design
```

## 必须设计的 logical components

| Component | Responsibility |
|---|---|
| API Adapter | REST contracts、DTO mapping、validation error exposure |
| Application Services | command/query orchestration |
| Executor Domain | managed executor definition、validation、runtime state semantics |
| Scheduling Domain | schedule definition、versioning、rebuild semantics |
| Monitoring Domain / Ports | state snapshot、execution record、metric event abstractions |
| Coordination Port | future single-execution lease contract |
| Infrastructure Adapters | JDK executor、metrics、future Redis implementation |
| Experiment Workloads | controlled load and failure scenarios |

## Package Map 规则

读取现有 Spring Boot application 的实际 package 名，将以下内容写成候选包映射；不得猜测或强制改源码：

```text
<actual.root.package>.api
<actual.root.package>.application
<actual.root.package>.domain.executor
<actual.root.package>.domain.scheduling
<actual.root.package>.domain.monitoring
<actual.root.package>.domain.coordination
<actual.root.package>.infrastructure
<actual.root.package>.experiment
```

必须说明：

- candidate packages are design targets only;
- V1 unified design decides which packages are introduced in its first implementation slice;
- 禁止为了匹配文档提前创建空目录或空类。

### 图示要求

至少一份 Mermaid component/dependency diagram。

---

# 7.3 `02-managed-executor-domain-model.md`

## 目的

完整描述动态线程池能力的目标领域模型与关键不变量，作为未来首版设计的输入。

## 必须包含的章节

```md
# Managed Executor Domain Model

## 1. Design Problem
## 2. Domain Concepts
## 3. Configuration Model
## 4. Runtime Snapshot Model
## 5. Configuration Update Semantics
## 6. Invariants and Validation Rules
## 7. Failure and Rejection Semantics
## 8. Candidate Use Cases
## 9. Deferred Decisions
```

## 必须定义的目标概念

| Concept | Responsibility |
|---|---|
| `ManagedExecutorId` | 稳定标识受管执行器 |
| `ExecutorDefinition` | 期望配置与元数据 |
| `ExecutorRuntimeSnapshot` | 实际运行状态读取模型 |
| `ExecutorConfigUpdate` | 运行期允许调整的命令 |
| `ManagedExecutorRegistry` | 注册、定位、查询受管执行器 |
| `ExecutorConfigValidator` | 配置转换不变量验证 |
| `TaskRejectionObservation` | 拒绝行为的可观测表达 |

## 必须说明的配置范围

将候选动态参数与非首版默认范围分开写明：

| Configuration Area | Candidate Initial Support | Deferred / Requires Design |
|---|---|---|
| `corePoolSize` | 可作为首版候选 | 需确定更新 API 与校验规则 |
| `maximumPoolSize` | 可作为首版候选 | 需确定合法过渡顺序 |
| `keepAliveTime` | 可作为首版候选 | 需确定单位与生效结果 |
| `allowCoreThreadTimeOut` | 待首版决定 | 不预设 |
| queue capacity replacement | 不作为默认首版范围 | 必须单独设计 |
| rejection policy runtime replacement | 待首版决定 | 必须定义风险与观测 |

## 必须定义的不变量

至少包括：

- `corePoolSize >= 0`；
- `maximumPoolSize > 0`；
- `maximumPoolSize >= corePoolSize`；
- 运行期从旧配置到新配置的应用顺序必须避免 JDK executor 非法中间态；
- 非法变更必须显式失败，不得静默忽略；
- 更新前后状态必须可被验证；
- 队列替换不因“动态线程池”字面含义而被默认包含。

### 图示要求

至少一份 Mermaid configuration update sequence diagram 或领域关系图。

---

# 7.4 `03-scheduling-reconfiguration-and-recovery-model.md`

## 目的

把用户已遇到的动态调度问题形成正式设计模型，供统一首版规划判断是否纳入 V1 或延后。

## 必须包含的章节

```md
# Scheduling Reconfiguration and Recovery Model

## 1. Problem Statement
## 2. Target Concepts
## 3. Desired Reconfiguration Semantics
## 4. Stale Schedule Prevention
## 5. Stall Detection and Recovery
## 6. Single-Node vs Distributed Semantics
## 7. Candidate Scenarios for V1 Planning
## 8. Deferred Decisions
```

## 必须定义的目标概念

| Concept | Purpose |
|---|---|
| `ManagedScheduledTaskId` | 标识可管理周期任务 |
| `ScheduleDefinition` | 期望周期、开关与触发规则 |
| `ScheduleVersion` | 防止旧任务链继续生效 |
| `TaskExecutionRecord` | 执行起止、成功失败和异常摘要 |
| `StallDetectionPolicy` | 多周期未执行或卡死的识别规则 |
| `ScheduleRebuildDecision` | 是否重建调度链的判断结果 |
| `ExecutionCoordinationPort` | 后续分布式唯一执行抽象 |

## 目标语义必须讨论

基于真实探索目标，明确记录：

```text
当周期配置发生变化时，候选目标语义为：
1. 旧调度链失效；
2. 变更后的任务允许立即触发一次；
3. 后续按新周期运行；
4. 旧版本任务即使迟到触发，也不得继续产生有效业务执行；
5. 若任务超过预期周期未成功执行，应支持检测并评估重建。
```

该语义必须标注为 **target design input / pending V1 inclusion decision**，不能描述为当前已实现。

### 图示要求

至少包括一份 Mermaid sequence diagram，展示：

```text
configuration change -> invalidate old version -> immediate trigger candidate -> new recurring chain -> stale invocation rejected
```

另需给出 single-node 与 future distributed coordination 的边界说明。

---

# 7.5 `04-observability-and-experiment-strategy.md`

## 目的

定义如何证明动态线程池和调度方案有效，而不是只实现接口。

## 必须包含的章节

```md
# Observability and Experiment Strategy

## 1. Experiment Objectives
## 2. Observation Model
## 3. Candidate Metrics
## 4. Controlled Workloads
## 5. Failure Injection Scenarios
## 6. Verification Strategy
## 7. Tooling Decisions Deferred to V1 Design
```

## Candidate Metrics

至少包含：

| Metric / Observation | Purpose | Candidate Phase |
|---|---|---|
| active thread count | 观察即时并发占用 | Executor experiment |
| current pool size | 观察实际扩容/收缩 | Executor experiment |
| queue depth | 观察积压风险 | Executor experiment |
| completed task count | 观察吞吐 | Executor experiment |
| rejection count | 观察过载与策略效果 | Executor experiment |
| task duration | 观察负载与超时 | Workload experiment |
| last execution timestamp | 观察周期任务健康状态 | Scheduling experiment |
| schedule version mismatch count | 观察旧链失效行为 | Scheduling experiment |
| rebuild count | 观察恢复策略触发 | Recovery experiment |
| coordination acquisition result | 观察多节点执行资格 | Distributed experiment |

## Controlled Workloads

至少设计：

- CPU-bound workload；
- blocking / simulated I/O workload；
- burst submission workload；
- intentionally stalled scheduled task；
- stale version invocation simulation；
- later distributed contention simulation。

## 验证策略

必须说明：

- Actuator/Micrometer 是候选承载技术，但是否进入首版、如何暴露由后续 V1 统一设计决定；
- 实验验证必须具备可重复条件和明确观察结果；
- 只做文档设计，不添加依赖或实现指标。

---

# 7.6 `05-operational-and-evolution-boundaries.md`

## 目的

定义系统运行边界、技术引入边界、实验到生产的差异，防止后续实现端将 Demo 扩成平台。

## 必须包含的章节

```md
# Operational and Evolution Boundaries

## 1. Current Operating Boundary
## 2. Configuration Boundary
## 3. Security Boundary
## 4. Persistence and Middleware Boundary
## 5. Deployment Evolution Boundary
## 6. Productionization Gap
## 7. ADR Trigger Rules
```

## 必须写入

- 当前框架阶段不引入业务依赖；
- 首版设计是否引入 Web、Validation、Actuator/Micrometer 尚待统一决策；
- Redis/Kafka/database/authentication/frontend/multi-node deployment 均不能被默认为首版组成；
- 即便实验支持多节点唯一执行，也不等同于生产级高可用；
- 只有影响长期结构或不可轻易撤销的决策才创建 ADR；
- 本阶段不批量创建空 ADR。

---

# 7.7 `06-v1-unified-design-planning-framework.md`

## 目的

落实用户最新要求：完整框架完成后，首个版本需要统一规划，而不是现在立即拆 capability changes。

该文档不是 V1 设计本身，而是 V1 统一设计阶段必须回答的问题模板。

## 必须包含的章节

```md
# V1 Unified Design Planning Framework

## 1. Purpose
## 2. Entry Preconditions
## 3. Questions V1 Design Must Resolve
## 4. Candidate V1 Capability Envelope
## 5. Explicitly Optional or Deferred Capabilities
## 6. Change Decomposition Decision Rules
## 7. Required V1 Design Outputs
## 8. Gate Before Implementation
```

## Entry Preconditions

必须写明，在启动首版统一设计之前至少应完成：

- Harness Constitution；
- Living Architecture；
- OpenSpec/SuperSpec/Agent entrypoint alignment；
- SuperSpec apply 所依赖的 Claude Code / Superpowers 能力核验；
- GitHub `claude_master` 作为审查事实源的确认。

## Questions V1 Design Must Resolve

至少列出：

1. V1 是只覆盖 managed executor，还是同时包含 scheduling reconfiguration？
2. Spring Web / Validation / Actuator / Micrometer 哪些进入 V1 工程底座？
3. 首版是否提供模拟负载与观测接口？
4. 首版配置来源是静态初始化 + REST 变更，还是另有来源？
5. 线程池允许运行期更新哪些参数，哪些明确排除？
6. 调度立即触发、版本失效和重建策略是否纳入 V1？
7. Redis 分布式协调是否明确延后？
8. V1 的验收场景、可重复实验脚本和验证证据是什么？
9. V1 最终应拆成一个 change 还是多个有依赖顺序的 changes？

## Candidate V1 Capability Envelope

允许列出候选项，但必须注明：

```text
Candidate only; no scope is approved until the V1 unified design is reviewed.
```

## Change Decomposition Decision Rules

必须说明：

- 框架阶段不预先锁定 change 拆分；
- V1 统一设计完成后，由 Codex 提出最小可审查 change 集；
- 只有经 ChatGPT/用户审核通过后，Claude Code 才能进入 apply；
- 不得把全部 roadmap 直接塞入一个巨型 change；
- 也不得为了流程形式将高度耦合的 V1 验证切得无法运行。

---

## 8. 本轮文档一致性检查

完成写作后逐项检查：

| 检查项 | 必须结果 |
|---|---|
| `bootstrap-ledger.md` 的权威分支 | `claude_master` |
| `project-harness.md` | 索引页而非旧版正文 |
| Harness 是否声称立即创建 feature change | 否 |
| `docs/architecture/` 是否存在索引与 7 份设计文件 | 是 |
| Architecture 是否将未来目标误写成当前实现 | 否 |
| 是否明确“V1 后续统一规划” | 是 |
| 是否创建 OpenSpec change | 否 |
| 是否修改源码或依赖 | 否 |

---

## 9. 验证与 Scope Check

执行：

```powershell
git diff --name-only
git diff --stat
.\mvnw.cmd test
git status --short --branch
```

如 OpenSpec 当前已正常可运行，可额外执行配置未被破坏的只读校验：

```powershell
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
```

本轮未修改 `openspec/**`，若该校验失败，不要顺手修改工具配置；在最终响应中记录为后续 Alignment 阶段输入。

## 允许变更范围

```text
docs/bootstrap/bootstrap-ledger.md
docs/harness/**
docs/architecture/**
```

## 必须无变化范围

```text
pom.xml
src/main/**
src/test/**
openspec/**
AGENTS.md
CLAUDE.md
.codex/**
.claude/**
README.md
```

若出现禁止范围修改，撤销本轮造成的越界修改后重新验证；无法安全撤销则返回：

```text
BLOCKED_SCOPE_VIOLATION
```

---

## 10. Commit 与 Push

验证通过后执行：

```powershell
git add docs/bootstrap/bootstrap-ledger.md docs/harness docs/architecture
git commit -m "docs: establish living architecture baseline for benchmark framework"
git push origin claude_master
```

禁止 force push，禁止将无关文件纳入提交。

---

## 11. 完成判定

本轮仅在满足以下所有条件时返回 `COMPLETED`：

- 以 `claude_master` 最新状态为工作起点；
- Phase 01 遗留的 ledger 和 harness index 已闭环；
- Architecture 索引与七份架构文档已创建；
- 文档明确先完成框架、后统一规划 V1；
- 未创建任何 OpenSpec change；
- 未修改源码、依赖或 Agent/OpenSpec 工具入口；
- Maven 测试通过；
- commit 与 push 成功。

---

## 12. 最终返回格式

完成后只返回摘要，不粘贴文档全文：

```text
STATUS: COMPLETED | BLOCKED | BLOCKED_PUSH | BLOCKED_SCOPE_VIOLATION | BLOCKED_BASELINE_TEST_FAILURE
PHASE: 02-living-architecture-baseline-and-v1-planning-framework
REPOSITORY: DynamicThreadPollerManager
BRANCH: claude_master
START_HEAD:
END_HEAD:
PUSHED: YES | NO

PHASE_01_REMEDIATION:
- bootstrap-ledger branch corrected: YES | NO
- project-harness converted to index: YES | NO
- immediate feature-change wording removed: YES | NO

ARCHITECTURE_FILES_CREATED:
- ...

VALIDATION:
- git diff scope check: PASS | FAIL
- .\mvnw.cmd test: PASS | FAIL
- OpenSpec read-only validation: PASS | FAIL | NOT_RUN

SCOPE_CHECK:
- pom.xml changed: NO
- src/main changed: NO
- src/test changed: NO
- openspec changed: NO
- AGENTS.md / CLAUDE.md changed: NO
- OpenSpec change created: NO
- feature implementation added: NO

NEXT_PHASE:
- Align AGENTS.md, CLAUDE.md, openspec/config.yaml and README navigation with
  Harness + Living Architecture, then prepare V1 unified design planning.
```
