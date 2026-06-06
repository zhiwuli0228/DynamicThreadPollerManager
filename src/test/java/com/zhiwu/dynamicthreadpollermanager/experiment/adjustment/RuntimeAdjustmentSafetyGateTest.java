package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessAssessment;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link RuntimeAdjustmentSafetyGate} covering all six
 * blocking rules plus the no-op rule from the spec.
 */
class RuntimeAdjustmentSafetyGateTest {

    private static final Instant T0 = Instant.parse("2026-06-05T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-06-05T10:01:00Z");
    private static final Instant T2 = Instant.parse("2026-06-05T10:02:00Z");
    private static final Instant T3 = Instant.parse("2026-06-05T10:03:00Z");
    private static final Instant T4 = Instant.parse("2026-06-05T10:04:00Z");
    private static final AtomicReference<Instant> CLOCK_NOW = new AtomicReference<>(T0);
    private static final Supplier<Instant> CLOCK = () -> CLOCK_NOW.get();

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

    private static ReadinessAssessment readyWithRisk() {
        return new ReadinessAssessment(
                ReadinessStatus.READY_WITH_RISK,
                List.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of(),
                List.of(),
                List.of("capped ratio 0.30 above ready ceiling"),
                ReadinessAssessment.DEFAULT_CONFIG_LABEL,
                List.of("run-1"));
    }

    private static ReadinessAssessment notReady() {
        return new ReadinessAssessment(
                ReadinessStatus.NOT_READY,
                List.of(ScenarioProfile.STEADY),
                List.of(ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of("missing required scenarioProfile RAMP"),
                List.of(),
                ReadinessAssessment.DEFAULT_CONFIG_LABEL,
                List.of("run-1"));
    }

    private static ScaleAdjustmentCommand scaleUp(int from, int to, String sourceRef) {
        return ScaleAdjustmentCommand.create("run-1", T0, from, to, "scale up", sourceRef, CLOCK);
    }

    private static ScaleAdjustmentCommand scaleDown(int from, int to, String sourceRef) {
        return ScaleAdjustmentCommand.create("run-1", T0, from, to, "scale down", sourceRef, CLOCK);
    }

    private static ExecutorStateSnapshot snapshot(int corePoolSize, int maxPoolSize) {
        return ExecutorStateSnapshot.builder(T0)
                .corePoolSize(corePoolSize)
                .maximumPoolSize(maxPoolSize)
                .build();
    }

    @Test
    void defaultConfigShouldExposeDesignValues() {
        SafetyGateConfig defaults = SafetyGateConfig.defaults();
        assertEquals(2, defaults.cooldownDecisionIntervals());
        assertEquals(5, defaults.maxAdjustmentsPerRun());
        assertTrue(defaults.blockImmediateOppositeDirection());
        assertFalse(defaults.allowReadyWithRisk());
    }

    @Test
    void shouldAllowScaleUpWhenReadyAndWithinBounds() {
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        SafetyGateDecision decision = gate.evaluate(scaleUp(8, 12, "policy-1:0"),
                snapshot(8, 16), ready());
        assertTrue(decision.isAllowed());
        assertNull(decision.failureCode());
        assertEquals(0, decision.appliedAdjustmentsForRun());
    }

    @Test
    void shouldRejectWhenNotReady() {
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        SafetyGateDecision decision = gate.evaluate(scaleUp(8, 12, "policy-1:0"),
                snapshot(8, 16), notReady());
        assertFalse(decision.isAllowed());
        assertSame(AdjustmentFailureCode.NOT_READY, decision.failureCode());
        assertNotNull(decision.reason());
        assertEquals(SafetyGateDecision.Outcome.REJECTED, decision.outcome());
    }

    @Test
    void shouldRejectReadyWithRiskByDefault() {
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        SafetyGateDecision decision = gate.evaluate(scaleUp(8, 12, "policy-1:0"),
                snapshot(8, 16), readyWithRisk());
        assertFalse(decision.isAllowed());
        assertSame(AdjustmentFailureCode.RISK_NOT_ACCEPTED, decision.failureCode());
    }

    @Test
    void shouldAllowReadyWithRiskWhenEnabled() {
        SafetyGateConfig config = new SafetyGateConfig(2, 5, true, true);
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate(config);
        SafetyGateDecision decision = gate.evaluate(scaleUp(8, 12, "policy-1:0"),
                snapshot(8, 16), readyWithRisk());
        assertTrue(decision.isAllowed());
    }

    @Test
    void shouldEnforceCooldown() {
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        SafetyGateDecision first = gate.evaluate(scaleUp(8, 12, "policy-1:0"),
                snapshot(8, 16), ready());
        assertTrue(first.isAllowed());
        gate.recordApplied(first);
        // Next command represents a real scale up, with the executor
        // at its new state.
        SafetyGateDecision afterOne = gate.evaluate(scaleUp(12, 16, "policy-1:1"),
                snapshot(12, 16), ready());
        assertFalse(afterOne.isAllowed());
        assertSame(AdjustmentFailureCode.COOLDOWN_ACTIVE, afterOne.failureCode());
    }

    @Test
    void shouldAllowAfterCooldownDecisionIntervals() {
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        SafetyGateDecision first = gate.evaluate(scaleUp(8, 12, "policy-1:0"),
                snapshot(8, 16), ready());
        assertTrue(first.isAllowed());
        gate.recordApplied(first);
        // Cooldown=2: first two calls rejected, third allowed.
        SafetyGateDecision afterOne = gate.evaluate(scaleUp(12, 14, "policy-1:1"),
                snapshot(12, 16), ready());
        assertFalse(afterOne.isAllowed());
        SafetyGateDecision afterTwo = gate.evaluate(scaleUp(12, 14, "policy-1:2"),
                snapshot(12, 16), ready());
        assertFalse(afterTwo.isAllowed());
        SafetyGateDecision allowed = gate.evaluate(scaleUp(12, 14, "policy-1:3"),
                snapshot(12, 16), ready());
        assertTrue(allowed.isAllowed());
    }

    @Test
    void shouldBlockImmediateOppositeDirection() {
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        SafetyGateDecision up = gate.evaluate(scaleUp(8, 12, "policy-1:0"),
                snapshot(8, 16), ready());
        assertTrue(up.isAllowed());
        gate.recordApplied(up);
        // Cooldown elapses...
        gate.evaluate(scaleUp(12, 14, "policy-1:1"), snapshot(12, 16), ready());
        gate.evaluate(scaleUp(12, 14, "policy-1:2"), snapshot(12, 16), ready());
        // Now the executor is at 12 and the caller wants to scale down.
        SafetyGateDecision down = gate.evaluate(scaleDown(12, 10, "policy-1:3"),
                snapshot(12, 16), ready());
        assertFalse(down.isAllowed());
        assertSame(AdjustmentFailureCode.OPPOSITE_DIRECTION, down.failureCode());
    }

    @Test
    void shouldNotBlockSameDirectionAfterCooldown() {
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        SafetyGateDecision up1 = gate.evaluate(scaleUp(8, 12, "policy-1:0"),
                snapshot(8, 16), ready());
        gate.recordApplied(up1);
        gate.evaluate(scaleUp(12, 14, "policy-1:1"), snapshot(12, 16), ready());
        gate.evaluate(scaleUp(12, 14, "policy-1:2"), snapshot(12, 16), ready());
        SafetyGateDecision up2 = gate.evaluate(scaleUp(12, 14, "policy-1:3"),
                snapshot(12, 16), ready());
        assertTrue(up2.isAllowed());
    }

    @Test
    void shouldEnforcePerRunLimit() {
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        // Apply five allowed scale-up adjustments, with cooldown calls
        // in between. The sixth must be rejected by the per-run limit.
        applyWithCooldown(gate, scaleUp(8, 12, "policy-1:0"), snapshot(8, 24));
        applyWithCooldown(gate, scaleUp(12, 14, "policy-1:1"), snapshot(12, 24));
        applyWithCooldown(gate, scaleUp(14, 16, "policy-1:2"), snapshot(14, 24));
        applyWithCooldown(gate, scaleUp(16, 18, "policy-1:3"), snapshot(16, 24));
        applyWithCooldown(gate, scaleUp(18, 20, "policy-1:4"), snapshot(18, 24));
        SafetyGateDecision rejected = gate.evaluate(scaleUp(20, 22, "policy-1:5"),
                snapshot(20, 24), ready());
        assertFalse(rejected.isAllowed());
        assertSame(AdjustmentFailureCode.RUN_LIMIT_EXCEEDED, rejected.failureCode());
    }

    @Test
    void shouldReturnNoOpWhenTargetMatchesCurrentState() {
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        ScaleAdjustmentCommand noOp = ScaleAdjustmentCommand.noOp(
                "run-1", T0, 8, "already at target", "policy-1:0", CLOCK);
        SafetyGateDecision decision = gate.evaluate(noOp, snapshot(8, 16), ready());
        assertEquals(SafetyGateDecision.Outcome.NO_OP, decision.outcome());
        assertFalse(decision.isAllowed());
        assertNull(decision.failureCode());
        assertNotNull(decision.reason());
    }

    @Test
    void shouldRejectInvalidCommand() {
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        // create() rejects no-op target (current == target).
        assertThrows(IllegalArgumentException.class, () -> ScaleAdjustmentCommand.create(
                "run-1", T0, 8, 8, "noop", "policy-1:0", CLOCK));
    }

    @Test
    void shouldRejectNullReadiness() {
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        assertThrows(NullPointerException.class,
                () -> gate.evaluate(scaleUp(8, 12, "policy-1:0"), snapshot(8, 16), null));
    }

    @Test
    void shouldRejectNullCommand() {
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        assertThrows(NullPointerException.class,
                () -> gate.evaluate(null, snapshot(8, 16), ready()));
    }

    @Test
    void shouldRejectNullSnapshot() {
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        assertThrows(NullPointerException.class,
                () -> gate.evaluate(scaleUp(8, 12, "policy-1:0"), null, ready()));
    }

    @Test
    void shouldNotRecordRejectedDecision() {
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        gate.evaluate(scaleUp(8, 12, "policy-1:0"), snapshot(8, 16), notReady());
        SafetyGateDecision next = gate.evaluate(scaleUp(8, 12, "policy-1:1"),
                snapshot(8, 16), ready());
        assertTrue(next.isAllowed());
    }

    @Test
    void shouldCapAtConfiguredPerRunLimit() {
        SafetyGateConfig config = new SafetyGateConfig(2, 3, true, false);
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate(config);
        applyWithCooldown(gate, scaleUp(8, 12, "policy-1:0"), snapshot(8, 16));
        applyWithCooldown(gate, scaleUp(12, 14, "policy-1:1"), snapshot(12, 16));
        applyWithCooldown(gate, scaleUp(14, 16, "policy-1:2"), snapshot(14, 16));
        SafetyGateDecision d4 = gate.evaluate(scaleUp(16, 18, "policy-1:3"),
                snapshot(16, 16), ready());
        assertFalse(d4.isAllowed());
        assertSame(AdjustmentFailureCode.RUN_LIMIT_EXCEEDED, d4.failureCode());
    }

    @Test
    void safetyGateConfigShouldRejectInvalidValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new SafetyGateConfig(-1, 5, true, false));
        assertThrows(IllegalArgumentException.class,
                () -> new SafetyGateConfig(2, 0, true, false));
    }

    /** Helper that drains the cooldown before applying the next command. */
    private static void applyWithCooldown(RuntimeAdjustmentSafetyGate gate,
                                          ScaleAdjustmentCommand command,
                                          ExecutorStateSnapshot snapshot) {
        SafetyGateDecision d = gate.evaluate(command, snapshot, ready());
        assertTrue(d.isAllowed(), () -> "expected allow for " + command);
        gate.recordApplied(d);
        // Drain the cooldown by issuing 2 throwaway commands.
        gate.evaluate(ScaleAdjustmentCommand.create("run-1", T1,
                command.targetPoolSize(), command.targetPoolSize() + 1,
                "drain1", "policy-drain:1", CLOCK), snapshot, ready());
        gate.evaluate(ScaleAdjustmentCommand.create("run-1", T2,
                command.targetPoolSize(), command.targetPoolSize() + 1,
                "drain2", "policy-drain:2", CLOCK), snapshot, ready());
    }
}
