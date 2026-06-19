# v0.15.0 SR Review Disposition

## Header

- Document type: SR review disposition
- Version: `v0.15.0`
- Date: `2026-06-17`
- Status: `DISPOSITION_COMPLETE`
- Review artifact: `21-sr-review.md`

## Disposition Summary

| Finding | Severity | Disposition | Action |
|---|---|---|---|
| P0-01 | P0 | ACCEPTED | Fix `LivePressureSamplerConfig.defaultConfig()` → `defaults(runId)` |
| P0-02 | P0 | ACCEPTED | Add `Supplier<Instant> clock` to GroupCoordinator constructor |
| P1-01 | P1 | ACCEPTED | Redesign executor identity: use String names throughout |
| P1-02 | P1 | ACCEPTED | Add `COORDINATION_REJECTED`/`COORDINATION_CAPPED` to enum; document in SR §6 |
| P1-03 | P1 | ACCEPTED (Option B) | Simplify GroupHealth: replace `currentPressureStates` with `loopStates` |
| P1-04 | P1 | CLOSED (no action) | Verified: all cross-package types are `public` — no visibility issue |
| P2-01 | P2 | ACCEPTED (residual) | Validation runner placeholder adequate for SR; details in Change 2 implementation |
| P2-02 | P2 | ACCEPTED | Specify Abramowitz & Stegun 26.2.17 in SR §2.13 |
| P2-03 | P2 | ACCEPTED (note) | Add implementation note about `current != target` requirement |
| P3-01 | P3 | ACCEPTED (residual) | Trivial — resolved at implementation |
| P3-02 | P3 | CLOSED (no action) | Verified: OscillationDetector and FeedbackCalibrator are `public` |

## Detailed Disposition

### P0-01: LivePressureSamplerConfig Factory Method

**Fix**: Replace `LivePressureSamplerConfig.defaultConfig()` → `LivePressureSamplerConfig.defaults(runId)` in all three mode methods in SR §2.12.
**SR updated**: Yes.

### P0-02: Missing Clock in GroupCoordinator Constructor

**Fix**: Add `Supplier<Instant> clock` as 6th constructor parameter. All `clock.get()` references in `coordinate()` body are now valid.
**SR updated**: Yes.

### P1-01: Executor Identity Model

**Fix**: Adopt explicit String identifiers throughout:
- `ExecutorGroup` stores `Map<String, ManagedExecutor>` (name → executor), with names from `ExecutorRegistry` or user-provided
- `CoordinatedAdjustmentAdapter` receives `String executorName` at construction
- `GroupCoordinator.identifyExecutor()` removed; executor name passed directly by adapter
- `ResourceBudget` allocations keyed by executor name (already the case)
**SR updated**: Yes — §2.4, §2.6, §2.7 updated with explicit executor names.

### P1-02: AdjustmentFailureCode Enum Addition

**Fix**: Add `COORDINATION_REJECTED` and `COORDINATION_CAPPED` to `AdjustmentFailureCode` enum. Update SR §6 to document this as an allowed modification.
**SR updated**: Yes.

### P1-03: GroupHealth Pressure States

**Fix (Option B)**: Replace `Map<String, PressureState> currentPressureStates` with `Map<String, LoopState> loopStates` in `GroupHealth`. Loop states are directly available via `loop.getState()`. Pressure states can be added in v0.16.0 when needed.
**SR updated**: Yes.

### P2-02: Statistical Algorithm Specification

**Fix**: Add to SR §2.13: use Abramowitz & Stegun 26.2.17 for standard normal CDF, with coefficients c1=0.196854, c2=0.115194, c3=0.000344, c4=0.019527. t-to-z transformation: `z = t * (1 - 1/(4*df)) / sqrt(1 + t²/(2*df))`.
**SR updated**: Yes — added to §2.13 inline.

## SR Changes Applied

| Section | Change |
|---|---|
| §2.4 GroupCoordinator | Added `Supplier<Instant> clock` param; removed `identifyExecutor()`, use passed executor name |
| §2.6 CoordinatedAdjustmentAdapter | Added `String executorName` param; removed `identifyExecutor()` |
| §2.7 ExecutorGroup | Changed to `Map<String, ManagedExecutor>`; construction accepts names |
| §2.10 GroupHealth | Replaced `currentPressureStates` with `loopStates: Map<String, LoopState>` |
| §2.12 ClosedLoopValidationRunner | Fixed `LivePressureSamplerConfig.defaults(runId)` |
| §2.13 StatisticalSignificanceCalculator | Added A&S 26.2.17 specification |
| §3.2 Failure Codes | Added `COORDINATION_REJECTED`, `COORDINATION_CAPPED` |
| §6 Non-Scope | Documented `AdjustmentFailureCode` enum addition as allowed modification |

## Disposition Conclusion

All 11 findings disposed:
- **P0**: 2 accepted and fixed (factory method, clock parameter)
- **P1**: 2 accepted and fixed (executor identity, GroupHealth), 1 accepted and fixed (enum addition), 1 closed no-action
- **P2**: 2 accepted (algorithm spec), 1 accepted (implementation note)
- **P3**: 1 residual, 1 closed no-action

Ready for closure verification.
