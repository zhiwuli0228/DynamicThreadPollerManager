# v0.10.0 Decision Log

## Header

- Version: `v0.10.0`
- Status: `DRAFT`
- Authoring date: `2026-06-13`

## Active Decisions

### D1: Direct Setter vs. Adapter-Only Mutation

**状态**: PROPOSED
**日期**: 2026-06-13

**决策**: `ManagedExecutor` 添加 `setRejectionPolicy(RejectedExecutionHandler)` 公开方法，`RejectionPolicyAdjustmentAdapter` 通过此方法执行策略替换。不采用"adapter 直接操作底层 TPE"的绕过模式。

**理由**:
- `ManagedExecutor` 是 executor 的唯一权威包装，所有 mutation 应经过它
- `setRejectionPolicy()` 同时更新 `ManagedExecutor.rejectionPolicy` 字段（改为 `volatile`），保持 `getRejectionPolicy()` 一致性
- 与 `setCorePoolSize`/`setMaximumPoolSize` 的模式一致：ManagedExecutor 提供 setter，adapter 调用 setter
- `ThreadPoolExecutor.setRejectedExecutionHandler()` 是线程安全的公开 API，无需额外保护

**备选方案**: Adapter 通过 `executor.unwrap().setRejectedExecutionHandler()` 直接操作底层 TPE。被拒绝原因：绕过 ManagedExecutor 导致 `getRejectionPolicy()` 返回过期值；破坏封装。

### D2: Safety Gate Criteria for Rejection Policy Replacement

**状态**: PROPOSED
**日期**: 2026-06-13

**决策**: `RejectionPolicySafetyGate` 至少检查以下条件：
1. Executor 存在且未 shutdown/terminated
2. 新 policy 非 null
3. 新 policy class != 当前 policy class（允许同类型不同实例的 no-op 检测留待 IR 细化）
4. Executor 不在 resize 操作中（`resizeInProgress` 检查）

**理由**:
- 条件 1 是基础安全检查：对已终止的 executor 替换策略无意义
- 条件 2 防止 NPE
- 条件 3 防止无意义的替换操作
- 条件 4 防止与 queue resize 并发导致状态混乱（resize 过程中 executor 正在被替换）

注意：与 `QueueResizeSafetyGate` 不同，rejection policy safety gate **不需要** readiness 评估 — policy 替换不涉及任务排空、线程终止或 executor 重建。

**备选方案**: 复用 `ControlGate<T>` 接口。被拒绝原因：v0.9.0 复盘已确认 `ControlGate<T>` 不适合非"快照 + readiness"模式的 safety gate（见 retrospective 流程改进 #3）。`RejectionPolicySafetyGate` 将使用与 `QueueResizeSafetyGate` 相同的独立接口模式。

### D3: Evidence Recording for Policy Replacement

**状态**: PROPOSED
**日期**: 2026-06-13

**决策**: 每次 rejection policy 替换操作记录以下证据：
- `beforePolicyClass`: String（旧 policy 的 canonical class name）
- `afterPolicyClass`: String（新 policy 的 canonical class name）
- `executorState`: ExecutorStateSnapshot（替换时刻的 executor 状态）
- `replacedAt`: Instant
- `success`: boolean
- `reason`: String

证据通过标准 `EvidenceRecorder` 管道记录。

**理由**:
- 与 v0.9.0 `ResizeEvidence` 模式一致但更简单（无需 before/after 双快照，policy 替换是瞬时操作）
- `beforePolicyClass`/`afterPolicyClass` 提供完整的审计轨迹
- 为未来 policy 趋势分析提供数据基础

### D4: ExecutorRebuildStrategy Policy Preservation Fix

**状态**: PROPOSED
**日期**: 2026-06-13

**决策**: 修改 `ExecutorRebuildStrategy.rebuild()` 第 75 行，将硬编码的 `new ThreadPoolExecutor.AbortPolicy()` 替换为 `oldTpe.getRejectedExecutionHandler()`。

**理由**:
- 当前行为是缺陷：rebuild 后的 executor 应保留原始配置的所有方面（core/max/keepAlive/threadFactory/rejectionPolicy），仅 queue capacity 变化
- v0.9.0 测试未覆盖此场景是因为所有测试使用默认 AbortPolicy，bug 不可见
- 修复一行代码，零架构影响

**备选方案**: 在 `QueueResizeCommand` 中添加可选的 rejection policy 字段，允许 resize 时同时指定新 policy。被拒绝原因：违反单一职责 — queue resize 和 policy replacement 应保持独立操作。

### D5: Change Decomposition Strategy

**状态**: PROPOSED
**日期**: 2026-06-13

**决策**: 2 个 change 串行交付：
1. `rejection-policy-command-and-adapter`（RejectionPolicyCommand, ManagedExecutor.setRejectionPolicy(), RejectionPolicyAdjustmentAdapter, RejectionPolicySafetyGate, PolicyReplacementEvidence, ExecutorRebuildStrategy 修复）
2. `rejection-policy-end-to-end-verification`（端到端集成测试：policy switch + 过载行为验证 + rebuild 策略保留）

**理由**:
- Change 1 包含所有核心组件和 ExecutorRebuildStrategy 修复，可独立编译和单元测试
- Change 2 依赖 change 1 的完整组件集进行端到端验证
- v0.9.0 经验：2 个 change 的分解粒度合理（v0.9.0 从 3 个合并为 2 个后在 SR 阶段确认）
- Rejection policy 替换比 queue resize 简单（无 rebuild 周期），change 1 的范围不会过大

实际 change 数量在 IR/SR 阶段确认后可能调整。

## Deferred

| ID | 项 | 原因 |
|---|---|---|
| DFR-01 | Automated CLI entry | v0.8.0 DFR-01, v0.9.0 DFR-01；持续 defer |
| DFR-02 | Data cleanup automation | v0.8.0 DFR-02；运维工具化阶段 |
| DFR-03 | Automatic policy trigger (closed-loop) | 需 trend detection/cooldown 先就位 |
| DFR-04 | Custom RejectedExecutionHandler implementations | 超出 v0.10.0 范围；JDK 内置四种策略覆盖主流场景 |
| DFR-05 | Multi-executor coordinated policy replacement | 架构范围外 |
