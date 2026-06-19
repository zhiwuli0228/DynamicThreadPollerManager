# Tasks: multi-executor-coordination

## 1. AdjustmentPriority

- [x] 1.1 Create `AdjustmentPriority` enum in `experiment.coordination`: CRITICAL(4), HIGH(3), NORMAL(2), LOW(1)
- [x] 1.2 Implement `getLevel()` and `canPreempt(AdjustmentPriority other)`
- [x] 1.3 Write unit test: 16 combinations of canPreempt, getLevel values

## 2. ExecutorGroupConfig

- [x] 2.1 Create `ExecutorGroupConfig` record with 7 fields (groupId, maxTotalThreads, maxTotalQueueCapacity, defaultPriority, memberPriorities, coordinationTimeoutMs, failOpen)
- [x] 2.2 Add compact constructor validation: non-blank groupId, maxTotalThreads>=1, maxTotalQueueCapacity>=0, coordinationTimeoutMs>=100, non-null defaultPriority/memberPriorities, defensive copy of memberPriorities
- [x] 2.3 Add `defaults(String groupId, int maxTotalThreads)` static factory
- [x] 2.4 Write unit tests: valid construction, each invalid field, defaults() values

## 3. ResourceBudget

- [x] 3.1 Create `ResourceBudget` class in `experiment.coordination`: maxTotalThreads, maxTotalQueueCapacity, ConcurrentHashMap for allocations
- [x] 3.2 Implement `availableThreads()`, `totalAllocatedThreads()`, `allocatedThreads(String)`, `getThreadAllocations()`
- [x] 3.3 Implement `synchronized reserve(String executorId, int threadDelta)` — validate delta <= available, allow negative delta (release)
- [x] 3.4 Implement `release(String executorId, int amount)` — delegates to reserve(-amount)
- [x] 3.5 Implement `snapshot()` — returns copy with same allocations
- [x] 3.6 Write unit tests: reserve reduces available, release increases, rejects over-allocation, negative release to zero removes entry, invariants hold
- [x] 3.7 Write concurrency test: 10 threads × 100 ops concurrent reserve/release, verify invariant

## 4. CoordinationOutcome + GroupCoordinationResult

- [x] 4.1 Create `CoordinationOutcome` enum: APPROVED_AS_IS, MODIFIED, REJECTED, CAPPED
- [x] 4.2 Create `GroupCoordinationResult` record: command, approvedCommand, outcome, rationale, conflicts, crossOscillationDetected, coordinatedAt
- [x] 4.3 Implement `isApproved()` (APPROVED_AS_IS or MODIFIED), `isRejected()` (REJECTED)
- [x] 4.4 Write unit tests: record construction, isApproved for all 4 outcomes

## 5. GroupCoordinationEntry + GroupCoordinationHistory

- [x] 5.1 Create `GroupCoordinationEntry` record: executorId, command, result, budgetBefore, budgetAfter, timestamp
- [x] 5.2 Create `GroupCoordinationHistory` class with CopyOnWriteArrayList
- [x] 5.3 Implement `record()`, `recent(int)`, `byExecutor(String)`, `totalCoordinationCount()`, `rejectedCount()`, `modifiedCount()`, `preemptionCount()`, `isEmpty()`, `clear()`
- [x] 5.4 Write unit tests: record+recent, byExecutor filtering, count accuracy, thread-safety (3 threads × 100 records)

## 6. CrossExecutorOscillationDetector

- [x] 6.1 Create `CrossExecutorOscillationDetector` class: windowSize (default 8)
- [x] 6.2 Implement `wouldCrossOscillate(ScaleAdjustmentCommand, String sourceId, GroupCoordinationHistory)` — 3 pattern detectors
- [x] 6.3 Pattern 1: lockstep counter-adjustment (A↑B↓A↑B↓)
- [x] 6.4 Pattern 2: resource ping-pong (same units between same pair >= 3 times)
- [x] 6.5 Pattern 3: priority thrashing (same source→target preemption >= 3 times)
- [x] 6.6 Write unit tests: each pattern detected, normal history false, empty history false, insufficient entries false

## 7. GroupCoordinator

- [x] 7.1 Create `GroupCoordinator` class: config, budget, history, crossDetector, adapters map, clock, internal lock
- [x] 7.2 Implement `coordinate(ScaleAdjustmentCommand command, String executorName)` — full algorithm per SR §2.4
- [x] 7.3 Fast path: no-op/scale-down → APPROVED_AS_IS immediately
- [x] 7.4 Budget sufficient path: reserve, APPROVED_AS_IS
- [x] 7.5 Preemption path: scan lower-priority executors, collect preemptible budget
- [x] 7.6 Full preemption: collect >= delta → MODIFIED
- [x] 7.7 Partial preemption: collect < delta but > 0 → CAPPED with capped command
- [x] 7.8 No preemption: collect == 0 → REJECTED
- [x] 7.9 Implement `applyPreemption(String executorId, int newTarget, String reason)` — direct adapter invocation
- [x] 7.10 Cross-oscillation check runs for every non-no-op coordination (advisory)
- [x] 7.11 History recording for every coordination call
- [x] 7.12 Write unit tests: all 4 outcomes, priority preemption, cross-oscillation flag, history recorded

## 8. CoordinatedAdjustmentAdapter

- [x] 8.1 Create `CoordinatedAdjustmentAdapter` implementing `ExecutorAdjustmentAdapter`
- [x] 8.2 Constructor: delegate, coordinator, executor, executorName, clock
- [x] 8.3 `currentState()` — delegates directly
- [x] 8.4 `apply(ScaleAdjustmentCommand)` — coordinate → handle outcome → delegate or return rejected
- [x] 8.5 CAPPED path: use approvedCommand with capped target
- [x] 8.6 Scale-down path: release budget after successful apply
- [x] 8.7 Write unit tests: delegates reads, REJECTED returns AdjustmentResult(REJECTED), CAPPED applies modified command, scale-down releases budget

## 9. ExecutorGroup

- [x] 9.1 Create `ExecutorGroup` class: config, members map, budget, coordinator, history
- [x] 9.2 Constructor validates: non-empty members, `sum(corePoolSize) <= maxTotalThreads`, queue budget if applicable
- [x] 9.3 Initialize budget with current corePoolSize allocations
- [x] 9.4 Getters: getConfig(), getMembers(), getBudget(), getCoordinator(), getHistory(), size(), contains()
- [x] 9.5 Write unit tests: valid construction, budget exceeded throws, getMembers unmodifiable, contains()

## 10. GroupHealth + GroupLoopOrchestrator

- [x] 10.1 Create `GroupHealth` record: groupId, totalExecutors, runningLoops, pausedLoops, stoppedLoops, emergencyStoppedLoops, loopStates (Map<String, LoopState>), budgetSnapshot, budgetAvailable, activeWarnings
- [x] 10.2 Create `GroupLoopOrchestrator` class: group, loops map, LoopComponents record
- [x] 10.3 Implement `startAll(Map<String, LoopComponents>)` — create AdjustmentLoop per executor with CoordinatedAdapter
- [x] 10.4 Implement `pauseAll()`, `resumeAll()`, `stopAll()`, `emergencyStopAll(String)`
- [x] 10.5 Implement `getGroupHealth()` — aggregate loop states, budget snapshot, utilization warnings
- [x] 10.6 Write unit tests: lifecycle (start→pause→resume→stop), getGroupHealth counts, budget utilization warning

## 11. AdjustmentFailureCode Addition

- [x] 11.1 Add `COORDINATION_REJECTED` to `AdjustmentFailureCode` enum
- [x] 11.2 Add `COORDINATION_CAPPED` to `AdjustmentFailureCode` enum
- [x] 11.3 Verify no existing code breaks (enum value addition is backward-compatible)

## 12. Full Verification

- [x] 12.1 Run `mvn test` — verify all 857 existing tests pass (zero regression)
- [x] 12.2 Verify all new Change 1 tests pass
