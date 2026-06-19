# v0.16.0 Decision Log

## D1: 整体策略 — 混合方案（扩展枚举 + 组合行为）

**背景**: 需要在已有代码基础上添加复杂场景、回滚、冷却、反振荡 4 个新关注点，同时修复 v0.15 遗留风险。

**选项**:
- A: 全部就地修改已有类型
- B: 全部使用装饰器/包装器模式
- C: 混合 — 扩展枚举 + 新装饰器/守卫类型组合

**决策**: 选 C — 混合方案。

**理由**:
- `ScenarioProfile` 是简单枚举，无下游多态分发（仅在 planner switch 中使用），添加新值是安全且最小的修改
- 回滚、冷却、反振荡是三个独立行为关注点，适合独立类型，遵循已有的 `CoordinatedAdjustmentAdapter` 装饰器模式
- v0.15 风险修复是小范围、局部化修改，适合就地修复
- `AdjustmentLoop` 已有 315 行和 15 步循环 — 内联新行为会违反单一职责

**影响**: 3 个新增枚举值 + 4 个新增主要类型 + 少量已有类型修改。

---

## D2: 回滚 — 适配器装饰器 + 安全门注入

**背景**: 需要检测调整后退化并发出回滚命令恢复到安全状态。

**选项**:
- A: 在 `AdjustmentLoop` 中内联回滚逻辑
- B: 独立的 `RollbackAwareAdjustmentAdapter` 装饰器

**决策**: 选 B — 装饰器模式。

**理由**:
- 适配器模式已被 `CoordinatedAdjustmentAdapter` 建立
- 回滚是 per-executor 关注点，属于适配器链
- 安全门需要评估回滚命令 — 注入避免与循环产生循环依赖
- 内联到 AdjustmentLoop 会违反单一职责

**影响**: 新增 `RollbackAwareAdjustmentAdapter` (153 行)、`DegradationConfig` record。

---

## D3: 冷却 — 独立 Gate 实现

**背景**: 需要将计数器冷却替换为基于时间的冷却，使用可注入时钟。

**选项**:
- A: 修改 `DefaultRuntimeAdjustmentSafetyGate` 就地替换
- B: 独立的 `TimeBasedCooldownSafetyGate` 实现

**决策**: 选 B — 独立实现。

**理由**:
- 现有 gate 使用整数计数器（`cooldownRemaining`），基于时间的冷却是完全不同的机制
- 独立实现避免修改已有 gate 和破坏已有测试
- `Supplier<Instant>` 模式已在 `AdjustmentLoop` 和 `ScaleAdjustmentCommand` 中使用
- 两种 gate 可并存

**影响**: 新增 `TimeBasedCooldownSafetyGate` (154 行)。

---

## D4: 反振荡 — 独立 Guard

**背景**: 需要从"建议性"升级为"阻断性"反振荡，在持续振荡时阻止非紧急调整。

**选项**:
- A: 扩展 `OscillationDetector` 添加阻断逻辑
- B: 扩展 `RuntimeAdjustmentSafetyGate` 添加振荡检查
- C: 独立的 `AntiOscillationGuard` 类

**决策**: 选 C — 独立 Guard。

**理由**:
- 守卫是独立关注点 — 振荡检测（识别模式）和守卫（执行策略）职责分离
- 独立测试和配置更简单
- 复用已有 `SafetyGateDecision` 返回类型，新增 `ANTI_OSCILLATION_ACTIVE` 失败码

**影响**: 新增 `AntiOscillationGuard` (93 行)、`AdjustmentFailureCode.ANTI_OSCILLATION_ACTIVE`。

---

## D5: 报告 — 独立 Report 类型

**背景**: 需要为复杂场景生成包含回滚计数、恢复时间、百分位延迟、观察窗口的报告。

**选项**:
- A: 扩展 `ValidationComparisonReport`
- B: 独立的 `ComplexScenarioReport` record

**决策**: 选 B — 独立类型。

**理由**:
- 已有报告绑定到 3 模式比较（baseline、static、closed-loop）
- 复杂场景报告有不同字段和不同数据源
- 分离遵循单一职责

**影响**: 新增 `ComplexScenarioReport` record、`ObservationWindow` record、`ComplexScenarioReportGenerator` (276 行)。

---

## D6: AdjustmentLoop 集成 — 构造器注入

**背景**: `AntiOscillationGuard` 需要集成到 15 步循环中的振荡检测和冷却门之间。

**选项**:
- A: 责任链模式
- B: 新增接口抽象
- C: 构造器注入（可空参数）

**决策**: 选 C — 构造器注入。

**理由**:
- 构造器注入遵循已有模式（循环已有 15 个构造器参数）
- 可空参数保持向后兼容性 — 已有调用者传 null 即可
- 步骤顺序固定且已知，不需要责任链的灵活性

**影响**: `AdjustmentLoop` 构造器新增 1 个参数。
