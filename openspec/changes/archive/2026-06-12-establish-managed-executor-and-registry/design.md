# establish-managed-executor-and-registry Design

## Header

- Change identifier: `establish-managed-executor-and-registry`
- Design purpose: establish the managed executor domain layer — `ManagedExecutor`, `ExecutorRegistry`, `RuntimeSetting`, `DeletionSafety`, and `ExecutorStateSnapshot` extensions
- Authoritative inputs:
  - `docs/04-development/versions/v0.7.0/10-ir.md`
  - `docs/04-development/versions/v0.7.0/20-sr.md`
  - `docs/04-development/versions/v0.7.0/decision-log.md`
  - `docs/00-project/current-state.md`
  - `docs/01-architecture/managed-executor-domain-model.md`

## 1. Scope

In scope:
- `ManagedExecutor` class wrapping `ThreadPoolExecutor`
- `ExecutorRegistry` class with `ConcurrentHashMap` backend
- `RuntimeSetting` — `AdjustableParameter` / `NonAdjustableParameter` enums, `IntParameterBounds` / `LongParameterBounds`
- `DeletionSafety` interface + `AtomicDeletionSafety` implementation
- `ExecutorStateSnapshot` extension (5 new nullable fields)
- Unit tests for all new classes
- No regression in existing test suite

Out of scope:
- `ManagedExecutorAdjustmentAdapter` (belongs to `bridge-adjustment-to-real-executor`)
- Safety gate integration
- Closed-loop experiment
- Queue resizing, scheduler, persistence, REST/API

## 2. Package and Class Layout

```
experiment.executor (new package)
├── ManagedExecutor.java
├── ExecutorRegistry.java
├── AdjustableParameter.java
├── NonAdjustableParameter.java
├── IntParameterBounds.java
├── LongParameterBounds.java
├── RuntimeSetting.java
├── DeletionSafety.java
└── AtomicDeletionSafety.java

experiment.adjustment (modified)
└── ExecutorStateSnapshot.java  ← 5 new fields + builder methods
```

## 3. Key Design Decisions

- `ManagedExecutor` implements `AutoCloseable` (close → shutdown), wraps a single `ThreadPoolExecutor`.
- Defaults: `Executors.defaultThreadFactory()`, `AbortPolicy`.
- `ExecutorRegistry` uses `ConcurrentHashMap`; `remove()` does NOT auto-shutdown.
- `DeletionSafety.canRemove()` requires both `refCount == 0` AND `isTerminated() == true`.
- `AtomicDeletionSafety` uses `ConcurrentHashMap<String, AtomicInteger>` for reference counting.
- `ExecutorStateSnapshot` new fields are nullable → backward compatible.
- `IntParameterBounds` for pool sizes, `LongParameterBounds` for keepAliveTime.

## 4. Verification Requirements

- `mvn test` exits 0 with all existing + new tests passing.
- `ManagedExecutor.setCorePoolSize(n) → getCorePoolSize() == n` (unit test).
- `ExecutorRegistry` concurrent register/get/remove no data race (concurrent test).
- `DeletionSafety` refCount > 0 or !isTerminated → canRemove == false.
- `ExecutorStateSnapshot` old builder usage (without new fields) compiles and passes.
- No test leaks threads (all executors shutdown in @AfterEach).

## 5. Closeout Steps

- Proposal, spec, tasks, and plan artifacts created.
- Current-state synchronized to reflect this change as active.
- Implementation authorized only after `EXECUTION_AUTHORIZED`.
