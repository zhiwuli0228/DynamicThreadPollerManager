## 1. QueueResizeCommand

- [ ] 1.1 Create `QueueResizeCommand` record with fields: targetQueueCapacity (int), resizeReason (String), timeoutMs (long)
- [ ] 1.2 Validate targetQueueCapacity > 0 in compact constructor, timeoutMs >= 0
- [ ] 1.3 Provide `fromCurrent(int currentCapacity, int newCapacity, String reason)` factory returning `Optional<QueueResizeCommand>`
- [ ] 1.4 Provide `direction(int currentCapacity)` returning Direction enum (EXPAND/SHRINK/NO_CHANGE)
- [ ] 1.5 Write `QueueResizeCommandTest` covering: valid creation, invalid capacity (0, -1), fromCurrent same-capacity returns empty, direction EXPAND/SHRINK

## 2. ExecutorRebuildStrategy

- [ ] 2.1 Create `RebuildResult` record with fields: success, beforeState, afterState, rebuildDurationMs, drainedTaskCount, rejectedTaskCount, direction, oldQueueCapacity, newQueueCapacity, errorMessage
- [ ] 2.2 Create `ExecutorRebuildStrategy` class with constructor(ExecutorRegistry, Supplier<Instant> clock)
- [ ] 2.3 Implement decommission phase: shutdown → drainTo → awaitTermination (with timeout) → shutdownNow if timeout
- [ ] 2.4 Implement commission phase: new ThreadPoolExecutor(resizedQueue, maintaining core/max/keepAlive/threadFactory) → wrap ManagedExecutor → re-register with same executorId
- [ ] 2.5 EXPAND: drain-and-replay (submit drained tasks to new executor, counting rejected)
- [ ] 2.6 SHRINK: drain-and-discard (drained tasks not replayed)
- [ ] 2.7 Snapshot beforeState before decommission, afterState after commission
- [ ] 2.8 Commission failure: return RebuildResult with success=false, errorMessage, afterState=null
- [ ] 2.9 Write `ExecutorRebuildStrategyTest` covering: EXPAND rebuild, SHRINK rebuild, thread config preserved, old executor terminated, new executor in registry, commission failure

## 3. QueueResizeSafetyGate

- [ ] 3.1 Create `QueueResizeSafetyGate` implementing `ControlGate<QueueResizeCommand>`
- [ ] 3.2 Implement evaluate(): check executor RUNNING state, capacity change, SHRINK safety (queue depth ≤ new capacity)
- [ ] 3.3 Return PERMIT when all checks pass, DENY with reason otherwise
- [ ] 3.4 Write `QueueResizeSafetyGateTest` covering: PERMIT valid resize, DENY non-RUNNING, DENY same capacity, DENY SHRINK with queue depth > new capacity

## 4. QueueResizeAdjustmentAdapter

- [ ] 4.1 Create `QueueResizeAdjustmentAdapter` with constructor(ExecutorRegistry, QueueResizeSafetyGate, ExecutorRebuildStrategy)
- [ ] 4.2 Implement apply(executorId, command): find executor → snapshot → safety gate → rebuild → build result
- [ ] 4.3 Handle EXECUTOR_NOT_FOUND
- [ ] 4.4 Handle SAFETY_GATE_DENIED
- [ ] 4.5 Handle REBUILD_FAILED
- [ ] 4.6 Add ConcurrentHashMap-based idempotency guard (RESIZE_IN_PROGRESS)
- [ ] 4.7 Write `QueueResizeAdjustmentAdapterTest` covering: successful apply, executor not found, safety gate denied, rebuild failed, idempotency guard

## 5. ResizeEvidence

- [ ] 5.1 Create `ResizeEvidence` record with fields: success, beforeState, afterState, rebuildDurationMs, drainedTaskCount, rejectedTaskCount, direction, oldQueueCapacity, newQueueCapacity, errorMessage
- [ ] 5.2 Provide `from(RebuildResult)` factory method
- [ ] 5.3 Write `ResizeEvidenceTest` covering: success evidence (afterState non-null), failure evidence (afterState null), field mapping from RebuildResult

## 6. Test Suite Verification

- [ ] 6.1 Run `mvn test` — all existing 433 tests pass (0 failures)
- [ ] 6.2 Run new tests — all pass (0 failures)
- [ ] 6.3 Verify ManagedExecutorAdjustmentAdapter tests unchanged and pass
- [ ] 6.4 Verify ScaleAdjustmentCommand tests unchanged and pass
