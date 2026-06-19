## Design Summary

Queue capacity resizing capability for DynamicThreadPollerManager v0.9.0. The core challenge: `ThreadPoolExecutor` does not support runtime work-queue replacement — capacity changes require executor decommission/recommission via `ExecutorRebuildStrategy`.

## Alternatives Considered

### Alternative A: Reflection-based Queue Swapping
- **Approach**: Use Java reflection to access and replace `ThreadPoolExecutor.workQueue` final field
- **Pros**: No executor restart needed, minimal code, fast
- **Cons**: JDK version-dependent, breaks module encapsulation (--add-opens), thread-unsafe during concurrent task submission, not maintainable
- **Why not chosen**: Violates "no reflection hack" constraint; fragile across JDK versions

### Alternative B: Custom ThreadPoolExecutor Subclass
- **Approach**: Extend `ThreadPoolExecutor` to expose a `setWorkQueue()` method
- **Pros**: Type-safe, no reflection, integrated into TPE lifecycle
- **Cons**: Breaks TPE contract (queue is supposed to be immutable after construction), misuse risk, doesn't solve task drain/transfer problem
- **Why not chosen**: Same fundamental problem — TPE internals assume queue stability; subclass adds complexity without solving drain-and-replay

### Alternative C: Executor Rebuild (Chosen)
- **Approach**: Decommission old executor (shutdown → drain → awaitTermination), commission new executor with resized queue (new TPE → wrap → re-register)
- **Pros**: Explicit, auditable, uses existing ExecutorRegistry infrastructure, each step recordable as evidence
- **Cons**: Blocking operation (seconds), brief unavailability during rebuild, drained tasks need replay handling
- **Why chosen**: Only safe approach; all surrounding infrastructure (ExecutorRegistry, DeletionSafety, ExecutorStateSnapshot) already exists

## Agreed Approach

Executor Rebuild Strategy with safety gate and evidence recording. Decommission phase: `shutdown → drainTo → awaitTermination`. Commission phase: `new ThreadPoolExecutor(resizedQueue) → wrap as ManagedExecutor → re-register with same executorId`. New `QueueResizeAdjustmentAdapter` (separate from existing `ManagedExecutorAdjustmentAdapter`). All operations go through `QueueResizeSafetyGate` for safety assessment.

## Key Decisions

1. Executor rebuild, not reflection — explicit lifecycle, auditable, no JDK internals
2. New adapter, not modified existing — follows v0.8.0 D1 "新建不修改" principle
3. Synchronous blocking resize — consistent with ScaleAdjustmentCommand semantics
4. SHRINK → drain-and-discard; EXPAND → drain-and-replay
5. ResizeEvidence via AdjustmentResult, not EvidenceRecorder

## Open Questions

- None remaining — all questions resolved in SR design phase
