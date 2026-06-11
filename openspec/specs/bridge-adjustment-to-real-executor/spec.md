# bridge-adjustment-to-real-executor Specification

## Purpose

The `bridge-adjustment-to-real-executor` capability bridges the experiment adjustment pipeline (v0.5.0–v0.6.0) to a real `ManagedExecutor` wrapping `ThreadPoolExecutor`. It introduces `ManagedExecutorAdjustmentAdapter`, which implements `ExecutorAdjustmentAdapter` and connects `ScaleAdjustmentCommand`, `RuntimeAdjustmentSafetyGate`, and `ExecutorRegistry` to apply parameter adjustments on a real thread pool executor. It also extends `AdjustmentFailureCode` with `EXECUTOR_NOT_FOUND` for cases where the target executor is not registered.

## Requirements

### Requirement: ManagedExecutorAdjustmentAdapter bridges adjustment to real executor
The system MUST provide a `ManagedExecutorAdjustmentAdapter` that implements `ExecutorAdjustmentAdapter` and bridges `ScaleAdjustmentCommand` to a real `ManagedExecutor` via `ExecutorRegistry` lookup, safety gate evaluation, and parameter application.

#### Scenario: Apply adjustment successfully
- **WHEN** `apply(command)` is called with `command.targetPoolSize()=6` for a registered executor with core=2, max=4
- **AND** the safety gate returns `ALLOW`
- **THEN** the adapter MUST set `corePoolSize` to 6 and `maximumPoolSize` to 6 on the `ManagedExecutor`
- **AND** return `AdjustmentResult` with status `APPLIED`, `afterState.corePoolSize()=6`

#### Scenario: Apply adjustment with target within current max
- **WHEN** `apply(command)` is called with `command.targetPoolSize()=3` for a registered executor with core=2, max=4
- **AND** the safety gate returns `ALLOW`
- **THEN** `setCorePoolSize(3)` MUST be called but `setMaximumPoolSize()` MUST NOT be called
- **AND** return `AdjustmentResult` with status `APPLIED`

#### Scenario: Rejected by safety gate
- **WHEN** `apply(command)` is called and the safety gate returns `REJECTED` with `COOLDOWN_ACTIVE`
- **THEN** the adapter MUST NOT call any setter on the `ManagedExecutor`
- **AND** return `AdjustmentResult` with status `REJECTED`, `failureCode=COOLDOWN_ACTIVE`

#### Scenario: Safety gate returns NO_OP
- **WHEN** `apply(command)` is called and the safety gate returns `NO_OP`
- **THEN** the adapter MUST NOT call any setter on the `ManagedExecutor`
- **AND** return `AdjustmentResult` with status `NO_OP`

#### Scenario: Executor not found in registry
- **WHEN** `apply(command)` is called for an executor name not present in `ExecutorRegistry`
- **THEN** the adapter MUST return `AdjustmentResult` with status `FAILED`, `failureCode=EXECUTOR_NOT_FOUND`

#### Scenario: Runtime exception from ThreadPoolExecutor
- **WHEN** `apply(command)` is called with a target value that `ThreadPoolExecutor.setCorePoolSize()` rejects (throws `IllegalArgumentException`)
- **AND** the safety gate returned `ALLOW`
- **THEN** the adapter MUST catch the exception and return `AdjustmentResult` with status `FAILED`, `failureCode=INVALID_COMMAND`
- **AND** MUST NOT call `safetyGate.recordApplied()`

#### Scenario: currentState returns snapshot from real executor
- **WHEN** `currentState()` is called on an adapter wired to a registered `ManagedExecutor` with core=2, max=4
- **THEN** the returned `ExecutorStateSnapshot` MUST have `corePoolSize=2`, `maximumPoolSize=4`
- **AND** the new fields (`poolSize`, `completedTaskCount`, etc.) MUST be populated from the underlying `ThreadPoolExecutor`

#### Scenario: currentState throws when executor not found
- **WHEN** `currentState()` is called on an adapter whose executor name is not registered
- **THEN** the adapter MUST throw `IllegalStateException`

#### Scenario: recordApplied called after successful apply
- **WHEN** `apply(command)` succeeds with `APPLIED`
- **THEN** the adapter MUST call `safetyGate.recordApplied(decision)` exactly once

#### Scenario: recordApplied NOT called after safety gate rejection
- **WHEN** `apply(command)` returns `REJECTED` or `NO_OP`
- **THEN** the adapter MUST NOT call `safetyGate.recordApplied()`

---

### Requirement: AdjustmentFailureCode extended with EXECUTOR_NOT_FOUND
The system MUST add `EXECUTOR_NOT_FOUND` to the `AdjustmentFailureCode` enum for cases where the target executor is not found in the registry.

#### Scenario: EXECUTOR_NOT_FOUND enum value exists
- **WHEN** `AdjustmentFailureCode.valueOf("EXECUTOR_NOT_FOUND")` is called
- **THEN** it MUST return the enum constant without throwing

#### Scenario: Existing enum values unchanged
- **WHEN** the existing 8 `AdjustmentFailureCode` constants are inspected
- **THEN** all MUST remain present and their ordinal values MUST be unchanged
