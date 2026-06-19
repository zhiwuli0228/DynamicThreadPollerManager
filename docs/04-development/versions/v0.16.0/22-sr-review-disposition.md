# v0.16.0 SR Review Disposition

## Header

- Document type: SR review disposition
- Version name: `v0.16.0`
- Reviewed artifact: `docs/04-development/versions/v0.16.0/21-sr-review.md`
- Disposition date: `2026-06-20`
- Disposition by: SR author, responding to independent review

## Disposition Summary

| Total Findings | FIX | DEFER | ACCEPT |
|---|---|---|---|
| 6 | 3 | 2 | 1 |

---

## Per-Finding Disposition

### F01 [P1] 冷却 key 按 `command.runId()` 而非 executor name → **FIX (documented, deferred fix)**

**处置**: 接受 finding。SR §9 已记录为已知 P1 偏差。此处置中新增修复方案和触发条件，SR 文档同步更新。

**具体内容**:

SR §9 偏差表 F01 行更新为包含修复方案：

| 偏差 | 级别 | 细节 | 修复方案 | 触发条件 |
|------|------|------|---------|---------|
| 冷却 key 按 runId 而非 executor name | P1 | `lastAppliedInstant` map 按 `command.runId()` 建 key。多执行器运行时，所有执行器共享冷却 | key 改为 `runId + ":" + executorName`。`ScaleAdjustmentCommand` 需新增 `executorName()` 字段或 `TimeBasedCooldownSafetyGate` 构造器接受 executor discriminator | 多执行器组场景出现冷却粒度过粗导致的操作延迟时 |

**理由**: 当前单执行器场景不受影响（`runId` 与 executor name 一一对应）。修复涉及 `ScaleAdjustmentCommand` API 变更，需在后续版本中统一处理。

**SR 更新**: §9 偏差表补充"修复方案"和"触发条件"列。

---

### F02 [P1] `AntiOscillationGuard.evaluate()` 返回 `allow(0, null)` → **FIX**

**处置**: 选择 review 推荐方案 B — 使用语义更准确的 `SafetyGateDecision` 工厂方法。紧急回滚路径返回 `noOp("bypassed by emergency rollback")`，无振荡路径返回 `noOp("no sustained oscillation detected")`。

**具体内容**:

`AntiOscillationGuard.evaluate()` 修改:

```java
// Before (line 54-55):
if (isEmergencyRollback) {
    return SafetyGateDecision.allow(0, null);
}

// After:
if (isEmergencyRollback) {
    return SafetyGateDecision.noOp("bypassed by emergency rollback");
}

// Before (line 74):
return SafetyGateDecision.allow(0, null);

// After:
return SafetyGateDecision.noOp("no sustained oscillation detected");
```

**理由**: `noOp` 语义更准确 — 守卫的职责是"是否阻断"，而非"是否允许"。紧急回滚绕过守卫属于"该决策被免除"，无振荡属于"无阻断操作"。`SafetyGateDecision.noOp(String)` 不携带 command，彻底消除 NPE 风险。`AdjustmentLoop` 当前仅检查 `isAllowed()`（`noOp` 的 outcome 为 `ALLOW`）— 行为不受影响。

**SR 更新**: §4.4 `AntiOscillationGuard.evaluate()` 伪代码 line 202, 211 改为 `SafetyGateDecision.noOp(...)`；API 签名验证增加 `SafetyGateDecision.noOp(String)` 条目。

---

### F03 [P2] `DegradationConfig` 三阈值仅 `queueDepthThreshold` 被使用 → **DEFER**

**处置**: DEFER。已在 SR §9 和 IR review (F04) 中记录。`throughputDropThreshold` 和 `latencyIncreaseThreshold` 保留为 "reserved for future use"。

**理由**: 队列深度是当前最可靠的退化指标 — 吞吐量在单次快照中高方差，延迟需要 per-task 时间戳。保留字段避免未来 API 变更，且验证逻辑确保配置值合法。SR §9 文档是充分的。

**SR 更新**: 无。已有文档充分覆盖。

---

### F04 [P2] 延迟百分位使用队列深度代理 → **DEFER**

**处置**: DEFER。已在 SR §4.5 (line 253) 和 §9 (line 376) 中记录。`ComplexScenarioReport` 字段名 (`p95LatencyMs`/`p99LatencyMs`) 保留以与报告语义对齐。

**理由**: 队列深度与任务等待时间正相关 — 高队列深度是高延迟的可靠代理指标。真实延迟测量需要 per-task 提交/开始/结束时间戳，超出当前执行器模型（仅快照 `completedTaskCount`）范围。字段名在"综合场景报告"上下文中对读者有意义。

**SR 更新**: 无。已有文档充分覆盖。

---

### F05 [P2] `computeSignificance()` jitter fallback → **DEFER**

**处置**: DEFER。已在 SR §4.6 (line 295-296) 和 §9 (line 377) 中记录为 defense-in-depth。

**理由**: Jitter fallback 仅在 `EvidenceRecorder` 数据不可用时激活（无证据录制的遗留运行）。移除它会破坏向后兼容性。Jitter 是确定性的（`(i % 3 - 1) * 0.5%`），非 v0.15 的 `Math.random()` 合成。当所有运行强制证据录制后可安全移除。

**SR 更新**: 无。已有文档充分覆盖。

---

### F06 [P3] `BiConsumer` 回调模式 — 正向偏差 → **ACCEPT**

**处置**: ACCEPT。确认此正向偏差。`BiConsumer<AdjustmentResult, AdjustmentResult>` 比直接注入 `LoopEvidenceRecorder` 更松耦合。

**理由**:
- 适配器不依赖会话上下文（`LoopEvidenceRecorder` 绑定到 `LoopSession`）
- 测试可注入 `(a, b) -> {}` no-op lambda
- 调用者（`AdjustmentLoop`）管理录制器注入，适配器保持可测试性
- 无额外依赖引入

**SR 更新**: 无。SR §4.2 line 141 已记录此设计选择。

---

## 修改后的 SR 更新计划

| SR 组件 | 变更 |
|---|---|
| §4.4 AntiOscillationGuard | `evaluate()` 伪代码中 `allow(0, null)` → `noOp("bypassed by emergency rollback")` 和 `noOp("no sustained oscillation detected")` |
| §4.4 API 签名验证 | 增加 `SafetyGateDecision.noOp(String)` 条目 |
| §9 已知偏差 | F01 行补充"修复方案"和"触发条件"列 |

无其他 SR 修改。三个 P2 finding (F03/F04/F05) 和 P3 finding (F06) 不影响 SR 文档内容。

---

## 处置后状态

| Finding | 原始级别 | 处置 | 状态 |
|---|---|---|---|
| F01 | P1 | FIX (SR §9 增加修复方案和触发条件) | CLOSED |
| F02 | P1 | FIX (改用 `noOp` 替代 `allow(0, null)`) | CLOSED |
| F03 | P2 | DEFER (SR §9 已记录) | CLOSED |
| F04 | P2 | DEFER (SR §4.5 + §9 已记录) | CLOSED |
| F05 | P2 | DEFER (SR §4.6 + §9 已记录) | CLOSED |
| F06 | P3 | ACCEPT (正向偏差) | CLOSED |

## 出口条件

所有 P0/P1 findings 已处置（3 FIX + 2 DEFER + 1 ACCEPT）。可进入 SR closure verification。
