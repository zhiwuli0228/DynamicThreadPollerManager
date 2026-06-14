# 管理变更标准

## 目的

本文定义从下一个版本开始必须遵守的需求管理、开发实现、测试验收流程。目标是避免后续 agent 跳过需求闭环、设计闭环、实现评审、测试评审或验收反向核查，导致实现范围漂移、证据不足或状态误报。

本文是 `docs/02-harness/` 下的治理规则，受 `docs/00-project/current-state.md` 约束。若两者冲突，以 `docs/00-project/current-state.md` 的当前授权状态为准。

## 适用范围

适用于所有后续能力版本和 OpenSpec change，包括但不限于：

- 新版本设计；
- 新 OpenSpec capability change；
- Java 源码或测试实现；
- 压测、实验数据获取、policy replay、executor mutation、queue resizing 等能力推进；
- 已实现能力的补救变更。

不适用于纯只读检查、简单文档修正、归档历史查询；但这些任务仍必须遵守当前授权边界。

## 核心原则

- 阶段必须有明确入口和出口，不允许用“已处理”替代“已闭环”。
- 需求、设计、实现、测试、验收证据必须可追踪。
- P0/P1 finding 未关闭前不得进入下一阶段。
- P2 可以作为残余风险保留，但必须记录非阻塞理由和后续触发条件。
- “处置完成”不等于“闭环完成”。
- “验收候选”不等于“accepted/closed”。
- OpenSpec `verify.md` 不等于归档后主 spec 有效，归档后必须重新校验当前仓库状态。
- 弱实现 agent 的输入文档必须足够具体，不能只提供原则性设计。

## 标准阶段

### 1. Version Baseline

目标：确定版本目标、边界、非目标和授权状态。

最低输出：

- `docs/04-development/versions/<version>/00-objectives-and-scope.md`
- `docs/04-development/versions/<version>/decision-log.md`
- `docs/00-project/current-state.md` 中的版本授权状态

出口条件：

- 版本目标和非范围清晰；
- 当前状态文件明确是否允许进入需求分析或 change decomposition；
- 未授权 Java 实现时，不得创建实现任务。

### 2. IR 需求分析

目标：把用户意图和项目问题转成可评审的需求基线。

推荐输出：

- `10-ir.md`
- `11-ir-review.md`
- `12-ir-review-disposition.md`
- `13-ir-closure-verification.md`

IR 必填内容：

- 需求来源；
- 当前问题；
- 范围内和范围外；
- 用户确认项；
- 验收草案；
- 风险和延期项；
- IR 到 capability/change 的初步分解；
- 不允许在 IR 阶段声明实现已完成。

出口条件：

- 独立需求评审完成；
- 所有 P0/P1 findings 已处置并通过闭环验证；
- 残余风险已记录；
- 明确允许进入 SR 功能设计。

### 3. SR 功能设计

目标：把需求转成可实现、可测试、可评审的设计。

推荐输出：

- `20-sr.md`
- `21-sr-review.md`
- `22-sr-review-disposition.md`
- `23-sr-closure-verification.md`

SR 必填内容：

- 模块边界；
- 数据模型；
- 接口、类或组件设计；
- 状态枚举和失败语义；
- 依赖方向和禁止依赖；
- 安全、并发、资源、观测边界；
- 测试映射；
- 非范围再次声明；
- 对弱实现 agent 足够明确的任务切分。

SR 伪代码强制验证规则（v0.9.0 复盘 P1/P2/P4 驱动，v0.12.0 复盘扩展）：

- SR 中任何组件伪代码引用已有类型（类、接口、record、enum）时，**必须先读取该类型的实际源码**，确认构造器签名、方法签名、泛型参数、工厂方法行为与伪代码一致。
- 至少需要验证的引用点：
  - `new` 构造调用 → 确认构造器存在且参数匹配；
  - `implements` / `extends` → 确认接口/父类的方法签名完全匹配；
  - 静态工厂方法（如 `XxxResult.success(...)` / `XxxResult.failed(...)`）→ 确认方法签名、参数顺序、返回值泛型；
  - 方法参数类型（如 `ReadinessSummary`、`ExecutorStateSnapshot`）→ 确认实际类型而非假设类型。
- 如果一个类型有多个重载，伪代码必须明确使用哪个重载，并在注释中标注"已验证 API 签名"。
- SR review 的必查项新增一条：随机抽取伪代码中的 3 个 API 调用点，读取实际源码验证签名匹配。若发现不一致 → P1 finding。

**序列化方向强制检查**（v0.12.0 复盘 P3 驱动）：
- SR 伪代码中出现序列化/反序列化调用时，必须验证方向性正确：
  - `render(Object)` → 对象到字符串（序列化）
  - `parse(String)` → 字符串到对象（反序列化）
  - `toMap()` → 对象到 Map（序列化）
  - `fromMap(Map)` → Map 到对象（反序列化）
- SR review 必查项：对比伪代码中序列化/反序列化方法名与源码实际 API 是否方向一致。若 `fromMap()` 路径调用 `render()` 或 `writeXxx()` 路径调用 `parse()` → P1 finding。

**Record 反序列化 null 兼容性检查**（v0.12.0 复盘 P2 驱动）：
- SR 中定义 record 类型且包含 `fromMap()` 方法时，必须检查 compact constructor 的 `requireNonNull` 是否与反序列化路径兼容。
- 规则：`fromMap()` 无法从持久化表示重建的字段（如需要运行时上下文的 `ScenarioRunOutcome`、`ManagedExecutorConfig`）必须在 compact constructor 中允许 null，或在 `fromMap()` 中提供合法的默认值。
- SR review 必查项：对每个包含 `fromMap()` 的 record 类型，追踪 `fromMap()` 调用链中所有 `requireNonNull` 字段是否能够从反序列化输入填充。若存在不可填充的非空字段 → P1 finding。

**跨类型转换边界值检查**（v0.12.0 复盘 P4 驱动）：
- SR 中定义跨类型转换方法（如 `XxxPreset.toYyyPreset()`）时，必须列出源类型和目标类型的合法值域差异表。
- 对每个值域不重叠的字段，必须在转换方法中给出明确的映射规则（默认值、最近合法值、或抛出异常）。
- SR review 必查项：随机抽取 1 个转换方法，对照源类型和目标类型的构造器/工厂方法参数约束，验证边界值（最小值、最大值、哨兵值如 -1/0）的映射结果。若存在未处理的边界值 → P1 finding。

Change 分解独立验证规则（v0.9.0 复盘 P3 驱动）：

- Change 分解后，必须对每个 change 执行独立可验证性检查：
  - 该 change 是否可以独立运行 `mvn test` 并通过其范围内的测试？
  - 该 change 是否依赖另一个 change 的源码才能编译？
  - 如果 change B 的唯一价值是测试 change A 的组件，change B 不是独立 change，应与 change A 合并。
- 如果两个 change 共享同一测试基础设施且无法独立运行测试，必须在 change decomposition 中明确记录"合并交付"决策及理由。

出口条件：

- 独立功能设计评审完成；
- 所有 P0/P1 findings 已处置并通过闭环验证；
- P2 残余风险有非阻塞理由；
- 明确允许创建或授权 OpenSpec change。

### 4. OpenSpec Change

目标：把已闭环 SR 分解为 OpenSpec/superspec 可执行 change。

最低输出：

- `proposal.md`
- `design.md`
- `specs/<capability>/spec.md`
- `tasks.md`
- `plan.md`

Schema 强制规则：

- 新建 capability change 必须使用 `schema: superspec`。
- 不允许再新建 `schema: spec-driven` 的 capability change。
- 继续任何既有 change 前，必须先读取 `openspec/changes/<name>/.openspec.yaml`，再决定能否使用 `/opsx:continue`、`/opsx:apply`、`/opsx:verify` 或 `finalize`。
- 若历史 change 已经固定为 `schema: spec-driven`，不得声称 `/opsx:continue` 可以生成 `finalize.md`；必须采用一次性兼容收尾，手工补齐真实的 `finalize.md` 并记录原因。
- 若 change 声称基于 superspec 创建，但 `.openspec.yaml` 不是 `schema: superspec`，这是 P1 流程缺陷，必须立即修正或停止交付。

出口条件：

- change 名称、scope、non-scope 与 SR 一致；
- spec scenarios 覆盖 SR 的关键验收语义；
- `docs/00-project/current-state.md` 明确进入 `EXECUTION_AUTHORIZED` 后，才允许实现。

### 5. Implementation 实现

目标：在授权边界内完成代码、测试和实现记录。

推荐输出：

- `30-implementation-record.md`
- OpenSpec `apply.md`

实现记录必填内容：

- 输入基线；
- 实现范围；
- 变更文件；
- 需求/设计追踪表；
- 验证命令和结果；
- 已知偏差；
- 残余风险；
- worktree 状态。

出口条件：

- 实现文件已提交或明确进入提交前检查；
- 测试命令真实运行；
- 未引入未授权依赖或范围扩张；
- 不得直接跳到 archive。

**Handler/包装器引入规则**（v0.12.0 复盘 P1 驱动）：

- 当对现有方法返回值引入包装层（如 `RejectedExecutionHandler` 包装器、InputStream 装饰器）时：
  - **对外 getter 返回原始对象**：`getXxx()` 返回原始未包装对象，而非包装器。
  - **内部委托给包装器**：包装后的对象仅用于框架/库内部（如传递给 TPE 构造器）。
  - **setter 必须同时维护两份引用**：`setXxx(newValue)` 必须同时更新原始引用和重新生成包装器。
- 实现 review 必查项：检查引入包装器的 getter 方法返回值类型是否与"未包装前"一致。对 `assertSame`/`assertInstanceOf` 断言的影响必须在实现记录或 review 中明确说明。
- 调用链下游如果通过 `unwrap()` 获取包装器后再次传递给构造器（如 `ExecutorRebuildStrategy`），必须改用对外 getter 获取原始对象。

**静态工厂方法修改规则**（v0.13.0 复盘 P1 驱动）：

- 修改既有 `public static` 工厂方法的内部行为时：
  - **禁止修改原方法的输出语义**：如果新行为改变了任何字段的输出值（包括从 `absent()`→`present()`、从默认值→实际值），必须使用重载方法而非修改原方法。
  - **原方法签名和行为必须保持不变**：原方法的所有现有调用方（包括测试）必须不经修改即可通过。
  - **重载方法通过新增参数可选地启用新行为**：如 `fromExecutor(ManagedExecutor, Instant)` → 新增 `fromExecutor(ManagedExecutor, Instant, SystemCpuProbe)`。
  - **测试覆盖双向**：原方法测试验证行为不变，重载方法测试验证新行为正确。
- 实现 review 必查项：对每个被修改的 `public static` 方法，运行所有现有调用方的测试（包括其他包的测试），确认零回归。若任何现有测试需要修改才能通过 → P1 finding。

### 6. Implementation Review Gate

目标：独立检查实现是否满足需求和设计。

推荐输出：

- `31-implementation-review.md`
- `32-implementation-review-disposition.md`
- `33-implementation-closure-verification.md`

评审重点：

- 行为缺陷；
- 需求或 SR 偏离；
- 边界破坏；
- 测试缺口；
- 文档状态误报；
- 未提交或 worktree 不干净。

出口条件：

- P0/P1 findings 已关闭；
- P2 残余风险可接受；
- 明确允许进入测试设计和测试验收阶段。

### 7. Test Design / Test Review

目标：证明测试不是事后补充，而是覆盖需求和设计的验收资产。

推荐输出：

- `40-test-design-and-evidence.md`
- `41-test-review.md`
- `42-test-review-disposition.md`
- `43-test-closure-verification.md`

测试设计必填内容：

- 分层测试策略；
- 覆盖矩阵；
- 测试用例 ID；
- 自动化映射；
- 执行命令和结果；
- evidence 路径；
- 残余风险和延期项。

出口条件：

- 覆盖矩阵证明关键 IR/SR/Spec 场景均有测试或明确残余风险；
- 测试评审 findings 已处置并闭环；
- 明确允许进入验收前反向核查。

**并发测试稳定性规则**（v0.12.0 复盘 P5 驱动）：

- 依赖线程调度时序的断言不得使用精确相等（`assertEquals(expected, actual)`），必须使用区间断言或条件等待：
  - **线程状态类**：对 `ThreadPoolExecutor.getActiveCount()` 等近似值 API 的 `assertEquals(0, ...)` → 替换为重试循环（如 `for (int i = 0; i < N && value != expected; i++) { Thread.sleep(M); }`），或替换为 `assertTrue(value >= 0 && value <= upperBound)`。
  - **并发计数类**：对多线程争用下的允许/拒绝计数的 `assertEquals(exactCount, ...)` → 替换为 `assertTrue(actual >= 1 && actual <= maxAllowed)`。
  - **同步门禁类**：对 `synchronized` 方法门禁在极端争用时的精确通过数断言 → 替换为区间断言。
- 新增并发测试 review 必查项：检查 `java.util.concurrent` 相关断言中是否存在 `assertEquals` 依赖线程时序的用法。若存在且无重试/区间保护 → P2 finding。

**优先级/排序测试断言规则**（v0.13.0 复盘 P3 驱动）：

- 涉及优先级、排序、状态机转换的测试断言必须针对**具体期望值**，不得使用候选集合：
  - `assertEquals(expectedState, actualState)` ✓ — 明确断言期望状态
  - `assertTrue(actual == A || actual == B)` ✗ — 无法证明优先级/排序逻辑正确
  - 例外：当且仅当多个输出在语义上等价（如两个不同策略可能产生相同评分时），可使用 `assertTrue(actual >= expected)` 或区间断言
- 测试 review 必查项：对涉及优先级链（如 `PressureState` 枚举序）、排序输出（如 `PolicyRanker.rank()` 返回值）、状态机转换的测试，检查是否存在 `assertTrue(A || B)` 形式。若存在且无语义等价理由 → P2 finding。

### 8. Acceptance Precheck / Archive

目标：归档前从用户确认项和验收语义反向核查到实现和证据。

推荐输出：

- `50-acceptance-precheck.md`
- `51-acceptance-precheck-verification.md`
- OpenSpec `verify.md`
- OpenSpec `finalize.md`
- archive commit

反向核查必须确认：

- 用户确认项已映射到 IR/SR/Spec；
- 每个关键场景有实现文件、测试用例和 evidence；
- residual risk 已记录且不阻塞当前归档；
- main spec 已同步；
- archived change 已移动；
- `docs/00-project/current-state.md` 已反映实际状态；
- `git status --short` 干净。

Archive 后强制同步规则：

- 执行 archive 后，必须立即更新 `docs/00-project/current-state.md`，移除 archived change 的 active 授权、移除 `Authorized OpenSpec change`、退出 `EXECUTION_AUTHORIZED`，并记录 archive 目录和同步后的 main spec 路径。
- 执行 archive 后，必须同步相关版本 README 或状态文件，把版本状态从执行授权改为已交付或已归档后的实际状态。
- archive 后不得只依赖 OpenSpec 文件移动结果；必须运行 `scripts/openspec-archive-guard.ps1 -Mode post-archive -ChangeName <name>`。
- post-archive guard 不通过时，禁止声称“归档完成”或“需求闭环”。
- archive move、main spec、`current-state.md`、版本状态文件、复盘或治理修正必须进入同一个归档收尾提交，除非明确拆分并记录原因。
- 若 `openspec list --json` 已无 active change，但 `current-state.md` 仍声明 active change，这是 P1 流程缺陷，必须先修正状态同步，而不是继续推进新需求。
- `docs/00-project/current-state.md` 中任何会被 verify/guard 读取的授权字段，必须使用脚本可识别的固定格式，例如 `Authorized OpenSpec change: <name>` 或 `Change name: <name>`；不能只用纯描述性短句代替。

出口条件：

- `openspec.cmd validate --all --json` 通过；
- 必要测试命令通过；
- active changes 与 archive 目录状态一致；
- 当前状态文件同步；
- 不得把 precheck 通过误写为用户最终验收，除非用户明确确认；
- 必须进入复盘阶段，不能在 archive 后直接结束交付。

Verify 阶段的自动推进规则：

- `verify.md` 必须只有一个最终结论；历史失败、旧指令或前一轮用户约束只能作为过程记录，不得保留为当前终态。
- `verify.md` 必须写入 `Machine-Actionable Closeout State`，至少包含 `Gate status`、`Worktree status`、`Blocking reason`、`Agent next action`、`User action required before next agent action`、`Archive status`。
- 当 `Gate status` 为 `PASS` 或 `PASS_WITH_WARNINGS` 且 `User action required before next agent action` 为 `no` 时，后续 agent 必须继续执行 `Agent next action`，不得停在“等待用户控制 archive”。
- “user-controlled archive” 只能用于确实需要用户作出验收、权限或发布决策的场景；不能用来替代 `commit`、`/opsx:continue`、`finalize.md` 生成或门禁验证。
- 若工作区在 verify 后仍为 dirty，但 dirty 内容正是当前实现、测试、证据或治理修正，则应标记为 `DIRTY_EXPECTED_BEFORE_COMMIT`，下一步必须是提交或提交前复核，而不是等待用户。
- 若 `pre-finalize` 门禁通过，标准下一步是提交当前实现/证据并运行 `/opsx:continue` 进入 finalize；不得直接 archive，也不得无理由停止。

### 9. Retrospective 复盘

目标：在每个需求、版本或 bounded change 完成后，记录实际执行偏差、根因、流程问题和后续改进，避免同类问题在后续 agent 协作中重复出现。

最低输出：

- `docs/08-retrospectives/<date>-<change-or-version>-retrospective.md`

复盘必填内容：

- 交付对象和完成日期；
- 计划路径与实际路径；
- 发生的问题、遗漏和风险；
- 问题根因；
- 本次已经采纳的修复措施；
- 仍未解决的问题；
- 需要固化到规则、模板、脚本或检查清单的改进；
- 后续 agent 必须遵守的行为。

出口条件：

- 复盘文档已创建并纳入 `docs/08-retrospectives/README.md`；
- 若复盘提出流程改进，相关治理文档、模板或脚本已同步，或明确记录为后续待办；
- 交付结论不能在复盘前标记为完全关闭。

## 严重级别规则

| 级别 | 含义 | 门禁规则 |
| --- | --- | --- |
| P0 | 阻断性错误、数据破坏、越权实现、核心需求反向实现 | 必须修复并闭环，不能进入下一阶段 |
| P1 | 关键需求缺失、设计不可实现、边界破坏、测试无法证明语义 | 必须修复并闭环，不能进入下一阶段 |
| P2 | 次要缺陷、可记录风险、非核心覆盖缺口 | 可修复或记录残余风险，但必须说明后续触发条件 |
| P3 | 文档清晰度、命名、非阻塞改进 | 可记录为后续改进 |

## 必须保留的追踪矩阵

每个能力版本至少维护一张追踪表，字段包括：

| 字段 | 说明 |
| --- | --- |
| 用户确认项或需求 ID | 原始来源或需求编号 |
| IR 条目 | 对应需求分析条目 |
| SR 条目 | 对应功能设计条目 |
| Spec 场景 | OpenSpec scenario 或验收场景 |
| 实现文件 | 相关源码或测试文件 |
| 测试 ID | 自动化或手工测试编号 |
| Evidence | 命令结果、报告或产物路径 |
| 状态 | `planned`、`implemented`、`verified`、`deferred`、`blocked` |
| 残余风险 | 非阻塞理由和后续触发条件 |

## 状态用语规则

- `draft`：草稿，不能作为实现输入。
- `ready for disposition`：评审完成，等待处置，不能进入下一阶段。
- `disposition completed`：处置完成，等待独立闭环验证。
- `closed`：闭环验证通过，无阻塞残余风险。
- `closed with recorded residual risk`：闭环验证通过，有已记录的非阻塞残余风险。
- `blocked`：存在阻塞项，不能进入下一阶段。
- `acceptance candidate`：可提交验收候选，但不代表最终 accepted。
- `accepted/closed`：只能在用户明确验收或项目规则明确允许时使用。

## 与 OpenSpec 的关系

- OpenSpec change 是 SR 闭环后的执行载体，不替代 IR/SR。
- OpenSpec `tasks.md` 是执行清单，不替代需求追踪矩阵。
- OpenSpec `verify.md` 是 pre-archive 验证记录，不替代 archive 后主 spec 校验。
- archive 后必须检查 `openspec/specs/<capability>/spec.md` 和 `docs/00-project/current-state.md`。
- OpenSpec change 完成后仍必须进入 retrospective 复盘，不能以 archive 代替复盘。

## 当前项目的默认推进策略

在没有新授权前，当前项目只允许文档框架维护。后续若推进下一阶段，推荐先设计数据获取/离线 replay 类版本，而不是直接进入 executor mutation。

原因：

- 当前 policy 已能给出非突变决策；
- executor mutation 会引入运行时状态风险；
- 需要先用 baseline pressure 和 policy replay 数据确认阈值、抖动和 gate 行为；
- 只有数据证明策略合理后，才适合设计 executor adapter 和 queue resizing。

## Agent 执行要求

- 开始任何新阶段前，必须读取本文。
- 若任务要求实现但 IR/SR/OpenSpec 授权不完整，必须停止并报告缺失门禁。
- 若另一个 agent 声称完成，必须复核当前仓库状态、测试结果、OpenSpec 状态和 worktree 状态。
- 不得把归档历史当作当前授权。
- 不得自行创建下一个版本或 change，除非 `docs/00-project/current-state.md` 明确授权。
- 每个需求、版本或 bounded change 完成后，必须补充复盘文档并检查是否需要同步治理规则。
