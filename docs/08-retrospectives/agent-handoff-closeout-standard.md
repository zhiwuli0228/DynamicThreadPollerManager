# Agent 交接与收尾标准

## 目的

本标准将跨 agent 协作中的经验教训转化为强制收尾行为。适用于 agent 完成、验证、归档 change，或将 change 交接给其他 agent 的场景。

## 核心规则

不要把前一个 agent 的“已完成”声明，或归档前生成的 `verify.md`，当作最终证明。必须在最终文件系统状态形成之后，重新验证当前仓库状态。

## 必须执行的最终验证

在 archive、spec sync、current-state 更新或交接之后，必须运行并记录：

```powershell
openspec.cmd validate --all --json
.\mvnw.cmd test
git status --short
```

如果任务修改了 OpenSpec schema 相关内容，还必须运行 schema workflow 文档中要求的对应 schema 校验命令。

## Delta Spec 与 Main Spec 区分规则

agent 必须区分：

- `openspec/changes/<change>/specs/**/spec.md`：archive 前使用的 delta spec 格式。
- `openspec/specs/**/spec.md`：archive 后使用的 main spec 格式。

archive 之后，必须从仓库根目录验证 main spec。delta spec 有效，不等于同步后的 main spec 有效。

## Scenario 覆盖规则

对 spec 中的每一个 `#### Scenario:`，验证时都应该识别：

- 实现证据。
- 测试证据。
- 结论：`covered`、`intentionally deferred` 或 `spec needs revision`。

如果 scenario 明确命名了具体字段、状态或行为，测试必须断言这些具体元素，而不能只测试一个更小的实现子集。

## Task 完成规则

`tasks.md` 中的已勾选 checkbox 只能作为执行证据，不能证明 requirement 已覆盖。

archive ready 必须同时满足：

- tasks 已完成，或未完成项被明确豁免。
- 最终仓库状态下 OpenSpec validation 通过。
- 测试通过。
- spec scenarios 已映射到实现和测试。
- `git status --short` 干净，或所有剩余文件都有明确说明。

## 共享可变状态规则

当实现引入 recorder、collector、scheduler、cache 或 concurrent map 时，验证必须同时检查外层和内层数据结构。

示例：

- `ConcurrentHashMap<String, ArrayList<T>>` 并不会自动变成整体线程安全。
- 如果可能发生 scheduled 或 concurrent sampling，每个 key 对应的 collection 也必须具备安全的并发语义，或者组件必须明确记录自己是单线程使用。

## 交接摘要格式

每次跨 agent 交接都应包含：

```text
Final verification:
- Active change: none / <name>
- Archived change: <path or N/A>
- Main spec sync: <path or N/A>
- openspec validate: pass/fail, command used
- tests: pass/fail, command used, test count if available
- spec scenarios mapped: yes/no
- worktree clean: yes/no
- final commit: <sha>
- residual risks: <none or list>
```

## 失败处理

如果 archive 之后的最终验证失败：

1. 不要声称 change 已完成。
2. 如果当前阶段允许维护，直接修复最终仓库状态。
3. 重新运行 OpenSpec validation 和测试。
4. 使用清晰的 fix commit message 单独提交修复。
5. 在交接摘要中更新 corrective commit SHA。
