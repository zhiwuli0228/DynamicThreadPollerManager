# Design: adaptive-loop-core

## Input Baseline

- SR: `docs/04-development/versions/v0.14.0/20-sr.md` (post-disposition, closure verified)
- IR: `docs/04-development/versions/v0.14.0/10-ir.md` (IR-v0.14-001 through IR-v0.14-004, IR-v0.14-006, IR-v0.14-008, IR-v0.14-009)

## Architecture

All new components in `com.zhiwu.dynamicthreadpollermanager.experiment.loop`.

Dependency direction: `experiment.loop` → existing packages (classification, policy, adjustment, metrics, analysis, executor). No reverse dependencies.

## Component Design

See SR §4 for full pseudocode. Key design points:

1. **LoopState**: 5-state enum with transitions enforced by AdjustmentLoop methods
2. **LoopConfig**: 8-field record (no cooldown — delegated to SafetyGate)
3. **LoopSession**: Immutable session record with Optional<Instant> endTime
4. **AdjustmentDecision**: Nullable selectedScore/selectedPolicy (null only for NO_OP)
5. **PressureStateMachine**: Set<Entry<>> lookup for legal/anomalous/illegal transitions
6. **AdjustmentHistory**: CopyOnWriteArrayList for thread-safe concurrent record/query
7. **DecisionOrchestrator**: Immutable; 7-step pipeline (classify→rank→find→input→evaluate→assemble)
8. **AdjustmentLoop**: Main controller with 16-step loop; orchestrator rebuilt on calibration

## Test Strategy

- Unit tests for all records/enums/classes with mock dependencies
- Mock OscillationDetector (always returns false) for AdjustmentLoop tests
- Mock FeedbackCalibrator for calibration trigger tests
- Mock LoopEvidenceRecorder for evidence recording tests
- Real PressureStateMachine/AdjustmentHistory used directly (no mocks needed)
