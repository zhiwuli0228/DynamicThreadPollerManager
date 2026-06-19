## Why

v0.13.0 delivered the diagnostic layer — the system can classify pressure states and score/rank policies. But there is no component that autonomously orchestrates the full pipeline: sample → classify → score → select → evaluate → apply → observe. The system can diagnose but cannot act on the diagnosis. This change builds the core closed-loop controller that connects diagnosis to action.

## What Changes

- **Loop lifecycle**: `LoopState` enum (IDLE/RUNNING/PAUSED/STOPPED/EMERGENCY_STOPPED) + `LoopConfig` record + `LoopSession` record
- **Decision orchestration**: `DecisionOrchestrator` that chains classify → rank → evaluate → `AdjustmentDecision`
- **State transition model**: `PressureStateMachine` + `PressureStateTransition` + `TransitionLegality` defining legal/anomalous/illegal transitions between 6 pressure states (v0.13.0 DFR-02)
- **Adjustment history**: `AdjustmentHistory` + `HistoryEntry` for recording and querying adjustment outcomes
- **Closed-loop controller**: `AdjustmentLoop` managing lifecycle and executing the main loop (wait → snapshots → decide → safety → apply → record)
- **Evidence recording interface**: `LoopEvidenceRecorder` + `LoopIterationEvidence` (stub implementation)

All new components are in `experiment.loop` package. No modification to existing code (Change 2 adds `ThresholdPolicyScorer` getters and provides `OscillationDetector`/`FeedbackCalibrator`).

## Capabilities

### New Capabilities
- `adaptive-loop-lifecycle`: Loop state machine with 5 states and controlled transitions (start/pause/resume/stop/emergencyStop/reset)
- `decision-orchestration`: Pipeline orchestrating pressure classification → policy ranking → policy evaluation → adjustment decision
- `pressure-state-machine`: Formal state transition model with legal/anomalous/illegal classification (v0.13.0 DFR-02 closure)
- `adjustment-history`: Thread-safe history storage with success/failure tracking and time-based queries
- `closed-loop-controller`: Autonomous adjustment loop with configurable sampling interval, safety gate integration, and iteration limits

### Modified Capabilities
- (none — Change 1 is purely additive)

## Impact

- **New source files**: 13 records/classes/interfaces in `experiment.loop` (~1200 lines)
- **Modified source files**: None
- **New test files**: ~12 unit tests (~700 lines)
- **No changes to**: Any existing package or interface
- **Breaking changes**: None
- **Dependencies**: No new external dependencies
