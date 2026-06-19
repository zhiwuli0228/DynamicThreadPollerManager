# Plan: closed-loop-validation-and-evidence

## Implementation Order

1. **ValidationMode + ValidationScenario** (no dependencies)
2. **ValidationRunResult + MetricComparison + StatisticalSignificance + ValidationComparisonReport** (record types, no logic)
3. **StatisticalSignificanceCalculator** (pure computation, no dependencies except JDK Math)
4. **ClosedLoopValidationRunner** (depends on all above + AdjustmentLoop + LivePressureSampler + ManagedExecutor)
5. **Full test verification**

## Parallelism Opportunities

- Tasks 1-2 can be implemented in parallel (independent type definitions)
- Task 3 can proceed once basic types exist (only needs double[] arrays)
- Task 4 is sequential (depends on 3 + Change 1 + existing infrastructure)
- Task 5 after all

## Verification Gate

- `mvn test` passes with zero failures (857 + Change 1 + new)
- Statistical calculator produces correct p-values (±0.01 vs reference)
- Validation runner executes all 3 modes without errors
- At least 2 metrics show closed-loop outperforming baseline with statistical significance
