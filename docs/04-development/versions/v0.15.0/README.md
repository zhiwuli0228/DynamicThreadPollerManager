# v0.15.0 Multi-Executor Coordination and Closed-Loop Validation

## Header

- Version name: `v0.15.0`
- Authoring date: `2026-06-16`
- Status: `ARCHIVED`
- Current phase: `ARCHIVED` — both changes implemented and archived 2026-06-19
- Authoritative branch: `claude_master`
- Requirement theme: multi-executor group coordination, resource budgeting, adjustment priority, closed-loop cross-validation (closed-loop vs static-policy vs baseline)

## Purpose

v0.15.0 extends the closed-loop adjustment capability from a single executor to a coordinated group, and provides empirical evidence that closed-loop adjustment outperforms both static-policy and no-adjustment baselines.

v0.14.0 proved that a single ManagedExecutor can autonomously run a closed control loop (sample → classify → score → select → adjust → observe) with safety guards against oscillation and over-adjustment. But real applications rarely have just one thread pool — they have multiple pools serving different workload types (request processing, background tasks, I/O operations), each with interdependent resource demands.

v0.15.0 adds two capabilities:
1. **Multi-executor coordination** — group multiple executors under a shared resource budget, coordinate adjustments to prevent conflicting resource allocation, and prioritize executors when resources are constrained.
2. **Closed-loop validation** — use v0.12.0's comparison infrastructure to run the same workload through three modes (closed-loop, best static policy, no-adjustment baseline) and produce statistically-validated evidence that closed-loop adjustment delivers measurable improvement.

## Scope Summary

| # | Change (candidate) | Scope |
|---|---|---|
| 1/2 | `multi-executor-coordination` | ExecutorGroup, GroupCoordinator, ResourceBudget, AdjustmentPriority, ExecutorGroupConfig, GroupLoopOrchestrator, GroupCoordinationResult |
| 2/2 | `closed-loop-validation-and-evidence` | ClosedLoopValidationRunner, ValidationScenario, ValidationComparisonReport, StatisticalSignificance, validation evidence artifacts, end-to-end cross-validation tests |

## Verification Target

- `mvn test`: all existing 857 tests pass (zero regression)
- New tests: group lifecycle, resource budget enforcement, priority-based allocation, conflicting adjustment detection, cross-validation scenario execution, comparison report generation, statistical significance calculation
- At least one end-to-end scenario: 2-executor group → workload applied to both → group coordinator detects resource contention → allocates budget by priority → both executors converge to stable state → validation report confirms closed-loop outperforms baseline

## Key Decisions

See `decision-log.md`.

- D1: Coordination model (centralized GroupCoordinator vs distributed peer-to-peer)
- D2: Resource budgeting strategy (static allocation vs dynamic vs priority-based)
- D3: Cross-validation methodology (paired comparison vs independent runs)
- D4: Adjustment conflict resolution (serialize vs merge vs reject)
- D5: Change decomposition strategy (2 changes)
- D6: Statistical significance framework

## Predecessor

- v0.14.0 adaptive closed-loop adjustment (ARCHIVED) — AdjustmentLoop, DecisionOrchestrator, PressureStateMachine, OscillationDetector, AdjustmentHistory, FeedbackCalibrator
- v0.13.0 pressure classification and policy scoring (IMPLEMENTED) — PressureClassifier, PolicyScorer, PolicyRanker
- v0.12.0 baseline comparison experiment framework (ARCHIVED) — BaselineExecutorCatalog, ComparableScenarioRunner, NormalizedComparisonMetrics, ComparisonReportArtifact
- v0.11.0 persistent evidence recording and live sampling (ARCHIVED) — LivePressureSampler, FileBackedEvidenceRecorder
- v0.10.0-v0.7.0 dynamic configuration dimensions and ManagedExecutor domain (ARCHIVED) — AdjustmentAdapter, SafetyGate, ExecutorRegistry

## Document Set

- `README.md`
- `00-objectives-and-scope.md`
- `decision-log.md`

IR/SR documents to be created during subsequent phases:
- `10-ir.md`
- `11-ir-review.md`
- `12-ir-review-disposition.md`
- `13-ir-closure-verification.md`
- `20-sr.md`
- `21-sr-review.md`
- `22-sr-review-disposition.md`
- `23-sr-closure-verification.md`
