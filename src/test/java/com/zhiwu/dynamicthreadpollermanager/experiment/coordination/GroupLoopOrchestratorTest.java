package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.DefaultRuntimeAdjustmentSafetyGate;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.InMemoryAdjustableExecutorProbe;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.RuntimeAdjustmentSafetyGate;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.ClassifierConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PolicyRanker;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PolicyScorer;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PressureClassifier;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.SnapshotPressureClassifier;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.ThresholdPolicyScorer;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.DecisionOrchestrator;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.AdjustmentHistory;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.FeedbackCalibrator;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.LoopConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.LoopEvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.LoopSession;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.LoopState;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.NoOpLoopEvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.OscillationDetector;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.PressureStateMachine;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.EvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.InMemoryEvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyEvaluator;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class GroupLoopOrchestratorTest {

    private ManagedExecutor executor;
    private ExecutorGroup group;
    private GroupLoopOrchestrator orchestrator;
    private final Supplier<Instant> clock = Instant::now;

    @BeforeEach
    void setUp() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));

        InMemoryAdjustableExecutorProbe adapter = new InMemoryAdjustableExecutorProbe(
                2, 4, 10, clock);

        group = ExecutorGroup.singleExecutor(
                "exec-A", executor, adapter, 10, clock);

        orchestrator = new GroupLoopOrchestrator(group);
    }

    @Test
    void shouldReturnGroup() {
        assertSame(group, orchestrator.getGroup());
    }

    @Test
    void shouldReportEmptyHealthWithNoLoops() {
        GroupHealth health = orchestrator.getGroupHealth();

        assertEquals(group.getConfig().groupId(), health.groupId());
        assertEquals(1, health.totalExecutors());
        assertEquals(0, health.runningLoops());
        assertEquals(0, health.pausedLoops());
        assertEquals(0, health.stoppedLoops());
        assertEquals(0, health.emergencyStoppedLoops());
        assertEquals(2, health.budgetSnapshot().get("exec-A"));
        assertEquals(8, health.budgetAvailable());
        assertTrue(health.activeWarnings().isEmpty());
    }

    @Test
    void shouldPauseAllWhenEmpty() {
        assertDoesNotThrow(() -> orchestrator.pauseAll());
    }

    @Test
    void shouldResumeAllWhenEmpty() {
        assertDoesNotThrow(() -> orchestrator.resumeAll());
    }

    @Test
    void shouldStopAllWhenEmpty() {
        Map<String, LoopSession> results = orchestrator.stopAll();
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldEmergencyStopAllWhenEmpty() {
        assertDoesNotThrow(() -> orchestrator.emergencyStopAll("test"));
    }

    @Test
    void shouldStartAndStopLifecycle() {
        Map<String, GroupLoopOrchestrator.LoopComponents> components =
                createLoopComponents("exec-A");

        Map<String, LoopSession> sessions = orchestrator.startAll(components);
        assertEquals(1, sessions.size());
        assertNotNull(sessions.get("exec-A"));

        GroupHealth health = orchestrator.getGroupHealth();
        assertEquals(1, health.runningLoops());
        assertEquals(LoopState.RUNNING, health.loopStates().get("exec-A"));

        orchestrator.pauseAll();
        health = orchestrator.getGroupHealth();
        assertEquals(1, health.pausedLoops());
        assertEquals(LoopState.PAUSED, health.loopStates().get("exec-A"));

        orchestrator.resumeAll();
        health = orchestrator.getGroupHealth();
        assertEquals(1, health.runningLoops());

        Map<String, LoopSession> stopped = orchestrator.stopAll();
        assertEquals(1, stopped.size());
        assertEquals(LoopState.STOPPED, stopped.get("exec-A").finalState());

        health = orchestrator.getGroupHealth();
        assertEquals(1, health.stoppedLoops());
    }

    @Test
    void shouldEmergencyStopAll() {
        Map<String, GroupLoopOrchestrator.LoopComponents> components =
                createLoopComponents("exec-A");

        orchestrator.startAll(components);

        orchestrator.emergencyStopAll("critical failure");
        GroupHealth health = orchestrator.getGroupHealth();
        assertEquals(1, health.emergencyStoppedLoops());
    }

    @Test
    void shouldReportBudgetUtilizationWarning() {
        // Use 9/10 threads to trigger >=90% utilization
        executor = new ManagedExecutor(9, 10, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));

        InMemoryAdjustableExecutorProbe adapter = new InMemoryAdjustableExecutorProbe(
                9, 10, 10, clock);

        group = ExecutorGroup.singleExecutor(
                "exec-A", executor, adapter, 10, clock);

        orchestrator = new GroupLoopOrchestrator(group);

        GroupHealth health = orchestrator.getGroupHealth();
        assertEquals(9, (int) health.budgetSnapshot().get("exec-A"));
        assertEquals(1, health.budgetAvailable());
        assertFalse(health.activeWarnings().isEmpty());
        assertTrue(health.activeWarnings().get(0).contains("90%"));
    }

    @Test
    void shouldRejectMissingComponents() {
        Map<String, GroupLoopOrchestrator.LoopComponents> empty = Map.of();
        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.startAll(empty));
    }

    private Map<String, GroupLoopOrchestrator.LoopComponents> createLoopComponents(
            String executorName) {

        var policy = ThresholdPolicyConfig.defaultAdaptive();
        LoopConfig loopConfig = LoopConfig.defaults(policy);

        PressureClassifier classifier = new SnapshotPressureClassifier();
        PolicyScorer scorer = new ThresholdPolicyScorer();
        PolicyRanker ranker = new PolicyRanker(scorer);
        PolicyEvaluator evaluator = new ThresholdPolicyEvaluator();
        ClassifierConfig classConfig = ClassifierConfig.defaults();
        DecisionOrchestrator decisionOrchestrator = new DecisionOrchestrator(
                classifier, ranker, evaluator, classConfig);

        RuntimeAdjustmentSafetyGate safetyGate = new DefaultRuntimeAdjustmentSafetyGate();
        AdjustmentHistory history = new AdjustmentHistory();
        LoopEvidenceRecorder loopEvidenceRecorder = new NoOpLoopEvidenceRecorder();
        PressureStateMachine stateMachine = new PressureStateMachine();
        EvidenceRecorder evidenceRecorder = new InMemoryEvidenceRecorder();
        OscillationDetector oscillationDetector = new OscillationDetector();
        FeedbackCalibrator calibrator = new FeedbackCalibrator();

        GroupLoopOrchestrator.LoopComponents comps =
                new GroupLoopOrchestrator.LoopComponents(
                        loopConfig, decisionOrchestrator, classifier, evaluator,
                        classConfig, safetyGate, history, loopEvidenceRecorder,
                        stateMachine, evidenceRecorder, clock, oscillationDetector,
                        calibrator, null);

        Map<String, GroupLoopOrchestrator.LoopComponents> result = new HashMap<>();
        result.put(executorName, comps);
        return result;
    }
}
