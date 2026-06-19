## 1. Adjustment Contracts

- [x] 1.1 创建 `experiment/adjustment` 包及对应测试包。
- [x] 1.2 添加 `ScaleAdjustmentCommand`，固定 deterministic `commandId`、run、decision timestamp、current/target pool size、reason、source decision reference 字段。
- [x] 1.3 添加 `ExecutorStateSnapshot`，固定 pool state 字段，并将 queue state 标记为只读观测。
- [x] 1.4 添加 `AdjustmentStatus`、`AdjustmentFailureCode`、`AdjustmentResult`、`AdjustmentEvidence` 等合同类型。
- [x] 1.5 增加 command/result/evidence 构造和 validation 单元测试。

## 2. Safety Gate

- [x] 2.1 添加 `RuntimeAdjustmentSafetyGate` 和默认实现。
- [x] 2.2 固定默认 safety config：`cooldownDecisionIntervals=2`、`maxAdjustmentsPerRun=5`、`blockImmediateOppositeDirection=true`、`allowReadyWithRisk=false`。
- [x] 2.3 增加 readiness `NOT_READY`、`READY_WITH_RISK` 未接受风险、cooldown、立即反向调整、单 run 超限和 no-op 测试。
- [x] 2.4 确保 safety gate 不执行 mutation，只返回 allow/reject/no-op decision。

## 3. Adapter and Probe

- [x] 3.1 添加 `ExecutorAdjustmentAdapter` contract。
- [x] 3.2 添加 in-memory adjustable executor probe，用于确定性测试 pool size adjustment。
- [x] 3.3 实现 adapter apply 语义，覆盖 `APPLIED`、`REJECTED`、`NO_OP`、`FAILED`。
- [x] 3.4 增加 adapter 测试，证明 before/requested/applied/after state 和 reason/failure code 正确。
- [x] 3.5 确认第一批实现不实例化或集成生产 `ThreadPoolExecutor`。

## 4. Evidence and Boundary Verification

- [x] 4.1 实现 runtime `AdjustmentEvidence` 创建路径，固定 `evidenceType=runtime_adjustment`。
- [x] 4.2 增加 evidence 测试，证明 source decision reference、status、before/after state 和 failure/rejection reason 可追踪。
- [x] 4.3 增加 boundary isolation test，确认 policy/analysis/scenario 包不反向依赖 adjustment。
- [x] 4.4 增加 queue resizing exclusion test，确认没有 `QueueCapacityController` 或 queue capacity mutation API。
- [x] 4.5 确认 `pom.xml` 无新依赖。

## 5. Verification and Receipts

- [x] 5.1 运行 `openspec.cmd validate --all --json`。
- [x] 5.2 运行 `.\mvnw.cmd test`。
- [x] 5.3 完成实现后生成 `apply.md`，记录实现范围、测试命令和偏差。
- [x] 5.4 完成 verify 后生成 `verify.md`，逐项映射 spec -> implementation -> tests -> evidence。
- [x] 5.5 仅在对应实现、测试、apply、verify 完成后更新本文件复选框。
