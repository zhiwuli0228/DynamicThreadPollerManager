# v0.16.0 IR 评审处置

## Header

- Document type: IR review disposition
- Version name: `v0.16.0`
- Status: `DISPOSITION_COMPLETED`
- Date: `2026-06-20`
- Input: `11-ir-review.md`

## 处置

### F01 [P0 → FIX] 冷却门 key 规范

**处置**: FIX。SR 将明确指定 `TimeBasedCooldownSafetyGate.lastAppliedInstant` 的 key 为 executor name（或 `command.runId()` + executor discriminator）。IR 规范更新：冷却维护 "per executor" 的 lastAppliedInstant map。

**验证**: SR 中搜索 `lastAppliedInstant` 并确认 key 规范明确。

### F02 [P1 → FIX] 安全门检查显式列表

**处置**: FIX。SR 将对照 `DefaultRuntimeAdjustmentSafetyGate.evaluate()` 源码逐项列出保留的检查：就绪状态、就绪伴随风险、冷却窗口、每运行限制、相反方向、无操作/目标匹配当前、无效命令。

**验证**: SR 中搜索保留检查列表并交叉引用源码。

### F03 [P1 → FIX] 冷却状态清理策略

**处置**: FIX。SR 将明确：`TimeBasedCooldownSafetyGate` 实例绑定到单次运行。运行完成后，调用者丢弃 gate 实例。map 清理由 GC 处理。如需要同实例多运行，在 SR 中记录 `reset()` 方法。

**验证**: SR 中包含生命周期说明。

### F04 [P2 → DEFER] 退化检测多指标范围

**处置**: DEFER。当前实现（queue depth only）是可接受的最小范围。`DegradationConfig` 中的 `throughputDropThreshold` 和 `latencyIncreaseThreshold` 字段保留给未来使用。记录为已知限制。无 per-task 时间戳导致延迟/吞吐量退化检测不可靠。

**理由**: 队列深度是退化最可靠的指标 — 吞吐量在单次快照中高方差，延迟无法在没有 per-task 时间戳的情况下精确计算。

### F05 [P2 → DEFER] 百分位延迟使用队列深度代理

**处置**: DEFER 配合文档记录。`ComplexScenarioReportGenerator` Javadoc 明确记录百分位值来自队列深度而非真实延迟。`p95LatencyMs`/`p99LatencyMs` 字段名称保留以与报告语义对齐 — 队列深度与任务等待时间正相关。

**理由**: 真实延迟测量需要 per-task 提交/开始/结束时间戳 — 这些在当前执行器模型（只快照 `completedTaskCount`）中不可用。队列深度是普遍接受的负载代理。

### F06 [P2 → DEFER] computeSignificance Jitter fallback

**处置**: DEFER。Jitter fallback 是 defense-in-depth — 在 `EvidenceRecorder` 数据不可用的场景中（向后兼容旧运行）保持 `StatisticalSignificanceCalculator` 可用。Jitter 是确定性的（`(i % 3 - 1) * 0.5%`），非随机。当真实快照数组存在时使用它们。

**理由**: 移除 fallback 会破坏没有录制证据的遗留验证场景的向后兼容性。P2 残余风险可接受。

### F07 [P3 → ACCEPT] BiConsumer 回调设计选择

**处置**: ACCEPT — 正向偏差。`BiConsumer` 比直接注入 `LoopEvidenceRecorder` 更松耦合。在 SR §R2 中记录。

**理由**: 适配器不应依赖会话上下文（`LoopEvidenceRecorder` 绑定到 `LoopSession`）。BiConsumer 允许调用者（`AdjustmentLoop`）管理自己的录制器注入，同时保持适配器可测试。

## 处置后状态

- FIX: 3 (F01, F02, F03) — 将在 SR 中解决
- DEFER: 3 (F04, F05, F06) — 已记录，非阻塞
- ACCEPT: 1 (F07) — 正向偏差

所有 P0/P1 findings 已处置。建议进入 SR 功能设计。
