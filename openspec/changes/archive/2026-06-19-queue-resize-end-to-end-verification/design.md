## Context

Change 2/2 of v0.9.0. Change 1 delivers QueueResizeCommand, ExecutorRebuildStrategy, QueueResizeSafetyGate, QueueResizeAdjustmentAdapter, and ResizeEvidence. This change verifies the integrated chain end-to-end using real executors and the existing ManagedExecutorScenarioRunner.

## Goals / Non-Goals

- Goals: QueueResizeEndToEndTest covering EXPAND, SHRINK, SAFETY_GATE_DENY paths; post-resize scenario re-run; ResizeEvidence completeness assertions; full regression check
- Non-Goals: Any new production code; any modification to ManagedExecutor, ExecutorRegistry, ManagedExecutorAdjustmentAdapter, or ManagedExecutorScenarioRunner

## Decisions

1. **Real ManagedExecutor only** — no mocked ThreadPoolExecutor in e2e tests. Follows v0.7.0 P6 lesson: mock-based executor tests hide termination/shutdown bugs.
2. **Reuse ManagedExecutorScenarioRunner** — run STEADY scenario before and after resize to verify executor health.
3. **Assert via ResizeEvidence fields** — not via log output or side-channel inspection.
4. **Single test class: QueueResizeEndToEndTest** — 6 test methods covering the full matrix.
5. **@AfterEach cleanup: release latches → shutdown → awaitTermination(10s) → shutdownNow** — follows v0.7.0 P6 mandated cleanup order.

## Risks / Trade-offs

- E2e tests use real threads — flakiness risk if timeouts too tight. Mitigated by using 30s default timeout from QueueResizeCommand.
- SHRINK safety gate test requires filling a queue with blocking tasks — cleanup must release latches before shutdown to avoid hangs.

## Dependencies

- Change 1/2: queue-resize-command-and-rebuild (all 5 components must be implemented)
- v0.7.0: ManagedExecutor, ExecutorRegistry, ManagedExecutorScenarioRunner
- No new external dependencies

## Migration Plan

No migration — test-only change. Run `mvn test` after both changes are implemented to confirm 433 + new tests pass.
