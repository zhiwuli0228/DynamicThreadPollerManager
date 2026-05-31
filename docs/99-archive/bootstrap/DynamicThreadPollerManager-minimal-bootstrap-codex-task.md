# DynamicThreadPollerManager：最小初始化执行任务书（交付 Codex）

> 任务类型：一次性项目初始化落地  
> 执行端：Codex  
> 后续实现端：Claude Code  
> 审核端：ChatGPT  
> 仓库：`https://github.com/zhiwuli0228/DynamicThreadPollerManager`  
> 目标分支：`main`

---

## 1. 已知项目基线

本任务书下发时，远端仓库处于 Spring Boot 初始骨架状态：

- 公开仓库，默认分支为 `main`。
- 当前仅有初始提交。
- Maven 项目。
- `pom.xml` 使用 Spring Boot `4.0.6`。
- `java.version` 为 `21`。
- 当前依赖包含基础 starter、Lombok 与测试/REST Docs/Testcontainers 依赖。
- 当前尚未建立 OpenSpec、SuperSpec、Harness、Codex/Claude Code 项目入口。
- 当前尚未开始动态线程池业务实现。

在实际执行前，必须重新读取远端/本地最新状态；若真实状态与以上描述不一致，以仓库实际状态为准，并在经验账本中注明差异。

---

## 2. 本轮目标

在**不实现任何动态线程池业务代码**的前提下，一次性完成下列初始化工作：

1. 为项目接入 OpenSpec。
2. 接入 `danielhanold/superspec` schema，并设置为默认 schema。
3. 为 Codex 与 Claude Code 建立可读取的项目职责与范围约束。
4. 建立一份足够简洁的 Harness 文档，作为项目长期治理基线。
5. 建立初始化经验账本，为未来脚手架化提炼可自动化步骤与动态探测事项。
6. 验证初始化结果后提交并推送到 GitHub `main` 分支。

本轮完成后的状态应是：

- 项目具备后续设计 change 和实现 change 的入口；
- 项目仍不包含动态线程池功能代码；
- 下一轮可由 Codex 创建第一个设计 change；
- 审核端可以直接通过 GitHub 获取初始化产物和提交历史。

---

## 3. 权限与禁止事项

### 3.1 允许执行

Codex 被授权直接完成：

- 读取项目文件和 Git 状态。
- 执行必要的环境探测命令。
- 安装或使用 OpenSpec CLI。
- 在项目内初始化 OpenSpec 的 Codex 与 Claude Code 集成。
- 拉取官方 SuperSpec 仓库并复制 schema 文件。
- 创建或修改本任务要求的治理文档与配置文件。
- 执行验证命令。
- `git add`、`git commit`、`git push origin main`。

### 3.2 明确禁止

本轮不得：

- 创建任何动态线程池功能类、接口、Controller、Service 或测试。
- 新增 Spring Web、Actuator、Redis、Kafka、数据库、前端或鉴权相关依赖。
- 创建第一个业务 change，或执行 `/opsx:apply`。
- 为了“以后可能需要”而引入额外治理文档、CI、静态检查、架构测试或发布流程。
- 重构 Spring Boot 初始代码。
- 删除现有项目文件，除非是 OpenSpec 初始化过程产生且确认无效的重复文件。
- 将初始化拆成多个冗长报告或多轮等待确认。

---

## 4. 执行步骤

## Step 1：确认真实基线

在项目根目录执行并记录关键信息：

```powershell
git status
git branch --show-current
git log --oneline -5
git remote -v
Get-Content pom.xml
.\mvnw.cmd test
```

要求：

- 当前操作基于 `main`。
- 开始修改前工作树应可解释；若已有未提交变更，先判断其是否来自本任务之外。
- 不因基线核查单独创建 commit。
- 将重要事实写入后续创建的 `docs/bootstrap/bootstrap-ledger.md`。

---

## Step 2：初始化 OpenSpec 工具链

先动态探测环境，不假定本机已安装或命令参数稳定：

```powershell
node --version
npm --version
openspec --version
```

若 `openspec` 不存在，则执行：

```powershell
npm install -g @fission-ai/openspec@latest
openspec --version
```

执行前确认 Node.js 满足 OpenSpec 当前最低要求；若 Node.js 不满足要求且无法无交互升级，则返回真实阻断，不伪造已完成状态。

接下来：

```powershell
openspec init --help
```

根据**当前安装版本输出的真实工具 ID 和选项**，初始化本仓库，使 Codex 与 Claude Code 都具备 OpenSpec 入口。不要凭记忆写死工具参数。

该项目使用 SuperSpec 完整设计与验证流，因此应启用支持以下命令的 expanded/custom workflow，并更新工具入口：

```text
new / continue / ff / apply / verify / archive
```

具体 CLI 操作方式以当前 OpenSpec CLI 帮助与交互结果为准，完成后执行必要的 `openspec update`。

---

## Step 3：安装并启用 SuperSpec

从官方仓库获取 `superspec` schema：

```text
https://github.com/danielhanold/superspec
```

要求：

1. 将官方仓库中的 `openspec/schemas/superspec` 完整复制到本项目：
   `openspec/schemas/superspec/`。
2. 必须保留 schema 配置、模板和嵌套目录，不得只复制部分文件。
3. 设置 `openspec/config.yaml` 的默认 schema 为：

```yaml
schema: superspec
```

4. 不允许用一条覆盖命令抹掉 OpenSpec 初始化生成的有效上下文或配置；应以合并方式编辑。

---

## Step 4：建立最小 Harness 文档

只创建以下一个长期治理文档：

```text
docs/harness/project-harness.md
```

其内容必须简洁，并包含以下章节：

### 4.1 Project Purpose

说明该项目是 Java 21 + Spring Boot 的探索型 Demo，用于验证：

- 动态受管线程池注册与运行时调整；
- 执行指标与模拟负载；
- 动态调度任务调整；
- 后续可能的多节点唯一执行与恢复策略。

### 4.2 Delivery Boundary

当前阶段明确：

- 先验证单节点内存实现；
- Redis、Kafka、多节点、前端、认证等必须通过后续独立 change 引入；
- Demo 不采用生产系统级别的重型门禁。

### 4.3 Architecture Rules

写入稳定规则：

- `api -> application -> domain`；
- `infrastructure` 实现 domain 所需的具体能力；
- domain 不依赖 Web DTO、Redis/Kafka 客户端或 Spring MVC 实现细节；
- 不得无审批跨 change 扩大技术栈或功能范围。

### 4.4 Engineering Rules

写入稳定规则：

- Java 21；
- Maven；
- JUnit 5 + Mockito，禁止 PowerMock；
- 每个功能 change 必须带测试；
- 不进行无关重构；
- 并发状态变更与线程池配置校验必须可验证。

### 4.5 AI Collaboration Model

明确职责：

- Codex / ChatGPT：需求分析、方案设计、OpenSpec/SuperSpec 工件、范围审查。
- Claude Code：按已批准的 change 实现代码、测试、验证、提交。
- Claude Code 不得绕过已批准工件自行扩展业务范围。

### 4.6 Roadmap

仅写粗粒度路线：

1. Local managed executor registry。
2. Runtime metrics and workload simulation。
3. Dynamic scheduled task reconfiguration。
4. Stalled task detection and recovery。
5. Distributed coordination experiment。

不要拆成额外文档。

---

## Step 5：建立 Agent 入口文件

### 5.1 创建或更新 `AGENTS.md`

该文件面向 Codex，必须做到：

- 要求 Codex 设计前读取：
  - `docs/harness/project-harness.md`
  - `openspec/config.yaml`
  - 当前 active change 工件（如存在）
- 明确 Codex 默认仅负责设计和审查，不负责实现应用代码。
- 明确一个 change 只处理一个实验能力。
- 明确禁止无关重构和未经批准引入 Redis/Kafka/前端/数据库等。

不要在 `AGENTS.md` 中复制完整 Harness 正文。

### 5.2 创建或更新 `CLAUDE.md`

该文件面向 Claude Code，必须做到：

- 要求实现前读取：
  - `docs/harness/project-harness.md`
  - 当前 active change 下的全部已批准工件
- 明确 Claude Code 仅实施已批准 tasks。
- 明确其负责测试、验证、提交。
- 明确不允许自行扩展范围或改变架构边界。

不要在 `CLAUDE.md` 中复制完整 Harness 正文。

---

## Step 6：配置精简的 `openspec/config.yaml`

保留 SuperSpec 所需配置，并设置精简 `context`。内容不得膨胀成长篇设计文档，应表达以下事实：

```yaml
schema: superspec

context: |
  Project: DynamicThreadPollerManager, an exploratory Spring Boot demo for dynamic thread-pool management.
  Baseline: Java 21, Maven, Spring Boot 4.0.6 at project initialization.
  Delivery model: Codex/ChatGPT design approved changes; Claude Code implements and verifies approved tasks.
  Scope boundary: initial capabilities are single-node and in-memory; Redis, Kafka, database, frontend, authentication and distributed execution require explicit later changes.
  Architecture: api -> application -> domain; infrastructure supplies concrete integrations; domain must not depend on transport DTOs or infrastructure clients.
  Engineering: JUnit 5 and Mockito; no PowerMock; each functional change includes tests; no unrelated refactoring.
  Reference: read docs/harness/project-harness.md for durable project rules.
```

注意：

- 若实际 Spring Boot 版本已与基线不同，应使用真实版本。
- 不在本轮自定义大量 artifact rules；SuperSpec schema 自带工件流程，后续发现必要约束时再最小增补。
- 不复制文档长内容到 `context`。

---

## Step 7：创建单一经验账本

创建：

```text
docs/bootstrap/bootstrap-ledger.md
```

只维护一份短文档，不生成分阶段报告。必须包含：

```md
# Bootstrap Ledger

## Baseline Observed

- Repository:
- Branch:
- Starting commit:
- Spring Boot version:
- Java version:
- Build tool:
- Initial test result:

## Toolchain Installed

- Node.js version:
- OpenSpec version:
- OpenSpec tool integrations enabled:
- Workflow/profile enabled:
- SuperSpec source and detected schema/version information:

## Files Added or Changed

- ...

## Validation Results

- Command:
  - Result:

## Experience for Future Scaffold

### Stable template content

- 可直接固化进未来脚手架的文件与约束。

### Dynamic detection required

- 不能写死、必须运行时探测的 CLI 参数、版本、工具 ID 或项目事实。

### Removed complexity

- 本项目刻意省略的重型流程，以及省略原因。
```

保持该文件事实化、短小，不写执行流水账。

---

## Step 8：验证

执行以下验证；若命令因当前 CLI 版本名称变化而不同，应根据 `openspec --help` 使用同等验证命令并记录真实命令。

```powershell
.\mvnw.cmd test
openspec schemas
openspec schema validate
openspec validate
git diff --stat
git status
```

验收条件：

- Maven 初始测试通过。
- OpenSpec 可识别项目配置。
- SuperSpec schema 被识别且校验通过。
- 本轮 diff 仅涉及 OpenSpec/SuperSpec、Harness、Agent 入口与经验账本相关文件。
- `src/main` 与 `src/test` 中不出现动态线程池业务实现。

---

## Step 9：提交并推送

验证通过后：

```powershell
git add .
git commit -m "chore: bootstrap openspec superspec and minimal ai harness"
git push origin main
```

如 push 因登录、权限或远端冲突失败：

- 不执行强制推送；
- 保留本地 commit；
- 在最终响应中说明失败原因、commit SHA 与可复现命令；
- 状态返回 `BLOCKED_PUSH`。

---

## 5. Codex 最终响应格式

本轮不需要生成额外报告文件；真实交付物已经位于仓库中。完成后仅返回以下摘要：

```text
STATUS: COMPLETED | BLOCKED | BLOCKED_PUSH

Repository:
Branch:
Starting HEAD:
Ending HEAD:
Pushed: YES | NO

Created/Updated Files:
- ...

Validation:
- .\mvnw.cmd test: PASS | FAIL
- openspec schemas: PASS | FAIL
- openspec schema validate: PASS | FAIL
- openspec validate: PASS | FAIL

Scope Check:
- Dynamic thread-pool business code added: NO
- Unapproved dependency added: NO
- Unrelated refactoring performed: NO

Bootstrap Ledger:
- docs/bootstrap/bootstrap-ledger.md

Next Recommended Action:
- Ask Codex to design the first SuperSpec change:
  establish-local-managed-executor-registry
```

---

## 6. 下一轮边界：暂不在本轮执行

完成本初始化任务后，下一轮才允许 Codex 创建：

```text
establish-local-managed-executor-registry
```

下一轮仍然只产出设计工件，不执行代码实现；经过审核后，才交给 Claude Code 执行实现。
