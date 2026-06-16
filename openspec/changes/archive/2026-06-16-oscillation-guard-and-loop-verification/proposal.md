## Why

Change 1 (`adaptive-loop-core`) provides the closed-loop skeleton, but two critical safety features are missing: oscillation detection (preventing ping-pong/over-adjustment/policy-switching patterns) and feedback-driven weight calibration (v0.13.0 DFR-01). Without these, the loop can enter destructive configuration oscillation or persistently select suboptimal policies. This change adds the safety guards and provides end-to-end verification that the complete closed-loop pipeline works correctly.

## What Changes

- **Oscillation detection**: `OscillationDetector` with sliding-window pattern matching for 3 oscillation types (ping-pong, over-adjustment, policy switching)
- **Feedback calibration**: `FeedbackCalibrator` that adjusts `ThresholdPolicyScorer` weights based on actual adjustment outcomes (median-split correlation method)
- **Evidence recorder implementation**: `InMemoryLoopEvidenceRecorder` implementing `LoopEvidenceRecorder` (from Change 1)
- **Scorer modification**: 4 package-visible weight getters added to `ThresholdPolicyScorer` (~4 lines)
- **End-to-end verification**: Integration tests validating ≥5-cycle autonomous loop run with state transitions and oscillation-triggered emergency stop

## Capabilities

### New Capabilities
- `oscillation-detection`: Sliding-window detection of ping-pong oscillation, over-adjustment, and policy switching patterns
- `feedback-weight-calibration`: Batch statistical calibration of scoring weights using median-split correlation on adjustment outcomes (v0.13.0 DFR-01 closure)
- `loop-evidence-recording`: In-memory implementation of loop iteration evidence recording

### Modified Capabilities
- `threshold-policy-scorer`: Package-visible weight getter methods (wResponsiveness/wSafety/wStability/wEfficiency) for FeedbackCalibrator access

## Impact

- **New source files**: 3 classes in `experiment.loop` (~400 lines)
- **Modified source files**: `ThresholdPolicyScorer.java` (~4 lines)
- **New test files**: ~6 tests (~500 lines)
- **Breaking changes**: None (weight getters are package-visible additions)
- **Dependencies**: Change 1 types (`AdjustmentLoop`, `AdjustmentDecision`, `AdjustmentHistory`, `LoopEvidenceRecorder`)
