## Context

v0.10.0 change 2/2 verifies that rejection-policy replacement behaves correctly under realistic overload conditions. Change 1/2 provides all core components (RejectionPolicyCommand, ManagedExecutor.setRejectionPolicy, RejectionPolicySafetyGate, RejectionPolicyAdjustmentAdapter, PolicyReplacementResult, PolicyReplacementEvidence, ExecutorRebuildStrategy fix, QueueResizeAdjustmentAdapter.isResizeInProgress). Change 2/2 validates them end-to-end with real ThreadPoolExecutor instances.

Input: change 1/2 implemented and tests passing.

## Goals / Non-Goals

- Goals: end-to-end policy switch + overload verification for all four JDK policies, safety gate DENY verification, rebuild policy preservation verification, evidence completeness verification
- Non-Goals: custom RejectedExecutionHandler testing, performance benchmarking, multi-threaded stress testing, closed-loop policy switching

## Decisions

1. **Real ThreadPoolExecutor only** — no mocking. Policy behavior is inherently tied to TPE internals; mocks cannot reproduce overload semantics.
2. **Overload = queue full + all threads busy** — achieved via blocking tasks (CountDownLatch) that occupy threads and queue slots
3. **DiscardPolicy/DiscardOldestPolicy assertions via indirect signals** — these policies silently discard, so verification uses taskCount, completedTaskCount, and identifiable task names rather than exception catching (per IR F06 resolution)
4. **Single-threaded test execution** — no concurrent stress. Concurrency semantics (last-write-wins) are proven by TPE specification, not by test.
5. **@AfterEach cleanup** — countDown all latches → shutdown → awaitTermination → registry.remove (v0.7.0 P6 lesson)

## Risks / Trade-offs

- DiscardOldestPolicy assertion is probabilistic in highly concurrent environments — but single-threaded test makes it deterministic
- Overload scenarios depend on precise thread/queue sizing (pool=1-1, queue=2) — brittle if TPE behavior changes (highly unlikely for stable JDK API)
- Tests use CountDownLatch blocking tasks that MUST be released in @AfterEach to avoid thread leaks

## Dependencies

- Change 1/2: all core components (RejectionPolicyCommand, ManagedExecutor modifications, RejectionPolicySafetyGate, RejectionPolicyAdjustmentAdapter, PolicyReplacementResult, PolicyReplacementEvidence, ExecutorRebuildStrategy fix, QueueResizeAdjustmentAdapter.isResizeInProgress)
- v0.7.0: ManagedExecutor, ExecutorRegistry
- v0.9.0: ExecutorRebuildStrategy, QueueResizeAdjustmentAdapter, QueueResizeCommand
- JDK: CountDownLatch, ThreadPoolExecutor, RejectedExecutionException

## Migration Plan

No migration needed — test-only change.
