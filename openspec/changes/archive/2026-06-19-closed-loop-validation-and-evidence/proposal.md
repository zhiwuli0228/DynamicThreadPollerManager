## Why

v0.14.0 delivered autonomous closed-loop adjustment for a single executor, and v0.15.0 Change 1 adds multi-executor coordination. But we have not empirically proven that closed-loop adjustment outperforms alternatives. v0.12.0's comparison infrastructure has been idle since delivery. This change builds the validation layer: a 3-way comparison runner (closed-loop vs best-static-policy vs no-adjustment baseline) that produces statistically-validated evidence (v0.14.0 DFR-04).

## What Changes

- **Validation scenario**: `ValidationScenario` + `ValidationMode` — defines workload, executor config, candidate policies, and duration for comparison
- **3-way comparison runner**: `ClosedLoopValidationRunner` — orchestrates closed-loop, static-policy, and baseline modes sequentially with identical workload
- **Comparison report**: `ValidationComparisonReport` + `ValidationRunResult` + `MetricComparison` — side-by-side metrics with deltas across 7 metrics
- **Statistical significance**: `StatisticalSignificance` + `StatisticalSignificanceCalculator` — in-project paired t-test, Cohen's d, 95% confidence intervals (no external dependencies)

All new components in `experiment.validation` package. No modification to existing code.

## Capabilities

### New Capabilities
- `closed-loop-validation`: 3-way comparison runner producing side-by-side evidence of closed-loop effectiveness
- `statistical-significance`: In-project statistical tests (paired t-test, Cohen's d, 95% CI) using Abramowitz & Stegun approximation

### Modified Capabilities
- (none — Change 2 is purely additive)

## Impact

- **New source files**: 8 types in `experiment.validation` (~800 lines)
- **Modified source files**: None
- **New test files**: ~4 test classes (~500 lines)
- **Breaking changes**: None
- **Dependencies**: No new external dependencies
- **Package**: `com.zhiwu.dynamicthreadpollermanager.experiment.validation`
