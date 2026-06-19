# v0.10.0 IR Closure Verification

## Header

- Document type: IR closure verification
- Version name: `v0.10.0`
- Verified artifacts: `11-ir-review.md`, `12-ir-review-disposition.md`
- Verification date: `2026-06-13`
- Verifier: IR author (post-disposition verification)

## Closure Verification

### P0 Findings

| Finding | Description | Disposition | Verified |
|---|---|---|---|
| F01 | PolicyReplacementResult 被引用但未定义 | FIX — 新增 IR-v0.10-003a | [x] |
| F02 | resizeInProgress 可见性缺口 | FIX — 暴露 public 方法 + 注入 Predicate | [x] |

### P1 Findings

| Finding | Description | Disposition | Verified |
|---|---|---|---|
| F03 | 并发 policy 替换幂等性判断不一致 | FIX — 明确记录 last-write-wins 语义 | [x] |
| F04 | Rebuild 修复测试覆盖范围 | DEFER_TO_SR（一个 P0 AC 足够） | [x] |
| F05 | fromCurrent() class vs equals 策略 | FIX — IR 直接决定 class 比较 | [x] |

### P2 Findings

| Finding | Description | Disposition | Verified |
|---|---|---|---|
| F06 | Discard 策略端到端断言策略 | DEFER_TO_SR（推荐方向已记录） | [x] |
| F07 | rejectionPolicy 字段 volatile vs 直接委托 | DEFER_TO_SR（推荐方案 B 直接委托） | [x] |

## IR 正向检查复核

- [x] IR 只做需求分析，不隐含实现授权
- [x] 7 条 IR（含新增 IR-v0.10-003a PolicyReplacementResult）覆盖 RejectionPolicyCommand → end-to-end 完整链路
- [x] Scope 边界明确：排除自定义 handler, closed-loop, 多执行器协调
- [x] 与 v0.9.0 的关键技术差异正确识别（无需 executor rebuild）
- [x] 复用现有基础设施（ExecutorRegistry, ManagedExecutor, ExecutorStateSnapshot）
- [x] v0.9.0 复盘三项流程改进全部应用（独立 result 类型、ControlGate 边界、SR 伪代码校验规则）
- [x] 现有 ManagedExecutorAdjustmentAdapter 和 QueueResizeAdjustmentAdapter 行为不受影响
- [x] 16 个 AC 覆盖 P0 关键路径 + P1 证据记录
- [x] 并发语义明确：policy-policy last-write-wins，policy-resize 需保护
- [x] ExecutorRebuildStrategy 修复是最小化变更（一行代码）
- [x] fromCurrent() 使用 class 比较决策已拍板（自定义 handler out of scope）

## Deferred to SR

以下事项已明确推迟到 SR 阶段决策：

| 事项 | 来源 | 推荐方向 |
|---|---|---|
| Rebuild 修复测试覆盖范围（是否需要全四种策略验证） | F04 | 一个非默认 policy 测试足够 |
| DiscardPolicy/DiscardOldestPolicy 端到端断言策略 | F06 | IR 已给出推荐断言方向 |
| rejectionPolicy 字段方案（volatile vs 直接委托 TPE） | F07 | 推荐方案 B（删除字段，直接委托 TPE） |
| resizeInProgress 检查的注入方式（Predicate vs 自定义接口） | F02 | 推荐函数式接口 Predicate\<String\> |

## 验证结论

**All P0/P1 findings CLOSED.** IR review 发现的 7 个 findings 已全部处置（3 FIX + 3 DEFER_TO_SR + 1 CLOSED via IR update）。IR 草案的 PolicyReplacementResult 定义缺口、resizeInProgress 可见性、并发语义不一致、fromCurrent() 策略悬空已修复。P2 findings 有明确的 SR 推荐方向。

**IR closure verified. 可以进入 SR（功能设计）阶段。**
