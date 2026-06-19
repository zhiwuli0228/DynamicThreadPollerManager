## Why

`ThresholdPolicyEvaluator` (v0.4.0) makes binary SCALE_UP/SCALE_DOWN/HOLD decisions from single-snapshot thresholds, but never classifies the executor's pressure state. The system can decide "what to do" but cannot answer "what state are we in." There is no trend analysis — the evaluator cannot distinguish "queue growing" from "queue steady but high." This change builds the diagnostic layer required before adaptive closed-loop adjustment (v0.14.0): a 6-state pressure classifier with trend-aware time-series analysis.

## What Changes

- **PressureState enum**: Define 6 semantic pressure states (UNDER_UTILIZED, NORMAL, QUEUE_BUILDUP, OVERLOAD, REJECTION_ACTIVE, RECOVERY) with priority ordering and human-readable descriptions
- **PressureClassifier interface + SnapshotPressureClassifier**: Trend-aware classifier that analyzes `List<ObservedSnapshot>` time series to compute a `PressureClassification` with confidence scoring. RECOVERY detection uses pure trend features (no cross-call state). OVERLOAD detection uses queue-capacity-relative thresholds
- **PressureClassification record**: Classification result with state, confidence [0.0-1.0], evidence list, metrics, and timestamp
- **ClassifierConfig record**: Configurable thresholds for trend window size, queue growth rate, rejection window, and queue capacity
- **NormalizedPressureMetrics record**: 11 metrics (9 base + 2 derived signals: queueGrowthRate, threadUtilizationRatio) computed from snapshot lists via `fromSnapshots()` + `withRejectedTaskCount()`

All new components are in new package `experiment.classification`. No new external dependencies. No changes to existing interfaces (EvidenceRecorder, PressureSampler, PressureSnapshot, PolicyEvaluator).

## Capabilities

### New Capabilities
- `pressure-state-classification`: 6-state pressure classification with trend-aware analysis, confidence scoring, and capacity-relative thresholds
- `normalized-pressure-metrics`: Cross-executor pressure metrics (11 fields) with linear-regression queue growth rate and thread utilization ratio, computed from ObservedSnapshot lists

### Modified Capabilities
- (none — no existing spec-level requirement changes)

## Impact

- **New package**: `experiment.classification` (6 types: enum, interface, 2 classes, 2 records)
- **New source files**: ~7 files (~550 lines)
- **New test files**: ~7 unit tests (~350 lines)
- **No changes to**: `PressureSnapshot`, `ObservedSnapshot`, `RuntimeObservation`, `ThresholdPolicyConfig`, `ThresholdPolicyEvaluator`, `NormalizedComparisonMetrics`
- **Breaking changes**: None (all additions are in new package)
- **Dependencies**: No new external dependencies
