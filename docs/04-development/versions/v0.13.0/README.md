# v0.13.0 Pressure Classification and Policy Scoring

## Header

- Version name: `v0.13.0`
- Authoring date: `2026-06-14`
- Status: `VERSION_DESIGN_DRAFT`
- Current phase: `VERSION_BASELINE` — version design documents created, IR not yet started
- Authoritative branch: `claude_master`
- Requirement theme: pressure state classification, trend-based classifier, policy scoring, policy ranking, CPU utilization probe

## Purpose

v0.13.0 closes the gap between "the system can sample pressure and compare executors" and "the system can classify what kind of pressure it's under and score which policy would best handle it." The primary motivation is to build the diagnostic layer required before adaptive closed-loop adjustment (v0.14.0).

Currently, `ThresholdPolicyEvaluator` makes binary scale-up/scale-down/hold decisions from single-snapshot thresholds, but never classifies the executor's pressure state (overload, underutilization, queue buildup, etc.). There is no trend analysis — the evaluator cannot distinguish "queue growing" from "queue steady but high." There is also no mechanism to score or rank multiple policy configurations for the same pressure state.

v0.13.0 introduces pressure classification (6-state model with confidence scoring), trend-aware analysis from snapshot time series, policy scoring across 4 dimensions, and CPU utilization as a real data source (DFR-01 from v0.12.0).

## Scope Summary

| # | Change (candidate) | Scope |
|---|---|---|
| 1/2 | `pressure-classification-engine` | PressureState enum, PressureClassifier interface, SnapshotPressureClassifier, NormalizedPressureMetrics, PressureClassification record |
| 2/2 | `policy-scoring-and-cpu-probe` | PolicyScore record, PolicyScorer interface, ThresholdPolicyScorer, PolicyRanker, SystemCpuProbe, CPU integration into RuntimeObservation, end-to-end verification |

## Verification Target

- `mvn test`: all existing 708 tests pass (zero regression)
- New tests: pressure state classification across all 6 states, trend-based vs single-snapshot classification, NormalizedPressureMetrics computation, policy scoring per-dimension breakdown, policy ranking for 3+ configs, CPU probe real-data round-trip, end-to-end classification→scoring→ranking pipeline

## Key Decisions

See `decision-log.md`.

- D1: Pressure state model (6 states + confidence)
- D2: Classifier design (time-series trend analysis vs single-snapshot)
- D3: NormalizedPressureMetrics (extend NormalizedComparisonMetrics vs new record)
- D4: Policy scoring model (rule-based heuristic vs simulation vs historical)
- D5: CPU probe approach (JDK ManagementFactory vs external library)
- D6: Change decomposition strategy (2 changes)

## Predecessor

- v0.12.0 baseline comparison experiment framework (ARCHIVED) — NormalizedComparisonMetrics, comparison infrastructure
- v0.11.0 persistent evidence recording and live sampling (ARCHIVED) — EvidenceRecorder, LivePressureSampler
- v0.10.0 rejection policy replacement (ARCHIVED) — dynamic config baseline complete
- v0.7.0 ManagedExecutor domain (IMPLEMENTED) — real executor infrastructure

## Document Set

- `README.md`
- `00-objectives-and-scope.md`
- `decision-log.md`

IR/SR documents to be created during subsequent phases.
