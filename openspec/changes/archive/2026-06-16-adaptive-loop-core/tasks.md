# Tasks: adaptive-loop-core

## 1. LoopState

- [ ] 1.1 Create `LoopState` enum in `experiment.loop`: IDLE, RUNNING, PAUSED, STOPPED, EMERGENCY_STOPPED
- [ ] 1.2 Write unit test: 5 enum values present, valueOf round-trip

## 2. LoopConfig

- [ ] 2.1 Create `LoopConfig` record with 8 fields (samplingIntervalMs, maxIterations, snapshotWindowSize, oscillationWindowSize, oscillationPatternThreshold, feedbackCalibrationWindow, emergencyStopThreshold, candidatePolicies)
- [ ] 2.2 Add compact constructor validation: samplingIntervalMs>=100, maxIterations>=0, snapshotWindowSize>=2, oscillationWindowSize>=4, patternThreshold>=1, calibrationWindow>=5, emergencyThreshold>=1, non-empty candidatePolicies
- [ ] 2.3 Add `defaults(List<ThresholdPolicyConfig>)` static factory
- [ ] 2.4 Write unit tests: valid construction, each invalid field, defaults() values, empty candidatePolicies throws

## 3. LoopSession

- [ ] 3.1 Create `LoopSession` record with fields: sessionId, loopConfig, startTime, endTime (Optional<Instant>), adjustmentCount, iterationCount, finalState, summary
- [ ] 3.2 Add compact constructor validation: non-blank sessionId, non-null loopConfig/startTime/finalState, adjustmentCount>=0, iterationCount>=adjustmentCount
- [ ] 3.3 Add `started(LoopConfig)` factory — UUID sessionId, Optional.empty() endTime, 0 counts
- [ ] 3.4 Add `ended(LoopState, int, int, String)` — Optional.of(Instant.now()) endTime
- [ ] 3.5 Write unit tests: valid construction, started() fields, ended() fields, invalid counts

## 4. AdjustmentDecision

- [ ] 4.1 Create `AdjustmentDecision` record with fields: classification, selectedScore (nullable), selectedPolicy (nullable), policyDecision, rationale, decidedAt
- [ ] 4.2 Add compact constructor: requireNonNull on classification/policyDecision/rationale/decidedAt; selectedScore/selectedPolicy nullable (for NO_OP)
- [ ] 4.3 Add `isNoOp()` — checks policyDecision.action() == HOLD
- [ ] 4.4 Add `toCommand(ManagedExecutor, String runId, Supplier<Instant>)` — delegates to ScaleAdjustmentCommand.create/noOp
- [ ] 4.5 Write unit tests: non-NO_OP construction, NO_OP construction (null score/policy), isNoOp(), toCommand() for both paths

## 5. TransitionLegality + PressureStateTransition

- [ ] 5.1 Create `TransitionLegality` enum: LEGAL, ANOMALOUS, ILLEGAL
- [ ] 5.2 Create `PressureStateTransition` record: from, to, timestamp, trigger, legality
- [ ] 5.3 Write unit test: enum values, record construction

## 6. PressureStateMachine

- [ ] 6.1 Create `PressureStateMachine` class with LEGAL/ANOMALOUS/ILLEGAL transition tables (Set<Entry<PressureState,PressureState>>)
- [ ] 6.2 Implement `isLegalTransition(from, to)` — "any→NORMAL always LEGAL", table lookup, default LEGAL
- [ ] 6.3 Implement `recordTransition(from, to, timestamp, trigger)` — compute legality, append to history
- [ ] 6.4 Implement `currentState()`, `recentTransitions(int)`, `transitionCount()`, `reset()`
- [ ] 6.5 Write unit tests: all LEGAL transitions, ANOMALOUS (OVERLOAD→NORMAL), ILLEGAL (RECOVERY→OVERLOAD), currentState after sequence, recentTransitions window, reset clears history

## 7. AdjustmentHistory + HistoryEntry

- [ ] 7.1 Create `HistoryEntry` record: decision, result, beforeClassification, afterClassification, recordedAt
- [ ] 7.2 Create `AdjustmentHistory` class with CopyOnWriteArrayList<HistoryEntry>
- [ ] 7.3 Implement `record(decision, result, before, after)`, `recent(int)`, `since(Instant)`, `totalAdjustmentCount()`, `successfulAdjustmentCount()`, `isEmpty()`, `clear()`
- [ ] 7.4 Implement `isImprovement(before, after)` — based on PressureState ordinal comparison
- [ ] 7.5 Write unit tests: record+recent, successfulAdjustmentCount (improvement/maintained/degraded), since() filtering, isEmpty, clear, thread-safety (concurrent record)

## 8. LoopEvidenceRecorder + LoopIterationEvidence

- [ ] 8.1 Create `LoopIterationEvidence` record: sessionId, iterationIndex, decision, result (nullable), beforeClassification (nullable), recordedAt
- [ ] 8.2 Create `LoopEvidenceRecorder` interface: recordIteration(), recordSessionStart(), recordSessionEnd(), getIterationEvidence()
- [ ] 8.3 Create stub `NoOpLoopEvidenceRecorder` implementing interface (no-ops, empty list returns)
- [ ] 8.4 Write unit test: stub methods don't throw

## 9. DecisionOrchestrator

- [ ] 9.1 Create `DecisionOrchestrator` class: classifier, ranker, evaluator, classifierConfig (all final — immutable)
- [ ] 9.2 Implement `decide(snapshots, candidates, executor, runId)` — 7-step pipeline per SR §4.9
- [ ] 9.3 Handle empty snapshots → NO_OP decision
- [ ] 9.4 Handle empty candidates → NO_OP decision
- [ ] 9.5 Construct PolicyEvaluationInput using lastSnapshot.timestamp() (not Instant.now())
- [ ] 9.6 Write unit tests: OVERLOAD→aggressive policy selected, empty snapshots→NO_OP, empty candidates→NO_OP, isNoOp=true for HOLD action

## 10. AdjustmentLoop

- [ ] 10.1 Create `AdjustmentLoop` class with 11 constructor parameters + fields per SR §4.10
- [ ] 10.2 Implement lifecycle: start()/pause()/resume()/stop()/emergencyStop()/reset() with state transition validation
- [ ] 10.3 Implement `runLoop()` — 16-step main loop (wait→snapshots→decide→oscillation→command→safety gate→apply→record→calibrate→maxIter)
- [ ] 10.4 Construct runtime ReadinessAssessment (READY, "runtime-loop") in start()
- [ ] 10.5 Handle pause via wait/notify on pauseLock
- [ ] 10.6 Handle emergency stop from oscillation detection
- [ ] 10.7 Runtime exception per iteration → catch and continue
- [ ] 10.8 Write unit tests: lifecycle (IDLE→RUNNING→PAUSED→STOPPED), illegal transitions throw, reset() from STOPPED, NO_OP decision skip, SafetyGate REJECTED handling, maxIterations reached

## 11. Full Test Verification

- [ ] 11.1 Run `mvn test` — verify all 774 existing tests pass (zero regression)
- [ ] 11.2 Verify all new Change 1 tests pass
