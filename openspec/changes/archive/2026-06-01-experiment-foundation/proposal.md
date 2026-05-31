## Why

The project needs a small but explicit runtime foundation before any metrics, scenarios, policy, or executor work can be built safely. Without that baseline, the later changes would duplicate the core experiment model and risk coupling the control logic to the wrong runtime shape.

## What Changes

**Experiment foundation**
- From: no shared experiment runtime model exists.
- To: a minimal experiment foundation will define `ExperimentRun`, `LoadScenario`, `PressureSnapshot`, `ControlPolicy`, `ScaleDecision`, `AdjustmentEvent`, `ResultSeries`, and `AnalysisSummary`, plus a small lifecycle coordinator.
- Reason: later changes need a stable contract boundary.
- Impact: non-breaking for current code because this is a new package and capability.

**Lifecycle ownership**
- From: no explicit run lifecycle is modeled.
- To: experiment runs will have a deterministic start, stop, and finalization flow.
- Reason: later scenario and metrics changes need a reliable runtime container.
- Impact: non-breaking, but it establishes the baseline behavior that future changes must preserve.

**Extension points**
- From: later work would otherwise have to guess where to plug in metrics, scenario, policy, and executor logic.
- To: the foundation will expose explicit extension points without implementing mutation or sampling itself.
- Reason: the current implementation model is weak, so the first change must reduce future coupling.
- Impact: non-breaking and intentionally conservative.

## Capabilities

### New Capabilities
- `experiment-foundation`: the experiment runtime skeleton, shared domain model, lifecycle coordinator, and extension points used by later v0.1.0 changes.

### Modified Capabilities
- none

## Impact

- Affected code: new `openspec/changes/experiment-foundation/specs/experiment-foundation/` spec package and later runtime foundation classes.
- Affected APIs: none yet; the change defines internal contracts first.
- Affected dependencies: none planned for the foundation itself.
- Affected systems: future metrics, scenario, policy, and executor changes will depend on this capability.
