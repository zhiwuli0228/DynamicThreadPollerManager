package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for the closed-loop adjustment system.
 */
class LoopEndToEndTest {

    private ManagedExecutor executor;
    private InMemoryEvidenceRecorder evidenceRecorder;
    private LivePressureSampler sampler;
    private AdjustmentLoop loop;
    private Supplier<Instant> clock;

    @BeforeEach
    void setUp() {
        clock = Instant::now;
        executor = new ManagedExecutor(4, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(20));
        evidenceRecorder = new InMemoryEvidenceRecorder();

        ThresholdPolicyConfig conservative = new ThresholdPolicyConfig(
                "conservative", 2, 4, 8, 10, 2, 1);
        ThresholdPolicyConfig moderate = new ThresholdPolicyConfig(
                "moderate", 2, 8, 6, 5, 3, 2);
        ThresholdPolicyConfig aggressive = new ThresholdPolicyConfig(
                "aggressive", 4, 16, 4, 4, 4, 4);

        SafetyGateConfig gateConfig = new SafetyGateConfig(0, 50, false, true);
        RuntimeAdjustmentSafetyGate safetyGate = new DefaultRuntimeAdjustmentSafetyGate(gateConfig);

        PressureClassifier classifier = new SnapshotPressureClassifier();
        ThresholdPolicyScorer scorer = new ThresholdPolicyScorer();
        PolicyRanker ranker = new PolicyRanker(scorer);
        PolicyEvaluator evaluator = new ThresholdPolicyEvaluator();
        ClassifierConfig classConfig = ClassifierConfig.defaults();
        DecisionOrchestrator orchestrator = new DecisionOrchestrator(
                classifier, ranker, evaluator, classConfig);

        ExecutorAdjustmentAdapter adapter = new InMemoryAdjustableExecutorProbe(
                4, 8, 20, clock);
        AdjustmentHistory history = new AdjustmentHistory();
        InMemoryLoopEvidenceRecorder loopEvidenceRecorder = new InMemoryLoopEvidenceRecorder();
        PressureStateMachine stateMachine = new PressureStateMachine();

        LoopConfig config = new LoopConfig(
                200, 10, 5, 4, 2, 10, 2,
                List.of(conservative, moderate, aggressive));

        loop = new AdjustmentLoop(
                config, orchestrator, classifier, evaluator, classConfig,
                adapter, safetyGate, history, loopEvidenceRecorder, stateMachine,
                evidenceRecorder, clock,
                new OscillationDetector(), new FeedbackCalibrator(), null);
    }

    @AfterEach
    void tearDown() {
        if (sampler != null && sampler.isRunning()) {
            sampler.stop();
        }
        if (loop.getState() == LoopState.RUNNING || loop.getState() == LoopState.PAUSED) {
            loop.stop();
        }
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    void shouldRunMultipleCyclesWithRealComponents() throws Exception {
        // Submit more tasks than core pool size (4) to create queue pressure
        CountDownLatch taskStarted = new CountDownLatch(8);
        for (int i = 0; i < 8; i++) {
            executor.submit(() -> {
                taskStarted.countDown();
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        taskStarted.await(2, TimeUnit.SECONDS);

        loop.start(executor);
        String sessionId = loop.getCurrentSession().sessionId();
        assertEquals(LoopState.RUNNING, loop.getState());

        LivePressureSamplerConfig samplerConfig = new LivePressureSamplerConfig(
                100, false, sessionId);
        sampler = new LivePressureSampler(executor, evidenceRecorder, samplerConfig);
        sampler.start(sessionId);

        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline
                && (loop.getState() == LoopState.RUNNING
                    || loop.getState() == LoopState.PAUSED)) {
            Thread.sleep(100);
        }

        sampler.stop();

        assertTrue(loop.getState() == LoopState.STOPPED
                        || loop.getState() == LoopState.IDLE,
                "Expected STOPPED or IDLE, was " + loop.getState());

        LoopSession session = loop.getCurrentSession();
        assertNotNull(session);
        // Verify at least 5 iterations ran
        assertTrue(session.iterationCount() >= 5,
                "Expected at least 5 iterations, got " + session.iterationCount());
        // Verify loop ended in STOPPED state (not EMERGENCY_STOPPED)
        assertEquals(LoopState.STOPPED, session.finalState(),
                "Expected STOPPED, was " + session.finalState());
        // Verify session summary is valid
        assertNotNull(session.summary());
        assertFalse(session.summary().isBlank());

        // Verify AdjustmentHistory has entries
        assertFalse(loop.getHistory().isEmpty(),
                "Expected AdjustmentHistory to have entries");
    }

    @Test
    void shouldEmergencyStopOnOscillation() throws Exception {
        OscillationDetector alwaysOscillate = new OscillationDetector(4, 1) {
            @Override
            public boolean wouldOscillate(AdjustmentDecision pending, AdjustmentHistory history) {
                return true;
            }
        };

        SafetyGateConfig gateConfig = new SafetyGateConfig(0, 50, false, true);
        RuntimeAdjustmentSafetyGate safetyGate = new DefaultRuntimeAdjustmentSafetyGate(gateConfig);

        PressureClassifier classifier = new SnapshotPressureClassifier();
        PolicyRanker ranker = new PolicyRanker(new ThresholdPolicyScorer());
        PolicyEvaluator evaluator = new ThresholdPolicyEvaluator();
        DecisionOrchestrator orchestrator = new DecisionOrchestrator(
                classifier, ranker, evaluator, ClassifierConfig.defaults());

        ExecutorAdjustmentAdapter adapter = new InMemoryAdjustableExecutorProbe(
                4, 8, 20, clock);
        AdjustmentHistory history = new AdjustmentHistory();
        InMemoryLoopEvidenceRecorder loopEvidenceRecorder = new InMemoryLoopEvidenceRecorder();
        PressureStateMachine stateMachine = new PressureStateMachine();

        LoopConfig oscillationConfig = new LoopConfig(
                100, 20, 5, 4, 2, 10, 2,
                List.of(ThresholdPolicyConfig.defaultAdaptive()));

        AdjustmentLoop oscillationLoop = new AdjustmentLoop(
                oscillationConfig, orchestrator, classifier, evaluator,
                ClassifierConfig.defaults(),
                adapter, safetyGate, history, loopEvidenceRecorder, stateMachine,
                evidenceRecorder, clock,
                alwaysOscillate, new FeedbackCalibrator(), null);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try { Thread.sleep(300); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        oscillationLoop.start(executor);
        String sessionId = oscillationLoop.getCurrentSession().sessionId();

        LivePressureSamplerConfig samplerConfig = new LivePressureSamplerConfig(
                100, false, sessionId);
        sampler = new LivePressureSampler(executor, evidenceRecorder, samplerConfig);
        sampler.start(sessionId);

        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline
                && (oscillationLoop.getState() == LoopState.RUNNING
                    || oscillationLoop.getState() == LoopState.PAUSED)) {
            Thread.sleep(100);
        }

        sampler.stop();

        assertEquals(LoopState.EMERGENCY_STOPPED, oscillationLoop.getState(),
                "Expected EMERGENCY_STOPPED, was " + oscillationLoop.getState());

        LoopSession session = oscillationLoop.getCurrentSession();
        assertNotNull(session);
        assertTrue(session.summary().contains("oscillation")
                        || session.summary().contains("emergency"),
                "Summary should mention oscillation: " + session.summary());
    }

    @Test
    void shouldResetAndStartNewSessionAfterEmergencyStop() throws Exception {
        OscillationDetector alwaysOscillate = new OscillationDetector(4, 1) {
            @Override
            public boolean wouldOscillate(AdjustmentDecision pending, AdjustmentHistory history) {
                return true;
            }
        };

        SafetyGateConfig gateConfig = new SafetyGateConfig(0, 50, false, true);
        RuntimeAdjustmentSafetyGate safetyGate = new DefaultRuntimeAdjustmentSafetyGate(gateConfig);

        PressureClassifier classifier = new SnapshotPressureClassifier();
        PolicyRanker ranker = new PolicyRanker(new ThresholdPolicyScorer());
        PolicyEvaluator evaluator = new ThresholdPolicyEvaluator();
        DecisionOrchestrator orchestrator = new DecisionOrchestrator(
                classifier, ranker, evaluator, ClassifierConfig.defaults());

        ExecutorAdjustmentAdapter adapter = new InMemoryAdjustableExecutorProbe(
                4, 8, 20, clock);
        AdjustmentHistory history = new AdjustmentHistory();
        InMemoryLoopEvidenceRecorder loopEvidenceRecorder = new InMemoryLoopEvidenceRecorder();
        PressureStateMachine stateMachine = new PressureStateMachine();

        LoopConfig oscillationConfig = new LoopConfig(
                100, 20, 5, 4, 2, 10, 2,
                List.of(ThresholdPolicyConfig.defaultAdaptive()));

        AdjustmentLoop oscillationLoop = new AdjustmentLoop(
                oscillationConfig, orchestrator, classifier, evaluator,
                ClassifierConfig.defaults(),
                adapter, safetyGate, history, loopEvidenceRecorder, stateMachine,
                evidenceRecorder, clock,
                alwaysOscillate, new FeedbackCalibrator(), null);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try { Thread.sleep(300); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        oscillationLoop.start(executor);
        String sessionId = oscillationLoop.getCurrentSession().sessionId();
        LivePressureSamplerConfig samplerConfig = new LivePressureSamplerConfig(
                100, false, sessionId);
        sampler = new LivePressureSampler(executor, evidenceRecorder, samplerConfig);
        sampler.start(sessionId);

        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline
                && (oscillationLoop.getState() == LoopState.RUNNING
                    || oscillationLoop.getState() == LoopState.PAUSED)) {
            Thread.sleep(100);
        }
        sampler.stop();

        assertEquals(LoopState.EMERGENCY_STOPPED, oscillationLoop.getState());

        oscillationLoop.reset();
        assertEquals(LoopState.IDLE, oscillationLoop.getState());
        assertTrue(history.isEmpty());

        oscillationLoop.start(executor);
        assertEquals(LoopState.RUNNING, oscillationLoop.getState());

        oscillationLoop.stop();
    }
}
