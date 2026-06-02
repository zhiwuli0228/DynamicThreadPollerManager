# Metrics Snapshot and Recording 跨 Agent 协作复盘

## 头信息

- 日期：`2026-06-02`
- Change：`metrics-snapshot-and-recording`
- 观察阶段：archive 和 main spec sync 之后
- 结果：需要补充修复，且修复已完成

## 发生了什么

`metrics-snapshot-and-recording` change 已经完成实现、验证、finalize 和 archive。后续复查发现，仓库最终状态仍然存在问题：

- 同步到 `openspec/specs/` 下的 main spec 仍然是 delta-spec 形态，导致 OpenSpec validation 失败。
- spec 明确提到 `pool size` 和 `completed task count`，但实现和测试只覆盖了 active threads、queue size 和 CPU utilization。
- in-memory recorder 使用了 `ConcurrentHashMap`，但每个 run 对应的 value 是 `ArrayList`，线程安全风险被外层线程安全 map 掩盖。

随后通过 corrective commit 修复：补齐 main spec 格式，增加 `poolSize` 和 `completedTaskCount` 支持，更新测试，并将每个 run 对应的 list 替换为具备并发安全语义的 list 类型。

## 根因

### 1. 跳过了最终状态验证

归档前的 `verify.md` 被当作了充分证据。但它不是最终证据。archive 之后的仓库最终状态包含不同文件，也适用不同的验证规则。

### 2. 混淆了 delta spec 和 main spec 格式

active change 中的 delta spec 在 archive 前是有效的，但同步后的 main spec 需要 `## Purpose` 和 `## Requirements`。archive 步骤改变了验证目标。

### 3. requirement 到 test 的映射不完整

spec scenario 列出了具体指标，但测试只断言了较小的已实现字段集合。task 全部勾选掩盖了指标覆盖缺口。

### 4. 表层并发检查遗漏了内层 collection

外层 `ConcurrentHashMap` 让 recorder 看起来具备并发意识，但每个 key 对应的 `ArrayList` 在并发 append 下仍然不安全。

## 已采纳的改进

### 1. 强制执行最终仓库状态验证

archive 或交接之后，agent 必须运行：

```powershell
openspec.cmd validate --all --json
.\mvnw.cmd test
git status --short
```

### 2. 必须显式验证 main spec sync

agent 必须在 archive 后检查 `openspec/specs/**/spec.md`，不能只检查 archive 前的 `openspec/changes/**/specs/**/spec.md`。

### 3. scenario 覆盖必须具体

每个 spec scenario 都应该映射到实现和测试。如果 scenario 命名了具体字段，测试必须断言这些字段；否则就应该修订 spec。

### 4. 交接摘要必须包含证据

跨 agent 交接必须报告执行过的命令、通过/失败结果、最终 commit SHA 和残余风险。

### 5. 并发检查必须检查嵌套状态

任何 recorder、collector、cache、scheduler 或 concurrent map 都应该检查嵌套的可变状态。外层容器线程安全不足以证明整体线程安全。

## 当前修复状态

修复提交：

```text
63426d95328647561af17a96c60e0091436a14ba
fix(metrics): align snapshot spec and implementation
```

已执行的修复验证：

```text
openspec.cmd validate --all --json  -> pass
.\mvnw.cmd test                     -> pass, 53 tests
git status --short                  -> clean after commit
```

## 后续必须遵守的行为

后续 agent 在处理 OpenSpec closeout 时必须遵守：

- `docs/08-retrospectives/agent-handoff-closeout-standard.md`
- `docs/02-harness/verification-policy.md`
- `docs/00-project/current-state.md`

如果这些文档之间存在冲突，`docs/00-project/current-state.md` 仍然是执行授权来源；本复盘目录提供 closeout 纪律和交接要求。
