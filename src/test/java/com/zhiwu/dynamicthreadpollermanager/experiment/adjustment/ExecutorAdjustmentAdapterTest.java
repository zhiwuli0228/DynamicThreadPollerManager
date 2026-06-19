package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessAssessment;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link ExecutorAdjustmentAdapter} contract and the
 * {@link InMemoryAdjustableExecutorProbe} implementation.
 */
class ExecutorAdjustmentAdapterTest {

    private static final Instant T0 = Instant.parse("2026-06-05T10:00:00Z");
    private static final Supplier<Instant> CLOCK = () -> T0;

    private static ReadinessAssessment ready() {
        return new ReadinessAssessment(
                ReadinessStatus.READY,
                List.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of(),
                List.of(),
                List.of(),
                ReadinessAssessment.DEFAULT_CONFIG_LABEL,
                List.of("run-1"));
    }

    private static ScaleAdjustmentCommand scaleUp(int from, int to) {
        return ScaleAdjustmentCommand.create("run-1", T0, from, to, "scale up", "policy-1:0", CLOCK);
    }

    @Test
    void probeShouldExposeCurrentState() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        ExecutorStateSnapshot snapshot = probe.currentState();
        assertEquals(4, snapshot.corePoolSize());
        assertEquals(8, snapshot.maximumPoolSize());
        assertEquals(0, snapshot.activeCount());
        assertEquals(0, snapshot.queueSize());
        assertEquals(10, snapshot.queueCapacity());
        assertNotNull(snapshot.observedAt());
    }

    @Test
    void probeShouldRejectNullClock() {
        assertThrows(NullPointerException.class, () -> new InMemoryAdjustableExecutorProbe(4, 8, 10, null));
    }

    @Test
    void probeShouldRejectInvalidSizing() {
        assertThrows(IllegalArgumentException.class, () -> new InMemoryAdjustableExecutorProbe(0, 8, 10, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryAdjustableExecutorProbe(8, 4, 10, CLOCK));
    }

    @Test
    void applyShouldChangeCorePoolSize() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        AdjustmentResult result = probe.apply(scaleUp(4, 6));
        assertEquals(AdjustmentStatus.APPLIED, result.status());
        assertEquals(6, result.appliedPoolSize());
        assertEquals(6, probe.currentState().corePoolSize());
        assertEquals(8, probe.currentState().maximumPoolSize());
    }

    @Test
    void applyShouldReturnNoOpWhenTargetEqualsCurrent() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        ScaleAdjustmentCommand noOp = ScaleAdjustmentCommand.noOp("run-1", T0, 4, "no-op", "policy-1:0", CLOCK);
        AdjustmentResult result = probe.apply(noOp);
        assertEquals(AdjustmentStatus.NO_OP, result.status());
        assertEquals(4, probe.currentState().corePoolSize());
    }

    @Test
    void applyShouldRejectWhenTargetExceedsMaximum() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        ScaleAdjustmentCommand command = scaleUp(4, 16);
        AdjustmentResult result = probe.apply(command);
        assertEquals(AdjustmentStatus.REJECTED, result.status());
        assertSame(AdjustmentFailureCode.INVALID_COMMAND, result.failureCode());
        assertEquals(4, probe.currentState().corePoolSize());
        // The result reason is the source command's reason in all statuses.
        assertEquals(command.reason(), result.reason());
    }

    @Test
    void applyShouldRejectWhenTargetBelowOne() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        // Build a command with target=0 (currentPoolSize=4) using the
        // package-private raw constructor to bypass create()'s positive check.
        ScaleAdjustmentCommand invalid = new ScaleAdjustmentCommand(
                "run-1:2026-06-05T10:00:00Z:4->0", "run-1", T0, 4, 0,
                "below minimum", "policy-1:0", T0);
        AdjustmentResult result = probe.apply(invalid);
        assertEquals(AdjustmentStatus.REJECTED, result.status());
        assertSame(AdjustmentFailureCode.INVALID_COMMAND, result.failureCode());
        assertEquals("below minimum", result.reason());
    }

    @Test
    void applyShouldReturnFailedWhenProbeThrows() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK) {
            @Override
            public AdjustmentResult apply(ScaleAdjustmentCommand command) {
                throw new IllegalStateException("boom");
            }
        };
        AdjustmentResult result;
        try {
            result = probe.apply(scaleUp(4, 6));
        } catch (Exception e) {
            result = null;
        }
        // The probe contract returns FAILED for any thrown exception.
        // Since the override above re-throws, we instead test the
        // standard behavior with a separate probe class.
        probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK) {
            @Override
            protected void doSetCorePoolSize(int newCore) {
                throw new IllegalStateException("boom");
            }
        };
        ScaleAdjustmentCommand command = scaleUp(4, 6);
        result = probe.apply(command);
        assertEquals(AdjustmentStatus.FAILED, result.status());
        assertSame(AdjustmentFailureCode.PROBE_FAILURE, result.failureCode());
        assertEquals(command.reason(), result.reason());
        assertEquals(4, probe.currentState().corePoolSize());
    }

    @Test
    void applyShouldCaptureBeforeAndAfterState() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        AdjustmentResult result = probe.apply(scaleUp(4, 6));
        assertNotNull(result.beforeState());
        assertNotNull(result.afterState());
        assertEquals(4, result.beforeState().corePoolSize());
        assertEquals(6, result.afterState().corePoolSize());
        assertEquals(6, result.requestedPoolSize());
        assertEquals(6, result.appliedPoolSize());
    }

    @Test
    void applyShouldPreserveReasonAndSourceDecisionRef() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        ScaleAdjustmentCommand cmd = ScaleAdjustmentCommand.create(
                "run-1", T0, 4, 6, "scale up", "policy-1:0", CLOCK);
        AdjustmentResult result = probe.apply(cmd);
        assertEquals("scale up", result.reason());
        assertEquals("policy-1:0", result.sourceDecisionRef());
        assertEquals(T0, result.decisionTimestamp());
    }

    @Test
    void applyShouldRejectNullCommand() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        assertThrows(NullPointerException.class, () -> probe.apply(null));
    }

    @Test
    void fullPipelineShouldWireGateAndProbe() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        ScaleAdjustmentCommand cmd = scaleUp(4, 6);
        SafetyGateDecision decision = gate.evaluate(cmd, probe.currentState(), ready());
        assertTrue(decision.isAllowed());
        gate.recordApplied(decision);
        AdjustmentResult result = probe.apply(cmd);
        assertEquals(AdjustmentStatus.APPLIED, result.status());
        assertEquals(6, probe.currentState().corePoolSize());
    }

    @Test
    void fullPipelineShouldRejectWhenGateRejects() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        // Burn the cooldown by running two drain commands first.
        ScaleAdjustmentCommand cmd1 = scaleUp(4, 6);
        SafetyGateDecision d1 = gate.evaluate(cmd1, probe.currentState(), ready());
        gate.recordApplied(d1);
        SafetyGateDecision d2 = gate.evaluate(cmd1, probe.currentState(), ready());
        assertFalse(d2.isAllowed());
        assertSame(AdjustmentFailureCode.COOLDOWN_ACTIVE, d2.failureCode());
    }

    @Test
    void probeShouldExposeAdjustmentCount() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        assertEquals(0, probe.appliedAdjustmentCount());
        probe.apply(scaleUp(4, 6));
        assertEquals(1, probe.appliedAdjustmentCount());
        probe.apply(scaleUp(6, 8));
        assertEquals(2, probe.appliedAdjustmentCount());
    }

    @Test
    void probeShouldNotMutateQueueCapacity() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        AdjustmentResult result = probe.apply(scaleUp(4, 6));
        assertEquals(10, result.afterState().queueCapacity());
        assertEquals(10, probe.currentState().queueCapacity());
    }
}
