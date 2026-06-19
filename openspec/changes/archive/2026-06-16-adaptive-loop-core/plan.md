# Plan: adaptive-loop-core

## Implementation Order

1. **LoopState + LoopConfig + LoopSession** (no dependencies)
2. **AdjustmentDecision** (depends on existing types: PolicyDecision, PressureClassification, etc.)
3. **TransitionLegality + PressureStateTransition + PressureStateMachine** (depends on PressureState)
4. **AdjustmentHistory + HistoryEntry** (depends on AdjustmentDecision, AdjustmentResult)
5. **LoopEvidenceRecorder + LoopIterationEvidence** (stub interface — no deps)
6. **DecisionOrchestrator** (depends on classifier, ranker, evaluator)
7. **AdjustmentLoop** (depends on all above + adapter, safetyGate)
8. **Full unit test verification**

## Parallelism Opportunities

- Tasks 1-3 can be implemented in parallel (independent type definitions)
- Tasks 4-5 can proceed once task 3 is done
- Tasks 6-7 are sequential (7 depends on 6)
- Task 8 after all

## Verification Gate

- `mvn test` passes with zero failures (774 existing + new)
- All loop lifecycle transitions tested
- DecisionOrchestrator pipeline tested with real classifier/ranker/evaluator
