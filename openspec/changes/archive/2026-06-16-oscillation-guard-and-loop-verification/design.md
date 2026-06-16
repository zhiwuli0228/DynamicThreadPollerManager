# Design: oscillation-guard-and-loop-verification

## Input Baseline

- SR: `docs/04-development/versions/v0.14.0/20-sr.md` §4.8, §4.11, §4.12, §4.13
- Change 1 types: `AdjustmentLoop`, `AdjustmentDecision`, `AdjustmentHistory`, `HistoryEntry`, `LoopEvidenceRecorder`, `LoopSession`

## Architecture

Components in `experiment.loop` (except ThresholdPolicyScorer modification in `experiment.classification`).

## Component Design

See SR §4 for full pseudocode:

1. **OscillationDetector** (§4.8): Stateless sliding-window pattern matcher. Three detection methods: ping-pong (direction alternation), over-adjustment (≥3 consecutive same direction), policy switching (same policy ≥2 times separated by others).

2. **FeedbackCalibrator** (§4.11): Returns new ThresholdPolicyScorer instances (immutable pattern). Uses median-split correlation: high-score group vs low-score group success rate difference → weight adjustment ±0.02.

3. **InMemoryLoopEvidenceRecorder**: Implements `LoopEvidenceRecorder` using in-memory List storage. Delegates to EvidenceRecorder for persistence if needed.

4. **ThresholdPolicyScorer modification**: 4 package-visible getters (~4 lines total).

## Test Strategy

- OscillationDetector: pure function — test with constructed HistoryEntry lists
- FeedbackCalibrator: test with constructed AdjustmentHistory, verify weight deltas
- End-to-end: real ManagedExecutor + LivePressureSampler + real classifier/ranker/evaluator + 3 candidate policies → ≥5 autonomous loop cycles → verify state transitions and adjustment history
- Emergency stop test: mock OscillationDetector (always true) → verify EMERGENCY_STOPPED
