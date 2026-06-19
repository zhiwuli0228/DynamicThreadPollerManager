# v0.14.0 Decision Log

## D1: 闭环架构 — 单线程轮询 vs 事件驱动

**背景**: `AdjustmentLoop` 需要一个执行模型来周期性执行采样→分类→评分→决策→执行循环。有两种主要架构选择。

**选项**:
- A: 单线程轮询（`while (state == RUNNING) { sleep(interval); doIteration(); }`）
- B: 事件驱动（`LivePressureSampler` 每次采样后发布事件，`AdjustmentLoop` 订阅并响应）
- C: `ScheduledExecutorService` 定时任务（`scheduler.scheduleAtFixedRate(iterationTask, 0, interval, MILLISECONDS)`）

**决策**: 选 A — 单线程轮询，逐步迁移到 C（ScheduledExecutorService）。

**理由**:
- 单线程轮询（选项 A）最简单、最透明、最容易测试——所有状态在当前线程中同步管理，不需要处理并发
- 事件驱动（选项 B）引入了事件总线和异步处理，对于单 executor 闭环而言过度复杂。当 v0.15.0+ 引入多 executor 并行闭环时，事件驱动模型的价值才会显现
- `ScheduledExecutorService`（选项 C）是选项 A 的自然演进——将 `sleep` 替换为定时的 `scheduleAtFixedRate`。但 C 引入了并发关注点（定时任务线程 vs 控制线程），增加了 `pause()`/`resume()` 的实现复杂度
- 选项 A 的一个潜在问题是 `sleep(interval)` 期间无法立即响应 `pause()`/`stop()` 调用。解决方案：使用 `wait(interval)` + `notify()` 替代 `sleep`，使控制线程可以唤醒循环线程
- IR 阶段应评估是否直接使用选项 C（ScheduledExecutorService + `ScheduledFuture.cancel()` 实现 pause）以简化实现

**影响**: `AdjustmentLoop` 内部使用 `while` 循环 + `wait/notify`（或 `ScheduledExecutorService`）。IR 阶段最终确定。

---

## D2: 调整决策模型 — 策略委派 vs 直接命令生成

**背景**: `DecisionOrchestrator` 需要从分类结果和候选策略列表中产出一个可执行的调整决策。有两种方式将"策略评分"连接到"调整执行"。

**选项**:
- A: 策略委派（Policy delegation）——选择最优策略，然后委托给 `PolicyEvaluator` 评估该策略以生成 `PolicyDecision`，最后将 `PolicyDecision` 转换为 `ScaleAdjustmentCommand`
- B: 直接命令生成（Direct command generation）——`AdjustmentDecision` 直接计算目标 poolSize（不经过 `PolicyEvaluator`），直接从分类结果和策略参数计算

**决策**: 选 A — 策略委派。

**理由**:
- 策略委派（选项 A）复用了 `PolicyEvaluator` 的决策逻辑（`SCALE_UP`/`SCALE_DOWN`/`HOLD` + 目标 poolSize 计算），保持决策逻辑集中。`AdjustmentLoop` 的职责是"在正确的时间选择正确的策略"，而非"生成正确的命令"
- 直接命令生成（选项 B）会在 `DecisionOrchestrator` 中重复 `ThresholdPolicyEvaluator` 的逻辑（阈值比较、步长计算），违反 DRY 原则
- 选项 A 保持了关注点分离：`PolicyScorer` 负责策略选择，`PolicyEvaluator` 负责命令生成，`AdjustmentLoop` 负责时机编排
- 如果未来引入非 `ThresholdPolicyEvaluator` 的评估器（如基于 ML 的评估器），选项 A 无需修改 `DecisionOrchestrator`
- v0.13.0 的 `PolicyScore` 已包含 `policyId`，可以直接映射到 `ThresholdPolicyConfig`——策略委派的类型链已完整

**影响**: `DecisionOrchestrator` 持有 `PolicyEvaluator` 引用。`AdjustmentDecision` 包含 `PolicyDecision` 字段。`toCommand()` 从 `PolicyDecision` 构造 `ScaleAdjustmentCommand`。

**边界情况**: 当 `PolicyEvaluator.evaluate()` 返回 `HOLD` 动作时，`AdjustmentDecision.isNoOp()` 返回 true，loop 跳过当前迭代。这处理了"最优策略建议不调整"的情况——分类和评分说"policy-X 最适合当前状态"，但 policy-X 的阈值评估说"当前状态不需要调整"。

---

## D3: 振荡检测策略 — 滑动窗口 vs 状态机

**背景**: 闭环调整的核心风险是配置振荡——系统在两种（或多种）配置之间来回切换而不收敛。需要检测并阻止这种模式。

**选项**:
- A: 滑动窗口模式匹配（Sliding window pattern matching）——检查最近 N 次调整形成的序列是否匹配已知振荡模式
- B: 状态机方法（State machine based）——构建一个显式的"振荡状态机"，每次调整触发状态转换
- C: 简单规则（Simple heuristics）——硬编码规则如"连续 2 次 reverse 方向 → 振荡"

**决策**: 选 A — 滑动窗口模式匹配。

**理由**:
- 滑动窗口（选项 A）可以检测多种振荡模式（乒乓、过度调整、策略切换振荡）而不需要为每种模式编写独立的状态机
- 状态机方法（选项 B）对复杂的多策略振荡建模能力强，但 v0.14.0 仅需检测 3 种明确的振荡模式，状态机的复杂性不匹配
- 简单规则（选项 C）是选项 A 的子集——滑动窗口可以表达简单规则，但反之不然
- 窗口大小（`oscillationWindowSize`）可配置，允许在不同场景下调优检测灵敏度
- 滑动窗口的实现是纯函数式的：输入历史记录列表 + 待处理决策 → 输出 `boolean`。易于单元测试

**检测算法设计**：

乒乓振荡检测：
```
目标 poolSize 序列: [10, 20, 10, 20] → 交替模式
检测: 计算相邻元素的差值的符号变化。如果符号在窗口内交替 ≥ patternThreshold 次 → 振荡
```

过度调整检测：
```
目标 poolSize 序列: [10, 15, 20, 25] → 连续增加
检测: 窗口内所有差值的符号相同 → 过度调整
```

策略切换振荡：
```
selectedPolicy 序列: [A, B, A, C, A] → A 出现 ≥ 3 次，且被其他策略隔开
检测: 窗口内唯一策略数 > 1，且至少一个策略被选中 ≥ 2 次且中间有不同策略
```

**影响**: `OscillationDetector` 是一个无状态类，接受 `List<HistoryEntry>` + `AdjustmentDecision` 作为输入。`oscillationWindowSize` 和 `oscillationPatternThreshold` 在 `LoopConfig` 中配置。

---

## D4: 反馈驱动权重校准 — 在线学习 vs 批量统计

**背景**: v0.13.0 DFR-01 要求基于实际调整结果校准评分权重。`ThresholdPolicyScorer` 的 4 个维度权重（0.35/0.30/0.20/0.15）需要从闭环数据中学习。

**选项**:
- A: 在线增量校准（Online incremental calibration）——每次调整后立即根据结果微调权重
- B: 批量统计校准（Batch statistical calibration）——收集 N 次调整后，批量分析并更新权重
- C: 不校准 — 等待更多数据后人工调整权重

**决策**: 选 B — 批量统计校准（窗口 = `feedbackCalibrationWindow`，默认 10）。

**理由**:
- 在线增量校准（选项 A）对单次异常数据过于敏感——一次"偶然成功"或"偶然失败"不应显著改变权重
- 批量统计（选项 B）使用统计窗口平滑噪声——10 次调整的统计模式比单次调整更有信息量
- 批量校准的时机是自然的：每当 `history.totalAdjustmentCount() % feedbackCalibrationWindow == 0` 时触发
- 选项 C 推迟了核心需求——DFR-01 的意图就是让系统能从数据中学习
- 批量窗口大小（10）与 `AdjustmentHistory` 的默认保留大小一致，不需要额外配置

**校准算法**：

对每个维度 d（responsiveness, safety, stability, efficiency）：
```
1. 计算维度 d 与调整成功之间的相关性:
   - 从最近 N 次调整中提取: (score_d_i, success_i) for i = 1..N
   - success_i = 1 如果调整后压力状态改善, 0 否则
   - correlation_d = 如果 score_d_i 高且 success_i=1 的频率高, 则 correlation > 0

2. 调整权重:
   - 如果 correlation_d > 0: weight_d += 0.02 (加分)
   - 如果 correlation_d ~ 0: weight_d 不变
   - 如果 correlation_d < 0: weight_d -= 0.02 (减分)

3. 归一化所有权重使 sum = 1.0
4. Clamp 每个权重到 [0.10, 0.50]
```

简单实现：不使用 Pearson 相关系数（对 ≤10 样本不可靠），使用"高分组的成功率 - 低分组的成功率"作为相关性近似。

**影响**: `FeedbackCalibrator` 依赖 `AdjustmentHistory` 和 `ThresholdPolicyScorer`。校准在 `AdjustmentLoop` 主循环中自动触发。初始权重 = v0.13.0 静态默认值。

---

## D5: 状态转换模型 — 有状态补充 vs 修改分类器

**背景**: v0.13.0 `SnapshotPressureClassifier` 是**无状态**的（每次 `classify()` 基于快照序列独立执行）。v0.14.0 需要状态转换追踪，但分类器不应被修改。

**选项**:
- A: 新增 `PressureStateMachine` 作为分类器的有状态包装器
- B: 修改 `SnapshotPressureClassifier` 添加有状态模式
- C: 修改 `PressureClassifier` 接口添加 `classifyWithHistory()` 方法

**决策**: 选 A — 新增 `PressureStateMachine` 作为独立组件。

**理由**:
- 不修改 v0.13.0 的稳定组件（`PressureClassifier`, `SnapshotPressureClassifier`）——它们是无状态的、可测试的、已验证的
- `PressureStateMachine` 是一个独立的关注点——它不分类，它验证并追踪分类结果之间的转换
- 闭环每次迭代中：分类器先产出 `PressureClassification`，状态机再验证 `previousState → newState` 转换的合法性
- 选项 B 和 C 会破坏分类器的单测隔离性（测试现在需要管理分类器的历史状态）
- 选项 A 的依赖关系清晰：`AdjustmentLoop` → `PressureClassifier`（分类） + `PressureStateMachine`（追踪转换）
- 与 v0.13.0 DFR-02 的意图一致："当前使用优先级链解决，不需要显式状态机" → v0.14.0 引入显式状态机

**异常转换的处理策略**:

`PressureStateMachine.isLegalTransition()` 返回三种结果：
- `LEGAL`：转换正常，记录
- `ANOMALOUS`（⚠️）：转换不常见但可能——分类器的置信度被降低一个等级（如 0.9 → 0.7），但不阻塞闭环。记录 warning
- `ILLEGAL`（❌）：转换在逻辑上不可能——可能是分类器配置错误或数据损坏。记录 severe warning，可选触发紧急停止

**影响**: `PressureStateMachine` 持有 `List<PressureStateTransition>` 作为转换历史。每个 `PressureStateTransition` 包含 from, to, timestamp, trigger（触发此转换的 adjustment decision reference）, legality。

---

## D6: Change 分解策略

**背景**: v0.14.0 包含两个子能力：闭环核心（loop + orchestrator + state machine）和闭环安全（oscillation detection + history + calibration + verification）。

**选项**:
- A: 单 change（`adaptive-closed-loop-adjustment`）
- B: 双 change（`adaptive-loop-core` → `oscillation-guard-and-loop-verification`）

**决策**: 选 B — 双 change。

**理由**:
- Change 1（`adaptive-loop-core`）可独立编译、独立测试：LoopState → AdjustmentLoop → DecisionOrchestrator → PressureStateMachine → AdjustmentDecision 构成完整的闭环骨架。OscillationDetector 和 FeedbackCalibrator 可 mock/stub
- Change 2（`oscillation-guard-and-loop-verification`）依赖 Change 1 的 `AdjustmentLoop`、`AdjustmentHistory` 和 `AdjustmentDecision` 类型，但振荡检测和权重校准算法独立于闭环主循环的实现细节
- Change 1 的测试验证"闭环能正确运行生命周期和决策编排"，Change 2 的测试验证"闭环能检测振荡、追踪历史、校准权重"
- 符合 managed-change-standard 的独立可验证性规则
- 遵循 v0.11.0/v0.12.0/v0.13.0 建立的双 change 模式

**影响**: 2 个 OpenSpec changes。Change 1 创建新包 `experiment.loop`。Change 2 在同一包中添加组件并实现端到端验证。

---

## D7: LoopEvidenceRecorder 设计 — 独立记录 vs 复用 EvidenceRecorder

**背景**: 闭环迭代需要记录证据（每次调整的决策、结果、前后分类）。v0.11.0 的 `EvidenceRecorder` 已经提供了 snapshot/evidence 记录接口。

**选项**:
- A: 新增 `LoopEvidenceRecorder` 接口，封装闭环特定的证据记录需求
- B: 直接使用 `EvidenceRecorder` 接口记录闭环证据
- C: 在 `AdjustmentHistory` 中嵌入证据记录

**决策**: 选 A — 新增 `LoopEvidenceRecorder`。

**理由**:
- `LoopEvidenceRecorder` 记录的语义与 `EvidenceRecorder` 不同：后者记录单个 run 的 snapshots/summaries，前者记录闭环的 iteration 序列（decision → result → observation 三元组）
- 新增接口允许在 Change 1 中使用 mock/stub，在 Change 2 中提供具体实现
- `LoopEvidenceRecorder` 可以使用 `EvidenceRecorder` 作为底层存储（组合，非继承）——将 loop iteration 证据写入与 run 关联的 evidence 存储
- 选项 B 将闭环证据与 run 证据混合，违反了"不同类型证据应有不同 API"的原则
- 选项 C 将存储逻辑嵌入 `AdjustmentHistory`，违反单一职责

**`LoopEvidenceRecorder` 接口**：

```java
public interface LoopEvidenceRecorder {
    void recordIteration(LoopSession session, int iterationIndex,
                        AdjustmentDecision decision, AdjustmentResult result,
                        PressureClassification beforeClassification);
    void recordSessionStart(LoopSession session);
    void recordSessionEnd(LoopSession session);
    List<LoopIterationEvidence> getIterationEvidence(String sessionId);
}
```

**影响**: `LoopEvidenceRecorder` 是一个新接口（`experiment.loop` 包）。Change 1 中定义接口（mock 实现），Change 2 中提供具体实现（可能委托到 `FileBackedEvidenceRecorder`）。

---

## DFR: Deferred 项

| ID | 描述 | 理由 | 后续版本 |
|---|---|---|---|
| DFR-01 | 多 executor 并行闭环协调 | 单 executor 闭环应首先验证闭环核心逻辑的正确性；多 executor 协调引入分布式一致性、资源竞争、策略隔离等新关注点 | 候选 v0.15.0 |
| DFR-02 | 闭环性能基准（闭环延迟 < 采样间隔的 10%） | 闭环延迟由分类器、评分器和调整适配器组成——每个组件的性能已在各自版本中验证。闭环本身的编排开销可忽略 | 候选 v0.15.0+ |
| DFR-03 | 策略自动生成（从历史数据学习最优阈值） | v0.14.0 的候选策略列表是预定义的 `ThresholdPolicyConfig` 列表。策略参数自动调优（如 scaleUpThreshold 的自动校准）需要更大规模的历史数据和更复杂的优化算法 | 候选 v0.16.0+ |
| DFR-04 | 基于比较运行的交叉验证（闭环调整 vs 静态策略 vs baseline） | v0.12.0 的比较基础设施可用于验证闭环是否确实优于静态策略，但此类比较需要独立的实验场景和统计显著性检验 | 候选 v0.15.0 |
| DFR-05 | 闭环调整的可视化/仪表盘 | UI 不在项目范围边界内 | 外部消费方 |
| DFR-06 | 多维度同时调整（一次调整同时修改 pool size + queue capacity + rejection policy） | 当前每次调整修改单一维度（通过单个 AdjustmentAdapter）。多维调整需要协调多个 adapter 的原子性和回滚 | 候选 v0.16.0+ |

## 既有 Deferred 项承接

| 来源 | ID | 描述 | v0.14.0 处置 |
|---|---|---|---|
| v0.13.0 | DFR-01 | 策略评分权重的自动校准 | ✅ 纳入 — `FeedbackCalibrator`（Change 2） |
| v0.13.0 | DFR-02 | 状态转换图/状态机 | ✅ 纳入 — `PressureStateMachine`（Change 1） |
| v0.13.0 | DFR-04 | 分类器性能基准 | ❌ 继续延后 — 闭环编排开销可忽略，分类器基准非 v0.14.0 关键路径 |
| v0.13.0 | DFR-03 | 复合压力状态 | ❌ 继续延后 — 复合状态增加复杂度但不改变闭环决策逻辑 |
