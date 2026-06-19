package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.DefaultRuntimeAdjustmentSafetyGate;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.RuntimeAdjustmentSafetyGate;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ScaleAdjustmentCommand;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessAssessment;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.ScaleDecision;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyDecision;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyEvaluationInput;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyEvaluator;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

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

class ClosedLoopExperimentTest {

    private ManagedExecutor executor;
    private ExecutorRegistry registry;
    private AtomicDeletionSafety deletionSafety;
    private ManagedExecutorAdjustmentAdapter adapter;
    private RuntimeAdjustmentSafetyGate safetyGate;
    private ReadinessAssessment readiness;
    private ThresholdPolicyConfig policyConfig;
    private ThresholdPolicyEvaluator evaluator;
    private Supplier<Instant> clock;

    @BeforeEach
    void setUp() {
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        deletionSafety = new AtomicDeletionSafety();
        registry = new ExecutorRegistry(deletionSafety);
        registry.register("experiment-executor", executor);

        safetyGate = new DefaultRuntimeAdjustmentSafetyGate();
        readiness = ready();
        adapter = new ManagedExecutorAdjustmentAdapter(
                registry, safetyGate, "experiment-executor", readiness);

        policyConfig = new ThresholdPolicyConfig(
                "default", 1, 10, 4, 1, 1, 3);
        evaluator = new ThresholdPolicyEvaluator();
        clock = Instant::now;
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (!executor.isTerminated()) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void fullClosedLoopScaleUp() throws Exception {
        // Phase 1: Generate queue pressure by submitting blocking tasks
        CountDownLatch blocker = new CountDownLatch(1);

        // Submit 2 tasks that block on the latch (consume core threads)
        executor.submit(() -> await(blocker));
        executor.submit(() -> await(blocker));

        // Submit 4 more tasks to fill the queue (queue capacity=10, so 4 fills it)
        for (int i = 0; i < 4; i++) {
            executor.submit(() -> await(blocker));
        }

        // Give threads time to start and queue to fill
        Thread.sleep(200);

        // Phase 2: Read executor state via adapter
        ExecutorStateSnapshot beforeState = adapter.currentState();
        assertEquals(2, beforeState.corePoolSize());
        assertEquals(4, beforeState.maximumPoolSize());
        assertNotNull(beforeState.activeCount());
        assertNotNull(beforeState.queueSize());
        assertTrue(beforeState.queueSize() > 0, "queue should have waiting tasks");

        // Phase 3: Build PressureSnapshot from ExecutorStateSnapshot
        PressureSnapshot pressureSnapshot = new PressureSnapshot(
                Instant.now(),
                beforeState.activeCount() != null ? beforeState.activeCount() : 0,
                beforeState.poolSize() != null ? beforeState.poolSize() : 0,
                beforeState.queueSize() != null ? beforeState.queueSize() : 0,
                beforeState.completedTaskCount() != null ? beforeState.completedTaskCount() : 0L,
                0.0);

        // Phase 4: Policy evaluation
        PolicyEvaluationInput input = new PolicyEvaluationInput(
                "closed-loop-run-1", pressureSnapshot, Instant.now());

        PolicyDecision decision = evaluator.evaluate(input, policyConfig);

        // With scaleUpQueueSizeThreshold=1 and queue pressure, should scale up
        assertNotNull(decision);
        ScaleDecision scaleDecision = decision.toScaleDecision();

        // Phase 5: Create adjustment command
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                scaleDecision.runId(),
                scaleDecision.timestamp(),
                scaleDecision.currentPoolSize(),
                scaleDecision.proposedPoolSize(),
                scaleDecision.reasoning(),
                "closed-loop-policy",
                clock);

        // Phase 6: Apply adjustment via adapter
        AdjustmentResult result = adapter.apply(command);

        // Phase 7: Verify
        assertEquals(AdjustmentStatus.APPLIED, result.status());
        assertEquals(command.targetPoolSize(), result.afterState().corePoolSize());
        assertTrue(result.afterState().maximumPoolSize() >= result.afterState().corePoolSize());
        assertEquals(2, result.beforeState().corePoolSize());
        assertTrue(result.afterState().corePoolSize() > result.beforeState().corePoolSize(),
                "after core pool size should be greater than before");

        // Extended fields populated
        assertNotNull(result.beforeState().poolSize());
        assertNotNull(result.beforeState().completedTaskCount());
        assertNotNull(result.beforeState().keepAliveTimeSeconds());
        assertNotNull(result.beforeState().largestPoolSize());
        assertNotNull(result.beforeState().taskCount());
        assertNotNull(result.afterState().poolSize());
        assertNotNull(result.afterState().completedTaskCount());
        assertNotNull(result.afterState().keepAliveTimeSeconds());
        assertNotNull(result.afterState().largestPoolSize());
        assertNotNull(result.afterState().taskCount());

        // Release blocked tasks
        blocker.countDown();
    }

    @Test
    void executorCleanupAfterExperiment() throws Exception {
        CountDownLatch blocker = new CountDownLatch(1);
        executor.submit(() -> await(blocker));
        Thread.sleep(100);

        assertFalse(executor.isTerminated());
        assertFalse(executor.isShutdown());

        executor.shutdownNow();
        assertTrue(executor.isShutdown());

        boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(terminated);
        assertTrue(executor.isTerminated());

        blocker.countDown();
    }

    @Test
    void deletionSafetyAndRegistryCleanupAfterExperiment() throws Exception {
        // Run a mini experiment
        CountDownLatch blocker = new CountDownLatch(1);
        executor.submit(() -> await(blocker));
        executor.submit(() -> await(blocker));
        Thread.sleep(100);

        // Verify the executor is registered and active
        assertTrue(registry.get("experiment-executor").isPresent());
        assertEquals(1, registry.size());

        // Release blocked tasks before shutdown
        blocker.countDown();

        // Shutdown and wait for termination
        executor.shutdown();
        boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(terminated, "executor should terminate within timeout");
        assertTrue(executor.isTerminated());

        // Now removal should succeed
        boolean removed = registry.remove("experiment-executor");
        assertTrue(removed);
        assertTrue(registry.get("experiment-executor").isEmpty());
        assertEquals(0, registry.size());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static ReadinessAssessment ready() {
        return new ReadinessAssessment(
                ReadinessStatus.READY,
                List.of(ScenarioProfile.STEADY),
                List.of(),
                List.of(),
                List.of(),
                ReadinessAssessment.DEFAULT_CONFIG_LABEL,
                List.of("closed-loop-run-1"));
    }
}
