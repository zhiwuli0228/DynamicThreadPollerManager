# Tasks: oscillation-guard-and-loop-verification

## 1. ThresholdPolicyScorer Modification

- [ ] 1.1 Add package-visible getters to `ThresholdPolicyScorer`: `wResponsiveness()`, `wSafety()`, `wStability()`, `wEfficiency()` — each returns the corresponding private final field
- [ ] 1.2 Verify no existing tests break (getters are package-visible, not public API)
- [ ] 1.3 Write unit test: getters return values matching constructor args

## 2. OscillationDetector

- [ ] 2.1 Create `OscillationDetector` class in `experiment.loop`: windowSize, patternThreshold (all final, validated >=4, >=1)
- [ ] 2.2 Implement `wouldOscillate(AdjustmentDecision pending, AdjustmentHistory history)` → boolean
- [ ] 2.3 Implement `detectPingPong(List<Integer> targets)` — direction alternation >= patternThreshold
- [ ] 2.4 Implement `detectOverAdjustment(List<Integer> targets)` — >=3 consecutive same direction
- [ ] 2.5 Implement `detectPolicySwitching(List<HistoryEntry>, AdjustmentDecision)` — same policy >=2x separated
- [ ] 2.6 Implement `detectedPattern(AdjustmentHistory)` → Optional<String>
- [ ] 2.7 Write unit tests: ping-pong detected [10,20,10,20], over-adjustment detected [10,15,20,25], policy switching detected [A,B,A,C,A], normal history not detected, empty history→false, insufficient data→false, default constructor values

## 3. FeedbackCalibrator

- [ ] 3.1 Create `FeedbackCalibrator` class: maxAdjustmentPerCycle (default 0.05), minWeight (0.10), maxWeight (0.50)
- [ ] 3.2 Implement `calibrate(AdjustmentHistory, ThresholdPolicyScorer, int windowSize)` → ThresholdPolicyScorer
- [ ] 3.3 Implement median-split correlation per dimension: high-score vs low-score group success rate difference
- [ ] 3.4 Implement weight adjustment: correlation>0.1→+0.02, correlation<-0.1→-0.02
- [ ] 3.5 Implement normalization (sum=1.0) and clamping [minWeight, maxWeight]
- [ ] 3.6 Return currentScorer if insufficient data (< windowSize adjustments)
- [ ] 3.7 Write unit tests: calibration produces different weights from default, sum=1.0 after calibration, each weight in [0.10,0.50], insufficient data returns same scorer, all-success/all-failure history (no correlation→no change), mixed history produces directional adjustment

## 4. InMemoryLoopEvidenceRecorder

- [ ] 4.1 Create `InMemoryLoopEvidenceRecorder` implementing `LoopEvidenceRecorder` in `experiment.loop`
- [ ] 4.2 Use thread-safe List (CopyOnWriteArrayList) for iteration evidence storage
- [ ] 4.3 Implement all 4 interface methods
- [ ] 4.4 Write unit tests: recordIteration→getIterationEvidence round-trip, recordSessionStart/End, thread safety

## 5. End-to-End Integration Tests

- [ ] 5.1 Create `LoopEndToEndTest` integration test class
- [ ] 5.2 Test: normal loop run ≥5 cycles with real ManagedExecutor, LivePressureSampler, real classifier/ranker/evaluator, 3 candidate policies (conservative/moderate/aggressive)
- [ ] 5.3 Verify at least 1 actual adjustment (non-NO_OP) occurred
- [ ] 5.4 Verify AdjustmentHistory has entries after run
- [ ] 5.5 Verify PressureStateMachine tracks state transitions
- [ ] 5.6 Verify loop ends in STOPPED state (not EMERGENCY_STOPPED) for normal workload
- [ ] 5.7 Test: oscillation triggers emergency stop — use mock OscillationDetector always returning true
- [ ] 5.8 Verify loop ends in EMERGENCY_STOPPED with oscillation reason in summary
- [ ] 5.9 Test: reset after emergency stop → can start new session

## 6. Full Test Verification

- [ ] 6.1 Run `mvn test` — verify all existing tests pass (774 + Change 1 new)
- [ ] 6.2 Verify all new Change 2 tests pass
- [ ] 6.3 Run `mvn test` with `-pl` to isolate Change 2 tests if needed
