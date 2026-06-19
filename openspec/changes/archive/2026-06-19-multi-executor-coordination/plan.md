# Plan: multi-executor-coordination

## Implementation Order

1. **AdjustmentPriority** (no dependencies)
2. **ExecutorGroupConfig** (depends on AdjustmentPriority)
3. **ResourceBudget** (no dependencies)
4. **CoordinationOutcome + GroupCoordinationResult** (depends on ScaleAdjustmentCommand)
5. **GroupCoordinationEntry + GroupCoordinationHistory** (depends on GroupCoordinationResult)
6. **CrossExecutorOscillationDetector** (depends on GroupCoordinationHistory)
7. **GroupCoordinator** (depends on all above + ResourceBudget + adapters)
8. **CoordinatedAdjustmentAdapter** (depends on GroupCoordinator)
9. **ExecutorGroup** (depends on all above)
10. **GroupHealth + GroupLoopOrchestrator** (depends on ExecutorGroup + AdjustmentLoop)
11. **AdjustmentFailureCode addition** (independent)
12. **Full test verification**

## Parallelism Opportunities

- Tasks 1, 3 can be implemented in parallel (independent types)
- Tasks 2, 4, 5 can proceed once 1 is done
- Task 6 can proceed once 5 is done
- Task 7 is sequential (depends on 3, 5, 6)
- Tasks 8-9 are sequential (8 depends on 7, 9 depends on 8)
- Task 10 depends on 9
- Task 11 is independent of all

## Verification Gate

- `mvn test` passes with zero failures (857 existing + ~30 new)
- All GroupCoordinator outcomes tested (APPROVED_AS_IS, MODIFIED, REJECTED, CAPPED)
- Priority preemption verified: CRITICAL preempts LOW
- Budget invariants verified under concurrent access
- GroupLoopOrchestrator lifecycle tested
