# v0.7.0 ManagedExecutor 受控执行器域设计

## Header

- Version name: `v0.7.0`
- Authoring date: `2026-06-11`
- Status: `CHANGE_2_ARCHIVED` — changes 1/3 and 2/3 archived; change 3/3 (`closed-loop-experiment-verification`) pending
- Current phase: `DESIGN_ONLY` — preparing change 3/3
- Authoritative branch: `claude_master`

## Change Status

| Change | Name | Status | Archive |
|---|---|---|---|
| 1/3 | establish-managed-executor-and-registry | ARCHIVED | `openspec/changes/archive/2026-06-12-establish-managed-executor-and-registry/` |
| 2/3 | bridge-adjustment-to-real-executor | ARCHIVED | `openspec/changes/archive/2026-06-12-bridge-adjustment-to-real-executor/` |
| 3/3 | closed-loop-experiment-verification | PENDING | — |

## Test Results

| Change | Tests | Result |
|---|---|---|
| 1/3 | 394 (380 existing + 14 new) | 0 failures |
| 2/3 | 409 (394 existing + 15 new) | 0 failures |

## Document Set

- `README.md`（本文档）
- `00-objectives-and-scope.md`
- `decision-log.md`
- `10-ir.md`
- `11-ir-review.md`
- `12-ir-review-disposition.md`
- `13-ir-closure-verification.md`
- `20-sr.md`
- `21-sr-review.md`
- `22-sr-review-disposition.md`
- `23-sr-closure-verification.md`

## Current Scope Boundary

Allowed now:
- Draft final v0.7.0 OpenSpec change (`closed-loop-experiment-verification`).
- Inspect archived artifacts and synchronized specs.

Not allowed now:
- Java source or test change (requires new `EXECUTION_AUTHORIZED`).
- Queue resizing, persistence, REST/API/UI, external dependencies.
