# v0.9.0 Decision Log

## Header

- Version: `v0.9.0`
- Status: `DRAFT`
- Authoring date: `2026-06-13`

## Active Decisions

### D1: Executor Rebuild vs. Queue Swapping

**状态**: PROPOSED
**日期**: 2026-06-13

**决策**: 采用 Executor Rebuild 策略（decommission → commission），不使用反射或自定义子类替换 work queue。

**理由**:
- `ThreadPoolExecutor.workQueue` 是 final 字段，无公开 setter
- 反射替换依赖 JDK 内部实现，跨版本不稳定
- Rebuild 策略可以利用现有 `ExecutorRegistry` 的注册/注销机制
- Rebuild 是显式的、可审计的操作，每一步都可记录证据

**备选方案**: 自定义 `ResizableThreadPoolExecutor` 子类包装可替换 queue。被拒绝原因：增加了不必要的继承层次；rebuild 策略更简单且覆盖全部语义。

### D2: Blocking vs. Non-Blocking Resize

**状态**: PROPOSED
**日期**: 2026-06-13

**决策**: Resize 操作默认同步（blocking），调用方等待 rebuild 完成后返回。可选 timeout 参数。

**理由**:
- 异步 resize 会增加"resize 中但尚未完成"的中间状态，复杂化安全门禁逻辑
- 同步 resize 的等待时间可控（drain tasks + shutdown + awaitTermination + create new）
- 与现有 `ScaleAdjustmentCommand` 的同步语义一致

**备选方案**: 异步 resize（返回 Future/CompletableFuture）。被拒绝原因：当前系统无异步调整先例，引入异步会增加并发复杂度而无明显收益。

### D3: Safety Gate Criteria for Queue Resize

**状态**: PROPOSED
**日期**: 2026-06-13

**决策**: QueueResizeSafetyGate 至少检查以下条件：
1. Executor 存在且状态为 RUNNING
2. 新容量 > 0
3. 新容量 != 当前容量
4. 若缩小队列（newCapacity < currentCapacity）：当前 queue 深度 <= newCapacity（否则需先排空）
5. Executor 不在已有的 resize 操作中（幂等保护）

**理由**:
- 条件 4 是最关键的安全检查：缩小队列时如果 queue 深度超过新容量，必须拒绝或先排空
- 条件 5 防止并发 resize 导致状态混乱

### D4: Evidence Recording for Resize

**状态**: PROPOSED
**日期**: 2026-06-13

**决策**: 每次 resize 操作记录以下证据：
- `beforeState`: ExecutorStateSnapshot（pre-rebuild）
- `afterState`: ExecutorStateSnapshot（post-rebuild, newly created executor）
- `rebuildDurationMs`: long（shutdown + create 耗时）
- `drainedTaskCount`: int（从旧 queue 排空的任务数）
- `resizeDirection`: ENUM（EXPAND / SHRINK）
- `success`: boolean

证据通过标准 `EvidenceRecorder` 管道记录。

**理由**:
- 与现有 `ScaleAdjustmentCommand` 的证据模式一致
- 为未来 trend detection 和 learned policies 提供数据基础

### D5: Change Decomposition Strategy

**状态**: PROPOSED
**日期**: 2026-06-13

**决策**: 2-3 个 change 串行交付：
1. `queue-resize-command-and-rebuild`（命令 + 重建策略 + 适配器）
2. `queue-resize-safety-and-evidence`（安全门禁 + 证据 + G10）
3. `queue-resize-end-to-end-verification`（端到端集成测试）

Change 1 和 2 可部分并行（change 2 的安全门禁可先定义接口，change 1 实现后集成）。Change 3 必须等 1+2 完成。

实际 change 数量在 IR/SR 阶段确认后可能合并为 2 个。

## Deferred

| ID | 项 | 原因 |
|---|---|---|
| DFR-01 | Automated CLI entry | v0.8.0 DFR-01；可纳入 v0.9.0 作为次要目标，或继续 defer |
| DFR-02 | Data cleanup automation | v0.8.0 DFR-02；运维工具化阶段 |
| DFR-03 | Rejection policy runtime switching | 与 queue resizing 正交；需单独设计 |
| DFR-04 | Automatic resize trigger (closed-loop) | 需 trend detection/cooldown 先就位；留给 v0.10.0+ |
| DFR-05 | Multi-executor coordinated resize | 架构范围外 |
