package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentFailureCode;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.DefaultRuntimeAdjustmentSafetyGate;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.RuntimeAdjustmentSafetyGate;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.SafetyGateConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.SafetyGateDecision;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ScaleAdjustmentCommand;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessAssessment;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ManagedExecutorAdjustmentAdapterTest {

    private ExecutorRegistry registry;
    private AtomicDeletionSafety deletionSafety;
    private ManagedExecutor executor;
    private RuntimeAdjustmentSafetyGate safetyGate;
    private ReadinessAssessment readyAssessment;
    private ManagedExecutorAdjustmentAdapter adapter;
    private Supplier<Instant> clock;

    @BeforeEach
    void setUp() {
        deletionSafety = new AtomicDeletionSafety();
        registry = new ExecutorRegistry(deletionSafety);
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-executor", executor);

        safetyGate = new DefaultRuntimeAdjustmentSafetyGate(SafetyGateConfig.defaults());
        readyAssessment = ready();
        clock = Instant::now;

        adapter = new ManagedExecutorAdjustmentAdapter(
                registry, safetyGate, "test-executor", readyAssessment);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (!executor.isTerminated()) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // --- APPLIED path ---

    @Test
    void applyShouldSucceedAndReturnApplied() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-1", Instant.now(), 2, 6,
                "scale up for pressure", "decision-1", clock);

        AdjustmentResult result = adapter.apply(command);

        assertEquals(AdjustmentStatus.APPLIED, result.status());
        assertEquals(6, result.afterState().corePoolSize());
        assertEquals(6, executor.getCorePoolSize());
        assertEquals(6, executor.getMaximumPoolSize());
        assertNull(result.failureCode());
    }

    @Test
    void applyWithTargetWithinMaxShouldNotChangeMax() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-2", Instant.now(), 2, 3,
                "small scale up", "decision-2", clock);

        AdjustmentResult result = adapter.apply(command);

        assertEquals(AdjustmentStatus.APPLIED, result.status());
        assertEquals(3, executor.getCorePoolSize());
        assertEquals(4, executor.getMaximumPoolSize());
    }

    @Test
    void recordAppliedShouldBeCalledAfterSuccess() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-3", Instant.now(), 2, 6,
                "scale up", "decision-3", clock);

        adapter.apply(command);

        assertEquals(1, ((DefaultRuntimeAdjustmentSafetyGate) safetyGate).appliedAdjustmentsForRun());
    }

    // --- REJECTED path ---

    @Test
    void applyShouldReturnRejectedWhenGateBlocks() {
        ReadinessAssessment notReady = new ReadinessAssessment(
                ReadinessStatus.NOT_READY,
                List.of(ScenarioProfile.STEADY),
                List.of(),
                List.of("not enough data"),
                List.of(),
                ReadinessAssessment.DEFAULT_CONFIG_LABEL,
                List.of("run-x"));
        ManagedExecutorAdjustmentAdapter blockedAdapter = new ManagedExecutorAdjustmentAdapter(
                registry, new DefaultRuntimeAdjustmentSafetyGate(), "test-executor", notReady);

        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-4", Instant.now(), 2, 6,
                "scale up", "decision-4", clock);

        AdjustmentResult result = blockedAdapter.apply(command);

        assertEquals(AdjustmentStatus.REJECTED, result.status());
        assertEquals(AdjustmentFailureCode.NOT_READY, result.failureCode());
        assertEquals(2, executor.getCorePoolSize()); // unchanged
    }

    @Test
    void applyShouldNotMutateWhenRejected() {
        ReadinessAssessment notReady = new ReadinessAssessment(
                ReadinessStatus.NOT_READY,
                List.of(ScenarioProfile.STEADY),
                List.of(),
                List.of("blocked"),
                List.of(),
                ReadinessAssessment.DEFAULT_CONFIG_LABEL,
                List.of("run-x"));
        ManagedExecutorAdjustmentAdapter blockedAdapter = new ManagedExecutorAdjustmentAdapter(
                registry, new DefaultRuntimeAdjustmentSafetyGate(), "test-executor", notReady);

        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-5", Instant.now(), 2, 8,
                "scale up", "decision-5", clock);

        blockedAdapter.apply(command);

        assertEquals(2, executor.getCorePoolSize());
        assertEquals(4, executor.getMaximumPoolSize());
        assertEquals(0, ((DefaultRuntimeAdjustmentSafetyGate) safetyGate).appliedAdjustmentsForRun());
    }

    // --- FAILED path: executor not found ---

    @Test
    void applyShouldReturnFailedWhenExecutorNotFound() {
        ManagedExecutorAdjustmentAdapter missingAdapter = new ManagedExecutorAdjustmentAdapter(
                registry, safetyGate, "non-existent", readyAssessment);

        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-6", Instant.now(), 2, 6,
                "scale up", "decision-6", clock);

        AdjustmentResult result = missingAdapter.apply(command);

        assertEquals(AdjustmentStatus.FAILED, result.status());
        assertEquals(AdjustmentFailureCode.EXECUTOR_NOT_FOUND, result.failureCode());
    }

    // --- Permissive safety gate bypasses parameter validation ---

    @Test
    void applyShouldSucceedWithPermissiveGateAllowingEdgeValue() {
        // A permissive gate that bypasses the < 1 targetPoolSize check.
        RuntimeAdjustmentSafetyGate permissiveGate = new RuntimeAdjustmentSafetyGate() {
            @Override
            public SafetyGateDecision evaluate(ScaleAdjustmentCommand cmd,
                                               ExecutorStateSnapshot state,
                                               ReadinessAssessment readiness) {
                if (cmd.isNoOp() || cmd.targetPoolSize() == state.corePoolSize()) {
                    return SafetyGateDecision.noOp("no-op");
                }
                return SafetyGateDecision.allow(0, cmd);
            }

            @Override
            public void recordApplied(SafetyGateDecision decision) {
            }
        };
        ManagedExecutorAdjustmentAdapter permissiveAdapter =
                new ManagedExecutorAdjustmentAdapter(
                        registry, permissiveGate, "test-executor", readyAssessment);

        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-7", Instant.now(), 2, 1,
                "down to min", "decision-7", clock);

        AdjustmentResult result = permissiveAdapter.apply(command);

        assertEquals(AdjustmentStatus.APPLIED, result.status());
        assertEquals(1, executor.getCorePoolSize());
    }

    // --- currentState ---

    @Test
    void currentStateShouldReturnSnapshotFromRealExecutor() {
        ExecutorStateSnapshot snapshot = adapter.currentState();

        assertEquals(2, snapshot.corePoolSize());
        assertEquals(4, snapshot.maximumPoolSize());
        assertEquals(10, snapshot.queueCapacity());
        assertNotNull(snapshot.poolSize());
        assertNotNull(snapshot.completedTaskCount());
        assertNotNull(snapshot.keepAliveTimeSeconds());
        assertNotNull(snapshot.largestPoolSize());
        assertNotNull(snapshot.taskCount());
    }

    @Test
    void currentStateShouldThrowWhenExecutorNotRegistered() {
        ManagedExecutorAdjustmentAdapter missingAdapter = new ManagedExecutorAdjustmentAdapter(
                registry, safetyGate, "non-existent", readyAssessment);

        assertThrows(IllegalStateException.class, missingAdapter::currentState);
    }

    // --- recordApplied contract ---

    @Test
    void recordAppliedShouldNotBeCalledAfterRejection() {
        // Exhaust the adjustment limit to force rejection
        RuntimeAdjustmentSafetyGate limitedGate = new DefaultRuntimeAdjustmentSafetyGate(
                new SafetyGateConfig(0, 1, false, true));
        ReadinessAssessment readyWithRisk = new ReadinessAssessment(
                ReadinessStatus.READY_WITH_RISK,
                List.of(ScenarioProfile.STEADY),
                List.of(),
                List.of(),
                List.of("some risk"),
                ReadinessAssessment.DEFAULT_CONFIG_LABEL,
                List.of("run-x"));
        ManagedExecutorAdjustmentAdapter limitedAdapter = new ManagedExecutorAdjustmentAdapter(
                registry, limitedGate, "test-executor", readyWithRisk);

        ScaleAdjustmentCommand cmd1 = ScaleAdjustmentCommand.create(
                "run-8a", Instant.now(), 2, 3,
                "first adjustment", "decision-8a", clock);
        limitedAdapter.apply(cmd1);
        assertEquals(1, ((DefaultRuntimeAdjustmentSafetyGate) limitedGate).appliedAdjustmentsForRun());

        ScaleAdjustmentCommand cmd2 = ScaleAdjustmentCommand.create(
                "run-8b", Instant.now(), 3, 4,
                "second adjustment - should be rejected", "decision-8b", clock);
        AdjustmentResult result2 = limitedAdapter.apply(cmd2);

        assertEquals(AdjustmentStatus.REJECTED, result2.status());
        assertEquals(AdjustmentFailureCode.RUN_LIMIT_EXCEEDED, result2.failureCode());
        assertEquals(1, ((DefaultRuntimeAdjustmentSafetyGate) limitedGate).appliedAdjustmentsForRun());
    }

    // --- NO_OP path ---

    @Test
    void applyShouldReturnNoOpWhenTargetEqualsCurrent() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.noOp(
                "run-9", Instant.now(), 2,
                "no change needed", "decision-9", clock);

        AdjustmentResult result = adapter.apply(command);

        assertEquals(AdjustmentStatus.NO_OP, result.status());
        assertNull(result.failureCode());
    }

    // --- Enum regression ---

    @Test
    void adjustmentFailureCodeShouldContainAllOriginalConstants() {
        assertNotNull(AdjustmentFailureCode.valueOf("NOT_READY"));
        assertNotNull(AdjustmentFailureCode.valueOf("RISK_NOT_ACCEPTED"));
        assertNotNull(AdjustmentFailureCode.valueOf("COOLDOWN_ACTIVE"));
        assertNotNull(AdjustmentFailureCode.valueOf("OPPOSITE_DIRECTION"));
        assertNotNull(AdjustmentFailureCode.valueOf("RUN_LIMIT_EXCEEDED"));
        assertNotNull(AdjustmentFailureCode.valueOf("INVALID_COMMAND"));
        assertNotNull(AdjustmentFailureCode.valueOf("PROBE_FAILURE"));
        assertNotNull(AdjustmentFailureCode.valueOf("UNSUPPORTED"));
    }

    @Test
    void adjustmentFailureCodeShouldContainExecutorNotFound() {
        assertNotNull(AdjustmentFailureCode.valueOf("EXECUTOR_NOT_FOUND"));
    }

    @Test
    void adjustmentFailureCodeShouldHaveNineConstants() {
        assertEquals(9, AdjustmentFailureCode.values().length);
    }

    // --- afterState verification ---

    @Test
    void afterStateShouldReflectAppliedChanges() {
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "run-10", Instant.now(), 2, 6,
                "scale up", "decision-10", clock);

        AdjustmentResult result = adapter.apply(command);

        assertEquals(AdjustmentStatus.APPLIED, result.status());
        assertEquals(6, result.afterState().corePoolSize());
        assertEquals(6, result.afterState().maximumPoolSize());
        assertEquals(2, result.beforeState().corePoolSize());
        assertEquals(4, result.beforeState().maximumPoolSize());
        assertNotNull(result.afterState().poolSize());
    }

    // --- helper ---

    private static ReadinessAssessment ready() {
        return new ReadinessAssessment(
                ReadinessStatus.READY,
                List.of(ScenarioProfile.STEADY),
                List.of(),
                List.of(),
                List.of(),
                ReadinessAssessment.DEFAULT_CONFIG_LABEL,
                List.of("run-x"));
    }
}
