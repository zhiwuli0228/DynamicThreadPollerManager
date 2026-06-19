## Why

v0.14.0 delivered autonomous closed-loop adjustment for a single ManagedExecutor. But real applications have multiple thread pools running concurrently. When multiple AdjustmentLoop instances run independently, they can make conflicting decisions — e.g., two loops both scale up, exceeding a shared resource budget. There is no mechanism to coordinate adjustments across executors. This change builds the multi-executor coordination layer: group abstraction, shared resource budget, priority-based conflict resolution, and group lifecycle management (v0.14.0 DFR-01).

## What Changes

- **Group abstraction**: `ExecutorGroup` + `ExecutorGroupConfig` — associate multiple ManagedExecutors under shared identity and resource budget
- **Resource budgeting**: `ResourceBudget` — thread-safe tracking of per-executor allocation with atomic reserve/release
- **Priority model**: `AdjustmentPriority` (CRITICAL/HIGH/NORMAL/LOW) — determines resource allocation precedence under contention
- **Central coordination**: `GroupCoordinator` + `GroupCoordinationResult` + `CoordinationOutcome` — intercepts scale-up commands, evaluates against budget, resolves conflicts by priority, returns approved/rejected/capped decisions
- **Coordinated adapter**: `CoordinatedAdjustmentAdapter` — decorator implementing `ExecutorAdjustmentAdapter`, injects coordination before delegation, no modification to v0.14.0 components
- **Group lifecycle**: `GroupLoopOrchestrator` + `GroupHealth` — manages multiple AdjustmentLoop instances (startAll/pauseAll/stopAll/emergencyStopAll) with aggregated health status
- **Coordination history**: `GroupCoordinationHistory` + `GroupCoordinationEntry` — thread-safe recording of all coordination decisions with budget snapshots
- **Cross-executor oscillation detection**: `CrossExecutorOscillationDetector` — detects lockstep counter-adjustment, resource ping-pong, and priority thrashing patterns

All new components in `experiment.coordination` package. One non-breaking addition to existing code: `AdjustmentFailureCode` enum gets `COORDINATION_REJECTED` and `COORDINATION_CAPPED` values.

## Capabilities

### New Capabilities
- `executor-group`: Group multiple ManagedExecutors under shared resource budget with construction-time validation
- `resource-budget`: Thread-safe shared resource tracking with atomic reserve/release and invariant enforcement
- `group-coordinator`: Centralized coordination with 4-outcome model (APPROVED_AS_IS/MODIFIED/REJECTED/CAPPED), priority-based preemption
- `coordinated-adapter`: Decorator implementing ExecutorAdjustmentAdapter, zero modification to AdjustmentLoop
- `group-orchestrator`: Multi-loop lifecycle management with aggregated GroupHealth
- `cross-executor-oscillation-detection`: Detection of cross-executor oscillation patterns (advisory, non-blocking)

### Modified Capabilities
- `adjustment-failure-codes`: Two new enum values added (COORDINATION_REJECTED, COORDINATION_CAPPED) — non-breaking addition

## Impact

- **New source files**: 13 types in `experiment.coordination` (~1200 lines)
- **Modified source files**: 1 (`AdjustmentFailureCode` — 2 enum values added)
- **New test files**: ~8 test classes (~700 lines)
- **Breaking changes**: None
- **Dependencies**: No new external dependencies
- **Package**: `com.zhiwu.dynamicthreadpollermanager.experiment.coordination`
