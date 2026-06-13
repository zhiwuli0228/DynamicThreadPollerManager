# Managed Executor Domain Model

## Purpose

This document defines the conceptual objects for a managed executor system.

## Concepts

- Managed executor: the runtime object being configured and observed.
- Executor registry: the container that holds named executor definitions or instances.
- Runtime setting: a controlled parameter that may later be adjusted when authorized.
- Workload profile: a bounded scenario used to exercise the executor.
- Deletion safety: the rule set that prevents accidental removal of an executor in active use.

## Model Principles

- The model should remain small until a version design expands it.
- Every mutable operation must be explicit and traceable.
- Safety semantics must be described before any implementation uses them.

## ManagedExecutor Property Implementation Rule

When adding a new configurable property to `ManagedExecutor`, apply this decision table:

| TPE provides public getter? | Action | Example |
|---|---|---|
| Yes | **Delete cache field.** Getter delegates to `executor.getXxx()` directly. Setter calls both `executor.setXxx()` and TPE's mutator if one exists. | `getRejectionPolicy()` → `executor.getRejectedExecutionHandler()` (v0.10.0) |
| No | **Keep cache field.** Initialize in constructor. TPE cannot provide the value on demand. | `getQueueCapacity()` → `this.queueCapacity` — TPE has no `getQueue()` getter (v0.9.0) |

**Rationale**: Caching a value that TPE already provides introduces consistency risk without benefit. When a setter mutates the underlying TPE, the cache must be updated in lockstep — a source of bugs. Direct delegation eliminates this risk entirely.

This rule emerged from the v0.10.0 `rejectionPolicy` field design evolution:
1. Original design (v0.10.0 scope): `private volatile RejectedExecutionHandler rejectionPolicy` — cache with volatile for visibility
2. IR review (F07): identified that TPE's `getRejectedExecutionHandler()` makes the cache field redundant
3. SR decision: delete the field entirely, delegate directly to TPE
4. Result: `ManagedExecutor` change was 2 lines added (getter/setter), 1 field deleted, 1 constructor line removed — minimal, consistent, no cache to maintain
