package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.EvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.InMemoryEvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class AdjustmentLoopLifecycleTest {

    private AdjustmentLoop loop;
    private ManagedExecutor executor;
    private LoopConfig config;
    private Supplier<Instant> clock;

    @BeforeEach
    void setUp() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));

        var policy = ThresholdPolicyConfig.defaultAdaptive();
        config = LoopConfig.defaults(policy);

        PressureClassifier classifier = new SnapshotPressureClassifier();
        PolicyScorer scorer = new ThresholdPolicyScorer();
        PolicyRanker ranker = new PolicyRanker(scorer);
        PolicyEvaluator evaluator = new ThresholdPolicyEvaluator();
        ClassifierConfig classConfig = ClassifierConfig.defaults();
        DecisionOrchestrator orchestrator = new DecisionOrchestrator(
                classifier, ranker, evaluator, classConfig);

        ExecutorAdjustmentAdapter adapter = new InMemoryAdjustableExecutorProbe(
                2, 4, 10, Instant::now);
        RuntimeAdjustmentSafetyGate safetyGate = new DefaultRuntimeAdjustmentSafetyGate();
        AdjustmentHistory history = new AdjustmentHistory();
        LoopEvidenceRecorder evidenceRecorder = new NoOpLoopEvidenceRecorder();
        PressureStateMachine stateMachine = new PressureStateMachine();
        EvidenceRecorder snapshotRecorder = new InMemoryEvidenceRecorder();

        clock = Instant::now;

        loop = new AdjustmentLoop(
                config, orchestrator, classifier, evaluator, classConfig,
                adapter, safetyGate, history, evidenceRecorder, stateMachine,
                snapshotRecorder, clock);
    }

    @Test
    void shouldStartInIdleState() {
        assertEquals(LoopState.IDLE, loop.getState());
    }

    @Test
    void shouldTransitionToRunningOnStart() {
        loop.start(executor);
        assertEquals(LoopState.RUNNING, loop.getState());
        assertNotNull(loop.getCurrentSession());
        loop.stop();
    }

    @Test
    void shouldThrowOnStartFromRunning() {
        loop.start(executor);
        try {
            assertThrows(IllegalStateException.class, () -> loop.start(executor));
        } finally {
            loop.stop();
        }
    }

    @Test
    void shouldPauseAndResume() {
        loop.start(executor);
        loop.pause();
        assertEquals(LoopState.PAUSED, loop.getState());
        loop.resume();
        assertEquals(LoopState.RUNNING, loop.getState());
        loop.stop();
    }

    @Test
    void shouldThrowOnPauseFromIdle() {
        assertThrows(IllegalStateException.class, () -> loop.pause());
    }

    @Test
    void shouldThrowOnResumeFromRunning() {
        loop.start(executor);
        try {
            assertThrows(IllegalStateException.class, () -> loop.resume());
        } finally {
            loop.stop();
        }
    }

    @Test
    void shouldStopAndReturnSession() {
        loop.start(executor);
        LoopSession session = loop.stop();
        assertEquals(LoopState.STOPPED, loop.getState());
        assertEquals(LoopState.STOPPED, session.finalState());
        assertTrue(session.endTime().isPresent());
    }

    @Test
    void shouldEmergencyStop() {
        loop.start(executor);
        LoopSession session = loop.emergencyStop("test emergency");
        assertEquals(LoopState.EMERGENCY_STOPPED, loop.getState());
        assertTrue(session.summary().contains("emergency stop"));
        assertTrue(session.summary().contains("test emergency"));
    }

    @Test
    void shouldResetFromStopped() {
        loop.start(executor);
        loop.stop();
        loop.reset();
        assertEquals(LoopState.IDLE, loop.getState());
        assertNull(loop.getCurrentSession());
        assertTrue(loop.getHistory().isEmpty());
    }

    @Test
    void shouldResetFromEmergencyStopped() {
        loop.start(executor);
        loop.emergencyStop("test");
        loop.reset();
        assertEquals(LoopState.IDLE, loop.getState());
    }

    @Test
    void shouldThrowOnResetFromRunning() {
        loop.start(executor);
        try {
            assertThrows(IllegalStateException.class, () -> loop.reset());
        } finally {
            loop.stop();
        }
    }

    @Test
    void shouldReuseAfterReset() {
        loop.start(executor);
        loop.stop();
        loop.reset();
        loop.start(executor);
        assertEquals(LoopState.RUNNING, loop.getState());
        loop.stop();
    }
}
