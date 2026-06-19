# establish-managed-executor-and-registry Specification

## Purpose

The `establish-managed-executor-and-registry` capability bridges the experiment infrastructure (v0.1.0–v0.6.0) to a real `ThreadPoolExecutor` for the first time. It introduces four core domain types in a new `experiment.executor` package:

- **`ManagedExecutor`** — a controlled wrapper around `java.util.concurrent.ThreadPoolExecutor` that exposes adjustable parameter setters (core/max pool size, keepAlive time) with validation against `RuntimeSetting` bounds, read-only state queries (active count, pool size, queue size, completed tasks, largest pool size, task count), read-only config queries (queue capacity, rejection policy), full lifecycle management (shutdown, shutdownNow, isShutdown, isStopped, isTerminated, awaitTermination), task submission (Callable and Runnable), unwrap access to the underlying executor, and `AutoCloseable` support.

- **`ExecutorRegistry`** — a thread-safe registry backed by `ConcurrentHashMap<String, ManagedExecutor>` with register, get, list, remove, and size operations. Removal is gated by `DeletionSafety.canRemove()` and does not auto-shutdown.

- **`RuntimeSetting`** — parameter classification via `AdjustableParameter` and `NonAdjustableParameter` enums, and value-range enforcement via `IntParameterBounds` (for pool sizes) and `LongParameterBounds` (for keepAlive time), with static default bounds constants.

- **`DeletionSafety`** — an interface and `AtomicDeletionSafety` implementation using `ConcurrentHashMap<String, AtomicInteger>` that prevents removal of executors still referenced by active experiments or not yet terminated.

The capability also extends `ExecutorStateSnapshot` with five new nullable fields (`poolSize`, `completedTaskCount`, `keepAliveTimeSeconds`, `largestPoolSize`, `taskCount`) while maintaining full backward compatibility.

## Requirements

### Requirement: ManagedExecutor wrapping ThreadPoolExecutor
The system MUST provide a `ManagedExecutor` class that wraps `java.util.concurrent.ThreadPoolExecutor` and exposes controlled parameter adjustment, read-only state, and lifecycle management.

#### Scenario: Construct with default thread factory and rejection policy
- **WHEN** a `ManagedExecutor` is created with corePoolSize=2, maxPoolSize=4, keepAliveTime=60s, and a LinkedBlockingQueue(10)
- **THEN** the executor MUST use `Executors.defaultThreadFactory()` and `ThreadPoolExecutor.AbortPolicy` as defaults

#### Scenario: setCorePoolSize reflects immediately
- **WHEN** `managedExecutor.setCorePoolSize(8)` is called
- **THEN** `managedExecutor.getCorePoolSize()` MUST return 8

#### Scenario: setMaximumPoolSize reflects immediately
- **WHEN** `managedExecutor.setMaximumPoolSize(12)` is called
- **THEN** `managedExecutor.getMaximumPoolSize()` MUST return 12

#### Scenario: setKeepAliveTime reflects immediately
- **WHEN** `managedExecutor.setKeepAliveTime(120, TimeUnit.SECONDS)` is called
- **THEN** `managedExecutor.getKeepAliveTime(TimeUnit.SECONDS)` MUST return 120

#### Scenario: submit Callable delegates to ThreadPoolExecutor
- **WHEN** a `Callable<Integer>` returning 42 is submitted
- **THEN** the returned `Future<Integer>.get()` MUST return 42

#### Scenario: submit Runnable delegates to ThreadPoolExecutor
- **WHEN** a `Runnable` is submitted
- **THEN** the task MUST execute on a thread managed by the underlying `ThreadPoolExecutor`

#### Scenario: read-only state reflects ThreadPoolExecutor values
- **WHEN** tasks are submitted and executed
- **THEN** `getActiveCount()`, `getPoolSize()`, `getQueueSize()`, `getCompletedTaskCount()`, `getLargestPoolSize()`, `getTaskCount()` MUST return values from the underlying `ThreadPoolExecutor`

#### Scenario: read-only config reflects construction values
- **WHEN** a `ManagedExecutor` is created with queueCapacity=10 and AbortPolicy
- **THEN** `getQueueCapacity()` MUST return 10 and `getRejectionPolicy()` MUST return AbortPolicy

#### Scenario: shutdown initiates orderly termination
- **WHEN** `shutdown()` is called
- **THEN** the underlying `ThreadPoolExecutor` MUST stop accepting new tasks and complete queued tasks, `isShutdown()` MUST return true

#### Scenario: shutdownNow initiates immediate stop
- **WHEN** `shutdownNow()` is called
- **THEN** the underlying `ThreadPoolExecutor` MUST attempt to stop all actively executing tasks, and `isStopped()` MUST return true

#### Scenario: awaitTermination waits for termination
- **WHEN** `awaitTermination(5, TimeUnit.SECONDS)` is called after shutdown and tasks complete
- **THEN** the method MUST return `true` within the timeout

#### Scenario: unwrap returns underlying ThreadPoolExecutor
- **WHEN** `unwrap()` is called
- **THEN** the returned object MUST be the underlying `ThreadPoolExecutor` instance

#### Scenario: close delegates to shutdown
- **WHEN** try-with-resources closes the `ManagedExecutor`
- **THEN** `isShutdown()` MUST return true

---

### Requirement: ExecutorRegistry with deletion safety
The system MUST provide an `ExecutorRegistry` that manages named `ManagedExecutor` instances with thread-safe registration, lookup, listing, and protected removal.

#### Scenario: Register and retrieve a managed executor
- **WHEN** a `ManagedExecutor` is registered with name "experiment-executor"
- **THEN** `registry.get("experiment-executor")` MUST return a non-empty Optional containing the executor

#### Scenario: Duplicate registration is rejected
- **WHEN** `register("same-name", executor2)` is called after the name is already registered
- **THEN** the operation MUST throw `IllegalArgumentException`

#### Scenario: Get non-existent name returns empty
- **WHEN** `registry.get("non-existent")` is called
- **THEN** the returned Optional MUST be empty

#### Scenario: List returns unmodifiable snapshot
- **WHEN** two executors are registered
- **THEN** `list().size()` MUST be 2 and the returned list MUST throw on modification

#### Scenario: Remove blocked by DeletionSafety
- **WHEN** `deletionSafety.canRemove("name", registry)` returns false
- **THEN** `registry.remove("name")` MUST return false and the executor MUST remain registered

#### Scenario: Remove succeeds when safety allows
- **WHEN** `deletionSafety.canRemove("name", registry)` returns true
- **THEN** `registry.remove("name")` MUST return true and `registry.get("name")` MUST return empty

#### Scenario: Thread-safe concurrent operations
- **WHEN** multiple threads concurrently register, get, and list executors
- **THEN** no data races or inconsistent state MUST occur

#### Scenario: Remove does not auto-shutdown
- **WHEN** `registry.remove("name")` succeeds
- **THEN** the removed `ManagedExecutor` MUST NOT have `shutdown()` called on it by the registry

---

### Requirement: RuntimeSetting parameter classification
The system MUST provide enums and bounds classes that classify executor parameters as adjustable or non-adjustable and define safe value ranges.

#### Scenario: Adjustable parameters cover pool sizing and keep-alive
- **WHEN** `AdjustableParameter` enum is inspected
- **THEN** it MUST contain `CORE_POOL_SIZE`, `MAX_POOL_SIZE`, and `KEEP_ALIVE_TIME`

#### Scenario: Non-adjustable parameters cover queue and rejection
- **WHEN** `NonAdjustableParameter` enum is inspected
- **THEN** it MUST contain `QUEUE_CAPACITY` and `REJECTION_POLICY`

#### Scenario: IntParameterBounds within range
- **WHEN** `IntParameterBounds.of(1, 10).within(5)` is called
- **THEN** the result MUST be `true`

#### Scenario: IntParameterBounds out of range
- **WHEN** `IntParameterBounds.of(1, 10).within(0)` is called
- **THEN** the result MUST be `false`

#### Scenario: LongParameterBounds within range
- **WHEN** `LongParameterBounds.of(0, Long.MAX_VALUE).within(60_000L)` is called
- **THEN** the result MUST be `true`

#### Scenario: RuntimeSetting provides default bounds
- **WHEN** `RuntimeSetting.CORE_POOL_SIZE_BOUNDS` is accessed
- **THEN** it MUST be `IntParameterBounds.of(1, Integer.MAX_VALUE)`

---

### Requirement: DeletionSafety with atomic reference counting
The system MUST provide a `DeletionSafety` interface and an `AtomicDeletionSafety` implementation that prevents removal of executors that are still referenced by active experiments or not yet terminated.

#### Scenario: canRemove returns false when refCount > 0
- **WHEN** `acquire("executor")` has been called once and `release()` has not been called
- **THEN** `canRemove("executor", registry)` MUST return false

#### Scenario: canRemove returns false when not terminated
- **WHEN** refCount is 0 but the executor's `isTerminated()` returns false
- **THEN** `canRemove("executor", registry)` MUST return false

#### Scenario: canRemove returns true when refCount == 0 and terminated
- **WHEN** refCount is 0 and the executor's `isTerminated()` returns true
- **THEN** `canRemove("executor", registry)` MUST return true

#### Scenario: canRemove returns true for unregistered name
- **WHEN** `canRemove("non-existent", registry)` is called for a name not in the registry
- **THEN** it MUST return true (idempotent semantics)

#### Scenario: release throws on negative count
- **WHEN** `release("executor")` is called with refCount already 0
- **THEN** the method MUST throw `IllegalStateException`

#### Scenario: acquire and release are thread-safe
- **WHEN** multiple threads concurrently acquire and release the same executor
- **THEN** the refCount MUST remain consistent and never go negative

---

### Requirement: ExecutorStateSnapshot extended fields
The system MUST extend `ExecutorStateSnapshot` with five new nullable fields that capture additional state available from a real `ThreadPoolExecutor`, while maintaining backward compatibility with existing builder usage.

#### Scenario: New fields are nullable with defaults
- **WHEN** an `ExecutorStateSnapshot` is built without setting new fields
- **THEN** `getPoolSize()`, `getCompletedTaskCount()`, `getKeepAliveTimeSeconds()`, `getLargestPoolSize()`, `getTaskCount()` MUST return null

#### Scenario: New fields are settable via builder
- **WHEN** the builder sets poolSize=4, completedTaskCount=100L, keepAliveTimeSeconds=60L, largestPoolSize=8, taskCount=150L
- **THEN** the corresponding getters MUST return the set values

#### Scenario: Equals and hashCode include new fields
- **WHEN** two snapshots differ only in `poolSize`
- **THEN** `equals()` MUST return false
