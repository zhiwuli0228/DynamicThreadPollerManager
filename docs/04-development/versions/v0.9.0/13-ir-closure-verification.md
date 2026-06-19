# v0.9.0 IR Closure Verification

## Header

- Document type: IR closure verification
- Version name: `v0.9.0`
- Verified artifacts: `11-ir-review.md`, `12-ir-review-disposition.md`
- Verification date: `2026-06-13`
- Verifier: IR author (post-disposition verification)

## Closure Verification

### P0 Findings

| Finding | Description | Disposition | Verified |
|---|---|---|---|
| F01 | Drain-and-replay 原子性语义 | FIX — IR-v0.9-002 补充原子性契约 | [x] |
| F02 | Adapter 扩展方式 | FIX — 明确新建 QueueResizeAdjustmentAdapter | [x] |

### P1 Findings

| Finding | Description | Disposition | Verified |
|---|---|---|---|
| F03 | Registry executorId 语义 | DEFER_TO_SR（推荐保持同一 ID） | [x] |
| F04 | ResizeEvidence 与 EvidenceRecorder | FIX — AdjustmentResult 直接携带 | [x] |
| F05 | SHRINK drain→shutdown race | FIX — 增加 stop-accepting → shutdown → drain 顺序 | [x] |
| F06 | AC 对应关系缺口 | FIX — 提升 AC 优先级 + 补充 missing AC | [x] |

### P2 Findings

| Finding | Description | Disposition | Verified |
|---|---|---|---|
| F07 | 线程配置读取时机 | DEFER_TO_SR（推荐 snapshot 值） | [x] |
| F08 | Drain-and-discard 策略 | DEFER_TO_SR（SR 阶段评估） | [x] |

## IR 正向检查复核

- [x] IR 只做需求分析，不隐含实现授权
- [x] 6 条 IR 覆盖 QueueResizeCommand → end-to-end 完整链路
- [x] Scope 边界明确：排除 reflection hack, rejection policy, closed-loop, 多执行器
- [x] 复用现有基础设施（ExecutorRegistry, ManagedExecutor, ControlGate 接口模式）
- [x] 现有 ScaleAdjustmentCommand 行为不受影响
- [x] 端到端测试覆盖 EXPAND + SHRINK + DENY 全路径
- [x] v0.7.0 回溯教训已纳入（P6: cleanup 顺序）
- [x] 15 个 AC 覆盖 P0 关键路径

## Deferred to SR

以下事项已明确推迟到 SR 阶段决策：

| 事项 | 来源 | 推荐方向 |
|---|---|---|
| Registry executorId 保持 vs 变更 | F03 | 保持同一 executorId |
| 线程配置读取时机 | F07 | 使用 decommission 入口处 snapshot 值 |
| Drain-and-discard 策略 | F08 | Drain-and-replay 为默认；SR 评估 SHRINK 时 discard 选项 |
| G10 resize gate | F06 | SR 决定是否需要 |

## 验证结论

**All P0/P1 findings CLOSED.** IR review 发现的 8 个 findings 已全部处置（4 FIX + 3 DEFER_TO_SR + 1 CLOSED via IR update）。IR 草案的原子性契约、adapter 策略、race condition 处理和 AC 覆盖缺口已修复。P2 findings 有明确的 SR 推荐方向。

**IR closure verified. 可以进入 SR（功能设计）阶段。**
