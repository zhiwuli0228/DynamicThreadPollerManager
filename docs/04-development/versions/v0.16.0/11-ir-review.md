# v0.16.0 IR 评审

## Header

- Document type: IR review
- Version name: `v0.16.0`
- Status: `DISPOSITION_APPLIED`
- Review date: `2026-06-20`
- Input: `10-ir.md`
- Reviewer: independent review

## Findings

### F01 [P0] DFR-01: 冷却门 key 按 executor name 还是 runId

IR-07 声明冷却维护 "per-executor 的 lastAppliedInstant map"，但未明确 key 是 executor name 还是 runId。在多执行器场景（v0.15.0）中，多个执行器共享同一个 runId。如果 key 是 runId，执行器 A 的调整会设置执行器 B 的冷却 — 这是不符合需求的。

**建议**: IR-07 补充明确 key = executor name。设计阶段（SR）必须指定 key 类型并解释多执行器语义。

### F02 [P1] IR-09: "所有其他安全检查"未明确列举

IR-09 声明保留其他检查但未逐一列举。潜在遗漏风险 — 某个检查可能在迁移中被遗漏。

**建议**: SR 中对照 `DefaultRuntimeAdjustmentSafetyGate` 的 `evaluate()` 源码逐项列出保留的检查。

### F03 [P1] IR-07: 冷却 per-run 状态重置不明确

`lastAppliedInstant` map 每个 run 增长。run 完成后如何清理？若 map 无限增长，长时间运行的系统中可能内存泄漏。

**建议**: SR 中指定清理策略或明确声明"run 完成后调用者负责创建新 gate 实例"。

### F04 [P2] IR-04: 退化检测仅检查队列深度

IR-04 描述队列深度、吞吐量、延迟三个指标，但当前代码中 `isDegraded()` 仅检查队列深度。`DegradationConfig` 包含所有三个阈值但仅队列深度被使用。

**建议**: SR 明确退化检测的多指标范围，或记录吞吐量/延迟检查为延期项。

### F05 [P2] IR-14: 百分位延迟使用队列深度代理

`ComplexScenarioReportGenerator.computePercentile()` 使用队列深度作为延迟代理，而非真实延迟测量。真实延迟需要每个任务的开始/结束时间戳。

**建议**: 记录为已知限制 — 真实延迟测量在无 per-task 时间戳的情况下不可行。队列深度是高延迟的相关代理。

### F06 [P2] IR-15: computeSignificance 仍保留 jitter fallback

真实快照不可用时，`computeSignificance()` 回退到 jitter 扩展方法（`expandWithJitter()`）。这不是完全合成数据（确定性 jitter，非随机），但也不是纯真实数据。

**建议**: 记录为残余风险。jitter fallback 是 defense-in-depth — 仅在 `EvidenceRecorder` 数据缺失时激活（向后兼容没有录制证据的运行）。

### F07 [P3] IR-05: 回滚证据通过 BiConsumer 回调而非直接注入

`RollbackAwareAdjustmentAdapter` 使用 `BiConsumer<AdjustmentResult, AdjustmentResult>` 进行回滚报告，而非直接注入 `LoopEvidenceRecorder`。与 IR-05 描述不完全一致。

**建议**: 正向偏差 — BiConsumer 更松耦合。在 SR 中记录此设计选择。

## 评审总结

- P0 findings: 1 (F01)
- P1 findings: 2 (F02, F03)
- P2 findings: 3 (F04, F05, F06)
- P3 findings: 1 (F07)
- 建议进入处置阶段
