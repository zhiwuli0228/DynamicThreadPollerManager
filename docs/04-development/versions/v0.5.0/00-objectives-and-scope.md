# v0.5.0 目标与范围

## Header

- Version name: `v0.5.0`
- Status: `DRAFT`
- Current phase: `IR_REQUIREMENT_DRAFT_AUTHORIZED`
- Requirement theme: executor adapter and queue resizing design readiness
- Current conclusion: requirement draft is authorized only

## 1. 背景

当前项目已经具备：

- 实验基础模型和生命周期；
- metrics snapshot 与 evidence recording；
- deterministic scenario runner 与 fixed baseline executor；
- adaptive policy 与 control gate；
- offline replay、decision evidence、summary aggregation、threshold sensitivity 和 mutation readiness assessment。

`v0.4.0` 已经解决“是否有可审计 replay evidence 和 readiness gate”的问题。下一阶段如果继续推进，需要回答 executor adapter 和 queue resizing 设计是否具备足够输入，以及运行时突变能力应被哪些保护条件限制。

## 2. 目标

`v0.5.0` 需求阶段聚焦回答：

- `v0.4.0` 的 replay/readiness 输出是否足以支撑 executor adapter 设计；
- executor adapter 需要暴露哪些最小控制能力；
- queue resizing 是否应作为同一版本范围，还是继续延期；
- runtime adjustment 的失败语义、回滚语义和边界保护应如何定义；
- 哪些 evidence 或测试结果会阻止进入 executor mutation 实现。

## 3. 范围内

- 定义 executor adapter 的需求边界。
- 定义 queue resizing 的需求判断和延期条件。
- 定义 runtime adjustment safety gate。
- 定义 adjustment evidence 和 audit trail 需求。
- 定义与现有 policy decision、control gate、offline readiness 的关系。
- 定义进入 OpenSpec change 前的证据门槛。

## 4. 范围外

- 不实现 executor mutation。
- 不实现 queue capacity resizing。
- 不修改 scenario runner 行为。
- 不引入 scheduler、cooldown runtime state 或 closed-loop controller。
- 不引入持久化数据库。
- 不新增 REST API、UI 或外部依赖。
- 不声明 throughput 改善，除非后续运行时实验实际证明。

## 5. 成功标准草案

- 能明确说明 executor adapter 的最小接口能力和禁止能力。
- 能明确说明 queue resizing 是否进入当前版本设计，或者为什么延期。
- 能明确说明 runtime adjustment 的安全门限、失败语义和 audit evidence。
- 能把 `v0.4.0` readiness output 映射为 `v0.5.0` 的设计输入或阻塞项。
- 能明确阻止没有 evidence 的 executor mutation 实现。

## 6. 当前阶段出口

进入 IR review 前应满足：

1. `10-ir.md` 完成需求条目、验收草案、风险和延期项。
2. 明确当前只授权需求草案。
3. 不创建 `20-sr.md`。
4. 不创建 OpenSpec change。
5. 不修改 Java 源码或测试。
