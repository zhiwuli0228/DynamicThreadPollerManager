# v0.7.0 Decision Log

## Header

- Version: `v0.7.0`
- Purpose: 记录 v0.7.0 设计过程中的关键架构和范围决策
- Status: `DRAFT`

## Decisions

### D-001: 使用真实 ThreadPoolExecutor 而非继续扩展探针

- **日期**: 2026-06-11
- **决策**: v0.7.0 实现 `ManagedExecutor` 直接包装 `java.util.concurrent.ThreadPoolExecutor`。
- **替代方案**:
  - (A) 继续扩展 `InMemoryAdjustableExecutorProbe`，增加任务执行模拟。拒绝原因：探针无法提供真实线程调度行为，闭环实验无法验证实际效果。
  - (B) 使用 TestNG/Custom executor。拒绝原因：引入非标准依赖，偏离项目目标。
- **影响**: 需要处理 `ThreadPoolExecutor` 的线程安全语义；所有调整路径必须通过安全门。
- **状态**: `decided`

### D-002: 不变更 queue capacity

- **日期**: 2026-06-11
- **决策**: `RuntimeSetting` 明确排除 queue capacity 的动态修改。`ManagedExecutor` 创建时接受 queue 配置，但运行时不可变。
- **理由**: `ThreadPoolExecutor` 不支持运行时替换 work queue；实现 queue resizing 需要停机/重建执行器，属于 queue resizing 设计范围（明确排除）。
- **状态**: `decided`

### D-003: 复用现有安全门，不设计新门

- **日期**: 2026-06-11
- **决策**: 调整路径复用 v0.5.0 的 `RuntimeAdjustmentSafetyGate` 和 `MutationReadinessGate`，不引入新的安全抽象。
- **理由**: 现有安全门已覆盖边界检查（min/max/step/readiness），且经过 IR/SR 闭环。新建安全门会增加不必要的复杂度。
- **状态**: `decided`

### D-004: ExecutorRegistry 为单实例进程内注册表

- **日期**: 2026-06-11
- **决策**: `ExecutorRegistry` 实现为进程内 `ConcurrentHashMap` 支持的注册表，不涉及分布式协调或外部存储。
- **理由**: 范围外约束禁止持久化和多节点协调。单进程注册表满足当前实验需求，架构可后续演进。
- **状态**: `decided`

### D-005: 不引入新的外部依赖

- **日期**: 2026-06-11
- **决策**: 所有实现仅依赖 Java 21 标准库和现有项目依赖（Spring Boot, JUnit 5, Mockito）。
- **理由**: `ThreadPoolExecutor`、`ConcurrentHashMap`、`ReentrantLock` 均在标准库中。无需引入 RxJava、Akka、Disruptor 等第三方并发库。
- **状态**: `decided`

### D-006: DeletionSafety 使用引用计数语义

- **日期**: 2026-06-11
- **决策**: `DeletionSafety` 判断标准为：当前是否有正在运行的实验引用该执行器。无引用则可删除。
- **理由**: 进程内注册表的生命周期与实验绑定；没有外部引用源。引用计数简单可验证，符合架构文档"删除安全"要求。
- **状态**: `decided`

### D-007: 首个闭环实验使用 steady workload

- **日期**: 2026-06-11
- **决策**: 闭环验证使用 `ScenarioProfile.STEADY`（固定速率），不使用 ramp/burst。
- **理由**: steady workload 提供最可预测的压力形态，调整前后差异最易量化。ramp/burst 留给后续验证。
- **状态**: `decided`
