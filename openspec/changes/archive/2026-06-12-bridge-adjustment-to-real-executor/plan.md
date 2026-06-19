# bridge-adjustment-to-real-executor Plan

## Header

- Change: `bridge-adjustment-to-real-executor`
- Schema: `superspec`
- Plan purpose: define the implementation sequence, verification commands, and autonomous continuation rule

## 1. Implementation Sequence

1. Add `EXECUTOR_NOT_FOUND` to `AdjustmentFailureCode` enum.
2. Implement `ManagedExecutorAdjustmentAdapter` → unit test it.
3. Run full `mvn test` for non-regression.
4. Verify existing `InMemoryAdjustableExecutorProbe` tests unchanged.

## 2. Verification Commands

```bash
# Run all tests
mvn test

# Verify compilation
mvn compile

# Verify openspec consistency
openspec validate --all --json
```

## 3. Autonomous Continuation Rule

After completing this change (proposal + design + spec + tasks + plan creation):
- Do NOT proceed to implementation until `docs/00-project/current-state.md` has been updated to `EXECUTION_AUTHORIZED` for this change.
- Once authorized, implement all tasks sequentially.
- After implementation, run `mvn test` and record results.
- Do NOT archive until verify + finalize gates are passed.
- After archive, the next change `closed-loop-experiment-verification` becomes the authorized successor.

## 4. Scope Boundary Reminder

- This change creates `ManagedExecutorAdjustmentAdapter` and adds `EXECUTOR_NOT_FOUND` to `AdjustmentFailureCode`.
- This change does NOT create closed-loop experiment tests or modify `experiment.scenario`.
- This change does NOT modify `experiment.policy`, `experiment.analysis`, or `experiment.metrics`.
- This change does NOT introduce new external dependencies.
