# Plan: oscillation-guard-and-loop-verification

## Implementation Order

1. **ThresholdPolicyScorer getters** (4 lines, no dependencies)
2. **OscillationDetector** (depends on AdjustmentDecision, AdjustmentHistory from Change 1)
3. **FeedbackCalibrator** (depends on AdjustmentHistory from Change 1, ThresholdPolicyScorer getters)
4. **InMemoryLoopEvidenceRecorder** (depends on LoopEvidenceRecorder from Change 1)
5. **Unit tests** for OscillationDetector + FeedbackCalibrator + InMemoryLoopEvidenceRecorder
6. **End-to-end integration tests** (requires Change 1 + Change 2 components)
7. **Full regression** `mvn test`

## Parallelism Opportunities

- Tasks 1-3 can be implemented in parallel (OscillationDetector and FeedbackCalibrator are independent)
- Task 4 can proceed after Change 1's LoopEvidenceRecorder is available
- Tasks 5-7 are sequential

## Verification Gate

- `mvn test` passes with zero failures (774 existing + Change 1 new + Change 2 new)
- Oscillation detection catches all 3 patterns
- Feedback calibration produces weight deltas from default
- E2E loop runs ≥5 cycles with ≥1 actual adjustment
- Emergency stop triggers on oscillation detection
