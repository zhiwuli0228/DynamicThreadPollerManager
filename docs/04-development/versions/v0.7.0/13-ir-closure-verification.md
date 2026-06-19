# v0.7.0 IR Closure Verification

## Header

- Document type: IR closure verification
- Version name: `v0.7.0`
- Verified artifacts:
  - `docs/04-development/versions/v0.7.0/10-ir.md`（修正后）
  - `docs/04-development/versions/v0.7.0/11-ir-review.md`
  - `docs/04-development/versions/v0.7.0/12-ir-review-disposition.md`
- Verification date: `2026-06-12`
- Verification basis: `docs/02-harness/managed-change-standard.md` 第 2 节 IR 出口条件

## 1. P0 阻断项闭合验证

| ID | 描述 | 处置 | 修正位置 | 验证 |
| --- | --- | --- | --- | --- |
| F01 | `ManagedExecutor` 缺少 shutdown/termination | FIXED | `10-ir.md` IR-v0.7-001 新增 4 条生命周期条款 | PASS — `shutdown()`, `isShutdown()`, `isTerminated()`, `awaitTermination()` 均已加入 IR |
| F02 | `remove()` 与 `shutdown()` 解耦 | FIXED | `10-ir.md` IR-v0.7-002 新增 2 条耦合条款 | PASS — `remove()` 不自动 shutdown，`DeletionSafety` 检查 `isTerminated()` 已加入 IR |

## 2. P1 关键项闭合验证

| ID | 描述 | 处置 | 验证 |
| --- | --- | --- | --- |
| F03 | 调整后快照语义 | DEFERRED_TO_SR | PASS — SR 触发条件已记录：定义采集时机、允许偏差范围、任务触发条件 |
| F04 | 可复现性声明过强 | FIXED | PASS — `10-ir.md` IR-v0.7-008 已修正为"策略决策路径可复现"，timing-dependent 值允许波动 |
| F05 | 与 BaselineWorkloadExecutor 交互 | DEFERRED_TO_SR | PASS — SR 触发条件已记录：定义集成角色 |
| F06 | 与 metrics 管道集成 | DEFERRED_TO_SR | PASS — SR 触发条件已记录：定义 SnapshotAssembler 关系 |
| F07 | 引用计数并发安全 | FIXED | PASS — `10-ir.md` IR-v0.7-004 已加入 `AtomicInteger` 原子性要求 |

## 3. P2 残余风险验证

| ID | 描述 | 处置 | 验证 |
| --- | --- | --- | --- |
| F08 | 默认值未指定 | ACCEPTED | PASS — SR 备忘已记录 |
| F09 | 调整失败回滚 | ACCEPTED | PASS — SR 备忘已记录 |
| F10 | Adapter 选择策略 | ACCEPTED | PASS — SR 备忘已记录 |
| F11 | 快照字段兼容性 | ACCEPTED | PASS — SR 备忘已记录 |

## 4. IR 版本一致性检查

| 检查项 | 结果 |
| --- | --- |
| `10-ir.md` 状态字段仍为 `DRAFT`，未声称 `closed` | PASS |
| `10-ir.md` 结论未声称授权 SR 或实现 | PASS |
| IR 条目优先级分布：P0×7（001-003, 005-009）, P1×3（004, 010, 011 新增生命周期） | PASS — 合理性确认 |
| 未在 IR 中声称"已实现" | PASS |
| 验收条件全部标记为"草案" | PASS |

## 5. 出口条件检查

对照 `managed-change-standard.md` 第 2 节出口条件：

| 条件 | 状态 |
| --- | --- |
| 独立需求评审完成 | PASS — `11-ir-review.md` 由独立 reviewer 完成 |
| 所有 P0 findings 已处置并通过闭环验证 | PASS — 2/2 FIXED and verified |
| 所有 P1 findings 已处置并通过闭环验证 | PASS — 2 FIXED, 3 DEFERRED_TO_SR with triggers |
| 残余风险已记录 | PASS — 4 P2 items recorded as SR memos |
| 明确允许进入 SR 功能设计 | PASS — 本验证确认 |

## 6. IR 修正后全文复核

已确认 `10-ir.md` 修正后的一致性：

- IR-v0.7-001 新增 lifecycle 4 条（F01 fix）与术语表中 `ManagedExecutor` 定义兼容。
- IR-v0.7-002 新增 remove/shutdown 耦合 2 条（F02 fix）与 `DeletionSafety` 术语定义一致，与 F01 的 shutdown 条款无矛盾。
- IR-v0.7-004 新增原子性要求（F07 fix）与 `DeletionSafety` 术语"基于引用计数"兼容。
- IR-v0.7-008 缩小可复现性定义（F04 fix）与风险表第 1 行 P1 风险描述一致。

无新增内部矛盾。

## 7. Closure Verification 结论

**所有 P0/P1 findings 均已处置并验证。IR 草案通过 closure verification。**

- P0: 2/2 FIXED and verified.
- P1: 2/5 FIXED and verified, 3/5 DEFERRED_TO_SR with explicit triggers.
- P2: 4/4 ACCEPTED_WITH_RECORD.

下一步：更新 `docs/00-project/current-state.md`，明确授权进入 `v0.7.0` SR 功能设计阶段。
