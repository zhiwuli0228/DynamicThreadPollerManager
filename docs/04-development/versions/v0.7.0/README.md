# v0.7.0 ManagedExecutor 受控执行器域设计

## Header

- Version name: `v0.7.0`
- Authoring date: `2026-06-11`
- Status: `IMPLEMENTED` — all 3 changes archived; 412 tests pass
- Authoritative branch: `claude_master`

## Purpose

v0.7.0 桥接了 v0.1.0-v0.6.0 的实验基础设施到真实 `ThreadPoolExecutor`，完成了首次闭环实验验证。

## Change Summary

| # | Change | Status | Tests |
|---|---|---|---|
| 1/3 | `establish-managed-executor-and-registry` | ARCHIVED | 394 pass |
| 2/3 | `bridge-adjustment-to-real-executor` | ARCHIVED | 409 pass |
| 3/3 | `closed-loop-experiment-verification` | ARCHIVED | 412 pass |

## Delivered Capabilities

| Capability | Description |
|---|---|
| `ManagedExecutor` | ThreadPoolExecutor wrapper with controlled parameter adjustment |
| `ExecutorRegistry` | Thread-safe named executor registry with deletion safety |
| `RuntimeSetting` | Parameter classification (adjustable/non-adjustable) and bounds |
| `DeletionSafety` | Atomic reference counting for safe executor removal |
| `ManagedExecutorAdjustmentAdapter` | Bridges ScaleAdjustmentCommand to real ManagedExecutor |
| `ExecutorStateSnapshot` extension | 5 new nullable fields from real ThreadPoolExecutor |
| Closed-loop experiment | End-to-end test proving full pipeline on real executor |

## Test Results

- **Final**: 412 tests, 0 failures, 0 errors
- **New tests**: ManagedExecutorTest (16), ExecutorRegistryTest (9), AtomicDeletionSafetyTest (9), ParameterBoundsTest (10), ManagedExecutorAdjustmentAdapterTest (15), ClosedLoopExperimentTest (3)
- **Non-regression**: All existing InMemoryAdjustableExecutorProbe tests pass unmodified

## Document Set

- `README.md`
- `00-objectives-and-scope.md`
- `decision-log.md`
- `10-ir.md` → `11-ir-review.md` → `12-ir-review-disposition.md` → `13-ir-closure-verification.md`
- `20-sr.md` → `21-sr-review.md` → `22-sr-review-disposition.md` → `23-sr-closure-verification.md`
- `15-experiment-data-acquisition-plan.md` — 真实 ManagedExecutor 数据获取方案（新增）

## Next Step

Data acquisition on real ManagedExecutor per `15-experiment-data-acquisition-plan.md`.
