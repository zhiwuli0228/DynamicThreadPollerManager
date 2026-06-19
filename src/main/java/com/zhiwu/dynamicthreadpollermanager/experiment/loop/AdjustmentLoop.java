package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessAssessment;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.EvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyEvaluator;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PolicyRanker;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Autonomous closed-loop adjustment controller.
 *
 * <p>Lifecycle: IDLE → RUNNING ⇄ PAUSED → STOPPED/EMERGENCY_STOPPED → (reset) → IDLE.
 * Cooldown is delegated to {@link RuntimeAdjustmentSafetyGate}.
 * Oscillation detection and feedback calibration are provided by Change 2
 * (stubbed here as no-op / always-false).
 */
public final class AdjustmentLoop {

    private final LoopConfig config;
    private volatile DecisionOrchestrator orchestrator;
    private final PressureClassifier classifier;
    private final PolicyEvaluator evaluator;
    private final ClassifierConfig classifierConfig;
    private final ExecutorAdjustmentAdapter adapter;
    private final RuntimeAdjustmentSafetyGate safetyGate;
    private final AdjustmentHistory history;
    private final LoopEvidenceRecorder loopEvidenceRecorder;
    private final PressureStateMachine stateMachine;
    private final EvidenceRecorder evidenceRecorder;
    private final Supplier<Instant> clock;

    // Change 2 components — stubbed in Change 1
    private final OscillationDetector oscillationDetector;
    private final FeedbackCalibrator calibrator;
    private final AntiOscillationGuard antiOscillationGuard;

    private volatile LoopState state = LoopState.IDLE;
    private LoopSession currentSession;
    private Thread loopThread;
    private volatile int consecutiveOscillations;

    private final Object pauseLock = new Object();
    private volatile boolean paused = false;
    private volatile int currentIteration = 0;

    public AdjustmentLoop(
            LoopConfig config,
            DecisionOrchestrator orchestrator,
            PressureClassifier classifier,
            PolicyEvaluator evaluator,
            ClassifierConfig classifierConfig,
            ExecutorAdjustmentAdapter adapter,
            RuntimeAdjustmentSafetyGate safetyGate,
            AdjustmentHistory history,
            LoopEvidenceRecorder loopEvidenceRecorder,
            PressureStateMachine stateMachine,
            EvidenceRecorder evidenceRecorder,
            Supplier<Instant> clock,
            OscillationDetector oscillationDetector,
            FeedbackCalibrator calibrator,
            AntiOscillationGuard antiOscillationGuard) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator must not be null");
        this.classifier = Objects.requireNonNull(classifier, "classifier must not be null");
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
        this.classifierConfig = Objects.requireNonNull(classifierConfig, "classifierConfig must not be null");
        this.adapter = Objects.requireNonNull(adapter, "adapter must not be null");
        this.safetyGate = Objects.requireNonNull(safetyGate, "safetyGate must not be null");
        this.history = Objects.requireNonNull(history, "history must not be null");
        this.loopEvidenceRecorder = Objects.requireNonNull(loopEvidenceRecorder, "loopEvidenceRecorder must not be null");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine must not be null");
        this.evidenceRecorder = Objects.requireNonNull(evidenceRecorder, "evidenceRecorder must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.oscillationDetector = Objects.requireNonNull(oscillationDetector, "oscillationDetector must not be null");
        this.calibrator = Objects.requireNonNull(calibrator, "calibrator must not be null");
        this.antiOscillationGuard = antiOscillationGuard;
    }

    // --- Lifecycle ---

    public LoopSession start(ManagedExecutor executor) {
        if (state != LoopState.IDLE) {
            throw new IllegalStateException("Cannot start: state is " + state);
        }
        state = LoopState.RUNNING;
        paused = false;
        consecutiveOscillations = 0;

        currentSession = LoopSession.started(config);
        loopEvidenceRecorder.recordSessionStart(currentSession);

        loopThread = new Thread(() -> runLoop(executor),
                "adjustment-loop-" + currentSession.sessionId());
        loopThread.setDaemon(true);
        loopThread.start();

        return currentSession;
    }

    public void pause() {
        if (state != LoopState.RUNNING) {
            throw new IllegalStateException("Cannot pause: state is " + state);
        }
        state = LoopState.PAUSED;
        paused = true;
    }

    public void resume() {
        if (state != LoopState.PAUSED) {
            throw new IllegalStateException("Cannot resume: state is " + state);
        }
        state = LoopState.RUNNING;
        paused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
    }

    public LoopSession stop() {
        if (state != LoopState.RUNNING && state != LoopState.PAUSED) {
            throw new IllegalStateException("Cannot stop: state is " + state);
        }
        state = LoopState.STOPPED;
        unpauseAndInterrupt();
        return finalizeSession(LoopState.STOPPED, "normal stop");
    }

    public LoopSession emergencyStop(String reason) {
        if (state != LoopState.RUNNING && state != LoopState.PAUSED) {
            throw new IllegalStateException("Cannot emergency stop: state is " + state);
        }
        state = LoopState.EMERGENCY_STOPPED;
        unpauseAndInterrupt();
        return finalizeSession(LoopState.EMERGENCY_STOPPED,
                "emergency stop: " + reason);
    }

    public void reset() {
        if (state != LoopState.STOPPED && state != LoopState.EMERGENCY_STOPPED) {
            throw new IllegalStateException("Cannot reset: state is " + state);
        }
        state = LoopState.IDLE;
        history.clear();
        stateMachine.reset();
        currentSession = null;
        loopThread = null;
        consecutiveOscillations = 0;
        currentIteration = 0;
    }

    public LoopState getState() { return state; }
    public LoopSession getCurrentSession() { return currentSession; }
    public AdjustmentHistory getHistory() { return history; }

    // --- Main loop ---

    private void runLoop(ManagedExecutor executor) {
        PressureClassification previousClassification = null;
        currentIteration = 0;

        ReadinessAssessment loopReadiness = new ReadinessAssessment(
                ReadinessStatus.READY,
                List.of(), List.of(), List.of(), List.of(),
                "runtime-loop",
                List.of(currentSession.sessionId()));

        while (state == LoopState.RUNNING || state == LoopState.PAUSED) {
            if (paused) {
                synchronized (pauseLock) {
                    try { pauseLock.wait(config.samplingIntervalMs()); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                }
                continue;
            }

            currentIteration++;

            try {
                // Step 1: sleep
                Thread.sleep(config.samplingIntervalMs());

                // Step 1.5: max iterations check (must be before continue paths)
                if (config.maxIterations() > 0 && currentIteration >= config.maxIterations()) {
                    stop();
                    break;
                }

                // Step 2: get recent snapshots
                List<ObservedSnapshot> allSnapshots = evidenceRecorder.snapshots(
                        currentSession.sessionId());
                int from = Math.max(0, allSnapshots.size() - config.snapshotWindowSize());
                List<ObservedSnapshot> recent = allSnapshots.subList(from, allSnapshots.size());
                if (recent.isEmpty()) continue;

                // Step 3: decide
                AdjustmentDecision decision = orchestrator.decide(
                        recent, config.candidatePolicies(),
                        executor, currentSession.sessionId());

                // Step 4: skip no-op
                if (decision.isNoOp()) {
                    loopEvidenceRecorder.recordIteration(currentSession, currentIteration,
                            decision, null, previousClassification);
                    continue;
                }

                // Step 5-6: oscillation check
                if (oscillationDetector.wouldOscillate(decision, history)) {
                    consecutiveOscillations++;
                    if (consecutiveOscillations >= config.emergencyStopThreshold()) {
                        emergencyStop("oscillation detected: "
                                + oscillationDetector.detectedPattern(history).orElse("unknown"));
                        break;
                    }
                    continue;
                }
                consecutiveOscillations = 0;

                // Step 7: build command
                ScaleAdjustmentCommand command = decision.toCommand(
                        executor, currentSession.sessionId(), clock);

                // Step 7.5: anti-oscillation guard (between oscillation check and safety gate)
                if (antiOscillationGuard != null) {
                    SafetyGateDecision guardDecision = antiOscillationGuard.evaluate(
                            decision, history, command.isEmergencyRollback());
                    if (!guardDecision.isAllowed()) {
                        loopEvidenceRecorder.recordIteration(currentSession, currentIteration,
                                decision, null, previousClassification);
                        continue;
                    }
                }

                // Step 8: safety gate
                ExecutorStateSnapshot executorState = adapter.currentState();
                SafetyGateDecision gateDecision = safetyGate.evaluate(
                        command, executorState, loopReadiness);

                // Step 9: check gate outcome
                if (gateDecision.outcome() == SafetyGateDecision.Outcome.REJECTED) {
                    loopEvidenceRecorder.recordIteration(currentSession, currentIteration,
                            decision, null, previousClassification);
                    continue;
                }
                if (gateDecision.outcome() == SafetyGateDecision.Outcome.NO_OP) {
                    continue;
                }

                // Step 10: apply
                AdjustmentResult result = adapter.apply(command);

                // Step 11: record applied → cooldown
                safetyGate.recordApplied(gateDecision);

                // Step 12: record in history with correct before/after
                PressureClassification beforeClass = previousClassification;
                PressureClassification afterClass = decision.classification();
                history.record(decision, result,
                        beforeClass != null ? beforeClass : afterClass,
                        afterClass);
                previousClassification = afterClass;

                // Step 13: state transition tracking
                if (beforeClass != null) {
                    stateMachine.recordTransition(
                            beforeClass.state(),
                            decision.classification().state(),
                            clock.get(),
                            "loop-decision:" + decision.selectedScore().policyId());
                }

                // Step 14: evidence
                loopEvidenceRecorder.recordIteration(currentSession, currentIteration,
                        decision, result, beforeClass);

                // Step 15: feedback calibration trigger
                if (history.totalAdjustmentCount() > 0
                        && history.totalAdjustmentCount() % config.feedbackCalibrationWindow() == 0) {
                    ThresholdPolicyScorer currentScorer =
                            (ThresholdPolicyScorer) orchestrator.ranker().scorer();
                    ThresholdPolicyScorer newScorer = calibrator.calibrate(
                            history, currentScorer, config.feedbackCalibrationWindow());
                    if (newScorer != currentScorer) {
                        this.orchestrator = new DecisionOrchestrator(
                                classifier, new PolicyRanker(newScorer),
                                evaluator, classifierConfig);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (RuntimeException e) {
                loopEvidenceRecorder.recordIteration(currentSession, currentIteration,
                        null, null, previousClassification);
            }
        }

        if (currentSession != null && currentSession.endTime().isEmpty()) {
            finalizeSession(state, "loop exited");
        }
    }

    private LoopSession finalizeSession(LoopState finalState, String summary) {
        loopEvidenceRecorder.recordSessionEnd(currentSession);
        int adjustments = history.totalAdjustmentCount();
        currentSession = currentSession.ended(finalState, adjustments, currentIteration, summary);
        return currentSession;
    }

    private void unpauseAndInterrupt() {
        paused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
        if (loopThread != null) {
            loopThread.interrupt();
        }
    }
}
