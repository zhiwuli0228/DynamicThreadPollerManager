# Plan: policy-scoring-and-cpu-probe

## Implementation Order

1. **PolicyScore** (Task 1, depends on PressureClassification/NormalizedPressureMetrics from change 1)
2. **PolicyScorer + ThresholdPolicyScorer** (Tasks 2-3, depend on Task 1, PressureClassification, ThresholdPolicyConfig)
3. **PolicyRanker** (Task 4, depends on Tasks 2-3)
4. **SystemCpuProbe** (Task 5, no dependencies — pure JDK API)
5. **RuntimeObservation Modification** (Task 6, depends on Task 5)
6. **End-to-end Integration Test** (Task 7, depends on all)
7. **Full Test Verification** (Task 8, depends on all)

## Parallelism Opportunities

- Task 5 (SystemCpuProbe) can start immediately — zero dependencies
- Tasks 1-4 (scoring/ranking) can proceed once change 1 types are available
- Task 6 (RuntimeObservation) can proceed once Task 5 is done
- Task 7 (E2E) is the integration point

## Test Strategy

- PolicyScore: verify construction validation, score ranges
- ThresholdPolicyScorer: verify all 4 dimensions produce values in [0.0, 1.0]; OVERLOAD favors aggressive strategy; UNDER_UTILIZED favors conservative strategy; compositeScore = weighted sum
- PolicyRanker: verify descending order, best() returns highest, empty list handling
- SystemCpuProbe: integration test verifying processCpuLoad() >= 0 on current platform
- RuntimeObservation: unit test with mock probe, null probe fallback
- E2E: 100-step managed executor scenario → snapshots → classify → score 3 configs → rank → verify
- Run `mvn test` after each task group

## Deliverable Files

**New source files** (all `src/main/java/.../experiment/classification/`):
- `PolicyScore.java`
- `PolicyScorer.java`
- `ThresholdPolicyScorer.java`
- `PolicyRanker.java`

**New source files** (`src/main/java/.../experiment/probe/`):
- `SystemCpuProbe.java`

**Modified source files**:
- `experiment/metrics/RuntimeObservation.java` (~20 lines — add 3-arg overload, modify 2-arg to delegate)

**New test files**:
- `PolicyScoreTest.java`
- `ThresholdPolicyScorerTest.java`
- `PolicyRankerTest.java`
- `SystemCpuProbeTest.java` (integration)
- `RuntimeObservationCpuProbeTest.java`
- `ClassificationScoringRankingE2ETest.java`

## Verification Gate

- `mvn test` passes with zero failures (existing 708 + new)
- CPU probe integration test passes on current platform
- No new external dependencies
- RuntimeObservation backward-compatible (2-arg signature unchanged)
