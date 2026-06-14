# Plan: pressure-classification-engine

## Implementation Order

1. **PressureState + ClassifierConfig** (Tasks 1-2, no dependencies)
2. **NormalizedPressureMetrics** (Task 3, depends on PressureSnapshot/ObservedSnapshot types — already exist)
3. **PressureClassification** (Task 4, depends on Task 1, 2)
4. **PressureClassifier + SnapshotPressureClassifier** (Task 5, depends on Tasks 1-4)
5. **Full Test Verification** (Task 6, depends on all)

## Parallelism Opportunities

- Tasks 1-2 (enum + config) and Task 3 (metrics) can proceed in parallel — no shared dependencies
- Task 4 (classification record) can proceed once Task 1 is done
- Task 5 (classifier) is the integration point requiring all prior tasks
- Task 3 can start immediately — only depends on existing types (ObservedSnapshot, PressureSnapshot)

## Test Strategy

- PressureState: verify 6 values exist, priority order, description() non-empty
- ClassifierConfig: verify construction validation, defaults()
- NormalizedPressureMetrics: verify all 11 fields from synthetic snapshots, queueGrowthRate positive/negative/~0, empty list handling, zero duration, withRejectedTaskCount()
- SnapshotPressureClassifier: verify all 6 states from constructed snapshot sequences, short-sequence degradation, empty list → NORMAL/confidence=0.0
- Run `mvn test` after each task group
- Existing 708 tests must continue to pass

## Deliverable Files

**New source files** (all `src/main/java/.../experiment/classification/`):
- `PressureState.java`
- `ClassifierConfig.java`
- `NormalizedPressureMetrics.java`
- `PressureClassification.java`
- `PressureClassifier.java`
- `SnapshotPressureClassifier.java`

**New test files** (all `src/test/java/.../experiment/classification/`):
- `PressureStateTest.java`
- `ClassifierConfigTest.java`
- `NormalizedPressureMetricsTest.java`
- `PressureClassificationTest.java`
- `SnapshotPressureClassifierTest.java`

## Verification Gate

- `mvn test` passes with zero failures (existing 708 + new)
- No new compiler warnings
- No new external dependencies
- No changes to existing packages
