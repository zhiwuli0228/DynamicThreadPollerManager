# v0.10.0 SR Review Disposition

## Header

- Document type: SR review disposition
- Version name: `v0.10.0`
- Reviewed artifact: `docs/04-development/versions/v0.10.0/21-sr-review.md`
- Disposition date: `2026-06-13`
- Disposition by: SR author, responding to independent review

## Disposition Summary

| Total Findings | FIX | ACCEPT | DEFER | CLOSED |
|---|---|---|---|---|
| 5 | 3 | 1 | 1 | 0 |

## Per-Finding Disposition

### F01 [P1] ManagedExecutor 构造器中 rejectionPolicy 字段赋值需同步删除 → **FIX**

**处置**: SR §4.2 补充标注构造器变更。

**具体修改**: SR §4.2 伪代码中 `getRejectionPolicy()` / `setRejectionPolicy()` 变更说明下方添加：

```
构造器变更（7 参数）:
- 删除行：this.rejectionPolicy = rejectionHandler;
- 理由：rejectionPolicy 字段已删除，rejectionHandler 已通过
  this.executor = new ThreadPoolExecutor(..., rejectionHandler) 传入 TPE，
  getRejectionPolicy() 直接从 TPE 读取，无需缓存
```

5 参数构造器不引用 `rejectionPolicy` 字段（委托给 7 参数构造器），无需修改。

---

### F02 [P1] Safety gate 三参数签名与 QueueResizeSafetyGate 两参数不一致 → **FIX**

**处置**: SR §4.3 补充签名差异的设计选择记录。

**SR 更新**: §4.3 设计决策补充：

```
- evaluate() 三参数签名 (command, executor, executorId):
  QueueResizeSafetyGate.evaluate() 是两参数 — 不需要 executorId 因为
  resize safety gate 不检查并发 resize (幂等保护在 adapter 层)。
  RejectionPolicySafetyGate 需要 executorId 用于 Predicate 检查并发 resize。
  ManagedExecutor 不携带 registry ID (ID 是 registry 的 key)，
  因此 executorId 必须作为独立参数传入。
  两个 safety gate 签名不一致是有意的 — 它们服务于不同的安全检查需求。
```

---

### F03 [P1] Evidence/Result 分层职责需明确 → **FIX**

**处置**: SR §4.5 + §4.6 明确分层职责。

**SR 更新**: §4.6 PolicyReplacementEvidence 补充设计决策：

```
- Evidence/Result 分层:
  - PolicyReplacementEvidence: 操作事实记录。success 是二元事实 (操作是否成功执行)，
    reason 是上下文 (成功原因=替换原因，失败原因=错误描述)。
  - PolicyReplacementResult: 调用方视角。failureCode 区分失败类型
    (SAFETY_GATE_DENIED / EXECUTOR_NOT_FOUND / POLICY_SET_FAILED)。
  - 分层原则: evidence 记录 "发生了什么"，result 告诉调用方 "该做什么"。
```

---

### F04 [P2] Adapter 幂等保护差异说明 → **ACCEPT**

**处置**: ACCEPT。SR §4.4 已包含设计决策说明并发语义，补充幂等保护差异对比。

**SR 更新**: §4.4 设计决策补充：

```
- 无需幂等保护: 与 QueueResizeAdjustmentAdapter 不同 (通过 resizeInProgress
  ConcurrentHashMap 防止并发 resize)，policy adapter 不需要幂等保护。
  原因: TPE.setRejectedExecutionHandler() 是原子 volatile 写,
  并发 policy 替换是 last-write-wins, 不产生中间状态。
  Queue resize 需要幂等保护因为 rebuild 过程涉及 shutdown → drain →
  awaitTermination → create → register 多步骤, 并发 rebuild 会导致状态混乱。
```

---

### F05 [P2] DiscardOldestPolicy 断言策略 → **DEFER_TO_IMPLEMENTATION**

**处置**: DEFER_TO_IMPLEMENTATION。SR 策略方向正确，实现阶段使用可区分的任务标识完成。

**推荐实现方式**: 使用带名称字段的 `Runnable` 实现（如 `NamedTask implements Runnable { String name; CountDownLatch latch; }`），通过 `name` 区分 Task-A/Task-B/Task-C，验证 Task-C 被执行而 Task-A 未被执行的模式。

---

## 修改后的 SR 更新计划

| SR 位置 | 变更 |
|---|---|
| §4.2 ManagedExecutor | 补充构造器变更标注（删除 rejectionPolicy 赋值行） |
| §4.3 RejectionPolicySafetyGate | 补充签名差异设计选择记录（三参数 vs 两参数） |
| §4.4 RejectionPolicyAdjustmentAdapter | 补充幂等保护差异对比说明 |
| §4.6 PolicyReplacementEvidence | 补充 Evidence/Result 分层设计决策 |

## 处置后状态

| Finding | 原始级别 | 处置 | 状态 |
|---|---|---|---|
| F01 | P1 | FIX (补充构造器变更标注) | CLOSED |
| F02 | P1 | FIX (补充签名差异理由) | CLOSED |
| F03 | P1 | FIX (补充分层职责说明) | CLOSED |
| F04 | P2 | ACCEPT (补充幂等保护差异对比) | CLOSED |
| F05 | P2 | DEFER_TO_IMPLEMENTATION (断言策略) | CLOSED |

## 出口条件

所有 P0/P1 findings 已处置。可进入 SR closure verification。
