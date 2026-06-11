# closed-loop-experiment-verification

## Why

v0.7.0 changes 1/3 and 2/3 delivered `ManagedExecutor`, `ExecutorRegistry`, and `ManagedExecutorAdjustmentAdapter` — all the pieces needed to run a real experiment on a `ThreadPoolExecutor`. But no end-to-end test proves these pieces work together: scenario workload → metrics observation → policy evaluation → adjustment → post-adjustment verification.

This change creates the closed-loop experiment test that verifies the full pipeline on a real `ManagedExecutor`. It is the final change for v0.7.0 and proves the project's core thesis: the experiment infrastructure built over v0.1.0–v0.6.0 actually works on a real `ThreadPoolExecutor`.

## What Changes

### New

- **`ClosedLoopExperimentTest`** — single end-to-end test class orchestrating the full pipeline on a real `ManagedExecutor` + `ThreadPoolExecutor`

### Non-changes

- No new source types (uses existing `ManagedExecutor`, `ExecutorRegistry`, `ManagedExecutorAdjustmentAdapter`, `ThresholdPolicyEvaluator`, etc.)
- No modifications to `experiment.scenario`, `experiment.policy`, `experiment.analysis`, `experiment.metrics`, or `experiment.adjustment`
- No new external dependencies

## Capabilities

### New capability: `closed-loop-experiment-verification`

The closed-loop test:
1. Creates and registers a `ManagedExecutor(core=2, max=4, queue=10)`
2. Submits workload tasks to generate queue pressure
3. Reads executor state via `ManagedExecutorAdjustmentAdapter.currentState()`
4. Constructs a `PressureSnapshot` from the executor state
5. Evaluates policy via `ThresholdPolicyEvaluator` → `PolicyDecision`
6. Converts to `ScaleDecision` → `ScaleAdjustmentCommand`
7. Applies adjustment via the adapter with safety gate
8. Verifies: status=APPLIED, core pool size changed, max pool size preserved/accommodated

## Impact

- New test file: `ClosedLoopExperimentTest.java`
- Zero impact on existing tests
- No source modifications
