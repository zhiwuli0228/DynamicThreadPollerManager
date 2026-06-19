# establish-managed-executor-and-registry Plan

## Header

- Change: `establish-managed-executor-and-registry`
- Schema: `superspec`
- Plan purpose: define the implementation sequence, verification commands, and autonomous continuation rule

## 1. Implementation Sequence

1. Create `experiment.executor` package.
2. Implement `ManagedExecutor` → unit test it.
3. Implement `ExecutorRegistry` → unit test it.
4. Implement `RuntimeSetting` enums + bounds classes → unit test them.
5. Implement `DeletionSafety` interface + `AtomicDeletionSafety` → unit test it.
6. Extend `ExecutorStateSnapshot` with 5 new fields → verify backward compat.
7. Run full `mvn test` for non-regression.

## 2. Verification Commands

```bash
# Run all tests
mvn test

# Verify compilation (includes new package)
mvn compile

# Check for leaked threads (manual inspection of test output)
mvn test 2>&1 | grep -i "thread"
```

## 3. Autonomous Continuation Rule

After completing this change (proposal + design + spec + tasks + plan creation):
- Do NOT proceed to implementation until `docs/00-project/current-state.md` has been updated to `EXECUTION_AUTHORIZED` for this change.
- Once authorized, implement all tasks sequentially.
- After implementation, run `mvn test` and record results.
- Do NOT archive until verify + finalize gates are passed.
- After archive, the next change `bridge-adjustment-to-real-executor` becomes the authorized successor.

## 4. Scope Boundary Reminder

- This change creates `ManagedExecutor`, `ExecutorRegistry`, `RuntimeSetting`, `DeletionSafety`, and extends `ExecutorStateSnapshot`.
- This change does NOT create `ManagedExecutorAdjustmentAdapter` or integrate safety gates.
- This change does NOT modify `experiment.scenario`, `experiment.policy`, `experiment.analysis`, or `experiment.metrics`.
