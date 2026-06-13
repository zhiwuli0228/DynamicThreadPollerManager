# v0.9.0 Queue Capacity Resizing

## Header

- Version name: `v0.9.0`
- Authoring date: `2026-06-13`
- Status: `DRAFT`
- Current phase: `SR_CLOSURE_VERIFIED`
- Requirement theme: runtime queue capacity resizing, executor rebuild strategy, safety coverage

## Purpose

v0.9.0 addresses the most persistently deferred architectural gap in the project: **runtime queue capacity resizing**. Since `ThreadPoolExecutor` does not support work-queue replacement after construction, v0.9.0 must introduce an executor rebuild strategy — decommission the old executor, commission a new one with the resized queue — with full safety gate, evidence recording, and rollback coverage.

## Scope Summary

| # | Change (candidate) | Scope |
|---|---|---|
| 1/? | `queue-resize-command-and-safety-gate` | QueueResizeCommand, QueueResizeSafetyGate, ResizeDecision |
| 2/? | `executor-rebuild-strategy` | ExecutorRebuildStrategy (decommission → commission cycle), task drain, thread handover |
| 3/? | `queue-resize-evidence-and-verification` | ResizeEvidence, end-to-end resize + re-acquire test, G10 resize gate |

## Verification Target

- `mvn test`: all existing 433 tests pass (zero regression)
- New tests: command validation, safety gate evaluation (permit/deny), rebuild cycle, task drain, end-to-end resize + re-run

## Key Decisions

See `decision-log.md`.

- D1: Executor rebuild vs. queue-swapping hack
- D2: Blocking vs. non-blocking resize
- D3: Safety gate criteria for queue resize
- D4: Evidence recording for resize operations
- D5: Change decomposition strategy

## Predecessor

- v0.8.0 ManagedExecutor data acquisition (IMPLEMENTED)
- v0.7.0 ManagedExecutor domain (IMPLEMENTED)
- v0.5.0 IR decision to defer queue resizing

## Document Set

- `README.md`
- `00-objectives-and-scope.md`
- `decision-log.md`
- `10-ir.md` — requirements analysis (6 IR entries)
- `11-ir-review.md` — independent IR review (8 findings)
- `12-ir-review-disposition.md` — disposition (4 FIX + 4 DEFER_TO_SR)
- `13-ir-closure-verification.md` — IR closure verified
- `20-sr.md` — functional design (5 core components, 2 candidate changes)
- `21-sr-review.md` — independent SR review (4 findings)
- `22-sr-review-disposition.md` — disposition (2 FIX + 1 DEFER + 1 FIX)
- `23-sr-closure-verification.md` — SR closure verified

## Next Step

`READY_FOR_CHANGE_DECOMPOSITION` — create OpenSpec changes, then EXECUTION_AUTHORIZED.
