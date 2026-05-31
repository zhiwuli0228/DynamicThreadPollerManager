# DynamicThreadPollerManager Phase 05 执行任务书
## Autonomous Delivery Policy Alignment 与 V1 Unified Design Planning

> 执行端：Codex  
> 总体设计与远端审查端：ChatGPT  
> 后续实现端：Claude Code  
> 目标仓库：`https://github.com/zhiwuli0228/DynamicThreadPollerManager`  
> 目标分支：`claude_master`  
> GitHub 操作：优先使用 `gh` CLI 进行远端核验、分支与提交确认  
> 本轮任务类型：治理策略修订 + V1 总体设计，不实施 Java 代码  
> 最新用户授权原则：项目目标是由 AI 完成设计、实现、验证、提交与推送；不采用逐阶段等待人工确认的自动化限制。

---

## 1. 当前远端基线

本任务书创建时，远端 `claude_master` 已完成框架基线冻结，最新已确认提交为：

```text
91c35cd docs: freeze framework baseline after claude runtime verification
```

已确认的框架事实：

- Harness Constitution 已建立；
- Living Architecture 已建立；
- Delivery Framework 已建立；
- OpenSpec / SuperSpec 已接入；
- SuperSpec v4 `apply` 依赖的 Superpowers skills 已由 Claude Code runtime 验证存在；
- Framework Completion Gate 已记录：
  - `FRAMEWORK_BASELINE_ESTABLISHED`；
- 当前未实施任何动态线程池业务能力；
- 当前未创建任何 capability change。

开始执行前必须重新通过 `gh` / `git` 获取真实远端 HEAD；若远端已有更新，以真实内容为准并检查是否与本任务冲突。

---

## 2. 新的治理决策：Autonomous AI Delivery

现有文档建立时采用了较强的人工作业门禁，例如：

- 用户批准下一阶段；
- ChatGPT 审核每个 design artifact 后，Claude Code 才能实施；
- 每个 change 实施前需要再次显式授权；
- 逐 change 停止等待人工确认。

用户现在明确要求：

```text
不需要任何自动化上的限制，目标是完全交给 AI 实现。
```

本轮必须将该要求准确落实为：

> **在明确的版本目标、架构边界、验收标准和禁止范围内，AI 可以连续执行设计、change 创建、实现、验证、提交、推送和必要的 GitHub 流程，不因“等待人工逐阶段批准”而暂停。**

该原则不等于取消工程治理。以下约束仍然必须保留：

- 不越过已确定的 V1 范围；
- 不绕开 Harness / Architecture 边界；
- 不静默扩大中间件、前端、认证、多节点等技术范围；
- 不跳过测试与验证证据；
- 不伪造通过状态；
- 仅遇到真实外部阻断、不可安全处理的冲突或超出 V1 授权的架构变更时停止。

---

## 3. 本轮目标

本轮由 Codex 一次性完成两个交付组：

### A. Autonomous Delivery Policy Alignment

将现有 Framework 中以人工审批为核心的流程，更新为 AI 自驱式交付模式。

### B. V1 Unified Design Planning

在完整框架基线之上，形成首版的统一设计方案，包括：

- V1 目标；
- V1 功能范围与非范围；
- 工程底座选型；
- 动态线程池与动态调度纳入范围的决策；
- 观测与实验范围；
- 测试与验收方案；
- V1 change decomposition；
- 供 Claude Code 后续一次性自主执行的 implementation mission 输入。

本轮只生成治理与设计资产，不执行代码实现。

---

## 4. 允许修改或创建的文件

### 4.1 允许更新的既有文件

```text
README.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
docs/bootstrap/bootstrap-ledger.md
docs/harness/project-harness.md
docs/harness/04-ai-delivery-workflow.md
docs/harness/05-change-classification-and-gates.md
docs/delivery/README.md
docs/delivery/01-branch-change-and-review-lifecycle.md
docs/delivery/02-framework-completion-gate.md
docs/architecture/06-v1-unified-design-planning-framework.md
```

### 4.2 允许创建的 V1 设计文档

```text
docs/v1/
├─ README.md
├─ 00-v1-product-scope-and-success-criteria.md
├─ 01-v1-technical-architecture-decisions.md
├─ 02-v1-domain-capability-design.md
├─ 03-v1-api-observability-and-experiment-design.md
├─ 04-v1-testing-and-acceptance-strategy.md
├─ 05-v1-change-decomposition-and-autonomous-execution-plan.md
└─ 06-v1-claude-code-autonomous-implementation-mission-draft.md
```

### 4.3 允许创建的 OpenSpec 工作区状态

本轮 **不允许创建** `openspec/changes/**`。

理由：V1 Unified Design 的职责是先确定首版整体范围与 change 拆分；具体 change artifacts 应由后续 Claude Code autonomous implementation mission 在获得完整 V1 授权后，按 V1 拆分方案连续创建、应用和验证，避免 Codex 设计文件与实际实现流程脱节。

---

## 5. 严格禁止范围

本轮不得修改或创建：

```text
pom.xml
src/main/**
src/test/**
docs/architecture/00-system-context-and-quality-attributes.md
docs/architecture/01-logical-architecture-and-package-boundaries.md
docs/architecture/02-managed-executor-domain-model.md
docs/architecture/03-scheduling-reconfiguration-and-recovery-model.md
docs/architecture/04-observability-and-experiment-strategy.md
docs/architecture/05-operational-and-evolution-boundaries.md
openspec/schemas/**
openspec/changes/**
openspec/specs/**
.codex/**
.claude/**
```

本轮不得：

- 实现任何 Java 代码；
- 引入任何依赖；
- 执行 `/opsx:new`、`/opsx:apply`、`/opsx:verify`、`/opsx:archive`；
- 更新生成的 skill/command 资产；
- 创建 feature branch 或 PR；
- 将 V1 设计误写为已实现；
- 将未来完整 roadmap 全部纳入 V1，除非经过明确范围决策且满足 Demo 首版克制原则。

---

## 6. 执行前基线核验

优先通过 `gh` 检查远端：

```powershell
gh auth status
gh repo view zhiwuli0228/DynamicThreadPollerManager --json nameWithOwner,url,defaultBranchRef
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.name + " " + .commit.sha'
gh api repos/zhiwuli0228/DynamicThreadPollerManager/commits/claude_master --jq '.sha + " " + .commit.message'
```

同步本地：

```powershell
git switch claude_master
git pull --ff-only origin claude_master
git status --short --branch
git log --oneline --decorate -10
```

读取全部框架资产：

```text
README.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
openspec/schemas/superspec/schema.yaml
docs/bootstrap/bootstrap-ledger.md
docs/harness/*.md
docs/architecture/*.md
docs/delivery/*.md
```

基线验证：

```powershell
.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
```

若当前工作树不干净且变更无法解释，返回 `BLOCKED_DIRTY_WORKTREE`。  
若基线测试或 OpenSpec 验证失败，不修改源码/schema，返回对应阻断。

---

# Part A：Autonomous Delivery Policy Alignment

## 7. 更新 `docs/harness/04-ai-delivery-workflow.md`

## 7.1 必须修正的核心变化

当前工作流中类似以下含义的规则必须被替换：

```text
User approves the next phase.
ChatGPT reviews design and authorizes each next change.
Claude Code implements only after per-change manual approval.
```

改为：

```text
The user authorizes an AI-executed mission by approving its bounded objective,
scope, exclusions and acceptance rules.

Within that mission boundary:
- Codex may complete required design and planning artifacts.
- Claude Code may complete implementation, tests, verification, commits,
  pushes and approved GitHub operations.
- AI execution does not pause solely for human phase-by-phase approval.
- ChatGPT remote review is an oversight and design-improvement mechanism, not
  a mandatory blocking step between already-authorized substeps.
```

## 7.2 必须明确的角色

| Role | Updated Responsibility |
|---|---|
| User | 定义目标和最终方向；不承担逐阶段人工搬运、检查或授权工作 |
| ChatGPT | 总体架构设计、任务授权边界制定、可选/事后远端审查、异常路线纠偏 |
| Codex | 在授权 mission 内完成治理、V1 设计、change 规划与必要设计工件 |
| Claude Code | 在授权 V1 implementation mission 内自主完成 change 创建/实现/测试/验证/提交/推送/必要 PR 流程 |

## 7.3 必须保留的真实阻断条件

AI 仅在以下情况停止并返回 `BLOCKED`：

- 缺少账号登录、授权或 push/PR 权限，无法非交互完成；
- 远端冲突无法以安全 fast-forward / 常规合并处理；
- 必需工具或插件实际不可用且不在任务授权的可安装范围内；
- 发现需求与已授权 V1 边界矛盾，继续实施会越权；
- 验证失败且在授权范围内无法可靠修复；
- 会导致破坏性数据操作、密钥泄露或不可逆外部风险的动作未被授权。

不要把以下情况作为阻断：

- 进入下一个已授权 change；
- 创建已在 V1 计划中定义的分支或工件；
- 运行测试、修复测试、提交或 push；
- 使用 `gh` 创建/更新 PR（当 mission 已允许时）；
- 为完成已授权 V1 所需的局部代码调整或文档同步。

---

## 8. 更新 `docs/harness/05-change-classification-and-gates.md`

将“人工审批 Gate”重写为“自主执行 Boundary + Evidence Gate”。

## 8.1 Capability Change 新流程

更新为：

```text
V1 autonomous mission defines bounded scope and allowed change set
  -> AI creates required SuperSpec design artifacts
  -> AI verifies artifacts against Harness / Architecture / V1 design
  -> Claude Code applies the change
  -> Claude Code runs tests and SuperSpec verification
  -> AI commits and pushes evidence
  -> AI proceeds to the next authorized change unless BLOCKED
  -> Remote review may occur during or after execution without becoming a
     default pause point
```

## 8.2 Gate 的新含义

保留 gate，但重定义为机器可执行检查：

| Gate | Required Evidence | Stops Automation When |
|---|---|---|
| Scope Gate | change 与 V1 allowed scope 对齐 | 需要越出 V1 或引入明确排除能力 |
| Architecture Gate | 依赖/包边界与 Architecture 对齐 | 需改变长期架构且 V1 未授权 |
| Test Gate | 测试、校验与失败证据 | 校验无法在授权范围内修复 |
| Traceability Gate | commits、change receipts、远端状态 | 无法记录/推送证据 |
| External Risk Gate | 权限与破坏性操作边界 | 需要真实外部授权 |

不得再写“ChatGPT/用户逐 change approval 才能继续实施”。

---

## 9. 更新 `docs/delivery/01-branch-change-and-review-lifecycle.md`

## 9.1 Framework Work 状态

保留 `claude_master` 作为框架资产和最终验收基线。

## 9.2 Future V1 Capability Work 改为自主交付模型

必须写入：

```text
V1 autonomous execution mission issued
  -> AI selects or creates the implementation branch/worktree strategy defined
     by the V1 plan
  -> Codex/Claude Code produce required SuperSpec artifacts and code in the
     authorized sequence
  -> tests / verify / receipts / finalize run automatically
  -> git commits and pushes are performed automatically
  -> gh PR and merge operations may be performed automatically when the V1
     execution plan selects a PR-based closeout path
  -> accepted outcome is integrated toward claude_master
  -> automation pauses only on BLOCKED conditions
```

V1 设计必须决定后续采用：

- `claude_master` 直接集成，或
- feature branches + PR merge，或
- SuperSpec worktree + finalize 的具体组合。

对于该 Demo，应优先选取**AI 自动化成本最低且仍保持可追溯**的方案，不引入仅服务于人工门禁的流程。

---

## 10. 更新 `docs/delivery/02-framework-completion-gate.md`

保留：

```text
FRAMEWORK_BASELINE_ESTABLISHED
```

增加：

```md
## Autonomous Delivery Policy Amendment

- Status: aligned in Phase 05.
- The completed framework does not require phase-by-phase human approval for
  future V1 execution.
- A bounded V1 autonomous execution mission may authorize AI to create required
  changes, implement them, verify, commit, push and continue through the
  approved V1 sequence.
- Oversight reviews remain available but are not default blocking gates.
- Automation must stop only on documented BLOCKED conditions.
```

将 Gate D 从“必须等待 V1 task issuance/审查”修订为：

```md
## Gate D - V1 Autonomous Entry Decision

- [x] No product capability was implemented before V1 design.
- [x] Framework baseline is sufficient for V1 unified design.
- [ ] A V1 autonomous execution mission has been produced from an explicit V1
      design and acceptance boundary.
```

本轮完成 V1 design 与 mission draft 后，可勾选第三项为：

```md
- [x] A V1 autonomous execution mission draft has been produced and is ready
      for execution authorization.
```

注意：本轮不执行 implementation mission。

---

## 11. 更新 `AGENTS.md`

Codex 入口改为：

- 当前已进入 `V1 unified design planning`；
- 本任务书即为 Codex 开展 V1 统一设计的授权；
- 在未来被赋予 autonomous execution mission 时，Codex 不需要因阶段过渡等待人工确认；
- Codex 本轮仍不得实现应用代码，因为本 mission 仅授权设计；
- Codex 必须输出可由 Claude Code 连续执行的 V1 implementation mission draft。

删除或修订以下人工门禁语义：

```text
No capability change may be created unless explicitly authorized by a later task document.
For V1 unified design work, only after explicitly authorized.
```

改为更准确的边界：

```text
No capability implementation may occur outside an active authorized mission.
The current Phase 05 mission authorizes V1 unified design documentation only;
it does not authorize code implementation or OpenSpec change creation.
A future V1 autonomous implementation mission may authorize continuous
change creation and implementation without per-change manual pauses.
```

---

## 12. 更新 `CLAUDE.md`

当前仍然不授权 Claude Code 实现 V1；但需改写未来机制。

必须明确：

```text
- Claude Code may implement continuously when a V1 autonomous implementation
  mission is supplied.
- Under such a mission, it may create/apply/verify/finalize the authorized
  SuperSpec changes, write tests, commit, push and perform selected gh actions
  without waiting for phase-by-phase human approval.
- It must stop only for documented BLOCKED conditions or scope expansion beyond
  the active mission.
- This Phase 05 task does not itself authorize implementation.
```

删除或修订“必须逐 change 额外人工批准/单独 implementation task 才能继续”的表述。

---

## 13. 更新 `openspec/config.yaml`

## 13.1 `context` 修改

保留既有 schema、架构、工程边界，调整 delivery roles 与状态描述：

```text
Current state: Framework baseline is established; V1 unified design is being
planned under an autonomous AI delivery model; business implementation has not
started.

Delivery model: ChatGPT sets mission boundaries and may audit remote results;
Codex produces governance and design artifacts; Claude Code may continuously
implement, verify, commit and push all changes authorized by a future V1
autonomous implementation mission without per-change human pause.

Automation boundary: AI execution stops only for external authorization
barriers, unsafe conflicts, unresolvable validation failures, or scope changes
outside the active mission.
```

## 13.2 `rules` 修改

保留 scope / architecture / test / traceability 规则；将以下人工门禁语义改为自主门禁：

- `proposal` / `design` / `tasks` / `plan` 必须声明是否处于 autonomous mission，并引用 V1 scope。
- `apply` 必须允许在 active V1 mission 范围内连续实现批准 tasks；不要求逐 change 人工暂停。
- `verify` 必须记录实际验证、范围核查和未授权能力检查。
- `finalize` 必须记录分支、提交、push/PR/merge 状态和是否继续下一个 authorized change。

修改后执行 OpenSpec 验证，禁止修改 schema 来迎合 config。

---

## 14. 更新 `README.md` 与 `docs/bootstrap/bootstrap-ledger.md`

## 14.1 README

当前状态更新为：

```md
- Current stage: framework baseline established; V1 unified design planning in progress.
- Delivery mode: autonomous AI execution within an approved V1 mission boundary.
- Implemented business capabilities: none.
```

说明：

- 完整框架已完成；
- V1 正在设计；
- 后续实现将由 AI 自主完成并留下验证/提交证据；
- 当前尚无实现能力。

## 14.2 Bootstrap Ledger

追加：

```md
## Phase 05 - Autonomous Delivery Policy and V1 Unified Design

- Trigger: user explicitly requested removal of phase-by-phase automation
  restrictions and set the goal of AI-completed implementation.
- Decision:
  - Human review is not a default blocking gate between authorized steps.
  - AI may continuously design, implement, verify, commit and push within a
    bounded V1 mission.
  - Architecture, scope, evidence and external-risk boundaries remain enforced.
- Framework baseline commit reviewed: `91c35cd...`.
- V1 design output: `docs/v1/`.
- No business implementation or OpenSpec capability change was created in
  this design-only phase.
```

---

# Part B：V1 Unified Design Planning

## 15. 创建 `docs/v1/README.md`

必须作为 V1 文档索引，明确：

- V1 是第一个可运行实验版本的设计，不是全部路线图；
- V1 设计由本 Phase 05 授权创建；
- 本阶段不实施代码；
- 后续 Claude Code 将接收一份 autonomous implementation mission，连续执行经本设计确定的 change set。

列出所有 `docs/v1/*.md` 文档及阅读顺序。

---

## 16. 创建 `00-v1-product-scope-and-success-criteria.md`

## 16.1 目标

明确 V1 到底验证什么，不将整个长期路线图一次性塞入首版。

## 16.2 必须作出明确设计决策

Codex 必须基于现有 Harness 与 Architecture，选择并说明 V1 是否包含以下能力：

| Candidate Capability | Must Decide: IN / OUT / OPTIONAL | Required Reasoning |
|---|---|---|
| Spring Boot Web/API foundation | IN/OUT | 是否需要 REST 驱动实验 |
| Bean Validation | IN/OUT | 配置修改契约是否需要 |
| Actuator / Micrometer | IN/OUT | V1 可观测验证是否需要 |
| In-memory managed executor registry | IN/OUT | 是否为 V1 核心 |
| Runtime config update | IN/OUT | 支持哪些参数 |
| Controlled workload scenarios | IN/OUT | 如何验证调整效果 |
| Dynamic scheduled task reconfiguration | IN/OUT | 是否纳入 V1 首版 |
| Stall detection/recovery | IN/OUT | 是否延后 |
| Redis/distributed coordination | 必须默认 OUT，除非有非常强理由 | Demo 首版应克制 |
| Virtual threads mode | 默认 OUT | 与 V1 目标关系 |

### 设计倾向但非强制结论

V1 应优先形成**能够演示动态线程池运行期调整效果的闭环**，通常包括：

- Web/API + Validation；
- Actuator/Micrometer 或等价可验证观测方式；
- In-memory managed executor registry；
- 核心线程池参数运行期更新；
- 模拟工作负载；
- 测试与实验说明。

动态 scheduled task 是否同时进入 V1，由 Codex基于复杂度、演示价值和 change 粒度作正式决定；不得含糊保留为“后续再说”而不作范围判定。

## 16.3 Success Criteria

必须形成可验证的 V1 成功标准，包括：

- 可运行；
- 可触发受控 workload；
- 可查询运行状态/观测结果；
- 可修改纳入范围的 executor 参数；
- 非法配置被拒绝；
- 关键行为有自动化测试；
- AI autonomous mission 可从零实现并验证；
- 明确哪些路线图能力不在 V1 中。

---

## 17. 创建 `01-v1-technical-architecture-decisions.md`

必须决定并记录：

- Spring Boot starters / dependencies 候选变更；
- package map 在 V1 中实际采用的子集；
- REST 与异常响应策略；
- 配置文件策略；
- metrics/observation 方案；
- 是否新增 ADR；若无需新增，给出理由；
- 为什么这些决策不突破既有长期 Architecture。

要求用表格列出：

| Decision | Selected Option | Alternatives Rejected | Rationale | Impact on Implementation |
|---|---|---|---|---|

不得修改 `pom.xml` 或源码；这里只做设计。

---

## 18. 创建 `02-v1-domain-capability-design.md`

针对 V1 IN scope 能力，定义：

- 核心 domain objects；
- commands / queries；
- configuration update transaction/order semantics；
- registry lifecycle；
- workload interaction；
- scheduling capability（仅当选择纳入 V1）；
- errors and validation；
- package/class candidates；
- concurrency invariants；
- Mermaid class/sequence/state 图。

必须将每个设计点映射至可验证行为。

---

## 19. 创建 `03-v1-api-observability-and-experiment-design.md`

必须定义：

- V1 REST endpoints 候选与 request/response contracts；
- API 层与 domain 层的分离边界；
- observability 的最小闭环；
- workload endpoints 或实验触发方式；
- 演示步骤；
- 指标/状态如何证明线程池配置变更效果；
- 若动态调度不在 V1，必须明确 API 不包含相关接口；
- 若选择 Actuator/Micrometer，明确暴露边界与 Demo 配置策略。

不得实施接口。

---

## 20. 创建 `04-v1-testing-and-acceptance-strategy.md`

必须包含：

- unit / application / API integration / context startup / deterministic concurrency tests 的测试范围；
- V1 每一项 success criterion 对应的自动验证；
- 禁止只依赖手工观察；
- 禁止依赖长时间 sleep 验证并发正确性；
- Maven 与 OpenSpec / SuperSpec 验证命令；
- 实现过程中的 auto-fix policy：在 V1 范围内，Claude Code 可自行修复编译、测试、设计一致性问题并重复验证，无需等待人工批准；
- 外部阻断与范围升级的停止条件。

---

## 21. 创建 `05-v1-change-decomposition-and-autonomous-execution-plan.md`

## 21.1 目标

将 V1 统一设计切分为可由 Claude Code 自动顺序执行的 SuperSpec change set。

## 21.2 必须输出

```md
## V1 Authorized Change Set

| Order | Change Name | Purpose | Depends On | Implementation May Proceed Automatically After Verification |
|---|---|---|---|---|
| ... | ... | ... | ... | YES |
```

change 数量应由实际 V1 设计决定，不预设固定数量。约束：

- 不把所有长期 roadmap 全部装进 V1；
- 不为了形式把高度耦合的最小可运行版本拆成过多 change；
- 每个 change 必须能测试与验收；
- Claude Code 可在每个 change 验证通过并 push 证据后，自动进入下一项 authorized change；
- 如果某 change 失败，可在其范围内自动修复并重试；
- 若需越出 V1 范围，才返回 BLOCKED。

## 21.3 Branch / Commit / Closeout 决策

为 V1 选择一个明确、低人工成本的执行策略。

可选之一：

### Option A：单一 V1 implementation branch + 顺序 changes + 最后自动合并

```text
claude_master
  -> ai/v1-implementation branch
  -> Claude sequentially creates/applies/verifies/finalizes V1 changes
  -> push evidence commits continuously
  -> final validation
  -> gh PR or automatic merge back to claude_master
```

### Option B：直接在 `claude_master` 自主实施

仅在你论证其对本 Demo 的可追溯性和回滚能力足够时选择。

Codex 必须选定一种并给出理由。优先考虑：

- AI 全流程低阻断；
- 能够审计；
- 失败可回退；
- 不引入无意义人工审核等待。

---

## 22. 创建 `06-v1-claude-code-autonomous-implementation-mission-draft.md`

## 22.1 目的

该文件是下一步交付给 Claude Code 的一份单一执行任务草案。它必须足够完整，使 Claude Code 在收到后：

- 自动建立选定分支/工作树；
- 按 V1 authorized change set 逐一创建 SuperSpec 工件；
- 自动实现、测试、验证、finalize；
- 自动提交、push，并按选定策略使用 `gh` 完成交付闭环；
- 不在每个 change 后暂停等待人工确认；
- 仅在 BLOCKED 条件发生时停止。

## 22.2 必须包含

- Mission objective；
- Repository / baseline branch / selected execution branch strategy；
- V1 scope and explicit exclusions；
- Mandatory reading list；
- Authorized change set；
- Autonomous execution loop；
- Allowed file/dependency/technology changes derived from V1 design；
- Testing and verification commands；
- Commit and push rules；
- `gh` usage rules；
- auto-fix and retry policy；
- BLOCKED conditions；
- final report format。

## 22.3 重要限制

该文档仅为 draft，本阶段 Codex 创建并提交它，但**不执行它**。  
ChatGPT 后续远端审阅 V1 设计的目的，是发现设计缺陷并完善 mission，而不是重新建立逐 change 人工批准流程。

---

## 23. OpenSpec / SuperSpec 与 V1 的关系

本阶段不得创建 `openspec/changes/**`。

但 V1 设计必须说明：

- 后续 Claude Code implementation mission 是否负责创建每个 change 的完整 SuperSpec 工件；
- 哪些 V1 文档是 change 创建的输入；
- 每个 change 的 proposal/design/specs/tasks/plan 应如何引用 `docs/v1/`；
- AI 如何通过 `openspec validate` 与 `verify` 自行判断能否进入下一 change；
- `finalize` 与分支/PR/merge 策略如何衔接。

---

## 24. Scope Check 与验证

完成全部文档和配置修改后执行：

```powershell
git diff --name-only
git diff --stat
.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
git status --short --branch
```

## 24.1 允许变化范围

```text
README.md
AGENTS.md
CLAUDE.md
openspec/config.yaml
docs/bootstrap/bootstrap-ledger.md
docs/harness/project-harness.md
docs/harness/04-ai-delivery-workflow.md
docs/harness/05-change-classification-and-gates.md
docs/delivery/README.md
docs/delivery/01-branch-change-and-review-lifecycle.md
docs/delivery/02-framework-completion-gate.md
docs/architecture/06-v1-unified-design-planning-framework.md
docs/v1/**
```

## 24.2 必须没有变化范围

```text
pom.xml
src/main/**
src/test/**
docs/architecture/00-system-context-and-quality-attributes.md
docs/architecture/01-logical-architecture-and-package-boundaries.md
docs/architecture/02-managed-executor-domain-model.md
docs/architecture/03-scheduling-reconfiguration-and-recovery-model.md
docs/architecture/04-observability-and-experiment-strategy.md
docs/architecture/05-operational-and-evolution-boundaries.md
openspec/schemas/**
openspec/changes/**
openspec/specs/**
.codex/**
.claude/**
```

如需修改禁止范围才能完成 V1 设计，返回 `BLOCKED_DESIGN_SCOPE_EXPANSION`，不得自行越界。

---

## 25. Commit、Push 与远端确认

验证通过后：

```powershell
git add README.md AGENTS.md CLAUDE.md openspec/config.yaml docs/bootstrap/bootstrap-ledger.md docs/harness docs/delivery docs/architecture/06-v1-unified-design-planning-framework.md docs/v1
git commit -m "docs: adopt autonomous ai delivery and define v1 unified design"
git push origin claude_master
```

随后使用 `gh` 确认：

```powershell
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master --jq '.commit.sha'
gh api repos/zhiwuli0228/DynamicThreadPollerManager/commits/claude_master --jq '.sha + " " + .commit.message'
```

不得实施 `docs/v1/06-v1-claude-code-autonomous-implementation-mission-draft.md`。

---

## 26. 完成判定

本轮仅在以下全部满足时返回 `COMPLETED`：

- 以 `claude_master` 最新框架基线为起点；
- Autonomous Delivery Policy 已写入 Harness、Delivery、Agent 与 OpenSpec context；
- 流程不再包含逐阶段人工批准作为默认阻断；
- 保留 Scope / Architecture / Test / Traceability / External Risk 机器可验证边界；
- V1 scope 已明确决定 IN / OUT，而非继续留白；
- V1 change decomposition 已形成；
- Claude Code autonomous implementation mission draft 已形成；
- 未创建 OpenSpec change；
- 未修改源码、依赖、schema 或生成资产；
- Maven / OpenSpec 验证通过；
- commit 和 push 成功。

---

## 27. 最终返回格式

```text
STATUS: COMPLETED | BLOCKED_DIRTY_WORKTREE | BLOCKED_BASELINE_TEST_FAILURE | BLOCKED_OPENSPEC_VALIDATION | BLOCKED_DESIGN_SCOPE_EXPANSION | BLOCKED_PUSH
PHASE: 05-autonomous-delivery-policy-and-v1-unified-design
EXECUTOR: Codex
REPOSITORY: DynamicThreadPollerManager
BRANCH: claude_master
START_HEAD:
END_HEAD:
PUSHED: YES | NO
GH_REMOTE_CONFIRMED: YES | NO

AUTONOMOUS_POLICY_ALIGNMENT:
- phase-by-phase human approval removed as default gate: YES | NO
- AI continuous execution within bounded mission documented: YES | NO
- BLOCKED conditions preserved: YES | NO
- evidence/test/scope gates preserved: YES | NO

V1_DESIGN:
- V1 scope decided: YES | NO
- In-scope capabilities:
  - ...
- Explicitly excluded capabilities:
  - ...
- Selected branch/closeout strategy:
- Authorized change set:
  - ...

FILES_CREATED:
- docs/v1/...

FILES_UPDATED:
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
- openspec/changes created: NO
- openspec/schemas changed: NO
- .codex/.claude changed: NO
- business implementation added: NO

NEXT_ACTION:
- Deliver `docs/v1/06-v1-claude-code-autonomous-implementation-mission-draft.md`
  to ChatGPT for remote quality review and final mission issuance, or execute
  it directly only if the user explicitly chooses to skip pre-execution review.
```

---

## 28. 停止点

本轮 Codex 只完成自治策略修订与 V1 设计，不实施业务代码。

用户可选择两种后续模式：

```text
模式 A：将 Phase 05 推送结果交由 ChatGPT 做一次 V1 总体设计审查，
        然后下发最终 Claude Code autonomous implementation mission。

模式 B：用户明确接受 Codex 生成的 V1 mission draft 后，直接交给
        Claude Code 自主实施，不等待 ChatGPT 审查。
```

两种模式均不要求 Claude Code 在每个 authorized change 之间等待人工批准。
