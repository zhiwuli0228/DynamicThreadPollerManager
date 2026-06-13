# v0.9.0 SR Review Disposition

## Header

- Document type: SR review disposition
- Version name: `v0.9.0`
- Reviewed artifact: `docs/04-development/versions/v0.9.0/21-sr-review.md`
- Disposition date: `2026-06-13`
- Disposition by: SR author, responding to independent review

## Disposition Summary

| Total Findings | FIX | DEFER_TO_IMPLEMENTATION | ACCEPT | CLOSED |
|---|---|---|---|---|
| 4 | 2 | 1 | 0 | 1 |

## Per-Finding Disposition

### F01 [P1] AdjustmentResult 泛型类型兼容性 → **FIX**

**处置**: 确认现有 API 并适配。

**代码验证**: 读取 `AdjustmentResult.java`：
```java
public record AdjustmentResult<T>(
    String executorId,
    boolean success,
    String failureCode,
    String failureReason,
    T evidence
) {
    public static <T> AdjustmentResult<T> success(String executorId, T evidence) {
        return new AdjustmentResult<>(executorId, true, null, null, evidence);
    }

    public static <T> AdjustmentResult<T> failed(
            String failureCode, String failureReason, T evidence) {
        return new AdjustmentResult<>(null, false,
                failureCode, failureReason, evidence);
    }
}
```

现有 API 是**泛型 `AdjustmentResult<T>`**，`evidence` 字段类型为泛型 `T`。`ResizeEvidence` 可直接作为 `T` 使用，无需实现任何接口。

**SR 更新**: SR §4.4 的伪代码中 `AdjustmentResult` 调用已与现有 API 一致（`AdjustmentResult.success(executorId, evidence)` / `AdjustmentResult.failed(...)`）。无需修改 SR。

---

### F02 [P1] 幂等保护缺失 → **FIX**

**处置**: 在 `QueueResizeAdjustmentAdapter` 中增加 `ConcurrentHashMap` 幂等保护。

**SR 更新**: 在 SR §4.4 的 `QueueResizeAdjustmentAdapter` 中增加：

```java
private final ConcurrentHashMap<String, AtomicBoolean> resizeInProgress
        = new ConcurrentHashMap<>();

public AdjustmentResult apply(String executorId, QueueResizeCommand command) {
    // Idempotency guard
    AtomicBoolean existing = resizeInProgress.putIfAbsent(
            executorId, new AtomicBoolean(true));
    if (existing != null) {
        return AdjustmentResult.failed(
            "RESIZE_IN_PROGRESS",
            "resize already in progress for executor " + executorId,
            null);
    }
    try {
        // ... existing apply logic ...
    } finally {
        resizeInProgress.remove(executorId);
    }
}
```

此保护确保同一 executor 的并发 resize 请求被串行化（第二个返回 RESIZE_IN_PROGRESS）。

---

### F03 [P2] RebuildResult 与 ResizeEvidence 耦合 → **DEFER_TO_IMPLEMENTATION**

**处置**: DEFER_TO_IMPLEMENTATION。两个类型的语义边界清晰（RebuildResult = strategy→adapter 内部传递，ResizeEvidence = 公开 evidence record）。实现阶段如果发现维护负担过大，可以合并。

**理由**: SR 阶段合并两个类型是过早优化——实现阶段会自然发现是否有真正的维护问题。当前分离是合理的抽象层次。

**SR 更新**: 无（SR 记录此决策，实现阶段自主选择）。

---

### F04 [P2] ControlGate readiness 参数 null 传参 → **FIX**

**处置**: 使用 `ReadinessSummary.NOT_EVALUATED` 占位值替代 null。

**SR 更新**: SR §4.3 的 `safetyGate.evaluate()` 调用改为：

```java
SafetyGateResult gateResult = safetyGate.evaluate(
        command, currentState, ReadinessSummary.NOT_EVALUATED);
```

或者在 `QueueResizeSafetyGate.evaluate()` 内部忽略 readiness 参数（已在 SR §4.3 伪代码中记录）。

---

## 处置后状态

| Finding | 原始级别 | 处置 | 状态 |
|---|---|---|---|
| F01 | P1 | FIX (确认泛型 API 兼容) | CLOSED |
| F02 | P1 | FIX (adapter 增加幂等保护) | CLOSED |
| F03 | P2 | DEFER_TO_IMPLEMENTATION | CLOSED |
| F04 | P2 | FIX (NOT_EVALUATED 占位值) | CLOSED |

## 出口条件

All P1 findings disposed. SR can proceed to closure verification.
