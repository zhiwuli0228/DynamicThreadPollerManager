# v0.14.0 Adaptive Closed-Loop Adjustment

## Header

- Version name: `v0.14.0`
- Authoring date: `2026-06-14`
- Status: `VERSION_DESIGN_DRAFT`
- Current phase: `SR_CLOSED` — SR closed (3 P0 + 5 P1 fixed), change decomposition authorized
- Authoritative branch: `claude_master`
- Requirement theme: autonomous closed-loop adjustment, oscillation detection, state transition model, adjustment decision orchestration, feedback-driven weight calibration

## Purpose

v0.14.0 closes the gap between "the system can diagnose what state it's in and which policy fits best" and "the system can autonomously act on that diagnosis." v0.13.0 delivered the diagnostic layer — pressure classification answers "what state am I in?" and policy scoring answers "which policy should handle this?" v0.14.0 adds the actuator: a looping controller that samples, classifies, scores, selects, adjusts, and observes — continuously, with safety guards against oscillation and over-adjustment.

The primary motivation is to demonstrate a complete closed-loop pipeline: from observation through diagnosis through decision through action through outcome assessment — all without human intervention.

## Scope Summary

| # | Change (candidate) | Scope |
|---|---|---|
| 1/2 | `adaptive-loop-core` | LoopState, LoopConfig, AdjustmentLoop, AdjustmentDecision, DecisionOrchestrator, PressureStateTransition model, LoopSession |
| 2/2 | `oscillation-guard-and-loop-verification` | OscillationDetector, AdjustmentHistory, HistoryWindow, LoopEvidenceRecorder, feedback-driven weight calibration (DFR-01 from v0.13.0), end-to-end verification |

## Verification Target

- `mvn test`: all existing 774 tests pass (zero regression)
- New tests: loop lifecycle transitions, decision orchestration pipeline, oscillation detection, state transition validation, adjustment history recording, weight calibration, end-to-end closed-loop run
- At least one end-to-end scenario: system starts idle → workload applied → loop classifies QUEUE_BUILDUP → scores policies → selects best → applies adjustment → observes recovery → loop continues

## Key Decisions

See `decision-log.md`.

- D1: Loop architecture (single-threaded polling vs event-driven)
- D2: Adjustment decision model (policy-driven delegate vs direct command generation)
- D3: Oscillation detection strategy (sliding window vs state machine based)
- D4: Feedback-driven weight calibration approach
- D5: State transition model formalization
- D6: Change decomposition strategy (2 changes)

## Predecessor

- v0.13.0 pressure classification and policy scoring (IMPLEMENTED) — PressureClassifier, PolicyScorer, PolicyRanker, PressureClassification, PolicyScore
- v0.12.0 baseline comparison experiment framework (ARCHIVED) — NormalizedComparisonMetrics, comparison infrastructure
- v0.11.0 persistent evidence recording and live sampling (ARCHIVED) — LivePressureSampler, FileBackedEvidenceRecorder
- v0.10.0 rejection policy replacement (ARCHIVED) — dynamic config baseline complete
- v0.9.0 queue capacity resizing (ARCHIVED) — ExecutorRebuildStrategy
- v0.7.0 ManagedExecutor domain (IMPLEMENTED) — AdjustmentAdapter, SafetyGate, ExperimentCoordinator

## Document Set

- `README.md`
- `00-objectives-and-scope.md`
- `decision-log.md`

IR/SR documents to be created during subsequent phases.

- `10-ir.md`
- `11-ir-review.md`
- `12-ir-review-disposition.md`
- `13-ir-closure-verification.md`
- `20-sr.md`
- `21-sr-review.md`
- `22-sr-review-disposition.md`
- `23-sr-closure-verification.md`
