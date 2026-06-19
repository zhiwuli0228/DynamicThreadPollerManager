## Purpose

Define the behavior of `QueueResizeCommand`, `ExecutorRebuildStrategy`, `QueueResizeSafetyGate`, `QueueResizeAdjustmentAdapter`, and `ResizeEvidence` for runtime queue capacity resizing via executor rebuild.

## Requirements

### Requirement: QueueResizeCommand invariants and creation
The system MUST provide an immutable `QueueResizeCommand` record that validates creation parameters and detects no-op resize requests.

#### Scenario: Valid command creation
- **WHEN** `QueueResizeCommand` is created with targetQueueCapacity=20
- **THEN** the record MUST store the value and return it via `targetQueueCapacity()`

#### Scenario: Invalid capacity is rejected
- **WHEN** `QueueResizeCommand` is created with targetQueueCapacity=0
- **THEN** an `IllegalArgumentException` MUST be thrown

#### Scenario: Negative capacity is rejected
- **WHEN** `QueueResizeCommand` is created with targetQueueCapacity=-1
- **THEN** an `IllegalArgumentException` MUST be thrown

#### Scenario: Same capacity returns empty
- **WHEN** `fromCurrent(10, 10, "no-op")` is called
- **THEN** `Optional.empty()` MUST be returned

#### Scenario: Direction EXPAND
- **WHEN** `direction(10)` is called on a command with targetQueueCapacity=20
- **THEN** `Direction.EXPAND` MUST be returned

#### Scenario: Direction SHRINK
- **WHEN** `direction(20)` is called on a command with targetQueueCapacity=10
- **THEN** `Direction.SHRINK` MUST be returned

---

### Requirement: ExecutorRebuildStrategy decommission and commission
The system MUST provide an `ExecutorRebuildStrategy` that safely replaces an executor's work queue by decommissioning the old executor and commissioning a new one with the resized queue.

#### Scenario: EXPAND rebuild succeeds
- **WHEN** `rebuild()` is called with an EXPAND command (queue 10→20) on a running executor
- **THEN** the old executor MUST be terminated, a new executor with queueCapacity=20 MUST be registered with the same executorId, and `RebuildResult.success()` MUST be true

#### Scenario: SHRINK rebuild succeeds
- **WHEN** `rebuild()` is called with a SHRINK command (queue 20→5) on a running executor
- **THEN** the old executor MUST be terminated, a new executor with queueCapacity=5 MUST be registered, and drained tasks MUST NOT be replayed

#### Scenario: Decommission order prevents race
- **WHEN** `rebuild()` is called, the decommission phase MUST execute in order: `shutdown()` → `drainTo()` → `awaitTermination()`

#### Scenario: Thread configuration preserved
- **WHEN** `rebuild()` is called, the new executor MUST retain the old executor's corePoolSize, maximumPoolSize, keepAliveTime, and threadFactory

#### Scenario: Commission failure returns failure result
- **WHEN** new ThreadPoolExecutor creation fails during commission
- **THEN** `RebuildResult.success()` MUST be false and `RebuildResult.errorMessage()` MUST be non-null

#### Scenario: AwaitTermination timeout forces shutdownNow
- **WHEN** `awaitTermination()` times out during decommission
- **THEN** `shutdownNow()` MUST be called and termination MUST be re-attempted with a 5-second timeout

---

### Requirement: QueueResizeSafetyGate evaluates resize safety
The system MUST provide a `QueueResizeSafetyGate` that checks safety conditions before allowing a queue resize operation.

#### Scenario: Valid resize is permitted
- **WHEN** `evaluate()` is called with a valid command and a RUNNING executor
- **THEN** `SafetyGateResult.permitted()` MUST be true

#### Scenario: Shrink denied when queue depth exceeds new capacity
- **WHEN** `evaluate()` is called with a SHRINK command (20→5) and the current queue depth is 8
- **THEN** `SafetyGateResult.permitted()` MUST be false with reason containing "queue depth"

#### Scenario: Non-running executor denied
- **WHEN** `evaluate()` is called on a TERMINATED executor
- **THEN** `SafetyGateResult.permitted()` MUST be false with reason containing "RUNNING"

#### Scenario: Same capacity denied
- **WHEN** `evaluate()` is called with targetQueueCapacity equal to current queue capacity
- **THEN** `SafetyGateResult.permitted()` MUST be false

---

### Requirement: QueueResizeAdjustmentAdapter integrates resize into adjustment pipeline
The system MUST provide a `QueueResizeAdjustmentAdapter` that receives `QueueResizeCommand`, evaluates safety, and executes rebuild, returning `AdjustmentResult<ResizeEvidence>`.

#### Scenario: Successful resize via adapter
- **WHEN** `apply(executorId, command)` is called with a valid EXPAND command
- **THEN** `AdjustmentResult.success()` MUST be true and `evidence()` MUST return non-null ResizeEvidence

#### Scenario: Safety gate denial via adapter
- **WHEN** `apply()` is called but safety gate denies
- **THEN** `AdjustmentResult.success()` MUST be false with failureCode="SAFETY_GATE_DENIED"

#### Scenario: Executor not found
- **WHEN** `apply()` is called with an executorId not in registry
- **THEN** `AdjustmentResult.success()` MUST be false with failureCode="EXECUTOR_NOT_FOUND"

#### Scenario: Idempotency guard prevents concurrent resize
- **WHEN** two concurrent `apply()` calls target the same executorId
- **THEN** the second call MUST return `AdjustmentResult.failed("RESIZE_IN_PROGRESS", ...)`

#### Scenario: Existing ScaleAdjustmentCommand unaffected
- **WHEN** `ManagedExecutorAdjustmentAdapter.apply(ScaleAdjustmentCommand)` is called
- **THEN** it MUST continue to work without any change in behavior

---

### Requirement: ResizeEvidence records resize operation details
The system MUST record complete evidence for every resize operation.

#### Scenario: Successful resize evidence
- **WHEN** a resize completes successfully
- **THEN** `ResizeEvidence` MUST contain: beforeState (non-null), afterState (non-null), rebuildDurationMs > 0, success=true, direction, oldQueueCapacity, newQueueCapacity, errorMessage=null

#### Scenario: Failed resize evidence
- **WHEN** a resize fails
- **THEN** `ResizeEvidence` MUST contain: beforeState (non-null), afterState=null, success=false, errorMessage (non-null)

#### Scenario: Evidence carried via AdjustmentResult
- **WHEN** `AdjustmentResult.evidence()` is called after a resize
- **THEN** the returned evidence MUST be a `ResizeEvidence` instance with correct field values
