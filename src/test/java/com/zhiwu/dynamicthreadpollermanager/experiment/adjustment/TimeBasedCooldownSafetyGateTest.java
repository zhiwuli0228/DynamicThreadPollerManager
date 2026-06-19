package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessAssessment;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class TimeBasedCooldownSafetyGateTest {

    private static final Instant T0 = Instant.parse("2026-06-05T10:00:00Z");
    private static final Duration COOLDOWN_1S = Duration.ofSeconds(1);

    private final AtomicReference<Instant> clockRef = new AtomicReference<>(T0);
    private final Supplier<Instant> clock = clockRef::get;
    private SafetyGateConfig config;

    @BeforeEach
    void setUp() {
        config = SafetyGateConfig.defaults();
    }

    private static ReadinessAssessment ready() {
        return new ReadinessAssessment(
                ReadinessStatus.READY,
                List.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of(), List.of(), List.of(),
                ReadinessAssessment.DEFAULT_CONFIG_LABEL,
                List.of("run-1"));
    }

    private static ReadinessAssessment readyWithRisk() {
        return new ReadinessAssessment(
                ReadinessStatus.READY_WITH_RISK,
                List.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of(), List.of(),
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
                List.of(), ReadinessAssessment.DEFAULT_CONFIG_LABEL,
                List.of("run-1"));
    }

    private static ScaleAdjustmentCommand scaleUp(int from, int to) {
        return ScaleAdjustmentCommand.create("run-1", T0, from, to,
                "scale up", "policy-1:0", () -> T0);
    }

    private static ScaleAdjustmentCommand scaleUpEmergency(int from, int to) {
        return ScaleAdjustmentCommand.create("run-1", T0, from, to,
                "rollback: emergency", "policy-rollback:0", () -> T0, true);
    }

    private ScaleAdjustmentCommand scaleUpTimed(int from, int to, String ref) {
        return ScaleAdjustmentCommand.create("run-1", clockRef.get(), from, to,
                "scale up", ref, clock);
    }

    private ScaleAdjustmentCommand scaleUpTimedEmergency(int from, int to, String ref) {
        return ScaleAdjustmentCommand.create("run-1", clockRef.get(), from, to,
                "rollback: emergency", ref, clock, true);
    }

    private ScaleAdjustmentCommand scaleDownTimed(int from, int to, String ref) {
        return ScaleAdjustmentCommand.create("run-1", clockRef.get(), from, to,
                "scale down", ref, clock);
    }

    private ExecutorStateSnapshot snapshot(int core, int max) {
        return ExecutorStateSnapshot.builder(clockRef.get())
                .corePoolSize(core).maximumPoolSize(max).build();
    }

    // --- Requirement 3.1: Rejects null clock ---

    @Test
    void shouldRejectNullClock() {
        assertThrows(NullPointerException.class, () ->
                new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, null));
    }

    @Test
    void shouldRejectNullConfig() {
        assertThrows(NullPointerException.class, () ->
                new TimeBasedCooldownSafetyGate(null, COOLDOWN_1S, clock));
    }

    @Test
    void shouldRejectNullCooldownDuration() {
        assertThrows(NullPointerException.class, () ->
                new TimeBasedCooldownSafetyGate(config, null, clock));
    }

    @Test
    void shouldRejectNegativeCooldownDuration() {
        assertThrows(IllegalArgumentException.class, () ->
                new TimeBasedCooldownSafetyGate(config, Duration.ofSeconds(-1), clock));
    }

    // --- Requirement 3.2: First command allowed when no prior adjustment ---

    @Test
    void shouldAllowFirstCommand() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);
        SafetyGateDecision decision = gate.evaluate(
                scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), ready());
        assertTrue(decision.isAllowed());
    }

    // --- Requirement 3.3: Cooldown rejects commands within window ---

    @Test
    void shouldRejectDuringCooldownWindow() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);

        SafetyGateDecision first = gate.evaluate(
                scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), ready());
        assertTrue(first.isAllowed());
        gate.recordApplied(first);

        // 500ms later — still within 1s cooldown
        clockRef.set(T0.plusMillis(500));
        SafetyGateDecision duringCooldown = gate.evaluate(
                scaleUpTimed(12, 14, "policy-1:1"), snapshot(12, 16), ready());
        assertFalse(duringCooldown.isAllowed());
        assertSame(AdjustmentFailureCode.COOLDOWN_ACTIVE, duringCooldown.failureCode());
    }

    @Test
    void shouldAllowAfterCooldownExpires() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);

        SafetyGateDecision first = gate.evaluate(
                scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), ready());
        assertTrue(first.isAllowed());
        gate.recordApplied(first);

        // 1500ms later — cooldown expired
        clockRef.set(T0.plusMillis(1500));
        SafetyGateDecision afterCooldown = gate.evaluate(
                scaleUpTimed(12, 14, "policy-1:1"), snapshot(12, 16), ready());
        assertTrue(afterCooldown.isAllowed());
    }

    @Test
    void shouldAllowExactlyAtCooldownBoundary() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);

        SafetyGateDecision first = gate.evaluate(
                scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), ready());
        gate.recordApplied(first);

        // Exactly 1000ms later — cooldown has elapsed
        clockRef.set(T0.plusSeconds(1));
        SafetyGateDecision atBoundary = gate.evaluate(
                scaleUpTimed(12, 14, "policy-1:1"), snapshot(12, 16), ready());
        assertTrue(atBoundary.isAllowed());
    }

    // --- Requirement 3.4: Emergency rollback bypasses cooldown ---

    @Test
    void shouldAllowEmergencyRollbackDuringCooldown() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);

        SafetyGateDecision first = gate.evaluate(
                scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), ready());
        gate.recordApplied(first);

        // 100ms later — still in cooldown, but emergency rollback
        clockRef.set(T0.plusMillis(100));
        ScaleAdjustmentCommand rollback = scaleUpTimedEmergency(12, 8, "rollback:1");
        assertTrue(rollback.isEmergencyRollback());
        SafetyGateDecision emergencyDecision = gate.evaluate(rollback, snapshot(12, 16), ready());
        assertTrue(emergencyDecision.isAllowed());
    }

    @Test
    void shouldBlockNonEmergencyDuringCooldown() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);

        SafetyGateDecision first = gate.evaluate(
                scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), ready());
        gate.recordApplied(first);

        clockRef.set(T0.plusMillis(100));
        SafetyGateDecision nonEmergency = gate.evaluate(
                scaleUpTimed(12, 14, "policy-1:1"), snapshot(12, 16), ready());
        assertFalse(nonEmergency.isAllowed());
        assertSame(AdjustmentFailureCode.COOLDOWN_ACTIVE, nonEmergency.failureCode());
    }

    @Test
    void shouldAllowEmergencyRollbackWithTargetEqualToPreviousSafeState() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);

        SafetyGateDecision scaleUp = gate.evaluate(
                scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), ready());
        gate.recordApplied(scaleUp);

        clockRef.set(T0.plusMillis(100));
        // Rollback from 12 back to 8 (previous safe state)
        ScaleAdjustmentCommand rollback = scaleUpTimedEmergency(12, 8, "rollback:1");
        SafetyGateDecision decision = gate.evaluate(rollback, snapshot(12, 16), ready());
        assertTrue(decision.isAllowed());
    }

    // --- Requirement 3.5: All other safety checks preserved ---

    @Test
    void shouldRejectWhenNotReady() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);
        SafetyGateDecision decision = gate.evaluate(
                scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), notReady());
        assertFalse(decision.isAllowed());
        assertSame(AdjustmentFailureCode.NOT_READY, decision.failureCode());
    }

    @Test
    void shouldRejectReadyWithRiskByDefault() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);
        SafetyGateDecision decision = gate.evaluate(
                scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), readyWithRisk());
        assertFalse(decision.isAllowed());
        assertSame(AdjustmentFailureCode.RISK_NOT_ACCEPTED, decision.failureCode());
    }

    @Test
    void shouldAllowReadyWithRiskWhenEnabled() {
        SafetyGateConfig allowRisk = new SafetyGateConfig(2, 5, true, true);
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(allowRisk, COOLDOWN_1S, clock);
        SafetyGateDecision decision = gate.evaluate(
                scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), readyWithRisk());
        assertTrue(decision.isAllowed());
    }

    @Test
    void shouldEnforcePerRunLimit() {
        SafetyGateConfig smallLimit = new SafetyGateConfig(0, 2, false, false);
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(smallLimit, Duration.ZERO, clock);

        SafetyGateDecision d1 = gate.evaluate(
                scaleUpTimed(8, 10, "policy-1:0"), snapshot(8, 16), ready());
        assertTrue(d1.isAllowed());
        gate.recordApplied(d1);

        SafetyGateDecision d2 = gate.evaluate(
                scaleUpTimed(10, 12, "policy-1:1"), snapshot(10, 16), ready());
        assertTrue(d2.isAllowed());
        gate.recordApplied(d2);

        SafetyGateDecision d3 = gate.evaluate(
                scaleUpTimed(12, 14, "policy-1:2"), snapshot(12, 16), ready());
        assertFalse(d3.isAllowed());
        assertSame(AdjustmentFailureCode.RUN_LIMIT_EXCEEDED, d3.failureCode());
    }

    @Test
    void shouldBlockImmediateOppositeDirection() {
        SafetyGateConfig oppConfig = new SafetyGateConfig(0, 5, true, false);
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(oppConfig, Duration.ZERO, clock);

        // Scale up
        SafetyGateDecision up = gate.evaluate(
                scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), ready());
        gate.recordApplied(up);

        // Immediately scale down (opposite direction)
        SafetyGateDecision down = gate.evaluate(
                scaleDownTimed(12, 10, "policy-1:1"), snapshot(12, 16), ready());
        assertFalse(down.isAllowed());
        assertSame(AdjustmentFailureCode.OPPOSITE_DIRECTION, down.failureCode());
    }

    @Test
    void shouldNotBlockSameDirection() {
        SafetyGateConfig oppConfig = new SafetyGateConfig(0, 5, true, false);
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(oppConfig, Duration.ZERO, clock);

        SafetyGateDecision up1 = gate.evaluate(
                scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), ready());
        gate.recordApplied(up1);

        SafetyGateDecision up2 = gate.evaluate(
                scaleUpTimed(12, 14, "policy-1:1"), snapshot(12, 16), ready());
        assertTrue(up2.isAllowed());
    }

    @Test
    void shouldReturnNoOpWhenTargetMatchesCurrentState() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);
        ScaleAdjustmentCommand noOp = ScaleAdjustmentCommand.noOp(
                "run-1", clockRef.get(), 8, "already at target", "policy-1:0", clock);
        SafetyGateDecision decision = gate.evaluate(noOp, snapshot(8, 16), ready());
        assertEquals(SafetyGateDecision.Outcome.NO_OP, decision.outcome());
        assertFalse(decision.isAllowed());
    }

    @Test
    void shouldRejectInvalidCommandNegativeTarget() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);
        ScaleAdjustmentCommand invalid = new ScaleAdjustmentCommand(
                "run-1:invalid", "run-1", T0, 4, -1,
                "negative target", "policy-1:0", T0, false);
        SafetyGateDecision decision = gate.evaluate(invalid, snapshot(4, 16), ready());
        assertFalse(decision.isAllowed());
        assertSame(AdjustmentFailureCode.INVALID_COMMAND, decision.failureCode());
    }

    @Test
    void shouldRejectNullCommand() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);
        assertThrows(NullPointerException.class,
                () -> gate.evaluate(null, snapshot(8, 16), ready()));
    }

    @Test
    void shouldRejectNullSnapshot() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);
        assertThrows(NullPointerException.class,
                () -> gate.evaluate(scaleUpTimed(8, 12, "policy-1:0"), null, ready()));
    }

    @Test
    void shouldRejectNullReadiness() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);
        assertThrows(NullPointerException.class,
                () -> gate.evaluate(scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), null));
    }

    // --- Requirement 3.6: recordApplied records correct timestamp ---

    @Test
    void shouldRecordAppliedTimestamp() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);

        clockRef.set(T0);
        SafetyGateDecision decision = gate.evaluate(
                scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), ready());
        gate.recordApplied(decision);

        assertEquals(T0, gate.lastAppliedInstant("run-1"));
        assertEquals(1, gate.appliedAdjustmentsForRun());
    }

    @Test
    void shouldNotRecordRejectedDecision() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);

        SafetyGateDecision rejected = gate.evaluate(
                scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), notReady());
        gate.recordApplied(rejected);

        assertNull(gate.lastAppliedInstant("run-1"));
        assertEquals(0, gate.appliedAdjustmentsForRun());
    }

    // --- Requirement 3.7: Clock controllable without sleep ---

    @Test
    void shouldControlTimeWithoutSleep() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(
                config, Duration.ofSeconds(5), clock);

        clockRef.set(T0);
        SafetyGateDecision d1 = gate.evaluate(
                scaleUpTimed(8, 12, "policy-1:0"), snapshot(8, 16), ready());
        gate.recordApplied(d1);

        // Advance 3s — still in cooldown
        clockRef.set(T0.plusSeconds(3));
        SafetyGateDecision d2 = gate.evaluate(
                scaleUpTimed(12, 14, "policy-1:1"), snapshot(12, 16), ready());
        assertFalse(d2.isAllowed());
        assertSame(AdjustmentFailureCode.COOLDOWN_ACTIVE, d2.failureCode());

        // Advance 6s — cooldown expired
        clockRef.set(T0.plusSeconds(6));
        SafetyGateDecision d3 = gate.evaluate(
                scaleUpTimed(12, 14, "policy-1:2"), snapshot(12, 16), ready());
        assertTrue(d3.isAllowed());
    }

    @Test
    void shouldTrackTimePerRunId() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);

        clockRef.set(T0);
        ScaleAdjustmentCommand cmdA = ScaleAdjustmentCommand.create(
                "run-A", T0, 8, 12, "scale up", "policy-A:0", clock);
        SafetyGateDecision dA = gate.evaluate(cmdA, snapshot(8, 16), ready());
        gate.recordApplied(dA);
        assertEquals(T0, gate.lastAppliedInstant("run-A"));

        clockRef.set(T0.plusMillis(500));
        ScaleAdjustmentCommand cmdB = ScaleAdjustmentCommand.create(
                "run-B", clockRef.get(), 8, 12, "scale up", "policy-B:0", clock);
        SafetyGateDecision dB = gate.evaluate(cmdB, snapshot(8, 16), ready());
        assertTrue(dB.isAllowed());
        gate.recordApplied(dB);
        assertEquals(T0.plusMillis(500), gate.lastAppliedInstant("run-B"));

        // run-A cooldown still active at 500ms
        clockRef.set(T0.plusMillis(500));
        ScaleAdjustmentCommand cmdA2 = ScaleAdjustmentCommand.create(
                "run-A", clockRef.get(), 12, 14, "scale up", "policy-A:1", clock);
        SafetyGateDecision dA2 = gate.evaluate(cmdA2, snapshot(12, 16), ready());
        assertFalse(dA2.isAllowed());
        assertSame(AdjustmentFailureCode.COOLDOWN_ACTIVE, dA2.failureCode());
    }

    @Test
    void shouldUseDefaultConfigValues() {
        SafetyGateConfig defaults = SafetyGateConfig.defaults();
        assertEquals(2, defaults.cooldownDecisionIntervals());
        assertEquals(5, defaults.maxAdjustmentsPerRun());
        assertTrue(defaults.blockImmediateOppositeDirection());
        assertFalse(defaults.allowReadyWithRisk());
    }

    @Test
    void shouldExposeCooldownDuration() {
        Duration fiveSec = Duration.ofSeconds(5);
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, fiveSec, clock);
        assertEquals(fiveSec, gate.cooldownDuration());
    }

    @Test
    void shouldExposeConfig() {
        TimeBasedCooldownSafetyGate gate = new TimeBasedCooldownSafetyGate(config, COOLDOWN_1S, clock);
        assertSame(config, gate.config());
    }
}
