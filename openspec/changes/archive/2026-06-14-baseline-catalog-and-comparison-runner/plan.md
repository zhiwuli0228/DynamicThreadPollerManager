# Plan: baseline-catalog-and-comparison-runner

## Implementation Order

Tasks are organized to minimize inter-task dependencies:

1. **CommonExecutorPreset + BaselineExecutorCatalog** (Tasks 1-2, no dependencies)
2. **NormalizedComparisonMetrics + MetricDelta + ComparisonResult** (Tasks 3-5, depend only on task 1 for CommonExecutorPreset type, can proceed in parallel with 1-2)
3. **ManagedExecutor Rejection Counting** (Task 6, no dependencies on 1-5, can proceed in parallel)
4. **ScenarioRunOutcome Extension** (Task 7, depends on task 6 for getRejectedTaskCount)
5. **ComparableScenarioRunner** (Task 8, depends on tasks 1-5 and 7)
6. **Full Test Verification** (Task 9, depends on all)

## Parallelism Opportunities

- Tasks 1-2 (Catalog) and Task 6 (Rejection Counting) can be implemented in parallel — zero shared dependencies
- Tasks 3-5 (Metrics/Result models) can proceed once task 1's CommonExecutorPreset exists
- Task 7 (ScenarioRunOutcome) can proceed once task 6 is done
- Task 8 (ComparableScenarioRunner) is the integration point requiring all prior tasks

## Test Strategy

- All new code must have unit tests
- ComparableScenarioRunner requires integration tests with real BaselineWorkloadExecutor and ManagedExecutor
- Existing 646 tests must continue to pass (zero regression)
- Run `mvn test` after each task group to catch regressions early

## Deliverable Files

**New source files** (all `src/main/java/.../experiment/scenario/`):
- `CommonExecutorPreset.java`
- `BaselineExecutorCatalog.java`
- `NormalizedComparisonMetrics.java`
- `MetricDelta.java`
- `ComparisonResult.java`
- `ComparableScenarioRunner.java`

**Modified source files**:
- `experiment/executor/ManagedExecutor.java` (~15 lines)
- `experiment/scenario/ScenarioRunOutcome.java` (~10 lines)
- `experiment/scenario/ManagedExecutorScenarioRunner.java` (~5 lines)

**New test files**:
- `CommonExecutorPresetTest.java`
- `BaselineExecutorCatalogTest.java`
- `NormalizedComparisonMetricsTest.java`
- `MetricDeltaTest.java`
- `ComparisonResultTest.java`
- `ComparableScenarioRunnerTest.java`
- `ManagedExecutorRejectionCountTest.java` (addition to existing ManagedExecutorTest or new file)
- `ScenarioRunOutcomeRejectedCountTest.java` (addition to existing)

## Verification Gate

- `mvn test` passes with zero failures across all tests (existing 646 + new)
- No new compiler warnings
- No new external dependencies
