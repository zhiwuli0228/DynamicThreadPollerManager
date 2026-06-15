# v0.14.0 Change Decomposition Plan

## Header

- Version name: `v0.14.0`
- Authorized by: `docs/00-project/current-state.md` (SR_CLOSED → READY_FOR_CHANGE_DECOMPOSITION)
- Decomposition date: `2026-06-14`
- SR baseline: `20-sr.md` (post-disposition, closure verified)
- Number of changes: 2

## Change 1: adaptive-loop-core

**Purpose**: 闭环核心骨架 — 生命周期管理、决策编排、状态机、调整历史。Change 1 可独立编译和测试（振荡检测和权重校准可 mock/stub）。

**Scope**:
- `LoopState` enum
- `LoopConfig` record
- `LoopSession` record
- `AdjustmentDecision` record
- `TransitionLegality` enum
- `PressureStateTransition` record
- `PressureStateMachine` class
- `AdjustmentHistory` class + `HistoryEntry` record
- `DecisionOrchestrator` class
- `AdjustmentLoop` class
- `LoopEvidenceRecorder` interface
- `LoopIterationEvidence` record
- Unit tests for all above

**Dependencies**: `PressureClassifier`, `PolicyRanker`, `PolicyEvaluator`, `ThresholdPolicyConfig`, `PolicyDecision`, `PolicyEvaluationInput`, `PolicyAction`, `GateStatus`, `PressureClassification`, `PressureState`, `NormalizedPressureMetrics`, `ClassifierConfig`, `ExecutorAdjustmentAdapter`, `RuntimeAdjustmentSafetyGate`, `SafetyGateDecision`, `ScaleAdjustmentCommand`, `AdjustmentResult`, `ExecutorStateSnapshot`, `EvidenceRecorder`, `ObservedSnapshot`, `PressureSnapshot`, `ReadinessAssessment`, `ReadinessStatus`, `ManagedExecutor`

**Deliverable files** (all `experiment.loop`):
- `LoopState.java`
- `LoopConfig.java`
- `LoopSession.java`
- `AdjustmentDecision.java`
- `TransitionLegality.java`
- `PressureStateTransition.java`
- `PressureStateMachine.java`
- `AdjustmentHistory.java`
- `HistoryEntry.java`
- `DecisionOrchestrator.java`
- `AdjustmentLoop.java`
- `LoopEvidenceRecorder.java`
- `LoopIterationEvidence.java`

## Change 2: oscillation-guard-and-loop-verification

**Purpose**: 闭环安全保障 — 振荡检测、权重校准、证据记录实现、端到端验证。Change 2 依赖 Change 1 的类型但算法独立。

**Scope**:
- `OscillationDetector` class
- `FeedbackCalibrator` class
- `LoopEvidenceRecorder` implementation (e.g. `InMemoryLoopEvidenceRecorder`)
- `ThresholdPolicyScorer` modification (4 package-visible weight getters)
- End-to-end integration tests (≥5 cycle closed-loop run, oscillation emergency stop)
- All Change 2 unit tests

**Dependencies**: Change 1 types (`AdjustmentLoop`, `AdjustmentDecision`, `AdjustmentHistory`, `HistoryEntry`, `LoopConfig`, `LoopSession`, `LoopEvidenceRecorder`)

**Deliverable files**:
- `OscillationDetector.java` (`experiment.loop`)
- `FeedbackCalibrator.java` (`experiment.loop`)
- `InMemoryLoopEvidenceRecorder.java` (`experiment.loop`)
- `ThresholdPolicyScorer.java` modification (~4 lines, `experiment.classification`)
- `OscillationDetectorTest.java`
- `FeedbackCalibratorTest.java`
- `LoopEndToEndTest.java` (integration)

## Independent Verifiability

| Check | Change 1 | Change 2 |
|---|---|---|
| Independent compilation | ✓ (all deps are existing interfaces) | ✓ (depends on Change 1 types) |
| Independent `mvn test` | ✓ (mock OscillationDetector/FeedbackCalibrator) | ✓ (Change 1 types present) |
| Depends on other change's source | No | Yes (Change 1 types) |
| Ordered delivery | First | Second |

**Decision**: Sequential delivery (Change 1 → Change 2), consistent with v0.11.0/v0.12.0/v0.13.0 pattern.
