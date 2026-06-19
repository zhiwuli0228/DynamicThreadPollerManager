# v0.15.0 Change Decomposition Plan

## Header

- Version: `v0.15.0`
- Date: `2026-06-17`
- Status: `READY_FOR_EXECUTION`
- IR: `10-ir.md` (closed — 12 IR entries, 38 ACs)
- SR: `20-sr.md` (closed — 11 findings resolved)
- Decision log: `decision-log.md` (D6: 2 changes)

## Change Breakdown

### Change 1/2: `multi-executor-coordination`

**Scope**: Executor group abstraction, resource budgeting, priority-based coordination, group lifecycle management, cross-executor oscillation detection, coordinated adapter decorator.

**New package**: `experiment.coordination`

**Types** (12 new):
| # | Type | Kind | Key Responsibility |
|---|---|---|---|
| 1 | `AdjustmentPriority` | enum | CRITICAL/HIGH/NORMAL/LOW with `canPreempt()` |
| 2 | `ExecutorGroupConfig` | record | Group identity, budget limits, priorities, timeout |
| 3 | `ResourceBudget` | class | Thread-safe budget tracking, atomic reserve/release |
| 4 | `GroupCoordinator` | class | Central coordination: budget check, priority preemption, cross-oscillation advisory |
| 5 | `CoordinationOutcome` | enum | APPROVED_AS_IS, MODIFIED, REJECTED, CAPPED |
| 6 | `GroupCoordinationResult` | record | Command + approved command + outcome + rationale + conflicts |
| 7 | `CoordinatedAdjustmentAdapter` | class | Implements `ExecutorAdjustmentAdapter`; coordination interceptor decorator |
| 8 | `ExecutorGroup` | class | Group identity, member executors, budget validation at construction |
| 9 | `GroupCoordinationEntry` | record | executorId + command + result + budget snapshots |
| 10 | `GroupCoordinationHistory` | class | Thread-safe (CopyOnWriteArrayList) storage + queries |
| 11 | `CrossExecutorOscillationDetector` | class | 3 pattern detectors (lockstep, ping-pong, thrashing) |
| 12 | `GroupLoopOrchestrator` | class | Multi-loop lifecycle: startAll/pauseAll/resumeAll/stopAll/emergencyStopAll |
| 13 | `GroupHealth` | record | Aggregated group status: loop states, budget snapshot, warnings |

**Modified types** (1 existing, non-breaking):
| # | Type | Change |
|---|---|---|
| M1 | `AdjustmentFailureCode` | Add `COORDINATION_REJECTED`, `COORDINATION_CAPPED` values |

**Estimated size**: ~1200 lines source, ~700 lines test, ~30 test methods

**Dependencies**: `experiment.loop` (AdjustmentLoop, LoopConfig, LoopSession), `experiment.adjustment` (ExecutorAdjustmentAdapter, ScaleAdjustmentCommand, AdjustmentResult), `experiment.executor` (ManagedExecutor, ExecutorRegistry), `experiment.metrics` (EvidenceRecorder)

**Independent verification**: Can be compiled and tested independently with mock AdjustmentLoop instances. Budget enforcement, priority preemption, and coordination algorithms are purely computational — testable with mock executors (InMemoryAdjustableExecutorProbe).

### Change 2/2: `closed-loop-validation-and-evidence`

**Scope**: 3-way comparison runner (closed-loop vs static-policy vs baseline), validation scenario definition, comparison report with statistical significance, end-to-end validation evidence.

**New package**: `experiment.validation`

**Types** (8 new):
| # | Type | Kind | Key Responsibility |
|---|---|---|---|
| 1 | `ValidationMode` | enum | CLOSED_LOOP, STATIC_POLICY, BASELINE |
| 2 | `ValidationScenario` | record | Scenario definition: workload, executor config, candidate policies, duration |
| 3 | `ValidationRunResult` | record | Per-mode results: snapshot count, metrics map, duration, adjustment count |
| 4 | `MetricComparison` | record | Side-by-side metric values for all 3 modes with deltas |
| 5 | `StatisticalSignificance` | record | p-value, confidence interval, effect size, significance flag |
| 6 | `ValidationComparisonReport` | record | Full report: 3 run results + 7 comparisons + 14 significance tests |
| 7 | `ClosedLoopValidationRunner` | class | Orchestrates all 3 modes, computes comparisons, generates report |
| 8 | `StatisticalSignificanceCalculator` | class | In-project paired t-test + Cohen's d + 95% CI |

**Estimated size**: ~800 lines source, ~500 lines test, ~20 test methods

**Dependencies**: `experiment.coordination` (Change 1), `experiment.loop` (AdjustmentLoop, LoopConfig), `experiment.executor` (ManagedExecutor, ManagedExecutorConfig), `experiment.metrics` (LivePressureSampler, EvidenceRecorder), `experiment.policy` (ThresholdPolicyConfig), `experiment.classification` (PressureState)

**Independent verification**: Can be compiled and tested after Change 1 is installed. Validation runner needs Change 1 for `ExecutorGroup` types (if group validation is tested). Core validation logic (3-mode execution, metric computation, statistical tests) is algorithmically independent.

## Dependency Graph

```
Change 1: multi-executor-coordination
    │
    ├── experiment.loop (read-only)
    ├── experiment.adjustment (read-only + enum addition)
    ├── experiment.executor (read-only)
    └── experiment.metrics (read-only)

Change 2: closed-loop-validation-and-evidence
    │
    ├── experiment.coordination (Change 1 — optional, for group validation)
    ├── experiment.loop (read-only)
    ├── experiment.executor (read-only)
    ├── experiment.metrics (read-only)
    ├── experiment.policy (read-only)
    └── experiment.classification (read-only)
```

## Implementation Order

1. Change 1 first (no dependency on Change 2)
2. Change 2 second (depends on Change 1 for `ExecutorGroup`, `GroupCoordinator` types)

## Verification Gates

### Per Change
- `mvn test` passes with zero failures (all existing + new tests)
- All ACs mapped to test cases

### Cross-Change
- Change 1 regression: 857 existing tests pass + ~30 new tests
- Change 2 regression: 857 + ~30 + ~20 new tests pass

## OpenSpec Changes

| Change | OpenSpec name | Schema |
|---|---|---|
| 1/2 | `multi-executor-coordination` | superspec |
| 2/2 | `closed-loop-validation-and-evidence` | superspec |
