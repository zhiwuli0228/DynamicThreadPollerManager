## Summary

Implement end-to-end verification for the queue resize capability delivered in change 1/2. This change adds `QueueResizeEndToEndTest` — a single test class with 6 end-to-end scenarios covering EXPAND, SHRINK, SAFETY_GATE_DENY, post-resize scenario re-run, ResizeEvidence completeness, and no-op detection. No production code changes.

## Motivation

Change 1/2 delivers the resize pipeline (command → safety gate → rebuild → evidence). But unit and integration tests in change 1 test components in isolation. The SR requires end-to-end proof that the full chain works: real executors, real queues, real scenario runner, and real evidence recording. This change closes the v0.9.0 verification loop.

## Changes

### New Test Types
- **QueueResizeEndToEndTest**: 6 test methods
  1. `expandResizeAndRerun` — EXPAND 10→20, pre/post STEADY scenario, verify queue capacity and thread config preserved
  2. `shrinkResize` — SHRINK 20→5, verify new queue capacity
  3. `safetyGateDenyShrinkWithQueueDepth` — fill queue to depth 8, attempt SHRINK to 5, verify DENIED
  4. `oldExecutorTerminatedAfterRebuild` — verify old executor is terminated, new executor is in registry
  5. `resizeEvidenceComplete` — verify all ResizeEvidence fields are populated correctly
  6. `noOpWhenCapacityUnchanged` — fromCurrent() returns empty for same capacity

### Modified Types
- None. Test-only change.

### Test Requirements
- All tests use real `ManagedExecutor` (no mocked `ThreadPoolExecutor`)
- `@AfterEach`: release latches → shutdown → awaitTermination(10s) → shutdownNow
- Reuse `ManagedExecutorScenarioRunner` for pre/post-resize health checks
- 433 existing tests must continue to pass (zero regression)

## Impact

- **Existing tests**: 433 tests continue to pass
- **New tests**: 6 end-to-end tests
- **Packages touched**: experiment.executor (test only)
- **No production code modified**
