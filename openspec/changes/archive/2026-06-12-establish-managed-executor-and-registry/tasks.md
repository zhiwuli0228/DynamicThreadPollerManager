## 1. ManagedExecutor

- [x] 1.1 Create `experiment.executor` package under `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/executor/`.
- [x] 1.2 Implement `ManagedExecutor` wrapping `ThreadPoolExecutor`: constructors (2 overloads), getters/setters for core/max pool size and keepAlive time, read-only state queries, read-only config queries, lifecycle methods (shutdown, shutdownNow, isShutdown, isStopped, isTerminated, awaitTermination), task submission (submit Callable, submit Runnable), unwrap, AutoCloseable.close.
- [x] 1.3 Verify `ManagedExecutor` behavior: parameter set/get round-trip, task submission and execution, lifecycle state transitions, shutdown releases threads.

## 2. ExecutorRegistry

- [x] 2.1 Implement `ExecutorRegistry` with `ConcurrentHashMap<String, ManagedExecutor>` backend: register, get, list, remove, size.
- [x] 2.2 Implement deletion flow: `remove()` consults `DeletionSafety.canRemove()` before removal; does NOT auto-shutdown.
- [x] 2.3 Verify `ExecutorRegistry` behavior: register/get round-trip, duplicate rejection, remove with safety gate, list snapshot immutability, concurrent access safety.

## 3. RuntimeSetting

- [x] 3.1 Implement `AdjustableParameter` enum (CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME).
- [x] 3.2 Implement `NonAdjustableParameter` enum (QUEUE_CAPACITY, REJECTION_POLICY).
- [x] 3.3 Implement `IntParameterBounds` with `of(min, max)` and `within(int)`.
- [x] 3.4 Implement `LongParameterBounds` with `of(min, max)` and `within(long)`.
- [x] 3.5 Implement `RuntimeSetting` with static default bounds constants.
- [x] 3.6 Verify bounds: within/out-of-range for both int and long variants.

## 4. DeletionSafety

- [x] 4.1 Define `DeletionSafety` interface: acquire, release, referenceCount, canRemove.
- [x] 4.2 Implement `AtomicDeletionSafety` with `ConcurrentHashMap<String, AtomicInteger>`.
- [x] 4.3 Implement `canRemove()` logic: refCount == 0 AND executor.isTerminated() for registered names; return true for unregistered names.
- [x] 4.4 Implement `release()` negative guard: throw `IllegalStateException`.
- [x] 4.5 Verify: acquire/release refCount consistency, canRemove edge cases (refCount>0, not terminated, unregistered), negative release rejection, thread safety.

## 5. ExecutorStateSnapshot Extension

- [x] 5.1 Add five new nullable fields to `ExecutorStateSnapshot`: poolSize (Integer), completedTaskCount (Long), keepAliveTimeSeconds (Long), largestPoolSize (Integer), taskCount (Long).
- [x] 5.2 Add builder setter methods for each new field.
- [x] 5.3 Update `equals()` and `hashCode()` to include new fields.
- [x] 5.4 Verify backward compatibility: existing builder usage without new fields compiles and passes existing tests.
- [x] 5.5 Verify new field round-trip: set via builder → get returns correct value.

## 6. Test Coverage

- [x] 6.1 `mvn test` exits 0 — all existing tests + new tests pass.
- [x] 6.2 New unit test class for `ManagedExecutor` lifecycle and delegation.
- [x] 6.3 New unit test class for `ExecutorRegistry` operations and concurrency.
- [x] 6.4 New unit test class for `DeletionSafety` / `AtomicDeletionSafety`.
- [x] 6.5 New unit test class for `IntParameterBounds` / `LongParameterBounds`.
- [x] 6.6 Existing `ExecutorStateSnapshot` tests continue to pass without modification.
- [x] 6.7 All tests clean up executors (shutdown in `@AfterEach`).
