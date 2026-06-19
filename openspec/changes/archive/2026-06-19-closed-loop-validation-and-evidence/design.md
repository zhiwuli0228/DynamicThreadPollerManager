# Design: closed-loop-validation-and-evidence

## Input Baseline

- SR: `docs/04-development/versions/v0.15.0/20-sr.md` §2.11-2.13
- IR: `docs/04-development/versions/v0.15.0/10-ir.md` (IR-v0.15-009, IR-v0.15-010, IR-v0.15-012)
- Decision log: `docs/04-development/versions/v0.15.0/decision-log.md` (D3, D7)
- Change 1: `multi-executor-coordination` (provides ExecutorGroup, GroupCoordinator for optional group validation)

## Architecture

All new components in `com.zhiwu.dynamicthreadpollermanager.experiment.validation`.

Dependency direction: `experiment.validation` → `experiment.loop`, `experiment.executor`, `experiment.metrics`, `experiment.policy`, `experiment.classification`, `experiment.model`, `experiment.coordination` (optional). No reverse dependencies.

## Component Design

1. **ValidationMode**: 3-value enum (CLOSED_LOOP, STATIC_POLICY, BASELINE)
2. **ValidationScenario**: 8-field record — scenarioId, workload (LoadScenario), executorConfig (ManagedExecutorConfig), candidatePolicies, bestStaticPolicy, duration (min 30s), minIterations (min 5), warmupPeriodMs (min 1000)
3. **ValidationRunResult**: Record per mode — mode, runId, snapshotCount, metrics map, durationMs, adjustmentCount, finalPressureState
4. **MetricComparison**: Side-by-side metric values with positive-delta-means-closed-loop-better convention
5. **StatisticalSignificance**: p-value, 95% CI, Cohen's d, significance flag, sample size
6. **ValidationComparisonReport**: Full report aggregating 3 run results + 7 metric comparisons + 14 significance tests
7. **ClosedLoopValidationRunner**: Core orchestrator — creates executor, runs workload, collects snapshots, computes metrics per mode. Uses `LivePressureSampler` with `LivePressureSamplerConfig.defaults(runId)`. Uses `ManagedExecutorConfig.toManagedExecutor()` to create executors.
8. **StatisticalSignificanceCalculator**: Static utility — paired t-test using Abramowitz & Stegun 26.2.17 for normal CDF; t-to-z transformation: `z = t * (1 - 1/(4*df)) / sqrt(1 + t²/(2*df))`; Cohen's d as `meanDiff / pooledStdDev`.

## Test Strategy

- Unit tests for ValidationScenario construction validation
- Integration tests: 3-way validation with real ManagedExecutor, short duration (30s)
- Statistical accuracy tests: compare p-values against reference values at df=[5,10,30]
- Edge case tests: n<2, zero variance, all identical differences
- Regression: all 857 + Change 1 tests pass
