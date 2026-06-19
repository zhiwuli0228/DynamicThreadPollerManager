# managed-executor-rejection-counting

## ADDED Requirements

### Requirement: ManagedExecutor SHALL expose rejected task count

The system MUST provide a `getRejectedTaskCount()` method on `ManagedExecutor` that returns the total number of tasks rejected by the executor's `RejectedExecutionHandler`.

#### Scenario: Rejection count increments when tasks exceed capacity
- **WHEN** more tasks are submitted than `queueCapacity + maxPoolSize` allows (triggering `AbortPolicy` rejections)
- **THEN** `getRejectedTaskCount()` returns a value greater than 0

#### Scenario: Rejection count is zero when no rejections occur
- **WHEN** an executor runs tasks within its capacity and no rejections are triggered
- **THEN** `getRejectedTaskCount()` returns 0

#### Scenario: Rejection counting is transparent to existing API
- **WHEN** `getRejectionPolicy()` is called
- **THEN** the returned handler is the original handler (not the counting wrapper)

### Requirement: ManagedExecutor rejection counting SHALL be thread-safe

The rejection counter MUST be threadsafe, correctly counting concurrent rejections from multiple threads.

#### Scenario: Concurrent rejections are all counted
- **WHEN** multiple threads simultaneously trigger rejections on the same executor
- **THEN** `getRejectedTaskCount()` equals the total number of rejections (no lost counts)
