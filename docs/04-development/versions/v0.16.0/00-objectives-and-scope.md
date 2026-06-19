# v0.16.0 目标与范围

## Header

- Version name: `v0.16.0`
- Codename: `complex-workload-and`
- Status: `IMPLEMENTED`
- Current phase: `VERSION_DESIGN_DRAFT`
- Requirement theme: 复杂工作负载场景、回滚感知调整、基于时间的冷却、反振荡门、复杂场景验证报告、v0.15 遗留风险修复
- Authoring date: `2026-06-19`

## 1. 背景

### 1.1 已完成的能力基线

| 版本 | 能力 | 状态 |
|------|------|------|
| v0.7.0 | ManagedExecutor 域与闭环实验 | IMPLEMENTED |
| v0.8.0 | 真实数据获取与 metrics 管道集成 | IMPLEMENTED |
| v0.9.0 | 队列容量动态调整（ExecutorRebuildStrategy） | IMPLEMENTED |
| v0.10.0 | 拒绝策略动态替换 | IMPLEMENTED |
| v0.11.0 | 持久化证据录制与自主采样 | IMPLEMENTED |
| v0.12.0 | 基线比较实验框架 | IMPLEMENTED |
| v0.13.0 | 压力分类与策略评分 | IMPLEMENTED |
| v0.14.0 | 自适应闭环调整（振荡检测 + 反馈校准） | IMPLEMENTED |
| v0.15.0 | 多执行器协调 + 闭环交叉验证 | IMPLEMENTED |

### 1.2 当前缺口

v0.15.0 的闭环控制系统仅在简单工作负载（STEADY、RAMP、BURST）下验证。生产环境条件 — 突发到达、长尾延迟、混合 CPU/IO、下游阻塞 — 暴露了稳定性验证和故障恢复的缺口：

1. **无复杂工作负载场景** — `ScenarioProfile` 只有 3 个值，无法模拟 LONG_TAIL、MIXED_CPU_IO、DOWNSTREAM_BLOCKED 等真实场景
2. **无回滚机制** — 有调整无撤销：如果一次调整导致性能退化，系统无法自动恢复
3. **冷却基于计数器而非时间** — `DefaultRuntimeAdjustmentSafetyGate` 用整数计数器做冷却，不可注入时钟，不可精确控制
4. **反振荡仅建议性** — `CrossExecutorOscillationDetector` 只标记不阻断，无法防止持续 ping-pong
5. **验证报告覆盖不全** — 缺少 p95/p99 延迟、队列深度变化、吞吐量变化、每决策观察窗口
6. **v0.15 遗留风险** — 统计显著性使用合成代理数组、`ExecutorRegistry` 被传 null、线程安全契约未文档化、行为测试缺失

### 1.3 JDK API 可行性评估

v0.16.0 不涉及新的 `ThreadPoolExecutor` 属性变更。回滚、冷却、反振荡都是控制层行为组合，通过装饰器/门/守卫模式实现，不新增 executor mutation 维度。

| 问题 | 回答 |
|------|------|
| 是否涉及新的 TPE 属性变更？ | no |
| 是否需要新的 executor mutation？ | no — 回滚复用现有 `ScaleAdjustmentCommand` |
| 是否需要 executor rebuild？ | no |

### 1.4 为什么是现在

- 三个动态配置维度（线程数、队列容量、拒绝策略）全部完成
- v0.14.0 证明了单执行器闭环调整可行，v0.15.0 证明了多执行器协调可行
- 在进入生产化之前，必须在复杂、可重复的工作负载下证明稳定性
- 回滚是闭环控制的必要安全网 — 没有撤销能力的自主系统是不完整的
- v0.15 的 4 个遗留风险阻塞了统计可信度

## 2. 目标

1. **复杂工作负载场景** — 4 种确定性可重复的复杂场景（BURST、LONG_TAIL、MIXED_CPU_IO、DOWNSTREAM_BLOCKED），基于种子的可重复性
2. **回滚感知调整** — 捕获调整前快照、检测调整后退化、通过安全门发出有界回滚命令
3. **基于时间的冷却** — 可注入 `Supplier<Instant>` 时钟的冷却门，紧急回滚绕过冷却
4. **反振荡门** — 当检测到持续振荡时阻断非紧急调整的独立守卫
5. **复杂场景验证报告** — 基于真实观测数据的 p95/p99、队列深度、吞吐量增量、每决策观察窗口
6. **v0.15 风险修复** — 替换合成代理数组、修复 null `ExecutorRegistry`、记录线程安全契约、添加行为测试

## 3. 范围内

1. **场景基础设施** (`experiment.scenario`):
   - `ScenarioProfile` enum 扩展：新增 `LONG_TAIL`、`MIXED_CPU_IO`、`DOWNSTREAM_BLOCKED`
   - `DeterministicScenarioPlanner.plan()` 扩展：3 个新 profile 的确定性公式
   - `ScenarioDefinition.seed()` 用于可重复性

2. **调整层** (`experiment.adjustment`):
   - `RollbackAwareAdjustmentAdapter` — 装饰器，捕获前后快照，检测退化，发出回滚
   - `TimeBasedCooldownSafetyGate` — 实现 `RuntimeAdjustmentSafetyGate`，可注入时钟
   - `DegradationConfig` record — 可配置的退化阈值
   - `AdjustmentFailureCode.ANTI_OSCILLATION_ACTIVE` — 新失败码
   - `ScaleAdjustmentCommand` — 新增 `emergencyRollback` 字段

3. **循环控制** (`experiment.loop`):
   - `AntiOscillationGuard` — 独立守卫，咨询 `OscillationDetector`，可配置阈值
   - `AdjustmentLoop` — 集成 anti-oscillation guard（构造器注入，可空）

4. **协调层** (`experiment.coordination`):
   - `GroupLoopOrchestrator` — 修复 null `ExecutorRegistry`，集成 `AntiOscillationGuard`

5. **验证与报告** (`experiment.validation`):
   - `ComplexScenarioReport` record + `ObservationWindow` record
   - `ComplexScenarioReportGenerator` — 从真实证据计算所有指标
   - `ClosedLoopValidationRunner.computeSignificance()` — 替换合成代理数组

6. **证据** (`experiment.metrics`, `experiment.acquisition`):
   - `EvidenceRecorder` + `LoopEvidenceRecorder` 接口 — Javadoc 线程安全契约
   - 并发写入争用测试（InMemoryEvidenceRecorder、FileBackedEvidenceRecorder）

7. **行为测试** (`experiment.coordination`):
   - `CoordinatedAdjustmentAdapter` — 拒绝/封顶/批准/委托路径
   - `GroupLoopOrchestrator` — startAll/emergencyStopAll/getGroupHealth

## 4. 范围外

- 跨 JVM 或分布式协调
- 新的外部依赖
- 前端、数据库、消息队列、认证或监控集成
- 多维度同时调整
- 策略自动生成
- `pom.xml` 依赖或 Java 版本变更
- `provided-api/`、`src/**/api/**`、`src/**/contract/**` 路径

## 5. 架构对齐

| 架构文档 | 本版本如何处理 |
|----------|--------------|
| 装饰器模式 | RollbackAwareAdjustmentAdapter 延续 CoordinatedAdjustmentAdapter 的装饰器模式 |
| 安全门接口 | TimeBasedCooldownSafetyGate 实现 RuntimeAdjustmentSafetyGate，替代 DefaultRuntimeAdjustmentSafetyGate |
| 时钟注入 | 使用 Supplier<Instant>（已在 AdjustmentLoop 和 ScaleAdjustmentCommand 中建立）|
| 证据录制 | 所有回滚/阻断/冷却事件通过 LoopEvidenceRecorder 记录 |
| 枚举安全扩展 | ScenarioProfile 枚举扩展（无多态分发，仅在 planner switch 中使用）|

## 6. 成功标准（草案）

1. 所有 4 种复杂场景基于相同 seed 产生确定性、可重复的计划
2. 检测到退化时触发回滚，最多 1 次回滚/决策
3. 基于时间的冷却在配置窗口内拒绝非紧急调整
4. 紧急回滚绕过冷却和反振荡
5. 反振荡门阻止持续振荡场景中的非紧急调整
6. 报告从真实观测数据计算所有指标（无合成代理）
7. 现有测试全部通过，新增 112 测试通过
8. `pom.xml`、`provided-api/`、受保护路径不受影响

## 7. 当前阶段出口

- [x] 目标与非范围明确
- [x] 能力基线表完整
- [x] 成功标准可验证
- [ ] IR 需求分析完成
- [ ] SR 功能设计完成
- [ ] 复盘完成
